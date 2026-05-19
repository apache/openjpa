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
package org.apache.openjpa.persistence.cascade.mx1;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "CMXB_CASCPERS")
public class CascadeMx1BCascadePersist implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected int value;

    @ManyToOne(targetEntity = CascadeMx1A.class, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "FK_FOR_A")
    protected CascadeMx1A a1;

    public CascadeMx1BCascadePersist() {
    }

    public CascadeMx1BCascadePersist(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public CascadeMx1BCascadePersist(String id, String name, int value, CascadeMx1A a1) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.a1 = a1;
    }

    public CascadeMx1A getA1() {
        return a1;
    }

    public void setA1(CascadeMx1A a1) {
        this.a1 = a1;
    }

    public CascadeMx1A getA1Info() {
        if (getA1() != null)
            return getA1();
        return null;
    }

    public String getBId() {
        return id;
    }

    public String getBName() {
        return name;
    }
}
