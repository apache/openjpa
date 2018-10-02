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
package org.apache.openjpa.persistence.jpql.joins;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import junit.framework.Assert;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Tests JQPL and Criteria API equivalent using left join fetch with QueryCache and DataCache enabled.
 */
public class TestJoinFetchWithQueryDataCache extends SQLListenerTestCase {
    EntityManager em;

    public void setUp() {
        super.setUp(DROP_TABLES, Employee.class, Department.class, "openjpa.QueryCompilationCache", "all",
            "openjpa.DataCache", "true", "openjpa.RemoteCommitProvider", "sjvm", "openjpa.QueryCache", "true"
            // This is a hack to work around using em.detach(...) w/ a 1.0 p.xml
            , "openjpa.Compatibility", "CopyOnDetach=false"
            );

        em = emf.createEntityManager();
        em.getTransaction().begin();

        Department dept;
        dept = new Department(10, "department 10");
        dept.setEmployees(new ArrayList<Employee>());
        dept.getEmployees().add(new Employee(11, "Emp11", dept));
        dept.getEmployees().add(new Employee(12, "Emp12", dept));
        dept.setEmployee2s(new ArrayList<Employee>());
        dept.getEmployee2s().add(new Employee(211, "Emp211", dept));
        dept.getEmployee2s().add(new Employee(212, "Emp212", dept));
        em.persist(dept);

        dept = new Department(20, "department 20");
        dept.setEmployees(new ArrayList<Employee>());
        dept.getEmployees().add(new Employee(21, "Emp21", dept));
        dept.getEmployees().add(new Employee(22, "Emp22", dept));
        dept.setEmployee2s(new ArrayList<Employee>());
        dept.getEmployee2s().add(new Employee(221, "Emp221", dept));
        dept.getEmployee2s().add(new Employee(222, "Emp222", dept));
        em.persist(dept);

        em.getTransaction().commit();

        em.close();
    }

    public void testJPQLNoFetch() {
        EntityManager em = emf.createEntityManager();
        List<Department> ds = em.createQuery("SELECT DISTINCT d FROM Department d WHERE d.deptno = 10").getResultList();
        System.out.println("-- testJPQLNoFetch -----");
        em.clear();
        Assert.assertEquals(1, ds.size());
        for (Department x : ds) {
            Assert.assertNull(x.getEmployees());
            Assert.assertNull(x.getEmployee2s());
            System.out.println(x);
        }

        em.close();
    }

    public void testJPQLOneFetch() {
        EntityManager em = emf.createEntityManager();
        List<Department> ds =
            em.createQuery("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.employee2s " + "WHERE d.deptno = 10")
                .getResultList();
        System.out.println("-- testJPQLOneFetch -----");
        em.clear();
        Assert.assertEquals(1, ds.size());
        for (Department x : ds) {
            Assert.assertNull(x.getEmployees());
            Assert.assertNotNull(x.getEmployee2s());
            Assert.assertEquals(2, x.getEmployee2s().size());
            System.out.println(x);
        }

        em.close();
    }

    public void testJPQLTwoFetch() {
        EntityManager em = emf.createEntityManager();
        List<Department> ds =
            em.createQuery(
                "SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.employees " + "LEFT JOIN FETCH d.employee2s "
                    + "WHERE d.deptno = 10").getResultList();
        System.out.println("-- testJPQLTwoFetch -----");
        em.clear();
        Assert.assertEquals(1, ds.size());
        for (Department x : ds) {
            Assert.assertNotNull(x.getEmployees());
            Assert.assertEquals(2, x.getEmployees().size());
            Assert.assertNotNull(x.getEmployee2s());
            Assert.assertEquals(2, x.getEmployee2s().size());
            System.out.println(x);
        }

        em.close();
    }

