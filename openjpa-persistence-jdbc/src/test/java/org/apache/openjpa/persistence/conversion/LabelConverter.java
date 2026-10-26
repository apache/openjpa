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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts a {@link Label} to its name. A test can make either direction
 * throw.
 */
@Converter
public class LabelConverter implements AttributeConverter<Label, String> {

    static volatile boolean failToDatabase;
    static volatile boolean failToEntity;

    @Override
    public String convertToDatabaseColumn(Label attribute) {
        if (failToDatabase) {
            throw new IllegalStateException("convertToDatabaseColumn");
        }
        return attribute == null ? null : attribute.getName();
    }

    @Override
    public Label convertToEntityAttribute(String dbData) {
        if (failToEntity) {
            throw new IllegalStateException("convertToEntityAttribute");
        }
        return dbData == null ? null : new Label(dbData);
    }
}
