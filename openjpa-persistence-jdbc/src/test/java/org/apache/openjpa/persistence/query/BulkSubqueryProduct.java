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
package org.apache.openjpa.persistence.query;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/**
 * Entity with a secondary table and a subclass in the same table, like the
 * product of the Jakarta Persistence TCK, the target of bulk updates and
 * deletes with a subquery over the same entity.
 */
@Entity
@Table(name = "BSQ_PRODUCT")
@SecondaryTable(name = "BSQ_PRODUCT_DETAILS",
    pkJoinColumns = @PrimaryKeyJoinColumn(name = "ID"))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "PRODUCT_TYPE")
public class BulkSubqueryProduct {

    @Id
    @Column(name = "ID")
    private long id;

    private int quantity;

    @Column(table = "BSQ_PRODUCT_DETAILS")
    private String detail;

    public BulkSubqueryProduct() {
    }

    public BulkSubqueryProduct(long id, int quantity) {
        this.id = id;
        this.quantity = quantity;
        this.detail = "detail " + id;
    }

    public long getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
