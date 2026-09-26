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
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;

/**
 * Embeddable that declares an element collection of its own, so the table of
 * that collection is only reachable through the embedded mapping.
 */
@Embeddable
public class BulkDeleteDetails {

    private String note;

    @ElementCollection
    @CollectionTable(name = "BULK_OWNER_LABELS",
        joinColumns = @JoinColumn(name = "OWNER_ID"))
    private Set<String> labels = new HashSet<>();

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Set<String> getLabels() {
        return labels;
    }
}
