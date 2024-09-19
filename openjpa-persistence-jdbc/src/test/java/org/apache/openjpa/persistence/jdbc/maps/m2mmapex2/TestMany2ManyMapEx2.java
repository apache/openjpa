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
package org.apache.openjpa.persistence.jdbc.maps.m2mmapex2;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;

import org.apache.openjpa.kernel.QueryImpl;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

import org.junit.Assert;

public class TestMany2ManyMapEx2 extends SQLListenerTestCase {

    public int numEmployees = 2;
    public int numPhoneNumbers = numEmployees + 1;
    public int numEmployeesPerPhoneNumber = 2;
    public int numPhoneNumbersPerEmployee = 2;

    public Map<Integer, Employee> empMap = new HashMap<>();
    public Map<Integer, PhoneNumber> phoneMap =
        new HashMap<>();

    public int empId = 1;
    public int phoneId = 1;
    public int divId = 1;
    public int deptId = 1;
    public List rsAllPhones = null;
    public List rsAllEmps = null;

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES,
            Department.class,
            Employee.class,
            PhoneNumber.class
        );
        createObj();
           rsAllPhones = getAll(PhoneNumber.class);
           rsAllEmps = getAll(Employee.class);
    }

    @AllowFailure
    public void testQueryInMemoryQualifiedId() throws Exception {
        queryQualifiedId(true);
    }
    public void testQueryQualifiedId() throws Exception {
        queryQualifiedId(false);
    }
    public void setCandidate(Query q, Class clz)
        throws Exception {
        org.apache.openjpa.persistence.QueryImpl q1 =
            (org.apache.openjpa.persistence.QueryImpl) q;
        org.apache.openjpa.kernel.Query q2 = q1.getDelegate();
        org.apache.openjpa.kernel.QueryImpl qi = (QueryImpl) q2;
        if (clz == PhoneNumber.class)
            qi.setCandidateCollection(rsAllPhones);
        else if (clz == Employee.class)
            qi.setCandidateCollection(rsAllEmps);
    }
    public void queryQualifiedId(boolean inMemory) throws Exception {
        EntityManager em = emf.createEntityManager();
        String query = "select KEY(e) from PhoneNumber p, " +
            " in (p.emps) e order by e.empId";
        Query q = em.createQuery(query);
        if (inMemory)
            setCandidate(q, PhoneNumber.class);
        List rs = q.getResultList();
        String d = (String) rs.get(0);

        query = "select KEY(p) from Employee e, " +
                " in (e.phones) p";
        q = em.createQuery(query);
        if (inMemory)
            setCandidate(q, Employee.class);
        rs = q.getResultList();
        Department d2 = (Department) rs.get(0);

        em.clear();
        query = "select ENTRY(e) from PhoneNumber p, " +
            " in (p.emps) e order by e.empId";
        q = em.createQuery(query);
        if (inMemory)
            setCandidate(q, PhoneNumber.class);
        rs = q.getResultList();
        Map.Entry me = (Map.Entry) rs.get(0);

        assertEquals(d, me.getKey());

        query = "select KEY(e) from PhoneNumber p, " +
            " in (p.emps) e WHERE value(e).empId = 1 and KEY(e) = 'String1'";
        q = em.createQuery(query);
        if (inMemory)
            setCandidate(q, PhoneNumber.class);
        rs = q.getResultList();
        assertEquals((String) rs.get(0), "String1");

        query = "select KEY(e) from PhoneNumber p, " +
            " in (p.emps) e WHERE KEY(e) = ?1";
        q = em.createQuery(query);
        q.setParameter(1, "String1");
        if (inMemory)
            setCandidate(q, PhoneNumber.class);
        rs = q.getResultList();
        assertEquals((String) rs.get(0), "String1");

        query = "select KEY(e) from PhoneNumber p, " +
            " in (p.emps) e WHERE KEY(e) = ?1";
        q = em.createQuery(query);
        q.setParameter(1, "String1");
        if (inMemory)
            setCandidate(q, PhoneNumber.class);
        rs = q.getResultList();
        assertEquals((String) rs.get(0), "String1");

        em.close();
    }

    public void testQueryObject() throws Exception {
        queryObj();
        findObj();
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
            PhoneNumber phoneNumber = new PhoneNumber();
            phoneNumber.setNumber(phoneId++);
            Department d = createDepartment(em, deptId++);
            phoneNumber.addEmployees("String" + e.getEmpId(), e);
            e.addPhoneNumber(d, phoneNumber);
            phoneMap.put(phoneNumber.getNumber(), phoneNumber);
            em.persist(phoneNumber);
            em.persist(d);
        }
        em.persist(e);
        return e;
    }

    public Department createDepartment(EntityManager em, int id) {
        Department d = new Department();
        d.setId(id);
        d.setName("d" + id);
        return d;
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
        for (PhoneNumber p : ps) {
            assertPhoneNumber(p);
        }
        tran.commit();
        em.close();
    }

    public void queryEmployee() throws Exception {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select e from Employee e");
        List<Employee> es = q.getResultList();
        for (Employee e : es) {
            assertEmployee(e);
        }
        tran.commit();
        em.close();
    }

    public void assertEmployee(Employee e) throws Exception {
        int id = e.getEmpId();
        Employee e0 = empMap.get(id);
        Map<Department, PhoneNumber> phones = e.getPhoneNumbers();
        Map<Department, PhoneNumber> phones0 = e0.getPhoneNumbers();
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

    public void checkPhoneMap(Map<Department, PhoneNumber> es0,
        Map<Department, PhoneNumber> es) throws Exception {
        Collection<Map.Entry<Department, PhoneNumber>> entrySets0 =
            es0.entrySet();
        for (Map.Entry<Department, PhoneNumber> entry0 : entrySets0) {
            Department d0 = entry0.getKey();
            PhoneNumber p0 = entry0.getValue();
            PhoneNumber p = es.get(d0);
            if (!p0.equals(p))
                throw new Exception("Assertion failure");
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

}
