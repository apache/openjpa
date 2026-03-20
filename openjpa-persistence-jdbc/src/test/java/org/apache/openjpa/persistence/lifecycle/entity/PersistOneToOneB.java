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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Mirrors TCK entity persist.oneXone.B - owning side of OneToOne
 * with cascade=PERSIST and orphanRemoval. Uses field access.
 */
@Entity
@Table(name = "PERSIST_1X1_B")
public class PersistOneToOneB implements java.io.Serializable {

    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected int value;

    @OneToOne(targetEntity = PersistOneToOneA.class,
              cascade = CascadeType.PERSIST,
              fetch = FetchType.EAGER,
              orphanRemoval = true)
    @JoinColumn(name = "FK_FOR_PERSIST_1X1_A")
    protected PersistOneToOneA a1;

    public PersistOneToOneB() {
    }

    public PersistOneToOneB(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public PersistOneToOneB(String id, String name, int value,
                            PersistOneToOneA a1) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.a1 = a1;
    }

    public PersistOneToOneA getA1() {
        return a1;
    }

    public void setA1(PersistOneToOneA a1) {
        this.a1 = a1;
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
