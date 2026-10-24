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

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts the whole map to one string of key=name pairs, so the attribute
 * type the converter declares is the map itself.
 */
@Converter
public class ItemMapConverter implements AttributeConverter<Map<String, Item>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, Item> attribute) {
        if (attribute == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Item> e : attribute.entrySet()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(e.getKey()).append('=').append(e.getValue().getName());
        }
        return sb.toString();
    }

    @Override
    public Map<String, Item> convertToEntityAttribute(String dbData) {
        Map<String, Item> items = new LinkedHashMap<>();
        if (dbData == null || dbData.isEmpty()) {
            return items;
        }
        for (String pair : dbData.split(",")) {
            int eq = pair.indexOf('=');
            items.put(pair.substring(0, eq), new Item(pair.substring(eq + 1)));
        }
        return items;
    }
}