    public void testCriteriaAPINoFetch() {
        EntityManager em = emf.createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // This query is equivalent to the following Java Persistence query
        // language query:
        // SELECT d
        // FROM Department d
        // WHERE d.deptno = 1

        CriteriaQuery<Department> q = cb.createQuery(Department.class);
        Root<Department> d = q.from(Department.class);
        q.where(cb.equal(d.get(Department_.deptno), 20)).select(d);

        List<Department> ds = em.createQuery(q).getResultList();
        System.out.println("-- testCriteriaAPINoFetch -----");
        em.clear();
        Assert.assertEquals(1, ds.size());
        for (Department x : ds) {
            Assert.assertNull(x.getEmployees());
            Assert.assertNull(x.getEmployee2s());
            System.out.println(x);
        }

        em.close();
    }

    public void testCriteriaAPIOneFetch() {
        EntityManager em = emf.createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // 6.5.4 Fetch Joins
        // Example:
        // CriteriaQuery<Department> q = cb.createQuery(Department.class);
        // Root<Department> d = q.from(Department.class);
        // d.fetch(Department_.employees, JoinType.LEFT);
        // q.where(cb.equal(d.get(Department_.deptno), 1)).select(d);
        //
        // This query is equivalent to the following Java Persistence query
        // language query:
        // SELECT DISTINCT d
        // FROM Department d LEFT JOIN FETCH d.employees
        // WHERE d.deptno = 1

        CriteriaQuery<Department> q = cb.createQuery(Department.class);
        Root<Department> d = q.from(Department.class);
        d.fetch(Department_.employees, JoinType.LEFT);
        q.where(cb.equal(d.get(Department_.deptno), 20)).select(d).distinct(true);

        List<Department> ds = em.createQuery(q).getResultList();
        System.out.println("-- testCriteriaAPIOneFetch -----");
        em.clear();
        Assert.assertEquals(1, ds.size());
        for (Department x : ds) {
            Assert.assertNotNull(x.getEmployees());
            Assert.assertEquals(2, x.getEmployees().size());
            Assert.assertNull(x.getEmployee2s());
            System.out.println(x);
        }

        em.close();
    }

    public void testCriteriaAPITwoFetch() {
        EntityManager em = emf.createEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // This query is equivalent to the following Java Persistence query
        // language query:
        // SELECT DISTINCT d
        // FROM Department d LEFT JOIN FETCH d.employees LEFT JOIN FETCH d.employee2s
        // WHERE d.deptno = 1
        CriteriaQuery<Department> q = cb.createQuery(Department.class);
        Root<Department> d = q.from(Department.class);
        d.fetch(Department_.employees, JoinType.LEFT);
        d.fetch(Department_.employee2s, JoinType.LEFT);
        q.where(cb.equal(d.get(Department_.deptno), 20)).select(d).distinct(true);

        List<Department> ds = em.createQuery(q).getResultList();
        System.out.println("-- testCriteriaAPITwoFetch -----");
        em.clear();
        Assert.assertEquals(1, ds.size());
        for (Department x : ds) {
            Assert.assertNotNull(x.getEmployees());
            Assert.assertEquals(2, x.getEmployees().size());
            Assert.assertNotNull(x.getEmployee2s());
            Assert.assertEquals(2, x.getEmployee2s().size());
            System.out.println(x);
        }

        em.close();
    }

    public void testConsecutiveJPQLJoinFetchCall() {
        doQuery(emf, false);
        doQuery(emf, true);
    }

    private void doQuery(EntityManagerFactory emf, boolean cached) {
        String query = "select o from Employee o " + "left join fetch o.dept " + "where o.dept.deptno = 10";
        EntityManager em = emf.createEntityManager();

        sql.clear();
        List<Employee> emps = em.createQuery(query, Employee.class).getResultList();
        Assert.assertEquals(4, emps.size());
        for (Employee emp : emps) {
            em.detach(emp);

            Assert.assertNotNull(emp.getDept());
            Assert.assertEquals(2, emp.getDept().getEmployees().size());
        }
        em.close();
        if (cached) {
            assertTrue(sql.size() == 0);
        }
    }
}
