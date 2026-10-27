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
package org.apache.openjpa.persistence.jpql.expressions;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.H2Dictionary;
import org.apache.openjpa.jdbc.sql.SybaseDictionary;
import org.apache.openjpa.kernel.Query;
import org.apache.openjpa.kernel.QueryImpl;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.common.apps.Address;
import org.apache.openjpa.persistence.common.apps.CompUser;
import org.apache.openjpa.persistence.common.apps.FemaleUser;
import org.apache.openjpa.persistence.common.apps.MaleUser;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;

public class TestInMemoryScalarExpressions extends AbstractTestCase {

    private int userid1, userid2, userid3, userid4, userid5, userid6;

    /**
     * Some databases trim the whitespace from a string upon insert. Store Shannon's name for
     * asserts later in the testcase.
     */
    private String expectedShannonName = "Shannon ";

    public TestInMemoryScalarExpressions(String name) {
        super(name, "jpqlclausescactusapp");
    }

    @Override
    public void setUp() {
        deleteAll(CompUser.class);
        OpenJPAEntityManagerSPI em = (OpenJPAEntityManagerSPI) currentEntityManager();
        startTx(em);

        Address[] add = new Address[]{
            new Address("43 Sansome", "SF", "United-Kingdom", "94104"),
            new Address("24 Mink", "ANTIOCH", "USA", "94513"),
            new Address("23 Ogbete", "CoalCamp", "NIGERIA", "00000"),
            new Address("10 Wilshire", "Worcester", "CANADA", "80080"),
            new Address("23 Bellflower", "Ogui", null, "02000"),
            new Address("22 Montgomery", "SF", null, "50054") };

        CompUser user1 = createUser("Seetha", "MAC", add[0], 36, true);
        CompUser user2 = createUser("Shannon ", "PC", add[1], 36, false);
        CompUser user3 = createUser("Ugo", "PC", add[2], 19, true);
        CompUser user4 = createUser("_Jacob", "LINUX", add[3], 10, true);
        CompUser user5 = createUser("Famzy", "UNIX", add[4], 29, false);
        CompUser user6 = createUser("Shade", "UNIX", add[5], 23, false);

        em.persist(user1);
        userid1 = user1.getUserid();
        em.persist(user2);
        userid2 = user2.getUserid();
        em.persist(user3);
        userid3 = user3.getUserid();
        em.persist(user4);
        userid4 = user4.getUserid();
        em.persist(user5);
        userid5 = user5.getUserid();
        em.persist(user6);
        userid6 = user6.getUserid();

        DBDictionary dict = ((JDBCConfiguration) em.getConfiguration()).getDBDictionaryInstance();
        if(dict instanceof SybaseDictionary) {
            expectedShannonName="Shannon";
        }

        endTx(em);
        endEm(em);
    }

    /**
     * OPENJPA-2956: NULLS FIRST and NULLS LAST were ignored in memory, which
     * left two of the four combinations with the wrong order.
     */
    public void testNullPrecedenceInMemory() {
        EntityManager em = currentEntityManager();
        List rsall = em.createQuery("SELECT e from CompUser e").getResultList();

        // two users have a null country
        assertNull(first(em, rsall,
            "SELECT e.address.country FROM CompUser e"
                + " ORDER BY e.address.country ASC NULLS FIRST"));
        assertNotNull(first(em, rsall,
            "SELECT e.address.country FROM CompUser e"
                + " ORDER BY e.address.country ASC NULLS LAST"));
        assertNull(first(em, rsall,
            "SELECT e.address.country FROM CompUser e"
                + " ORDER BY e.address.country DESC NULLS FIRST"));
        assertNotNull(first(em, rsall,
            "SELECT e.address.country FROM CompUser e"
                + " ORDER BY e.address.country DESC NULLS LAST"));

        endEm(em);
    }

