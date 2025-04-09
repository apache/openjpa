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
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "INDICES1"
    , indexes = {@Index(name = "idx_index1", columnList = "INDEX1")
        , @Index(name = "idx_long", columnList = "LONG_NAME", unique = true)
        , @Index(name = "idx_wo_spaces", columnList = "INDEX1,COL2,COL3")
        , @Index(name = "idx_with_spaces", columnList = " LONG_NAME , COL2, COL3 ")
        , @Index(columnList = "LONG_NAME, COL2")})
public class EntityWithIndices {
    @Id
    @Column(name = "PK")
    private Long pk;

    @Column(name = "INDEX1")
    private String index1;

    @Column(name = "LONG_NAME")
    private String longName;

    @Column(name = "NAME")
    private String name;
    
    @Column(name = "COL2") 
    private String col2;
    
    @Column(name = "COL3") 
    private String col3;

    public Long getPk() {
        return pk;
    }

    public void setPk(Long pk) {
        this.pk = pk;
    }

    public String getIndex1() {
        return index1;
    }

    public void setIndex1(String index1) {
        this.index1 = index1;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCol2() {
        return col2;
    }

    public void setCol2(String col2) {
        this.col2 = col2;
    }

    public String getCol3() {
        return col3;
    }

    public void setCol3(String col3) {
        this.col3 = col3;
    }
}
