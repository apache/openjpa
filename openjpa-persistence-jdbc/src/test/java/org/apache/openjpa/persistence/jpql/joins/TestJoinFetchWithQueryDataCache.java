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

import junit.framework.Assert;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestJoinFetchWithQueryDataCache extends SQLListenerTestCase {
    EntityManager em;

    public void setUp() {
        super.setUp(DROP_TABLES, Employee.class, Department.class, "openjpa.QueryCompilationCache", "all",
            "openjpa.DataCache", "true", "openjpa.RemoteCommitProvider", "sjvm", "openjpa.QueryCache", "true"
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
