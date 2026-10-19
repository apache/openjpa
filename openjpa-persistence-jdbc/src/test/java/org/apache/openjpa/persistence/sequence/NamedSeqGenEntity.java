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
 * Entity using a named {@code @SequenceGenerator} with non-default
 * {@code initialValue} and {@code allocationSize}. The generator is shared
 * with {@link NamedSeqGenEntity2} so that the two entities draw from the
 * same counter.
 */
@Entity
@Table(name = "OJPA3014_ENT1")
@SequenceGenerator(name = NamedSeqGenEntity.GENERATOR,
    sequenceName = NamedSeqGenEntity.SEQUENCE,
    initialValue = NamedSeqGenEntity.INITIAL,
    allocationSize = NamedSeqGenEntity.ALLOCATE)
public class NamedSeqGenEntity {

    public static final String GENERATOR = "OJPA3014Gen";
    public static final String SEQUENCE = "OJPA3014_SEQ";
    public static final int INITIAL = 100;
    public static final int ALLOCATE = 7;

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
