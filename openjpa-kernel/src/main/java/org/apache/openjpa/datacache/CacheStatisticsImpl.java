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
import java.util.Map.Entry;
import java.util.Set;

import org.apache.openjpa.util.OpenJPAId;

/**
 * The default CacheStatistics(SPI) implementation.
 */
public class CacheStatisticsImpl implements CacheStatisticsSPI {
    private static final long serialVersionUID = 9014495759588003166L;
    private static final int ARRAY_SIZE = 3;
    private long[] totalStat = new long[ARRAY_SIZE];
    private long[] stat = new long[ARRAY_SIZE];
    private Map<String, long[]> stats = new HashMap<>();
    private Map<String, long[]> totalStats = new HashMap<>();

    private Date start = new Date();
    private Date since = new Date();
    private boolean enabled = false;

    private static final int READ = 0;
    private static final int HIT = 1;
    private static final int WRITE = 2;

    @Override
    public long getReadCount() {
        return stat[READ];
    }

    @Override
    public long getHitCount() {
        return stat[HIT];
    }

    @Override
    public long getWriteCount() {
        return stat[WRITE];
    }

    @Override
    public long getTotalReadCount() {
        return totalStat[READ];
    }

    @Override
    public long getTotalHitCount() {
        return totalStat[HIT];
    }

    @Override
    public long getTotalWriteCount() {
        return totalStat[WRITE];
    }

    @Override
    public long getReadCount(Class<?> c) {
        return getReadCount(c.getName());
    }

    @Override
    public long getReadCount(String str){
        return getCount(stats, str, READ);
    }

    @Override
    public long getHitCount(Class<?> c) {
        return getHitCount(c.getName());
    }

    @Override
    public long getHitCount(String str) {
        return getCount(stats, str, HIT);
    }

    @Override
    public long getWriteCount(Class<?> c) {
        return getWriteCount(c.getName());
    }
    @Override
    public long getWriteCount(String str) {
        return getCount(stats, str, WRITE);
    }

    @Override
    public long getTotalReadCount(Class<?> c) {
        return getTotalReadCount(c.getName());
    }

    @Override
    public long getTotalReadCount(String str) {
        return getCount(totalStats, str, READ);
    }

    @Override
    public long getTotalHitCount(Class<?> c) {
        return getTotalHitCount(c.getName());
    }

    @Override
    public long getTotalHitCount(String str) {
        return getCount(totalStats, str, HIT);
    }

    @Override
    public long getTotalWriteCount(Class<?> c) {
        return getCount(totalStats, c.getName(), WRITE);
    }

    @Override
    public long getTotalWriteCount(String str) {
        return getCount(totalStats, str, WRITE);
    }

    @Override
    public Date since() {
        return since;
    }

    @Override
    public Date start() {
        return start;
    }

    @Override
    public void reset() {
        stat = new long[ARRAY_SIZE];
        stats.clear();
        since = new Date();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Set<String> classNames() {
        return totalStats.keySet();
    }

    @Override
    public Map<String, long[]> toMap() {
        Map<String, long[]> res = new HashMap<>();
        for(Entry<String, long[]> s  : stats.entrySet()){
            res.put(s.getKey(), s.getValue());
        }
        return res;
    }

    /**
     * SPI implementation
     */
    @Override
    public void enable() {
        enabled = true;
    }

    @Override
    public void disable() {
        enabled = false;
    }

    @Override
    public void newGet(Class<?> cls, boolean hit) {
        if (!enabled) {
            return;
        }
        cls = (cls == null) ? Object.class : cls;
        String clsName = cls.getName();
        addSample(clsName, READ);
        if (hit) {
            addSample(clsName, HIT);
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

    @Override
    public void newPut(Class<?> cls) {
        if (!enabled) {
            return;
        }
        cls = (cls == null) ? Object.class : cls;
        addSample(cls.getName(), WRITE);
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
    private void addSample(String c, int index) {
        stat[index]++;
        totalStat[index]++;
        addSample(stats, c, index);
        addSample(totalStats, c, index);
    }

    private void addSample(Map<String, long[]> target, String c, int index) {
        long[] row = target.get(c);
        if (row == null) {
            row = new long[ARRAY_SIZE];
        }
        row[index]++;
        target.put(c, row);
    }

    private long getCount(Map<String, long[]> target, String c, int index) {
        long[] row = target.get(c);
        return (row == null) ? 0 : row[index];
    }
}
