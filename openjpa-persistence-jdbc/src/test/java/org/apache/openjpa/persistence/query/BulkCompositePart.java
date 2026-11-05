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
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Root of a joined inheritance hierarchy with a composite primary key, the
 * target of bulk updates of the columns of several tables.
 */
@Entity
@Table(name = "BCK_PART")
@IdClass(BulkCompositeId.class)
@Inheritance(strategy = InheritanceType.JOINED)
public class BulkCompositePart {

    @Id
    @Column(name = "REGION")
    private long region;

    @Id
    @Column(name = "PART_NUMBER")
    private long number;

    private int quantity;

    private String name;

    public BulkCompositePart() {
    }

    public BulkCompositePart(long region, long number, int quantity) {
        this.region = region;
        this.number = number;
        this.quantity = quantity;
        this.name = "name " + region + "-" + number;
    }

    public long getRegion() {
        return region;
    }

    public long getNumber() {
        return number;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
