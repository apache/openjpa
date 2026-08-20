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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uses property access (annotations on getters) to mirror TCK Order entity.
 * With runtime enhancement, property access allows the pcsubclass to intercept
 * getter calls and trigger lazy loading for hollow entities.
 */
@Entity
@Table(name = "UNENH_SIMPLE_ORDER")
public class UnenhancedSimpleOrderEntity implements java.io.Serializable {
    private int id;

    private int total;

    private String description;

    public UnenhancedSimpleOrderEntity() {}

    public UnenhancedSimpleOrderEntity(int id, int total, String description) {
        this.id = id;
        this.total = total;
        this.description = description;
    }

    @Id
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnenhancedSimpleOrderEntity that = (UnenhancedSimpleOrderEntity) o;
        return id == that.id && total == that.total
            && java.util.Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, total, description);
    }
}
