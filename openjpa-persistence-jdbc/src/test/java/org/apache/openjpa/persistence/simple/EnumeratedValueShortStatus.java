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
 * Enum using JPA 3.2 @EnumeratedValue with a short code.  A short code is
 * mapped to an int column, so the value read back from the database is an
 * Integer while the value declared by the enum is a Short.
 */
public enum EnumeratedValueShortStatus {

    ACTIVE((short) 11),
    INACTIVE((short) 22),
    PENDING((short) 33);

    @EnumeratedValue
    private final short code;

    EnumeratedValueShortStatus(short code) {
        this.code = code;
    }

    public short getCode() {
        return code;
    }
}
