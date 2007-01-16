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
package org.apache.openjpa.persistence.detachment;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Test that attaching an instance without having changed it still overwrites
 * any changes to the managed copy.
 *
 * @author Abe White
 */
public class TestAttachWithNoChanges
    extends TestCase {

    private EntityManagerFactory emf;

    public void setUp() {
        String types = DetachmentOneManyParent.class.getName() + ";"
            + DetachmentOneManyChild.class.getName(); 
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" + types + ")");
        emf = Persistence.createEntityManagerFactory("test", props);
    }

    public void tearDown() {
        if (emf == null)
            return;
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.createQuery("delete from DetachmentOneManyChild").
                executeUpdate();
            em.createQuery("delete from DetachmentOneManyParent").
                executeUpdate();
            em.getTransaction().commit();
            em.close();
            emf.close();
        } catch (Exception e) {
        }
    }
    
    public void testAttachWithNoChangesChecksVersion() {
try {
        DetachmentOneManyChild e = new DetachmentOneManyChild();
        DetachmentOneManyParent p = new DetachmentOneManyParent();
        e.setName("orig");
        p.addChild(e);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(p);
        em.persist(e);
        em.flush();
        em.clear();
        
        DetachmentOneManyChild changed = em.find(DetachmentOneManyChild.class,
            e.getId()); 
        changed.setName("newname");
        em.flush();

        em.merge(e);
        try {
            em.flush();
            fail("Should not be able to flush old version over new.");
        } catch (OptimisticLockException ole) {
            // expected
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
} catch (RuntimeException re) {
re.printStackTrace();
throw re;
}
    }

    public static void main(String[] args) {
        TestRunner.run(TestAttachWithNoChanges.class);
    }
}

