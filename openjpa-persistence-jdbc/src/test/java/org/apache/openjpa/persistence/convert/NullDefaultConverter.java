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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter that substitutes defaults for null in both directions, as the
 * specification allows. A null attribute is stored as {@link #DB_DEFAULT},
 * a null column value is read back as {@link #ENTITY_DEFAULT}, and the
 * attribute value {@link #ERASE} is stored as a null column value.
 */
@Converter
public class NullDefaultConverter
        implements AttributeConverter<String, String> {

    public static final String DB_DEFAULT = "DB_DEFAULT";
    public static final String ENTITY_DEFAULT = "ENTITY_DEFAULT";
    public static final String ERASE = "ERASE";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null)
            return DB_DEFAULT;
        if (ERASE.equals(attribute))
            return null;
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null)
            return ENTITY_DEFAULT;
        if (DB_DEFAULT.equals(dbData))
            return null;
        return dbData;
    }
}
