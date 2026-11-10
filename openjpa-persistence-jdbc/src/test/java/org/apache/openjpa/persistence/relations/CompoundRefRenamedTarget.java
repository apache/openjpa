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
package org.apache.openjpa.persistence.relations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * Entity with a compound id whose column names differ from the names of the id
 * attributes, referenced by {@link CompoundRefRenamedOwner}.
 */
@Entity
@IdClass(CompoundRefTargetId.class)
public class CompoundRefRenamedTarget {

    @Id
    @Column(name = "KEY_1")
    private String id1;

    @Id
    @Column(name = "KEY_2")
    private String id2;

    private String name;

    public CompoundRefRenamedTarget() {
    }

    public CompoundRefRenamedTarget(String id1, String id2, String name) {
        this.id1 = id1;
        this.id2 = id2;
        this.name = name;
    }

    public String getId1() {
        return id1;
    }

    public String getId2() {
        return id2;
    }

    public String getName() {
        return name;
    }
}
