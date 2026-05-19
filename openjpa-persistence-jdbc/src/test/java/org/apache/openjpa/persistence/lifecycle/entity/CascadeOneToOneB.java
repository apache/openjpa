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
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Mirrors TCK entity cascadeall.oneXone.B - owning side of OneToOne
 * with cascade=ALL and orphanRemoval. Uses field access with
 * non-standard getter names.
 */
@Entity
@Table(name = "CASCADE_1X1_B")
public class CascadeOneToOneB implements java.io.Serializable {

    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected int value;

    @OneToOne(targetEntity = CascadeOneToOneA.class,
              cascade = CascadeType.ALL, optional = true,
              orphanRemoval = true)
    @JoinColumn(name = "FK_FOR_CASCADE_1X1_A")
    protected CascadeOneToOneA a1;

    public CascadeOneToOneB() {
    }

    public CascadeOneToOneB(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public CascadeOneToOneB(String id, String name, int value,
                            CascadeOneToOneA a1) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.a1 = a1;
    }

    public CascadeOneToOneA getA1() {
        return a1;
    }

    public void setA1(CascadeOneToOneA a1) {
        this.a1 = a1;
    }

    public boolean isA() {
        return getA1() != null;
    }

    public CascadeOneToOneA getA1Info() {
        if (isA()) {
            return getA1();
        }
        return null;
    }

    public String getBId() {
        return id;
    }

    public String getBName() {
        return name;
    }

    public void setBName(String name) {
        this.name = name;
    }

    public int getBValue() {
        return value;
    }
}
