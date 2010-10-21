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
package org.apache.openjpa.instrumentation.jconsole;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.openjpa.datacache.CacheStatistics;

/**
 * This class is a wrapper around org.apache.openjpa.datacache.CacheStatistics along with the stringified types that a
 * given cache knows about.
 */
public class DataCacheStatistic {
    CacheStatistics _stats;
    Map<String, Boolean> _types;
    List<String> _master;
    List<String> _enabledTypes;
    List<String> _disabledTypes;

    public DataCacheStatistic(CacheStatistics stats, Map<String, Boolean> types) {
        _stats = stats;
        _types = types;
        _enabledTypes = new ArrayList<String>();
        _disabledTypes = new ArrayList<String>();
        _master = new ArrayList<String>();
        for (Entry<String, Boolean> type : types.entrySet()) {
            boolean enabled = type.getValue();
            String name = type.getKey();
            if (enabled) {
                _enabledTypes.add(name);
            } else {
                _disabledTypes.add(name);
            }
            _master.add(name);
        }
    }

    public long getReads(String cls) {
        return _stats.getReadCount(cls);
    }

    public long getHits(String cls) {
        return _stats.getHitCount(cls);
    }

    public long getWrites(String cls) {
        return _stats.getWriteCount(cls);
    }

    public List<String> getEnabledTypes() {
        return _enabledTypes;
    }

    public List<String> getDisabledTypes() {
        return _disabledTypes;
    }

    public List<String> getAllTypes() {
        return _master;
    }

    public int getNumTypes() {
        return _disabledTypes.size() + _enabledTypes.size();
    }
}
