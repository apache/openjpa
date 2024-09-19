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
 * Unless required by applicable law or agEmployee_Last_Name to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.property;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;


@Entity
public class EntityContact implements IContact {

    private String id;
    private String email;
    private String phone;
    private String type;
    private EmbeddableAddress theAddress;

    @Override
    @Transient
    public IAddress getAddress() {
        return(IAddress) this.getTheAddress();
    }
    @Override
    public void setAddress(IAddress address) {
        if (address instanceof EmbeddableAddress) {
            this.setTheAddress((EmbeddableAddress)address);
        }
        else if (address == null) {
            this.setTheAddress(null);
        }
        else {
            throw new ClassCastException("Invalid Implementaion of IAddress.  " +
            "Class must be instance of org.apache.openjpa.persistence.compatible.EmbeddableAddress");
        }
    }

    @Id
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getEmail() {
        return email;
    }
    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getPhone() {
        return phone;
    }
    @Override
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String getType() {
        return type;
    }
    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Embedded
    private  EmbeddableAddress getTheAddress() {
        return theAddress;
    }
    private  void setTheAddress(EmbeddableAddress address) {
        this.theAddress = address;
    }

    @Override
    public String toString() {
        return( "org.apache.openjpa.persistence.compatible.EntityContact: " +
                " id: " + getId() +
                " email: " + getEmail() +
                " phone: " + getPhone() +
                " type: " + getType() +
                " address: " + getAddress() );
    }
}
