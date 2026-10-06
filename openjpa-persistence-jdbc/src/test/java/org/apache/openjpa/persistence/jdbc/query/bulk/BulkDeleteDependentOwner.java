/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.jdbc.query.bulk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.Dependent;
import org.apache.openjpa.persistence.ElementDependent;

/**
 * Owner of the relations that a bulk delete must not cascade along: a direct
 * relation marked {@link Dependent}, a direct relation carrying
 * {@link CascadeType#REMOVE}, a uni-directional join table collection and a
 * bi-directional one-to-many marked {@link ElementDependent}, next to an
 * element collection.
 * <p>
 * Only the two single-valued relations used to force a bulk delete of this
 * entity onto the in-memory path: the removed guard read the cascade of the
 * <em>field</em> value, which {@link Dependent} and a to-one
 * {@link CascadeType#REMOVE} set. {@link ElementDependent} and the JPA
 * cascades of a collection set it on the element value instead, which the
 * guard never looked at, so {@code parts}, {@code notes} and {@code tags}
 * never triggered it.
 */
@Entity
@Table(name = "BULK_DEP_OWNER")
public class BulkDeleteDependentOwner {
    @Id
    private long id;

    private String name;

    @OneToOne
    @JoinColumn(name = "PROFILE_ID")
    @Dependent
    private BulkDeleteDependentChild profile;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "CATEGORY_ID")
    private BulkDeleteDependentChild category;

    @OneToMany
    @JoinTable(name = "BULK_DEP_OWNER_PARTS",
        joinColumns = @JoinColumn(name = "OWNER_ID"),
        inverseJoinColumns = @JoinColumn(name = "PART_ID"))
    @ElementDependent
    private Set<BulkDeleteDependentChild> parts = new HashSet<>();

    @OneToMany(mappedBy = "owner")
    @ElementDependent
    private List<BulkDeleteDependentNote> notes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "BULK_DEP_OWNER_TAGS",
        joinColumns = @JoinColumn(name = "OWNER_ID"))
    private Set<String> tags = new HashSet<>();

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

    public BulkDeleteDependentChild getProfile() {
        return profile;
    }

    public void setProfile(BulkDeleteDependentChild profile) {
        this.profile = profile;
    }

    public BulkDeleteDependentChild getCategory() {
        return category;
    }

    public void setCategory(BulkDeleteDependentChild category) {
        this.category = category;
    }

    public Set<BulkDeleteDependentChild> getParts() {
        return parts;
    }

    public List<BulkDeleteDependentNote> getNotes() {
        return notes;
    }

    public Set<String> getTags() {
        return tags;
    }
}
