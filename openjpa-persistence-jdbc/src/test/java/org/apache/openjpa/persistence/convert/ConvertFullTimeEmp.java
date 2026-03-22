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
package org.apache.openjpa.persistence.convert;

import java.io.Serializable;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity extending MappedSuperclass with property access.
 * Has a SalaryConverter on salary.
 * The char[] lastName from the superclass should use auto-apply
 * CharArrayConverter.
 * Mirrors TCK FullTimeEmployee.
 */
@Entity
@Table(name = "CONV_FT_EMP")
@Access(AccessType.PROPERTY)
public class ConvertFullTimeEmp extends ConvertMappedSuper
        implements Serializable {

    private String salary;

    public ConvertFullTimeEmp() {
    }

    public ConvertFullTimeEmp(int id, String firstName, char[] lastName,
            String salary) {
        super(id, firstName, lastName);
        this.salary = salary;
    }

    @Column(name = "SALARY")
    @Convert(converter = SalaryConverter.class)
    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConvertFullTimeEmp)) return false;
        ConvertFullTimeEmp other = (ConvertFullTimeEmp) o;
        if (this.getId() != other.getId()) return false;
        if (!this.getFirstName().equals(other.getFirstName())) return false;
        if (!new String(this.getLastName())
                .equals(new String(other.getLastName()))) return false;
        return this.getSalary().equals(other.getSalary());
    }

    @Override
    public int hashCode() {
        return getId() + getFirstName().hashCode()
            + new String(getLastName()).hashCode()
            + getSalary().hashCode();
    }

    @Override
    public String toString() {
        return "ConvertFullTimeEmp[id=" + getId()
            + ", first=" + getFirstName()
            + ", last=" + new String(getLastName())
            + ", salary=" + getSalary() + "]";
    }
}
