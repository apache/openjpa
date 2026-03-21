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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Entity with property access and ElementCollection of embeddable
 * Address ordered by nested embeddable dot notation: "zipCode.zip DESC".
 * Mirrors TCK A entity (property access).
 */
@Entity
@Table(name = "UOB_OWNER_PROP")
public class UnenhancedObOwnerProperty implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String name;
    protected List<UnenhancedObAddress> addressList = new ArrayList<>();

    public UnenhancedObOwnerProperty() {
    }

    public UnenhancedObOwnerProperty(String id, String name,
            List<UnenhancedObAddress> addr) {
        this.id = id;
        this.name = name;
        this.addressList = addr;
    }

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "NAME")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "UOB_OWNER_PROP_ADDR",
        joinColumns = @JoinColumn(name = "OWNER_ID"))
    @OrderBy("zipCode.zip DESC")
    public List<UnenhancedObAddress> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<UnenhancedObAddress> addr) {
        this.addressList = addr;
    }
}
