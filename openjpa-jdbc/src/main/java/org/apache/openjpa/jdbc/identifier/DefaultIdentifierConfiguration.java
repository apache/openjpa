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
package org.apache.openjpa.jdbc.identifier;

import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.lib.identifier.IdentifierConfiguration;
import org.apache.openjpa.lib.identifier.IdentifierRule;
import org.apache.openjpa.lib.identifier.IdentifierUtil;

public class DefaultIdentifierConfiguration implements IdentifierConfiguration {

    private DBIdentifierRule normalizingRule = new DBIdentifierRule();
    private Map<String, IdentifierRule> normalizingRules = new HashMap<>();
    private final String conversionKey = getLeadingDelimiter() + getIdentifierDelimiter() + getTrailingDelimiter();

    public DefaultIdentifierConfiguration() {
        normalizingRules.put(IdentifierRule.DEFAULT_RULE, normalizingRule);
    }

    @Override
    public boolean delimitAll() {
        return false;
    }

    @Override
    public IdentifierRule getDefaultIdentifierRule() {
        return normalizingRule;
    }

    @Override
    public String getDelimitedCase() {
        return IdentifierUtil.CASE_PRESERVE;
    }

    @Override
    public String getSchemaCase() {
        return IdentifierUtil.CASE_PRESERVE;
    }

    @Override
    public String getLeadingDelimiter() {
        return IdentifierUtil.DOUBLE_QUOTE;
    }

    @Override
    public String getIdentifierDelimiter() {
        return IdentifierUtil.DOT;
    }

    @Override
    public String getIdentifierConcatenator() {
        return IdentifierUtil.UNDERSCORE;
    }

    @Override
    public <T> IdentifierRule getIdentifierRule(T t) {
        return normalizingRule;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, IdentifierRule> getIdentifierRules() {
        return normalizingRules;
    }

    @Override
    public String getTrailingDelimiter() {
        return IdentifierUtil.DOUBLE_QUOTE;
    }

    @Override
    public boolean getSupportsDelimitedIdentifiers() {
        return true;
    }

    @Override
    public String getConversionKey() {
        return conversionKey;
    }
}
