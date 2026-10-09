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

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestQueryEscapeCharacters
    extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(Employee.class, CLEAR_TABLES,
            "openjpa.jdbc.QuerySQLCache", "true");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Employee e = new Employee();
        e.setName("Mike Dick");
        e.setEmpId(1);
        em.persist(e);

        e = new Employee();
        e.setName("Mike Jones");
        e.setEmpId(2);
        em.persist(e);

        e = new Employee();
        e.setName("Mike Smith");
        e.setEmpId(3);
        em.persist(e);

        e = new Employee();
        e.setName("M%ke Smith");
        e.setEmpId(4);
        em.persist(e);
        em.getTransaction().commit();
        em.close();
    }

    @Override
    public void tearDown() throws Exception {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("Delete from Employee").executeUpdate();
        em.getTransaction().commit();
        em.close();
        super.tearDown();
    }

    public void testNormalQuery() {
        performFind ("Employee.findByName", "%Dick", 1);
    }

    public void testMultiResultQuery() {
        performFind ("Employee.findByName", "Mike%", 3);
    }

    public void testEscapedQuery() {
        performFind ("Employee.findByNameEscaped",
                "M\\%%", 1);
    }

    public void testDoubleEscapedQuery() {
        performFind ("Employee.findByName", "\\\\", 0);
    }

    public void testWrongEscape() {
        performFind ("Employee.findByName", "M|%%", 0);
    }

    public void testDoubleSlashQuery() {
        // a LIKE without an ESCAPE clause has no escape character on any
        // database, so this is a pattern of one ordinary backslash
        performFind ("Employee.findByName", "\\", 0);
    }

    /**
     * The dictionary escape character is only used to render an ESCAPE
     * clause; it never turns a plain pattern character into an escape
     * (OPENJPA-3009). The dictionary is flagged as needing an explicit
     * ESCAPE clause so that the generated clause is exercised on every
     * database, not only on the ones which set the flag themselves.
     */
    @SuppressWarnings("unchecked")
    public void testDifferentEscapeCharacter () {
        OpenJPAEntityManagerFactorySPI ojpaEmf = emf;
        JDBCConfiguration conf = (JDBCConfiguration)ojpaEmf.getConfiguration();
        DBDictionary dict = conf.getDBDictionaryInstance();

        String escape = dict.searchStringEscape;
        String noEscape = dict.searchStringNoEscape;
        boolean requiresEscape = dict.requiresSearchStringEscapeForLike;

        // Would be nice to just pass a map to the createEntityManager, but
        // seems like it would be too much trouble to get the proper DB type
        // and then build the string for the map.
        dict.searchStringEscape = "|";
        dict.requiresSearchStringEscapeForLike = true;
        // a character which cannot occur in the patterns below; the empty
        // string literal is not accepted by every database
        dict.searchStringNoEscape = "'~'";
        EntityManager em = emf.createEntityManager();

        String unnamedQuery =
            "Select e from Employee e where e.name LIKE :name";
        try {
            Query q = em.createNamedQuery("Employee.findByName");
            q.setParameter("name", "M|%%");
            List<Employee> emps = q.getResultList();
            assertEquals(0, emps.size());

            q = em.createQuery(unnamedQuery);
            q.setParameter("name", "M|%%");
            emps = q.getResultList();
            assertEquals(0, emps.size());

            // an ESCAPE clause in the query still escapes
            q = em.createQuery(unnamedQuery + " ESCAPE '|'");
            q.setParameter("name", "M|%%");
            emps = q.getResultList();
            assertEquals(1, emps.size());
        } finally {
            em.close();
            dict.searchStringEscape = escape;
            dict.searchStringNoEscape = noEscape;
            dict.requiresSearchStringEscapeForLike = requiresEscape;
        }
    }

    /**
     * The SQL generated for a parameterized LIKE without an ESCAPE clause
     * does not depend on the pattern, so re-executing the query with another
     * pattern from the prepared query cache stays correct (OPENJPA-3009).
     */
    public void testCachedSqlDoesNotDependOnThePattern() {
        String jpql = "Select e from Employee e where e.name LIKE :name";

        EntityManager em = emf.createEntityManager();
        try {
            for (int i = 0; i < 3; i++) {
                Query q = em.createQuery(jpql);
                q.setParameter("name", "Mike%");
                assertEquals("run " + i, 3, q.getResultList().size());

                q = em.createQuery(jpql);
                q.setParameter("name", "%Dick");
                assertEquals("run " + i, 1, q.getResultList().size());

                // a backslash is an ordinary character, so this matches
                // nothing rather than 'M' plus any two characters
                q = em.createQuery(jpql);
                q.setParameter("name", "M\\%%");
                assertEquals("run " + i, 0, q.getResultList().size());
            }
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    private void performFind (String namedQuery, String parameter,
            int expected) {
        EntityManager em = emf.createEntityManager();

        Query q = em.createNamedQuery(namedQuery);
        q.setParameter("name", parameter);
        List<Employee> emps = q.getResultList();
        assertEquals(expected, emps.size());

        String unnamedQuery =
            "Select e from Employee e where e.name LIKE :name";
        if (namedQuery.equals("Employee.findByNameEscaped")) {
            unnamedQuery =
                "Select e from Employee e where e.name LIKE :name ESCAPE '\\'";
        }
        q = em.createQuery(unnamedQuery);
        q.setParameter("name", parameter);
        emps = q.getResultList();
        assertEquals(expected, emps.size());
        em.close();
    }
}
