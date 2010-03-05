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
package org.apache.openjpa.persistence.conf;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;

import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

public class TestSwitchConnection extends AbstractPersistenceTestCase {
    private String defaultJndiName = "jdbc/mocked";
    private String[] jndiNames = { "jdbc/mocked1" };
    
    protected void initEMF(String cfName) { 
        EntityManagerFactory emf = getEmf("openjpa.ConnectionFactoryName", cfName);
        
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("Delete from confPerson").executeUpdate();
        em.getTransaction().commit();
        em.close();
        
        emf.close();
    }
    
    protected EntityManagerFactory getEmf(String cfPropertyName, String cfPropertyValue) { 
        // null out the driver to prevent system properties from taking effect.
        // do not set connectionFactoryModeManaged - or connectionFactory2 will be used. 
        return createEMF(
            "openjpa.ConnectionDriverName", "", 
            cfPropertyName, cfPropertyValue,
            Person.class); 
    }
    
    protected EntityManager getEm(EntityManagerFactory emf, String name, String value) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(name, value);
        return emf.createEntityManager(props);
    }
    
    protected void createTables() { 
        // create an EMF for each database;
        initEMF(defaultJndiName);
        initEMF(jndiNames[0]);
    }
    
    public void testConnectionFactoryName() { 
        // split out so that we can try javax.persistence.jtaDataSource in the future. 
        overridePropertyOnEM("openjpa.ConnectionFactoryName", jndiNames[0]);
    }

    public void overridePropertyOnEM(String name, String value) {
        // TODO Disable for non derby. 
        createTables();
        
        // use the default JndiName for the base EntityManagerFactory
        EntityManagerFactory emf = getEmf(name, defaultJndiName);
        assertNotNull(emf);

        EntityManager em = emf.createEntityManager();
        assertNotNull(em);

        EntityManager em1 = getEm(emf, name, value); 
        assertNotNull(em1);

        // 'prove' that we're using a different database by inserting the same row
        em.getTransaction().begin();
        em.persist(new Person(1));
        em.getTransaction().commit();

        em1.getTransaction().begin();
        em1.persist(new Person(1));
        em1.getTransaction().commit();

        em.clear();
        em1.clear();

        // sanity test, make sure inserting the same row again fails.
        
        em.getTransaction().begin();
        em.persist(new Person(1));
        try {
            em.getTransaction().commit();
            fail("Should not be able to commit the same row a second time");
        } catch (RollbackException rbe) {
            assertTrue(rbe.getCause() instanceof EntityExistsException);
            // expected
        }

        em1.getTransaction().begin();
        em1.persist(new Person(1));
        try {
            em1.getTransaction().commit();
            fail("Should not be able to commit the same row a second time");
        } catch (RollbackException rbe) {
            assertTrue(rbe.getCause() instanceof EntityExistsException);
            // expected
        }
        
        em.close();
        em1.close();
        emf.close();
    }
}
