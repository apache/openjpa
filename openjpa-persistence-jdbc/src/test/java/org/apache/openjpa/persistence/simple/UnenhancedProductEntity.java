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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Property-access entity with an embedded ShelfLife, mimicking TCK's Product.
 * Named "Unenhanced*" so the build-time enhancer skips it.
 */
@Entity
@Table(name = "UNENHANCED_PRODUCT")
public class UnenhancedProductEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private double price;
    private UnenhancedShelfLife shelfLife;

    public UnenhancedProductEntity() {
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

    @Column(name = "PRICE")
    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Embedded
    public UnenhancedShelfLife getShelfLife() {
        return shelfLife;
    }

    public void setShelfLife(UnenhancedShelfLife shelfLife) {
        this.shelfLife = shelfLife;
    }
}
