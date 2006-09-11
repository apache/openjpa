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
package org.apache.openjpa.persistence.inheritance;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Perform basic operations on an inheritance hierarchy involving multiple
 * @MappedSuperclasses.
 *
 * @author Abe White
 */
public class TestMultipleMappedSuperclassHierarchy
    extends TestCase {

    private EntityManagerFactory emf;

    public void setUp() {
        String types = MappedSuperclassBase.class.getName() + ";"
            + MappedSuperclassL2.class.getName() + ";"
            + EntityL3.class.getName();
        Map props = new HashMap();
        props.put("openjpa.MetaDataFactory", "jpa(Types=" + types + ")");
        emf = Persistence.createEntityManagerFactory("test", props);
    }

    public void tearDown() {
        if (emf == null)
            return;
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.createQuery("delete from EntityL3").executeUpdate();
            em.getTransaction().commit();
            em.close();
            emf.close();
        } catch (Exception e) {
        }
    }

    public void testPersist() {
        EntityL3 ent = new EntityL3();
        ent.setL2Data(99); 
        ent.setL3Data(100);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(ent);
        em.getTransaction().commit();
        long id = ent.getId();
        assertTrue(id != 0);
        em.close();

        em = emf.createEntityManager();
        ent = em.find(EntityL3.class, id);
        assertNotNull(ent);
        assertEquals(99, ent.getL2Data());
        assertEquals(100, ent.getL3Data());
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestMultipleMappedSuperclassHierarchy.class);
    }
}

