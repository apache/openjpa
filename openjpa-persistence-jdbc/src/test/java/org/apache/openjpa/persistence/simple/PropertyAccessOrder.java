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
package org.apache.openjpa.persistence.simple;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity using property access with non-standard JavaBeans getter/setter naming.
 * The getter is getdescription() / setdescription() (lowercase 'd')
 * which mirrors the pattern used in the JPA TCK entityManager tests.
 */
@Entity
@Table(name = "PA_ORDER")
public class PropertyAccessOrder implements java.io.Serializable {

    private int id;
    private int total;
    private String description;

    public PropertyAccessOrder() {
    }

    public PropertyAccessOrder(int id, int total, String description) {
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

    // Non-standard getter/setter with lowercase 'd' (matches TCK pattern)
    public String getdescription() {
        return description;
    }

    public void setdescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof PropertyAccessOrder order)) return false;
        return id == order.id && total == order.total
            && Objects.equals(description, order.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, total, description);
    }

    @Override
    public String toString() {
        return "PropertyAccessOrder id=" + getId() + ", total=" + getTotal()
            + ", desc=" + getdescription();
    }
}
