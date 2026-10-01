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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Owner of a uni-directional join table relation, of element collections of a
 * basic and of an embeddable type, of a map of embeddables and of an element
 * collection declared inside an embeddable. All of those tables are owned by
 * this entity, so a bulk delete must remove their rows.
 */
@Entity
@Table(name = "BULK_OWNER")
public class BulkDeleteOwner {
    @Id
    private long id;

    private String name;

    @ElementCollection
    @CollectionTable(name = "BULK_OWNER_NICKNAMES",
        joinColumns = @JoinColumn(name = "OWNER_ID"))
    private Set<String> nicknames = new HashSet<>();

    @OneToMany
    @JoinTable(name = "BULK_OWNER_ITEMS",
        joinColumns = @JoinColumn(name = "OWNER_ID"),
        inverseJoinColumns = @JoinColumn(name = "ITEM_ID"))
    private Set<BulkDeleteItem> items = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "BULK_OWNER_ADDRESSES",
        joinColumns = @JoinColumn(name = "OWNER_ID"))
    private List<BulkDeleteAddress> addresses = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "BULK_OWNER_ALIASES",
        joinColumns = @JoinColumn(name = "OWNER_ID"))
    @MapKeyColumn(name = "ALIAS")
    private Map<String, BulkDeleteAddress> aliases = new HashMap<>();

    @Embedded
    private BulkDeleteDetails details = new BulkDeleteDetails();

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

    public Set<String> getNicknames() {
        return nicknames;
    }

    public Set<BulkDeleteItem> getItems() {
        return items;
    }

    public List<BulkDeleteAddress> getAddresses() {
        return addresses;
    }

    public Map<String, BulkDeleteAddress> getAliases() {
        return aliases;
    }

    public BulkDeleteDetails getDetails() {
        return details;
    }
}
