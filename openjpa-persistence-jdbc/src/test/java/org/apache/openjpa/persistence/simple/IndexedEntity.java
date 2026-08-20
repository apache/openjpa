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
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Entity with @Index using ASC/DESC sort directions in columnList.
 */
@Entity
@Table(name = "INDEXED_ENTITY", indexes = {
    @Index(name = "IDX_VAL_DESC", columnList = "svalue DESC"),
    @Index(name = "IDX_MULTI", columnList = "svalue ASC, svalue2 DESC")
})
public class IndexedEntity {

    @Id
    private int id;

    private String svalue;

    private String svalue2;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSvalue() {
        return svalue;
    }

    public void setSvalue(String svalue) {
        this.svalue = svalue;
    }

    public String getSvalue2() {
        return svalue2;
    }

    public void setSvalue2(String svalue2) {
        this.svalue2 = svalue2;
    }
}
