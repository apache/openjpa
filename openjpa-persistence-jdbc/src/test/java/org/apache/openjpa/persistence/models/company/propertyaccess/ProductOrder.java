/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.models.company.propertyaccess;

import java.util.*;
import javax.persistence.*;
import org.apache.openjpa.persistence.models.company.*;

@Entity(name="PRP_ProductOrder")
@Table(name="PRP_ProductOrder") // OPENJPA-121
public class ProductOrder implements IProductOrder {
    private static long idCounter = System.currentTimeMillis();

    private long id = idCounter++;

    private List<LineItem> items = new LinkedList<LineItem>();
    private Date orderDate;
    private Date shippedDate;
    private Customer customer;

    public void setItems(List<? extends ILineItem> items) {
        this.items = (List<LineItem>) items;
    }

    @OneToMany
    public List<LineItem> getItems() {
        return this.items;
    }


    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    @Basic
    public Date getOrderDate() {
        return this.orderDate;
    }


    public void setShippedDate(Date shippedDate) {
        this.shippedDate = shippedDate;
    }

    @Basic
    public Date getShippedDate() {
        return this.shippedDate;
    }


    public void setCustomer(ICustomer customer) {
        setCustomer((Customer) customer);
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    @OneToOne
    public Customer getCustomer() {
        return this.customer;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Id
    public long getId() {
        return this.id;
    }

}