    /**
     * OPENJPA-2956: a set operation cannot be evaluated in memory, and must
     * say so rather than return a wrong result.
     */
    public void testSetOperationInMemoryIsRejected() {
        EntityManager em = currentEntityManager();
        List rsall = em.createQuery("SELECT e from CompUser e").getResultList();

        org.apache.openjpa.persistence.QueryImpl q1 =
            (org.apache.openjpa.persistence.QueryImpl) em.createQuery(
                "SELECT e.name FROM CompUser e WHERE e.age > 25"
                    + " UNION SELECT e.name FROM CompUser e WHERE e.age > 30");
        try {
            ((QueryImpl) q1.getDelegate()).setCandidateCollection(rsall);
            q1.getResultList();
            fail("a set operation must not be evaluated in memory");
        } catch (RuntimeException e) {
            StringBuilder sb = new StringBuilder();
            for (Throwable t = e; t != null; t = t.getCause()) {
                sb.append(t.getMessage()).append(' ');
            }
            String msg = sb.toString();
            assertTrue("must be rejected, not merely fail: " + msg,
                msg.contains("cannot be evaluated in-memory"));
        } finally {
            endEm(em);
        }
    }

    /**
     * OPENJPA-3042: EXTRACT(WEEK) evaluated in memory returned the day of the
     * week instead of the ISO-8601 week of the year.
     */
    public void testExtractWeekInMemory() {
        EntityManager em = currentEntityManager();
        List rsall = em.createQuery("SELECT e from CompUser e").getResultList();

        // a Monday, first day of ISO week 1
        assertExtract(em, rsall, "WEEK", LocalDate.of(2024, 1, 1), 1);
        // a Sunday, still in ISO week 52 of 2023
        assertExtract(em, rsall, "WEEK", LocalDate.of(2023, 12, 31), 52);
        // a Sunday, in ISO week 53 of 2020
        assertExtract(em, rsall, "WEEK", LocalDate.of(2021, 1, 3), 53);
        assertExtract(em, rsall, "WEEK", LocalDate.of(2006, 3, 21), 12);

        assertExtract(em, rsall, "WEEK", LocalDateTime.of(2021, 1, 3, 23, 59), 53);
        assertExtract(em, rsall, "WEEK",
            OffsetDateTime.of(2021, 1, 3, 23, 59, 0, 0, ZoneOffset.ofHours(5)), 53);
        assertExtract(em, rsall, "WEEK", java.sql.Date.valueOf("2021-01-03"), 53);
        assertExtract(em, rsall, "WEEK", Timestamp.valueOf("2021-01-03 23:59:00"), 53);

        endEm(em);
    }

    /**
     * OPENJPA-3042: every field of EXTRACT evaluated in memory, for each
     * temporal type a persistent attribute or a parameter can hold.
     */
    public void testExtractFieldsInMemory() {
        EntityManager em = currentEntityManager();
        List rsall = em.createQuery("SELECT e from CompUser e").getResultList();

        LocalDateTime ldt = LocalDateTime.of(2021, 11, 3, 18, 19, 23, 250_000_000);
        Object[] values = {
            ldt,
            ldt.atOffset(ZoneOffset.ofHours(-7)),
            Timestamp.valueOf(ldt),
            new java.util.Date(Timestamp.valueOf(ldt).getTime()),
            GregorianCalendar.from(ldt.atZone(ZoneId.systemDefault())),
            ldt.atZone(ZoneId.systemDefault()).toInstant(),
        };
        for (Object value : values) {
            assertExtract(em, rsall, "YEAR", value, 2021);
            assertExtract(em, rsall, "QUARTER", value, 4);
            assertExtract(em, rsall, "MONTH", value, 11);
            assertExtract(em, rsall, "WEEK", value, 44);
            assertExtract(em, rsall, "DAY", value, 3);
            assertExtract(em, rsall, "HOUR", value, 18);
            assertExtract(em, rsall, "MINUTE", value, 19);
            assertExtract(em, rsall, "SECOND", value, 23.25f);
        }

        // a calendar is read in its own time zone
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2021, Calendar.JANUARY, 3, 23, 30, 0);
        assertExtract(em, rsall, "DAY", cal, 3);
        assertExtract(em, rsall, "HOUR", cal, 23);

