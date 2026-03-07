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
package org.apache.openjpa.persistence.enhance.fieldaccess;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity using PROPERTY access (@Id on getter) with a field that has
 * non-standard accessor names (getdescription/setdescription instead of
 * getDescription/setDescription).
 *
 * Per the JPA spec, in PROPERTY access mode only properties following
 * JavaBeans naming conventions are persistent. The "description" field
 * with non-standard accessors should be treated as non-persistent.
 */
@Entity
@Table(name = "FA_ORDER")
public class FieldAccessOrder implements java.io.Serializable {

    private int id;

    private int total;

    private String description;

    public FieldAccessOrder() {
    }

    public FieldAccessOrder(int id, int total, String description) {
        this.id = id;
        this.total = total;
        this.description = description;
    }

    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    // Non-standard accessor names (lowercase 'd') — not JavaBeans compliant.
    // In PROPERTY access mode, this should NOT be a persistent property.
    public String getdescription() {
        return description;
    }

    public void setdescription(String description) {
        this.description = description;
    }
}
