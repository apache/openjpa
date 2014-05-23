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
package org.apache.openjpa.persistence.jpql.joins;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Entity implementation class for Entity: Parent
 *
 */
@Entity
@Table(name="FETCHDEPT")
public class Department implements Serializable {

        private static final long serialVersionUID = -5537435298484817651L;

        @Id
        private int deptno;
        @Version
        private int version;
        private String name;
        @OneToMany(cascade=CascadeType.ALL)
        private List<Employee> employees;
        @OneToMany(cascade=CascadeType.ALL)
        private List<Employee> employee2s;

        public Department() {
                super();
        }

        public Department(int deptno, String name) {
                super();
                this.deptno = deptno;
                this.name = name;
        }
        public int getDeptno() {
                return this.deptno;
        }

        public void setDeptno(int deptno) {
                this.deptno = deptno;
        }

        public int getVersion() {
                return this.version;
        }

        public void setVersion(int version) {
                this.version = version;
        }

        public String getName() {
                return this.name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public List<Employee> getEmployees() {
                return this.employees;
        }
        public void setEmployees(List<Employee> employees) {
                this.employees = employees;
        }

        public List<Employee> getEmployee2s() {
                return this.employee2s;
        }
        public void setEmployee2s(List<Employee> employees) {
                this.employee2s = employees;
        }

        public String toString() {
                return "[Department:depno=" + deptno + ", version=" + version + ", name=" + name +
                                ", employees=" + employees + ", employee2s=" + employee2s+ ']';
        }

}
