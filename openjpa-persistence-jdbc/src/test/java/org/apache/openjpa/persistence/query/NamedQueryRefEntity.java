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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;

/**
 * Entity declaring named queries with and without a JPA 3.2 {@code resultClass}.
 */
@Entity
@Table(name = "NQ_REF_ENTITY")
@NamedQueries({
    @NamedQuery(name = "NQRef.all",
        query = "select o from NamedQueryRefEntity o",
        resultClass = NamedQueryRefEntity.class,
        hints = @QueryHint(name = "openjpa.FetchPlan.MaxFetchDepth", value = "2")),
    @NamedQuery(name = "NQRef.names",
        query = "select o.name from NamedQueryRefEntity o",
        resultClass = String.class),
    @NamedQuery(name = "NQRef.untyped",
        query = "select o from NamedQueryRefEntity o where o.name = 'x'")
})
@NamedNativeQuery(name = "NQRef.native",
    query = "select id, name from NQ_REF_ENTITY",
    resultClass = NamedQueryRefEntity.class)
public class NamedQueryRefEntity {

    @Id
    private int id;

    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
