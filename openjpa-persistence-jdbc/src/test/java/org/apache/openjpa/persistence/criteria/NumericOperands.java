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
package org.apache.openjpa.persistence.criteria;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity with numeric attributes of different Java types, used to check the
 * result type of arithmetic expressions.
 */
@Entity
@Table(name = "CR_NUMOPS")
public class NumericOperands {
    @Id
    private long id;

    private int intA;
    private Integer intB;
    private short shortA;
    private long longA;
    private Long longB;
    private BigDecimal decA;

    public NumericOperands() {
    }

    public NumericOperands(long id, int intA, Integer intB, short shortA,
        long longA, Long longB, BigDecimal decA) {
        this.id = id;
        this.intA = intA;
        this.intB = intB;
        this.shortA = shortA;
        this.longA = longA;
        this.longB = longB;
        this.decA = decA;
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

    public Long getLongB() {
        return longB;
    }

    public BigDecimal getDecA() {
        return decA;
    }
}
