/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb;

import static org.mockito.Mockito.spy;

import com.google.common.util.concurrent.MoreExecutors;
import com.palantir.atlasdb.cache.DefaultTimestampCache;
import com.palantir.atlasdb.cell.api.TransactionKeyValueServiceManager;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.keyvalue.impl.DelegatingTransactionKeyValueServiceManager;
import com.palantir.atlasdb.keyvalue.impl.InMemoryKeyValueService;
import com.palantir.atlasdb.keyvalue.impl.StatsTrackingKeyValueService;
import com.palantir.atlasdb.keyvalue.impl.TracingKeyValueService;
import com.palantir.atlasdb.keyvalue.impl.TrackingKeyValueService;
import com.palantir.atlasdb.sweep.queue.SpecialTimestampsSupplier;
import com.palantir.atlasdb.sweep.queue.TargetedSweeper;
import com.palantir.atlasdb.transaction.api.ConflictHandler;
import com.palantir.atlasdb.transaction.api.DeleteExecutor;
import com.palantir.atlasdb.transaction.api.snapshot.KeyValueSnapshotReaderManager;
import com.palantir.atlasdb.transaction.impl.CachingTestTransactionManager;
import com.palantir.atlasdb.transaction.impl.ConflictDetectionManager;
import com.palantir.atlasdb.transaction.impl.ConflictDetectionManagers;
import com.palantir.atlasdb.transaction.impl.DefaultDeleteExecutor;
import com.palantir.atlasdb.transaction.impl.SweepStrategyManager;
import com.palantir.atlasdb.transaction.impl.SweepStrategyManagers;
import com.palantir.atlasdb.transaction.impl.TestKeyValueSnapshotReaderManagers;
import com.palantir.atlasdb.transaction.impl.TestTransactionManager;
import com.palantir.atlasdb.transaction.impl.TestTransactionManagerImpl;
import com.palantir.atlasdb.transaction.impl.TransactionTables;
import com.palantir.atlasdb.transaction.knowledge.TransactionKnowledgeComponents;
import com.palantir.atlasdb.transaction.service.TransactionService;
import com.palantir.atlasdb.transaction.service.TransactionServices;
import com.palantir.atlasdb.util.AtlasDbMetrics;
import com.palantir.atlasdb.util.MetricsManager;
import com.palantir.atlasdb.util.MetricsManagers;
import com.palantir.common.concurrent.PTExecutors;
import com.palantir.lock.LockClient;
import com.palantir.lock.LockService;
import com.palantir.lock.v2.TimelockService;
import com.palantir.timelock.paxos.InMemoryTimelockExtension;
import com.palantir.timestamp.TimestampService;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AtlasDbTestCase {
    private static final String CLIENT = "fake lock client";

    protected final MetricsManager metricsManager = MetricsManagers.createForTests();

    protected LockClient lockClient;
    protected LockService lockService;
    protected TrackingKeyValueService keyValueService;
    protected TransactionKeyValueServiceManager txnKeyValueServiceManager;
    protected TimelockService timelockService;
    protected TimestampService timestampService;
    protected ConflictDetectionManager conflictDetectionManager;
    protected SweepStrategyManager sweepStrategyManager;
    protected TestTransactionManager serializableTxManager;
    protected TestTransactionManager txManager;
    protected TransactionService transactionService;
    protected TargetedSweeper sweepQueue;
    protected SpecialTimestampsSupplier sweepTimestampSupplier;
    protected int sweepQueueShards = 128;

    protected TransactionKnowledgeComponents knowledge;

    protected ExecutorService deleteExecutor;
    protected KeyValueSnapshotReaderManager keyValueSnapshotReaderManager;

    @RegisterExtension
    public InMemoryTimelockExtension inMemoryTimelockExtension = new InMemoryTimelockExtension(CLIENT);

    @BeforeEach
    public void setUp() throws Exception {
        deleteExecutor = MoreExecutors.newDirectExecutorService();
        lockClient = LockClient.of(CLIENT);
        lockService = inMemoryTimelockExtension.getLockService();
        timelockService = inMemoryTimelockExtension.getLegacyTimelockService();
        timestampService = inMemoryTimelockExtension.getTimestampService();
        keyValueService = trackingKeyValueService(getBaseKeyValueService());
        txnKeyValueServiceManager = new DelegatingTransactionKeyValueServiceManager(keyValueService);
        TransactionTables.createTables(keyValueService);
        transactionService = spy(TransactionServices.createRaw(keyValueService, timestampService, false));
        conflictDetectionManager = ConflictDetectionManagers.createWithoutWarmingCache(keyValueService);
        sweepStrategyManager = SweepStrategyManagers.createDefault(keyValueService);
        sweepQueue = spy(TargetedSweeper.createUninitializedForTest(keyValueService, () -> sweepQueueShards));
        knowledge = TransactionKnowledgeComponents.createForTests(keyValueService, metricsManager.getTaggedRegistry());
        DeleteExecutor typedDeleteExecutor = new DefaultDeleteExecutor(keyValueService, deleteExecutor);
        keyValueSnapshotReaderManager = TestKeyValueSnapshotReaderManagers.createForTests(
                txnKeyValueServiceManager, transactionService, sweepStrategyManager, typedDeleteExecutor);
        setUpTransactionManagers();
        sweepQueue.initialize(serializableTxManager);
        sweepTimestampSupplier = new SpecialTimestampsSupplier(
                () -> txManager.getUnreadableTimestamp(), () -> txManager.getImmutableTimestamp());
    }

    private void setUpTransactionManagers() {
        serializableTxManager = constructTestTransactionManager();

        txManager = new CachingTestTransactionManager(serializableTxManager);
    }

    private TrackingKeyValueService trackingKeyValueService(KeyValueService originalKeyValueService) {
        return spy(new TrackingKeyValueService(new StatsTrackingKeyValueService(originalKeyValueService)));
    }

    protected TestTransactionManager constructTestTransactionManager() {
        return new TestTransactionManagerImpl(
                metricsManager,
                keyValueService,
                inMemoryTimelockExtension,
                lockService,
                transactionService,
                conflictDetectionManager,
                sweepStrategyManager,
                DefaultTimestampCache.createForTests(),
                sweepQueue,
                knowledge,
                MoreExecutors.newDirectExecutorService(),
                keyValueSnapshotReaderManager);
    }

    protected KeyValueService getBaseKeyValueService() {
        ExecutorService executor = PTExecutors.newSingleThreadExecutor();
        InMemoryKeyValueService inMemoryKvs = new InMemoryKeyValueService(false, executor);
        KeyValueService tracingKvs = TracingKeyValueService.create(inMemoryKvs);
        return AtlasDbMetrics.instrument(metricsManager.getRegistry(), KeyValueService.class, tracingKvs);
    }

    @AfterEach
    public void tearDown() {
        // JUnit keeps instantiated test cases in memory, so we need to null out
        // some fields to prevent OOMs.
        keyValueService.close();
        keyValueService = null;
        transactionService.close();
        transactionService = null;
        sweepQueue.close();
        sweepQueue = null;
        timestampService = null;
        txManager.close();
        txManager = null;
    }

    protected void overrideConflictHandlerForTable(TableReference table, ConflictHandler conflictHandler) {
        txManager.overrideConflictHandlerForTable(table, conflictHandler);
    }
}
