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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the whole list to one string, so the attribute type the converter
 * declares is the collection itself, not its element.
 */
@Converter
public class ItemListConverter implements AttributeConverter<List<Item>, String> {

    @Override
    public String convertToDatabaseColumn(List<Item> attribute) {
        if (attribute == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Item item : attribute) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(item.getName());
        }
        return sb.toString();
    }

    @Override
    public List<Item> convertToEntityAttribute(String dbData) {
        List<Item> items = new ArrayList<>();
        if (dbData == null || dbData.isEmpty()) {
            return items;
        }
        for (String name : dbData.split(",")) {
            items.add(new Item(name));
        }
        return items;
    }
}
