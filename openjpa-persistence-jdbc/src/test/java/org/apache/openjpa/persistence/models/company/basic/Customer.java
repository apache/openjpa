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
package org.apache.openjpa.persistence.models.company.basic;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.apache.openjpa.persistence.models.company.IAddress;
import org.apache.openjpa.persistence.models.company.ICustomer;
import org.apache.openjpa.persistence.models.company.IProductOrder;

@Entity(name="BAS_Customer")
public class Customer extends Person implements ICustomer {
    @OneToMany(mappedBy="customer")
    private Collection<ProductOrder> orders = new ArrayList<>();

    @OneToOne
    private Address shippingAddress;

    @OneToOne
    private Address billingAddress;

    @Override
    public void setOrders(Collection<? extends IProductOrder> orders) {
        this.orders = (Collection<ProductOrder>) orders;
    }

    @Override
    public Collection<ProductOrder> getOrders() {
        return this.orders;
    }


    @Override
    public void setShippingAddress(IAddress shippingAddress) {
        this.shippingAddress = (Address) shippingAddress;
    }

    @Override
    public IAddress getShippingAddress() {
        return this.shippingAddress;
    }


    @Override
    public void setBillingAddress(IAddress billingAddress) {
        this.billingAddress = (Address) billingAddress;
    }

    @Override
    public IAddress getBillingAddress() {
        return this.billingAddress;
    }
}
