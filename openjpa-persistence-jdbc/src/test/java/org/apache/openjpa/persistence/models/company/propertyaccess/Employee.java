/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.models.company.propertyaccess;

import java.util.*;
import javax.persistence.*;
import org.apache.openjpa.persistence.models.company.*;

@Entity(name="PRP_Employee")
public abstract class Employee extends Person implements IEmployee {
    private FullTimeEmployee manager;
    private Company company;
    private String title;
    private Date hireDate;

    public void setManager(IFullTimeEmployee manager) {
        setManager((FullTimeEmployee) manager);
    }

    public void setManager(FullTimeEmployee manager) {
        this.manager = manager;
    }

    @OneToOne
    public FullTimeEmployee getManager() {
        return this.manager;
    }


    public void setCompany(ICompany company) {
        setCompany((Company) company);
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @OneToOne
    public Company getCompany() {
        return this.company;
    }


    public void setTitle(String title) {
        this.title = title;
    }

    @Basic
    public String getTitle() {
        return this.title;
    }


    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
    }

    @Basic
    public Date getHireDate() {
        return this.hireDate;
    }
}
