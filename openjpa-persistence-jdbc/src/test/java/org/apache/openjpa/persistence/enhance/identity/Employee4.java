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
package org.apache.openjpa.persistence.enhance.identity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Employee4 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", insertable = false, updatable = false,
            nullable = false)
	private long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent")
	private List<Dependent4> children = new ArrayList<>();

    private int age;

    public int getAge() {
    	return age;
    }

    public void setAge(int age) {
    	this.age = age;
    }

    public long getId() {
        return id;
    }

    public List<Dependent4> getChildren() {
        return children;
    }

    public void addChild(Dependent4 child) {
        if (child == null) {
            throw new IllegalArgumentException("Cannot add a null Child");
        }
        this.getChildren().add(child);
    }

    public void setChildren(List<Dependent4> children) {
        this.children = children;
    }
}
