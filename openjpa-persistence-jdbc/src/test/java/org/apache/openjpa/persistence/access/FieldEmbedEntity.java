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
@Access(AccessType.FIELD)
@NamedQueries( {
    @NamedQuery(name="FieldEmbedEntity.query",
        query="SELECT fs FROM FieldEmbedEntity fs WHERE " +
        "fs.id = :id AND fs.name = :name AND fs.epa.firstName = :firstName " +
        "AND fs.epa.lastName = :lastName"),
    @NamedQuery(name="FieldEmbedEntity.badQuery",
        query="SELECT fs FROM FieldEmbedEntity fs WHERE " +
        "fs.id = :id AND fs.name = :name AND fs.epa.fName = :firstName " +
        "AND fs.epa.lName = :lastName") } )
public class FieldEmbedEntity {

    @Id
    @GeneratedValue
    private int id;

    @Basic
    private String name;

    @Embedded
    private EmbedPropAccess epa;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public EmbedPropAccess getEPA() {
        return epa;
    }

    public void setEPA(EmbedPropAccess ep) {
        epa = ep;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FieldEmbedEntity) {
            FieldEmbedEntity ps = (FieldEmbedEntity)obj;
            return epa.equals(ps.getEPA()) && id == ps.getId() &&
                   name.equals(ps.getName());
        }
        return false;
    }
}
