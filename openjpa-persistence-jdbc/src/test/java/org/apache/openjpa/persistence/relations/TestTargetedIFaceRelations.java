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
package org.apache.openjpa.persistence.relations;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Perform basic operations on an entity with interface relations that use
 * the targetEntity attribute to set a concrete related type.
 *
 * @author Abe White
 */
public class TestTargetedIFaceRelations
    extends TestCase {

    private EntityManagerFactory emf;

    public void setUp() {
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" 
            + TargetedIFaceRelationParent.class.getName() + ")");
        emf = Persistence.createEntityManagerFactory("test", props);
    }

    public void tearDown() {
        if (emf == null)
            return;
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.createQuery("delete from TargetedIFaceRelationParent").
                executeUpdate();
            em.getTransaction().commit();
            em.close();
            emf.close();
        } catch (Exception e) {
        }
    }

    public void testPersist() {
        TargetedIFaceRelationParent parent = new TargetedIFaceRelationParent();
        parent.setName("parent");
        TargetedIFaceRelationParent child = new TargetedIFaceRelationParent();
        child.setName("child");
        parent.setIFace(child);
        
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(parent);
        em.getTransaction().commit();
        long id = parent.getId();
        assertTrue(id != 0);
        em.close();

        em = emf.createEntityManager();
        parent = em.find(TargetedIFaceRelationParent.class, id);
        assertNotNull(parent);
        assertEquals("parent", parent.getName());
        assertNotNull(parent.getIFace());
        assertEquals("child", parent.getIFace().getName());
        assertEquals(TargetedIFaceRelationParent.class, 
            parent.getIFace().getClass());
        assertNull(((TargetedIFaceRelationParent) parent.getIFace()).
            getIFace());
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestTargetedIFaceRelations.class);
    }
}

