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
package org.apache.openjpa.enhance;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name="UN_PROP")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class UnenhancedPropertyAccess
    implements UnenhancedType, Serializable, Cloneable {

    
    private static final long serialVersionUID = 1L;
    private int id;
    private int version;
    private String sf = "foo";
    private String lazyField = "lazy";

    @Override
    @Id @GeneratedValue
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Version
    protected int getVersion() {
        return version;
    }

    protected void setVersion(int v) {
        version = v;
    }

    @Override
    @Basic
    public String getStringField() {
        return sf;
    }

    @Override
    public void setStringField(String s) {
        sf = s;
    }

    @Override
    @Basic(fetch = FetchType.LAZY)
    public String getLazyField() {
        return lazyField;
    }

    public void setLazyField(String s) {
        lazyField = s;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null)
            return false;
        if (!getClass().isAssignableFrom(o.getClass()))
            return false;

        return getId() == ((UnenhancedPropertyAccess) o).getId();
    }

    @Override
    public int hashCode() {
        return getId();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
