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
package org.apache.openjpa.persistence.embed;

import java.io.Serializable;

/**
 * IdClass for DID2bEmployee. NOT @Embeddable.
 * Mirrors TCK DID2bEmployeeId (JPA 2.4.1.3 ex2b).
 */
public class DID2bEmployeeId implements Serializable {
    String firstName;
    String lastName;

    public DID2bEmployeeId() {}
    public DID2bEmployeeId(String fn, String ln) { firstName = fn; lastName = ln; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String fn) { firstName = fn; }
    public String getLastName() { return lastName; }
    public void setLastName(String ln) { lastName = ln; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DID2bEmployeeId)) return false;
        DID2bEmployeeId x = (DID2bEmployeeId) o;
        return eq(firstName, x.firstName) && eq(lastName, x.lastName);
    }

    @Override
    public int hashCode() {
        return (firstName != null ? firstName.hashCode() : 0)
             + 31 * (lastName != null ? lastName.hashCode() : 0);
    }

    private static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
