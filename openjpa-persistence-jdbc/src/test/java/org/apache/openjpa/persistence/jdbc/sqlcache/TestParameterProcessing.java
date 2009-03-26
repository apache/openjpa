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
package org.apache.openjpa.persistence.jdbc.sqlcache;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

@AllowFailure
public class TestParameterProcessing extends SingleEMFTestCase {
    public void setUp() {
        super.setUp(DROP_TABLES, Person.class, Address.class);
        createTestData();
    }
    
    private void createTestData() {
        if (count(Person.class)>0) return;
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Person p1 = new Person("John", "Doe", (short)45, 1964);
        Person p2 = new Person("John", "Doe", (short)42, 1967);
        Person p3 = new Person("Harry", "Doe", (short)12, 1995);
        Person p4 = new Person("Barry", "Doe", (short)22, 1985);
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);
        em.persist(p4);
        em.getTransaction().commit();
    }
    
    public void testPositional() {
        String jpql = "select p from Person p where p.firstName=?1" +
                      " and p.lastName='Doe' and p.age > ?2";
        EntityManager em = emf.createEntityManager();
        
        OpenJPAQuery q1 = OpenJPAPersistence.cast(em.createQuery(jpql));
        assertEquals(JPQLParser.LANG_JPQL, q1.getLanguage());
        
        List result1 = q1.setParameter(1, "John")
                       .setParameter(2, (short)40)
                       .getResultList();
        
        assertEquals(2, result1.size());
        
        OpenJPAQuery q2 = OpenJPAPersistence.cast(em.createQuery(jpql));
        assertEquals(QueryLanguages.LANG_PREPARED_SQL, q2.getLanguage());
        List result2 = q2.setParameter(1, "Harry")
                  .setParameter(2, (short)10)
                  .getResultList();
        
        assertEquals(1, result2.size());
    }
    
    public void testNamed() {
        String jpql = "select p from Person p where p.firstName=:first" +
                      " and p.lastName='Doe' and p.age > :age";
        EntityManager em = emf.createEntityManager();
        
        OpenJPAQuery q1 = OpenJPAPersistence.cast(em.createQuery(jpql));
        assertEquals(JPQLParser.LANG_JPQL, q1.getLanguage());
        
        List result1 = q1.setParameter("first", "John")
                       .setParameter("age", (short)40)
                       .getResultList();
        
        assertEquals(2, result1.size());
        
        OpenJPAQuery q2 = OpenJPAPersistence.cast(em.createQuery(jpql));
        assertEquals(QueryLanguages.LANG_PREPARED_SQL, q2.getLanguage());
        List result2 = q2.setParameter("first", "Barry")
                  .setParameter("age", (short)20)
                  .getResultList();
        
        assertEquals(1, result2.size());
    }
    
    public void testWrongParameterValueTypeThrowException() {
        String jpql = "select p from Person p where p.firstName=:first" 
                    + " and p.age > :age";
        EntityManager em = emf.createEntityManager();

        OpenJPAQuery q1 = OpenJPAPersistence.cast(em.createQuery(jpql));
        try {
            List result1 = q1.setParameter("first", (short)40)
                             .setParameter("age", "John")
                             .getResultList();
            fail("Expected to fail with wrong parameter value");
        } catch (IllegalArgumentException e) {
            // good
        }
    }
    
    public void testNullParameterValueForPrimitiveTypeThrowsException() {
        String jpql = "select p from Person p where p.firstName=:first" 
                    + " and p.age > :age";
        EntityManager em = emf.createEntityManager();

        OpenJPAQuery q1 = OpenJPAPersistence.cast(em.createQuery(jpql));
        try {
            List result1 = q1.setParameter("first", "John")
                             .setParameter("age", null)
                             .getResultList();
            fail("Expected to fail with null parameter value for primitives");
        } catch (RuntimeException e) {
            // good
        }
    }

}
