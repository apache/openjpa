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

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/**
 * Entity for testing @SecondaryTable schema generation.
 */
@Entity
@Table(name = "SCHEMAGENSIMPLE")
@SecondaryTable(
    name = "SCHEMAGENSIMPLE_SECOND",
    pkJoinColumns = @PrimaryKeyJoinColumn(name = "SECONDARY_ID"),
    foreignKey = @ForeignKey(
        name = "MYCONSTRAINT",
        value = ConstraintMode.CONSTRAINT))
public class SchemaGenSecondary implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private int simpleId;

    public SchemaGenSecondary() {}

    public SchemaGenSecondary(int id) {
        this.simpleId = id;
    }

    public int getSimpleId() { return simpleId; }
    public void setSimpleId(int id) { this.simpleId = id; }
}
