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
package org.apache.openjpa.persistence.embed.compositepk;

import java.io.Serializable;
import java.util.Objects;

public class ChapterId implements Serializable {
    private static final long serialVersionUID = 1L;

    private SubjectKey owner;

    private Integer position;

    public ChapterId() {
    }

    public ChapterId(SubjectKey owner, Integer position) {
        this.owner = owner;
        this.position = position;
    }

    public SubjectKey getOwner() {
        return owner;
    }

    public Integer getPosition() {
        return position;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, position);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChapterId other)) {
            return false;
        }
        return Objects.equals(owner, other.owner) && Objects.equals(position, other.position);
    }
}
