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
package org.apache.openjpa.persistence.entitymanager;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Simple entity with int PK for testing merge and find operations.
 * Prefixed with "Unenhanced" to skip build-time enhancement.
 */
@Entity
@Table(name = "EM_SPEC_ORDER")
public class UnenhancedOrder implements java.io.Serializable {

    private int id;
    private int total;
    private String description;

    public UnenhancedOrder() {
    }

    public UnenhancedOrder(int id, int total, String description) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "UnenhancedOrder id=" + getId() + ", total=" + getTotal()
            + ", desc=" + getDescription();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnenhancedOrder)) return false;
        UnenhancedOrder other = (UnenhancedOrder) o;
        return this.getId() == other.getId()
            && this.getTotal() == other.getTotal()
            && ((this.getDescription() == null && other.getDescription() == null)
                || (this.getDescription() != null
                    && this.getDescription().equals(other.getDescription())));
    }

    @Override
    public int hashCode() {
        return id;
    }
}
