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

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Owner of an element collection whose primary key is composite, so its owned
 * table cannot be deleted by a single key column and the bulk delete has to
 * fall back to the in-memory path.
 */
@Entity
@Table(name = "BULK_CID_OWNER")
@IdClass(BulkDeleteCompositeId.class)
public class BulkDeleteCompositeOwner {
    @Id
    private long tenant;

    @Id
    private long number;

    private String name;

    @ElementCollection
    @CollectionTable(name = "BULK_CID_TAGS",
        joinColumns = {
            @JoinColumn(name = "OWNER_TENANT", referencedColumnName = "TENANT"),
            @JoinColumn(name = "OWNER_NUMBER", referencedColumnName = "NUMBER")})
    private Set<String> tags = new HashSet<>();

    public long getTenant() {
        return tenant;
    }

    public void setTenant(long tenant) {
        this.tenant = tenant;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getTags() {
        return tags;
    }
}
