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
@Table(name="UN_FIELD")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class UnenhancedFieldAccess
    implements UnenhancedType, Serializable, Cloneable {

    
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue private int id;
    @Version public int version;
    protected String stringField = "foo";

    private long longVal;

    @Basic(fetch = FetchType.LAZY)
    private String lazyField = "lazy";

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setStringField(String s) {
        stringField = s;
    }

    @Override
    public String getStringField() {
        return stringField;
    }

    @Override
    public String getLazyField() {
        return lazyField;
    }


    public long getLongVal() {
        return longVal;
    }

    public void setLongVal(long longVal) {
        this.longVal = longVal;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null)
            return false;
        if (!getClass().isAssignableFrom(o.getClass()))
            return false;

        return id == ((UnenhancedFieldAccess) o).id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
