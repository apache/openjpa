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
import jakarta.persistence.Basic;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

@Entity
@Access(AccessType.PROPERTY)
@NamedQueries( {
    @NamedQuery(name="PropEmbedEntity.query",
        query="SELECT fs FROM PropEmbedEntity fs WHERE " +
        "fs.id = :id AND fs.name = :name AND fs.embedProp.fName = :firstName " +
        "AND fs.embedProp.lName = :lastName"),
    @NamedQuery(name="PropEmbedEntity.badQuery",
        query="SELECT fs FROM PropEmbedEntity fs WHERE " +
        "fs.id = :id AND fs.name = :name AND fs.embedProp.firstName = " +
        ":firstName AND fs.embedProp.lastName = :lastName") } )
public class PropEmbedEntity {

    private int id;

    private String name;

    private EmbedFieldAccess efa;

    public void setId(int id) {
        this.id = id;
    }

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    public String getName() {
        return name;
    }

    @Embedded
    public EmbedFieldAccess getEmbedProp() {
        return efa;
    }

    public void setEmbedProp(EmbedFieldAccess ef) {
        efa = ef;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropEmbedEntity) {
            PropEmbedEntity ps = (PropEmbedEntity)obj;
            return getEmbedProp().equals(ps.getEmbedProp())
                && getId() == ps.getId() &&
                getName().equals(ps.getName());
        }
        return false;
    }
}
