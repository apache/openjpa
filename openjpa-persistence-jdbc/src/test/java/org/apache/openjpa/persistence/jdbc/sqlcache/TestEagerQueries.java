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
import javax.persistence.Query;

import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that if a query generates more than one SQL statements, then it is
 * not cached.
 * 
 * @author Pinaki Poddar
 *
 */

//--------------------------------------------------------------------------
//  Deactivated becuase of schema definition clasing with other
//  commonly named stuff such as Person Book etc.

public abstract class TestEagerQueries extends SingleEMFTestCase {
    public void setUp() {
        super.setUp(DROP_TABLES, 
            Person.class, Author.class, Singer.class,
            Merchandise.class, Book.class, CD.class);
        createTestData();
    }
    
    private void createTestData() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Author a1 = new Author("Author1", "", (short)40, 1960);
        Author a2 = new Author("Author1", "", (short)40, 1960);
        Author a3 = new Author("Author1", "", (short)40, 1960);
        Singer s1 = new Singer("Singer1", "", (short)40, 1960);
        Singer s2 = new Singer("Singer1", "", (short)40, 1960);
        Book   b1 = new Book();
        Book   b2 = new Book();
        CD     c1 = new CD();
        CD     c2 = new CD();
        
        b1.addAuthor(a1);
        b1.addAuthor(a2);
        b2.addAuthor(a2);
        b2.addAuthor(a3);
        c1.setSinger(s1);
        c2.setSinger(s2);
        
        em.persist(a1); em.persist(a2);   em.persist(a3);
        em.persist(s1); em.persist(s2);
        em.persist(b1); em.persist(b2);
        em.persist(c1); em.persist(c2);
        em.getTransaction().commit();
    }
    
    public void testQueryWithLazyRelationIsCached() {
        // Author is lazily related to Book
        String jpql = "select p from Author p";
        EntityManager em = emf.createEntityManager();
        
        Query q1 = em.createQuery(jpql);
        assertEquals(OpenJPAPersistence.cast(q1).getLanguage(), JPQLParser.LANG_JPQL);
        List<Author> authors1 = q1.getResultList();
        assertFalse(authors1.isEmpty());
        Author author1 = authors1.iterator().next();
        em.close(); // nothing will be loaded by chance
        
        assertNull(author1.getBooks());
        
        // do the same thing again, this time query should be cached
        em = emf.createEntityManager();
        Query q2 = em.createQuery(jpql);
        assertEquals(OpenJPAPersistence.cast(q2).getLanguage(), QueryLanguages.LANG_PREPARED_SQL);
        List<Author> authors2 = q2.getResultList();
        assertFalse(authors2.isEmpty());
        Author author2 = authors2.iterator().next();
        em.close();
        
        assertNull(author2.getBooks());
    }
    
    public void testQueryWithEagerRelationIsNotCached() {
        // Book is eagerly related to Author
        String jpql = "select b from Book b";
        EntityManager em = emf.createEntityManager();
        
        Query q1 = em.createQuery(jpql);
        assertEquals(OpenJPAPersistence.cast(q1).getLanguage(), JPQLParser.LANG_JPQL);
        List<Book> books = q1.getResultList();
        assertFalse(books.isEmpty());
        Book book1 = books.iterator().next();
        em.close(); // nothing will be loaded by chance
        
        assertNotNull(book1.getAuthors());
        assertFalse(book1.getAuthors().isEmpty());
        
        // do the same thing again, this time query should not be cached
        // because it requires multiple selects
        em = emf.createEntityManager();
        Query q2 = em.createQuery(jpql);
        assertEquals(OpenJPAPersistence.cast(q2).getLanguage(), JPQLParser.LANG_JPQL);
        List<Book> books2 = q2.getResultList();
        assertFalse(books2.isEmpty());
        Book book2 = books2.iterator().next();
        em.close();
        
        assertNotNull(book2.getAuthors());
        assertFalse(book2.getAuthors().isEmpty());
    }

}
