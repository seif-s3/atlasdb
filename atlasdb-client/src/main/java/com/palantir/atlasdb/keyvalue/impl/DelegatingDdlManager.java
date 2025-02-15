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

package com.palantir.atlasdb.keyvalue.impl;

import com.palantir.atlasdb.cell.api.DdlManager;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import java.util.Map;
import java.util.Set;

public final class DelegatingDdlManager implements DdlManager {

    private final KeyValueService delegate;

    public DelegatingDdlManager(KeyValueService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void createTables(Map<TableReference, byte[]> tableRefToTableMetadata) {
        delegate.createTables(tableRefToTableMetadata);
    }

    @Override
    public void dropTables(Set<TableReference> tableRefs) {
        delegate.dropTables(tableRefs);
    }
}
