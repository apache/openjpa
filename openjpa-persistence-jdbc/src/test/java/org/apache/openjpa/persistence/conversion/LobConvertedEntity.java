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
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * Converted LOB attributes. The kind of LOB follows the converter's database
 * type, not the attribute type. {@code plainBytes} is converted to bytes
 * without being a LOB; the last fields have no converter.
 */
@Entity
public class LobConvertedEntity {

    @Id
    private long id;

    @Lob
    @Convert(converter = ItemListConverter.class)
    private List<Item> items = new ArrayList<>();

    @Lob
    @Convert(converter = ItemConverter.class)
    private Item item;

    @ElementCollection
    @Lob
    @Convert(converter = StringBytesConverter.class)
    private List<String> elements = new ArrayList<>();

    @Lob
    @Convert(converter = StringBytesConverter.class)
    private String bytes;

    @Convert(converter = StringBytesConverter.class)
    private String plainBytes;

    @Lob
    private String text;

    @Lob
    private byte[] data;

    @Lob
    private Item serial;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Item> getItems() {
        return items;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<String> getElements() {
        return elements;
    }

    public String getBytes() {
        return bytes;
    }

    public void setBytes(String bytes) {
        this.bytes = bytes;
    }

    public String getPlainBytes() {
        return plainBytes;
    }

    public void setPlainBytes(String plainBytes) {
        this.plainBytes = plainBytes;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Item getSerial() {
        return serial;
    }

    public void setSerial(Item serial) {
        this.serial = serial;
    }
}
