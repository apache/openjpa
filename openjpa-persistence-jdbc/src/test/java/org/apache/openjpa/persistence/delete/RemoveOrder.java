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
package org.apache.openjpa.persistence.delete;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "DEL_RM_ORDER")
public class RemoveOrder {

    @Id
    private long id;

    @Version
    private int version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false)
    private RemoveCustomer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<RemoveOrderLine> lines = new ArrayList<>();

    public RemoveOrder() {
    }

    public RemoveOrder(long id, RemoveCustomer customer) {
        this.id = id;
        this.customer = customer;
    }

    public void addLine(RemoveOrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }

    public long getId() {
        return id;
    }

    public RemoveCustomer getCustomer() {
        return customer;
    }

    public List<RemoveOrderLine> getLines() {
        return lines;
    }
}
