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
package org.apache.openjpa.lib.identifier;

import java.util.Map;

/**
 * The IdentifierConfiguration interface.  Implementers of this interface supply
 * naming configuration information to consumers of names/identifiers.
 */
public interface IdentifierConfiguration {

    /**
     * Returns the leading delimiter value to use when delimiting a name.
     */
    String getLeadingDelimiter();

    /**
     * Returns the trailing delimiter value to use when delimiting a name.
     */
    String getTrailingDelimiter();

    /**
     * Returns true if global name delimiting is enabled.
     */
    boolean delimitAll();

    /**
     * Returns true if delimiting is supported
     */
    boolean getSupportsDelimitedIdentifiers();

    /**
     * Returns the value used to concatenate multiple names together.
     * For example: "_" used in TABLE1_TABLE2
     */
    String getIdentifierConcatenator();

    /**
     * Returns the value used to delimit between individual names.
     * For example: "." used in MYSCHEMA.MYTABLE
     */
    String getIdentifierDelimiter();

    /**
     * Gets the default naming rule
     */
    IdentifierRule getDefaultIdentifierRule();

    /**
     * Returns all naming rules
     */
    <T> Map<T, IdentifierRule> getIdentifierRules();

    /**
     * Returns a naming rule or null if the rule is
     * not found.
     */
    <T> IdentifierRule getIdentifierRule(T t);

    /**
     * Returns the case that is used when delimiting.
     * @return upper, lower, or preserve
     */
    String getDelimitedCase();

    /**
     * Returns the case that is used when delimiters are not used.
     * @return upper, lower, or preserve
     */
    String getSchemaCase();

    /**
     * Returns a key that can be used to determine whether conversion
     * should take place.  Id configurations should create a key unique
     * to their configuration.  The typical key is:
     * leading delimiter (") + name separator(.) + trailing delimiter(")
     */
    String getConversionKey();
}
