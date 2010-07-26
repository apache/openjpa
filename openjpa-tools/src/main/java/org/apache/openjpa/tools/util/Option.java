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
package org.apache.openjpa.tools.util;


/**
 * A simple immutable object represents meta-data about a command option.
 *  
 * @author Pinaki Poddar
 *
 */
public class Option<T> implements Comparable<Option<T>> {
    /**
     * The type of value this option represents.
     */
    private final Class<T> _valueType;
    
    /**
     * Is this option mandatory to be present in the available list of options?
     */
    private final boolean _mandatory;
    
    /**
     * Possible names of this command option.
     * All aliases must start with a dash (<code>-</code>). 
     * The first (zeroth) string is adjudged to be the visible name of the option.
     */
    private final String[] aliases;
    
    /**
     * Does the option require a value?
     */
    private boolean requiresInput;
    
    /**
     * A default value for this option.
     * Only permissible if this option requires a value.
     */
    private T defValue;
    
    /**
     * The original string that was converted to produce value for this option.
     */
    private String originalString;
    
    /**
     * A description String.
     */
    private String _description = "";
    
    public static final String DASH = "-";
    
    /**
     * Create a option with given aliases.
     * 
     * @param mandatory is this option mandatory
     * @param requiresInput does it require a value?
     * @param aliases strings each must start with a dash (<code>-</code>).  
     */
    public Option(Class<T> type, boolean mandatory, boolean requiresInput, String...aliases) {
        if (type == String.class || type == Integer.class || type == Long.class || type == Boolean.class) {
            _valueType = type;
            _mandatory = mandatory;
        } else {
            throw new IllegalArgumentException("Does not know how to convert String value to " 
                      + type.getName());
        }
        if (aliases == null || aliases.length == 0)
            throw new IllegalArgumentException("Can not create command with null or empty aliases");
        for (String alias : aliases) {
            if (!isValidName(alias)) {
                throw new IllegalArgumentException("Invalid alias [" + alias + "]. " +
                        "Aliases must start with - followed by at least one character");
            }
        }
        this.aliases = aliases;
        this.requiresInput = requiresInput;
    }
    
    /**
     * Is this option mandatory?
     */
    public boolean isMandatory() {
        return _mandatory;
    }
    
    /**
     * Affirms if the given string can be a valid option name.
     * An option name always starts with dash and must be followed by at least one character.
     */
    public static boolean isValidName(String s) {
        return s != null && s.startsWith(DASH) && s.length() > 1;
    }
    
    /**
     * Gets the first alias as the name.
     */
    public String getName() {
        return aliases[0];
    }
    
    /**
     * Sets the default value for this option.
     * 
     * @param v a default value.
     * 
     * @return this command itself.
     * 
     * @exception IllegalStateException if this option does not require a value.
     */
    public Option<T> setDefault(String s) {
        if (!requiresInput)
            throw new IllegalStateException(this + " does not require a value. Can not set default value [" + s + "]");
        defValue = (T)convert(s);
        return this;
    }

    
    /**
     * Sets description for this option.
     * @param desc a non-null string
     */
    public Option<T> setDescription(String desc) {
        _description = desc == null ? "" : desc;
        return this;
    }
    
    public String getDescription() {
        return _description;
    }
    
    /**
     * Affirms if the given name any of the aliases.
     * @param name
     * @return true if the name matches (case-sensitively) with any of the aliases.
     */
    public boolean match(String name) {
        for (String alias : aliases) {
            if (name.equals(alias))
                return true;
        }
        return false;
    }
    
    /**
     * Affirms if this option requires a value.
     */
    public boolean requiresInput() {
        return requiresInput;
    }
    
    /**
     * Gets the default value of this option.
     * 
     * @return the default value. null if no default value has been set.
     */
    public T getDefaultValue() {
        return defValue;
    }
    
    public String getOriginalString() {
        return originalString;
    }
    
    public Class<?> getValueType() {
        return _valueType;
    }
    
    public String toString() {
        return getName();
    }
    
    /**
     * Converts the string to a value.
     * 
     */
    public Object convert(String v) {
        originalString = v;
        if (_valueType == String.class)
            return v;
        if (_valueType == Integer.class) {
            return Integer.parseInt(v);
        }
        if (_valueType == Long.class) {
            return Long.parseLong(v);
        }
        if (_valueType == Boolean.class) {
            return Boolean.parseBoolean(v);
        }
        return v;
    }

    @Override
    public int compareTo(Option<T> o) {
        if (isMandatory() && !o.isMandatory()) {
            return -1;
        } else if (!isMandatory() && o.isMandatory()) {
            return 1;
        } else {
            return getName().compareTo(o.getName());
        }
    }
}
