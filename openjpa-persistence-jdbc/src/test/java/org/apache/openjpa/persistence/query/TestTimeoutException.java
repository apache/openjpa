/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.openjpa.persistence.query;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.Query;
import javax.persistence.QueryTimeoutException;

import junit.framework.AssertionFailedError;

import org.apache.openjpa.persistence.exception.PObject;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that correct timeout exceptions are being thrown depending on whether it is a query or a lock operation.
 * 
 * @author Pinaki Poddar
 *
 */
public class TestTimeoutException extends SingleEMFTestCase {
    private final Class<?> entityClass = PObject.class;
    private final ExecutorService scheduler = Executors.newCachedThreadPool();
    public void setUp() {
        super.setUp(entityClass);
    }
    
    public void testQueryTimeOutExceptionWhileQueryingWithLocksOnAlreadyLockedEntities() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        assertNotSame(em1, em2);
        Object oid = createEntity(em1);
        
        em1.getTransaction().begin();
        Object entity = em1.find(entityClass, oid);
        assertNotNull(entity);
        em1.lock(entity, LockModeType.PESSIMISTIC_WRITE);
        
        em2.getTransaction().begin();
        final Query query = em2.createQuery("select p from PObject p");
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        long timeout = 1000;
        query.setHint("javax.persistence.query.timeout", timeout);
        try {
            query.getResultList();
            fail("Expected " + QueryTimeoutException.class.getName());
        } catch (Throwable t) {
            assertError(t, QueryTimeoutException.class, timeout);
        }
        
        assertTrue(em2.getTransaction().isActive());
        em2.getTransaction().rollback();
        em1.getTransaction().rollback();
    }
    
    public void testLockTimeOutExceptionWhileLockingAlreadyLockedEntities() {
        EntityManager em1 = emf.createEntityManager();
        final EntityManager em2 = emf.createEntityManager();
        assertNotSame(em1, em2);
        final Object oid = createEntity(em1);
        
        em1.getTransaction().begin();
        final Object entity1 = em1.find(entityClass, oid);
        assertNotNull(entity1);
        em1.lock(entity1, LockModeType.PESSIMISTIC_WRITE);
        
        em2.getTransaction().begin();
        final Object entity2 = em2.find(entityClass, oid);
        final long timeout = 1000;
        try {
            Map<String,Object> hint = new HashMap<String, Object>();
            hint.put("javax.persistence.lock.timeout", timeout);
            em2.lock(entity2, LockModeType.PESSIMISTIC_WRITE, hint);
            fail("Expected " + LockTimeoutException.class.getName());
        } catch (Throwable t) {
           assertError(t, LockTimeoutException.class, timeout);
        }
        assertTrue(em2.getTransaction().isActive());
        em2.getTransaction().rollback();
        
        em1.getTransaction().rollback();
    }

    
    public Object createEntity(EntityManager em) {
        long id = System.nanoTime();
        em.getTransaction().begin();
        PObject pc = new PObject();
        pc.setId(id);
        em.persist(pc);
        em.getTransaction().commit();
        return id;
    }
    
    
    /**
     * Assert that an exception of proper type has been thrown by the given task within the given timeout.
     * @param t
     * @param expeceted
     */
    void assertError(Throwable actual, Class<? extends Throwable> expected, long timeout) {
        if (!expected.isAssignableFrom(actual.getClass())) {
                actual.printStackTrace();
                throw new AssertionFailedError(actual.getClass().getName() + " was raised but expected " + 
                        expected.getName());
        }
        
    }   
    
}
