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
package org.apache.openjpa.persistence.models.company.idclass;

import java.util.*;
import javax.persistence.*;
import org.apache.openjpa.persistence.models.company.*;

@Entity(name="IDC_Customer")
public final class Customer extends Person implements ICustomer {

    @OneToMany(mappedBy="customer")
    private Collection<ProductOrder> orders = new ArrayList<ProductOrder>();

    @OneToOne
    private Address shippingAddress;

    @OneToOne
    private Address billingAddress;

    public void setOrders(Collection<? extends IProductOrder> orders) {
        this.orders = (Collection<ProductOrder>) orders;
    }

    public Collection<ProductOrder> getOrders() {
        return this.orders;
    }


    public void setShippingAddress(IAddress shippingAddress) {
        this.shippingAddress = (Address) shippingAddress;
    }

    public IAddress getShippingAddress() {
        return this.shippingAddress;
    }


    public void setBillingAddress(IAddress billingAddress) {
        this.billingAddress = (Address) billingAddress;
    }

    public IAddress getBillingAddress() {
        return this.billingAddress;
    }
}
