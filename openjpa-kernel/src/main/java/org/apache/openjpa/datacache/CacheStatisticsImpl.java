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
package org.apache.openjpa.datacache;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.util.OpenJPAId;

/**
 * The default CacheStatistics(SPI) implementation.
 */
public class CacheStatisticsImpl implements CacheStatisticsSPI {
    private static final long serialVersionUID = 9014495759588003166L;
    private static final int ARRAY_SIZE = 3;
    private long[] astat = new long[ARRAY_SIZE];
    private long[] stat = new long[ARRAY_SIZE];
    private Map<Class<?>, long[]> stats = new HashMap<Class<?>, long[]>();
    private Map<Class<?>, long[]> astats = new HashMap<Class<?>, long[]>();
    private Date start = new Date();
    private Date since = new Date();
    private boolean enabled = false;

    private static final int READ = 0;
    private static final int HIT = 1;
    private static final int WRITE = 2;

    public long getReadCount() {
        return stat[READ];
    }

    public long getHitCount() {
        return stat[HIT];
    }

    public long getWriteCount() {
        return stat[WRITE];
    }

    public long getTotalReadCount() {
        return astat[READ];
    }

    public long getTotalHitCount() {
        return astat[HIT];
    }

    public long getTotalWriteCount() {
        return astat[WRITE];
    }

    public long getReadCount(Class<?> c) {
        return getCount(stats, c, READ);
    }

    public long getHitCount(Class<?> c) {
        return getCount(stats, c, HIT);
    }

    public long getWriteCount(Class<?> c) {
        return getCount(stats, c, WRITE);
    }

    public long getTotalReadCount(Class<?> c) {
        return getCount(astats, c, READ);
    }

    public long getTotalHitCount(Class<?> c) {
        return getCount(astats, c, HIT);
    }

    public long getTotalWriteCount(Class<?> c) {
        return getCount(astats, c, WRITE);
    }

    public Date since() {
        return since;
    }

    public Date start() {
        return start;
    }

    public void reset() {
        stat = new long[ARRAY_SIZE];
        stats.clear();
        since = new Date();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<Class<?>> classNames() {
        return astats.keySet();
    }
    
    /**
     * SPI implementation
     */
    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public void newGet(Class<?> cls, boolean hit) {
        if (!enabled) {
            return;
        }
        cls = (cls == null) ? Object.class : cls;
        addSample(cls, READ);
        if (hit) {
            addSample(cls, HIT);
        }
    }

    public void newGet(Object oid, boolean hit) {
        if (!enabled) {
            return;
        }
        if (oid instanceof OpenJPAId) {
            newGet(((OpenJPAId) oid).getType(), hit);
        }
    }

    public void newPut(Class<?> cls) {
        if (!enabled) {
            return;
        }
        cls = (cls == null) ? Object.class : cls;
        addSample(cls, WRITE);
    }

    public void newPut(Object oid) {
        if (!enabled) {
            return;
        }
        if (oid instanceof OpenJPAId) {
            newPut(((OpenJPAId) oid).getType());
        }
    }

    /**
     *  Private worker methods.
     */
    private void addSample(Class<?> c, int index) {
        stat[index]++;
        astat[index]++;
        addSample(stats, c, index);
        addSample(astats, c, index);
    }

    private void addSample(Map<Class<?>, long[]> target, Class<?> c, int index) {
        long[] row = target.get(c);
        if (row == null) {
            row = new long[ARRAY_SIZE];
        }
        row[index]++;
        target.put(c, row);
    }

    private long getCount(Map<Class<?>, long[]> target, Class<?> c, int index) {
        long[] row = target.get(c);
        return (row == null) ? 0 : row[index];
    }
}
