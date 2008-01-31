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
package org.apache.openjpa.persistence.simple;

import java.io.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import junit.textui.TestRunner;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that a EntityManagerFactory can be used after serialization.
 *
 * @author David Ezzio
 */
public class TestSerializedFactory
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(AllFieldTypes.class);
    }

    public void testSerializedEntityManagerFactory() throws Exception {
        // serialize and deserialize the entity manager factory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(emf);
        EntityManagerFactory emf2 = 
            (EntityManagerFactory) new ObjectInputStream(
            new ByteArrayInputStream(baos.toByteArray())).readObject();
            
        // use the deserialized entity manager factory
        assertTrue("The deserialized entity manager factory is not open",
            emf2.isOpen());
        EntityManager em = emf2.createEntityManager();
        assertTrue("The newly created entity manager is not open", em.isOpen());

        // exercise the entity manager produced from the deserialized EMF
        em.getTransaction().begin();
        em.persist(new AllFieldTypes());
        em.getTransaction().commit();
        
        // close the extra resources
        em.close();
        assertFalse("The entity manager is not closed", em.isOpen());
        emf2.close();
        assertFalse("The entity manager factory is not closed", emf2.isOpen());
    }
    
    public static void main(String[] args) {
        TestRunner.run(TestSerializedFactory.class);
    }
}

