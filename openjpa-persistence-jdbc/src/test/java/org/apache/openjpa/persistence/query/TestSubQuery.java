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

import java.util.List;
import javax.persistence.EntityManager;

import junit.textui.TestRunner;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Test queries with subselects
 *
 * @author Catalina Wei
 */
public class TestSubQuery
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(CustomerEntity.class, CustomerEntity.class,
              OrderEntity.class, OrderEntity.class);
    }

    public void testQuery() {
        EntityManager em = emf.createEntityManager();
        String[] query = new String[] {
            "select o from Customer c, in(c.orders) o where o.amount > "
                + "(select avg(o.amount) from Order o)",
            "select c from Customer c, in(c.orders) o where o.amount > "
                + "(select avg(o.amount) from in(c.orders) o)",
            "select c from Customer c where "
                + "((select sum(o.amount) from Order o where o.customer = c) "
                + "between 100 and 200) order by c.name",        
            "select o from Order o where o.amount < "
                + "(select max(o2.amount) from Order o2 where "
                + "o2.amount = o.amount)",
            "select o from Order o where o.amount > "
                + "(select avg(o.amount) from Customer c, in(c.orders) o)",
            "select o.oid from Order o where o.amount > 10 "
                + "and o.amount < (select min(o2.amount) from Order o2 where "
                + "o2.amount > 0)",
            "select o from Order o where o.amount > any " 
                + "(select o.amount from Customer c, in (c.orders) o where "
                + "c.cid = 1)",
            "select o from Order o where o.amount between "
                + "(select min(o.amount) from Customer c, in(c.orders) o) and "
                + "(select avg(o.amount) from Customer c, in(c.orders) o)"
        };
        
        int failures = 0;
        for (int i=0; i<query.length; i++) {
            try {
                List res = em.createQuery(query[i])
                    .getResultList();
            } catch (Exception e) {
                failures++;
            }
        }
        em.getTransaction().begin();
        try {
            String update = "update Order o set o.amount = o.amount + 1 where "
                + "o.oid not in (select o2.oid from Customer c, "
                + "in(c.orders) o2)";
            em.createQuery(update).executeUpdate();
        }
        catch (Exception e) {
            failures++;
        }
        em.getTransaction().commit();
        assertEquals(0, failures);
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestSubQuery.class);
    }
}

