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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.PersistentCollection;
import org.apache.openjpa.persistence.jdbc.ContainerTable;
import org.apache.openjpa.persistence.jdbc.ElementColumn;
import org.apache.openjpa.persistence.jdbc.XJoinColumn;

/**
 * Owner of an element collection whose collection table is shared with
 * {@link BulkDeleteSharedA}. A constant join column discriminates the rows of
 * that table, so the rows of this owner are only the ones carrying its own
 * discriminator value.
 */
@Entity
@Table(name = "BULK_SHARED_B")
public class BulkDeleteSharedB {
    @Id
    private long id;

    private String name;

    @PersistentCollection
    @ContainerTable(name = "BULK_SHARED_COLL",
        joinColumns = {
            @XJoinColumn(name = "OWNER_ID", referencedColumnName = "ID",
                columnDefinition = "BIGINT"),
            @XJoinColumn(name = "DISC", referencedColumnName = "'B'",
                columnDefinition = "VARCHAR(1)") })
    @ElementColumn(name = "VAL")
    private Set<String> values = new HashSet<>();

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

    public Set<String> getValues() {
        return values;
    }
}
