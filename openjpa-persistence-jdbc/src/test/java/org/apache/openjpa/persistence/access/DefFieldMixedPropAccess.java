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

import static jakarta.persistence.AccessType.FIELD;
import static jakarta.persistence.AccessType.PROPERTY;

import jakarta.persistence.Access;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity(name="DFMPA")
@Access(value=FIELD)
@NamedQueries( {
    @NamedQuery(name="DFMPA.query",
        query="SELECT df FROM DFMPA df WHERE " +
        "df.id = :id AND df.stringField = :strVal"),
    @NamedQuery(name="DFMPA.badQuery",
        query="SELECT p FROM DFMPA p WHERE " +
        "p.id = :id AND p.strField = :strVal") } )
public class DefFieldMixedPropAccess {

    @Id
    @GeneratedValue
    private int id;

    @Version
    private int version;

    @Transient
    private String strField;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setStringField(String val) {
        this.setStrField(val);
    }

    @Access(value=PROPERTY)
    public String getStringField() {
        return getStrField();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefFieldMixedPropAccess) {
            DefFieldMixedPropAccess fa = (DefFieldMixedPropAccess)obj;
            return id == fa.getId() &&
              getStrField().equals(fa.getStringField());
        }
        return false;
    }

    public void setStrField(String strField) {
        this.strField = strField;
    }

    public String getStrField() {
        return strField;
    }
}
