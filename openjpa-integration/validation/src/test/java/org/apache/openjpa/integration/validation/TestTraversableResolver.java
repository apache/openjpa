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
package org.apache.openjpa.integration.validation;

import java.lang.annotation.ElementType;
import java.util.HashMap;
import java.util.Map;

import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.TraversableResolver;

import junit.framework.TestCase;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.validation.TraversableResolverImpl;

import org.hibernate.validator.engine.PathImpl;

/**
 * Test the TraversableResolver methods
 * 
 * First run several testcases from a user perspective. These test the methods
 * indirectly:
 *    1) testLoadedTitle()
 *    2} testUnloaded()
 *    3) testCascading()
 * 
 * Then test the methods directly:
 *    1) testPages()
 *    2) testTitle
 */
public class TestTraversableResolver extends TestCase {
    private static OpenJPAEntityManagerFactorySPI emf = null;
    private OpenJPAEntityManager em;
    private Book book;
    
    /**
     * Create a book with a title that is too long, and the embedded
     * publisher has a name that is also too long. However, use a
     * persistence unit with validation-mode set to NONE. Therefore,
     * the create should be successful. This is to setup a situation
     * where fields to be potentially validated are not necessarily loaded.
     */
    @Override
    public void setUp() {
        createEMF("non-validation-pu", "SchemaAction='drop,add')");
        createBook(1, "long title", 234);
        emf.close();
    }
    
    private void createEMF(String pu, String schemaAction) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true,"
            + schemaAction);
        emf = (OpenJPAEntityManagerFactorySPI)OpenJPAPersistence.createEntityManagerFactory(
            pu,
            "org/apache/openjpa/integration/validation/persistence.xml",
            map);
        assertNotNull(emf);
    }
    
    /**
     * By default, the title is not loaded. Make sure it gets loaded,
     * make a change in a different field, and commit. The isLoaded() method
     * of the TraversableResolver should return true, resulting in a validation
     * being performed and a ConstraintViolationException should be returned
     * because the title is too long.
     */
    public void testLoadedTitle() {
        createEMF("validation-pu", "SchemaAction='add')");
        em = emf.createEntityManager();
        em.getTransaction().begin();
        book = em.find(org.apache.openjpa.integration.validation.Book.class, 1);
        assertNotNull(book);
        book.setPages(124);
        // load the title
        String title = book.getTitle();
        assertEquals("long title", title);
        boolean exceptionCaught = false;
        try {
            em.getTransaction().commit();
        } catch (ConstraintViolationException e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
        em.close();
        emf.close();
    }
    
    /**
     * By default, the title and publisher are not loaded. Make a change in a different field
     * and commit. The isLoaded() method of the TraversableResolver should return
     * false for both of these. Therefore a validation should not be performed. 
     * The commit should succeed with no exception.
     */
    public void testUnloaded() {
        createEMF("non-validation-pu", "SchemaAction='add')");
        em = emf.createEntityManager();
        em.getTransaction().begin();
        book = em.find(org.apache.openjpa.integration.validation.Book.class, 1);
        assertNotNull(book);
        book.setPages(124);
        boolean exceptionCaught = false;
        try {
            em.getTransaction().commit();
        } catch (ConstraintViolationException e) {
            exceptionCaught = true;
        }
        assertFalse(exceptionCaught);
        em.close();
        emf.close();
    }
    
    /**
     * By default, the publisher is not loaded. Make sure it gets loaded.
     * The isLoaded() and isCascadable() methods should both return true,
     * resulting in a validation being performed. A ConstraintViolation
     * should be thrown since the publisher name is too long.
     */
    public void testCascading() {
        createEMF("validation-pu", "SchemaAction='add')");
        em = emf.createEntityManager();
        em.getTransaction().begin();
        book = em.find(org.apache.openjpa.integration.validation.Book.class, 1);
        assertNotNull(book);
        book.setPages(124);
        // load the embedded publisher
        Publisher publisher = book.getPublisher();
        assertNotNull(publisher);
        publisher.setPublisherID("yyy");
        boolean exceptionCaught = false;
        try {
            em.getTransaction().commit();
        } catch (Exception e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
        em.close();
        emf.close();
    }
    
    /**
     * Test the isReachable() and isCascadable() methods on the pages element of Book,
     * which is eagerly fetched by default. 
     */
    public void testPages() {
        createEMF("validation-pu", "SchemaAction='add')");
        em = emf.createEntityManager();
        em.getTransaction().begin();
        book = em.find(org.apache.openjpa.integration.validation.Book.class, 1);
        assertNotNull(book);
        PathImpl path = PathImpl.createPathFromString("org.apache.openjpa.integration.validation.Book.pages");
        Path.Node node = path.getLeafNode();
        TraversableResolver tr = new TraversableResolverImpl();
        assertTrue(tr.isReachable(book, node, Book.class, null, ElementType.METHOD));
        assertTrue(tr.isCascadable(book, node, Book.class, null, ElementType.METHOD));
        em.getTransaction().commit();
        em.close();
        emf.close();
    }
    
    /**
     * Test the isReachable() method on the title.
     * It is configured with fetch=FetvhType.LAZY.
     */
    public void testTitle() {
        createEMF("validation-pu", "SchemaAction='add')");
        em = emf.createEntityManager();
        em.getTransaction().begin();
        book = em.find(org.apache.openjpa.integration.validation.Book.class, 1);
        assertNotNull(book);
        PathImpl path = PathImpl.createPathFromString("org.apache.openjpa.integration.validation.Book.title");
        Path.Node node = path.getLeafNode();
        TraversableResolver tr = new TraversableResolverImpl();
        assertFalse(tr.isReachable(book, node, Book.class, null, ElementType.FIELD));
        em.getTransaction().commit();
        em.close();
        emf.close();
    }
    
    private void createBook(int id, String title, int pages) {
        em = emf.createEntityManager();
        book = new Book(id);
        book.setTitle(title);
        book.setPages(pages);
        Publisher publisher = new Publisher();
        publisher.setName("long name");
        publisher.setPublisherID("xxx");
        book.setPublisher(publisher);
        em.getTransaction().begin();
        em.persist(book);
        em.getTransaction().commit();
        em.close();
    }
}
