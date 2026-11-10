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
 * reserve and that only their generated lists of invalid column words had,
 * with attributes named after words that both reserve (<code>range</code>)
 * and that only MariaDB reserves (<code>returning</code>) (OPENJPA-3074).
 */
@Entity
public class Dual {

    @Id
    private int id;

    private String range;

    private String returning;

    public Dual() {
    }

    public Dual(int id, String range, String returning) {
        this.id = id;
        this.range = range;
        this.returning = returning;
    }

    public int getId() {
        return id;
    }

    public String getRange() {
        return range;
    }

    public String getReturning() {
        return returning;
    }

    public void setReturning(String returning) {
        this.returning = returning;
    }
}
