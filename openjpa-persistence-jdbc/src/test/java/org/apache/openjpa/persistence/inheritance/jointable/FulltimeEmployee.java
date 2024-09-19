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
package org.apache.openjpa.persistence.inheritance.jointable;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.apache.openjpa.persistence.jdbc.Index;

@Inheritance(strategy=InheritanceType.JOINED)
@Entity
@Table(name="WFulltimeEmployee")
public class FulltimeEmployee extends Employee {
    @Column(name="FTEmpProp1",length=10)
    @Basic
    private String ftEmpProp1;


    @ManyToOne(optional=true,cascade={CascadeType.PERSIST,CascadeType.MERGE,CascadeType.REFRESH},fetch=FetchType.LAZY)
    @JoinColumn(name="Dept_No",referencedColumnName="OID")
    @Index
    private Department dept;

    public FulltimeEmployee() {
    }

    public FulltimeEmployee(String desc) {
        setDescription(desc);
    }

    public String getFTEmpProp1() {
        return ftEmpProp1;
    }

    public void setFTEmpProp1(String ftEmpProp1) {
        this.ftEmpProp1 = ftEmpProp1;
    }

    public Department getDept() {
        return dept;
    }

    public void setDept(Department dept) {
        this.dept = dept;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FulltimeEmployee) {
            FulltimeEmployee c = (FulltimeEmployee) other;
            if (c.getOID() == this.getOID() &&
                c.getDept() == this.getDept())
                return true;
        }
        return false;
    }
}

