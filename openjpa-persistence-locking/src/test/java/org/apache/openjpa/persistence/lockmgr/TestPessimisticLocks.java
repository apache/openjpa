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

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Test Pessimistic Lock and exception behavior against EntityManager and Query interface methods.
 */
public class TestPessimisticLocks extends SQLListenerTestCase {

    public void setUp() {
        setSupportedDatabases(
//                org.apache.openjpa.jdbc.sql.DerbyDictionary.class,
//                org.apache.openjpa.jdbc.sql.OracleDictionary.class,
                org.apache.openjpa.jdbc.sql.DB2Dictionary.class);
        if (isTestsDisabled()) {
            return;
        }

        setUp(Employee.class, Department.class, "openjpa.LockManager", "mixed");
        String empTable = getMapping(Employee.class).getTable().getFullName();
        String deptTable = getMapping(Department.class).getTable().getFullName();

        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();

            em.createQuery("delete from " + empTable).executeUpdate();
            em.createQuery("delete from " + deptTable).executeUpdate();

            em.getTransaction().commit();

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

            em.getTransaction().begin();
            em.persist(d1);
            em.persist(d2);
            em.persist(e1);
            em.persist(e2);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /*
     * Test a find with pessimistic lock after a query with pessimistic lock and expect PessimisticLockException.
     */
    public void testFindWithLockTimeoutAfterQueryWithPessimisticLocks() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        try {
            em1.getTransaction().begin();

            Query query = em1.createQuery(
                    "select e from Employee e where e.id < 10 order by e.id").setFirstResult(1);
            query.setLockMode(LockModeType.PESSIMISTIC_READ);
            List<Employee> q = query.getResultList();
            assertEquals("Expected 1 element with emplyee id=2", q.size(), 1);
            assertEquals("Test Employee first name = 'first.2'", q.get(0).getFirstName(), "first.2");

            em2.getTransaction().begin();

            Map<String,Object> map = new HashMap<String,Object>();
            map.put("javax.persistence.lock.timeout", 2000);
            em2.find(Employee.class, 2, LockModeType.PESSIMISTIC_READ, map);
            fail("Unexcpected find succeeded. Should throw a PessimisticLockException.");
        } catch (QueryTimeoutException e) {            
            // TODO: DB2: This is the current unexpected exception due to OPENJPA-991.
            // Remove this when the problem is fixed
//            System.out.println("Caught " + e.getClass().getName() + ":" + e.getMessage());
        } catch (PessimisticLockException e) {
            // TODO: This is the expected exception but will be fixed under OPENJPA-991
//            System.out.println("Caught " + e.getClass().getName() + ":" + e.getMessage());
        } catch (Exception ex) {
            fail("Caught unexpected " + ex.getClass().getName() + ":" + ex.getMessage());
        } finally {
            if( em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if( em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }

        try {
            em1.getTransaction().begin();

            Query query = em1.createQuery(
                    "select e.department from Employee e where e.id < 10 order by e.department.id").setFirstResult(1);
            query.setLockMode(LockModeType.PESSIMISTIC_READ);
            List<Department> q = query.getResultList();
            assertEquals("Expected 1 element with department id=20", q.size(), 1);
            assertEquals("Test department name = 'D20'", q.get(0).getName(), "D20");

            em2.getTransaction().begin();

            Map<String,Object> map = new HashMap<String,Object>();
            map.put("javax.persistence.lock.timeout", 2000);
            Employee emp = em2.find(Employee.class, 1, LockModeType.PESSIMISTIC_READ, map);
            assertNotNull("Query locks department but find locks Employee.", emp);
            fail("Unexcpected find succeeded. Should throw a PessimisticLockException.");
        } catch (QueryTimeoutException e) {            
            // TODO: This is the current unexpected exception due to OPENJPA-991. Remove this when the problem is fixed 
//            System.out.println("Caught " + e.getClass().getName() + ":" + e.getMessage());
        } catch (PessimisticLockException e) {
            // TODO: This is the expected exception but will be fixed under OPENJPA-991
//            System.out.println("Caught " + e.getClass().getName() + ":" + e.getMessage());
        } catch (Exception ex) {
            fail("Caught unexpected " + ex.getClass().getName() + ":" + ex.getMessage());
        } finally {
            if( em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if( em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }
        em1.close();
        em2.close();
    }

    /*
     * Test a query with pessimistic lock after a find with pessimistic lock and expect PessimisticLockException.
     */
    public void testQueryAfterFindWithPessimisticLocks() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        try {
            em2.getTransaction().begin();

            Map<String,Object> map = new HashMap<String,Object>();
            map.put("javax.persistence.lock.timeout", 2000);
            em2.find(Employee.class, 1, LockModeType.PESSIMISTIC_READ, map);

            em1.getTransaction().begin();

            Query query = em1.createQuery(
                    "select e.department from Employee e where e.id < 10 order by e.department.id").setFirstResult(1);
            query.setLockMode(LockModeType.PESSIMISTIC_READ);
            List<Department> q = query.getResultList();

            fail("Unexcpected find succeeded. Should throw a PessimisticLockException.");
        } catch (PessimisticLockException e) {
            // This is the expected exception.
        } catch (Exception ex) {
            fail("Caught unexpected " + ex.getClass().getName() + ":" + ex.getMessage());
        } finally {
            if( em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if (em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }
        em1.close();
        em2.close();
    }

    /*
     * Test a query with pessimistic lock with query timeout set after a find
     * with pessimistic lock and expect QueryTimeoutException.
     */
    public void testQueryWithQueryTimeoutAfterFindWithPessimisticLocks() {
        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();
        try {
            em2.getTransaction().begin();

            Map<String,Object> map = new HashMap<String,Object>();
            map.put("javax.persistence.lock.timeout", 2000);
            em2.find(Employee.class, 1, LockModeType.PESSIMISTIC_READ, map);

            em1.getTransaction().begin();

            Query query = em1.createQuery(
                    "select e.department from Employee e where e.id < 10 order by e.department.id").setFirstResult(1);
            query.setLockMode(LockModeType.PESSIMISTIC_READ);
            query.setHint("javax.persistence.query.timeout", 2000);
            List<Department> q = query.getResultList();

            fail("Unexcpected find succeeded. Should throw a PessimisticLockException.");
        } catch (QueryTimeoutException e) {            
            // This is the expected exception.
        } catch (Exception ex) {
            fail("Caught unexpected " + ex.getClass().getName() + ":" + ex.getMessage());
        } finally {
            if( em1.getTransaction().isActive())
                em1.getTransaction().rollback();
            if( em2.getTransaction().isActive())
                em2.getTransaction().rollback();
        }
        em1.close();
        em2.close();
    }
}
