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
package org.apache.openjpa.persistence.models.company.fetchlazy;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;

import org.apache.openjpa.persistence.models.company.IAddress;
import org.apache.openjpa.persistence.models.company.IPerson;

@Entity(name="LAZ_Person")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class Person implements IPerson {
    private static long idCounter = System.currentTimeMillis();

    @Id
    private long id = idCounter++;

    @Basic(fetch=FetchType.LAZY)
    private String firstName;

    @Basic(fetch=FetchType.LAZY)
    private String lastName;

    @OneToOne(fetch=FetchType.LAZY)
    private Address homeAddress;

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getFirstName() {
        return this.firstName;
    }


    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String getLastName() {
        return this.lastName;
    }


    @Override
    public void setHomeAddress(IAddress homeAddress) {
        this.homeAddress = (Address) homeAddress;
    }

    @Override
    public IAddress getHomeAddress() {
        return this.homeAddress;
    }


    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

}
