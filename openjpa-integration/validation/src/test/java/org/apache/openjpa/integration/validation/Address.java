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
package org.apache.openjpa.integration.validation;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity(name="VAddress")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class Address implements IAddress, Serializable {
    @Transient
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private long id;

    @Basic
    @Pattern(regexp = "^.*$", flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "can contain any character")
    private String streetAddress;   // @NotNull is on IAddress getter

    @Basic
    @Pattern(regexp = "^[A-Z .-]*$", flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "can only contain alpha, '.', '-' and ' ' characters")
    private String city;            // @NotNull is on IAddress getter

    @Basic
    @Size(min = 2, max = 2)
    @Pattern(regexp = "^[A-Z]+$", flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "can only contain alpha characters")
    private String state;           // @NotNull is on IAddress getter

    @Basic
    @Size(min = 5, max = 5)
    @Pattern(regexp = "^[0-9]+$", flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "can only contain numeric characters")
    private String postalCode;      // @NotNull is on IAddress getter

    @Basic
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
