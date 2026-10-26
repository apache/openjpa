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
package org.apache.openjpa.persistence.conversion;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

import org.apache.openjpa.persistence.PersistentCollection;
import org.apache.openjpa.persistence.jdbc.ContainerTable;

/**
 * Converted attributes whose values the data cache must not share between
 * persistence contexts.
 */
@Entity
public class ConvertedCacheEntity {

    @Id
    private long id;

    @PersistentCollection(elementType = Item.class, fetch = FetchType.EAGER)
    @ContainerTable(name = "CONV_CACHE_ELEMENTS")
    @Convert(converter = ItemConverter.class)
    private List<Item> elements = new ArrayList<>();

    @Convert(converter = LabelConverter.class)
    private Label label;

    @Convert(converter = BufferBytesConverter.class)
    private Buffer bytes;

    @Convert(converter = BufferDateConverter.class)
    private Buffer date;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Item> getElements() {
        return elements;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public Buffer getBytes() {
        return bytes;
    }

    public void setBytes(Buffer bytes) {
        this.bytes = bytes;
    }

    public Buffer getDate() {
        return date;
    }

    public void setDate(Buffer date) {
        this.date = date;
    }
}
