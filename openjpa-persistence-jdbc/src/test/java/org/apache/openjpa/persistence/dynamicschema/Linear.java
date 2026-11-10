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
package org.apache.openjpa.persistence.dynamicschema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * An entity whose default table name is a word that MySQL and MariaDB
 * reserve, with attributes named after words that only MariaDB reserves
 * (<code>offset</code>), that only MySQL reserves (<code>ntile</code>) and
 * that both reserve (<code>limit</code>), which cannot be table or column
 * names as they are (OPENJPA-3074).
 */
@Entity
public class Linear {

    @Id
    private int id;

    private int offset;

    private int limit;

    private int ntile;

    public Linear() {
    }

    public Linear(int id, int offset, int limit, int ntile) {
        this.id = id;
        this.offset = offset;
        this.limit = limit;
        this.ntile = ntile;
    }

    public int getId() {
        return id;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getNtile() {
        return ntile;
    }
}
