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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="MED3_MBI")
@IdClass(PersonId3.class)
public class MedicalHistory3 {
    String name;

    @Id
    @OneToOne Person3 patient;

    public Person3 getPatient() {
        return patient;
    }

    public void setPatient(Person3 p) {
        this.patient = p;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof MedicalHistory3)) return false;
        MedicalHistory3 m0 = (MedicalHistory3)o;
        String name0 = m0.getName();
        if (name != null && !name.equals(name0)) return false;
        if (name == null && name0 != null) return false;
        Person3 p0 = m0.getPatient();
        if (patient != null && !patient.equals(p0)) return false;
        if (patient == null && p0 != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int ret = 0;
        ret = ret * 31 + name.hashCode();
       	//ret = ret * 31 + patient.hashCode();
        return ret;
    }

}
