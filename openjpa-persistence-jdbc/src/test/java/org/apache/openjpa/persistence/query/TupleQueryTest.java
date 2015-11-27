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
package org.apache.openjpa.persistence.query;

import junit.textui.TestRunner;
import org.apache.openjpa.persistence.simple.NamedEntity;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Tuple;

public class TupleQueryTest
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(NamedEntity.class);

        NamedEntity e = new NamedEntity();
        e.setName("e"); 

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(e);
        em.getTransaction().commit();
        em.close();
    }

    public void testNormalQuery() {
        EntityManager em = emf.createEntityManager();
        try {
            em.createQuery("select e.name from named e", Tuple.class);
            fail();
        } catch (final PersistenceException pe) {
            // ok
        } finally {
            em.close();
        }
    }

    public static void main(String[] args) {
        TestRunner.run(TupleQueryTest.class);
    }
}

