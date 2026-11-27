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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class NestedSubqueryOwner {

    @Id
    private long id;

    private String name;

    @ElementCollection
    private Set<String> tags = new HashSet<>();

    @ElementCollection
    private Map<String, String> attributes = new HashMap<>();

    @ManyToOne
    private NestedSubqueryOwner parent;

    @OneToMany(mappedBy = "parent")
    private Set<NestedSubqueryOwner> children = new HashSet<>();

    public NestedSubqueryOwner() {
    }

    public NestedSubqueryOwner(long id, String name, String... tags) {
        this.id = id;
        this.name = name;
        this.tags.addAll(Set.of(tags));
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public NestedSubqueryOwner getParent() {
        return parent;
    }

    public void setParent(NestedSubqueryOwner parent) {
        this.parent = parent;
    }

    public Set<NestedSubqueryOwner> getChildren() {
        return children;
    }
}
