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
package org.apache.openjpa.persistence.sequence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Entity whose named generator qualifies its sequence with a catalog and a
 * schema, so that the key of the emulated counter row is pinned for the
 * qualified case too. Only the metadata of this entity is resolved; it is
 * never mapped to a table.
 */
@Entity
@Table(name = "OJPA3014_ENT3")
@SequenceGenerator(name = QualifiedSeqGenEntity.GENERATOR,
    sequenceName = QualifiedSeqGenEntity.SEQUENCE,
    catalog = QualifiedSeqGenEntity.CATALOG,
    schema = QualifiedSeqGenEntity.SCHEMA,
    initialValue = 50,
    allocationSize = 3)
public class QualifiedSeqGenEntity {

    public static final String GENERATOR = "OJPA3014Gen3";
    public static final String SEQUENCE = "OJPA3014_SEQ3";
    public static final String CATALOG = "OJPA3014CAT";
    public static final String SCHEMA = "OJPA3014SCH";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = GENERATOR)
    private long id;

    private String name;

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
}
