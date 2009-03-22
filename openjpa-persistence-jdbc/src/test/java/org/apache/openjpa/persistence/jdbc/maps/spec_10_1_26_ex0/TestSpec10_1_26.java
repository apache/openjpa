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
package org.apache.openjpa.persistence.jdbc.maps.spec_10_1_26_ex0;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestSpec10_1_26 extends SingleEMFTestCase {

    public int numDepartments = 2;
    public int numEmployeesPerDept = 2;
    public List<String> namedQueries = new ArrayList<String>();

    public int deptId = 1;
    public int empId = 1;

    protected List<String> sql = new ArrayList<String>();
    protected int sqlCount;

    public void setUp() {
        super.setUp(DROP_TABLES,
            Department1.class,
            Department2.class,
            Department3.class,
            Employee1.class,
            Employee2.class,
            Employee3.class,
            EmployeeName3.class,
            EmployeePK2.class,
            "openjpa.jdbc.JDBCListeners", 
            new JDBCListener[] { 
            this.new Listener() 
        });
        createObj();
    }

    public void testQueryQualifiedId() throws Exception {
        EntityManager em = emf.createEntityManager();
        String query = "select KEY(e) from Department1 d, " +
            " in (d.empMap) e";
        List rs = em.createQuery(query).getResultList();
        Integer d = (Integer) rs.get(0);
        String query2 = "select KEY(e) from Department2 d, " +
            " in (d.empMap) e";
        List rs2 = em.createQuery(query2).getResultList();
        EmployeePK2 d2 = (EmployeePK2) rs2.get(0);
        String query3 = "select KEY(e) from Department3 d, " +
            " in (d.emps) e";
        List rs3 = em.createQuery(query3).getResultList();
        EmployeeName3 d3 = (EmployeeName3) rs3.get(0);
        em.close();
    }

    public void testQueryObject() {
        queryObj();
    }

    public List<String> getSql() {
        return sql;
    }

    public int getSqlCount() {
        return sqlCount;
    }


    public void createObj() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        for (int i = 0; i < numDepartments; i++)
            createDepartment1(em, deptId++);

        for (int i = 0; i < numDepartments; i++)
            createDepartment2(em, deptId++);

        for (int i = 0; i < numDepartments; i++)
            createDepartment3(em, deptId++);

        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createDepartment1(EntityManager em, int id) {
        Department1 d = new Department1();
        d.setDeptId(id);
        Map empMap = new HashMap();
        for (int i = 0; i < numEmployeesPerDept; i++) {
            Employee1 e = createEmployee1(em, empId++);
            //d.addEmployee1(e);
            empMap.put(e.getEmpId(), e);
            e.setDepartment(d);
            em.persist(e);
        }
        d.setEmpMap(empMap);
        em.persist(d);
    }

    public Employee1 createEmployee1(EntityManager em, int id) {
        Employee1 e = new Employee1();
        e.setEmpId(id);
        return e;
    }

    public void createDepartment2(EntityManager em, int id) {
        Department2 d = new Department2();
        d.setDeptId(id);
        for (int i = 0; i < numEmployeesPerDept; i++) {
            Employee2 e = createEmployee2(em, empId++);
            d.addEmployee(e);
            e.setDepartment(d);
            em.persist(e);
        }
        em.persist(d);
    }

    public Employee2 createEmployee2(EntityManager em, int id) {
        Employee2 e = new Employee2("e" + id, new Date());
        return e;
    }

    public void createDepartment3(EntityManager em, int id) {
        Department3 d = new Department3();
        d.setDeptId(id);
        for (int i = 0; i < numEmployeesPerDept; i++) {
            Employee3 e = createEmployee3(em, empId++);
            d.addEmployee(e);
            e.setDepartment(d);
            em.persist(e);
        }
        em.persist(d);
    }

    public Employee3 createEmployee3(EntityManager em, int id) {
        Employee3 e = new Employee3();
        EmployeeName3 name = new EmployeeName3("f" + id, "l" + id);
        e.setEmpId(id);
        e.setName(name);
        return e;
    }

    public void findObj() {
        EntityManager em = emf.createEntityManager();
        Department1 d1 = em.find(Department1.class, 1);
        assertDepartment1(d1);

        Employee1 e1 = em.find(Employee1.class, 1);
        assertEmployee1(e1);

        Department2 d2 = em.find(Department2.class, 3);
        assertDepartment2(d2);

        Map empMap = d2.getEmpMap();
        Set<EmployeePK2> keys = empMap.keySet();
        for (EmployeePK2 key : keys) {
            Employee2 e2 = em.find(Employee2.class, key);
            assertEmployee2(e2);
        }

        Department3 d3 = em.find(Department3.class, 5);
        assertDepartment3(d3);

        Employee3 e3 = em.find(Employee3.class, 9);
        assertEmployee3(e3);

        em.close();
    }

    public void assertDepartment1(Department1 d) {
        int id = d.getDeptId();
        Map<Integer, Employee1> es = d.getEmpMap();
        Assert.assertEquals(2,es.size());
        Set keys = es.keySet();
        for (Object obj : keys) {
            Integer empId = (Integer) obj;
            Employee1 e = es.get(empId);
            Assert.assertEquals(empId.intValue(), e.getEmpId());
        }
    }

    public void assertDepartment2(Department2 d) {
        int id = d.getDeptId();
        Map<EmployeePK2, Employee2> es = d.getEmpMap();
        Assert.assertEquals(2,es.size());
        Set<EmployeePK2> keys = es.keySet();
        for (EmployeePK2 pk : keys) {
            Employee2 e = es.get(pk);
            Assert.assertEquals(pk, e.getEmpPK());
        }
    }	

    public void assertDepartment3(Department3 d) {
        int id = d.getDeptId();
        Map<EmployeeName3, Employee3> es = d.getEmployees();
        Assert.assertEquals(2,es.size());
        Set<EmployeeName3> keys = es.keySet();
        for (EmployeeName3 key : keys) {
            Employee3 e = es.get(key);
            Assert.assertEquals(key, e.getName());
        }
    }

    public void assertEmployee1(Employee1 e) {
        int id = e.getEmpId();
        Department1 d = e.getDepartment();
        assertDepartment1(d);
    }

    public void assertEmployee2(Employee2 e) {
        EmployeePK2 pk = e.getEmpPK();
        Department2 d = e.getDepartment();
        assertDepartment2(d);
    }

    public void assertEmployee3(Employee3 e) {
        int id = e.getEmpId();
        Department3 d = e.getDepartment();
        assertDepartment3(d);
    }

    public void queryObj() {
        queryDepartment1(emf);
        queryEmployee1(emf);
        queryDepartment2(emf);
        queryEmployee2(emf);
        queryDepartment3(emf);
        queryEmployee3(emf);
    }

    public void queryDepartment1(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select d from Department1 d");
        List<Department1> ds = q.getResultList();
        for (Department1 d : ds)
            assertDepartment1(d);

        tran.commit();
        em.close();
    }

    public void queryEmployee1(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select e from Employee1 e");
        List<Employee1> es = q.getResultList();
        for (Employee1 e : es)
            assertEmployee1(e);

        tran.commit();
        em.close();
    }

    public void queryDepartment2(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select d from Department2 d");
        List<Department2> ds = q.getResultList();
        for (Department2 d : ds)
            assertDepartment2(d);

        tran.commit();
        em.close();
    }

    public void queryEmployee2(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select e from Employee2 e");
        List<Employee2> es = q.getResultList();
        for (Employee2 e : es)
            assertEmployee2(e);

        tran.commit();
        em.close();
    }

    public void queryDepartment3(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select d from Department3 d");
        List<Department3> ds = q.getResultList();
        for (Department3 d : ds)
            assertDepartment3(d);

        tran.commit();
        em.close();
    }

    public void queryEmployee3(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select e from Employee3 e");
        List<Employee3> es = q.getResultList();
        for (Employee3 e : es)
            assertEmployee3(e);

        tran.commit();
        em.close();
    }

    public class Listener extends AbstractJDBCListener {
        @Override
        public void beforeExecuteStatement(JDBCEvent event) {
            if (event.getSQL() != null && sql != null) {
                sql.add(event.getSQL());
                sqlCount++;
            }
        }
    }
}