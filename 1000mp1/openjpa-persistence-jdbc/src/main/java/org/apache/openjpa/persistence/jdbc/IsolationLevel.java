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

import java.sql.Connection;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;

/**
 * Isolation levels for use in {@link JDBCFetchPlan#setIsolation}.
 *
 * @since 0.9.7
 */
public enum IsolationLevel {
    DEFAULT(-1),
    NONE(Connection.TRANSACTION_NONE),
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE);

    private final int _connectionConstant;

    private IsolationLevel(int connectionConstant) {
        _connectionConstant = connectionConstant;
    }

    protected int getConnectionConstant() {
        return _connectionConstant;
    }

    protected static IsolationLevel fromConnectionConstant(int constant) {
        switch(constant) {
            case -1:
            case JDBCFetchConfiguration.DEFAULT:
                return DEFAULT;

            case Connection.TRANSACTION_NONE:
                return NONE;

            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return READ_UNCOMMITTED;

            case Connection.TRANSACTION_READ_COMMITTED:
                return READ_COMMITTED;

            case Connection.TRANSACTION_REPEATABLE_READ:
                return REPEATABLE_READ;

            case Connection.TRANSACTION_SERIALIZABLE:
                return SERIALIZABLE;

            default:
                throw new IllegalArgumentException(
                    Integer.valueOf(constant).toString());
        }
    }
}