        LocalDate ld = LocalDate.of(2021, 11, 3);
        for (Object value : new Object[] { ld, java.sql.Date.valueOf(ld) }) {
            assertExtract(em, rsall, "YEAR", value, 2021);
            assertExtract(em, rsall, "QUARTER", value, 4);
            assertExtract(em, rsall, "MONTH", value, 11);
            assertExtract(em, rsall, "WEEK", value, 44);
            assertExtract(em, rsall, "DAY", value, 3);
        }

        LocalTime lt = LocalTime.of(18, 19, 23, 500_000_000);
        assertExtract(em, rsall, "HOUR", lt, 18);
        assertExtract(em, rsall, "MINUTE", lt, 19);
        assertExtract(em, rsall, "SECOND", lt, 23.5f);
        assertExtract(em, rsall, "SECOND", java.sql.Time.valueOf("18:19:23"), 23f);

        // EXTRACT of null is null
        org.apache.openjpa.persistence.QueryImpl q1 =
            (org.apache.openjpa.persistence.QueryImpl) em.createQuery(
                "SELECT EXTRACT(WEEK FROM :d) FROM CompUser e");
        ((QueryImpl) q1.getDelegate()).setCandidateCollection(rsall);
        q1.setParameter("d", null);
        assertNull(q1.getResultList().get(0));

