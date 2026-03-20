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
package org.apache.openjpa.persistence.query;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Parent entity with an eagerly-loaded collection of children.
 * Used to test that JDBCStoreManager correctly handles inverse
 * relation setting for runtime-enhanced entities in eager collections.
 */
@Entity
@Table(name = "EAGER_PARENT")
public class EagerParentEntity {

    @Id
    private long id;

    private String name;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER,
               cascade = CascadeType.ALL)
    private List<EagerChildEntity> children = new ArrayList<>();

    public EagerParentEntity() {
    }

    public EagerParentEntity(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<EagerChildEntity> getChildren() {
        return children;
    }

    public void setChildren(List<EagerChildEntity> children) {
        this.children = children;
    }

    public void addChild(EagerChildEntity child) {
        this.children.add(child);
        child.setParent(this);
    }
}
