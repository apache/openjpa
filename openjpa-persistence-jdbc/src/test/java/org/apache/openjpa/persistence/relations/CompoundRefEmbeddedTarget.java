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

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * Entity with an embedded compound id which is referenced by {@link CompoundRefEmbeddedOwner}.
 */
@Entity
public class CompoundRefEmbeddedTarget {

    @EmbeddedId
    private CompoundRefEmbeddedTargetKey id;

    private String name;

    public CompoundRefEmbeddedTarget() {
    }

    public CompoundRefEmbeddedTarget(String id1, String id2, String name) {
        this.id = new CompoundRefEmbeddedTargetKey(id1, id2);
        this.name = name;
    }

    public CompoundRefEmbeddedTargetKey getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
