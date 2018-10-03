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

import org.apache.openjpa.persistence.models.company.IAddress;

@Entity(name="LAZ_Address")
public class Address implements IAddress {
    private static long idCounter = System.currentTimeMillis();

    @Id
    private long id = idCounter++;

    @Basic(fetch=FetchType.LAZY)
    private String streetAddress;

    @Basic(fetch=FetchType.LAZY)
    private String city;

    @Basic(fetch=FetchType.LAZY)
    private String state;

    @Basic(fetch=FetchType.LAZY)
    private String postalCode;

    @Basic(fetch=FetchType.LAZY)
    private String phoneNumber;

    @Override
    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    @Override
    public String getStreetAddress() {
        return this.streetAddress;
    }


    @Override
    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String getCity() {
        return this.city;
    }


    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getState() {
        return this.state;
    }


    @Override
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    @Override
    public String getPostalCode() {
        return this.postalCode;
    }


    @Override
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String getPhoneNumber() {
        return this.phoneNumber;
    }


    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

}
