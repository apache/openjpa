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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Mirrors TCK Customer entity — property access with @ElementCollection.
 * Uses property access so that isIntercepting() returns true for
 * pcsubclass (DynamicPersistenceCapable), matching the TCK environment
 * where class redefinition is available.
 * Named "Unenhanced*" so the build-time enhancer skips it.
 */
@Entity
@Table(name = "UNENHANCED_CUST")
public class UnenhancedElementCollectionEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private List<String> phones = new ArrayList<>();

    public UnenhancedElementCollectionEntity() {
    }

    public UnenhancedElementCollectionEntity(String id) {
        this.id = id;
    }

    @Id
    @Column(name = "CUST_ID")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "NAME")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ElementCollection
    @CollectionTable(name = "UNENHANCED_PHONES", joinColumns = @JoinColumn(name = "ID"))
    @Column(name = "PHONE_NUMBER")
    public List<String> getPhones() {
        return this.phones;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(this.getClass().getSimpleName() + "[");
        result.append("id: " + getId());
        // Direct field access — same as TCK Customer.toString()
        // With property access + pcsubclass, isIntercepting()=true,
        // so assignLazyLoadProxies() is skipped and phones can be null
        if (phones.size() > 0) {
            result.append(", phones[");
            int i = 0;
            for (String s : phones) {
                result.append(s);
                i++;
                if (i < phones.size()) {
                    result.append(",");
                }
            }
            result.append("]");
        } else {
            result.append(", phones: empty");
        }
        result.append("]");
        return result.toString();
    }
}
