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
package org.apache.openjpa.persistence.criteria;

import java.util.List;

import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.openjpa.persistence.test.AllowFailure;

/**
 * Tests type-strict version of Criteria API.
 * 
 * Most of the tests build Criteria Query and then execute the query as well as
 * a reference JPQL query supplied as a string. The test is validated by
 * asserting that the resultant SQL queries for these two alternative form of
 * executing a query are the same.
 * 
 * 
 */
public class TestJPQLSubquery extends CriteriaTest {
    
    public void testSubqueries1() {
        String jpql = "SELECT goodCustomer FROM Customer goodCustomer WHERE "
            + "goodCustomer.balanceOwed < (SELECT AVG(c.balanceOwed) " 
            + " FROM Customer c)";
        execute(jpql);
    }

    public void testSubqueries2() {
        String jpql = "SELECT DISTINCT emp FROM Employee emp WHERE EXISTS ("
            + "SELECT spouseEmp FROM Employee spouseEmp WHERE spouseEmp ="
            + " emp.spouse)";
        execute(jpql);
    }

    public void testSubqueries3() {
        String jpql = "SELECT emp FROM Employee emp WHERE emp.salary > ALL ("
            + "SELECT m.salary FROM Manager m WHERE m.department = "
            + "emp.department)";
        execute(jpql);
    }

    public void testSubqueries4() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM c.orders o) > 10";
        execute(jpql);
    }

    public void testSubqueries4a() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o WHERE c = c1) > 10";
        execute(jpql);
    }    
    
    public void testSubqueries4b() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o WHERE c.id = c1.id) > 10";
        execute(jpql);
    }    
    
    public void atestSubqueries4c() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o) > 10";
        execute(jpql);
    }
    
    public void testSubqueries5() {
        String jpql = "SELECT o FROM Order o WHERE 10000 < ALL ("
            + "SELECT a.balance FROM o.customer c JOIN c.accounts a)";
        execute(jpql);
    }

    public void atestSubqueries5a() {
        String jpql = "SELECT o FROM Order o WHERE o.name = SOME ("
            + "SELECT a.name FROM o.customer c JOIN c.accounts a)";
        execute(jpql);
    }
    
    public void testSubqueries6() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT a.balance FROM c.accounts a)";
        execute(jpql);
    }

    public void atestSubqueries6a() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE o.name = "
            + "SOME (SELECT a.name FROM c.accounts a)";
        execute(jpql);
    }
    
    public void testSubqueries6b() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address a JOIN a.user u)";
        execute(jpql);
    }
 
    public void testSubqueryies6c() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c JOIN c.address a WHERE 10000 < "
            + "ALL (SELECT u.age FROM a.user u)";
        execute(jpql);
    }
    
    public void testSubqueries6d() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address.user u)";
        execute(jpql);
    }
    
    public void btestSubqueries6e() {
        String jpql = "SELECT o FROM Order o JOIN o.customer.address a WHERE 10000 < "
            + "ALL (SELECT u.age FROM a.user u)";
        execute(jpql);
    }    
    
    void execute(String jpql) {
        sql.clear();
        Query jQ = em.createQuery(jpql);
        try {
            List jList = jQ.getResultList();
        } catch (PersistenceException e) {
            e.printStackTrace();
            fail("Wrong SQL for JPQL :" + jpql + "\r\nSQL  :" + sql.get(0));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Wrong JPQL :" + jpql);
        }
        
        for (int i = 0; i < sql.size(); i++) {
            System.out.println("jSQL = " + sql.get(i));
        }
    }
}
