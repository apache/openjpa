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
package org.apache.openjpa.persistence.flush;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Order entity for flush mode tests.
 * Mirrors the TCK schema30 Order entity.
 */
@Entity
@Table(name = "FLUSH_ORDER")
public class UnenhancedFlushOrder {

    private String id;
    private double totalPrice;
    private UnenhancedFlushCustomer customer;

    public UnenhancedFlushOrder() {
    }

    public UnenhancedFlushOrder(String id, double totalPrice) {
        this.id = id;
        this.totalPrice = totalPrice;
    }

    public UnenhancedFlushOrder(String id, UnenhancedFlushCustomer customer) {
        this.id = id;
        this.customer = customer;
    }

    @Id
    @Column(name = "ID")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "TOTALPRICE")
    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    @ManyToOne
    @JoinColumn(name = "FK_CUSTOMER")
    public UnenhancedFlushCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(UnenhancedFlushCustomer customer) {
        this.customer = customer;
    }
}
