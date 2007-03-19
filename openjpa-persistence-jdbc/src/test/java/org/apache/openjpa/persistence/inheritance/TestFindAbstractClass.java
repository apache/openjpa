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
 * Test that you can find a concrete subclass record when passing in its
 * abstract base class to EntityManager.find().
 *
 * @author Abe White
 */
public class TestFindAbstractClass
    extends TestCase {

    private EntityManagerFactory emf;

    public void setUp() {
        String types = AbstractBase.class.getName() + ";"
            + ConcreteSubclass.class.getName();
        Map props = new HashMap(System.getProperties());
        props.put("openjpa.MetaDataFactory", "jpa(Types=" + types + ")");
        emf = Persistence.createEntityManagerFactory("test", props);

        ConcreteSubclass e = new ConcreteSubclass();
        e.setId("id");
        e.setSubclassData(1); 

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(e);
        em.getTransaction().commit();
        em.close();
    }

    public void tearDown() {
        if (emf == null)
            return;
        try {
            EntityManager em = emf.createEntityManager();
            em.getTransaction().begin();
            em.createQuery("delete from ConcreteSubclass").executeUpdate();
            em.getTransaction().commit();
            em.close();
            emf.close();
        } catch (Exception e) {
        }
    }

    public void testFind() {
        EntityManager em = emf.createEntityManager();
        AbstractBase e = em.find(AbstractBase.class, "id");
        assertNotNull(e);
        assertTrue(e instanceof ConcreteSubclass);
        assertEquals(1, ((ConcreteSubclass) e).getSubclassData());
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestFindAbstractClass.class);
    }
}