        endEm(em);
    }

    /**
     * OPENJPA-3042: EXTRACT(WEEK) evaluated in memory matches the database
     * where the database computes the ISO-8601 week. H2 numbers the weeks
     * of EXTRACT(WEEK) by the week definition of the default locale, which
     * is the ISO one for a locale such as de_DE but not for en_US.
     */
    public void testExtractWeekInMemoryMatchesDatabase() {
        EntityManager em = currentEntityManager();
        DBDictionary dict = ((JDBCConfiguration) ((OpenJPAEntityManagerSPI) em)
            .getConfiguration()).getDBDictionaryInstance();
        if (!(dict instanceof H2Dictionary)
            || !WeekFields.of(Locale.getDefault()).equals(WeekFields.ISO)) {
            // Derby does not support EXTRACT, H2 computes the ISO week only
            // for a locale whose week definition is the ISO one
            endEm(em);
            return;
        }
        List rsall = em.createQuery("SELECT e from CompUser e").getResultList();
        for (String date : new String[] { "2024-01-01", "2023-12-31", "2021-01-03" }) {
            Object[] db = (Object[]) em.createQuery("SELECT EXTRACT(WEEK FROM {d '" + date
                + "'}), e.userid FROM CompUser e").getResultList().get(0);
            assertExtract(em, rsall, "WEEK", LocalDate.parse(date), ((Number) db[0]).intValue());
        }
        endEm(em);
    }

    private void assertExtract(EntityManager em, List candidates, String field,
        Object value, Object expected) {
        org.apache.openjpa.persistence.QueryImpl q1 =
            (org.apache.openjpa.persistence.QueryImpl) em.createQuery(
                "SELECT EXTRACT(" + field + " FROM :d) FROM CompUser e");
        ((QueryImpl) q1.getDelegate()).setCandidateCollection(candidates);
        q1.setParameter("d", value);
        assertEquals(field + " of " + value + " (" + value.getClass().getName() + ")",
            expected, q1.getResultList().get(0));
    }

    private Object first(EntityManager em, List candidates, String jpql) {
        org.apache.openjpa.persistence.QueryImpl q1 =
            (org.apache.openjpa.persistence.QueryImpl) em.createQuery(jpql);
        ((QueryImpl) q1.getDelegate()).setCandidateCollection(candidates);
        return q1.getResultList().get(0);
    }

    public void testCoalesceExpressions() {
        EntityManager em = currentEntityManager();
        List rsall = em.createQuery("SELECT e from CompUser e")
            .getResultList();

        String query = "SELECT e.name as name, " +
                " COALESCE (e.address.country, 'Unknown')" +
                " FROM CompUser e ORDER BY name";
        org.apache.openjpa.persistence.QueryImpl q1 =
            (org.apache.openjpa.persistence.QueryImpl) em.createQuery(query);
        Query q2 = q1.getDelegate();
        Query qi = (QueryImpl) q2;
        qi.setCandidateCollection(rsall);
        List rs = q1.getResultList();
        Object[] result = (Object[]) rs.get(0);
        assertEquals("the name is not famzy", "Famzy", result[0]);
        assertEquals("Unknown", result[1]);

        endEm(em);
    }

    public void testNullIfExpressions() {
        EntityManager em = currentEntityManager();
        List rsall = em.createQuery("SELECT e from CompUser e")
            .getResultList();

        String query = "SELECT e.name,  " +
            " NULLIF (e.address.country, 'USA'), " +
            " e.address.country as res " +
            " FROM CompUser e ORDER BY res";

        org.apache.openjpa.persistence.QueryImpl q1 =
            (org.apache.openjpa.persistence.QueryImpl) em.createQuery(query);
        Query q2 = q1.getDelegate();
        Query qi = (QueryImpl) q2;
        qi.setCandidateCollection(rsall);
        List rs = q1.getResultList();
        Object[] result = (Object[]) rs.get(2);
        assertEquals("the name is not shannon ", expectedShannonName, result[0]);
        assertNull("is not null", result[1]);

        endEm(em);
    }

    public void testSimpleCaseExpression() {
        EntityManager em = currentEntityManager();
        List rsall = em.createQuery("SELECT e from CompUser e")
            .getResultList();

        String query = "SELECT e.name, " +
            " CASE e.address.country WHEN 'USA' " +
            " THEN 'us' " +
            " ELSE 'non-us'  END as d2, " +
            " e.address.country " +
            " FROM CompUser e order by d2";

        org.apache.openjpa.persistence.QueryImpl q1 =
            (org.apache.openjpa.persistence.QueryImpl) em.createQuery(query);
        Query q2 = q1.getDelegate();
        Query qi = (QueryImpl) q2;
        qi.setCandidateCollection(rsall);
        List rs = q1.getResultList();
        Object[] result = (Object[]) rs.get(5);
        assertEquals("the name is not shannon ", expectedShannonName, result[0]);
        assertEquals("is not 'us'", "us", result[1]);

        endEm(em);
    }

    public void testGeneralCaseExpression() {
        EntityManager em = currentEntityManager();
        List rsall = em.createQuery("SELECT e from CompUser e")
            .getResultList();

        String query = "SELECT e.name as name, " +
            " CASE WHEN e.age > 30 THEN  30 " +
            " WHEN e.age < 15 THEN  15 " +
            " ELSE 20 " +
            " END, " +
            " e.age " +
            " FROM CompUser e ORDER BY name";

        org.apache.openjpa.persistence.QueryImpl q1 =
            (org.apache.openjpa.persistence.QueryImpl) em.createQuery(query);
        Query q2 = q1.getDelegate();
        Query qi = (QueryImpl) q2;
        qi.setCandidateCollection(rsall);
        List rs = q1.getResultList();
        Object[] result = (Object[]) rs.get(3);
        assertEquals("the name is not shannon ", expectedShannonName, result[0]);
        assertEquals("not 30", "30", result[1].toString());

        endEm(em);
    }

    public CompUser createUser(String name, String cName, Address add, int age,
        boolean isMale) {
        CompUser user = null;
        if (isMale) {
            user = new MaleUser();
            user.setName(name);
            user.setComputerName(cName);
            user.setAddress(add);
            user.setAge(age);
        } else {
            user = new FemaleUser();
            user.setName(name);
            user.setComputerName(cName);
            user.setAddress(add);
            user.setAge(age);
        }
        return user;
    }
}
