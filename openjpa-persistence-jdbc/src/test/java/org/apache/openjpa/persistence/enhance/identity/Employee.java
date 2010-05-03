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

import javax.persistence.*;

import java.util.*;

@Entity
@Table(name="EMP2_MBI")
public class Employee {
    @EmbeddedId
    EmployeeId empId;
    
    @OneToMany(mappedBy="emp")
    List<Dependent> dependents = new ArrayList<Dependent>();
    
    public EmployeeId getEmpId() {
        return empId;
    }
    
    public void setEmpId(EmployeeId empId) {
        this.empId = empId;
    }
    
    public List<Dependent> getDependents() {
        return dependents;
    }
    
    public void setDependents(List<Dependent> dependents) {
        this.dependents = dependents;
    }
    
    public void addDependent(Dependent d) {
        dependents.add(d);
    }
    
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Employee)) return false;
        Employee e0 = (Employee)o;
        EmployeeId eid0 = e0.getEmpId();
        List<Dependent> ds0 = e0.getDependents();
        if (!empId.equals(eid0)) return false;
        if (ds0 != null && ds0.size() != 0 && dependents == null) return false; 
        if (ds0 == null && dependents != null && dependents.size() != 0)
            return false;
        if (ds0 == null && dependents == null) return true;
        if (ds0 != null && dependents != null) { 
            if (ds0.size() != dependents.size()) return false;
        }
        return true;
    }
    
    public int hashCode() {
        int ret = 0;
        ret = ret * 31 + empId.hashCode();
        for (Dependent d : dependents) {
            ret = ret * 31 +d.id.hashCode();
        }
        return ret;
    }
}
