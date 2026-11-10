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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * An entity whose attributes are named after words that MySQL 8.0 and later
 * reserve, which cannot be column names as they are (OPENJPA-3074).
 */
@Entity
public class RankedEntry {

    @Id
    private int id;

    private int rank;

    private String window;

    private String lead;

    private String system;

    @Column(name = "ROW_NUMBER")
    private int rowNumber;

    @Column(name = "\"LAG\"")
    private int lag;

    public RankedEntry() {
    }

    public RankedEntry(int id, int rank, String window, int rowNumber) {
        this.id = id;
        this.rank = rank;
        this.window = window;
        this.lead = "lead " + id;
        this.system = "system " + id;
        this.rowNumber = rowNumber;
        this.lag = rowNumber * 2;
    }

    public int getId() {
        return id;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getWindow() {
        return window;
    }

    public String getLead() {
        return lead;
    }

    public String getSystem() {
        return system;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public int getLag() {
        return lag;
    }
}
