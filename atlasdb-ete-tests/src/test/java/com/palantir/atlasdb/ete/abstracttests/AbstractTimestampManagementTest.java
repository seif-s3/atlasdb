/*
 * (c) Copyright 2023 Palantir Technologies Inc. All rights reserved.
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
package com.palantir.atlasdb.ete.abstracttests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.palantir.atlasdb.ete.utilities.EteExtension;
import com.palantir.atlasdb.timestamp.EteTimestampResource;
import org.junit.jupiter.api.Test;

public abstract class AbstractTimestampManagementTest {

    // TODO(jlach): why single node? why not all nodes?
    // based on TodoEteTest
    private final EteTimestampResource timestampClient =
            EteExtension.createClientToSingleNode(EteTimestampResource.class);

    @Test
    public void shouldBeAbleToFetchAndForwardTimestamp() {
        assertThatCode(timestampClient::getFreshTimestamp).doesNotThrowAnyException();

        long newts = timestampClient.getFreshTimestamp() + 10000000;
        timestampClient.fastForwardTimestamp(newts);

        assertThat(timestampClient.getFreshTimestamp()).isGreaterThan(newts);
    }
}
