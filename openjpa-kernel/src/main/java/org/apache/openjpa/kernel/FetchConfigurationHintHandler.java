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
package org.apache.openjpa.kernel;

import java.util.HashMap;
import java.util.Map;

/**
 * Fetch configuration hint handler. Handles openjpa.* and openjpa.jdbc.* hints.
 *
 * @since 2.0.0
 * @nojavadoc
 */
@SuppressWarnings("serial")
public class FetchConfigurationHintHandler extends AbstractHintHandler {

    protected static final Map<String, String> hintsMap = new HashMap<String, String>();

    static {
        // Initialize hint to property name mapping.
        hintsMap.put(PREFIX_JDBC + "TransactionIsolation", "Isolation");
    }

    /**
     * Constructor; supply delegate.
     */
    public FetchConfigurationHintHandler(FetchConfigurationImpl fConfig) {
        super(fConfig);
    }

    public boolean setHintInternal(String hintName, Object value, boolean validateThrowException) {
        boolean valueSet = false;
        String longPrefix = hintName.substring(0, hintName.lastIndexOf(DOT) + 1);
        if ((longPrefix.equals(PREFIX_JDBC) || longPrefix.equals(PREFIX_OPENJPA))) {
            valueSet = hintToSetter(_fConfig, hintToPropName(hintName), value);
        } else {
            valueSet = true;
        }
        return valueSet;
    }

    private String hintToPropName(String hintName) {
        String propName = hintsMap.get(hintName);
        if (propName == null) {
            propName = hintName;
        }
        return propName;
    }
}
