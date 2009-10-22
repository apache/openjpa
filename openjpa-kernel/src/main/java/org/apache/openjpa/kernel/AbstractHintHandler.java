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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.Reflection;
import org.apache.openjpa.lib.util.Localizer;

/**
 * Default hint handler abstract base class.
 *
 * @since 2.0.0
 * @nojavadoc
 */
public abstract class AbstractHintHandler implements Serializable {

    private static final Localizer _loc = Localizer
        .forPackage(AbstractHintHandler.class);

    protected static final String DOT = ".";
    protected static final String BLANK = "";

    protected static final String PREFIX_OPENJPA = "openjpa.";
    protected static final String PREFIX_JDBC = PREFIX_OPENJPA + "jdbc.";

    protected FetchConfiguration _fConfig;

    /**
     * Constructor; supply delegate.
     */
    public AbstractHintHandler(FetchConfiguration fConfig) {
        _fConfig = fConfig;
    }

    protected abstract boolean setHintInternal(String hintName, Object value,
        boolean validateThrowException);

    public boolean setHint(String hintName, Object value, boolean validateThrowException) {
        String key = hintToKey(hintName);
        boolean valueSet = !hintName.equals(key);
        if (hasPrecedent(hintName)) {
            try {
                valueSet |= setHintInternal(key, value, validateThrowException);
            } catch (RuntimeException rte) {
                if (validateThrowException) {
                    if (rte instanceof IllegalArgumentException)
                        throw rte;
                    else if (rte instanceof ClassCastException)
                        throw new IllegalArgumentException(_loc.get("bad-hint-value", key, value, rte.getMessage())
                            .getMessage());
                    else {
                        handleException(rte);
                    }
                } else
                    _fConfig.getContext().getConfiguration().getConfigurationLog().warn(
                            _loc.get("bad-hint-value", key, value, rte.getMessage()));
            }
        } else {
            valueSet = true;
        }
        return valueSet;
    }
    
    protected String hintToKey(String key) {
        return key;
    }
    
    protected boolean hasPrecedent(String key) {
        return true;
    }

    protected void handleException(RuntimeException e) {
        throw e;
    }
    
    protected final boolean hintToSetter(Object target, String k, 
        Object value) {
        if (target == null || k == null)
            return false;
        // remove key prefix as the source of property name
        k = getSuffixOf(k);
        Method setter = Reflection.findSetter(target.getClass(), k, true);
        Class paramType = setter.getParameterTypes()[0];
        if (Enum.class.isAssignableFrom(paramType) && value instanceof String) {
            // to accomodate alias name input in relationship with enum values 
            String strValue = ((String) value).toUpperCase().replace('-', '_');
            value = Enum.valueOf(paramType, strValue);
        }
        Filters.hintToSetter(target, k, value);
        return true;
    }

    protected static String getPrefixOf(String key) {
        int firstDot = key == null ? -1 : key.indexOf(DOT);
        return (firstDot != -1) ? key.substring(0, firstDot) : key;
    }

    protected static String getSuffixOf(String key) {
        int lastDot = key == null ? -1 : key.lastIndexOf(DOT);
        return (lastDot != -1) ? key.substring(lastDot + 1) : key;
    }
}
