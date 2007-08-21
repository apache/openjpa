/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.persistence.jdbc;

import org.apache.openjpa.jdbc.kernel.EagerFetchModes;

/**
 * Type of fetching to employ.
 *
 * @author Abe White
 * @since 0.4.0
 * @published
 */
public enum FetchMode {
    NONE(EagerFetchModes.EAGER_NONE),
    JOIN(EagerFetchModes.EAGER_JOIN),
    PARALLEL(EagerFetchModes.EAGER_PARALLEL);

    private final int eagerFetchConstant;

    private FetchMode(int value) {
        eagerFetchConstant = value;
    }

    int toKernelConstant() {
        return eagerFetchConstant;
    }

    static FetchMode fromKernelConstant(int kernelConstant) {
        switch (kernelConstant) {
            case EagerFetchModes.EAGER_NONE:
                return NONE;

            case EagerFetchModes.EAGER_JOIN:
                return JOIN;

            case EagerFetchModes.EAGER_PARALLEL:
                return PARALLEL;

            default:
                throw new IllegalArgumentException(kernelConstant + "");
        }
    }
}
