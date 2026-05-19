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
package org.apache.openjpa.persistence.identity;

import java.math.BigInteger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity with BigInteger @Id sharing a table with BigDecimal @Id entity.
 * Tests that the shared table/column scenario works correctly for both types.
 */
@Entity
@Table(name = "SHARED_ID_TABLE")
public class SharedTableBigIntegerId {

    @Id
    @Column(name = "ID")
    private BigInteger id;

    @Column(name = "THEVALUE")
    private BigInteger value;

    public SharedTableBigIntegerId() {
    }

    public SharedTableBigIntegerId(BigInteger id, BigInteger value) {
        this.id = id;
        this.value = value;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }
}
