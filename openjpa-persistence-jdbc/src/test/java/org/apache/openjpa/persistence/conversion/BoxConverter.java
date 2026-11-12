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
 * An auto applied converter whose attribute type is parameterized.
 */
@Converter(autoApply = true)
public class BoxConverter implements AttributeConverter<Box<String>, String> {

    @Override
    public String convertToDatabaseColumn(Box<String> attribute) {
        return attribute == null ? null : attribute.getContent();
    }

    @Override
    public Box<String> convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new Box<>(dbData);
    }
}
