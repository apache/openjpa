/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.conf;

import java.util.*;

import org.apache.openjpa.lib.conf.*;
import org.apache.openjpa.lib.util.concurrent.*;
import org.apache.openjpa.util.*;

/**
 * <p>A cache of compiled queries.</p>
 *
 * @author Abe White
 * @since 0.9.6 (also existed in prior versions of Kodo)
 * @nojavadoc
 */
public class QueryCompilationCacheValue
    extends PluginValue {

    /**
     * Query compilation cache configuration property key.
     */
    private static final String KEY = "QueryCompilationCache";

    public static final String[] ALIASES = {
        "true", CacheMap.class.getName(),
        "all", ConcurrentHashMap.class.getName(),
        "false", null,
    };

    private final OpenJPAConfiguration _conf;

    public QueryCompilationCacheValue(OpenJPAConfiguration conf) {
        super(KEY, true);
        setAliases(ALIASES);
        setDefault(ALIASES[0]);
        setClassName(ALIASES[1]);
        setInstantiatingGetter("this.instantiate");
        setScope(getClass());
        _conf = conf;
    }

    /**
     * Instantiate internal map.
     */
    public void instantiate() {
        if (get() == null)
            instantiate(Map.class, _conf, true);
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
