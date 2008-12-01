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
package org.apache.openjpa.slice;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * Tests query ordering.
 * 
 * @author Pinaki Poddar 
 *
 */
public class TestQuery extends SliceTestCase {
    private int POBJECT_COUNT = 25;
    private int VALUE_MIN = 100;
    private int VALUE_MAX = VALUE_MIN + POBJECT_COUNT - 1;
    
    protected String getPersistenceUnitName() {
        return "ordering";
    }

    public void setUp() throws Exception {
        super.setUp(PObject.class, Person.class, Address.class, CLEAR_TABLES);
        int count = count(PObject.class);
        if (count == 0) {
            create(POBJECT_COUNT);
        }
    }
    
    void create(int N) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i=0;i<POBJECT_COUNT;i++) {
            PObject pc = new PObject();
            pc.setValue(VALUE_MIN + i);
            em.persist(pc);
            String slice = SlicePersistence.getSlice(pc);
            String expected = (pc.getValue()%2 == 0) ? "Even" : "Odd";
            assertEquals(expected, slice);
        }
        em.getTransaction().commit();
    }
    
    public void testQueryResultIsOrderedAcrossSlice() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("SELECT p.value,p FROM PObject p ORDER BY p.value ASC");
        List result = query.getResultList();
        Integer old = Integer.MIN_VALUE;
        for (Object row : result) {
            Object[] line = (Object[])row;
            int value = ((Integer)line[0]).intValue();
            PObject pc = (PObject)line[1];
            assertTrue(value >= old);
            old = value;
            assertEquals(value, pc.getValue());
        }
        em.getTransaction().rollback();
    }
    
    public void testAggregateQuery() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Object count = em.createQuery("SELECT COUNT(p) FROM PObject p").getSingleResult();
        Object max   = em.createQuery("SELECT MAX(p.value) FROM PObject p").getSingleResult();
        Object min   = em.createQuery("SELECT MIN(p.value) FROM PObject p").getSingleResult();
        Object sum   = em.createQuery("SELECT SUM(p.value) FROM PObject p").getSingleResult();
        Object minmax   = em.createQuery("SELECT MIN(p.value),MAX(p.value) FROM PObject p").getSingleResult();
        Object min1 = ((Object[])minmax)[0];
        Object max1 = ((Object[])minmax)[1];
        em.getTransaction().rollback();
        
        assertEquals(POBJECT_COUNT, ((Number)count).intValue());
        assertEquals(VALUE_MAX, ((Number)max).intValue());
        assertEquals(VALUE_MIN, ((Number)min).intValue());
        assertEquals((VALUE_MIN+VALUE_MAX)*POBJECT_COUNT, 2*((Number)sum).intValue());
        assertEquals(min, min1);
        assertEquals(max, max1);
    }
    
    public void testSetMaxResult() {
        EntityManager em = emf.createEntityManager();
        int limit = 3;
        em.getTransaction().begin();
        List result = em.createQuery("SELECT p.value,p FROM PObject p ORDER BY p.value ASC")
            .setMaxResults(limit).getResultList();
        int i = 0;
        for (Object row : result) {
            Object[] line = (Object[])row;
            int value = ((Integer)line[0]).intValue();
            PObject pc = (PObject)line[1];
            System.err.println(++i + "." + SlicePersistence.getSlice(pc) + ":" 
                    + pc.getId() + "," + pc.getValue());
        }
        assertEquals(limit, result.size());
        em.getTransaction().rollback();
    }
    
    public void testHint() {
        List<String> targets = new ArrayList<String>();
        targets.add("Even");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("SELECT p FROM PObject p");
        query.setHint(ProductDerivation.HINT_TARGET, "Even");
        List result = query.getResultList();
        for (Object pc : result) {
            String slice = SlicePersistence.getSlice(pc);
            assertTrue(targets.contains(slice));
        }
        em.getTransaction().rollback();
    }
}
