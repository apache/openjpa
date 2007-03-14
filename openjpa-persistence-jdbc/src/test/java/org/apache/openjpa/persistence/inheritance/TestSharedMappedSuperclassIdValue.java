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
import javax.persistence.Persistence;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Test that sibling classes with a shared id value declared in their 
 * mapped superclass are distinguished correctly.
 *
 * @author Abe White
 */
public class TestSharedMappedSuperclassIdValue
    extends TestCase {

    private EntityManagerFactory emf;

    public void setUp() {
        String types = MappedSuperclassBase.class.getName() + ";"
            + MappedSuperclassL2.class.getName() + ";"
            + EntityL3.class.getName() + ";"
            + EntityL3Sibling.class.getName();
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" + types + ")");
        emf = Persistence.createEntityManagerFactory("test", props);

        EntityL3 ent = new EntityL3();
        ent.setId(1);
        ent.setL2Data(99); 
        ent.setL3Data(100);
        EntityL3Sibling sib = new EntityL3Sibling();
        sib.setId(1);
        sib.setL2Data(100); 
        sib.setSiblingL3Data(101);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(ent);
        em.persist(sib);
        em.getTransaction().commit();
        em.close();

    }

    public void tearDown() {
        if (emf == null)
            return;
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.createQuery("delete from EntityL3").executeUpdate();
            em.createQuery("delete from EntityL3Sibling").executeUpdate();
            em.getTransaction().commit();
            em.close();
            emf.close();
        } catch (Exception e) {
        }
    }

    public void testFind() {
        EntityManager em = emf.createEntityManager();
        EntityL3 ent = em.find(EntityL3.class, 1L);
        assertNotNull(ent);
        assertEquals(99, ent.getL2Data());
        assertEquals(100, ent.getL3Data());

        EntityL3Sibling sib = em.find(EntityL3Sibling.class, 1L);
        assertNotNull(sib);
        assertEquals(100, sib.getL2Data());
        assertEquals(101, sib.getSiblingL3Data());

        em.close();
    }

    public void testGetReference() {
        EntityManager em = emf.createEntityManager();
        EntityL3 ent = em.getReference(EntityL3.class, 1L);
        assertNotNull(ent);

        EntityL3Sibling sib = em.getReference(EntityL3Sibling.class, 1L);
        assertNotNull(sib);

        assertEquals(99, ent.getL2Data());
        assertEquals(100, ent.getL3Data());
        assertEquals(100, sib.getL2Data());
        assertEquals(101, sib.getSiblingL3Data());

        em.close();
    }


    public static void main(String[] args) {
        TestRunner.run(TestSharedMappedSuperclassIdValue.class);
    }
}

