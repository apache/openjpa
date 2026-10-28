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
package org.apache.openjpa.persistence.jpql.expressions;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity with numeric attributes of different Java types, used to check the
 * value and the result type of a signed arithmetic expression.
 */
@Entity
@Table(name = "JPQL_SIGNOPS")
public class SignedOperands {
    @Id
    private long id;

    private int intA;
    private Integer intB;
    private short shortA;
    private long longA;
    private BigDecimal decA;
    private double doubleA;

    public SignedOperands() {
    }

    public SignedOperands(long id, int intA, Integer intB, short shortA,
        long longA, BigDecimal decA, double doubleA) {
        this.id = id;
        this.intA = intA;
        this.intB = intB;
        this.shortA = shortA;
        this.longA = longA;
        this.decA = decA;
        this.doubleA = doubleA;
    }

    public long getId() {
        return id;
    }

    public int getIntA() {
        return intA;
    }

    public Integer getIntB() {
        return intB;
    }

    public short getShortA() {
        return shortA;
    }

    public long getLongA() {
        return longA;
    }

    public BigDecimal getDecA() {
        return decA;
    }

    public double getDoubleA() {
        return doubleA;
    }
}
