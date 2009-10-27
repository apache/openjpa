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
package org.apache.openjpa.persistence.subquery;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests subquery restructure code.
 * 
 */
public class TestJPQLSubquery extends SingleEMFTestCase {
    SQLAuditor auditor;
    public void setUp() {
        auditor = new SQLAuditor();    
        setUp(
                Account.class, Address.class, CompUser.class, Contact.class,
                Customer.class, Department.class, Employee.class, Exempt.class,
                DependentId.class, Dependent.class, FrequentFlierPlan.class,
                LineItem.class, Magazine.class, Manager.class,
                Order.class, Phone.class, Product.class,
                Publisher.class, Request.class, Person.class,
                "openjpa.jdbc.JDBCListeners", new JDBCListener[] {  auditor },
                DROP_TABLES
                );
    }    


    public void testSubqueries1() {
        String jpql = "SELECT goodCustomer FROM Customer goodCustomer WHERE "
            + "goodCustomer.balanceOwed < (SELECT AVG(c.balanceOwed) "
            + " FROM Customer c)";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t2.id, t2.city, t2.country, t2.county, t2.state, "
            + "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, "
            + "t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, t0.status "
            + "FROM CR_CUST t0 LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id "
            + "LEFT OUTER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID "
            + "WHERE (t0.balanceOwed < (SELECT AVG(t1.balanceOwed) FROM CR_CUST t1))";

        executeAndCompareSQL(jpql, expectedSQL);
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
            + "LEFT OUTER JOIN CR_FFPLAN t5 ON t1.FREQUENTFLIERPLAN_ID = t5.id "
            + "LEFT OUTER JOIN CR_MGR t6 ON t1.MANAGER_ID = t6.id "
            + "LEFT OUTER JOIN CR_EMP t8 ON t1.SPOUSE_EMPID = t8.empId "
            + "LEFT OUTER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID "
            + "LEFT OUTER JOIN CR_DEPT t7 ON t6.DEPARTMENT_DEPTNO = t7.deptNo "
            + "WHERE (EXISTS (SELECT t0.empId FROM CR_EMP t0 WHERE (t0.empId = t1.SPOUSE_EMPID)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubqueries3() {
        String jpql = "SELECT emp FROM Employee emp WHERE emp.salary > ALL ("
            + "SELECT m.salary FROM Manager m WHERE m.department = "
            + "emp.department)";

        String expectedSQL = "SELECT t0.empId, t0.EMP_TYPE, " +
            "t2.id, t2.city, t2.country, t2.county, t2.state, t2.street, " +
            "t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, t4.deptNo, " +
            "t4.name, t5.id, t5.annualMiles, t5.name, t6.id, t7.deptNo, t7.name, t6.name, t6.salary, t0.name, " +
            "t0.rating, t0.salary, t8.empId, t8.EMP_TYPE, t8.ADDRESS_ID, t8.DEPARTMENT_DEPTNO, " +
            "t8.FREQUENTFLIERPLAN_ID, t8.MANAGER_ID, t8.name, t8.rating, t8.salary, t8.hireDate, t0.hireDate " +
            "FROM CR_EMP t0 LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id " +
            "LEFT OUTER JOIN CR_DEPT t4 ON t0.DEPARTMENT_DEPTNO = t4.deptNo " +
            "LEFT OUTER JOIN CR_FFPLAN t5 ON t0.FREQUENTFLIERPLAN_ID = t5.id " +
            "LEFT OUTER JOIN CR_MGR t6 ON t0.MANAGER_ID = t6.id " +
            "LEFT OUTER JOIN CR_EMP t8 ON t0.SPOUSE_EMPID = t8.empId " +
            "LEFT OUTER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID " +
            "LEFT OUTER JOIN CR_DEPT t7 ON t6.DEPARTMENT_DEPTNO = t7.deptNo " +
            "WHERE (CAST(t0.salary AS NUMERIC) > ALL (SELECT t1.salary " +
            "FROM CR_MGR t1 WHERE (t1.DEPARTMENT_DEPTNO = t0.DEPARTMENT_DEPTNO)))";
        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubqueries4() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM c.orders o) > 10";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, " +
            "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, " +
            "t0.balanceOwed, t0.creditRating, " +
            "t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, t0.status " +
            "FROM CR_CUST t0 LEFT OUTER JOIN CR_ADDR t3 ON t0.ADDRESS_ID = t3.id " +
            "LEFT OUTER JOIN CR_COMPUSER t4 ON t3.id = t4.ADD_ID " +
            "WHERE ((SELECT COUNT(t2.id) FROM CR_ODR t1, CR_ODR t2 " +
            "WHERE (t1.id = t2.id) AND (t0.id = t1.CUSTOMER_ID)) > ?)";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubqueries4a() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o WHERE c = c1) > 10";

        String expectedSQL = "SELECT t2.id, t2.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, "
            + "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, " 
            + "t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, t2.name, t2.status "
            + "FROM CR_CUST t2 LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id "
            + "LEFT OUTER JOIN CR_COMPUSER t4 ON t3.id = t4.ADD_ID "
            + "WHERE ((SELECT COUNT(t1.id) "
            + "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE (t2.id = t0.id)) > ?)";

        executeAndCompareSQL(jpql, expectedSQL);
    }    

    public void testSubqueries4b() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o WHERE c.id = c1.id) > 10";

        String expectedSQL = "SELECT t2.id, t2.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, "
            + "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, " 
            + "t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, t2.name, t2.status "
            + "FROM CR_CUST t2 LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id "
            + "LEFT OUTER JOIN CR_COMPUSER t4 ON t3.id = t4.ADD_ID "
            + "WHERE ((SELECT COUNT(t1.id) "
            + "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE (t2.id = t0.id)) > ?)";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubqueries4c() {
        String jpql = "SELECT c FROM Customer c WHERE "
            + "(SELECT COUNT(o) FROM Customer c1 JOIN c1.orders o) > 10";

        String expectedSQL = "SELECT t2.id, t2.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, "
            + "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, "
            + "t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, t2.name, t2.status "
            + "FROM CR_CUST t2 LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id "
            + "LEFT OUTER JOIN CR_COMPUSER t4 ON t3.id = t4.ADD_ID "
            + "WHERE ((SELECT COUNT(t1.id) FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID) > ?)";

        executeAndCompareSQL(jpql, expectedSQL);
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
            + "LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID "
            + "WHERE ((SELECT COUNT(t1.id) "
            + "FROM CR_CUST t0 "
            + "INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID "
            + "INNER JOIN CR_ADDR t4 ON t0.ADDRESS_ID = t4.id "
            + "WHERE (t3.county = t4.county)) > ?)";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubqueries4e() {
        String jpql = "SELECT c FROM Customer c WHERE EXISTS "
            + "(SELECT o.id FROM Order o WHERE o.customer = c)";

        String expectedSQL = "SELECT t1.id, t1.accountNum, t2.id, t2.city, t2.country, t2.county, t2.state, " + 
        "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, t1.balanceOwed, " +
        "t1.creditRating, t1.filledOrderCount, t1.firstName, t1.lastName, t1.name, t1.status " + 
        "FROM CR_CUST t1 " + 
        "LEFT OUTER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID WHERE " + 
        "(EXISTS (SELECT t0.id FROM CR_ODR t0 WHERE (t0.CUSTOMER_ID = t1.id)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // This test results in SQLException:
    // Syntax error: Encountered &quot;ALL&quot; at line 1, column 550. 
    //{SELECT t3.id, t3.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, 
    // t5.county, t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, 
    // t6.creditRating, t6.name, t5.zipCode, t4.balanceOwed, t4.creditRating, 
    // t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, t3.delivered,
    // t3.name, t3.orderTs, t3.quantity, t3.totalCost 
    // FROM CR_ODR t3 LEFT OUTER JOIN CR_CUST t4 ON t3.CUSTOMER_ID = t4.id
    // LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id
    // LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID 
    // WHERE (CAST(? AS BIGINT) < 
    // CAST(ALL (SELECT t2.balance 
    // FROM CR_CUST t0 INNER JOIN CR_CUST_CR_ACCT t1 ON t0.id = t1.CUSTOMER_ID
    // INNER JOIN CR_ACCT t2 ON t1.ACCOUNTS_ID = t2.id 
    // WHERE (t3.CUSTOMER_ID = t0.id)) AS BIGINT))} [code=30000, state=42X01]
    public void ctestSubqueries5() {
        String jpql = "SELECT o FROM Order o WHERE 10000 < ALL ("
            + "SELECT a.balance FROM o.customer c JOIN c.accounts a)";

        String expectedSQL = "SELECT t3.id, t3.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, "
            + "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, "
            + "t3.delivered, t3.name, t3.orderTs, t3.quantity, t3.totalCost " 
            + "FROM CR_ODR t3 LEFT OUTER JOIN CR_CUST t4 ON t3.CUSTOMER_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id " 
            + "LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID " 
            + "WHERE (? < ALL (SELECT t2.balance FROM CR_CUST t0 INNER JOIN CR_CUST_CR_ACCT t1 ON "
            + "t0.id = t1.CUSTOMER_ID INNER JOIN CR_ACCT t2 ON t1.ACCOUNTS_ID = t2.id " 
            + "WHERE (t3.CUSTOMER_ID = t0.id)))";

        executeAndCompareSQL(jpql, expectedSQL);
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
            + "LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID "
            + "WHERE (t3.name = ANY (SELECT t2.name "
            + "FROM CR_CUST t0 INNER JOIN CR_CUST_CR_ACCT t1 ON t0.id = t1.CUSTOMER_ID "
            + "INNER JOIN CR_ACCT t2 ON t1.ACCOUNTS_ID = t2.id WHERE (t3.CUSTOMER_ID = t0.id)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // This test results in SQLException:
    //Syntax error: Encountered &quot;ALL&quot; at line 1, column 598. 
    //{SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, 
    //t5.country, t5.county, t5.state, t5.street, t6.userid, t6.DTYPE, 
    //t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, 
    //t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, 
    //t4.lastName, t4.name, t4.status, t0.delivered, t0.name, t0.orderTs, 
    //t0.quantity, t0.totalCost 
    //FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id 
    //LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id 
    //LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id 
    //LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID 
    //WHERE (CAST(? AS BIGINT) < 
    //CAST(ALL (SELECT t3.balance 
    //FROM CR_CUST_CR_ACCT t2, CR_ACCT t3 
    //WHERE (t2.ACCOUNTS_ID = t3.id) AND (t1.id = t2.CUSTOMER_ID)) AS BIGINT))}
    //[code=30000, state=42X01]
    public void ctestSubqueries6() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT a.balance FROM c.accounts a)";

        String expectedSQL = "SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, "
            + "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID WHERE (? < ALL ("
            + "SELECT t3.balance FROM CR_CUST_CR_ACCT t2, CR_ACCT t3 WHERE (t2.ACCOUNTS_ID = t3.id) AND "
            + "(t1.id = t2.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
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
            + "LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID WHERE (t0.name = ANY ("
            + "SELECT t3.name FROM CR_CUST_CR_ACCT t2, CR_ACCT t3 WHERE (t2.ACCOUNTS_ID = t3.id) "
            + "AND (t1.id = t2.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
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
            + "LEFT OUTER JOIN CR_COMPUSER t5 ON t4.id = t5.ADD_ID WHERE (t0.name = ANY "
            + "(SELECT t2.county FROM CR_ADDR t2 WHERE (t1.ADDRESS_ID = t2.id)))";
        
        executeAndCompareSQL(jpql, expectedSQL);
    }

    // This test results in SQLException:
    //Syntax error: Encountered &quot;ALL&quot; at line 1, column 598. 
    //{SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, 
    //t5.country, t5.county, t5.state, t5.street, t6.userid, t6.DTYPE, 
    //t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, 
    //t4.balanceOwed, t4.creditRating, t4.filledOrderCount, 
    //t4.firstName, t4.lastName, t4.name, t4.status, t0.delivered, 
    //t0.name, t0.orderTs, t0.quantity, t0.totalCost 
    //FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id 
    //LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id 
    //LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id 
    //LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID 
    //WHERE (CAST(? AS BIGINT) < 
    //CAST(ALL (SELECT t3.age 
    //FROM CR_ADDR t2 INNER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID 
    //WHERE (t1.ADDRESS_ID = t2.id)) AS BIGINT))} [code=30000, state=42X01]
    public void ctestSubqueries6c() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address a JOIN a.user u)";

        String expectedSQL = "SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, "
            + "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID WHERE (? < ALL ("
            + "SELECT t3.age FROM CR_ADDR t2 INNER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID "
            + "WHERE (t1.ADDRESS_ID = t2.id)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // This test results in SQLException:
    //Syntax error: Encountered &quot;ALL&quot; at line 1, column 645. 
    //{SELECT t2.id, t2.count, t5.id, t5.accountNum, t6.id, t6.city, 
    //t6.country, t6.county, t6.state, t6.street, t7.userid, t7.DTYPE, 
    //t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, 
    //t5.balanceOwed, t5.creditRating, t5.filledOrderCount, 
    //t5.firstName, t5.lastName, t5.name, t5.status, t2.delivered, 
    //t2.name, t2.orderTs, t2.quantity, t2.totalCost 
    //FROM CR_ODR t2 INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id 
    //LEFT OUTER JOIN CR_CUST t5 ON t2.CUSTOMER_ID = t5.id 
    //INNER JOIN CR_ADDR t4 ON t3.ADDRESS_ID = t4.id 
    //LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id 
    //LEFT OUTER JOIN CR_COMPUSER t7 ON t6.id = t7.ADD_ID 
    //WHERE (CAST(? AS BIGINT) < 
    //CAST(ALL (SELECT t1.age 
    //FROM CR_ADDR t0 INNER JOIN CR_COMPUSER t1 ON t0.id = t1.ADD_ID 
    //WHERE (t4.city = t0.city AND t3.ADDRESS_ID = t0.id)) AS BIGINT) AND 1 = 1)} 
    //[code=30000, state=42X01]
    public void ctestSubqueries6d() {
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
            + "LEFT OUTER JOIN CR_COMPUSER t7 ON t6.id = t7.ADD_ID WHERE (? < ALL ("
            + "SELECT t1.age FROM CR_ADDR t0 INNER JOIN CR_COMPUSER t1 ON t0.id = t1.ADD_ID "
            + "WHERE (t4.city = t0.city AND "
            + "t3.ADDRESS_ID = t0.id)) AND 1 = 1)";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // This test results in SQLException:
    //Syntax error: Encountered &quot;ALL&quot; at line 1, column 645. 
    //{SELECT t0.id, t0.count, t5.id, t5.accountNum, t6.id, t6.city,
    //t6.country, t6.county, t6.state, t6.street, t7.userid, 
    //t7.DTYPE, t7.age, t7.compName, t7.creditRating, t7.name, 
    //t6.zipCode, t5.balanceOwed, t5.creditRating, t5.filledOrderCount,
    //t5.firstName, t5.lastName, t5.name, t5.status, t0.delivered, t0.name,
    //t0.orderTs, t0.quantity, t0.totalCost 
    //FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id 
    //LEFT OUTER JOIN CR_CUST t5 ON t0.CUSTOMER_ID = t5.id 
    //INNER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id 
    //LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id 
    //LEFT OUTER JOIN CR_COMPUSER t7 ON t6.id = t7.ADD_ID 
    //WHERE (CAST(? AS BIGINT) < 
    //CAST(ALL (SELECT t4.age 
    //FROM CR_COMPUSER t3, CR_COMPUSER t4 
    //WHERE (t3.userid = t4.userid) AND (t2.id = t3.ADD_ID)) AS BIGINT) AND 1 = 1)}
    //[code=30000, state=42X01]
    public void ctestSubqueries6e() {
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
            + "LEFT OUTER JOIN CR_COMPUSER t7 ON t6.id = t7.ADD_ID "
            + "WHERE (? < ALL (SELECT t4.age FROM CR_COMPUSER t3, CR_COMPUESR t4 "
            + "WHERE (t3.userid = t4.userid) AND (t2.id = t3.ADD_ID)) AND 1 = 1)";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // This test results in SQLException:
    //Syntax error: Encountered &quot;ALL&quot; at line 1, column 598. 
    //{SELECT t0.id, t0.count, t5.id, t5.accountNum, t6.id, t6.city,
    //t6.country, t6.county, t6.state, t6.street, t7.userid, t7.DTYPE,
    //t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode,
    //t5.balanceOwed, t5.creditRating, t5.filledOrderCount, 
    //t5.firstName, t5.lastName, t5.name, t5.status, t0.delivered,
    //t0.name, t0.orderTs, t0.quantity, t0.totalCost 
    //FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id 
    //LEFT OUTER JOIN CR_CUST t5 ON t0.CUSTOMER_ID = t5.id 
    //LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id 
    //LEFT OUTER JOIN CR_COMPUSER t7 ON t6.id = t7.ADD_ID 
    //WHERE (CAST(? AS BIGINT) < 
    //CAST(ALL (SELECT t4.age 
    //FROM CR_ADDR t2 INNER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID, CR_COMPUSER t4 
    //WHERE (t3.userid = t4.userid) AND (t1.ADDRESS_ID = t2.id)) AS BIGINT))}
    //[code=30000, state=42X01]
    public void ctestSubqueries6f() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address.user u)";

        String expectedSQL = "SELECT t0.id, t0.count, t5.id, t5.accountNum, "
            + "t6.id, t6.city, t6.country, t6.county, "
            + "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, "
            + "t7.creditRating, t7.name, t6.zipCode, "
            + "t5.balanceOwed, t5.creditRating, t5.filledOrderCount, t5.firstName, "
            + "t5.lastName, t5.name, t5.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t5 ON t0.CUSTOMER_ID = t5.id "
            + "LEFT OUTER JOIN CR_ADDR t6 ON t5.ADDRESS_ID = t6.id "
            + "LEFT OUTER JOIN CR_COMPUSER t7 ON t6.id = t7.ADD_ID "
            + "WHERE (? < ALL (SELECT t4.age "
            + "FROM CR_ADDR t2 INNER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID, CR_COMPUSER t4 "
            + "WHERE (t3.userid = t4.userid) AND (t1.ADDRESS_ID = t2.id)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // This test results in SQLException:
    //Syntax error: Encountered &quot;ALL&quot; at line 1, column 598. 
    //{SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city,
    //t5.country, t5.county, t5.state, t5.street, t6.userid, t6.DTYPE, 
    //t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, 
    //t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName,
    //t4.lastName, t4.name, t4.status, t0.delivered, t0.name, t0.orderTs,
    //t0.quantity, t0.totalCost 
    //FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id 
    //LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id 
    //LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id 
    //LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID 
    //WHERE (CAST(? AS BIGINT) < 
    //CAST(ALL (SELECT t3.age 
    //FROM CR_ADDR t2 INNER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID 
    //WHERE (t1.ADDRESS_ID = t2.id)) AS BIGINT))} [code=30000, state=42X01]
    public void ctestSubqueries6g() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c WHERE 10000 < "
            + "ALL (SELECT u.age FROM c.address a JOIN a.user u)";

        String expectedSQL = "SELECT t0.id, t0.count, t4.id, t4.accountNum, t5.id, t5.city, t5.country, t5.county, "
            + "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, t5.zipCode, "
            + "t4.balanceOwed, t4.creditRating, t4.filledOrderCount, t4.firstName, t4.lastName, t4.name, t4.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 "
            + "INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t4 ON t0.CUSTOMER_ID = t4.id "
            + "LEFT OUTER JOIN CR_ADDR t5 ON t4.ADDRESS_ID = t5.id "
            + "LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID WHERE (? < ALL ("
            + "SELECT t3.age "
            + "FROM CR_ADDR t2 "
            + "INNER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID WHERE (t1.ADDRESS_ID = t2.id)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // This test results in SQLException:
    //Syntax error: Encountered &quot;ALL&quot; at line 1, column 592. 
    //{SELECT t0.id, t0.count, t1.id, t1.accountNum, t6.id, t6.city,
    //t6.country, t6.county, t6.state, t6.street, t7.userid, t7.DTYPE, 
    //t7.age, t7.compName, t7.creditRating, t7.name, t6.zipCode, 
    //t1.balanceOwed, t1.creditRating, t1.filledOrderCount, t1.firstName, 
    //t1.lastName, t1.name, t1.status, t0.delivered, t0.name, t0.orderTs, 
    //t0.quantity, t0.totalCost 
    //FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id 
    //INNER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id 
    //LEFT OUTER JOIN CR_ADDR t6 ON t1.ADDRESS_ID = t6.id 
    //LEFT OUTER JOIN CR_COMPUSER t7 ON t6.id = t7.ADD_ID 
    //WHERE (CAST(? AS BIGINT) < 
    //CAST(ALL (SELECT t5.age 
    //FROM CR_CUST t3, CR_COMPUSER t4, CR_COMPUSER t5 
    //WHERE (t4.userid = t5.userid) AND (t0.CUSTOMER_ID = t3.id) AND (t2.id = t4.ADD_ID))
    //AS BIGINT))} [code=30000, state=42X01]
    public void ctestSubqueries6h() {
        String jpql = "SELECT o FROM Order o JOIN o.customer.address a WHERE 10000 < "
            + "ALL (SELECT u.age FROM a.user u)";

        String expectedSQL = "SELECT t0.id, t0.count, t1.id, t1.accountNum, "
            + "t6.id, t6.city, t6.country, t6.county, "
            + "t6.state, t6.street, t7.userid, t7.DTYPE, t7.age, t7.compName, "
            + "t7.creditRating, t7.name, t6.zipCode, "
            + "t1.balanceOwed, t1.creditRating, t1.filledOrderCount, t1.firstName, "
            + "t1.lastName, t1.name, t1.status, "
            + "t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "INNER JOIN CR_ADDR t2 ON t1.ADDRESS_ID = t2.id "
            + "LEFT OUTER JOIN CR_ADDR t6 ON t1.ADDRESS_ID = t6.id "
            + "LEFT OUTER JOIN CR_COMPUSER t7 ON t6.id = t7.ADD_ID WHERE (? < ALL ("
            + "SELECT t5.age FROM  CR_CUST t3,  CR_COMPUSER t4, CR_COMPUSER t5 "
            + "WHERE (t4.userid = t5.userid) AND (t0.CUSTOMER_ID = t3.id) AND (t2.id = t4.ADD_ID)))";
      
        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testExist1() {
        String jpql = "SELECT DISTINCT c.name FROM CompUser c WHERE EXISTS"
            + " (SELECT a FROM Address a WHERE a = c.address )";

        String expectedSQL = "SELECT DISTINCT t1.name FROM CR_COMPUSER t1 WHERE (EXISTS ("
            + "SELECT t0.id FROM CR_ADDR t0 WHERE (t0.id = t1.ADD_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testExist1a() {
        String jpql = "SELECT DISTINCT o.name FROM CompUser o WHERE EXISTS"
            + " (SELECT s FROM CompUser s WHERE s.address.country = "
            + "o.address.country)";

        String expectedSQL = "SELECT DISTINCT t2.name " + "FROM CR_COMPUSER t2 "
        + "INNER JOIN CR_ADDR t3 ON t2.ADD_ID = t3.id "
        + "WHERE (EXISTS (" + "SELECT t0.userid " + "FROM CR_COMPUSER t0 "
        + "INNER JOIN CR_ADDR t1 ON t0.ADD_ID = t1.id "
        + "WHERE (t1.country = t3.country)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }
    
    public void testExist1b() {
        String jpql = "select c from Customer c left join c.orders o where exists"
            + " (select o2 from c.orders o2 where o2 = o)";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t5.id, t5.city, t5.country, t5.county, " +
            "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, " +
            "t5.zipCode, t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, " +
            "t0.name, t0.status " +
            "FROM CR_CUST t0 " +
            "LEFT OUTER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID " +
            "LEFT OUTER JOIN CR_ODR t2 ON t0.id = t2.CUSTOMER_ID " +
            "LEFT OUTER JOIN CR_ADDR t5 ON t0.ADDRESS_ID = t5.id " +
            "LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID WHERE (EXISTS (" +
            "SELECT t4.id FROM CR_ODR t3, CR_ODR t4 " +
            "WHERE (t2.id = t4.id AND t3.id = t4.id) AND (t0.id = t3.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testNotExist1() {
        String jpql = "SELECT DISTINCT c.name FROM CompUser c WHERE NOT EXISTS"
            + " (SELECT a FROM Address a WHERE a = c.address )";

        String expectedSQL = "SELECT DISTINCT t1.name FROM CR_COMPUSER t1 WHERE (NOT (EXISTS ("
            + "SELECT t0.id FROM CR_ADDR t0 WHERE (t0.id = t1.ADD_ID))))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testNotExist1a() {
        String jpql = "select c from Customer c left join c.orders o where not exists"
            + " (select o2 from c.orders o2 where o2 = o)";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t5.id, t5.city, t5.country, t5.county, " +
            "t5.state, t5.street, t6.userid, t6.DTYPE, t6.age, t6.compName, t6.creditRating, t6.name, " +
            "t5.zipCode, t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, " +
            "t0.name, t0.status " +
            "FROM CR_CUST t0 " +
            "LEFT OUTER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID " +
            "LEFT OUTER JOIN CR_ODR t2 ON t0.id = t2.CUSTOMER_ID " +
            "LEFT OUTER JOIN CR_ADDR t5 ON t0.ADDRESS_ID = t5.id " +
            "LEFT OUTER JOIN CR_COMPUSER t6 ON t5.id = t6.ADD_ID WHERE (NOT (EXISTS (" +
            "SELECT t4.id FROM CR_ODR t3, CR_ODR t4 " +
            "WHERE (t2.id = t4.id AND t3.id = t4.id) AND (t0.id = t3.CUSTOMER_ID))))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testAny() {
        String jpql = "SELECT o.name FROM CompUser o "
            + "WHERE o.address.zipCode = ANY (SELECT s.computerName "
            + " FROM CompUser s WHERE s.address.country IS NOT NULL)";

        String expectedSQL = "SELECT t0.name "
            + "FROM CR_COMPUSER t0 "
            + "INNER JOIN CR_ADDR t1 ON t0.ADD_ID = t1.id "
            + "WHERE (t1.zipCode = ANY ("
            + "SELECT t2.compName "
            + "FROM CR_COMPUSER t2 "
            + "INNER JOIN CR_ADDR t3 ON t2.ADD_ID = t3.id WHERE (t3.country IS NOT NULL)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery01() {
        String jpql = "select o1.id from Order o1 where o1.id in "
            + " (select distinct o.id from LineItem i, Order o"
            + " where i.quantity > 10 and o.count > 1000 and i.id = o.id)";

        // redundant t1
        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.id IN (" +
            "SELECT DISTINCT t2.id " +
            "FROM CR_ODR t1 JOIN CR_ODR t2 ON (1 = 1), CR_LI t3 WHERE (" +
            "CAST(t3.quantity AS BIGINT) > CAST(? AS BIGINT) " +
            "AND CAST(t2.count AS BIGINT) > CAST(? AS BIGINT) " +
            "AND t3.id = t2.id)))";
        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery02() {
        String jpql = "select o.id from Order o where o.customer.balanceOwed ="
            + " (select max(o2.customer.balanceOwed) from Order o2"
            + " where o.customer.id = o2.customer.id)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 "
            + "INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id WHERE (t1.balanceOwed = ("
            + "SELECT MAX(t3.balanceOwed) FROM CR_ODR t2 "
            + "INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id WHERE (t0.CUSTOMER_ID = t2.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
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
            + "LEFT OUTER JOIN CR_COMPUSER t5 ON t4.id = t5.ADD_ID WHERE (t1.balanceOwed = ("
            + "SELECT MAX(t3.balanceOwed) FROM CR_ODR t2 "
            + "INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id WHERE (t0.CUSTOMER_ID = t2.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery04() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select count(i) from o.lineItems i)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 " +
            "WHERE (CAST(t0.quantity AS BIGINT) > " +
            "CAST((SELECT COUNT(t2.id) " +
            "FROM CR_LI t1, CR_LI t2 " +
            "WHERE (t1.id = t2.id) AND (t0.id = t1.ORDER_ID)) AS BIGINT))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery05() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select count(o.quantity) from Order o)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 " +
            "WHERE (CAST(t0.quantity AS BIGINT) > " +
            "CAST((SELECT COUNT(t1.quantity) " +
            "FROM CR_ODR t1) AS BIGINT))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery06() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select count(o.id) from Order o)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (CAST(t0.quantity AS BIGINT) > " +
            "CAST((SELECT COUNT(t1.id) FROM CR_ODR t1) AS BIGINT))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery07() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select avg(o.quantity) from Order o)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.quantity > ("
            + "SELECT AVG(t1.quantity) FROM CR_ODR t1))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery08() {
        String jpql = "select c.name from Customer c "
            + "where exists (select o from c.orders o where o.id = 1) "
            + "or exists (select o from c.orders o where o.id = 2)";

        String expectedSQL = "SELECT t0.name FROM CR_CUST t0 WHERE (EXISTS (" +
            "SELECT t2.id FROM CR_ODR t1, CR_ODR t2 " +
            "WHERE (CAST(t2.id AS BIGINT) = CAST(? AS BIGINT) AND t1.id = t2.id) " +
            "AND (t0.id = t1.CUSTOMER_ID)) OR EXISTS (" +
            "SELECT t4.id FROM CR_ODR t3, CR_ODR t4 " +
            "WHERE (CAST(t4.id AS BIGINT) = CAST(? AS BIGINT) AND t3.id = t4.id) " +
            "AND (t0.id = t3.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery09() {
        String jpql = "select c.name from Customer c, in(c.orders) o "
            + "where o.quantity between "
            + "(select max(o.quantity) from Order o) and "
            + "(select avg(o.quantity) from Order o) ";

        String expectedSQL = "SELECT t0.name "
            + "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE ("
            + "t1.quantity >= (SELECT MAX(t2.quantity) FROM CR_ODR t2) AND "
            + "t1.quantity <= (SELECT AVG(t3.quantity) FROM CR_ODR t3))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery10() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select sum(o2.quantity) from Customer c, "
            + "in(c.orders) o2) ";

        String expectedSQL = "SELECT t2.id FROM CR_ODR t2 WHERE (CAST(t2.quantity AS BIGINT) > " +
            "CAST((SELECT SUM(t1.quantity) FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID) " +
            "AS BIGINT))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery11() {
        String jpql = "select o.id from Order o where o.quantity between"
            + " (select avg(o2.quantity) from Customer c, in(c.orders) o2)"
            + " and (select min(o2.quantity) from Customer c, in(c.orders)"
            + " o2)";

        String expectedSQL = "SELECT t4.id FROM CR_ODR t4 WHERE (t4.quantity >= ("
            + "SELECT AVG(t1.quantity) "
            + "FROM CR_CUST t0 INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID) AND "
            + "t4.quantity <= ("
            + "SELECT MIN(t3.quantity) "
            + "FROM CR_CUST t2 INNER JOIN CR_ODR t3 ON t2.id = t3.CUSTOMER_ID))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery12() {
        String jpql = "select o.id from Customer c, in(c.orders)o "
            + "where o.quantity > (select sum(o2.quantity)"
            + " from c.orders o2)";

        String expectedSQL = "SELECT t1.id FROM CR_CUST t0 " +
            "INNER JOIN CR_ODR t1 ON t0.id = t1.CUSTOMER_ID WHERE (CAST(t1.quantity AS BIGINT) > " +
            "CAST((SELECT SUM(t3.quantity) " +
            "FROM CR_ODR t2, CR_ODR t3 WHERE (t2.id = t3.id) AND (t0.id = t2.CUSTOMER_ID)) AS BIGINT))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery13() {
        String jpql = "select o1.id, c.name from Order o1, Customer c"
            + " where o1.quantity = "
            + " any(select o2.quantity from in(c.orders) o2)";

        String expectedSQL = "SELECT t0.id, t1.name " +
            "FROM CR_ODR t0 JOIN CR_CUST t1 ON (1 = 1) WHERE (t0.quantity = ANY (" +
            "SELECT t3.quantity FROM CR_ODR t2, CR_ODR t3 WHERE (t2.id = t3.id) AND (t1.id = t2.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery14() {
        String jpql = "SELECT p, m FROM Publisher p "
            + "LEFT OUTER JOIN p.magazineCollection m "
            + "WHERE m.id = (SELECT MAX(m2.id) FROM Magazine m2 "
            + "WHERE m2.idPublisher.id = p.id AND m2.id = "
            + "(SELECT MAX(m3.id) FROM Magazine m3 "
            + "WHERE m3.idPublisher.id = p.id)) ";

        String expectedSQL = "SELECT t0.id, t1.id, t1.date_published, t1.id_publisher, t1.name "
            + "FROM CR_PSH t0 LEFT OUTER JOIN CR_MG t1 ON t0.id = t1.id_publisher WHERE (t1.id = ("
            + "SELECT MAX(t2.id) FROM CR_MG t2 WHERE ("
            + "t2.id_publisher = t0.id AND "
            + "t2.id = (SELECT MAX(t3.id) FROM CR_MG t3 WHERE (t3.id_publisher = t0.id)))))";

        executeAndCompareSQL(jpql, expectedSQL);
    }
 
    // not supported in JPA1
    public void ntestSubquery15() {
        String jpql = "select o.id from Order o where o.delivered =(select "
            + "   CASE WHEN o2.quantity > 10 THEN true"
            + "     WHEN o2.quantity = 10 THEN false "
            + "     ELSE false END from Order o2"
            + " where o.customer.id = o2.customer.id)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.delivered = ("
            + "SELECT  CASE  WHEN t1.quantity > ? THEN 1 WHEN t1.quantity = ? THEN 0 ELSE 0 END  "
            + "FROM CR_ODR t1 WHERE (t0.CUSTOMER_ID = t1.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);

    }

    // not supported in JPA1
    public void ntestSubquery16() {
        String jpql = "select o1.id from Order o1 where o1.quantity > "
            + " (select o.quantity*2 from LineItem i, Order o"
            + " where i.quantity > 10 and o.quantity > 1000 and i.id = "
            + "o.id)";
        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.quantity > ("
            + "SELECT (t2.quantity * ?) FROM CR_ODR t1 JOIN CR_ODR t2 ON (1 = 1), CR_LI t3 WHERE ("
            + "t3.quantity > ? AND t2.quantity > ? AND t3.id = t2.id)))";

        executeAndCompareSQL(jpql, expectedSQL);

    }

    // not supported in JPA1  
    public void ntestSubquery17() {
        String jpql = "select o.id from Order o where o.customer.name ="
            + " (select substring(o2.customer.name, 3) from Order o2"
            + " where o.customer.id = o2.customer.id)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 "
            + "INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id WHERE (t1.name = ("
            + "SELECT SUBSTR(CAST((t3.name) AS VARCHAR(1000)), 3) "
            + "FROM CR_ODR t2 INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id "
            + "WHERE (t0.CUSTOMER_ID = t2.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // not supported in JPA1
    public void ntestSubquery18() {
        String jpql = "select o.id from Order o where o.orderTs >"
            + " (select CURRENT_TIMESTAMP from o.lineItems i)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.orderTs > ("
            + "SELECT CURRENT_TIMESTAMP FROM CR_LI t1 WHERE "
            + "(t0.id = t1.ORDER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // not supported in JPA1
    public void ntestSubquery19() {
        String jpql = "select o.id from Order o where o.quantity >"
            + " (select SQRT(o.quantity) from Order o where o.delivered"
            + " = true)";

        String expectedSQL = "SELECT t0.id FROM CR_ODR t0 WHERE (t0.quantity > ("
            + "SELECT SQRT(t1.quantity) FROM CR_ODR t1 WHERE (t1.delivered = ?)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // not supported in JPA1
    public void ntestSubquery20() {
        String jpql = "select o.id from Order o where o.customer.name in"
            + " (select CONCAT(o.customer.name, 'XX') from Order o"
            + " where o.quantity > 10)";

        String expectedSQL = "SELECT t2.id FROM CR_ODR t2 "
            + "INNER JOIN CR_CUST t3 ON t2.CUSTOMER_ID = t3.id WHERE (t3.name IN ("
            + "SELECT (CAST(t1.name AS VARCHAR(1000)) || CAST(? AS VARCHAR(1000))) "
            + "FROM CR_ODR t0 INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id WHERE (t0.quantity > ?)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // not supported in JPA1
    public void ntestSubquery21() {
        String jpql = "select c from Customer c where c.creditRating ="
            + " (select CASE WHEN o2.quantity > 10 THEN "
            + "            Customer$CreditRating.POOR "
            + "        WHEN o2.quantity = 10 THEN "
            + "            Customer$CreditRating.GOOD "
            + "        ELSE "
            + "            Customer$CreditRating.EXCELLENT "
            + "        END from Order o2 "
            + "   where c.id = o2.customer.id)";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t2.id, t2.city, t2.country, t2.county, t2.state, " +
        "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, " + 
        "t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, " + 
        "t0.status " + 
        "FROM CR_CUST t0 " + 
        "LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID " + 
        "WHERE (t0.creditRating = (" + 
        "SELECT  CASE  WHEN t1.quantity > ? THEN 0 WHEN t1.quantity = ? THEN 1 ELSE 2 END  " + 
        "FROM CR_ODR t1 WHERE (t0.id = t1.CUSTOMER_ID)))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // not supported in JPA1
    public void ntestSubquery22() {
        String jpql = "select c from Customer c "
            + "where c.creditRating = (select COALESCE (c1.creditRating, "
            + "org.apache.openjpa.persistence.criteria."
            + "Customer$CreditRating.POOR) "
            + "from Customer c1 where c1.name = 'Famzy') order by c.name "
            + "DESC";

        String expectedSQL = "SELECT t0.id, t0.accountNum, t2.id, t2.city, t2.country, t2.county, t2.state, " + 
        "t2.street, t3.userid, t3.DTYPE, t3.age, t3.compName, t3.creditRating, t3.name, t2.zipCode, " + 
        "t0.balanceOwed, t0.creditRating, t0.filledOrderCount, t0.firstName, t0.lastName, t0.name, " + 
        "t0.status " + 
        "FROM CR_CUST t0 " + 
        "LEFT OUTER JOIN CR_ADDR t2 ON t0.ADDRESS_ID = t2.id " + 
        "LEFT OUTER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID WHERE (t0.creditRating = (" + 
        "SELECT  COALESCE(t1.creditRating,0) FROM CR_CUST t1 WHERE (t1.name = ?))) " + 
        "ORDER BY t0.name DESC";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    // not supported in JPA1
    public void ntestSubquery23() {
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
        "LEFT OUTER JOIN CR_COMPUSER t3 ON t2.id = t3.ADD_ID WHERE (t0.creditRating = (" + 
        "SELECT  NULLIF(t1.creditRating,0) FROM CR_CUST t1 WHERE (t1.name = ?))) " + 
        "ORDER BY t0.name DESC";

        executeAndCompareSQL(jpql, expectedSQL);
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
            "LEFT OUTER JOIN CR_COMPUSER t4 ON t3.id = t4.ADD_ID WHERE (CAST(t0.count AS BIGINT) > " +
            "CAST((SELECT COUNT(t1.id) FROM CR_ODR t1) AS BIGINT))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testSubquery25() {
        String jpql = "select o from Order o where o.count > (select count(o2) from Order o2)";

        String expectedSQL = "SELECT t0.id, t0.count, t2.id, t2.accountNum, t3.id, t3.city, t3.country, " +
            "t3.county, t3.state, t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, " +
            "t4.name, t3.zipCode, t2.balanceOwed, t2.creditRating, t2.filledOrderCount, t2.firstName, t2.lastName, " +
            "t2.name, t2.status, t0.delivered, t0.name, t0.orderTs, t0.quantity, " +
            "t0.totalCost " +
            "FROM CR_ODR t0 " +
            "LEFT OUTER JOIN CR_CUST t2 ON t0.CUSTOMER_ID = t2.id " +
            "LEFT OUTER JOIN CR_ADDR t3 ON t2.ADDRESS_ID = t3.id " +
            "LEFT OUTER JOIN CR_COMPUSER t4 ON t3.id = t4.ADD_ID WHERE (CAST(t0.count AS BIGINT) > " +
            "CAST((SELECT COUNT(t1.id) FROM CR_ODR t1) AS BIGINT))";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    
    public void testSubSelectMaxDateRange() {
        String jpql = "SELECT e,d from Employee e, Dependent d "
            + "WHERE e.empId = :empid "
            + "AND d.id.empid = (SELECT MAX (e2.empId) FROM Employee e2) "
            + "AND d.id.effDate > :minDate "
            + "AND d.id.effDate < :maxDate ";

        String expectedSQL = "SELECT t0.empId, t1.effDate, t1.empid, t1.name " +
            "FROM CR_EMP t0 JOIN CR_DEPENDENT t1 ON (1 = 1) " +
            "WHERE (t0.empId = ? " +
            "AND CAST(t1.empid AS BIGINT) = CAST((SELECT MAX(t2.empId) FROM CR_EMP t2) AS BIGINT) " +
            "AND t1.effDate > ? AND t1.effDate < ?)";

        EntityManager em = emf.createEntityManager();

        Query q = em.createQuery(jpql);
        q.setParameter("empid", (int) 101);
        q.setParameter("minDate", new Date(100));
        q.setParameter("maxDate", new Date(100000));

        executeQueryAndCompareSQL(q, jpql, expectedSQL);
        em.close();
    }
    
    public void testCorrelatedNestedSubquery1() {
        String jpql = "Select Object (c) From Customer c Where Not Exists ("
                + "   Select a.id From Account As a Where "
                + "        a.customer = c  And "
                + "        exists (select o.id from Order o where o.customer = c and o.count = 1))";

        String expectedSQL = "SELECT t1.id, t1.accountNum, t3.id, t3.city, t3.country, t3.county, t3.state, " +
        "t3.street, t4.userid, t4.DTYPE, t4.age, t4.compName, t4.creditRating, t4.name, t3.zipCode, " +
        "t1.balanceOwed, t1.creditRating, t1.filledOrderCount, t1.firstName, t1.lastName, t1.name, " +
        "t1.status " +
        "FROM CR_CUST t1 LEFT OUTER JOIN CR_ADDR t3 ON t1.ADDRESS_ID = t3.id " +
        "LEFT OUTER JOIN CR_COMPUSER t4 ON t3.id = t4.ADD_ID WHERE (NOT (EXISTS (" +
        "SELECT t0.id FROM CR_ACCT t0 WHERE (t0.CUSTOMER_ID = t1.id AND EXISTS (" +
        "SELECT t2.id FROM CR_ODR t2 WHERE (t2.CUSTOMER_ID = t1.id AND CAST(t2.count AS BIGINT) = " +
        "CAST(? AS BIGINT)))))))";

        executeAndCompareSQL(jpql, expectedSQL);
    }
    
    public void testCorrelatedNestedSubquery1a() {
        String jpql = "Select Object (o) From Product o Where Not Exists ("
            + "   Select a.id From Account As a Where "
            + "        a.product = o  And "
            + "        exists (select r.id from Request r where r.account = a and r.status = 1))";

        String expectedSQL = "SELECT t1.pid, t1.version, t1.productType FROM CR_PRD t1 WHERE (NOT (EXISTS (" +
            "SELECT t0.id FROM CR_ACCT t0 WHERE (t0.PRODUCT_PID = t1.pid AND EXISTS (" +
            "SELECT t2.id FROM CR_REQ t2 WHERE (t2.ACCOUNT_ID = t0.id AND CAST(t2.status AS BIGINT) = " +
            "CAST(? AS BIGINT)))))))";

        executeAndCompareSQL(jpql, expectedSQL);
    }
    
    public void testPluralCorrelatedJoin1() {
        String jpql = "SELECT o.quantity, o.totalCost, "
            + "a.zipCode FROM Customer c JOIN c.orders o JOIN c.address a "
            + "WHERE a.state = " 
            + "(SELECT o.name from Customer c1 JOIN c1.orders o1 where o.quantity = o1.quantity)";
        
        String expectedSQL = "SELECT t2.quantity, t2.totalCost, t1.zipCode "
            + "FROM CR_CUST t0 INNER JOIN CR_ODR t2 ON t0.id = t2.CUSTOMER_ID "
            + "INNER JOIN CR_ADDR t1 ON t0.ADDRESS_ID = t1.id "
            + "WHERE (t1.state = "
            + "(SELECT t2.name "
            + "FROM CR_CUST t3 INNER JOIN CR_ODR t4 ON t3.id = t4.CUSTOMER_ID "
            + "WHERE (t2.quantity = t4.quantity)) AND 1 = 1)";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testPluralCorrelatedJoin2() {
        String jpql = "SELECT d.name FROM Department d JOIN d.employees e JOIN e.manager m "
           + "WHERE m.name = (SELECT e1.name from Employee e1 JOIN e1.manager m1 "
           + "where m.name = m1.name)";
        String expectedSQL = "SELECT t0.name FROM CR_DEPT t0 "
            + "INNER JOIN CR_DEPT_CR_EMP t1 ON t0.deptNo = t1.DEPARTMENT_DEPTNO "
            + "INNER JOIN CR_EMP t2 ON t1.EMPLOYEES_EMPID = t2.empId "
            + "INNER JOIN CR_EMP t6 ON t1.EMPLOYEES_EMPID = t6.empId "
            + "INNER JOIN CR_MGR t3 ON t2.MANAGER_ID = t3.id "
            + "INNER JOIN CR_MGR t7 ON t6.MANAGER_ID = t7.id "
            + "WHERE (t3.name = (SELECT t4.name FROM CR_EMP t4 "
            + "INNER JOIN CR_MGR t5 ON t4.MANAGER_ID = t5.id "
            + "WHERE (t7.name = t5.name)) AND 1 = 1)";

        executeAndCompareSQL(jpql, expectedSQL);
    }

    public void testPluralCorrelatedJoin3() {
        String jpql = "SELECT o FROM Order o JOIN o.customer c JOIN c.accounts a WHERE c.name = "
            + "ANY (SELECT a1.name FROM Account a1 WHERE a.owner = a1.owner)";
        String expectedSQL = "SELECT t0.id, t0.count, t6.id, t6.accountNum, t7.id, t7.city, "
            + "t7.country, t7.county, t7.state, t7.street, t8.userid, t8.DTYPE, t8.age, "
            + "t8.compName, t8.creditRating, t8.name, t7.zipCode, t6.balanceOwed, "
            + "t6.creditRating, t6.filledOrderCount, t6.firstName, t6.lastName, t6.name, "
            + "t6.status, t0.delivered, t0.name, t0.orderTs, t0.quantity, t0.totalCost "
            + "FROM CR_ODR t0 "
            + "INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "LEFT OUTER JOIN CR_CUST t6 ON t0.CUSTOMER_ID = t6.id "
            + "INNER JOIN CR_CUST_CR_ACCT t2 ON t1.id = t2.CUSTOMER_ID "
            + "LEFT OUTER JOIN CR_ADDR t7 ON t6.ADDRESS_ID = t7.id "
            + "INNER JOIN CR_ACCT t3 ON t2.ACCOUNTS_ID = t3.id "
            + "INNER JOIN CR_ACCT t4 ON t2.ACCOUNTS_ID = t4.id "
            + "LEFT OUTER JOIN CR_COMPUSER t8 ON t7.id = t8.ADD_ID WHERE (t1.name = "
            + "ANY (SELECT t5.name FROM CR_ACCT t5 "
            + "WHERE (t4.OWNER_ID = t5.OWNER_ID)) AND 1 = 1)";

        executeAndCompareSQL(jpql, expectedSQL);
    }    

    public void testPluralCorrelatedJoin4() {
        String jpql = 
        "SELECT o.quantity FROM Order o JOIN o.customer c JOIN c.accounts a JOIN a.owner owner WHERE c.name = "
        + "ANY (SELECT a1.name FROM Account a1 JOIN a1.owner owner1 WHERE owner.name = owner1.name)";
        String expectedSQL = "SELECT t0.quantity FROM CR_ODR t0 "
            + "INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "INNER JOIN CR_CUST_CR_ACCT t2 ON t1.id = t2.CUSTOMER_ID "
            + "INNER JOIN CR_ACCT t3 ON t2.ACCOUNTS_ID = t3.id "
            + "INNER JOIN CR_ACCT t7 ON t2.ACCOUNTS_ID = t7.id "
            + "INNER JOIN CR_PSN t4 ON t3.OWNER_ID = t4.id "
            + "INNER JOIN CR_PSN t8 ON t7.OWNER_ID = t8.id WHERE (t1.name = "
            + "ANY (SELECT t5.name "
            + "FROM CR_ACCT t5 INNER JOIN CR_PSN t6 ON t5.OWNER_ID = t6.id "
            + "WHERE (t8.name = t6.name)) AND 1 = 1)";

        executeAndCompareSQL(jpql, expectedSQL);
    }    

    public void testPluralCorrelatedJoin5() {
        String jpql = "SELECT o.quantity FROM Order o JOIN o.customer c JOIN c.accounts a WHERE c.name = "
            + "ANY (SELECT owner.name FROM a.owner owner WHERE owner.id = 1)";
        String expectedSQL = "SELECT t0.quantity FROM CR_ODR t0 "
            + "INNER JOIN CR_CUST t1 ON t0.CUSTOMER_ID = t1.id "
            + "INNER JOIN CR_CUST_CR_ACCT t2 ON t1.id = t2.CUSTOMER_ID "
            + "INNER JOIN CR_ACCT t3 ON t2.ACCOUNTS_ID = t3.id "
            + "INNER JOIN CR_ACCT t4 ON t2.ACCOUNTS_ID = t4.id "
            + "WHERE (t1.name = "
            + "ANY (SELECT t5.name FROM CR_PSN t5 WHERE ("
            + "CAST(t5.id AS BIGINT) = CAST(? AS BIGINT) AND t4.OWNER_ID = t5.id)) "
            + "AND 1 = 1)";

        executeAndCompareSQL(jpql, expectedSQL);
    }    

    void executeQueryAndCompareSQL(Query q, String jpql, String expectedSQL) {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        DBDictionary dict = conf.getDBDictionaryInstance();
        System.out.println("\n>>> JPQL: "+jpql);
        List<String> jSQL = null;
        try {
            jSQL = executeQueryAndCollectSQL(q);
        } catch (Exception e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            fail("JPQL " + jpql + " failed to execute\r\n" + w);
        }

        if (!(dict instanceof DerbyDictionary))
            return;

        for (int i = 0; i < jSQL.size(); i++) {
            if (!jSQL.get(i).equals(expectedSQL)) {
                printSQL("SQL for JPQL", jSQL.get(i));
                printSQL("Expected SQL", expectedSQL);
                assertEquals(i + "-th Expected SQL and SQL for JPQL: " + jpql + " are different", expectedSQL, jSQL
                    .get(i));
            }
        }       
    }

    void executeAndCompareSQL(String jpql, String expectedSQL) {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        DBDictionary dict = conf.getDBDictionaryInstance();
        EntityManager em = emf.createEntityManager();

        Query jQ = em.createQuery(jpql);

        System.out.println("\n>>> JPQL: "+jpql);
        List<String> jSQL = null;
        try {
            jSQL = executeQueryAndCollectSQL(jQ);
        } catch (Exception e) {
            StringWriter w = new StringWriter();
            e.printStackTrace(new PrintWriter(w));
            fail("JPQL " + jpql + " failed to execute\r\n" + w);
        }

        em.close();
        if (!(dict instanceof DerbyDictionary))
            return;

        for (int i = 0; i < jSQL.size(); i++) {
            if (!jSQL.get(i).equals(expectedSQL)) {
                printSQL("SQL for JPQL", jSQL.get(i));
                printSQL("Expected SQL", expectedSQL);
                assertEquals(i + "-th Expected SQL and SQL for JPQL: " + jpql + " are different", expectedSQL, jSQL
                    .get(i));
            }
        }
    }

    /**
     * Execute the given query and return the generated SQL. If the query execution fail because the generated SQL is
     * ill-formed, then raised exception should carry the ill-formed SQL for diagnosis.
     */
    List<String> executeQueryAndCollectSQL(Query q) {
        auditor.clear();
        try {
            q.getResultList();
        } catch (Exception e) {
            throw new RuntimeException(extractSQL(e), e);
        }
        assertFalse(auditor.getSQLs().isEmpty());
        return auditor.getSQLs();
    }

    String extractSQL(Exception e) {
        Throwable t = e.getCause();
        return "Can not extract SQL from exception " + e;
    }

    public class SQLAuditor extends AbstractJDBCListener {
        private List<String> sqls = new ArrayList<String>();

        @Override
        public void beforeExecuteStatement(JDBCEvent event) {
            if (event.getSQL() != null && sqls != null) {
                sqls.add(event.getSQL());
            }
        }

        void clear() {
            sqls.clear();
        }

        List<String> getSQLs() {
            return new ArrayList<String>(sqls);
        }
    }

    void printSQL(String header, String sql) {
        System.err.println(header);
        System.err.println(sql);
    }

    void printSQL(String header, List<String> sqls) {
        System.err.println(header);
        for (int i = 0; sqls != null && i < sqls.size(); i++) {
            System.err.println(i + ":" + sqls.get(i));
        }
    }
}
