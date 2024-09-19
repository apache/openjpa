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
import jakarta.persistence.Transient;

@Entity
@Access(AccessType.FIELD)
@NamedQueries( {
    @NamedQuery(name="MixedFieldSub.query",
        query="SELECT fs FROM MixedFieldSub fs WHERE " +
        "fs.mid = :id AND fs.name = :name AND fs.createDate = :crtDate " +
        "AND fs.myField = :myField"),
    @NamedQuery(name="MixedFieldSub.badQuery",
        query="SELECT fs FROM MixedFieldSub fs WHERE " +
        "fs.mid = :id AND fs.name = :name AND fs.myFieldProp = :myField") } )
public class MixedFieldSub extends MixedMappedSuper {

    private String myField;

    @Transient
    private Date crtDate;

    @Access(AccessType.PROPERTY)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreateDate() {
        return crtDate;
    }

    public void setCreateDate(Date date) {
        crtDate = date;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MixedFieldSub) {
            MixedFieldSub ps = (MixedFieldSub)obj;
            String crtDateString = ps.getCreateDate() != null ? ps.getCreateDate().toString() : null;
            if (!crtDate.toString().equals(crtDateString))
                return false;
            return super.equals(obj);
        }
        return false;
    }

    public void setMyFieldProp(String myField) {
        this.myField = myField;
    }

    public String getMyFieldProp() {
        return myField;
    }

}
