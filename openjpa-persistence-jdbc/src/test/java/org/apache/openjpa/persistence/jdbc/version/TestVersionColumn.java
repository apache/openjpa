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
 package org.apache.openjpa.persistence.jdbc.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import org.apache.openjpa.persistence.jdbc.version.model.IntVersion;
import org.apache.openjpa.persistence.jdbc.version.model.ShortVersion;
import org.apache.openjpa.persistence.jdbc.version.model.TimestampVersion;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

 public class TestVersionColumn extends SQLListenerTestCase {
     @BeforeEach
     public void setUp() {
        super.setUp(IntVersion.class, TimestampVersion.class, ShortVersion.class);
    }

     @Test
     public void testNullIntegerVersion() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        em.persist(new IntVersion());
        em.persist(new IntVersion());
        tran.commit();

        tran.begin();
        em.createNativeQuery("UPDATE IntVersion set version = NULL").executeUpdate();
        tran.commit();
        em.clear();

        resetSQL();

        List<IntVersion> results = em.createQuery("SELECT i from IntVersion i", IntVersion.class).getResultList();
        assertNotNull(results, "No results found");
        assertFalse(results.isEmpty(), "No results found");
        for (IntVersion iv : results) {
            assertEquals(0, iv.getVersion(), "Version should be initialized to 0, was: " + iv.getVersion());
            em.find(IntVersion.class, iv.getId());
        }

        assertEquals(1, getSQLCount(), "Unexpected number of SQL statements: " + getSQLCount());

        em.close();
    }

     @Test
     public void testNullTimestampVersion() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        em.persist(new TimestampVersion());
        em.persist(new TimestampVersion());
        tran.commit();

        tran.begin();
        em.createNativeQuery("UPDATE TimestampVersion set version = NULL").executeUpdate();
        tran.commit();
        em.clear();

        resetSQL();
        List<TimestampVersion> results =
            em.createQuery("SELECT i from TimestampVersion i", TimestampVersion.class).getResultList();
        assertNotNull(results, "No results found");
        assertFalse(results.isEmpty(), "No results found");
        for (TimestampVersion iv : results) {
            assertEquals(null, iv.getVersion(), "Version should be initialized to null, was: " + iv.getVersion());
            em.find(TimestampVersion.class, iv.getId());
        }

        assertEquals(1, getSQLCount(), "Unexpected number of SQL statements: " + getSQLCount());

        em.close();
    }

     @Test
     public void testNullShortVersion() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        em.persist(new ShortVersion());
        em.persist(new ShortVersion());
        tran.commit();

        tran.begin();
        em.createNativeQuery("UPDATE ShortVersion set version = NULL").executeUpdate();
        tran.commit();
        em.clear();

        resetSQL();
        List<ShortVersion> results =
            em.createQuery("SELECT i from ShortVersion i", ShortVersion.class).getResultList();
        assertNotNull(results, "No results found");
        assertFalse(results.isEmpty(), "No results found");
        for (ShortVersion iv : results) {
            assertEquals(0, iv.getVersion(), "Version should be initialized to 0, was" + iv.getVersion());
            em.find(ShortVersion.class, iv.getId());
        }


        assertEquals(1, getSQLCount(), "Unexpected number of SQL statements: " + getSQLCount());

        em.close();
    }
}
