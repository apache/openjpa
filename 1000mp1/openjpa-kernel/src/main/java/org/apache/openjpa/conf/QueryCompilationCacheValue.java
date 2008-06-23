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
package org.apache.openjpa.conf;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.lib.util.concurrent.ConcurrentMap;
import org.apache.openjpa.lib.util.concurrent.ConcurrentHashMap;
import org.apache.openjpa.util.CacheMap;

/**
 * A cache of compiled queries.
 *
 * @author Abe White
 * @since 0.9.6 (also existed in prior versions of Kodo)
 * @nojavadoc
 */
public class QueryCompilationCacheValue
    extends PluginValue {

    public static final String[] ALIASES = {
        "true", CacheMap.class.getName(),
        "all", ConcurrentHashMap.class.getName(),
        "false", null,
    };

    public QueryCompilationCacheValue(String prop) {
        super(prop, true);
        setAliases(ALIASES);
        setDefault(ALIASES[0]);
        setClassName(ALIASES[1]);
    }

    public Object newInstance(String clsName, Class type,
        Configuration conf, boolean fatal) {
        // make sure map handles concurrency
        Map map = (Map) super.newInstance(clsName, type, conf, fatal);
        if (map != null && !(map instanceof Hashtable)
            && !(map instanceof CacheMap)
            && !(map instanceof ConcurrentMap))
            map = Collections.synchronizedMap(map);
        return map;
	}
}
