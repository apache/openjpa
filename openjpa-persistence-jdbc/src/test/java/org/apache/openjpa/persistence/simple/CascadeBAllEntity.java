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

@Entity
@Table(name = "CASCADE_B_MX1_ALL")
public class CascadeBAllEntity implements java.io.Serializable {
    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected int value;

    @ManyToOne(cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "FK_A")
    protected CascadeAEntity a1;

    public CascadeBAllEntity() {}

    public CascadeBAllEntity(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public CascadeBAllEntity(String id, String name, int value, CascadeAEntity a1) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.a1 = a1;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getValue() { return value; }
    public CascadeAEntity getA1() { return a1; }
    public void setA1(CascadeAEntity a1) { this.a1 = a1; }
}
