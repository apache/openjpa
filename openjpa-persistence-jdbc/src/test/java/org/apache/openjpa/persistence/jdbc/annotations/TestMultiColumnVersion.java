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
package org.apache.openjpa.persistence.jdbc.annotations;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Array;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.strats.MultiColumnVersionStrategy;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests numeric version spanning multiple columns and those columns spanning
 * multiple tables.
 *
 * @author Pinaki Poddar
 */
public class TestMultiColumnVersion extends SingleEMFTestCase {
    @BeforeEach
    public void setUp() {
        setUp(MultiColumnVersionPC.class, MultiColumnSecondaryVersionPC.class,
        		CLEAR_TABLES);
    }

    @Test
    void versionStrategyIsSet() {
    	assertStrategy(MultiColumnVersionPC.class);
    	assertStrategy(MultiColumnSecondaryVersionPC.class);
    }

    public void assertStrategy(Class cls) {
    	ClassMapping mapping = getMapping(cls);
    	assertNotNull(mapping.getVersion());
        assertInstanceOf(MultiColumnVersionStrategy.class, mapping.getVersion().getStrategy());
    }

    @Test
    void versionOnPersistAndUpdateForSingleTable() {
    	OpenJPAEntityManager em = emf.createEntityManager();
    	em.getTransaction().begin();
        MultiColumnVersionPC pc = new MultiColumnVersionPC();
        assertNull(em.getVersion(pc));
        em.persist(pc);
        em.getTransaction().commit();
        assertVersionEquals(new Number[]{1,1, 1.0f}, em.getVersion(pc));

    	em.getTransaction().begin();
    	pc.setName("updated");
    	em.merge(pc);
        em.getTransaction().commit();
        assertVersionEquals(new Number[]{2,2, 2.0f}, em.getVersion(pc));
        em.close();
    }

    @Test
    void concurrentOptimisticUpdateFailsForSingleTable() {
    	OpenJPAEntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        OpenJPAEntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();

        MultiColumnVersionPC pc1 = new MultiColumnVersionPC();
        em1.persist(pc1);
        em1.getTransaction().commit();
        em1.getTransaction().begin();
        Object oid = em1.getObjectId(pc1);


        MultiColumnVersionPC pc2 = em2.find(MultiColumnVersionPC.class, oid);
        assertVersionEquals(em1.getVersion(pc1), em2.getVersion(pc2));

        pc1.setName("Updated in em1");
        pc2.setName("Updated in em2");
        em1.getTransaction().commit();
        em1.close();

        try {
            em2.getTransaction().commit();
            fail("Optimistic fail");
        } catch (Exception e) {
        } finally {
            em2.close();
        }
    }

    @Test
    void concurrentOptimisticReadSucceedsForSingleTable() {
    	OpenJPAEntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        OpenJPAEntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();

        MultiColumnVersionPC pc1 = new MultiColumnVersionPC();
        em1.persist(pc1);
        em1.getTransaction().commit();
        em1.getTransaction().begin();
        Object oid = em1.getObjectId(pc1);


        MultiColumnVersionPC pc2 = em2.find(MultiColumnVersionPC.class, oid);
        assertVersionEquals(em1.getVersion(pc1), em2.getVersion(pc2));

        em1.getTransaction().commit();
        em1.close();
        em2.getTransaction().commit();
        em2.close();
    }

    @Test
    void versionOnPersistAndUpdateForMultiTable() {
    	OpenJPAEntityManager em = emf.createEntityManager();
    	em.getTransaction().begin();
    	MultiColumnSecondaryVersionPC pc = new MultiColumnSecondaryVersionPC();
        assertNull(em.getVersion(pc));
        em.persist(pc);
        em.getTransaction().commit();
        assertVersionEquals(new Number[]{1,1,1,1}, em.getVersion(pc));

    	em.getTransaction().begin();
    	pc.setName("updated");
    	em.merge(pc);
        em.getTransaction().commit();
        assertVersionEquals(new Number[]{2,2,2,2}, em.getVersion(pc));
        em.close();
    }

    @Test
    void concurrentOptimisticUpdateFailsForMultiTable() {
    	OpenJPAEntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        OpenJPAEntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();

    	MultiColumnSecondaryVersionPC pc1 = new MultiColumnSecondaryVersionPC();
        em1.persist(pc1);
        em1.getTransaction().commit();
        em1.getTransaction().begin();
        Object oid = em1.getObjectId(pc1);


        MultiColumnSecondaryVersionPC pc2 =
            em2.find(MultiColumnSecondaryVersionPC.class, oid);
        assertVersionEquals(em1.getVersion(pc1), em2.getVersion(pc2));

        pc1.setName("Updated in em1");
        pc2.setName("Updated in em2");
        em1.getTransaction().commit();
        em1.close();

        try {
            em2.getTransaction().commit();
            fail("Optimistic fail");
        } catch (Exception e) {
        } finally {
            em2.close();
        }
    }

    @Test
    void concurrentOptimisticReadSucceedsForMultiTable() {
    	OpenJPAEntityManager em1 = emf.createEntityManager();
        em1.getTransaction().begin();
        OpenJPAEntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();

    	MultiColumnSecondaryVersionPC pc1 = new MultiColumnSecondaryVersionPC();
        em1.persist(pc1);
        em1.getTransaction().commit();
        em1.getTransaction().begin();
        Object oid = em1.getObjectId(pc1);


    	MultiColumnSecondaryVersionPC pc2 =
    	    em2.find(MultiColumnSecondaryVersionPC.class, oid);
        assertVersionEquals(em1.getVersion(pc1), em2.getVersion(pc2));

        em1.getTransaction().commit();
        em1.close();
        em2.getTransaction().commit();
        em2.close();
    }

    static void assertVersionEquals(Object expected, Object actual) {
    	assertTrue(expected.getClass().isArray());
    	assertTrue(actual.getClass().isArray());
    	assertEquals(Array.getLength(expected), Array.getLength(actual));
    	int n = Array.getLength(expected);
    	for (int i = 0; i < n; i++) {
    		Object v1 = Array.get(expected, i);
    		Object v2 = Array.get(actual, i);
    		// exact equality may fail on non-integral values
    		assertTrue(Math.abs(((Number)v1).doubleValue()
       		            - ((Number)v2).doubleValue())
       		    < 0.01,
       		    "element " + i + " mismatch. Expeceted: " +
       		    v1 + " actual: " + v2);
    	}
    }
}
