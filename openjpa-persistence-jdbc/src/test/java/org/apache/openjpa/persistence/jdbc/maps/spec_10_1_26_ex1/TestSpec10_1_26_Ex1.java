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
package org.apache.openjpa.persistence.jdbc.maps.spec_10_1_26_ex1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import junit.framework.Assert;

import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestSpec10_1_26_Ex1 extends SingleEMFTestCase {

    public int numDepartments = 2;
    public int numEmployeesPerDept = 2;
    public List<String> namedQueries = new ArrayList<String>();

    public int deptId = 1;
    public int empId = 1;

    protected List<String> sql = new ArrayList<String>();
    protected int sqlCount;

    public void setUp() {
        super.setUp(CLEAR_TABLES,
            Department.class,
            Employee.class,
            "openjpa.jdbc.JDBCListeners", 
            new JDBCListener[] { 
            this.new Listener() 
        });
        createObj();
    }

    public void testQualifiedId() throws Exception {
        EntityManager em = emf.createEntityManager();
        String query = "select KEY(e) from Department d, " +
            " in (d.empMap) e order by d.deptId, e.empId";
        List rs = em.createQuery(query).getResultList();
        Integer d = (Integer) rs.get(0);

        em.clear();
        String query4 = "select ENTRY(e) from Department d, " +
            " in (d.empMap) e order by d.deptId, e.empId";
        List rs4 = em.createQuery(query4).getResultList();
        Map.Entry me = (Map.Entry) rs4.get(0);

        assertTrue(d.equals(me.getKey()));

        // test KEY(e) of basic type in conditional expression
        sql.clear();
        query = "select KEY(e) from Department d, " +
            " in (d.empMap) e where KEY(e) > 1";
        rs = em.createQuery(query).getResultList();
        assertTrue(sql.get(0).toUpperCase().indexOf(">") > 0);

        em.close();
    }

    public void testQueryObject() throws Exception {
        queryObj();
        findObj();
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
            createDepartment(em, deptId++);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createDepartment(EntityManager em, int id) {
        Department d = new Department();
        d.setDeptId(id);
        Map emps = new HashMap();
        for (int i = 0; i < numEmployeesPerDept; i++) {
            Employee e = createEmployee(em, empId++);
            d.addEmployee(e);
            emps.put(e.getEmpId(), e);
            e.setDepartment(d);
            em.persist(e);
        }
        em.persist(d);
    }

    public Employee createEmployee(EntityManager em, int id) {
        Employee e = new Employee();
        e.setEmpId(id);
        return e;
    }

    public void findObj() {
        EntityManager em = emf.createEntityManager();
        Department d = em.find(Department.class, 1);
        assertDepartment(d);

        Employee e = em.find(Employee.class, 1);
        assertEmployee(e);

        // updateObj by adding a new Employee
        updateObj(em, d);

        deleteObj(em, d);
        em.close();
    }

    public void updateObj(EntityManager em, Department d) {
        EntityTransaction tran = em.getTransaction();
        
        // add an element
        tran.begin();
        Employee e = createEmployee(em,
            numDepartments * numEmployeesPerDept + 1);
        d.addEmployee(e);
        e.setDepartment(d);
        em.persist(d);
        em.persist(e);
        em.flush();
        tran.commit();

        // remove an element
        tran.begin();
        d.removeEmployee(e.getEmpId());
        e.setDepartment(null);
        em.persist(d);
        em.persist(e);
        em.flush();
        tran.commit();
        
    }

    public void deleteObj(EntityManager em, Department d) {
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        em.remove(d);
        tran.commit();
    }
    
    public void assertDepartment(Department d) {
        int id = d.getDeptId();
        Map<Integer, Employee> es = d.getEmpMap();
        Assert.assertEquals(2, es.size());
        Set keys = es.keySet();
        for (Object obj : keys) {
            Integer empId = (Integer) obj;
            Employee e = es.get(empId);
            Assert.assertEquals(empId.intValue(), e.getEmpId());
        }
    }

    public void queryObj() {
        queryDepartment();
        queryEmployee();
    }

    public void queryDepartment() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select d from Department d");
        List<Department> ds = q.getResultList();
        for (Department d : ds)
            assertDepartment(d);

        tran.commit();
        em.close();
    }

    public void queryEmployee() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select e from Employee e");
        List<Employee> es = q.getResultList();
        for (Employee e : es)
            assertEmployee(e);

        tran.commit();
        em.close();
    }

    public void assertEmployee(Employee e) {
        int id = e.getEmpId();
        Department d = e.getDepartment();
        assertDepartment(d);
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
