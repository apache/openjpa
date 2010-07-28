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

import org.apache.openjpa.persistence.ArgumentException;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

public class TestOverrideNonJtaDataSource extends AbstractPersistenceTestCase {
    private String defaultJndiName = "jdbc/mocked";
    private String[] jndiNames = { "jdbc/mocked1" };

    protected void init(String cfName) {
        EntityManagerFactory emf = getEmf("openjpa.ConnectionFactoryName", cfName, true);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("Delete from confPerson").executeUpdate();
        em.getTransaction().commit();
        em.close();
        emf.close();
    }

    protected void setUp() {
        // create an EMF for each database.
        init(defaultJndiName);
        init(jndiNames[0]);
    }
    
    protected EntityManagerFactory getEmf(String cfPropertyName, String cfPropertyValue) {
        return getEmf(cfPropertyName, cfPropertyValue, false);
    }

    protected EntityManagerFactory getEmf(String cfPropertyName, String cfPropertyValue, boolean syncMappings) {
        // null out the driver to prevent system properties from taking effect.
        if (syncMappings) {
            return createEMF(
                "openjpa.jdbc.SynchronizeMappings", "buildSchema",
                "openjpa.ConnectionDriverName", "",
                "openjpa.ConnectionFactoryMode", "managed",
                "openjpa.ConnectionFactoryName", defaultJndiName,  // must have a cf1, to initialize configuration
                cfPropertyName,cfPropertyValue, 
                Person.class);
        }
        return createEMF(
            "openjpa.ConnectionDriverName", "", 
            "openjpa.ConnectionFactoryMode", "managed",
            "openjpa.ConnectionFactoryName", defaultJndiName, // must have a cf1, to initialize configuration
            cfPropertyName,cfPropertyValue, 
            Person.class);
    }

    protected EntityManager getEm(EntityManagerFactory emf, String name, String value) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(name, value);
        return emf.createEntityManager(props);
    }

    public String getPersistenceUnitName() {
        return "TestCfSwitching";
    }

    public void testConnectionFactoryName() {
        // TODO Disable for non derby.
        // split out so that we can try javax.persistence.jtaDataSource in the future.
        overridePropertyOnEM("openjpa.ConnectionFactory2Name", jndiNames[0]);
    }

    public void testJtaDataSource() {
        // TODO Disable for non derby.
        // split out so that we can try javax.persistence.jtaDataSource in the future.
        overridePropertyOnEM("javax.persistence.nonJtaDataSource", jndiNames[0]);
    }

    public void overridePropertyOnEM(String name, String value) {
        // use the default JndiName for the base EntityManagerFactory
        EntityManagerFactory emf = getEmf(name, defaultJndiName);
        assertNotNull(emf);

        EntityManager em = emf.createEntityManager();
        assertNotNull(em);

        EntityManager em1 = getEm(emf, name, value);
        assertNotNull(em1);

        // 'prove' that we're using a different database by inserting the same row
        em.getTransaction().begin();
        em.persist(new Person(1, "em"));
        em.getTransaction().commit();

        em1.getTransaction().begin();
        em1.persist(new Person(1, "em1"));
        em1.getTransaction().commit();

        em.clear();
        em1.clear();

        Person p = em.find(Person.class, 1);
        Person p1 = em1.find(Person.class, 1);
        assertNotSame(p, p1);
        assertEquals("em", p.getName());
        assertEquals("em1", p1.getName());

        em.clear();
        em1.clear();

        // make sure inserting the same row again fails.
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

    public void testInvalidCfName() throws Exception {
        // ensure EM creation fails - when provided an invalid JNDI name
        EntityManagerFactory emf = null;
        try {
            emf = getEmf("openjpa.ConnectionFactory2Name", defaultJndiName);
            getEm(emf, "openjpa.ConnectionFactory2Name", "jdbc/NotReal");
            fail("Expected an excepton when creating an EM with a bogus JNDI name");
        } catch (ArgumentException e) {
            assertTrue(e.isFatal());
            System.out.println(e);
            assertTrue(e.getMessage().contains("jdbc/NotReal")); // ensure failing JNDI name is in the message
            assertTrue(e.getMessage().contains("EntityManager")); // ensure where the JNDI name came from is in message
        }
    }
    
    public void testDataCache() { 
        EntityManagerFactory emf = null;
    
        emf = getEmf("openjpa.DataCache", "true");
        try {
            getEm(emf, "openjpa.ConnectionFactoryName", "jdbc/NotReal");
            fail("Expected an excepton when creating an EM with a bogus JNDI name");
        } catch (ArgumentException e) {
            assertTrue(e.isFatal());
            assertTrue(e.getMessage().contains("jdbc/NotReal")); 
            assertTrue(e.getMessage().contains("L2 Cache")); 
        }
    }
    
    public void testQueryCache() { 
        EntityManagerFactory emf = null;
    
        emf = getEmf("openjpa.QueryCache", "true");
        try {
            getEm(emf, "openjpa.ConnectionFactoryName", "jdbc/NotReal");
            fail("Expected an excepton when creating an EM with a bogus JNDI name");
        } catch (ArgumentException e) {
            assertTrue(e.isFatal());
            assertTrue(e.getMessage().contains("jdbc/NotReal")); 
            assertTrue(e.getMessage().contains("openjpa.QueryCache")); 
        }
    }
    
    public void testSyncMappings() { 
        EntityManagerFactory emf = null;
    
        emf = getEmf("openjpa.jdbc.SynchronizeMappings", "buildSchema");
        try {
            getEm(emf, "openjpa.ConnectionFactoryName", "jdbc/NotReal");
            fail("Expected an excepton when creating an EM with a bogus JNDI name");
        } catch (ArgumentException e) {
            assertTrue(e.isFatal());
            assertTrue(e.getMessage().contains("jdbc/NotReal")); 
            assertTrue(e.getMessage().contains("openjpa.jdbc.SynchronizeMappings")); 
        }
    }
}
