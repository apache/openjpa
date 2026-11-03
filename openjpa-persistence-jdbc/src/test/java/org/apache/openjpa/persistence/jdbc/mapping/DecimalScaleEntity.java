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
package org.apache.openjpa.persistence.jdbc.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity with decimal attributes mapped with and without precision and scale.
 */
@Entity
@Table(name = "MAP_DECSCALE")
public class DecimalScaleEntity {
    @Id
    private long id;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private BigDecimal unsized;

    @Column(scale = 3)
    private BigDecimal scaleOnly;

    private BigInteger bigInteger;

    @Column(scale = 2)
    private Double doubleScale;

    public DecimalScaleEntity() {
    }

    public DecimalScaleEntity(long id, BigDecimal amount, BigDecimal unsized,
        BigDecimal scaleOnly, BigInteger bigInteger, Double doubleScale) {
        this.id = id;
        this.amount = amount;
        this.unsized = unsized;
        this.scaleOnly = scaleOnly;
        this.bigInteger = bigInteger;
        this.doubleScale = doubleScale;
    }

    public long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getUnsized() {
        return unsized;
    }

    public BigDecimal getScaleOnly() {
        return scaleOnly;
    }

    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public Double getDoubleScale() {
        return doubleScale;
    }
}
