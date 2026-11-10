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
package org.apache.openjpa.persistence.relations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entity referencing {@link CompoundRefRenamedTarget} through the two parts of a compound
 * foreign key which are also exposed as read-only basic attributes. The target has
 * a compound id whose column names differ from its attribute names.
 */
@Entity
@Table(name = "CR_REN_OWNER")
public class CompoundRefRenamedOwner {

    @Id
    private long id;

    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "b_id1", referencedColumnName = "KEY_1"),
            @JoinColumn(name = "b_id2", referencedColumnName = "KEY_2")
    })
    private CompoundRefRenamedTarget b;

    @Column(name = "b_id1", insertable = false, updatable = false)
    private String bId1;

    @Column(name = "b_id2", insertable = false, updatable = false)
    private String bId2;

    public CompoundRefRenamedOwner() {
    }

    public CompoundRefRenamedOwner(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public CompoundRefRenamedTarget getB() {
        return b;
    }

    public void setB(CompoundRefRenamedTarget b) {
        this.b = b;
    }

    public String getBId1() {
        return bId1;
    }

    public String getBId2() {
        return bId2;
    }
}
