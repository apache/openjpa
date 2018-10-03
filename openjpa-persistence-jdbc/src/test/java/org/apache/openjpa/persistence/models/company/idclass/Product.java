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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.apache.openjpa.persistence.models.company.ICompany;
import org.apache.openjpa.persistence.models.company.IProduct;

@Entity(name="IDC_Product")
public class Product implements IProduct {
    private static int ids = 1;

    @Id
    private int id = ++ids;

    @Basic
    private String name;

    @Basic
    private byte[] image;

    @Basic
    private float price;

    @ManyToMany
    private Set<Company> distributors = new HashSet<>();

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }


    @Override
    public void setImage(byte[] image) {
        this.image = image;
    }

    @Override
    public byte[] getImage() {
        return this.image;
    }


    @Override
    public void setPrice(float price) {
        this.price = price;
    }

    @Override
    public float getPrice() {
        return this.price;
    }


    @Override
    public void setDistributors(Set<? extends ICompany> distributors) {
        this.distributors = (Set<Company>) distributors;
    }

    @Override
    public Set<Company> getDistributors() {
        return this.distributors;
    }
}
