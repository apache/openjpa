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
        String expectedSQL = "SELECT t0.id, t0.accountNum, t2.id, t2.city, t2.country, t2.county, t2.state, " + 
        "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, " + 
        "t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, t0.status " + 
        "FROM CR_CUST t0 LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID " + 
        "WHERE (t0.balanceOwed < (SELECT AVG(t1.balanceOwed) FROM CR_CUST t1 ))"; 

        execute(jpql, expectedSQL);
    }

    public void testSubqueries2() {
        String jpql = "SELECT DISTINCT emp FROM Employee emp WHERE EXISTS ("
            + "SELECT spouseEmp FROM Employee spouseEmp WHERE spouseEmp ="
            + " emp.spouse)";
        String expectedSQL = "SELECT t1.empId, t1.EMP_TYPE, t2.id, t2.city, t2.country, t2.county, t2.state, " + 
        "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, t4.deptNo, " + 
        "t4.name, t5.id, t5.annualMiles, t5.name, t6.id, t7.deptNo, t7.name, t6.name, t6.salary, t1.name, " + 
        "t1.rating, t1.salary, t8.empId, t8.EMP_TYPE, t8.ADDRESS_ID, t8.DEPARTMENT_DEPTNO, t8.FREQUENTFLIERPLAN_ID, " +
        "t8.MANAGER_ID, t8.name, t8.rating, t8.salary, t8.hireDate, t1.hireDate " + 
        "FROM CR_EMP t1 LEFT OUTER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CR_DEPT t4 ON t1.DEPARTMENT_DEPTNO = t4.deptNo " + 
        "LEFT OUTER JOIN FrequentFlierPlan t5 ON t1.FREQUENTFLIERPLAN_ID = t5.id " + 
        "LEFT OUTER JOIN CR_MGR t6 ON t1.MANAGER_ID = t6.id " + 
        "LEFT OUTER JOIN CR_EMP t8 ON t1.SPOUSE_EMPID = t8.empId " + 
        "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID " + 
        "LEFT OUTER JOIN CR_DEPT t7 ON t6.DEPARTMENT_DEPTNO = t7.deptNo " + 
        "WHERE (EXISTS (SELECT t0.empId FROM CR_EMP t0 WHERE (t0.empId = t1.SPOUSE_EMPID) ))"; 

        execute(jpql, expectedSQL);
    }

    public void testSubqueries3() {
        String jpql = "SELECT emp FROM Employee emp WHERE emp.salary > ALL ("
            + "SELECT m.salary FROM Manager m WHERE m.department = "
            + "emp.department)";
        String expectedSQL = "SELECT t0.empId, t0.EMP_TYPE, t2.id, t2.city, t2.country, t2.county, t2.state, " + 
        "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, t4.deptNo, " + 
        "t4.name, t5.id, t5.annualMiles, t5.name, t6.id, t7.deptNo, t7.name, t6.name, t6.salary, t0.name, t0.rating, " + 
        "t0.salary, t8.empId, t8.EMP_TYPE, t8.ADDRESS_ID, t8.DEPARTMENT_DEPTNO, t8.FREQUENTFLIERPLAN_ID, " + 
        "t8.MANAGER_ID, t8.name, t8.rating, t8.salary, t8.hireDate, t0.hireDate " + 
        "FROM CR_EMP t0 LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CR_DEPT t4 ON t0.DEPARTMENT_DEPTNO = t4.deptNo " + 
        "LEFT OUTER JOIN FrequentFlierPlan t5 ON t0.FREQUENTFLIERPLAN_ID = t5.id " + 
        "LEFT OUTER JOIN CR_MGR t6 ON t0.MANAGER_ID = t6.id " + 
        "LEFT OUTER JOIN CR_EMP t8 ON t0.SPOUSE_EMPID = t8.empId " + 
        "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID " + 
        "LEFT OUTER JOIN CR_DEPT t7 ON t6.DEPARTMENT_DEPTNO = t7.deptNo " + 
        "WHERE (t0.salary > ALL (SELECT t1.salary " + 
        "FROM CR_MGR t1 WHERE (t1.DEPARTMENT_DEPTNO = t0.DEPARTMENT_DEPTNO) ))"; 
        execute(jpql, expectedSQL);
    }

    public void testSubqueries4() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM c.orders o) > 10";
        String expectedSQL = "SELECT t0.id, t0.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, " + 
        "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, t0.balanceOwed, " + 
        "t0.creditRating, " + "t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, t0.status " + 
        "FROM CR_CUST t0 LEFT OUTER JOIN CR_ADDR t3 ON t0.ADDRESS_ID = t3.id " + 
        "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID " + 
        "WHERE ((SELECT COUNT(t2.id) FROM  CR_ODR t1, CR_ODR t2 WHERE (t1.id = t2.id) AND " + 
        "(t0.id = t1.CUSTOMER_ID) ) > ?)"; 

        execute(jpql, expectedSQL);
    }

    public void testSubqueries4a() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o WHERE c = c1) > 10";
        String expectedSQL = "SELECT t2.id, t2.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, " + 
        "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, t2.balanceOwed, " + 
        "t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, t2.name, t2.status " + 
        "FROM CR_CUST t2 LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id " + 
        "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID " + 
        "WHERE ((SELECT COUNT(t1.id) " + 
        "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE (t2.id = t0.id) ) > ?)";

        
        execute(jpql, expectedSQL);
    }    
    
    public void testSubqueries4b() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o WHERE c.id = c1.id) > 10";
        
        String expectedSQL = "SELECT t2.id, t2.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, " + 
        "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, t2.balanceOwed, " + 
        "t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, t2.name, t2.status " + 
        "FROM CR_CUST t2 LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id " +
        "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID " +
        "WHERE ((SELECT COUNT(t1.id) " + 
        "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE (t2.id = t0.id) ) > ?)"; 
        execute(jpql, expectedSQL);
    }    
    
    public void testSubqueries4c() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o) > 10";
        String expectedSQL = "SELECT t2.id, t2.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, " + 
        "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, t2.balanceOwed, " + 
        "t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, t2.name, t2.status " + 
        "FROM CR_CUST t2 LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id " + 
        "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID " + 
        "WHERE ((SELECT COUNT(t1.id) FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID ) > ?)"; 

        execute(jpql, expectedSQL);
    }
    
    public void testSubqueries5() {
        String jpql = "SELECT o FROM Order o WHERE 10000 < ALL ("
            + "SELECT a.balance FROM o.customer c JOIN c.accounts a)";
        String expectedSQL = "SELECT t3.id, t3.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, " + 
        "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, " + 
        "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, " + 
        "t3.delivered, t3.name, t3.orderTs, t3.quantity, t3.totalCost FROM CR_ODR t3 LEFT OUTER JOIN CR_CUST t4 ON " + 
        "t3.CUSTOMER_ID = t4.id LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id LEFT OUTER JOIN CompUser t6 ON " + 
        "t5.id = t6.ADD_ID WHERE (? < ALL (SELECT t2.balance FROM CR_CUST t0 INNER JOIN CR_CUST_CR_ACCT t1 ON " + 
        "t0.id = t1.CUSTOMER_ID INNER JOIN CR_ACCT t2 ON t1.ACCOUNTS_ID = t2.id WHERE (t3.CUSTOMER_ID = t0.id) ))"; 

        execute(jpql, expectedSQL);
    }

    public void testSubqueries5a() {
        String jpql = "SELECT o FROM Order o WHERE o.name = SOME ("
            + "SELECT a.name FROM o.customer c JOIN c.accounts a)";
        String expectedSQL = "SELECT t3.id, t3.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, " + 
        "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, " + 
        "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, " + 
        "t3.delivered, t3.name, t3.orderTs, t3.quantity, t3.totalCost " + 
        "FROM CR_ODR t3 LEFT OUTER JOIN CR_CUST t4 ON t3.CUSTOMER_ID = t4.id " + 
        "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id " + 
        "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID " + 
        "WHERE (t3.name = ANY (SELECT t2.name " + 
        "FROM CR_CUST t0 INNER JOIN CR_CUST_CR_ACCT t1 ON t0.id = t1.CUSTOMER_ID " + 
        "INNER JOIN CR_ACCT t2 ON t1.ACCOUNTS_ID = t2.id WHERE (t3.CUSTOMER_ID = t0.id) ))"; 

        execute(jpql, expectedSQL);
    }
    
    public void testSubqueries6() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT a.balance FROM c.accounts a)";
        String expectedSQL = "SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, " + 
        "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, " + 
        "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, " + 
        "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost " + 
        "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id " + 
        "LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id " + 
        "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id " + 
        "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID WHERE (? < ALL (" + 
        "SELECT t3.balance FROM  CR_CUST_CR_ACCT t2, CR_ACCT t3 WHERE (t2.ACCOUNTS_ID = t3.id) AND " + 
        "(t1.id = t2.CUSTOMER_ID) ))"; 

        execute(jpql, expectedSQL);
    }

    public void testSubqueries6a() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE o.name = "
            + "SOME (SELECT a.name FROM c.accounts a)";
        String expectedSQL = "SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, " + 
        "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, " + 
        "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, " + 
        "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost " + 
        "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id " + 
        "LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id " + 
        "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id " + 
        "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID WHERE (t0.name = ANY (" + 
        "SELECT t3.name FROM  CR_CUST_CR_ACCT t2, CR_ACCT t3 WHERE (t2.ACCOUNTS_ID = t3.id) " + 
        "AND (t1.id = t2.CUSTOMER_ID) ))"; 

        execute(jpql, expectedSQL);
    }
    
    public void testSubqueries6b() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE o.name = "
            + "SOME (SELECT a.county FROM c.address a)";
        String expectedSQL = "SELECT t0.id, t0.count, t3.id, t3.accountNum, t4.id, t4.city, t4.country, t4.county, " + 
        "t4.state, t4.street, t5.userid, t5.DTYPE, t5.age, t5.compName, t5.creditRating, t5.name, t4.zipCode, " + 
        "t3.balanceOwed, t3.creditRating, t3.filledOrderCount, t3.firstName, t3.lastName, t3.name, t3.status, " + 
        "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost " + 
        "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id " + 
        "LEFT OUTER JOIN CR_CUST t3 ON t0.CUSTOMER_ID = t3.id " + 
        "LEFT OUTER JOIN CR_ADDR t4 ON t3.ADDRESS_ID = t4.id " + 
        "LEFT OUTER JOIN CompUser t5 ON t4.id = t5.ADD_ID WHERE (t0.name = ANY " + 
        "(SELECT t2.county FROM CR_ADDR t2 WHERE (t1.ADDRESS_ID = t2.id) ))"; 
