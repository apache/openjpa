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

import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts {@link StaticSettings} to its criteria followed by its users,
 * as in {@code criteria;name=password;name=password}.
 */
@Converter
public class SettingsConverter implements AttributeConverter<Settings, String> {

    @Override
    public String convertToDatabaseColumn(Settings attribute) {
        if (attribute == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder(attribute.getCriteria());
        for (Map.Entry<String, String> user : ((StaticSettings) attribute).getUsers().entrySet()) {
            buf.append(';').append(user.getKey()).append('=').append(user.getValue());
        }
        return buf.toString();
    }

    @Override
    public Settings convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String[] parts = dbData.split(";");
        StaticSettings settings = new StaticSettings();
        settings.setCriteria(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String[] user = parts[i].split("=");
            settings.getUsers().put(user[0], user[1]);
        }
        return settings;
    }
}
