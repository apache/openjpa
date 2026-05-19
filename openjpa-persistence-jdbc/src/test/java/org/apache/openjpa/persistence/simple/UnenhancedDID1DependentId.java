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

import java.io.Serializable;

/**
 * IdClass for UnenhancedDID1Dependent.
 * Mirrors TCK DID1DependentId.
 */
public class UnenhancedDID1DependentId implements Serializable {

    String name;
    long emp;

    public UnenhancedDID1DependentId() {
    }

    public UnenhancedDID1DependentId(String name, long emp) {
        this.name = name;
        this.emp = emp;
    }

    public long getEmp() {
        return emp;
    }

    public void setEmp(long emp) {
        this.emp = emp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return (int) emp;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof UnenhancedDID1DependentId)) {
            return false;
        }
        UnenhancedDID1DependentId other = (UnenhancedDID1DependentId) object;
        if (this.emp != other.emp || this.name == null || !(other.name.equals(this.name))) {
            return false;
        }
        return true;
    }
}
