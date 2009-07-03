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

import java.util.Date;
import java.util.List;

import javax.persistence.PersistenceException;
import javax.persistence.Query;

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
        String expectedSQL = "SELECT t0.id, t0.accountNum, t2.id, t2.city, t2.country, t2.county, t2.state, "
            + "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, "
            + "t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, t0.status "
            + "FROM CR_CUST t0 LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id "
            + "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID "
            + "WHERE (t0.balanceOwed < (SELECT AVG(t1.balanceOwed) FROM CR_CUST t1 ))";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries2() {
        String jpql = "SELECT DISTINCT emp FROM Employee emp WHERE EXISTS ("
            + "SELECT spouseEmp FROM Employee spouseEmp WHERE spouseEmp ="
            + " emp.spouse)";
        String expectedSQL = "SELECT t1.empId, t1.EMP_TYPE, t2.id, t2.city, t2.country, t2.county, t2.state, "
            + "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, t4.deptNo, "
            + "t4.name, t5.id, t5.annualMiles, t5.name, t6.id, t7.deptNo, t7.name, t6.name, t6.salary, t1.name, "
            + "t1.rating, t1.salary, t8.empId, t8.EMP_TYPE, t8.ADDRESS_ID, t8.DEPARTMENT_DEPTNO, "
            + "t8.FREQUENTFLIERPLAN_ID, "
            + "t8.MANAGER_ID, t8.name, t8.rating, t8.salary, t8.hireDate, t1.hireDate "
            + "FROM CR_EMP t1 LEFT OUTER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id "
            + "LEFT OUTER JOIN CR_DEPT t4 ON t1.DEPARTMENT_DEPTNO = t4.deptNo "
            + "LEFT OUTER JOIN FrequentFlierPlan t5 ON t1.FREQUENTFLIERPLAN_ID = t5.id "
            + "LEFT OUTER JOIN CR_MGR t6 ON t1.MANAGER_ID = t6.id "
            + "LEFT OUTER JOIN CR_EMP t8 ON t1.SPOUSE_EMPID = t8.empId "
            + "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID "
            + "LEFT OUTER JOIN CR_DEPT t7 ON t6.DEPARTMENT_DEPTNO = t7.deptNo "
            + "WHERE (EXISTS (SELECT t0.empId FROM CR_EMP t0 WHERE (t0.empId = t1.SPOUSE_EMPID) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries3() {
        String jpql = "SELECT emp FROM Employee emp WHERE emp.salary > ALL ("
            + "SELECT m.salary FROM Manager m WHERE m.department = "
            + "emp.department)";
        String expectedSQL = "SELECT t0.empId, t0.EMP_TYPE, t2.id, t2.city, t2.country, t2.county, t2.state, "
            + "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, t4.deptNo, "
            + "t4.name, t5.id, t5.annualMiles, t5.name, t6.id, t7.deptNo, t7.name, t6.name, t6.salary, t0.name, "
            + "t0.rating, t0.salary, t8.empId, t8.EMP_TYPE, t8.ADDRESS_ID, t8.DEPARTMENT_DEPTNO, "
            + "t8.FREQUENTFLIERPLAN_ID, t8.MANAGER_ID, t8.name, t8.rating, t8.salary, t8.hireDate, t0.hireDate "
            + "FROM CR_EMP t0 LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id "
            + "LEFT OUTER JOIN CR_DEPT t4 ON t0.DEPARTMENT_DEPTNO = t4.deptNo "
            + "LEFT OUTER JOIN FrequentFlierPlan t5 ON t0.FREQUENTFLIERPLAN_ID = t5.id "
            + "LEFT OUTER JOIN CR_MGR t6 ON t0.MANAGER_ID = t6.id "
            + "LEFT OUTER JOIN CR_EMP t8 ON t0.SPOUSE_EMPID = t8.empId "
            + "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID "
            + "LEFT OUTER JOIN CR_DEPT t7 ON t6.DEPARTMENT_DEPTNO = t7.deptNo "
            + "WHERE (t0.salary > ALL (SELECT t1.salary "
            + "FROM CR_MGR t1 WHERE (t1.DEPARTMENT_DEPTNO = t0.DEPARTMENT_DEPTNO) ))";
        execute(jpql, expectedSQL);
    }

    public void testSubqueries4() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM c.orders o) > 10";
        String expectedSQL = "SELECT t0.id, t0.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, "
            + "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, " 
            + "t0.balanceOwed, t0.creditRating, "
            + "t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, t0.status "
            + "FROM CR_CUST t0 LEFT OUTER JOIN CR_ADDR t3 ON t0.ADDRESS_ID = t3.id "
            + "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID "
            + "WHERE ((SELECT COUNT(t2.id) FROM  CR_ODR t1, CR_ODR t2 WHERE (t1.id = t2.id) AND "
            + "(t0.id = t1.CUSTOMER_ID) ) > ?)";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries4a() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o WHERE c = c1) > 10";
        String expectedSQL = "SELECT t2.id, t2.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, "
            + "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, " 
            + "t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, t2.name, t2.status "
            + "FROM CR_CUST t2 LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id "
            + "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID "
            + "WHERE ((SELECT COUNT(t1.id) "
            + "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE (t2.id = t0.id) ) > ?)";

        execute(jpql, expectedSQL);
    }    

    public void testSubqueries4b() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o WHERE c.id = c1.id) > 10";

        String expectedSQL = "SELECT t2.id, t2.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, "
            + "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, " 
            + "t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, t2.name, t2.status "
            + "FROM CR_CUST t2 LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id "
            + "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID "
            + "WHERE ((SELECT COUNT(t1.id) "
            + "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE (t2.id = t0.id) ) > ?)";
        execute(jpql, expectedSQL);
    }

    public void testSubqueries4c() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o) > 10";
        String expectedSQL = "SELECT t2.id, t2.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, "
            + "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, "
            + "t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, t2.name, t2.status "
            + "FROM CR_CUST t2 LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id "
            + "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID "
            + "WHERE ((SELECT COUNT(t1.id) FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID ) > ?)";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries4d() {
        String jpql = "SELECT c FROM Customer c WHERE (SELECT COUNT(o) "
            + "FROM Customer c1 JOIN c1.orders o WHERE c.address.county = c1.address.county) > 10";

        String expectedSQL = "SELECT t2.id, t2.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, "
            + "t5.zipCode, t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, "
            + "t2.name, t2.status "
            + "FROM CR_CUST t2 "
            + "INNER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t2.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID "
            + "WHERE ((SELECT COUNT(t1.id) "
            + "FROM CR_CUST t0 "
            + "INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID "
            + "INNER JOIN CR_ADDR t4 ON t0.ADDRESS_ID = t4.id "
            + "WHERE (t3.county = t4.county) ) > ?)";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries4e() {
        String jpql = "SELECT c FROM Customer c WHERE EXISTS "
            + "(SELECT o.id FROM Order o WHERE o.customer = c)";
        String expectedSQL = "SELECT t1.id, t1.accountNum, t2.id, t2.city, t2.country, t2.county, t2.state, " + 
        "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, t1.balanceOwed, " + 
        "t1.creditRating, t1.filledOrderCount, t1.firstName, t1.lastName, t1.name, t1.status " + 
        "FROM CR_CUST t1 " + 
        "LEFT OUTER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID WHERE " + 
        "(EXISTS (SELECT t0.id FROM CR_ODR t0 WHERE (t0.CUSTOMER_ID = t1.id) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries5() {
        String jpql = "SELECT o FROM Order o WHERE 10000 < ALL ("
            + "SELECT a.balance FROM o.customer c JOIN c.accounts a)";
        String expectedSQL = "SELECT t3.id, t3.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, "
            + "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, "
            + "t3.delivered, t3.name, t3.orderTs, t3.quantity, t3.totalCost " 
            + "FROM CR_ODR t3 LEFT OUTER JOIN CR_CUST t4 ON t3.CUSTOMER_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id " 
            + "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID " 
            + "WHERE (? < ALL (SELECT t2.balance FROM CR_CUST t0 INNER JOIN CR_CUST_CR_ACCT t1 ON "
            + "t0.id = t1.CUSTOMER_ID INNER JOIN CR_ACCT t2 ON t1.ACCOUNTS_ID = t2.id " 
            + "WHERE (t3.CUSTOMER_ID = t0.id) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries5a() {
        String jpql = "SELECT o FROM Order o WHERE o.name = SOME ("
            + "SELECT a.name FROM o.customer c JOIN c.accounts a)";
        String expectedSQL = "SELECT t3.id, t3.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, "
            + "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, "
            + "t3.delivered, t3.name, t3.orderTs, t3.quantity, t3.totalCost "
            + "FROM CR_ODR t3 LEFT OUTER JOIN CR_CUST t4 ON t3.CUSTOMER_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID "
            + "WHERE (t3.name = ANY (SELECT t2.name "
            + "FROM CR_CUST t0 INNER JOIN CR_CUST_CR_ACCT t1 ON t0.id = t1.CUSTOMER_ID "
            + "INNER JOIN CR_ACCT t2 ON t1.ACCOUNTS_ID = t2.id WHERE (t3.CUSTOMER_ID = t0.id) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries6() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT a.balance FROM c.accounts a)";
        String expectedSQL = "SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, "
            + "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID WHERE (? < ALL ("
            + "SELECT t3.balance FROM  CR_CUST_CR_ACCT t2, CR_ACCT t3 WHERE (t2.ACCOUNTS_ID = t3.id) AND "
            + "(t1.id = t2.CUSTOMER_ID) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries6a() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE o.name = "
            + "SOME (SELECT a.name FROM c.accounts a)";
        String expectedSQL = "SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, "
            + "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID WHERE (t0.name = ANY ("
            + "SELECT t3.name FROM  CR_CUST_CR_ACCT t2, CR_ACCT t3 WHERE (t2.ACCOUNTS_ID = t3.id) "
            + "AND (t1.id = t2.CUSTOMER_ID) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubqueries6b() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE o.name = "
            + "SOME (SELECT a.county FROM c.address a)";
        String expectedSQL = "SELECT t0.id, t0.count, t3.id, t3.accountNum, t4.id, t4.city, t4.country, t4.county, "
            + "t4.state, t4.street, t5.userid, t5.DTYPE, t5.age, t5.compName, t5.creditRating, t5.name, t4.zipCode, "
            + "t3.balanceOwed, t3.creditRating, t3.filledOrderCount, t3.firstName, t3.lastName, t3.name, t3.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t3 ON t0.CUSTOMER_ID = t3.id "
            + "LEFT OUTER JOIN CR_ADDR t4 ON t3.ADDRESS_ID = t4.id "
            + "LEFT OUTER JOIN CompUser t5 ON t4.id = t5.ADD_ID WHERE (t0.name = ANY "
            + "(SELECT t2.county FROM CR_ADDR t2 WHERE (t1.ADDRESS_ID = t2.id) ))";
        ;
        execute(jpql, expectedSQL);
    }

    public void testSubqueries6c() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address a JOIN a.user u)";

        String expectedSQL1 = "SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, "
            + "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID WHERE (? < ALL ("
            + "SELECT t3.age FROM CR_ADDR t2 INNER JOIN CompUser t3 ON t2.id = t3.ADD_ID "
            + "WHERE (t1.ADDRESS_ID = t2.id) ))";

        execute(jpql, expectedSQL1);
    }

    public void testSubqueries6d() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c JOIN c.address a WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address a1 JOIN a1.user u WHERE a.city = a1.city)";
        String expectedSQL = "SELECT t2.id, t2.count, t5.id, t5.accountNum, t6.id, t6.city, t6.country, t6.county, "
            + "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, "
            + "t5.balanceOwed, t5.creditRating, t5.filledOrderCount, t5.firstName, t5.lastName, t5.name, t5.status, "
            + "t2.delivered, t2.name, t2.orderTs, t2.quantity, t2.totalCost "
            + "FROM CR_ODR t2 INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id "
            + "LEFT OUTER JOIN CR_CUST t5 ON t2.CUSTOMER_ID = t5.id "
            + "INNER JOIN CR_ADDR t4 ON t3.ADDRESS_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id "
            + "LEFT OUTER JOIN CompUser t7 ON t6.id = t7.ADD_ID " 
            + "WHERE ((? < ALL (" 
            + "SELECT t1.age FROM CR_ADDR t0 INNER JOIN CompUser t1 ON t0.id = t1.ADD_ID WHERE ((t4.city = t0.city AND "
            + "t3.ADDRESS_ID = t0.id)) ) AND 1 = 1))"; 
        
        execute(jpql, expectedSQL);
    }

    public void testSubqueries6e() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c JOIN c.address a WHERE 10000 < "
            + "ALL (SELECT u.age FROM a.user u)";

        String expectedSQL = "SELECT t0.id, t0.count, t5.id, t5.accountNum, t6.id, t6.city, t6.country, t6.county, "
            + "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, "
            + "t5.balanceOwed, t5.creditRating, t5.filledOrderCount, t5.firstName, t5.lastName, t5.name, t5.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t5 ON t0.CUSTOMER_ID = t5.id "
            + "INNER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id "
            + "LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id "
            + "LEFT OUTER JOIN CompUser t7 ON t6.id = t7.ADD_ID "
            + "WHERE ((? < ALL (SELECT t4.age FROM  CompUser t3, CompUser t4 "
            + "WHERE (t3.userid = t4.userid) AND (t2.id = t3.ADD_ID) ) AND 1 = 1))"; 

        execute(jpql, expectedSQL);
    }

    public void testSubqueries6f() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address.user u)";

        String expectedSQL = "SELECT t0.id, t0.count, t5.id, t5.accountNum, t6.id, t6.city, t6.country, t6.county, "
            + "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, "
            + "t5.balanceOwed, t5.creditRating, t5.filledOrderCount, t5.firstName, t5.lastName, t5.name, t5.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t5 ON t0.CUSTOMER_ID = t5.id "
            + "LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id "
            + "LEFT OUTER JOIN CompUser t7 ON t6.id = t7.ADD_ID "
            + "WHERE (? < ALL (SELECT t4.age "
            + "FROM CR_ADDR t2 INNER JOIN CompUser t3 ON t2.id = t3.ADD_ID, CompUser t4 "
            + "WHERE (t3.userid = t4.userid) AND (t1.ADDRESS_ID = t2.id) ))";

        execute(jpql, expectedSQL);
    }

    // redundant t3
    // compare to 6e, t3 should be in main query for LEFT OUTER JOIN
    public void testSubqueries6g() {
        String jpql = "SELECT o FROM Order o JOIN o.customer.address a WHERE 10000 < "
            + "ALL (SELECT u.age FROM a.user u)";
        String expectedSQL = "SELECT t0.id, t0.count, t1.id, t1.accountNum, t6.id, t6.city, t6.country, t6.county, "
            + "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, "
            + "t1.balanceOwed, t1.creditRating, t1.filledOrderCount, t1.firstName, t1.lastName, t1.name, t1.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "INNER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id "
            + "LEFT OUTER JOIN CR_ADDR t6 ON t1.ADDRESS_ID = t6.id "
            + "LEFT OUTER JOIN CompUser t7 ON t6.id = t7.ADD_ID WHERE (? < ALL ("
            + "SELECT t5.age FROM  CR_CUST t3,  CompUser t4, CompUser t5 "
            + "WHERE (t4.userid = t5.userid) AND (t0.CUSTOMER_ID = t3.id) AND (t2.id = t4.ADD_ID) ))";

        execute(jpql, expectedSQL);
    }

    public void testExist1() {
        String jpql = "SELECT DISTINCT c.name FROM CompUser c WHERE EXISTS"
            + " (SELECT a FROM Address a WHERE a = c.address )";

        String expectedSQL = "SELECT DISTINCT t1.name FROM CompUser t1 WHERE (EXISTS ("
            + "SELECT t0.id FROM CR_ADDR t0 WHERE (t0.id = t1.ADD_ID) ))";

        execute(jpql, expectedSQL);
    }

    public void testExist1a() {
        String jpql = "SELECT DISTINCT o.name FROM CompUser o WHERE EXISTS"
            + " (SELECT s FROM CompUser s WHERE s.address.country = "
            + "o.address.country)";

        String expectedSQL = "SELECT DISTINCT t2.name " + "FROM CompUser t2 "
        + "INNER JOIN CR_ADDR t3 ON t2.ADD_ID = t3.id "
        + "WHERE (EXISTS (" + "SELECT t0.userid " + "FROM CompUser t0 "
        + "INNER JOIN CR_ADDR t1 ON t0.ADD_ID = t1.id "
        + "WHERE (t1.country = t3.country) ))";

        execute(jpql, expectedSQL);

    }

    public void testExist1b() {
        String jpql = "select c from Customer c left join c.orders o where exists"
            + " (select o2 from c.orders o2 where o2 = o)";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, "
            + "t5.zipCode, t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, "
            + "t0.name, t0.status "
            + "FROM CR_CUST t0 "
            + "LEFT OUTER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID "
            + "LEFT OUTER JOIN CR_ODR t2 ON t0.id = t2.CUSTOMER_ID "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t0.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID "
            + "WHERE (EXISTS (" + 
            "SELECT t4.id FROM  CR_ODR t3, CR_ODR t4 WHERE ((t2.id = t4.id AND t3.id = t4.id)) "
            + "AND (t0.id = t3.CUSTOMER_ID) ))"; 

        execute(jpql, expectedSQL);
    }

    public void testNotExist1() {
        String jpql = "SELECT DISTINCT c.name FROM CompUser c WHERE NOT EXISTS"
            + " (SELECT a FROM Address a WHERE a = c.address )";

        String expectedSQL = "SELECT DISTINCT t1.name FROM CompUser t1 WHERE (NOT (EXISTS ("
            + "SELECT t0.id FROM CR_ADDR t0 WHERE (t0.id = t1.ADD_ID) )))";

        execute(jpql, expectedSQL);
    }

    public void testNotExist1a() {
        String jpql = "select c from Customer c left join c.orders o where not exists"
            + " (select o2 from c.orders o2 where o2 = o)";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, "
            + "t5.zipCode, t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, "
            + "t0.name, t0.status "
            + "FROM CR_CUST t0 "
            + "LEFT OUTER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID "
            + "LEFT OUTER JOIN CR_ODR t2 ON t0.id = t2.CUSTOMER_ID "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t0.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CompUser t6 ON t5.id = t6.ADD_ID "
            + "WHERE (NOT (EXISTS ("
            + "SELECT t4.id FROM  CR_ODR t3, CR_ODR t4 WHERE ((t2.id = t4.id AND t3.id = t4.id)) " 
            + "AND (t0.id = t3.CUSTOMER_ID) )))"; 

        execute(jpql, expectedSQL);
    }

    public void testAny() {
        String jpql = "SELECT o.name FROM CompUser o "
            + "WHERE o.address.zipCode = " + " ANY (SELECT s.computerName "
            + " FROM CompUser s WHERE s.address.country IS NOT NULL)";

        String expectedSQL = "SELECT t0.name "
            + "FROM CompUser t0 "
            + "INNER JOIN CR_ADDR t1 ON t0.ADD_ID = t1.id "
            + "WHERE (t1.zipCode = ANY ("
            + "SELECT t2.compName "
            + "FROM CompUser t2 "
            + "INNER JOIN CR_ADDR t3 ON t2.ADD_ID = t3.id WHERE (t3.country IS NOT NULL) ))";

        execute(jpql, expectedSQL);
    }

    // redundant t1
    public void testSubquery01() {
        String jpql = "select o1.id from Order o1 where o1.id in "
            + " (select distinct o.id from LineItem i, Order o"
            + " where i.quantity > 10 and o.count > 1000 and i.id = o.id)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 "
            + "WHERE (t0.id IN ("
            + "SELECT DISTINCT t2.id "
            + "FROM CR_ODR t1 JOIN CR_ODR t2 ON (1 = 1), CR_LI t3 WHERE ("
            + "(t3.quantity > ? AND (t2.count > ? AND t3.id = t2.id))) ))"; 

        execute(jpql, expectedSQL);
    }

    public void testSubquery02() {
        String jpql = "select o.id from Order o where o.customer.balanceOwed ="
            + " (select max(o2.customer.balanceOwed) from Order o2"
            + " where o.customer.id = o2.customer.id)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 "
            + "INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id WHERE (t1.balanceOwed = ("
            + "SELECT MAX(t3.balanceOwed) FROM CR_ODR t2 "
            + "INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id WHERE (t0.CUSTOMER_ID = t2.CUSTOMER_ID) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubquery03() {
        String jpql = "select o from Order o where o.customer.balanceOwed ="
            + " (select max(o2.customer.balanceOwed) from Order o2"
            + " where o.customer.id = o2.customer.id)";

        String expectedSQL = "SELECT t0.id, t0.count, t1.id, t1.accountNum, t4.id, t4.city, t4.country, "
            + "t4.county, t4.state, t4.street, t5.userid, t5.DTYPE, t5.age, t5.compName, t5.creditRating, t5.name, "
            + "t4.zipCode, t1.balanceOwed, t1.creditRating, t1.filledOrderCount, t1.firstName, t1.lastName, "
            + "t1.name, t1.status, t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 "
            + "INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_ADDR t4 ON t1.ADDRESS_ID = t4.id "
            + "LEFT OUTER JOIN CompUser t5 ON t4.id = t5.ADD_ID WHERE (t1.balanceOwed = ("
            + "SELECT MAX(t3.balanceOwed) FROM CR_ODR t2 "
            + "INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id WHERE (t0.CUSTOMER_ID = t2.CUSTOMER_ID) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubquery04() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select count(i) from o.lineItems i)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.quantity > ("
            + "SELECT COUNT(t2.id) FROM  CR_LI t1, CR_LI t2 "
            + "WHERE (t1.id = t2.id) AND (t0.id = t1.ORDER_ID) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubquery05() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select count(o.quantity) from Order o)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.quantity > ("
            + "SELECT COUNT(t1.quantity) FROM CR_ODR t1 ))";
        execute(jpql, expectedSQL);
    }

    public void testSubquery06() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select count(o.id) from Order o)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.quantity > ("
            + "SELECT COUNT(t1.id) FROM CR_ODR t1 ))";

        execute(jpql, expectedSQL);
    }

    public void testSubquery07() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select avg(o.quantity) from Order o)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.quantity > ("
            + "SELECT AVG(t1.quantity) FROM CR_ODR t1 ))";
        execute(jpql, expectedSQL);

    }

    public void testSubquery08() {
        String jpql = "select c.name from Customer c "
            + "where exists (select o from c.orders o where o.id = 1) "
            + "or exists (select o from c.orders o where o.id = 2)";

        String expectedSQL = "SELECT t0.name FROM CR_CUST t0 WHERE (EXISTS ("
            + "SELECT t2.id FROM  CR_ODR t1, CR_ODR t2 " 
            + "WHERE ((t2.id = ? AND t1.id = t2.id)) AND (t0.id = t1.CUSTOMER_ID) ) OR EXISTS ("
            + "SELECT t4.id FROM  CR_ODR t3, CR_ODR t4 "
            + "WHERE ((t4.id = ? AND t3.id = t4.id)) AND (t0.id = t3.CUSTOMER_ID) ))"; 
        execute(jpql, expectedSQL);
    }

    public void testSubquery09() {
        String jpql = "select c.name from Customer c, in(c.orders) o "
            + "where o.quantity between "
            + "(select max(o.quantity) from Order o) and "
            + "(select avg(o.quantity) from Order o) ";

        String expectedSQL = "SELECT t0.name "
            + "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE ("
            + "(t1.quantity >= (SELECT MAX(t2.quantity) FROM CR_ODR t2 ) AND "
            + "t1.quantity <= (SELECT AVG(t3.quantity) FROM CR_ODR t3 )))"; 
        execute(jpql, expectedSQL);
    }

    public void testSubquery10() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select sum(o2.quantity) from Customer c, "
            + "in(c.orders) o2) ";

        String expectedSQL = "SELECT t2.id FROM CR_ODR t2 WHERE (t2.quantity > ("
            + "SELECT SUM(t1.quantity) FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID ))";
        execute(jpql, expectedSQL);
    }

    public void testSubquery11() {
        String jpql = "select o.id from Order o where o.quantity between"
            + " (select avg(o2.quantity) from Customer c, in(c.orders) o2)"
            + " and (select min(o2.quantity) from Customer c, in(c.orders)"
            + " o2)";

        String expectedSQL = "SELECT t4.id FROM CR_ODR t4 WHERE "
            + "((t4.quantity >= ("
            + "SELECT AVG(t1.quantity) " 
            + "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID ) AND "
            + "t4.quantity <= ("
            + "SELECT MIN(t3.quantity) "
            + "FROM CR_CUST t2 INNER JOIN CR_ODR t3 ON t2.id = t3.CUSTOMER_ID )))"; 

        execute(jpql, expectedSQL);
    }

    public void testSubquery12() {
        String jpql = "select o.id from Customer c, in(c.orders)o "
            + "where o.quantity > (select sum(o2.quantity)"
            + " from c.orders o2)";

        String expectedSQL = "SELECT t1.id FROM CR_CUST t0 "
            + "INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE (t1.quantity > ("
            + "SELECT SUM(t3.quantity) FROM  CR_ODR t2, CR_ODR t3 "
            + "WHERE (t2.id = t3.id) AND (t0.id = t2.CUSTOMER_ID) ))";
        execute(jpql, expectedSQL);
    }

    public void testSubquery13() {
        String jpql = "select o1.id, c.name from Order o1, Customer c"
            + " where o1.quantity = "
            + " any(select o2.quantity from in(c.orders) o2)";

        String expectedSQL = "SELECT t0.id, t1.name " + 
        "FROM CR_ODR t0 JOIN CR_CUST t1 ON (1 = 1) WHERE (t0.quantity = ANY (" + 
        "SELECT t3.quantity FROM  CR_ODR t2, CR_ODR t3 WHERE (t2.id = t3.id) AND (t1.id = t2.CUSTOMER_ID) ))"; 

        execute(jpql, expectedSQL);
    }

    public void testSubquery14() {
        String jpql = "SELECT p, m FROM Publisher p "
            + "LEFT OUTER JOIN p.magazineCollection m "
            + "WHERE m.id = (SELECT MAX(m2.id) FROM Magazine m2 "
            + "WHERE m2.idPublisher.id = p.id AND m2.id = "
            + "(SELECT MAX(m3.id) FROM Magazine m3 "
            + "WHERE m3.idPublisher.id = p.id)) ";

        String expectedSQL = "SELECT t0.id, t1.id, t1.date_published, t1.id_publisher, t1.name "
            + "FROM Publisher t0 LEFT OUTER JOIN Magazine t1 ON t0.id = t1.id_publisher WHERE "
            + "(t1.id = ("
            + "SELECT MAX(t2.id) FROM Magazine t2 WHERE (("
            + "t2.id_publisher = t0.id AND "
            + "t2.id = (SELECT MAX(t3.id) FROM Magazine t3 WHERE (t3.id_publisher = t0.id) ))) ))"; 
        execute(jpql, expectedSQL);
    }

    public void testSubquery15() {
        String jpql = "select o.id from Order o where o.delivered =(select "
            + "   CASE WHEN o2.quantity > 10 THEN true"
            + "     WHEN o2.quantity = 10 THEN false "
            + "     ELSE false END from Order o2"
            + " where o.customer.id = o2.customer.id)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.delivered = ("
            + "SELECT  CASE  WHEN t1.quantity > ? THEN 1 WHEN t1.quantity = ? THEN 0 ELSE 0 END  "
            + "FROM CR_ODR t1 WHERE (t0.CUSTOMER_ID = t1.CUSTOMER_ID) ))";
        execute(jpql, expectedSQL);
    }

    public void testSubquery16() {
        String jpql = "select o1.id from Order o1 where o1.quantity > "
            + " (select o.quantity*2 from LineItem i, Order o"
            + " where i.quantity > 10 and o.quantity > 1000 and i.id = "
            + "o.id)";
        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.quantity > ("
            + "SELECT (t2.quantity * ?) FROM CR_ODR t1 JOIN CR_ODR t2 ON (1 = 1), CR_LI t3 WHERE ("
            + "(t3.quantity > ? AND (t2.quantity > ? AND t3.id = t2.id))) ))"; 
        execute(jpql, expectedSQL);
    }

    public void testSubquery17() {
        String jpql = "select o.id from Order o where o.customer.name ="
            + " (select substring(o2.customer.name, 3) from Order o2"
            + " where o.customer.id = o2.customer.id)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 "
            + "INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id WHERE (t1.name = ("
            + "SELECT SUBSTR(CAST((t3.name) AS VARCHAR(1000)), 3) "
            + "FROM CR_ODR t2 INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id "
            + "WHERE (t0.CUSTOMER_ID = t2.CUSTOMER_ID) ))";
        execute(jpql, expectedSQL);
    }

    public void testSubquery18() {
        String jpql = "select o.id from Order o where o.orderTs >"
            + " (select CURRENT_TIMESTAMP from o.lineItems i)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.orderTs > ("
            + "SELECT CURRENT_TIMESTAMP FROM  CR_LI t1, CR_LI t2 WHERE (t1.id = t2.id) AND "
            + "(t0.id = t1.ORDER_ID) ))";
        execute(jpql, expectedSQL);
    }

    public void testSubquery19() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select SQRT(o.quantity) from Order o where o.delivered"
            + " = true)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.quantity > ("
            + "SELECT SQRT(t1.quantity) FROM CR_ODR t1 WHERE (t1.delivered = ?) ))";
        execute(jpql, expectedSQL);
    }

    public void testSubquery20() {
        String jpql = "select o.id from Order o where o.customer.name in"
            + " (select CONCAT(o.customer.name, 'XX') from Order o"
            + " where o.quantity > 10)";

        String expectedSQL = "SELECT t2.id FROM CR_ODR t2 "
            + "INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id WHERE (t3.name IN ("
            + "SELECT (CAST(t1.name AS VARCHAR(1000)) || CAST(? AS VARCHAR(1000))) "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id WHERE (t0.quantity > ?) ))";

        execute(jpql, expectedSQL);
    }

    public void testSubquery21() {
        String jpql = "select c from Customer c where c.creditRating ="
            + " (select " + "   CASE WHEN o2.quantity > 10 THEN "
            + "          criteria1.Customer$CreditRating.POOR "
            + "        WHEN o2.quantity = 10 THEN "
            + "          criteria1.Customer$CreditRating.GOOD "
            + "        ELSE "
            + "          criteria1.Customer$CreditRating.EXCELLENT "
            + "        END " + "   from Order o2 "
            + "   where c.id = o2.customer.id)";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t2.id, t2.city, t2.country, t2.county, t2.state, " +
        "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, " + 
        "t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, " + 
        "t0.status " + 
        "FROM CR_CUST t0 " + 
        "LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID " + 
        "WHERE (t0.creditRating = (" + 
        "SELECT  CASE  WHEN t1.quantity > ? THEN 0 WHEN t1.quantity = ? THEN 1 ELSE 2 END  " + 
        "FROM CR_ODR t1 WHERE (t0.id = t1.CUSTOMER_ID) ))";
        execute(jpql, expectedSQL);
    }

    //enum literal should be integer, not string
    public void atestSubquery22() {
        String jpql = "select c from Customer c "
            + "where c.creditRating = (select COALESCE (c1.creditRating, "
            + "criteria1.Customer$CreditRating.POOR) "
            + "from Customer c1 where c1.name = 'Famzy') order by c.name "
            + "DESC";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t2.id, t2.city, t2.country, t2.county, t2.state, " + 
        "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, " + 
        "t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, " + 
        "t0.status " + 
        "FROM CR_CUST t0 " + 
        "LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID WHERE (t0.creditRating = (" + 
        "SELECT  COALESCE(t1.creditRating,'POOR') FROM CR_CUST t1 WHERE (t1.name = ?) )) " + 
        "ORDER BY t0.name DESC";
        execute(jpql, expectedSQL);

    }

    public void testSubquery23() {
        String jpql = "select c from Customer c "
            + "where c.creditRating = (select NULLIF (c1.creditRating, "
            + "org.apache.openjpa.persistence.criteria."
            + "Customer$CreditRating.POOR) "
            + "from Customer c1 where c1.name = 'Famzy') "
            + "order by c.name DESC";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t2.id, t2.city, t2.country, t2.county, " + 
        "t2.state, t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, " + 
        "t2.zipCode, t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, " + 
        "t0.name, t0.status " + 
        "FROM CR_CUST t0 " + 
        "LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CompUser t3 ON t2.id = t3.ADD_ID WHERE (t0.creditRating = (" + 
        "SELECT  NULLIF(t1.creditRating,0) FROM CR_CUST t1 WHERE (t1.name = ?) )) " + 
        "ORDER BY t0.name DESC";
        execute(jpql, expectedSQL);

    }

    public void testSubquery24() {
        String jpql = "select o from Order o where o.count > (select count(o) from Order o)";
        String expectedSQL = "SELECT t0.id, t0.count, t2.id, t2.accountNum, t3.id, t3.city, t3.country, " + 
        "t3.county, t3.state, t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, " + 
        "t4.name, t3.zipCode, t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, " + 
        "t2.lastName, t2.name, t2.status, t0.delivered, t0.name, t0.orderTs, t0.quantity, " + 
        "t0.totalCost " + 
        "FROM CR_ODR t0 " + 
        "LEFT OUTER JOIN CR_CUST t2 ON t0.CUSTOMER_ID = t2.id " + 
        "LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id " + 
        "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID WHERE (t0.count > (" + 
        "SELECT COUNT(t1.id) FROM CR_ODR t1 ))";
        execute(jpql, expectedSQL);

    }

    public void testSubquery25() {
        String jpql = "select o from Order o where o.count > (select count(o2) from Order o2)";

        String expectedSQL = "SELECT t0.id, t0.count, t2.id, t2.accountNum, t3.id, t3.city, t3.country, " + 
        "t3.county, t3.state, t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, " + 
        "t4.name, t3.zipCode, t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, " + 
        "t2.lastName, t2.name, t2.status, t0.delivered, t0.name, t0.orderTs, t0.quantity, " + 
        "t0.totalCost " + 
        "FROM CR_ODR t0 " + 
        "LEFT OUTER JOIN CR_CUST t2 ON t0.CUSTOMER_ID = t2.id " + 
        "LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id " + 
        "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID WHERE (t0.count > (" + 
        "SELECT COUNT(t1.id) FROM CR_ODR t1 ))";
        execute(jpql, expectedSQL);
    }

    /**
     * this test scenario must use Dependent.java and DependentId.java in
     * org.apache.openjpa.persistence.query package.
     */
    public void atestSubSelectMaxDateRange() {
        String jpql = "SELECT e,d from Employee e, Dependent d "
            + "WHERE e.empId = :empid "
            + "AND d.id.empid = (SELECT MAX (e2.empId) FROM Employee e2) "
            + "AND d.id.effDate > :minDate "
            + "AND d.id.effDate < :maxDate ";
        String expectedSQL = "SELECT t0.empId, t1.effDate, t1.empid, t1.name " + 
        "FROM CR_EMP t0 JOIN SUBQ_DEPENDENT t1 ON (1 = 1) " + 
        "WHERE (t0.empId = ? " + 
        "AND t1.empid = (SELECT MAX(t2.empId) FROM CR_EMP t2 ) " + 
        "AND t1.effDate > ? AND t1.effDate < ?)";

        sql.clear();
        Query jQ = em.createQuery(jpql);
        jQ.setParameter("empid", (long) 101);
        jQ.setParameter("minDate", new Date(100));
        jQ.setParameter("maxDate", new Date(100000));

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
    
    public void testCorrelatedNestedSubquery1() {
        String jpql = "Select Object (c) From Customer c Where Not Exists ("
                + "   Select a.id From Account As a Where "
                + "        a.customer = c  And "
                + "        exists (select o.id from Order o where o.customer = c and o.count = 1))";

        String expectedSQL = "SELECT t1.id, t1.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, "
            + "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, "
            + "t1.balanceOwed, t1.creditRating, t1.filledOrderCount, t1.firstName, t1.lastName, t1.name, "
            + "t1.status "
            + "FROM CR_CUST t1 LEFT OUTER JOIN CR_ADDR t3 ON t1.ADDRESS_ID = t3.id "
            + "LEFT OUTER JOIN CompUser t4 ON t3.id = t4.ADD_ID WHERE (NOT (EXISTS ("
            + "SELECT t0.id FROM CR_ACCT t0 WHERE ((t0.CUSTOMER_ID = t1.id AND EXISTS ("
            + "SELECT t2.id FROM CR_ODR t2 WHERE ((t2.CUSTOMER_ID = t1.id AND t2.count = ?)) ))) )))";
        
        execute(jpql, expectedSQL);
    }
    
    public void testCorrelatedNestedSubquery1a() {
        String jpql = "Select Object (o) From Product o Where Not Exists ("
            + "   Select a.id From Account As a Where "
            + "        a.product = o  And "
            + "        exists (select r.id from Request r where r.account = a and r.status = 1))";

        String expectedSQL = "SELECT t1.pid, t1.version, t1.productType FROM CR_PRDT t1 WHERE (NOT (EXISTS ("
            + "SELECT t0.id FROM CR_ACCT t0 WHERE ((t0.PRODUCT_PID = t1.pid AND EXISTS ("
            + "SELECT t2.id FROM Request t2 WHERE ((t2.ACCOUNT_ID = t0.id AND t2.status = ?)) ))) )))";
        
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
