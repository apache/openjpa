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
package org.apache.openjpa.persistence;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestUnwrap extends SingleEMFTestCase {
    
    /**
     * Tests a query can be unwrapped as an instance of a series of class or 
     * interface. 
     */
    public void testValidQueryUnwrap() {
        OpenJPAEntityManager em = emf.createEntityManager();
        Query query = em.createQuery(QueryLanguages.LANG_SQL,"");
        
        Class[] validCasts = new Class[] {
            org.apache.openjpa.persistence.OpenJPAQuery.class,
            org.apache.openjpa.persistence.OpenJPAQuerySPI.class,
            org.apache.openjpa.kernel.DelegatingQuery.class,
            org.apache.openjpa.kernel.Query.class,
            org.apache.openjpa.kernel.QueryImpl.class
        };
        for (Class c : validCasts) {
            Object unwrapped = query.unwrap(c);
            assertTrue(c.isInstance(unwrapped));
        }
    }

    /**
     * Tests a EntityManager can be unwrapped as an instance of a series of 
     * class or interface. 
     */
    public void testValidEntityManagerUnwrap() {
        EntityManager em = emf.createEntityManager();
        
        Class<?>[] validCasts = new Class[] {
            org.apache.openjpa.persistence.OpenJPAEntityManager.class,
            org.apache.openjpa.persistence.OpenJPAEntityManagerSPI.class,
            org.apache.openjpa.kernel.DelegatingBroker.class,
            org.apache.openjpa.kernel.Broker.class
        };
        for (Class<?> c : validCasts) {
            Object unwrapped = em.unwrap(c);
            assertTrue(c.isInstance(unwrapped));
        }
    }
    
    /**
     * Tests a EntityManager can be unwrapped as an instance of a series of 
     * class or interface. 
     */
    public void testInvalidEntityManagerUnwrap() {
        EntityManager em = emf.createEntityManager();
        
        Class<?>[] validCasts = new Class[] {
            Object.class,
            Properties.class,
            null,
        };
        for (Class<?> c : validCasts) {
            try {
                em.unwrap(c);
                fail("Expected to fail to unwarp with " + c);
            } catch (PersistenceException e) {
                EntityTransaction txn = em.getTransaction();
                assertFalse(txn.isActive());
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Unexpected exception while unwrapping " + c);
            }
        }
    }

}
