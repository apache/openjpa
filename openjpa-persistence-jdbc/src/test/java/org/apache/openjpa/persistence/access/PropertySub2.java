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

import java.util.Date;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Access(AccessType.PROPERTY)
@NamedQueries( {
    @NamedQuery(name="PropertySub2.query",
        query="SELECT ps FROM PropertySub2 ps WHERE " +
        "ps.id = :id AND ps.name = :name AND ps.createDate = :crtDate"),
    @NamedQuery(name="PropertySub2.badQuery",
        query="SELECT ps FROM PropertySub2 ps WHERE " +
        "ps.id = :id AND ps.name = :name AND ps.crtDate = :crtDate") } )
public class PropertySub2 extends MappedSuperField {

    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreateDate() {
        return crtDate;
    }

    public void setCreateDate(Date date) {
        crtDate = date;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropertySub2) {
            PropertySub2 ps = (PropertySub2)obj;
            String crtDateString = ps.getCreateDate() != null ? ps.getCreateDate().toString() : null;
            if (!crtDate.toString().equals(crtDateString))
                return false;
            return super.equals(obj);
        }
        return false;
    }

}
