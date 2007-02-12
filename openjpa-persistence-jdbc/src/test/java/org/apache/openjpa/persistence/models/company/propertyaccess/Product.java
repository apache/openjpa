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

@Entity(name="PRP_Product")
@Table(name="PRP_Product") // OPENJPA-121
public class Product implements IProduct {
    private static long idCounter = System.currentTimeMillis();

    private long id = idCounter++;

    private String name;
    private byte[] image;
    private float price;
    private Set<Company> distributors = new HashSet<Company>();

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    public String getName() {
        return this.name;
    }


    public void setImage(byte[] image) {
        this.image = image;
    }

    @Basic
    public byte[] getImage() {
        return this.image;
    }


    public void setPrice(float price) {
        this.price = price;
    }

    @Basic
    public float getPrice() {
        return this.price;
    }


    public void setDistributors(Set<? extends ICompany> distributors) {
        this.distributors = (Set<Company>) distributors;
    }

    @ManyToMany
    public Set<Company> getDistributors() {
        return this.distributors;
    }


    public void setId(long id) {
        this.id = id;
    }

    @Id
    public long getId() {
        return this.id;
    }

}
