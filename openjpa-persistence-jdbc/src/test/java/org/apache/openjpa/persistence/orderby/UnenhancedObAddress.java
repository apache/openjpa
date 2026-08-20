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
package org.apache.openjpa.persistence.orderby;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

/**
 * Embeddable Address with property access containing a nested
 * embedded ZipCode. Used to test @OrderBy dot notation like
 * "zipCode.zip DESC" (property access name).
 */
@Embeddable
public class UnenhancedObAddress implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    protected String street;
    protected String city;
    protected String state;
    protected UnenhancedObZipCode zipcode;

    public UnenhancedObAddress() {
    }

    public UnenhancedObAddress(String street, String city, String state,
            UnenhancedObZipCode zip) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = zip;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @Embedded
    public UnenhancedObZipCode getZipCode() {
        return zipcode;
    }

    public void setZipCode(UnenhancedObZipCode zipcode) {
        this.zipcode = zipcode;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnenhancedObAddress))
            return false;
        UnenhancedObAddress other = (UnenhancedObAddress) o;
        return this.getStreet().equals(other.getStreet())
            && this.getCity().equals(other.getCity())
            && this.getState().equals(other.getState())
            && this.getZipCode().getZip().equals(other.getZipCode().getZip());
    }

    @Override
    public int hashCode() {
        return getStreet().hashCode() + getCity().hashCode()
            + getState().hashCode() + getZipCode().getZip().hashCode();
    }

    @Override
    public String toString() {
        return "UnenhancedObAddress[street: " + getStreet()
            + ", city: " + getCity()
            + ", state: " + getState()
            + ", zip: " + getZipCode().getZip() + "]";
    }
}
