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
package org.apache.openjpa.persistence.lockmgr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PessimisticLockException;
import javax.persistence.Query;
import javax.persistence.QueryTimeoutException;
import javax.persistence.TypedQuery;

import junit.framework.AssertionFailedError;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.LockTimeoutException;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.apache.openjpa.util.OpenJPAException;

/**
 * Test Pessimistic Lock and exception behavior against EntityManager and Query
 * interface methods.
 */
public class TestPessimisticLocks extends SQLListenerTestCase {

    private DBDictionary dict = null;

    public void setUp() {
        // Disable tests for any DB that has supportsQueryTimeout==false, like Postgres
        OpenJPAEntityManagerFactorySPI tempEMF = emf;
        if (tempEMF == null) {
            tempEMF = createEMF();
        }
        assertNotNull(tempEMF);
        DBDictionary dict = ((JDBCConfiguration)tempEMF.getConfiguration()).getDBDictionaryInstance();
        assertNotNull(dict);
        if (!dict.supportsQueryTimeout)
            setTestsDisabled(true);
        if (emf == null) {
            closeEMF(tempEMF);
        }

        if (isTestsDisabled())
            return;
        
        setUp(CLEAR_TABLES, Employee.class, Department.class, "openjpa.LockManager", "mixed");

        EntityManager em = null;
        em = emf.createEntityManager();
        em.getTransaction().begin();

        Employee e1, e2;
        Department d1, d2;
        d1 = new Department();
        d1.setId(10);
        d1.setName("D10");

        e1 = new Employee();
        e1.setId(1);
        e1.setDepartment(d1);
        e1.setFirstName("first.1");
        e1.setLastName("last.1");

        d2 = new Department();
        d2.setId(20);
        d2.setName("D20");

        e2 = new Employee();
        e2.setId(2);
        e2.setDepartment(d2);
        e2.setFirstName("first.2");
        e2.setLastName("last.2");

        em.persist(d1);
        em.persist(d2);
        em.persist(e1);
        em.persist(e2);
        em.getTransaction().commit();
    }

    /*
     * Test find with pessimistic lock after a query with pessimistic lock.
     */
    public void testFindAfterQueryWithPessimisticLocks() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        em1.getTransaction().begin();
        TypedQuery<Employee> query = em1.createQuery("select e from Employee e where e.id < 10", Employee.class)
                .setFirstResult(1);
        // Lock all selected Employees, skip the first one, i.e should lock
        // Employee(2)
        query.setLockMode(LockModeType.PESSIMISTIC_READ);
        query.setHint("javax.persistence.query.timeout", 2000);
        List<Employee> employees = query.getResultList();
        assertEquals("Expected 1 element with emplyee id=2", employees.size(), 1);
        assertTrue("Test Employee first name = 'first.2'", employees.get(0).getFirstName().equals("first.1")
                || employees.get(0).getFirstName().equals("first.2"));

