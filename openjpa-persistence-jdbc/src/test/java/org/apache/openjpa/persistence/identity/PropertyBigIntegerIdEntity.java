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
 * Property-access entity with BigInteger @Id sharing a table with
 * a BigDecimal @Id entity. Mirrors TCK PropertyBigIntegerId.
 */
@Entity
@Table(name = "SHARED_ID_TABLE2")
public class PropertyBigIntegerIdEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    protected BigInteger id;
    private BigInteger bigInteger;

    public PropertyBigIntegerIdEntity() {
    }

    public PropertyBigIntegerIdEntity(BigInteger id, BigInteger bigInteger) {
        this.id = id;
        this.bigInteger = bigInteger;
    }

    @Id
    @Column(name = "ID")
    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    @Column(name = "THEVALUE")
    public BigInteger getBigInteger() {
        return this.bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }
}
