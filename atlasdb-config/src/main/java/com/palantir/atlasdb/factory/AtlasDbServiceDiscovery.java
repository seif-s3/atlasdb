/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.atlasdb.factory;

import com.palantir.atlasdb.namespacedeleter.NamespaceDeleterFactory;
import com.palantir.atlasdb.spi.AtlasDbFactory;
import com.palantir.atlasdb.spi.KeyValueServiceConfig;
import com.palantir.atlasdb.spi.TransactionKeyValueServiceConfig;
import com.palantir.atlasdb.spi.TransactionKeyValueServiceManagerFactory;
import com.palantir.atlasdb.timestamp.DbTimeLockFactory;
import com.palantir.atlasdb.transaction.api.snapshot.KeyValueSnapshotReaderManagerFactory;
import com.palantir.logsafe.SafeArg;
import com.palantir.logsafe.exceptions.SafeIllegalStateException;
import java.util.ServiceLoader;
import java.util.function.Function;

public final class AtlasDbServiceDiscovery {
    private AtlasDbServiceDiscovery() {
        // util
    }

    public static AtlasDbFactory createAtlasFactoryOfCorrectType(KeyValueServiceConfig config) {
        return createAtlasDbServiceOfCorrectType(config, AtlasDbFactory::getType, AtlasDbFactory.class);
    }

    public static TransactionKeyValueServiceManagerFactory<?>
            createTransactionKeyValueServiceManagerFactoryOfCorrectType(TransactionKeyValueServiceConfig config) {
        return createAtlasDbServiceOfCorrectType(
                config.type(),
                TransactionKeyValueServiceManagerFactory::getType,
                TransactionKeyValueServiceManagerFactory.class);
    }

    // TODO (jkong): A little cheaty, currently coupling usage of TKVSMF and KVSRF to be special or non-special...
    public static KeyValueSnapshotReaderManagerFactory createKeyValueSnapshotReaderManagerFactoryOfCorrectType(
            TransactionKeyValueServiceConfig config) {
        return createAtlasDbServiceOfCorrectType(
                config.type(),
                KeyValueSnapshotReaderManagerFactory::getType,
                KeyValueSnapshotReaderManagerFactory.class);
    }

    public static DbTimeLockFactory createDbTimeLockFactoryOfCorrectType(KeyValueServiceConfig config) {
        return createAtlasDbServiceOfCorrectType(config, DbTimeLockFactory::getType, DbTimeLockFactory.class);
    }

    public static NamespaceDeleterFactory createNamespaceDeleterFactoryOfCorrectType(KeyValueServiceConfig config) {
        if (!config.enableNamespaceDeletionDangerousIKnowWhatIAmDoing()) {
            throw new SafeIllegalStateException("Cannot construct a NamespaceDeleterFactory when keyValueService"
                    + ".enableNamespaceDeletionDangerousIKnowWhatIAmDoing is false.");
        }
        return createAtlasDbServiceOfCorrectType(
                config, NamespaceDeleterFactory::getType, NamespaceDeleterFactory.class);
    }

    private static <T> T createAtlasDbServiceOfCorrectType(
            KeyValueServiceConfig config, Function<T, String> typeExtractor, Class<T> clazz) {
        return createAtlasDbServiceOfCorrectType(config.type(), typeExtractor, clazz);
    }

    private static <T> T createAtlasDbServiceOfCorrectType(
            String type, Function<T, String> typeExtractor, Class<T> clazz) {
        for (T element : ServiceLoader.load(clazz)) {
            if (type.equalsIgnoreCase(typeExtractor.apply(element))) {
                return element;
            }
        }
        throw new SafeIllegalStateException(
                "No atlas provider for the configured type could be found. "
                        + "Ensure that the implementation of the AtlasDbFactory is annotated "
                        + "@AutoService with a suitable class as parameter and that it is on your classpath.",
                SafeArg.of("class", clazz),
                SafeArg.of("type", type));
    }
}
