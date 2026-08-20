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
package org.apache.openjpa.persistence.lifecycle.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Mirrors TCK entity cascadeall.oneXone.A - inverse side of OneToOne
 * with orphanRemoval. Uses field access with non-standard getter names.
 */
@Entity
@Table(name = "CASCADE_1X1_A")
public class CascadeOneToOneA implements java.io.Serializable {

    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected int value;

    @OneToOne(targetEntity = CascadeOneToOneB.class, mappedBy = "a1",
              orphanRemoval = true)
    protected CascadeOneToOneB b1;

    public CascadeOneToOneA() {
    }

    public CascadeOneToOneA(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public CascadeOneToOneA(String id, String name, int value,
                            CascadeOneToOneB b1) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.b1 = b1;
    }

    public CascadeOneToOneB getB1() {
        return b1;
    }

    public boolean isB1() {
        return getB1() != null;
    }

    public CascadeOneToOneB getB1Info() {
        if (isB1()) {
            return getB1();
        }
        return null;
    }

    public String getAId() {
        return id;
    }

    public String getAName() {
        return name;
    }

    public int getAValue() {
        return value;
    }
}
