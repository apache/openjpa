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

import jakarta.persistence.Embeddable;

@Embeddable
public class DependentId2 {
    String name;
    EmployeeId2 empPK;

    public DependentId2() {}

    public DependentId2(String name, EmployeeId2 empPK) {
        this.name = name;
        this.empPK = empPK;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setEmpPK(EmployeeId2 empPK) {
        this.empPK = empPK;
    }

    public EmployeeId2 getEmpPK() {
        return empPK;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof DependentId2)) return false;
        DependentId2 d = (DependentId2)o;
        if (!empPK.equals(d.getEmpPK())) return false;
        if (name != null && !name.equals(d.getName())) return false;
        if (name == null && d.getName() != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int ret = 0;
        if (empPK != null)
            ret = ret * 31 + empPK.hashCode();
        ret = ret * 31 + name.hashCode();
        return ret;
    }

}
