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

@Entity(name="PRP_Company")
@Table(name="PRP_Company") // OPENJPA-121
public class Company implements ICompany {
    private static long idCounter = System.currentTimeMillis();

    private long id = idCounter++;

    private String name;
    private Address address;
    private Set<Employee> employees = new HashSet<Employee>();
    private Set<Product> products = new HashSet<Product>();

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    public String getName() {
        return this.name;
    }


    public void setAddress(IAddress address) {
        setAddress((Address) address);
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @OneToOne
    public Address getAddress() {
        return this.address;
    }


    public void setEmployees(Set<? extends IEmployee> employees) {
        this.employees = (Set<Employee>) employees;
    }

    @OneToMany(mappedBy="company")
    public Set<Employee> getEmployees() {
        return this.employees;
    }


    public void setProducts(Set<? extends IProduct> products) {
        this.products = (Set<Product>) products;
    }

    @ManyToMany(mappedBy="distributors")
    public Set<Product> getProducts() {
        return this.products;
    }


    public void setId(long id) {
        this.id = id;
    }

    @Id
    public long getId() {
        return this.id;
    }

}
