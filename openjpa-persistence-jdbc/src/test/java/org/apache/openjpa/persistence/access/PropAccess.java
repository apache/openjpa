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

import static jakarta.persistence.AccessType.PROPERTY;

import jakarta.persistence.Access;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Version;

@Entity
@Access(value=PROPERTY)
@NamedQuery(name="PropertyAccess.query",
    query="SELECT pa FROM PropAccess pa WHERE " +
        "pa.id = :id AND pa.strProp = :strVal")
public class PropAccess {

    private int id;

    private int version;

    private String strField;

    public void setId(int id) {
        this.id = id;
    }

    @Id
    @GeneratedValue
    public int getId() {
        return id;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Version
    public int getVersion() {
        return version;
    }

    public void setStrProp(String var) {
        this.strField = var;
    }

    public String getStrProp() {
        return strField;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PropAccess) {
            PropAccess pa = (PropAccess)obj;
            return getId() == pa.getId() &&
                getStrProp().equals(pa.getStrProp());
        }
        return false;
    }
}
