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
package org.apache.openjpa.persistence.jdbc.maps.m2mmapex8;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import junit.framework.Assert;

import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestMany2ManyMapEx8 extends SingleEMFTestCase {

    public int numEmployees = 2;
    public int numPhoneNumbers = numEmployees + 1;
    public int numEmployeesPerPhoneNumber = 2;
    public int numPhoneNumbersPerEmployee = 2;
    public Map<Integer, PhoneNumber> phones =
        new HashMap<Integer, PhoneNumber>();
    public List<String> namedQueries = new ArrayList<String>();

    public Map<Integer, Employee> empMap = new HashMap<Integer, Employee>();
    public Map<Integer, PhoneNumber> phoneMap =
        new HashMap<Integer, PhoneNumber>();

    public int empId = 1;
    public int phoneId = 1;
    public int divId = 1;
    public int deptId = 1;

    protected List<String> sql = new ArrayList<String>();
    protected int sqlCount;

    public void setUp() {
        super.setUp(CLEAR_TABLES,
            Employee.class,
            FullName.class,
            PhoneNumber.class,
            "openjpa.jdbc.JDBCListeners", 
            new JDBCListener[] {  this.new Listener() }
        );
        createObj();
    }

    public void testQueryQualifiedId() throws Exception {
        EntityManager em = emf.createEntityManager();
        String query = "select KEY(e) from PhoneNumber p, " +
            " in (p.emps) e order by e.empId";
        List rs = em.createQuery(query).getResultList();
        String d = (String) rs.get(0);

        String query2 = "select KEY(p) from Employee e, " +
            " in (e.phones) p";
        List rs2 = em.createQuery(query2).getResultList();
        FullName d2 = (FullName) rs2.get(0);

        em.clear();
        String query4 = "select ENTRY(e) from PhoneNumber p, " +
            " in (p.emps) e order by e.empId";
        List rs4 = em.createQuery(query4).getResultList();
        Map.Entry me = (Map.Entry) rs4.get(0);

        assertTrue(d.equals(me.getKey()));

        // test KEY(e) of basic type in conditional expression
        sql.clear();
        query = "select KEY(e) from PhoneNumber p, " +
            " in (p.emps) e where KEY(e) like '%1'";
        rs = em.createQuery(query).getResultList();
        assertTrue(sql.get(0).toUpperCase().indexOf("LIKE") > 0);

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
        for (int i = 0; i < numEmployees; i++) {
            Employee e = createEmployee(em, empId++);
            empMap.put(e.getEmpId(), e);
        }
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public Employee createEmployee(EntityManager em, int id) {
        Employee e = new Employee();
        e.setEmpId(id);
        for (int i = 0; i < numPhoneNumbersPerEmployee; i++) { 
            FullName name = new FullName("f" + id + i, "l" + id + i);
            PhoneNumber phoneNumber = new PhoneNumber();
            phoneNumber.setNumber(phoneId++);
            phoneNumber.addEmployees("String" + e.getEmpId() + i, e);
            e.addPhoneNumber(name, phoneNumber);
            em.persist(phoneNumber);
            phoneMap.put(phoneNumber.getNumber(), phoneNumber);
        }
        em.persist(e);
        return e;
    }

    public void findObj() throws Exception {
        EntityManager em = emf.createEntityManager();
        Employee e = em.find(Employee.class, 1);
        assertEmployee(e);

        PhoneNumber p = em.find(PhoneNumber.class, 1);
        assertPhoneNumber(p);
        em.close();
    }

    public void queryObj() throws Exception {
        queryEmployee();
        queryPhoneNumber();
    }

    public void queryPhoneNumber() throws Exception {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select p from PhoneNumber p");
        List<PhoneNumber> ps = q.getResultList();
        for (PhoneNumber p : ps){
            assertPhoneNumber(p);
        }
        tran.commit();
        em.close();
    }

    public void queryEmployee() throws Exception  {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select e from Employee e");
        List<Employee> es = q.getResultList();
        for (Employee e : es){
            assertEmployee(e);
        }
        tran.commit();
        em.close();
    }

    public void assertEmployee(Employee e) throws Exception {
        int id = e.getEmpId();
        Employee e0 = empMap.get(id);
        Map<FullName, PhoneNumber> phones = e.getPhoneNumbers();
        Map<FullName, PhoneNumber> phones0 = e0.getPhoneNumbers();
        Assert.assertEquals(phones0.size(), phones.size());
        checkPhoneMap(phones0, phones);
    }

    public void assertPhoneNumber(PhoneNumber p) throws Exception {
        int number = p.getNumber();
        PhoneNumber p0 = phoneMap.get(number);
        Map<String, Employee> es = p.getEmployees();
        Map<String, Employee> es0 = p0.getEmployees();
        Assert.assertEquals(es0.size(), es.size());
        checkEmpMap(es0, es);
    }

    public void checkPhoneMap(Map<FullName, PhoneNumber> es0, 
        Map<FullName, PhoneNumber> es) throws Exception {
        Collection<Map.Entry<FullName, PhoneNumber>> entrySets0 =
            es0.entrySet();
        for (Map.Entry<FullName, PhoneNumber> entry0 : entrySets0) {
            FullName key0 = entry0.getKey();
            PhoneNumber p0 = entry0.getValue();
            PhoneNumber p = es.get(key0);
            if (!p0.equals(p))
                throw new Exception("Assertion Failure");

        }
    }

    public void checkEmpMap(Map<String, Employee> es0, Map<String, Employee> es)
        throws Exception {
        Collection<Map.Entry<String, Employee>> entrySets0 = es0.entrySet();
        for (Map.Entry<String, Employee> entry0 : entrySets0) {
            String key0 = entry0.getKey();
            Employee e0 = entry0.getValue();
            Employee e = es.get(key0);
            if (!e0.equals(e))
                throw new Exception("Assertion failure");
        }
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