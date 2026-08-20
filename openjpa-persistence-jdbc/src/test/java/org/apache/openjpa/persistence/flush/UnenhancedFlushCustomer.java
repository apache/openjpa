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
package org.apache.openjpa.persistence.flush;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Customer entity for flush mode tests.
 * Mirrors the TCK schema30 Customer entity.
 */
@Entity
@Table(name = "FLUSH_CUSTOMER")
public class UnenhancedFlushCustomer {

    private String id;
    private String name;
    private UnenhancedFlushSpouse spouse;
    private Collection<UnenhancedFlushOrder> orders =
        new ArrayList<>();

    public UnenhancedFlushCustomer() {
    }

    public UnenhancedFlushCustomer(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
    @Column(name = "ID")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "NAME")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "customer")
    public UnenhancedFlushSpouse getSpouse() {
        return spouse;
    }

    public void setSpouse(UnenhancedFlushSpouse spouse) {
        this.spouse = spouse;
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
    public Collection<UnenhancedFlushOrder> getOrders() {
        return orders;
    }

    public void setOrders(Collection<UnenhancedFlushOrder> orders) {
        this.orders = orders;
    }
}
