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
package org.apache.openjpa.persistence.access;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Transient;

@Entity
@Access(AccessType.FIELD)
@NamedQueries( {
    @NamedQuery(name="MixedNestedEmbedEntity.query",
        query="SELECT fs FROM MixedNestedEmbedEntity fs WHERE " +
        "fs.id = :id AND fs.name = :name AND " +
        "fs.eip.innerName = :innerName AND " +
        "fs.eip.outerField.outName = :outerName"),
    @NamedQuery(name="MixedNestedEmbedEntity.badQuery",
        query="SELECT fs FROM MixedNestedEmbedEntity fs WHERE " +
        "fs.id = :id AND fs.name = :name AND " +
        "fs.eip.innerName = :innerName AND " +
        "fs.eip.outerField.outerName = :outerName") })
public class MixedNestedEmbedEntity {

    @Transient
    private int mid;

    private String name;

    @Embedded
    private EmbedInnerProp eip;

    public void setId(int id) {
        this.mid = id;
    }

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    public int getId() {
        return mid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public EmbedInnerProp getEmbedProp() {
        return eip;
    }

    public void setEmbedProp(EmbedInnerProp ep) {
        eip = ep;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MixedNestedEmbedEntity) {
            MixedNestedEmbedEntity ps = (MixedNestedEmbedEntity)obj;
            return getEmbedProp().equals(ps.getEmbedProp())
                && getId() == ps.getId() &&
                getName().equals(ps.getName());
        }
        return false;
    }
}
