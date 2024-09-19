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
package org.apache.openjpa.persistence.proxy.delayed.llist;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.proxy.delayed.Award;
import org.apache.openjpa.persistence.proxy.delayed.Certification;
import org.apache.openjpa.persistence.proxy.delayed.IDepartment;
import org.apache.openjpa.persistence.proxy.delayed.IEmployee;
import org.apache.openjpa.persistence.proxy.delayed.Location;
import org.apache.openjpa.persistence.proxy.delayed.Product;

@Entity
@Table(name="DC_DEPARTMENT")
public class Department implements IDepartment, Serializable {

    private static final long serialVersionUID = -6923551949033215888L;

    @Id
    @GeneratedValue
    private int id;

    @OneToMany(cascade={CascadeType.ALL}, fetch=FetchType.LAZY, targetEntity=Employee.class)
    @JoinTable(name="DC_DEP_EMP")
    private Queue<IEmployee> employees;

    @OrderColumn
    @OneToMany(cascade={CascadeType.ALL}, fetch=FetchType.LAZY)
    @JoinTable(name="DC_DEP_LOC")
    private LinkedList<Location> locations;

    @OneToMany(cascade={CascadeType.ALL}, fetch=FetchType.EAGER)
    @JoinTable(name="DC_DEP_PRD")
    private Queue<Product> products;

    @ElementCollection(fetch=FetchType.LAZY)
    @CollectionTable(name="DC_DEP_CERT")
    private Queue<Certification> certifications;

    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="DC_DEP_AWD")
    private LinkedList<Award> awards;

    @Override
    public void setEmployees(Collection<IEmployee> employees) {
        this.employees = (Queue<IEmployee>)employees;
    }

    @Override
    public Collection<IEmployee> getEmployees() {
        return employees;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setLocations(Collection<Location> locations) {
        this.locations =(LinkedList<Location>)locations;
    }

    @Override
    public Collection<Location> getLocations() {
        return locations;
    }

    @Override
    public void setProducts(Collection<Product> products) {
        this.products = (Queue<Product>)products;
    }

    @Override
    public Collection<Product> getProducts() {
        return products;
    }

    @Override
    public void setCertifications(Collection<Certification> certifications) {
        this.certifications = (Queue<Certification>)certifications;
    }

    @Override
    public Collection<Certification> getCertifications() {
        return certifications;
    }

    @Override
    public void setAwards(Collection<Award> awards) {
        this.awards = (LinkedList<Award>)awards;
    }

    @Override
    public Collection<Award> getAwards() {
        return awards;
    }
}
