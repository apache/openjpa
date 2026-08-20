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
 * Property-access entity with BigDecimal @Id sharing a table with
 * a BigInteger @Id entity. Mirrors TCK PropertyBigDecimalId.
 */
@Entity
@Table(name = "SHARED_ID_TABLE2")
public class PropertyBigDecimalIdEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    protected BigDecimal id;
    private BigDecimal bigDecimal;

    public PropertyBigDecimalIdEntity() {
    }

    public PropertyBigDecimalIdEntity(BigDecimal id, BigDecimal bigDecimal) {
        this.id = id;
        this.bigDecimal = bigDecimal;
    }

    @Id
    @Column(name = "ID")
    public BigDecimal getId() {
        return id;
    }

    public void setId(BigDecimal id) {
        this.id = id;
    }

    @Column(name = "THEVALUE")
    public BigDecimal getBigDecimal() {
        return this.bigDecimal;
    }

    public void setBigDecimal(BigDecimal bigDecimal) {
        this.bigDecimal = bigDecimal;
    }
}
