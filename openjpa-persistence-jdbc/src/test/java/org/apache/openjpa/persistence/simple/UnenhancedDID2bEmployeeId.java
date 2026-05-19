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
package org.apache.openjpa.persistence.simple;

import java.io.Serializable;

/**
 * IdClass for UnenhancedDID2bEmployee. NOT @Embeddable.
 * Mirrors TCK DID2bEmployeeId.
 */
public class UnenhancedDID2bEmployeeId implements Serializable {

    String firstName;
    String lastName;

    public UnenhancedDID2bEmployeeId() {
    }

    public UnenhancedDID2bEmployeeId(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnenhancedDID2bEmployeeId)) {
            return false;
        }
        UnenhancedDID2bEmployeeId other = (UnenhancedDID2bEmployeeId) obj;
        if ((this.firstName == null) ? (other.firstName != null) : !this.firstName.equals(other.firstName)) {
            return false;
        }
        if ((this.lastName == null) ? (other.lastName != null) : !this.lastName.equals(other.lastName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (this.firstName != null ? this.firstName.hashCode() : 0);
        hash = 29 * hash + (this.lastName != null ? this.lastName.hashCode() : 0);
        return hash;
    }
}
