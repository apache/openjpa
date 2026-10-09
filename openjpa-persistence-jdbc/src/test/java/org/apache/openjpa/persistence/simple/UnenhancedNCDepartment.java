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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Unenhanced department entity with a generated identifier, used by {@link TestTransientRefNoCascade}.
 */
@Entity
@Table(name = "TRNSREF_DEPT")
public class UnenhancedNCDepartment implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;

    public UnenhancedNCDepartment() {}

    public UnenhancedNCDepartment(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
    @GeneratedValue
    @Column(name = "ID")
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "NAME")
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
