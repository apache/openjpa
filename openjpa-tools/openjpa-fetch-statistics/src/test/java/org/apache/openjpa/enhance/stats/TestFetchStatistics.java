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

package org.apache.openjpa.enhance.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for Open JPA Fetch Statistic.
 */
public class TestFetchStatistics {

    @BeforeEach
    void setUp() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("fetchStatisticPU");
        emf.createEntityManager().close();
        FetchStatsCollector.clear();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void fieldAccess() {
        FetchStatsCollector.clear();

        AEntity aEntity = new AEntity(1, "t", "d", null);


        Set<String> res = FetchStatsCollector.getStatistics();
        assertTrue(res.contains("org.apache.openjpa.enhance.stats.AEntity.name"));
        assertTrue(res.contains("org.apache.openjpa.enhance.stats.AEntity.desc"));

        assertEquals("td", aEntity.getFullInfo());
        res = FetchStatsCollector.getStatistics();
        assertFalse(res.contains("org.apache.openjpa.enhance.stats.AEntity.name"));
        assertFalse(res.contains("org.apache.openjpa.enhance.stats.AEntity.desc"));

    }

    @Test
    void embeddedEntityFieldAccess() {
        AEntity aEntity = new AEntity(1, "t1", "d1", null);

        assertEquals("extra d1", aEntity.getExtraInfo());

        Set<String> res = FetchStatsCollector.getStatistics();

        assertFalse(res.contains("org.apache.openjpa.enhance.stats.AEntity.extraInfo"));
    }

    @Test
    void propertyFieldMixedAccess() {
        FetchStatsCollector.clear();

        BEntity b = new BEntity(2, "t2", "d2");

        Set<String> res = FetchStatsCollector.getStatistics();

        assertTrue(res.contains("org.apache.openjpa.enhance.stats.BEntity.name"), res.toString());
        assertTrue(res.contains("org.apache.openjpa.enhance.stats.BEntity.bool"), res.toString());

        b.getName();
        b.isBool();

        res = FetchStatsCollector.getStatistics();

        assertFalse(res.contains("org.apache.openjpa.enhance.stats.BEntity.name"));
        assertFalse(res.contains("org.apache.openjpa.enhance.stats.BEntity.bool"));

    }

    @Test
    void mixedAccess() {
        BEntity e = new BEntity(1, "t1", "d1");


        Set<String> res = FetchStatsCollector.getStatistics();

        assertTrue(res.contains("org.apache.openjpa.enhance.stats.BEntity.name"));
        assertTrue(res.contains("org.apache.openjpa.enhance.stats.BEntity.desc"));

        e.getName();
        e.getCustomDesc();

        res = FetchStatsCollector.getStatistics();

        assertFalse(res.contains("org.apache.openjpa.enhance.stats.BEntity.name"));
        assertFalse(res.contains("org.apache.openjpa.enhance.stats.BEntity.desc"));

    }

    @Test
    void fieldAccessThroughRelationship() {
        BEntity bf1 = new BEntity(1, "t1", "d1");
        AEntity af3 = new AEntity(3, "t3", "d3", bf1);

        assertEquals("t1", af3.getReferredBEntityName());
        af3.isChecked();

        Set<String> res = FetchStatsCollector.getStatistics();

        assertFalse(res.contains("org.apache.openjpa.enhance.stats.AEntity.checked"));
    }

    @Test
    void propertyAccessThroughInheritance() {
        FetchStatsCollector.clear();

        ChildEntity cEntity = new ChildEntity(1, "t1", "d1", "cn1");

        Set<String> res = FetchStatsCollector.getStatistics();

        assertTrue(res.contains("org.apache.openjpa.enhance.stats.ChildEntity.childName"));
        assertTrue(res.contains("org.apache.openjpa.enhance.stats.BEntity.name"));
        assertTrue(res.contains("org.apache.openjpa.enhance.stats.BEntity.desc"));

        // touch fields
        cEntity.getChildName();
        cEntity.getName();
        cEntity.getCustomDesc();


        res = FetchStatsCollector.getStatistics();

        assertFalse(res.contains("org.apache.openjpa.enhance.stats.ChildEntity.childName"));
        assertFalse(res.contains("org.apache.openjpa.enhance.stats.BEntity.name"));
        assertFalse(res.contains("org.apache.openjpa.enhance.stats.BEntity.desc"));
    }

    // used for manual test of the output interval
    public void _testOutputInterval() throws InterruptedException {
        Thread.currentThread();
        Thread.sleep(60 * 60 * 1000);
    }
}
