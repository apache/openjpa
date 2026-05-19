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

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity that overrides the MappedSuperclass converter.
 * Employee3 has DotConverter on firstName; this entity overrides
 * with DotConverter2 via class-level @Converts.
 * Mirrors TCK FullTimeEmployee2.
 */
@Entity
@Table(name = "CONV_FT_EMP2")
@Converts({@Convert(attributeName = "firstName",
    converter = DotConverter2.class)})
public class ConvertFullTimeEmp2 extends ConvertMappedSuper2
        implements Serializable {

    private String salary;

    public ConvertFullTimeEmp2() {
    }

    public ConvertFullTimeEmp2(int id, String firstName, String lastName,
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
        if (!(o instanceof ConvertFullTimeEmp2)) return false;
        ConvertFullTimeEmp2 other = (ConvertFullTimeEmp2) o;
        return this.getId() == other.getId()
            && this.getFirstName().equals(other.getFirstName())
            && this.getLastName().equals(other.getLastName())
            && this.getSalary().equals(other.getSalary());
    }

    @Override
    public int hashCode() {
        return getId() + getFirstName().hashCode()
            + getLastName().hashCode() + getSalary().hashCode();
    }

    @Override
    public String toString() {
        return "ConvertFullTimeEmp2[id=" + getId()
            + ", first=" + getFirstName()
            + ", last=" + getLastName()
            + ", salary=" + getSalary() + "]";
    }
}
