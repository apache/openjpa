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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Mirrors TCK entity cascadeall.manyXone.B - owning side of ManyToOne
 * with cascade=ALL. Uses field access with non-standard getter names.
 */
@Entity
@Table(name = "CASCADE_MX1_B")
public class CascadeManyToOneB implements java.io.Serializable {

    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected int value;

    @ManyToOne(targetEntity = CascadeManyToOneA.class,
               cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "FK_FOR_CASCADE_MX1_A")
    protected CascadeManyToOneA a1;

    public CascadeManyToOneB() {
    }

    public CascadeManyToOneB(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public CascadeManyToOneB(String id, String name, int value,
                             CascadeManyToOneA a1) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.a1 = a1;
    }

    public CascadeManyToOneA getA1() {
        return a1;
    }

    public void setA1(CascadeManyToOneA a1) {
        this.a1 = a1;
    }

    public boolean isA() {
        return getA1() != null;
    }

    public CascadeManyToOneA getA1Info() {
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

    public void setBName(String bName) {
        this.name = bName;
    }

    public int getBValue() {
        return value;
    }
}
