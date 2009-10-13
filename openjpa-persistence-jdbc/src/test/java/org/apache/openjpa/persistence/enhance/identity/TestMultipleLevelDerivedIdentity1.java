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
package org.apache.openjpa.persistence.enhance.identity;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;


/**
 * This is a variation of TestMultipleLevelDerivedIdentity Using
 * MapsId annotations.
 * @author Fay Wang
 * 
 */
@SuppressWarnings("unchecked")
public class TestMultipleLevelDerivedIdentity1 extends SingleEMFTestCase {
	private static String LIBRARY_NAME = "LIB";
	private static String BOOK_NAME    = "foo";
	private static int    NUM_PAGES    = 3;
    public void setUp() throws Exception {
        super.setUp(CLEAR_TABLES, Library1.class, Book1.class, Page1.class,
            BookId1.class, PageId1.class,
            "openjpa.RuntimeUnenhancedClasses", "unsupported");
        create();
    }
    
	public void testPersist() {
		create();
	}

	public void testQueryRootLevel() {
		EntityManager em = emf.createEntityManager();
		List<Library1> list = em.createQuery("SELECT p FROM Library1 p")
							   .getResultList();
		assertFalse(list.isEmpty());
		Library1 lib = (Library1) list.get(0);
		BookId1 bid = new BookId1(BOOK_NAME, lib.getName());
		Book1 b = lib.getBook(bid);
		assertNotNull(b);
		
		Page1 p = b.getPage(new PageId1(1, bid));
		assertNotNull(p);
	}
	
	public void testQueryIntermediateLevel() {
		EntityManager em = emf.createEntityManager();
		List<Book1> list = em.createQuery("SELECT p FROM Book1 p")
							   .getResultList();
		assertFalse(list.isEmpty());
		Book1 book = list.get(0);
		Library1 lib = book.getLibrary();
		for (int i=1; i<=NUM_PAGES; i++) {
			PageId1 pid = new PageId1(i, book.getBid());
			Page1 page = book.getPage(pid);
			assertNotNull(page);
			assertEquals(book, page.getBook());
			assertEquals(lib, page.getBook().getLibrary());
			assertEquals(page, page.getBook().getPage(
				new PageId1(pid.getNumber(), book.getBid())));
		}
	}
	
	public void testQueryLeafLevel() {
		EntityManager em = emf.createEntityManager();
		List<Page1> list = em.createQuery("SELECT p FROM Page1 p")
							   .getResultList();
		assertFalse(list.isEmpty());
		Book1 book = list.get(0).getBook();
		Library1 lib = book.getLibrary();
		for (Page1 page : list) {
			assertEquals(book, page.getBook());
			assertEquals(lib, page.getBook().getLibrary());
			assertEquals(page, page.getBook().
				getPage(page.getPid()));
		}
	}

	public void testFindRootNode() {
		EntityManager em = emf.createEntityManager();
		Library1 lib = em.find(Library1.class, LIBRARY_NAME);
		assertNotNull(lib);
		BookId1 bid = new BookId1(BOOK_NAME, lib.getName());
		Book1 b = lib.getBook(bid);
		assertNotNull(b);
		PageId1 pid = new PageId1(1, bid);
		assertNotNull(b.getPage(pid));
	}
	
	public void testFindIntermediateNode() {
		EntityManager em = emf.createEntityManager();
		
		BookId1 bookId = new BookId1();
		bookId.setLibrary(LIBRARY_NAME);
		bookId.setName(BOOK_NAME);
		Book1 book = em.find(Book1.class, bookId);
		assertNotNull(book);
	}
	
	public void testFindLeafNode() {
		EntityManager em = emf.createEntityManager();
		
		BookId1 bookId = new BookId1();
		bookId.setLibrary(LIBRARY_NAME);
		bookId.setName(BOOK_NAME);
		PageId1 pageId = new PageId1();
		pageId.setBook(bookId);
		pageId.setNumber(2);
		Page1 page = em.find(Page1.class, pageId);
		assertNotNull(page);
	}
	
	public void testUpdate() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		BookId1 bookId = new BookId1();
		bookId.setLibrary(LIBRARY_NAME);
		bookId.setName(BOOK_NAME);
		Book1 book = em.find(Book1.class, bookId);
		assertNotNull(book);
		book.setAuthor("modifiy Author");
		em.getTransaction().commit();
	}
	
	public void testDeleteRoot() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Library1 lib = em.find(Library1.class, LIBRARY_NAME);
		em.remove(lib);
		em.getTransaction().commit();
		
	    assertEquals(0, count(Library1.class));
	    assertEquals(0, count(Book1.class));
	    assertEquals(0, count(Page1.class));
	}
	
	public void testDeleteLeafObtainedByQuery() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
        Page1 page = (Page1)em.createQuery(
                "SELECT p FROM Page1 p WHERE p.pid.number=2")
			.getSingleResult();
		assertNotNull(page);
		em.remove(page);
		em.getTransaction().commit();
		
	    assertEquals(1, count(Library1.class));
	    assertEquals(1, count(Book1.class));
	    assertEquals(NUM_PAGES-1, count(Page1.class));
	}
	
	public void testDeleteLeafObtainedByFind() {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		BookId1 bookId = new BookId1();
		bookId.setLibrary(LIBRARY_NAME);
		bookId.setName(BOOK_NAME);
		PageId1 pageId = new PageId1();
		pageId.setBook(bookId);
		pageId.setNumber(2);
		Page1 page = em.find(Page1.class, pageId);
		assertNotNull(page);
		em.remove(page);
		em.getTransaction().commit();
		
	    assertEquals(1, count(Library1.class));
	    assertEquals(1, count(Book1.class));
	    assertEquals(NUM_PAGES-1, count(Page1.class));
	}

	
	/**
	 * Create a Library with a Book and three Pages.
	 */
	public void create() {
		if (count(Library1.class) > 0)
			return;
		
		EntityManager em = null;
		em = emf.createEntityManager();
		em.getTransaction().begin();
		
		Library1 lib = new Library1();
		lib.setName(LIBRARY_NAME);

		Book1 book = new Book1();
		BookId1 bid = new BookId1();
		bid.setName(BOOK_NAME);
		bid.setLibrary(lib.getName());
		book.setBid(bid);
		lib.addBook(book);
		for (int i = 1; i <= NUM_PAGES; i++) {
			Page1 page = new Page1();
			PageId1 pid = new PageId1(i, bid);
			page.setPid(pid);
			book.addPage(page);
		}
		em.persist(lib);
		em.getTransaction().commit();

		em.clear();
	}
}