;
        execute(jpql, expectedSQL);
    }

    public void testSubqueries6c() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address a JOIN a.user u)";
        
        String expectedSQL1 = "SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, " + 
        "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, " + 
        "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, " + 
        "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost " + 
        "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id " + 
        "LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id " + 
        "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id " + 
        "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID WHERE (? < ALL (" + 
        "SELECT t3.age FROM CR_ADDR t2 INNER JOIN CompUser t3 ON t2.id = t3.ADD_ID WHERE (t1.ADDRESS_ID = t2.id) ))"; 
        
        execute(jpql, expectedSQL1);
    }
 
    public void testSubqueries6d() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c JOIN c.address a WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address a1 JOIN a1.user u WHERE a.city = a1.city)";
        String expectedSQL = "SELECT t2.id, t2.count, t5.id, t5.accountNum, t6.id, t6.city, t6.country, t6.county, " + 
        "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, " + 
        "t5.balanceOwed, t5.creditRating, t5.filledOrderCount, t5.firstName, t5.lastName, t5.name, t5.status, " + 
        "t2.delivered, t2.name, t2.orderTs, t2.quantity, t2.totalCost " + 
        "FROM CR_ODR t2 INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id " +
        "LEFT OUTER JOIN CR_CUST t5 ON t2.CUSTOMER_ID = t5.id " + 
        "INNER JOIN CR_ADDR t4 ON t3.ADDRESS_ID = t4.id " + 
        "LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id " + 
        "LEFT OUTER JOIN CompUser t7 ON t6.id = t7.ADD_ID WHERE (? < ALL (" + 
        "SELECT t1.age FROM CR_ADDR t0 INNER JOIN CompUser t1 ON t0.id = t1.ADD_ID WHERE (t4.city = t0.city AND " + 
        "t3.ADDRESS_ID = t0.id) ) AND 1 = 1)"; 

        execute(jpql, expectedSQL);
    }

    public void testSubqueries6e() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c JOIN c.address a WHERE 10000 < "
            + "ALL (SELECT u.age FROM a.user u)";
        String expectedSQL = "SELECT t0.id, t0.count, t5.id, t5.accountNum, t6.id, t6.city, t6.country, t6.county, " + 
        "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, " + 
        "t5.balanceOwed, t5.creditRating, t5.filledOrderCount, t5.firstName, t5.lastName, t5.name, t5.status, " + 
        "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON " + 
        "t0.CUSTOMER_ID = t1.id LEFT OUTER JOIN CR_CUST t5 ON t0.CUSTOMER_ID = t5.id INNER JOIN CR_ADDR t2 ON " + 
        "t1.ADDRESS_ID = t2.id LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id LEFT OUTER JOIN CompUser t7 ON " + 
        "t6.id = t7.ADD_ID WHERE (? < ALL (SELECT t4.age FROM  CompUser t3, CompUser t4 WHERE (t3.userid = t4.userid) " + 
        "AND (t2.id = t3.ADD_ID) ) AND 1 = 1)"; 

        String expectedSQL1 = "SELECT t0.id, t0.count, t5.id, t5.accountNum, t6.id, t6.city, t6.country, t6.county, " + 
        "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, " + 
        "t5.balanceOwed, t5.creditRating, t5.filledOrderCount, t5.firstName, t5.lastName, t5.name, t5.status, " + 
        "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost " + 
        "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id " + 
        "LEFT OUTER JOIN CR_CUST t5 ON t0.CUSTOMER_ID = t5.id " + 
        "INNER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id " + 
        "INNER JOIN CompUser t3 ON t2.id = t3.ADD_ID " + 
        "LEFT OUTER JOIN CompUser t7 ON t6.id = t7.ADD_ID " + 
        "WHERE (? < ALL (SELECT t4.age FROM CompUser t4 WHERE (t3.userid = t4.userid) ) AND 1 = 1)"; 
        
        execute(jpql, expectedSQL1);
    }
    
    public void testSubqueries6f() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address.user u)";
        
        String expectedSQL = "SELECT t0.id, t0.count, t5.id, t5.accountNum, t6.id, t6.city, t6.country, t6.county, " + 
        "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, " + 
        "t5.balanceOwed, t5.creditRating, t5.filledOrderCount, t5.firstName, t5.lastName, t5.name, t5.status, " + 
        "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON " + 
        "t0.CUSTOMER_ID = t1.id LEFT OUTER JOIN CR_CUST t5 ON t0.CUSTOMER_ID = t5.id LEFT OUTER JOIN CR_ADDR t6 ON " + 
        "t5.ADDRESS_ID = t6.id LEFT OUTER JOIN CompUser t7 ON t6.id = t7.ADD_ID WHERE (? < ALL (SELECT t4.age FROM " + 
        "CR_ADDR t2 INNER JOIN CompUser t3 ON t2.id = t3.ADD_ID, CompUser t4 WHERE (t3.userid = t4.userid) AND " + 
        "(t1.ADDRESS_ID = t2.id) ))";

        String expectedSQL1 = "SELECT t0.id, t0.count, t5.id, t5.accountNum, t6.id, t6.city, t6.country, t6.county, " + 
        "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, " + 
        "t5.balanceOwed, t5.creditRating, t5.filledOrderCount, t5.firstName, t5.lastName, t5.name, t5.status, " + 
        "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost " + 
        "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id " + 
        "LEFT OUTER JOIN CR_CUST t5 ON t0.CUSTOMER_ID = t5.id " + 
        "INNER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id " + 
        "INNER JOIN CompUser t3 ON t2.id = t3.ADD_ID " + 
        "LEFT OUTER JOIN CompUser t7 ON t6.id = t7.ADD_ID " + 
        "WHERE (? < ALL (SELECT t4.age FROM CompUser t4 WHERE (t3.userid = t4.userid) ))"; 

        
        execute(jpql, expectedSQL1);
    }
    
    public void testSubqueries6g() {
        String jpql = "SELECT o FROM Order o JOIN o.customer.address a WHERE 10000 < "
            + "ALL (SELECT u.age FROM a.user u)";
        String expectedSQL = "SELECT t0.id, t0.count, t1.id, t1.accountNum, t6.id, t6.city, t6.country, t6.county, " + 
        "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, " + 
        "t1.balanceOwed, t1.creditRating, t1.filledOrderCount, t1.firstName, t1.lastName, t1.name, t1.status, " + 
        "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost " + 
        "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id " + 
        "INNER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CR_ADDR t6 ON t1.ADDRESS_ID = t6.id " + 
        "INNER JOIN CompUser t4 ON t2.id = t4.ADD_ID " + 
        "LEFT OUTER JOIN CompUser t7 ON t6.id = t7.ADD_ID WHERE (? < ALL (" + 
        "SELECT t5.age FROM  CR_CUST t3, CompUser t5 WHERE (t4.userid = t5.userid) AND (t0.CUSTOMER_ID = t3.id) ))";
        execute(jpql, expectedSQL);
    }    
    
    void execute(String jpql, String expectedSQL) {
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
            String jSQL = sql.get(i).trim();
            System.out.println("jSQL = " + sql.get(i));
            if (expectedSQL != null) {
                assertEquals(expectedSQL, jSQL);
            }
        }
    }


}
