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
package org.apache.openjpa.persistence.models.company.fetchlazy;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.apache.openjpa.persistence.models.company.IAddress;
import org.apache.openjpa.persistence.models.company.ICompany;
import org.apache.openjpa.persistence.models.company.IEmployee;
import org.apache.openjpa.persistence.models.company.IProduct;

@Entity(name="LAZ_Company")
public class Company implements ICompany {
    private static long idCounter = System.currentTimeMillis();

    @Id
    private long id = idCounter++;

    @Basic(fetch=FetchType.LAZY)
    private String name;

    @OneToOne(fetch=FetchType.LAZY)
    private Address address;

    @OneToMany(mappedBy="company", fetch=FetchType.LAZY)
    private Set<Employee> employees = new HashSet<>();

    @ManyToMany(mappedBy="distributors", fetch=FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }


    @Override
    public void setAddress(IAddress address) {
        this.address = (Address) address;
    }

    @Override
    public IAddress getAddress() {
        return this.address;
    }


    @Override
    public void setEmployees(Set<? extends IEmployee> employees) {
        this.employees = (Set<Employee>) employees;
    }

    @Override
    public Set<Employee> getEmployees() {
        return this.employees;
    }


    @Override
    public void setProducts(Set<? extends IProduct> products) {
        this.products = (Set<Product>) products;
    }

    @Override
    public Set<Product> getProducts() {
        return this.products;
    }


    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

}
