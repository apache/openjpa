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
package org.apache.openjpa.jdbc.conf;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.util.CacheMap;


/**
 * A cache of sql queries.
 *
 * @since 1.2
 * @nojavadoc
 */
public class QuerySQLCacheValue
    extends PluginValue {

    public static final String[] ALIASES = {
        "true", CacheMap.class.getName(),
        "all", ConcurrentHashMap.class.getName(),
        "false", null,
    };
    
    public QuerySQLCacheValue(String prop) {
        super(prop, true); 
        setAliases(ALIASES);
        setDefault(ALIASES[0]);
        setClassName(ALIASES[1]);
    }
    
    public boolean isSQLCacheOn() {
        if (getClassName() == null) 
            return false;
        return true;
    }
    
    public Object newInstance() {
        // make sure map handles concurrency
        String clsName = getClassName();
        if (clsName == null)
            return null;
        Map map = null;

        try {
            // Use the "OpenJPA" classloader first...
            map = (Map) Configurations.newInstance(clsName, this.getClass()
                    .getClassLoader());
        } catch (Exception e) {
            // If the "OpenJPA" classloader fails, then try the classloader
            // that was used to load java.util.Map...
            map = (Map) Configurations.newInstance(clsName,
                    Map.class.getClassLoader());
        }
        if (map != null
                && !(map instanceof Hashtable)
                && !(map instanceof CacheMap)
                && !(map instanceof 
                        org.apache.openjpa.lib.util.concurrent.ConcurrentMap)
                && !(map instanceof java.util.concurrent.ConcurrentMap))
            map = Collections.synchronizedMap(map);
        return map;
    }

}
