/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.atlasdb.transaction.impl;

import com.palantir.atlasdb.cell.api.DdlManager;
import com.palantir.atlasdb.cell.api.TransactionKeyValueService;
import com.palantir.atlasdb.cell.api.TransactionKeyValueServiceManager;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.logsafe.exceptions.SafeIllegalStateException;
import java.util.Optional;
import java.util.function.LongSupplier;

public final class FakeTransactionKeyValueServiceManager implements TransactionKeyValueServiceManager {
    private final TransactionKeyValueService delegate;

    public FakeTransactionKeyValueServiceManager(TransactionKeyValueService delegate) {
        this.delegate = delegate;
    }

    @Override
    public TransactionKeyValueService getTransactionKeyValueService(LongSupplier timestampSupplier) {
        return delegate;
    }

    @Override
    public Optional<KeyValueService> getKeyValueService() {
        return Optional.empty();
    }

    @Override
    public DdlManager getDdlManager() {
        throw new SafeIllegalStateException("Not expecting to call in to DDL manager here");
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void close() {}
}
