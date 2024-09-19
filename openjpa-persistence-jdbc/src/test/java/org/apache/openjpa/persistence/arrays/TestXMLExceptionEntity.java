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
package org.apache.openjpa.persistence.arrays;

import java.util.ArrayList;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.arrays.model.XMLExceptionEntity;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestXMLExceptionEntity extends SingleEMFTestCase {

    @Override
    protected String getPersistenceUnitName() {
        return "arrays";
    }

    public void testExceptionArrayAsLob() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        XMLExceptionEntity e = new XMLExceptionEntity();
        e.setId(1);
        em.persist(e);
        e.setExceptions(new ArrayList<>());
        e.getExceptions().add(new Exception("Exception 1"));
        e.getExceptions().add(new Exception("Exception 2"));
        em.getTransaction().commit();

        em.clear();
        e = em.find(XMLExceptionEntity.class, 1);

        assertNotNull(e);
        assertNotNull(e.getExceptions());
        assertEquals(2, e.getExceptions().size());
        // we don't really care about ordering for this example.

        em.getTransaction().begin();
        em.remove(e);
        em.getTransaction().commit();

        em.close();
    }

    public void testExceptionPersistentCollection() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        XMLExceptionEntity e = new XMLExceptionEntity();
        e.setId(1);
        em.persist(e);
        e.setPersCollExceptions(new ArrayList<>());
        e.getPersCollExceptions().add(new Exception("Exception 1"));
        e.getPersCollExceptions().add(new Exception("Exception 2"));
        em.getTransaction().commit();

        em.clear();
        e = em.find(XMLExceptionEntity.class, 1);

        assertNotNull(e);
        assertNotNull(e.getPersCollExceptions());
        assertEquals(2, e.getPersCollExceptions().size());
        // we don't really care about ordering for this example.

        em.getTransaction().begin();
        em.remove(e);
        em.getTransaction().commit();

        em.close();
    }

    public void testExceptionElementCollection() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        XMLExceptionEntity e = new XMLExceptionEntity();
        e.setId(1);
        em.persist(e);
        e.setElemCollExceptions(new ArrayList<>());
        e.getElemCollExceptions().add(new Exception("Exception 1").toString());
        e.getElemCollExceptions().add(new Exception("Exception 2").toString());
        em.getTransaction().commit();

        em.clear();
        e = em.find(XMLExceptionEntity.class, 1);

        assertNotNull(e);
        assertNotNull(e.getElemCollExceptions());
        assertEquals(2, e.getElemCollExceptions().size());
        // we don't really care about ordering for this example.

        em.getTransaction().begin();
        em.remove(e);
        em.getTransaction().commit();

        em.close();
    }

}
