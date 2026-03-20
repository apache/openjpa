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
package org.apache.openjpa.persistence.jdbc.query;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

import org.apache.openjpa.persistence.jdbc.query.domain.HireDateEmployee;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that setParameter with Calendar/Date and TemporalType correctly converts
 * values to java.sql types when the entity field uses @Temporal(DATE).
 *
 * This reproduces JPA TCK query.apitests failures:
 * - setParameterIntCalendarTemporalTypeTest
 * - setParameterStringCalendarTemporalTypeTest
 * - setParameterParameterDateTemporalTypeTest
 * - setParameter8Test
 */
public class TestTemporalTypeParameterConversion extends SingleEMFTestCase {

    private static final String JPQL_POSITIONAL =
        "SELECT e FROM HireDateEmployee e WHERE e.hireDate = ?1";
    private static final String JPQL_NAMED =
        "SELECT e FROM HireDateEmployee e WHERE e.hireDate = :hDate";

    private Date targetDate;

    @Override
    public void setUp() throws Exception {
        super.setUp(CLEAR_TABLES, HireDateEmployee.class);

        // Use a date-only value (no time component) for reliable DATE comparison
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        targetDate = sdf.parse("2005-02-18");

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        HireDateEmployee emp = new HireDateEmployee();
        emp.setName("John");
        emp.setHireDate(targetDate);
        em.persist(emp);

        HireDateEmployee emp2 = new HireDateEmployee();
        emp2.setName("Jane");
        emp2.setHireDate(sdf.parse("2010-06-15"));
        em.persist(emp2);

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests query.setParameter(int, Calendar, TemporalType.DATE) with positional parameter.
     * Mirrors TCK setParameterIntCalendarTemporalTypeTest.
     */
    public void testSetParameterIntCalendarTemporalTypeDate() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Calendar cal = Calendar.getInstance();
        cal.setTime(targetDate);

        // Test Query version
        Query query = em.createQuery(JPQL_POSITIONAL);
        List<?> results = query.setParameter(1, cal, TemporalType.DATE).getResultList();
        assertEquals("Query with positional Calendar param should find 1 result", 1, results.size());
        assertEquals("John", ((HireDateEmployee) results.get(0)).getName());

        em.getTransaction().commit();

        // Test TypedQuery version
        em.getTransaction().begin();
        TypedQuery<HireDateEmployee> tquery = em.createQuery(JPQL_POSITIONAL, HireDateEmployee.class);
        List<HireDateEmployee> tresults = tquery.setParameter(1, cal, TemporalType.DATE).getResultList();
        assertEquals("TypedQuery with positional Calendar param should find 1 result", 1, tresults.size());
        assertEquals("John", tresults.get(0).getName());

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests query.setParameter(String, Calendar, TemporalType.DATE) with named parameter.
     * Mirrors TCK setParameterStringCalendarTemporalTypeTest.
     */
    public void testSetParameterStringCalendarTemporalTypeDate() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Calendar cal = Calendar.getInstance();
        cal.setTime(targetDate);

        // Test Query version
        Query query = em.createQuery(JPQL_NAMED);
        List<?> results = query.setParameter("hDate", cal, TemporalType.DATE).getResultList();
        assertEquals("Query with named Calendar param should find 1 result", 1, results.size());
        assertEquals("John", ((HireDateEmployee) results.get(0)).getName());

        em.getTransaction().commit();

        // Test TypedQuery version
        em.getTransaction().begin();
        TypedQuery<HireDateEmployee> tquery = em.createQuery(JPQL_NAMED, HireDateEmployee.class);
        List<HireDateEmployee> tresults = tquery.setParameter("hDate", cal, TemporalType.DATE).getResultList();
        assertEquals("TypedQuery with named Calendar param should find 1 result", 1, tresults.size());
        assertEquals("John", tresults.get(0).getName());

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests query.setParameter(int, Date, TemporalType.DATE) with positional parameter.
     * Mirrors TCK setParameter8Test.
     */
    public void testSetParameterIntDateTemporalTypeDate() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        // Use java.util.Date (NOT java.sql.Date) to test conversion
        Date utilDate = new Date(targetDate.getTime());

        // Test Query version
        Query query = em.createQuery(JPQL_POSITIONAL);
        List<?> results = query.setParameter(1, utilDate, TemporalType.DATE).getResultList();
        assertEquals("Query with positional Date param should find 1 result", 1, results.size());
        assertEquals("John", ((HireDateEmployee) results.get(0)).getName());

        em.getTransaction().commit();

        // Test TypedQuery version
        em.getTransaction().begin();
        TypedQuery<HireDateEmployee> tquery = em.createQuery(JPQL_POSITIONAL, HireDateEmployee.class);
        List<HireDateEmployee> tresults = tquery.setParameter(1, utilDate, TemporalType.DATE).getResultList();
        assertEquals("TypedQuery with positional Date param should find 1 result", 1, tresults.size());
        assertEquals("John", tresults.get(0).getName());

        em.getTransaction().commit();
        em.close();
    }

    /**
     * Tests query.setParameter(Parameter, Date, TemporalType.DATE) with Parameter object.
     * Mirrors TCK setParameterParameterDateTemporalTypeTest.
     */
    public void testSetParameterParameterDateTemporalTypeDate() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Date utilDate = new Date(targetDate.getTime());

        // Test Query version
        Query query = em.createQuery(JPQL_POSITIONAL);
        @SuppressWarnings("unchecked")
        Parameter<Date> p = (Parameter<Date>) query.getParameter(1);
        query.setParameter(p, utilDate, TemporalType.DATE);
        List<?> results = query.getResultList();
        assertEquals("Query with Parameter<Date> param should find 1 result", 1, results.size());
        assertEquals("John", ((HireDateEmployee) results.get(0)).getName());

        em.getTransaction().commit();

        // Test TypedQuery version
        em.getTransaction().begin();
        TypedQuery<HireDateEmployee> tquery = em.createQuery(JPQL_POSITIONAL, HireDateEmployee.class);
        @SuppressWarnings("unchecked")
        Parameter<Date> tp = (Parameter<Date>) tquery.getParameter(1);
        tquery.setParameter(tp, utilDate, TemporalType.DATE);
        List<HireDateEmployee> tresults = tquery.getResultList();
        assertEquals("TypedQuery with Parameter<Date> param should find 1 result", 1, tresults.size());
        assertEquals("John", tresults.get(0).getName());

        em.getTransaction().commit();
        em.close();
    }
}
