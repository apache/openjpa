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
package org.apache.openjpa.persistence.jdbc.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.jdbc.Index;

/**
 * Test entity for verifying that the {@code columnNames} attribute of the
 * OpenJPA-specific {@link Index} annotation is correctly applied when placed
 * at the field level.
 */
@Entity
@Table(name = "IDX_COL_NAMES_ENTITY")
public class EntityWithIndexColumnNames {

    @Id
    @Column(name = "PK")
    private Long pk;

    @Column(name = "COL_A")
    @Index(name = "idx_col_a_b", columnNames = {"COL_A", "COL_B"})
    private String colA;

    @Column(name = "COL_B")
    private String colB;

    public Long getPk() {
        return pk;
    }

    public void setPk(Long pk) {
        this.pk = pk;
    }

    public String getColA() {
        return colA;
    }

    public void setColA(String colA) {
        this.colA = colA;
    }

    public String getColB() {
        return colB;
    }

    public void setColB(String colB) {
        this.colB = colB;
    }
}
