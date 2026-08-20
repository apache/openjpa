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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Child entity with ManyToOne cascade=ALL for testing
 * persist-after-remove scenarios.
 */
@Entity
@Table(name = "MX1_CHILD")
public class MxOneChild implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected int value;

    @ManyToOne(cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "FK_PARENT")
    protected MxOneParent parent;

    public MxOneChild() {
    }

    public MxOneChild(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public MxOneChild(String id, String name, int value, MxOneParent parent) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.parent = parent;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public MxOneParent getParent() {
        return parent;
    }

    public void setParent(MxOneParent parent) {
        this.parent = parent;
    }
}
