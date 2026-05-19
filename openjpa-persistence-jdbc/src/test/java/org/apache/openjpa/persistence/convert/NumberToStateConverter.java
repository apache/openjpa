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
 * Converts Integer to state abbreviation String.
 * Throws RuntimeException for -1 (toDB) and "-2" (toEntity).
 * Mirrors TCK NumberToStateConverter.
 */
@Converter
public class NumberToStateConverter
        implements AttributeConverter<Integer, String> {

    @Override
    public String convertToDatabaseColumn(Integer attribute) {
        if (attribute.equals(1)) {
            return "MA";
        } else if (attribute.equals(2)) {
            return "CA";
        } else if (attribute.equals(-1)) {
            throw new RuntimeException(
                "Exception was thrown from convertToDatabaseColumn");
        }
        return attribute.toString();
    }

    @Override
    public Integer convertToEntityAttribute(String dbData) {
        if (dbData.equals("MA")) {
            return 1;
        } else if (dbData.equals("CA")) {
            return 2;
        } else if (dbData.equals("-2")) {
            throw new RuntimeException(
                "Exception was thrown from convertToEntityAttribute");
        }
        return Integer.valueOf(dbData);
    }
}
