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
package com.palantir.processors;

@AutoDelegate
public interface TestInterface {
    void methodWithNoParameters();

    void methodWithOneParameter(int p1);

    void methodWithTwoParameters(int p1, int p2);

    void methodWithVarArgs(int... parameters);

    int methodWithReturnType();

    int methodWithReturnTypeAndParameters(int p1);

    int methodWithReturnTypeAndVarArgs(int... parameters);

    @DoNotDelegate
    void methodThatMustBeImplemented();

    void overloadedMethod();

    void overloadedMethod(int p1);

    void overloadedMethod(Integer p1, Integer p2);

    @DoDelegate
    default void defaultMethod() {}

    void overriddenMethod(Integer p1, Integer p2, Integer p3);

    static void staticMethod() {}
}
