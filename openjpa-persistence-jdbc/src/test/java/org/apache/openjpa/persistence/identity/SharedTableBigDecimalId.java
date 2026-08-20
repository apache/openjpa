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

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity with BigDecimal @Id sharing a table with BigInteger @Id entity.
 * Tests that BigDecimal values are not cast to BigInteger when multiple
 * entities share the same table and column.
 */
@Entity
@Table(name = "SHARED_ID_TABLE")
public class SharedTableBigDecimalId {

    @Id
    @Column(name = "ID")
    private BigDecimal id;

    @Column(name = "THEVALUE")
    private BigDecimal value;

    public SharedTableBigDecimalId() {
    }

    public SharedTableBigDecimalId(BigDecimal id, BigDecimal value) {
        this.id = id;
        this.value = value;
    }

    public BigDecimal getId() {
        return id;
    }

    public void setId(BigDecimal id) {
        this.id = id;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
