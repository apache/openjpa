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
package org.apache.openjpa.persistence.convert;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Entity with an element collection of embeddables and a converter
 * override on an attribute of the collection's elements.
 */
@Entity
@Table(name = "CONV_EMBED_COLL")
public class ConvertEmbedCollectionEntity {

    @Id
    private String id;

    @ElementCollection
    @CollectionTable(name = "CONV_EMBED_COLL_ADDR",
        joinColumns = @JoinColumn(name = "ENTITY_ID"))
    @Converts(value = {
        @Convert(attributeName = "street", converter = DotConverter.class)
    })
    private List<ConvertAddress> addresses = new ArrayList<>();

    public ConvertEmbedCollectionEntity() {
    }

    public ConvertEmbedCollectionEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<ConvertAddress> getAddresses() {
        return addresses;
    }
}
