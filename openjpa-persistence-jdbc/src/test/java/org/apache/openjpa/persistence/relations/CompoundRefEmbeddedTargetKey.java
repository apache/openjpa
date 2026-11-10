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

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embedded id of {@link CompoundRefEmbeddedTarget}.
 */
@Embeddable
public class CompoundRefEmbeddedTargetKey implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id1")
    private String id1;

    @Column(name = "id2")
    private String id2;

    public CompoundRefEmbeddedTargetKey() {
    }

    public CompoundRefEmbeddedTargetKey(String id1, String id2) {
        this.id1 = id1;
        this.id2 = id2;
    }

    public String getId1() {
        return id1;
    }

    public String getId2() {
        return id2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof CompoundRefEmbeddedTargetKey other
                && Objects.equals(id1, other.id1)
                && Objects.equals(id2, other.id2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id1, id2);
    }
}
