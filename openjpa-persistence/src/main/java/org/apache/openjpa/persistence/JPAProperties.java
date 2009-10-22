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
package org.apache.openjpa.persistence;

import java.util.Map;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.kernel.DataCacheRetrieveMode;
import org.apache.openjpa.kernel.DataCacheStoreMode;

/**
 * Enumerates configuration property keys defined in JPA 2.0 Specification.
 * <br>
 * Provides static utility functions to read their values from supplied
 * map of properties.
 * <br>
 * Provides static utility functions to convert them to values that are
 * fit for consumption by OpenJPA implementation.  
 * <br>
 * @author Pinaki Poddar
 * @since 2.0.0
 *
 */
public class JPAProperties {
    public static final String DOT                 = "\\.";
    public static final String PREFIX              = "javax.persistence.";
    public static final String LOCK_TIMEOUT        = PREFIX + "lock.timeout";
    public static final String QUERY_TIMEOUT       = PREFIX + "query.timeout";
    public static final String CACHE_STORE_MODE    = PREFIX + "cache.storeMode";
    public static final String CACHE_RETRIEVE_MODE = PREFIX + "cache.retrieveMode";
    
    public static boolean isValidKey(String key) {
        return key != null && key.startsWith(PREFIX);
    }
    
    /**
     * Gets a bean-style property name from the given key.
     * 
     * 
     * @param key must begin with JPA property prefix <code>javax.persistence</code>
     * 
     * @return null if the key is not a valid JPA property.
     * Otherwise, concatenates each part of the string. Part of string is what appears between DOT character.
     */
    public static String getBeanProperty(String key) {
        if (!isValidKey(key))
            throw new IllegalArgumentException("Invalid JPA property " + key);
        String[] parts = key.split(DOT);
        StringBuilder buf = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            buf.append(StringUtils.capitalize(parts[i]));
        }
        return buf.toString();
    }
    
    public static CacheRetrieveMode getCacheRetrieveMode(Map<String,Object> props) {
        return get(CacheRetrieveMode.class, CacheRetrieveMode.values(), CACHE_RETRIEVE_MODE, props);
    }
    
    static CacheStoreMode getCacheStoreMode(Map<String,Object> props) {
        return get(CacheStoreMode.class, CacheStoreMode.values(), CACHE_STORE_MODE, props);
    }
    
    static <E extends Enum<E>> E get(Class<E> type, String key, Map<String,Object> prop) {
        return get(type, null, key, prop);
    }
    
    /**
     * Convert the given user value to a value consumable by OpenJPA kernel constructs.
     * 
     * @return the same value if the given key is not a valid JPA property key or the value is null.
     */
    public static Object convertValue(String key, Object value) {
        if (value == null)
            return null;
        if (JPAProperties.isValidKey(key)) {
            if (value instanceof CacheRetrieveMode) {
                return DataCacheRetrieveMode.valueOf(value.toString());
            } else if (value instanceof CacheStoreMode) {
                return DataCacheStoreMode.valueOf(value.toString());
            }
        }
        return value;
    }
    
    /**
     * Gets a enum value of the given type from the given properties looking up with the given key.
     * Converts the original value from a String or ordinal number, if necessary.
     * Conversion from an integral number to enum value is only attempted if the allowed enum values
     * are provided as non-null, non-empty array. 
     * 
     * @return null if the key does not exist in the given properties.
     */
    static <E extends Enum<E>> E get(Class<E> type, E[] values, String key, Map<String,Object> prop) {
        if (prop == null)
            return null;
        Object val = prop.get(key);
        if (val == null)
            return null;
        if (type.isInstance(val))
            return (E)val;
        if (val instanceof String) {
            return Enum.valueOf(type, val.toString());
        }
        if (values != null && values.length > 0 && val instanceof Number) {
            return values[((Number)val).intValue()];
        }
        return null; 
    }
}
