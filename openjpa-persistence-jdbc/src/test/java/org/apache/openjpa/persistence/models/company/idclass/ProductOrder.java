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
package org.apache.openjpa.persistence.models.company.idclass;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.openjpa.persistence.models.company.ICustomer;
import org.apache.openjpa.persistence.models.company.ILineItem;
import org.apache.openjpa.persistence.models.company.IProductOrder;

@Entity(name="IDC_ProductOrder")
public class ProductOrder implements IProductOrder {
    private static int ids = 1;

    @Id
    private int id = ++ids;

    @OneToMany
    private List<LineItem> items = new LinkedList<>();

    @Basic
    private Date orderDate;

    @Basic
    private Date shippedDate;

    @OneToOne
    private Customer customer;

    @Override
    public void setItems(List<? extends ILineItem> items) {
        this.items = (List<LineItem>) items;
    }

    @Override
    public List<LineItem> getItems() {
        return this.items;
    }


    @Override
    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    @Override
    public Date getOrderDate() {
        return this.orderDate;
    }


    @Override
    public void setShippedDate(Date shippedDate) {
        this.shippedDate = shippedDate;
    }

    @Override
    public Date getShippedDate() {
        return this.shippedDate;
    }


    @Override
    public void setCustomer(ICustomer customer) {
        this.customer = (Customer) customer;
    }

    @Override
    public ICustomer getCustomer() {
        return this.customer;
    }
}
