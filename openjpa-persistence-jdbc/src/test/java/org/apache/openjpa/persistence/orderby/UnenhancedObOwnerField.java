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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
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
 * Entity with field access and ElementCollection of embeddable
 * Address ordered by nested embeddable dot notation: "zipcode.zip DESC".
 * Mirrors TCK A2 entity (field access).
 * Note: uses field name "zipcode" (lowercase c) not property name "zipCode".
 */
@Entity
@Table(name = "UOB_OWNER_FIELD")
@Access(AccessType.FIELD)
public class UnenhancedObOwnerField implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    protected String id;

    @Column(name = "NAME")
    protected String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "UOB_OWNER_FIELD_ADDR",
        joinColumns = @JoinColumn(name = "OWNER_ID"))
    @OrderBy("zipcode.zip DESC")
    protected List<UnenhancedObAddress> lAddress = new ArrayList<>();

    public UnenhancedObOwnerField() {
    }

    public UnenhancedObOwnerField(String id, String name,
            List<UnenhancedObAddress> addr) {
        this.id = id;
        this.name = name;
        this.lAddress = addr;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UnenhancedObAddress> getAddressList() {
        return lAddress;
    }

    public void setAddressList(List<UnenhancedObAddress> addr) {
        this.lAddress = addr;
    }
}
