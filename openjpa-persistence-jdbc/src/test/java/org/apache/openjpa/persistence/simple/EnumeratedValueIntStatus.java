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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.EnumeratedValue;

/**
 * Enum using JPA 3.2 @EnumeratedValue with an int code.  The codes are
 * deliberately different from the ordinals, so that a mapping which falls back
 * to the ordinal is detected.
 */
public enum EnumeratedValueIntStatus {

    ACTIVE(10),
    INACTIVE(20),
    PENDING(30);

    @EnumeratedValue
    private final int code;

    EnumeratedValueIntStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
