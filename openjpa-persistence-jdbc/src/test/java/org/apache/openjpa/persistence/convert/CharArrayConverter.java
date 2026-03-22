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
 * Auto-apply converter for char[] fields.
 * Converts "Doe" to "Smith" (toDB) and "Smith" to "James" (toEntity).
 * Mirrors TCK CharConverter.
 */
@Converter(autoApply = true)
public class CharArrayConverter
        implements AttributeConverter<char[], String> {

    @Override
    public String convertToDatabaseColumn(char[] attribute) {
        String s = new String(attribute);
        if (attribute.length == 3 && attribute[0] == 'D'
                && attribute[1] == 'o' && attribute[2] == 'e') {
            s = "Smith";
        }
        return s;
    }

    @Override
    public char[] convertToEntityAttribute(String dbData) {
        if ("Smith".equals(dbData)) {
            return new char[]{'J', 'a', 'm', 'e', 's'};
        }
        return dbData.toCharArray();
    }
}
