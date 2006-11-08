/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Test that querying the id of a related many-one (or one-one) does not create
 * a join across the tables.
 *
 * @author Abe White
 */
public class TestQueryIdOfRelationDoesNotJoin
    extends TestCase {

    private EntityManagerFactory emf;
    private long e3Id;

    public void setUp() {
        Map props = new HashMap();
        props.put("openjpa.MetaDataFactory", "jpa(Types="
                + ManyOneEntity.class.getName() + ";"
                + ManyOneEntitySub.class.getName() + ")");
        emf = Persistence.createEntityManagerFactory("test", props);

        ManyOneEntity e1 = new ManyOneEntity();
        e1.setName("e1");
        ManyOneEntity e2 = new ManyOneEntity();
        e2.setName("e2");
        ManyOneEntity e3 = new ManyOneEntity();
        e3.setName("e3");
        e1.setRel(e3);
        e2.setRel(e1);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(e1);
        em.getTransaction().commit();
        e3Id = e3.getId();

        // we intentionally create an orphaned reference on e1.rel
        em.getTransaction().begin();
        em.remove(e3);
        em.getTransaction().commit();
        em.close();
    }

    public void tearDown() {
        if (emf == null)
            return;
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.createQuery("delete from ManyOneEntity").executeUpdate();
            em.getTransaction().commit();
            em.close();
            emf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testQuery() {
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery("select e from ManyOneEntity e "
            + "where e.rel.id = :id").setParameter("id", e3Id);
        List res = q.getResultList();
        assertEquals(1, res.size());

        ManyOneEntity e = (ManyOneEntity) res.get(0);
        assertEquals("e1", e.getName());
        assertNull(e.getRel());
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestQueryIdOfRelationDoesNotJoin.class);
    }
}

