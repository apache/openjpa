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
package org.apache.openjpa.persistence.generationtype;

import javax.persistence.EntityManager;
import javax.persistence.EntityExistsException;

import org.apache.openjpa.persistence.InvalidStateException;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestGeneratedValues extends SingleEMFTestCase {
    
    public void setUp() { 
        setUp(GeneratedValues.class, CLEAR_TABLES);
    }

    public void testDefaultValues() { 
        EntityManager em = emf.createEntityManager();

        GeneratedValues gv = new GeneratedValues();
        GeneratedValues gv2 = new GeneratedValues();

        em.getTransaction().begin();
        em.persist(gv);
        em.persist(gv2);
        em.getTransaction().commit();

        em.refresh(gv);
        em.refresh(gv2);

        assertFalse(gv.getId() == gv2.getId());
        assertFalse(gv.getField() == gv2.getField());
    }
    
    public void testInitialValues() { 
        EntityManager em = emf.createEntityManager();

        GeneratedValues gv = new GeneratedValues(7, 9);

        try {
            em.getTransaction().begin();
            em.persist(gv);
            em.getTransaction().commit();
        } catch (InvalidStateException ise) {
            // expected result
            return;
        }  catch (EntityExistsException eee) {
            // also ok
            return;
        }
        
        // should not get here...
        fail();
    }
    
    public void testIdSetter() { 
        EntityManager em = emf.createEntityManager();

        GeneratedValues gv = new GeneratedValues();
        gv.setId(3);

        try {
            em.getTransaction().begin();
            em.persist(gv);
            em.getTransaction().commit();
        } catch (InvalidStateException ise) {
            // expected result
            return;
        }  catch (EntityExistsException eee) {
            // also ok
            return;
        }
        
        // should not get here...
        fail();
    }
    
    public void testFieldSetter() { 
        EntityManager em = emf.createEntityManager();

        GeneratedValues gv = new GeneratedValues();
        gv.setField(5);

        try {
            em.getTransaction().begin();
            em.persist(gv);
            em.getTransaction().commit();
        } catch (InvalidStateException ise) {
            // expected result
            return;
        }
        
        // should not get here...
        fail();
    }
}
