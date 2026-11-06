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
package org.apache.openjpa.persistence.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/**
 * Subclass of {@link BulkCompositePart} in its own joined table with a
 * secondary table. The primary key columns of each table have other names,
 * so the keys of a row have to be mapped from table to table.
 */
@Entity
@Table(name = "BCK_ENGINE")
@PrimaryKeyJoinColumns({
    @PrimaryKeyJoinColumn(name = "ENGINE_REGION",
        referencedColumnName = "REGION"),
    @PrimaryKeyJoinColumn(name = "ENGINE_NUMBER",
        referencedColumnName = "PART_NUMBER") })
@SecondaryTable(name = "BCK_ENGINE_INFO", pkJoinColumns = {
    @PrimaryKeyJoinColumn(name = "INFO_REGION",
        referencedColumnName = "ENGINE_REGION"),
    @PrimaryKeyJoinColumn(name = "INFO_NUMBER",
        referencedColumnName = "ENGINE_NUMBER") })
public class BulkCompositeEngine extends BulkCompositePart {

    private int power;

    @Column(table = "BCK_ENGINE_INFO")
    private String label;

    public BulkCompositeEngine() {
    }

    public BulkCompositeEngine(long region, long number, int quantity,
        int power) {
        super(region, number, quantity);
        this.power = power;
        this.label = "label " + region + "-" + number;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
