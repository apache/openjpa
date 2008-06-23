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
package org.apache.openjpa.persistence.callbacks;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;

import junit.framework.TestCase;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.callbacks.ExceptionsFromCallbacksEntity.CallbackTestException;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests against JPA section 3.5's description of callback exception handling.
 */
public class TestExceptionsFromCallbacks
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(ExceptionsFromCallbacksEntity.class);
    }

    public void testPrePersistException() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ExceptionsFromCallbacksEntity o = new ExceptionsFromCallbacksEntity();
        o.setThrowOnPrePersist(true);
        try {
            em.persist(o);
            fail("persist should have failed");
        } catch (CallbackTestException cte) {
            // transaction should be still active, but marked for rollback
            assertTrue(em.getTransaction().isActive());
            assertTrue(em.getTransaction().getRollbackOnly());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    public void testPreUpdateExceptionDuringFlush() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ExceptionsFromCallbacksEntity o = new ExceptionsFromCallbacksEntity();
        o.setThrowOnPreUpdate(true);
        em.persist(o);
        o.setStringField("foo");
        try {
            em.flush();
            fail("flush should have failed");
        } catch (CallbackTestException cte) {
            // transaction should be still active, but marked for rollback
            assertTrue(em.getTransaction().isActive());
            assertTrue(em.getTransaction().getRollbackOnly());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }

    public void testPreUpdateExceptionDuringCommit() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ExceptionsFromCallbacksEntity o = new ExceptionsFromCallbacksEntity();
        o.setThrowOnPreUpdate(true);
        em.persist(o);
        o.setStringField("foo");
        try {
            em.getTransaction().commit();
            fail("commit should have failed");
        } catch (RollbackException re) {
            assertEquals(CallbackTestException.class,
                re.getCause().getClass());
            
            // transaction should be rolled back at this point
            assertFalse(em.getTransaction().isActive());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }
    
    public void testPostLoadException() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        ExceptionsFromCallbacksEntity o = new ExceptionsFromCallbacksEntity();
        o.setThrowOnPostLoad(true);
        em.persist(o);
        em.getTransaction().commit();
        Object oid = OpenJPAPersistence.cast(em).getObjectId(o);
        em.close();
        
        em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            o = em.find(ExceptionsFromCallbacksEntity.class, oid);
            fail("find should have failed");
        } catch (CallbackTestException cte) {
            // transaction should be active but marked for rollback
            assertTrue(em.getTransaction().isActive());
            assertTrue(em.getTransaction().getRollbackOnly());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            em.close();
        }
    }
}
