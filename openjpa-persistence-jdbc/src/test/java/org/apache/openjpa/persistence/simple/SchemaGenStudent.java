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

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "SCHEMAGENSTUDENT")
public class SchemaGenStudent {

    @Id
    private int studentId;

    private String studentName;

    @ManyToMany(mappedBy = "students", cascade = CascadeType.ALL)
    private List<SchemaGenCourse> courses;

    public int getStudentId() { return studentId; }
    public void setStudentId(int id) { this.studentId = id; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String name) { this.studentName = name; }
    public List<SchemaGenCourse> getCourses() { return courses; }
    public void setCourses(List<SchemaGenCourse> courses) { this.courses = courses; }
}