        em2.getTransaction().begin();
        Map<String, Object> hints = new HashMap<String, Object>();
        hints.put("javax.persistence.lock.timeout", 2000);
        // find Employee(2) with a lock, should block and expected a
        // PessimisticLockException
        try {
            em2.find(Employee.class, 2, LockModeType.PESSIMISTIC_READ, hints);
            fail("Unexcpected find succeeded. Should throw a PessimisticLockException.");
        } catch (Throwable e) {
            assertError(e, PessimisticLockException.class);
        } finally {
            if (em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if (em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }

        em1.getTransaction().begin();
        TypedQuery<Department> query2 = em1.createQuery("select e.department from Employee e where e.id < 10",
                Department.class).setFirstResult(1);
        // Lock all selected Departments, skip the first one, i.e should
        // lock Department(20)
        query.setLockMode(LockModeType.PESSIMISTIC_READ);
        query.setHint("javax.persistence.query.timeout", 2000);
        List<Department> depts = query2.getResultList();
        assertEquals("Expected 1 element with department id=20", depts.size(), 1);
        assertTrue("Test department name = 'D20'", depts.get(0).getName().equals("D10")
                || depts.get(0).getName().equals("D20"));

        em2.getTransaction().begin();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("javax.persistence.lock.timeout", 2000);
        // find Employee(2) with a lock, no block since only department was
        // locked
        try {
            Employee emp = em2.find(Employee.class, 1, LockModeType.PESSIMISTIC_READ, map);
            assertNotNull("Query locks department only, therefore should find Employee.", emp);
            assertEquals("Test Employee first name = 'first.1'", emp.getFirstName(), "first.1");
        } catch (Exception ex) {
            fail("Caught unexpected " + ex.getClass().getName() + ":" + ex.getMessage());
        } finally {
            if (em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if (em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }
        em1.close();
        em2.close();
    }

    /*
     * Test find with pessimistic lock after a query with pessimistic lock.
     */
    public void testFindAfterQueryOrderByWithPessimisticLocks() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        em1.getTransaction().begin();
        Query query = em1.createQuery("select e from Employee e where e.id < 10 order by e.id").setFirstResult(1);
        // Lock all selected Employees, skip the first one, i.e should lock
        // Employee(2)
        query.setLockMode(LockModeType.PESSIMISTIC_READ);
        query.setHint("javax.persistence.query.timeout", 2000);
        List<Employee> q = query.getResultList();
        assertEquals("Expected 1 element with emplyee id=2", q.size(), 1);
        assertEquals("Test Employee first name = 'first.2'", q.get(0).getFirstName(), "first.2");

        em2.getTransaction().begin();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("javax.persistence.lock.timeout", 2000);
        // find Employee(2) with a lock, should block and expected a
        // PessimisticLockException
        try {
            em2.find(Employee.class, 2, LockModeType.PESSIMISTIC_READ, map);
            fail("Unexcpected find succeeded. Should throw a PessimisticLockException.");
        } catch (Exception e) {
            assertError(e, PessimisticLockException.class);
        } finally {
            if (em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if (em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }

        em1.getTransaction().begin();
        query = em1.createQuery("select e.department from Employee e where e.id < 10 order by e.department.id")
                .setFirstResult(1);
        // Lock all selected Departments, skip the first one, i.e should
        // lock Department(20)
        query.setLockMode(LockModeType.PESSIMISTIC_READ);
        query.setHint("javax.persistence.query.timeout", 2000);
        List<Department> result = query.getResultList();
        assertEquals("Expected 1 element with department id=20", q.size(), 1);
        assertEquals("Test department name = 'D20'", result.get(0).getName(), "D20");

        em2.getTransaction().begin();
        map.clear();
        map.put("javax.persistence.lock.timeout", 2000);
        // find Employee(2) with a lock, no block since only department was
        // locked
        try {
            Employee emp = em2.find(Employee.class, 1, LockModeType.PESSIMISTIC_READ, map);
            assertNotNull("Query locks department only, therefore should find Employee.", emp);
            assertEquals("Test Employee first name = 'first.1'", emp.getFirstName(), "first.1");
        } catch (Exception ex) {
            fail("Caught unexpected " + ex.getClass().getName() + ":" + ex.getMessage());
        } finally {
            if (em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if (em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }
        em1.close();
        em2.close();
    }

    /*
     * Test query with pessimistic lock after a find with pessimistic lock.
     */
    public void testQueryAfterFindWithPessimisticLocks() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        try {
            em2.getTransaction().begin();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("javax.persistence.lock.timeout", 2000);
            // Lock Emplyee(1), no department should be locked
            em2.find(Employee.class, 1, LockModeType.PESSIMISTIC_READ, map);

            em1.getTransaction().begin();
            Query query = em1.createQuery("select e.department from Employee e where e.id < 10").setFirstResult(1);
            query.setLockMode(LockModeType.PESSIMISTIC_READ);
            query.setHint("javax.persistence.query.timeout", 2000);
            // Lock all selected Department but skip the first, i.e. lock
            // Department(20), should query successfully.
            List<Department> q = query.getResultList();
            assertEquals("Expected 1 element with department id=20", q.size(), 1);
            assertTrue("Test department name = 'D20'", q.get(0).getName().equals("D10")
                    || q.get(0).getName().equals("D20"));
        } catch (Exception ex) {
            fail("Caught unexpected " + ex.getClass().getName() + ":" + ex.getMessage());
        } finally {
            if (em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if (em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }

        em2.getTransaction().begin();

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("javax.persistence.lock.timeout", 2000);
        // Lock Emplyee(2), no department should be locked
        em2.find(Employee.class, 2, LockModeType.PESSIMISTIC_READ, map);

        em1.getTransaction().begin();
        Query query = em1.createQuery("select e from Employee e where e.id < 10").setFirstResult(1);
        // Lock all selected Employees, skip the first one, i.e should lock
        // Employee(2)
        query.setLockMode(LockModeType.PESSIMISTIC_READ);
        query.setHint("javax.persistence.query.timeout", 1000);
        try {
            List<Employee> q = query.getResultList();
            fail("Unexcpected find succeeded. Should throw a PessimisticLockException.");
        } catch (Exception e) {
            assertError(e, PessimisticLockException.class);
        } finally {
            if (em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if (em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }
        em1.close();
        em2.close();
    }

    /*
     * Test query with pessimistic lock after a find with pessimistic lock.
     */
    public void testQueryOrderByAfterFindWithPessimisticLocks() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        em2.getTransaction().begin();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("javax.persistence.lock.timeout", 2000);
        // Lock Emplyee(1), no department should be locked
        em2.find(Employee.class, 1, LockModeType.PESSIMISTIC_READ, map);

        em1.getTransaction().begin();
        Query query = em1.createQuery("select e.department from Employee e where e.id < 10 order by e.department.id")
                .setFirstResult(1);
        query.setLockMode(LockModeType.PESSIMISTIC_READ);
        query.setHint("javax.persistence.query.timeout", 2000);
        // Lock all selected Department but skip the first, i.e. lock
        // Department(20), should query successfully.
        try {
            List<Department> q = query.getResultList();
            assertEquals("Expected 1 element with department id=20", q.size(), 1);
            assertEquals("Test department name = 'D20'", q.get(0).getName(), "D20");
        } catch (Exception ex) {
            fail("Caught unexpected " + ex.getClass().getName() + ":" + ex.getMessage());
        } finally {
            if (em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if (em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }

        em2.getTransaction().begin();

        map.clear();
        map.put("javax.persistence.lock.timeout", 2000);
        // Lock Emplyee(2), no department should be locked
        em2.find(Employee.class, 2, LockModeType.PESSIMISTIC_READ, map);

        em1.getTransaction().begin();
        query = em1.createQuery("select e from Employee e where e.id < 10 order by e.department.id").setFirstResult(1);
        // Lock all selected Employees, skip the first one, i.e should lock
        // Employee(2)
        query.setLockMode(LockModeType.PESSIMISTIC_READ);
        query.setHint("javax.persistence.query.timeout", 2000);
        try {
            List<?> q = query.getResultList();
            fail("Unexcpected find succeeded. Should throw a PessimisticLockException.");
        } catch (Exception e) {
            assertError(e, PessimisticLockException.class);
        } finally {
            if (em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if (em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }
        em1.close();
        em2.close();
    }

    /*
     * Test multiple execution of the same query with pessimistic lock.
     */
    public void testRepeatedQueryWithPessimisticLocks() {
        EntityManager em = emf.createEntityManager();
        resetSQL();
        em.getTransaction().begin();
        String jpql = "select e.firstName from Employee e where e.id = 1";
        Query q1 = em.createQuery(jpql);
        q1.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        String firstName1 = (String) q1.getSingleResult();
        //Expected sql for Derby is:
        //SELECT t0.firstName FROM Employee t0 WHERE (t0.id = CAST(? AS BIGINT)) FOR UPDATE WITH RR
        String SQL1 = toString(sql);
        
        // run the second time
        resetSQL();
        Query q2 = em.createQuery(jpql);
        q2.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        String firstName2 = (String) q2.getSingleResult();
        String SQL2 = toString(sql);
        assertEquals(SQL1, SQL2);
        em.getTransaction().commit();
    }

    /**
     * Assert that an exception of proper type has been thrown. Also checks that
     * that the exception has populated the failed object.
     * 
     * @param actual
     *            exception being thrown
     * @param expeceted
     *            type of the exception
     */
    void assertError(Throwable actual, Class<? extends Throwable> expected) {
        if (!expected.isAssignableFrom(actual.getClass())) {
            actual.printStackTrace();
            throw new AssertionFailedError(actual.getClass().getName() + " was raised but expected "
                    + expected.getName());
        }
        Object failed = getFailedObject(actual);
        assertNotNull("Failed object is null", failed);
        assertNotEquals("null", failed);
    }

    Object getFailedObject(Throwable e) {
        if (e instanceof LockTimeoutException) {
            return ((LockTimeoutException) e).getObject();
        }
        if (e instanceof PessimisticLockException) {
            return ((PessimisticLockException) e).getEntity();
        }
        if (e instanceof QueryTimeoutException) {
            return ((QueryTimeoutException) e).getQuery();
        }
        if (e instanceof OpenJPAException) {
            return ((OpenJPAException) e).getFailedObject();
        }
        return null;
    }

}
