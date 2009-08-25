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
package org.apache.openjpa.persistence.query;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import junit.framework.Assert;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Test JDBC escape syntax for date, time, and timestamp literals
 */
public class TestJDBCEscapeDate extends SingleEMFTestCase {

    public void setUp() {
        setUp(Employee.class, DROP_TABLES);
    }

    public void testJDBCEscape() {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        Employee e = new Employee();
        e.setEmpId(1);
        e.setName("name1");
        e.setHireDate(new Date());
        e.setHireTime(new Date());
        e.setHireTimestamp(new Date());
        em.persist(e);
        tran.begin();
        em.flush();
        tran.commit();
        em.clear();
        
        String[] jpql = {
            "select a from Employee a where a.hireDate >= {d '2009-08-25'}",
            "select a from Employee a where a.hireTime >= {t '00:00:00'}",
            "select a from Employee a where a.hireTimestamp >= {ts '2009-08-25 00:00:00'}",
            "select a from Employee a where a.hireTimestamp >= {ts '2009-08-25 00:00:00.1'}",
            "select a from Employee a where a.hireTimestamp >= {ts '2009-08-25 00:00:00.11'}",
            "select a from Employee a where a.hireTimestamp >= {ts '2009-08-25 00:00:00.111'}",
            "select a from Employee a where a.hireTimestamp >= {ts '2009-08-25 00:00:00.1111'}",
            "select a from Employee a where a.hireTimestamp >= {ts '2009-08-25 00:00:00.11111'}",
            "select a from Employee a where a.hireTimestamp >= {ts '2009-08-25 00:00:00.111111'}",
            "select {t '00:00:00'}, a.empId from Employee a",
        };

        for (int i = 0; i < jpql.length; i++) {
            Query q = em.createQuery(jpql[i]);
            List results = q.getResultList();
            Assert.assertEquals(1, results.size());
        }

        String wrongTs = "select a from Employee a where a.hireTimestamp > {ts '2009-08-25 00:00:00.1111111'}";
        try {
            Query q = em.createQuery(wrongTs);
            List results = q.getResultList();
            Assert.fail();
        } catch (Exception ex) {
        }

        em.getTransaction().begin();
        String update = "update Employee a set a.hireTimestamp = {ts '2009-08-25 00:00:00.111111'} where a.empId = 1";
        Query q = em.createQuery(update);
        int updateCnt = q.executeUpdate();
        em.getTransaction().commit();
        Assert.assertEquals(1, updateCnt);
        em.close();
    }
}
