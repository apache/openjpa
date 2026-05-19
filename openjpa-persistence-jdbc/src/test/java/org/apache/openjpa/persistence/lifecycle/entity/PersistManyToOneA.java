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
import jakarta.persistence.Table;

/**
 * Mirrors TCK entity persist.manyXone.A - plain entity, no
 * relationship back. Uses field access.
 */
@Entity
@Table(name = "PERSIST_MX1_A")
public class PersistManyToOneA implements java.io.Serializable {

    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected int value;

    public PersistManyToOneA() {
    }

    public PersistManyToOneA(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public String getAId() {
        return id;
    }

    public String getAName() {
        return name;
    }

    public void setAName(String name) {
        this.name = name;
    }

    public int getAValue() {
        return value;
    }
}
