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
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.slice.SlicePersistence;

public class TestQuery extends SliceTestCase {
    public void setUp() throws Exception {
        super.setUp(PObject.class, Person.class, Address.class);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        long id = System.currentTimeMillis();
        for (int i=0;i<10;i++) {
            PObject pc = new PObject(id++);
            pc.setValue(i);
            em.persist(pc);
            String slice = SlicePersistence.getSlice(pc);
            String expected = (i%2 == 0) ? "Even" : "Odd";
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
        for (Object row:result) {
            Object[] line = (Object[])row;
            int value = ((Integer)line[0]).intValue();
            PObject pc = (PObject)line[1];
            assertTrue(value >= old);
            old = value;
            assertEquals(value, pc.getValue());
        }
        em.getTransaction().commit();
    }
    
    public void testAggregateQuery() {
        EntityManager em = emf.createEntityManager();
        List result = em.createQuery("SELECT COUNT(p) FROM PObject p").getResultList();
        for (Object r:result)
            System.err.println(r);
    }
    
    public void testSetMaxResult() {
        EntityManager em = emf.createEntityManager();
        int limit = 3;
        em.getTransaction().begin();
        List result = em.createQuery("SELECT p.value,p FROM PObject p ORDER BY p.value ASC")
            .setMaxResults(limit).getResultList();
        int i = 0;
        for (Object row:result) {
            Object[] line = (Object[])row;
            int value = ((Integer)line[0]).intValue();
            PObject pc = (PObject)line[1];
            System.err.println(++i + "." + SlicePersistence.getSlice(pc) + ":" + pc.getId() + "," + pc.getValue());
        }
        em.getTransaction().rollback();
        assertEquals(limit, result.size());
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
    
    protected String getPersistenceUnitName() {
        return "ordering";
    }
}
