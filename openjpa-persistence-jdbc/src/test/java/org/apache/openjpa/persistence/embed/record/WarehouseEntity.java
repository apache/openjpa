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
package org.apache.openjpa.persistence.embed.record;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity that embeds the same AddressRecord twice using
 * {@link AttributeOverrides} to map each to distinct columns.
 */
@Entity
@Table(name = "REC_WAREHOUSE")
public class WarehouseEntity {

    @Id
    @GeneratedValue
    private long id;

    private String name;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street",
                column = @Column(name = "SHIP_STREET")),
        @AttributeOverride(name = "city",
                column = @Column(name = "SHIP_CITY")),
        @AttributeOverride(name = "zip",
                column = @Column(name = "SHIP_ZIP"))
    })
    private AddressRecord shippingAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street",
                column = @Column(name = "BILL_STREET")),
        @AttributeOverride(name = "city",
                column = @Column(name = "BILL_CITY")),
        @AttributeOverride(name = "zip",
                column = @Column(name = "BILL_ZIP"))
    })
    private AddressRecord billingAddress;

    public WarehouseEntity() {
    }

    public WarehouseEntity(String name, AddressRecord shippingAddress,
                           AddressRecord billingAddress) {
        this.name = name;
        this.shippingAddress = shippingAddress;
        this.billingAddress = billingAddress;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AddressRecord getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(AddressRecord shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public AddressRecord getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(AddressRecord billingAddress) {
        this.billingAddress = billingAddress;
    }
}
