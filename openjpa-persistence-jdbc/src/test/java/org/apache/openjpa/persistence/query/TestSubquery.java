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
import javax.persistence.Query;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Test JPQL subquery
 */
public class TestSubquery
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(Customer.class, Customer.CustomerKey.class, Order.class,
            OrderItem.class, Employee.class, Dependent.class,
            DependentId.class, CLEAR_TABLES);
    }

    static String[]  querys = new String[] {
        "select o1.oid from Order o1 where o1.oid in " +
            " (select distinct o.oid from OrderItem i, Order o" +
            " where i.quantity > 10 and o.amount > 1000 and i.lid = o.oid)" ,
        "select o.oid from Order o where o.customer.name =" +
            " (select max(o2.customer.name) from Order o2" +
            " where o.customer.cid.id = o2.customer.cid.id)",
        "select o from Order o where o.customer.name =" +
            " (select max(o2.customer.name) from Order o2" +
            " where o.customer.cid.id = o2.customer.cid.id)",
        "select o.oid from Order o where o.amount >" +
            " (select count(i) from o.lineitems i)",
        "select o.oid from Order o where o.amount >" +
            " (select count(o.amount) from Order o)",
        "select o.oid from Order o where o.amount >" +
            " (select count(o.oid) from Order o)",
        "select o.oid from Order o where o.amount >" +
            " (select avg(o.amount) from Order o)",
        "select c.name from Customer c where exists" +
            " (select o from c.orders o where o.oid = 1) or exists" +
            " (select o from c.orders o where o.oid = 2)",
        "select c.name from Customer c, in(c.orders) o where o.amount between" +
            " (select max(o.amount) from Order o) and" +
            " (select avg(o.amount) from Order o) ",
        "select o.oid from Order o where o.amount >" +
            " (select sum(o2.amount) from Customer c, in(c.orders) o2) ",   
        "select o.oid from Order o where o.amount between" +
            " (select avg(o2.amount) from Customer c, in(c.orders) o2)" +
            " and (select min(o2.amount) from Customer c, in(c.orders) o2)",
        "select o.oid from Customer c, in(c.orders)o where o.amount >" +
            " (select sum(o2.amount) from c.orders o2)",
        "select o1.oid, c.name from Order o1, Customer c where o1.amount = " +
            " any(select o2.amount from in(c.orders) o2)",
    // outstanding problem subqueries:
    //"select o from Order o where o.amount > (select count(o) from Order o)",
    //"select o from Order o where o.amount > (select count(o2) from Order o2)",
    // "select c from Customer c left join c.orders p where not exists"
    //   + " (select o2 from c.orders o2 where o2 = o",
    };


    static String[] updates = new String[] {
        "update Order o set o.amount = 1000 where o.customer.name = " +
            " (select max(o2.customer.name) from Order o2 " + 
            " where o.customer.cid.id = o2.customer.cid.id)",  
    };


    public void testSubquery() {
        EntityManager em = emf.createEntityManager();
        for (int i = 0; i < querys.length; i++) {
            String q = querys[i];
            List rs = em.createQuery(q).getResultList();
            assertEquals(0, rs.size());
        }

        em.getTransaction().begin();
        for (int i = 0; i < updates.length; i++) {
            int updateCount = em.createQuery(updates[i]).executeUpdate();
            assertEquals(0, updateCount);
        }

        em.getTransaction().rollback();
        em.close();
    }

    /**
     * Verify a sub query can contain MAX and additional date comparisons 
     * without losing the correct alias information. This sort of query 
     * originally caused problems for DBDictionaries which used DATABASE syntax. 
     */
    public void testSubSelectMaxDateRange() {        
        String query =
            "SELECT e,d from Employee e, Dependent d "
                + "WHERE e.empId = :empid "
                + "AND d.id.empid = (SELECT MAX (e2.empId) FROM Employee e2) "
                + "AND d.id.effDate > :minDate "
                + "AND d.id.effDate < :maxDate ";
        EntityManager em = emf.createEntityManager();
        Query q = em.createQuery(query);
        q.setParameter("empid", (long) 101);
        q.setParameter("minDate", new Date(100));
        q.setParameter("maxDate", new Date(100000));
        q.getResultList();
        em.close();
    }
}
