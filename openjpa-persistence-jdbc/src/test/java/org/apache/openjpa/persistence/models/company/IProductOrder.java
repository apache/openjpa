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
package org.apache.openjpa.persistence.models.company;

import java.util.*;

public interface IProductOrder {

    public void setItems(List<? extends ILineItem> items);
    public List<? extends ILineItem> getItems();

    public void setOrderDate(Date orderDate);
    public Date getOrderDate();

    public void setShippedDate(Date shippedDate);
    public Date getShippedDate();

    public void setCustomer(ICustomer customer);
    public ICustomer getCustomer();
}
