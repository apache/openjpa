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

import javax.persistence.*;
import org.apache.openjpa.persistence.models.company.*;

@Entity(name="PRP_FullTimeEmployee")
public final class FullTimeEmployee extends Employee
    implements IFullTimeEmployee {
    private float salary;

    public void setSalary(float salary) {
        this.salary = salary;
    }

    @Basic
    public float getSalary() {
        return this.salary;
    }
}
