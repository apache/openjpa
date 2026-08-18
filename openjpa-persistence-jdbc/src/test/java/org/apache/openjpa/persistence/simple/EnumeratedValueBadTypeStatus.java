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
 * Invalid enum: the @EnumeratedValue field is neither a string nor an integral
 * type, which must be rejected when the mapping is built.
 */
public enum EnumeratedValueBadTypeStatus {

    ACTIVE(1.5d),
    INACTIVE(2.5d);

    @EnumeratedValue
    private final double code;

    EnumeratedValueBadTypeStatus(double code) {
        this.code = code;
    }

    public double getCode() {
        return code;
    }
}
