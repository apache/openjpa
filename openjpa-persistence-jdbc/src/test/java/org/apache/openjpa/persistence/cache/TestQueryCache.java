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
package org.apache.openjpa.persistence.cache;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NamedQuery;

import org.apache.openjpa.persistence.cache.common.apps.Entity1;
import org.apache.openjpa.persistence.cache.common.apps.Entity2;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;

@NamedQuery(name = "setParam1",
    query = "SELECT o FROM Entity1 o WHERE o.pk LIKE :pk")
public class TestQueryCache extends AbstractTestCase {

    EntityManager em;

    public TestQueryCache(String name) {
        super(name);
        System.setProperty("cactus.contextURL",
            "http://localhost:9000/cachecactus");
        em = currentEntityManager();
    }

    /*public static Test suite()
    {
        ServletTestSuite suite = new ServletTestSuite();
        suite.addTestSuite(TestQueryCache.class);
        return suite;
    }*/
    public void setUp() {
        System.setProperty("cactus.contextURL",
            "http://localhost:9000/cactuswebapp");

        //deleteAll(Entity2.class);
        deleteAll(Entity1.class);

        int instNum = 10;

        startTx(em);

        //create and persist multiple entity1 instances
        for (int i = 0; i < instNum; i++) {
            Entity1 ent = new Entity1(i, "string" + i, i + 2);
            Entity2 ent2 = new Entity2(i * 2, "ent2" + i, i);
            ent.setEntity2Field(ent2);
            em.persist(ent);
        }

        endTx(em);
        endEm(em);
    }

    public void testResultList() {
        em = currentEntityManager();
        List list = em.createQuery("Select object(o) from Entity1 o")
            .getResultList();

        assertEquals(10, list.size());

        endEm(em);
    }

    public void testGetSingleList() {
        em = currentEntityManager();
        String curr = 2 + "";

        Entity1 ret = (Entity1) em
            .createQuery("SELECT o FROM Entity1 o WHERE o.pk LIKE :pk")
            .setParameter("pk", curr)
            .getSingleResult();

        assertNotNull(ret);
        assertEquals("string2", ret.getStringField());
        assertEquals(4, ret.getIntField());

        endEm(em);
    }

    public void testExecuteUpdate() {
        String curr = 2 + "";
        String curr2 = 22 + "";

        em = currentEntityManager();
        startTx(em);

        Entity1 entity1 = (Entity1) em
            .createQuery("SELECT o FROM Entity1 o WHERE o.pk LIKE :pk")
            .setParameter("pk", curr)
            .getSingleResult();

        int ret = em.createQuery("Delete FROM Entity1 o WHERE o.pk LIKE :pk")
            .setParameter("pk", curr)
            .executeUpdate();
        assertEquals(ret, 1);

//cascade remove doesn't remove the entity2
        int retTmp = em.createQuery("Delete FROM Entity2 o WHERE o.pk LIKE :pk")
            .setParameter("pk", entity1.getEntity2Field().getPk())
            .executeUpdate();

        int ret2 = em.createQuery("Delete FROM Entity1 o WHERE o.pk LIKE :pk")
            .setParameter("pk", curr2)
            .executeUpdate();

        assertEquals(ret2, 0);

        endTx(em);
        endEm(em);
    }

    public void testSetMaxResults() {
        em = currentEntityManager();

        List l = em.createQuery("Select object(o) from Entity1 o")
            .setMaxResults(5)
            .getResultList();

        assertNotNull(l);
        assertEquals(5, l.size());

        endEm(em);
    }

    public void testSetFirstResults() {
        em = currentEntityManager();

        List l = em.createQuery("Select object(o) from Entity1 o")
            .setFirstResult(3)
            .getResultList();

        Entity1 ent = (Entity1) l.get(0);

        assertNotNull(ent);
        assertEquals("string3", ent.getStringField());
        assertEquals(5, ent.getIntField());

        endEm(em);
    }

    // Tests Binding an argument to a named parameter.
    // pk, the named parameter --Not working yet--
    public void xxxtestSetParameter1() {

        em = currentEntityManager();
        String curr = 2 + "";

        List ret = em.createQuery("SELECT o FROM Entity1 o WHERE o.pk LIKE :pk")
            .setParameter("pk", curr)
            .getResultList();

        assertNotNull(ret);
        assertEquals(1, ret.size());

        ret = em.createNamedQuery("setParam1")
            .setParameter("pk", curr)
            .getResultList();

        assertNotNull(ret);
        assertEquals(1, ret.size());

        endTx(em);
    }

    //rest of the interface is tested by the CTS
}
