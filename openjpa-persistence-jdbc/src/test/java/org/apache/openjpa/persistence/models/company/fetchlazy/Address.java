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
package org.apache.openjpa.persistence.models.company.fetchlazy;

import javax.persistence.*;
import org.apache.openjpa.persistence.models.company.*;

@Entity(name="LAZ_Address")
@Table(name="LAZ_Address") // OPENJPA-121
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

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getStreetAddress() {
        return this.streetAddress;
    }


    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return this.city;
    }


    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return this.state;
    }


    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getPostalCode() {
        return this.postalCode;
    }


    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

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
