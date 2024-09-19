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
package org.apache.openjpa.persistence.embed.attrOverrides;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

@Embeddable
public class ContactInfo {
	@ManyToOne
	Address address;

	// Bidirectional
	@ManyToMany
    List<PhoneNumber> phoneNumbers = new ArrayList<>();

    @Embedded
    EmergencyContactInfo ecInfo;

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void addPhoneNumber(PhoneNumber phoneNumber) {
        phoneNumbers.add(phoneNumber);
    }

    public Address getAddress() {
    	return address;
    }

    public void setAddress(Address address) {
    	this.address = address;
    }

    public EmergencyContactInfo getEmergencyContactInfo() {
    	return ecInfo;
    }

    public void setEmergencyContactInfo(EmergencyContactInfo ecInfo) {
    	this.ecInfo = ecInfo;
    }
}
