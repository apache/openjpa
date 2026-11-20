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
package org.apache.openjpa.persistence.delete;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "DEL_RM_LINE")
public class RemoveOrderLine {

    @Id
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private RemoveOrder order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private RemoveProduct product;

    private int quantity;

    public RemoveOrderLine() {
    }

    public RemoveOrderLine(long id, RemoveProduct product, int quantity) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public RemoveOrder getOrder() {
        return order;
    }

    void setOrder(RemoveOrder order) {
        this.order = order;
    }

    public RemoveProduct getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
}
