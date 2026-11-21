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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Line of a {@link BulkRangeOrder}, whose foreign key column is in its own
 * table.
 */
@Entity
@Table(name = "BRG_LINE")
public class BulkRangeLine {

    @Id
    @Column(name = "ID")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private BulkRangeOrder order;

    @Column(name = "QUANTITY")
    private int quantity;

    public BulkRangeLine() {
    }

    public BulkRangeLine(long id, BulkRangeOrder order, int quantity) {
        this.id = id;
        this.order = order;
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public BulkRangeOrder getOrder() {
        return order;
    }

    public int getQuantity() {
        return quantity;
    }
}
