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
package org.apache.openjpa.persistence.spring;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.AutoDetachType;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.models.library.Book;
import org.apache.openjpa.persistence.models.library.Borrower;
import org.apache.openjpa.persistence.models.library.Subject;
import org.apache.openjpa.persistence.models.library.Volunteer;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestLibService extends SingleEMFTestCase
        implements TransactionalEntityManagerFactory {

    private static EnumSet<AutoDetachType> txScope =
            EnumSet.allOf(AutoDetachType.class);

    private LibService service;

    @Override
    public EntityManager getTransactionalEntityManager() {
        // return a transactionally scoped entity manager
        OpenJPAEntityManager em = emf.createEntityManager();
        txScope.remove(AutoDetachType.NONE);
        em.setAutoDetach(txScope);
        return em;
    }

    @BeforeEach
    public void setUp() {
        // declare the library model classes
        super.setUp(DROP_TABLES, Book.class, Borrower.class, Subject.class,
            Volunteer.class);

        // put golden data in database
        LibTestingService libTestingService = new LibTestingService();
        libTestingService.setEntityManager(emf.createEntityManager());
        libTestingService.repopulateDB();
        libTestingService.close();

        // create the LibService
        service = new LibServiceImpl();
        service.setTransactionalEntityManagerFactory(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        service = null;
    }

    /**
     * Using known data, test the LibraryService.findBookByTitle() method and
     * verify the information returned.
     */
    @Test
    void findBookByTitle() {
        String title = "Gone Sailing";
        String qTitle = "\"" + title + "\"";
        String bName = "Dick";

        Assertions.assertDoesNotThrow(() -> {
            // find the book
            Book book = service.findBookByTitle(title);
            assertNotNull(book, "could not find the book " + qTitle);
            assertEquals(title, book.getTitle(),
                "the book found was not the book " + qTitle);

            // get the book's borrower
            Borrower borrower = book.getBorrower();
            assertNotNull(borrower, "could not find the borrower " + bName);
            assertEquals(bName, borrower.getName(),
                "the borrower found was not " + bName);

            // get the borrower's volunteer status
            Volunteer volunteer = borrower.getVolunteer();
            assertNotNull(volunteer,
                "could not find " + bName + "'s volunteer status");
            assertSame(volunteer.getBorrower(), borrower,
                "could not find the reference from " + bName
                    + "'s volunteer status back to " + bName);

            // get the book's subjects
            List<Subject> subjects = book.getSubjects();
            assertNotNull(subjects, "no subjects for the book " + qTitle);
            assertEquals(
                2, subjects.size(),
                "unexpected number of subjects for the book " + qTitle);
        }, "Unable to findBookByTitle");
    }

    /**
     * Using known data, test the LibraryService.findBorrowerByName method and
     * verify the information returned.
     */
    @Test
    void findBorrowerByName() {
        String bName = "Harry";
        Assertions.assertDoesNotThrow(() -> {
            Borrower harry = service.findBorrowerByName(bName);
            assertNotNull(harry, "Could not find " + bName);
            assertEquals(bName, harry.getName(),
                "the borrower found is not " + bName);
        }, "Unable to find borrower by name");
    }

    /**
     * Using known data, test the LibraryService.borrowBook() operation.
     * <ul>
     * <li>Can we find Tom, and has he borrowed one book?</li>
     * <li>Can we find the book entitled "Gone Visiting"?</li>
     * <li>After Tom borrows the new book, has he borrowed two books?</li>
     * </ul>
     */
    @Test
    void borrowBook() {
        String bName = "Tom";
        String title = "Gone Visiting";

        try {
            // find the borrower Tom
            Borrower borrower = service.findBorrowerByName(bName);
            assertNotNull(borrower, "Could not find " + bName);
            List<Book> books = borrower.getBooks();
            assertEquals(1,
                    (books == null ? 0 : borrower.getBooks().size()), bName + " has borrowed an unexpected number of books");

            // find the book "Gone Visiting"
            Book book = service.findBookByTitle(title);
            assertNotNull(book, "Could not find the book " + title);

            // have Tom borrow the book
            service.borrowBook(borrower, book);
            List<Book> borrowedBooks = borrower.getBooks();
            assertEquals(2, borrowedBooks.size(),
                    "Unexpected number of books borrowed");

            // Verify that the update is in the database
            borrower = service.findBorrowerByName(bName);
            assertNotNull(borrower, "Could not find " + bName);
            List<Book> booksBorrowed2 = borrower.getBooks();
            assertEquals(2,
                    booksBorrowed2.size(), bName + " has borrowed an unexpected number of books");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unable to borrow a book");
        }
    }

    /**
     * Test the LibraryService.returnBook() operation, etc.
     */
    @Test
    void returnBook() {
        String bName = "Harry";

        try {
            // find the borrower Harry
            Borrower borrower = service.findBorrowerByName(bName);
            assertNotNull(borrower, "Could not find " + bName);
            List<Book> books = borrower.getBooks();
            assertEquals(1,
                    (books == null ? 0 : borrower.getBooks().size()), bName + " has borrowed an unexpected number of books");

            // find the one book Harry has borrowed
            Book book = borrower.getBooks().get(0);
            service.returnBook(book);

            // Verify that the update is in the database
            borrower = service.findBorrowerByName(bName);
            assertNotNull(borrower, "Could not find " + bName);
            assertEquals(0,
                    borrower.getBooks().size(), bName + " has borrowed an unexpected number of books");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unable to return a book");
        }
    }
}
