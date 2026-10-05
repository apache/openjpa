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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Inverse side of a dependent, bi-directional one-to-many: the foreign key
 * lives in this table, which the owner does not own, so a bulk delete of the
 * owner does not reach these rows.
 */
@Entity
@Table(name = "BULK_DEP_NOTE")
public class BulkDeleteDependentNote {
    @Id
    private long id;

    private String text;

    @ManyToOne
    @JoinColumn(name = "OWNER_ID")
    private BulkDeleteDependentOwner owner;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public BulkDeleteDependentOwner getOwner() {
        return owner;
    }

    public void setOwner(BulkDeleteDependentOwner owner) {
        this.owner = owner;
    }
}
