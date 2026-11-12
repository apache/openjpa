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

/**
 * A generic base converter that leaves both types to its subclasses, so the
 * database type is only known from the type arguments a subclass passes.
 */
public abstract class AbstractCodeConverter<X, Y> implements AttributeConverter<X, Y> {

    protected abstract Y toColumn(X attribute);

    protected abstract X toAttribute(Y dbData);

    @Override
    public Y convertToDatabaseColumn(X attribute) {
        return attribute == null ? null : toColumn(attribute);
    }

    @Override
    public X convertToEntityAttribute(Y dbData) {
        return dbData == null ? null : toAttribute(dbData);
    }
}
