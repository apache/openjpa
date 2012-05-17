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
package org.apache.openjpa.persistence.meta;


import java.util.List;
import java.util.UUID;

import javax.persistence.Query;

import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.meta.common.apps.ExternalValues;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * <p>Tests the {@link ExternalValuesFieldMapping}.</p>
 *
 * @author Abe White
 * @author Pinaki Poddar (added binding query parameter tests) 
 */
public class TestExternalValues
    extends SingleEMFTestCase {

    public void setUp()
        throws Exception {
        super.setUp(CLEAR_TABLES, ExternalValues.class);
    }
    
    public void testPositionalBindingQueryParameterEqualsDeclaredType() {
        UUID uuid = new UUID(1,4);
        createInstance(uuid);
        
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        String jpql = "SELECT p FROM ExternalValues p WHERE p.uuid=?1";
        List<ExternalValues> result = em.createQuery(jpql)
                                        .setParameter(1, uuid)
                                        .getResultList();
        assertFalse(result.isEmpty());
        for (ExternalValues x:result) {
            assertEquals(uuid, x.getUuid());
        }
    }
    
    public void testNamedBindingQueryParameterEqualsDeclaredType() {
        UUID uuid = new UUID(2,4);
        createInstance(uuid);
        
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        String jpql = "SELECT p FROM ExternalValues p WHERE p.uuid=:uuid";
        List<ExternalValues> result = em.createQuery(jpql)
                                        .setParameter("uuid", uuid)
                                        .getResultList();
        assertFalse(result.isEmpty());
        for (ExternalValues pc:result) {
            assertEquals(uuid, pc.getUuid());
        }
    }
    
    public void testPositionalBindingQueryParameterNotEqualsExternalizedType() {
        UUID uuid = new UUID(1,4);
        createInstance(uuid);
        
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        String jpql = "SELECT p FROM ExternalValues p WHERE p.uuid=?1";
        Query query = em.createQuery(jpql)
                        .setParameter(1, uuid.toString());
                                        
        try {
            query.getResultList();
            fail("Expected ArgumentException");
       } catch (ArgumentException ex) {
           // expected
       }
    }

    public void testNamedBindingQueryParameterNotEqualsExternalizedType() {
        UUID uuid = new UUID(2,4);
        createInstance(uuid);
        
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        String jpql = "SELECT p FROM ExternalValues p WHERE p.uuid=:uuid";
        Query query = em.createQuery(jpql)
                        .setParameter("uuid", uuid.toString());
        try {
             query.getResultList();
             fail("Expected ArgumentException");
        } catch (ArgumentException ex) {
            // expected
        }
    }
    
    private void createInstance(UUID uuid) {
    	OpenJPAEntityManagerSPI em = emf.createEntityManager();
        em.getTransaction().begin();
        ExternalValues pc = new ExternalValues();
        pc.setUuid(uuid);
        em.persist(pc);
        em.getTransaction().commit();
        em.clear();
    }
}

