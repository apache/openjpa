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

import javax.persistence.*;
import org.apache.openjpa.persistence.models.company.*;

@Entity(name="PRP_LineItem")
@Table(name="PRP_LineItem") // OPENJPA-121
public class LineItem implements ILineItem {
    private static long idCounter = System.currentTimeMillis();

    private long id = idCounter++;

    private int quantity;
    private Product product;

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Basic
    public int getQuantity() {
        return this.quantity;
    }


    public void setProduct(IProduct product) {
        setProduct((Product) product);
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    @OneToOne
    public Product getProduct() {
        return this.product;
    }


    public void setId(long id) {
        this.id = id;
    }

    @Id
    public long getId() {
        return this.id;
    }

}
