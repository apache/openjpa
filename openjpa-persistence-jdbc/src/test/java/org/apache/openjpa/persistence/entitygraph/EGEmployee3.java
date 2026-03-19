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
package org.apache.openjpa.persistence.entitygraph;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Table;

@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "first_last_graph",
        attributeNodes = {
            @NamedAttributeNode("firstName"),
            @NamedAttributeNode(value = "lastName")
        }
    ),
    @NamedEntityGraph(
        name = "last_salary_graph",
        includeAllAttributes = false,
        attributeNodes = {
            @NamedAttributeNode(value = "lastName"),
            @NamedAttributeNode(value = "salary")
        }
    ),
    @NamedEntityGraph(
        name = "lastname_department_subgraphs",
        includeAllAttributes = true,
        attributeNodes = {
            @NamedAttributeNode(value = "lastName"),
            @NamedAttributeNode(value = "department", subgraph = "department_sub_graph")
        },
        subgraphs = {
            @NamedSubgraph(
                name = "department_sub_graph",
                type = EGDepartment.class,
                attributeNodes = { @NamedAttributeNode("name") }
            )
        }
    )
})
@Entity
@Table(name = "EG_EMPLOYEE3")
public class EGEmployee3 {

    @Id
    private int id;

    private String firstName;

    private String lastName;

    private float salary;

    @ManyToOne
    private EGDepartment department;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public float getSalary() { return salary; }
    public void setSalary(float salary) { this.salary = salary; }
    public EGDepartment getDepartment() { return department; }
    public void setDepartment(EGDepartment department) { this.department = department; }
}
