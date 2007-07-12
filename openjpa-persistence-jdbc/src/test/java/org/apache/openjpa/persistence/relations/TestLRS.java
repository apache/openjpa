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
package org.apache.openjpa.persistence.relations;

import java.util.Iterator;
import javax.persistence.EntityManager;

import junit.textui.TestRunner;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Test LRS relations.
 *
 * @author Abe White
 */
public class TestLRS
    extends SingleEMFTestCase {

    private long id;

    public void setUp() {
        setUp(LRSEntity.class, BasicEntity.class, CLEAR_TABLES);
        
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        LRSEntity lrs = new LRSEntity();
        lrs.setName("lrs"); 
        for (int i = 1; i <= 3; i++) {
            BasicEntity basic = new BasicEntity();
            basic.setName("basic" + i);
            em.persist(basic);
            lrs.getLRSList().add(basic);
        }
        em.persist(lrs);
        em.getTransaction().commit();
        id = lrs.getId();
        em.close();
    }

    public void testEMClear() {
        EntityManager em = emf.createEntityManager();
        LRSEntity lrs = em.find(LRSEntity.class, id);
        assertLRS(lrs, "lrs");
        em.clear();
        assertNull(lrs.getLRSList());
        assertMerge(lrs);
        em.close();
    }

    public void testEMClose() {
        EntityManager em = emf.createEntityManager();
        LRSEntity lrs = em.find(LRSEntity.class, id);
        assertLRS(lrs, "lrs");
        em.close();
        assertNull(lrs.getLRSList());
        assertMerge(lrs);
    }

    public void testDetachCopy() {
        OpenJPAEntityManager em = emf.createEntityManager();
        LRSEntity lrs = em.find(LRSEntity.class, id);
        assertLRS(lrs, "lrs");
        lrs = em.detach(lrs); 
        assertEquals("lrs", lrs.getName());
        assertNull(lrs.getLRSList());
        em.close();
        assertMerge(lrs);
    }

    private void assertLRS(LRSEntity lrs, String name) {
        assertNotNull(lrs);
        assertEquals(name, lrs.getName());
        assertEquals(3, lrs.getLRSList().size());
        Iterator itr = lrs.getLRSList().iterator();
        for (int i = 1; itr.hasNext(); i++) {
            BasicEntity basic = (BasicEntity) itr.next();
            assertEquals("basic" + i, basic.getName());
        }
        OpenJPAPersistence.close(itr);
    }

    private void assertMerge(LRSEntity lrs) {
        lrs.setName("changed");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        assertLRS(em.merge(lrs), "changed");
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        assertLRS(em.find(LRSEntity.class, id), "changed");
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestLRS.class);
    }
}

