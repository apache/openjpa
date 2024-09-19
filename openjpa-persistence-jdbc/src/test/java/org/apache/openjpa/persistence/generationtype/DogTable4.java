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
package org.apache.openjpa.persistence.generationtype;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity(name = "DogTable4")
@Table(name = "DOGTABLES4")
public class DogTable4 implements Serializable

{
    
    private static final long serialVersionUID = 1L;

    @Id
    @TableGenerator(name = "Dog_Gen4", table = "ID_Gen4", schema="SCHEMA4G",
            pkColumnName = "GEN_NAME", valueColumnName = "GEN_VAL",
            pkColumnValue = "ID2", initialValue = 100, allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "Dog_Gen4")
    private int id2;

    private String name;

    private float price;

    private boolean domestic;

    public DogTable4() {
        super();

    }

    public DogTable4(String name) {
        this.name = name;

    }

    public int getId2() {
        return id2;
    }

    public void setId2(int id) {
        this.id2 = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {

        this.price = price;
    }

    public boolean isDomestic() {
        return domestic;
    }

    public void setDomestic(boolean domestic) {
        this.domestic = domestic;
    }
}
