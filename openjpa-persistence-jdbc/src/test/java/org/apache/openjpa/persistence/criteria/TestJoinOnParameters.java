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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a parameter used only in the ON condition of a join of a criteria
 * query is registered with the query and can be bound.
 */
public class TestJoinOnParameters extends CriteriaTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        deleteData();
        createData();
    }

    @AfterEach
    public void tearDown() throws Exception {
        deleteData();
        super.tearDown();
    }

    private void createData() {
        em.getTransaction().begin();
        Address a1 = new Address();
        a1.setCity("Rome");
        Address a2 = new Address();
        a2.setCity("Oslo");

        Customer c1 = new Customer();
        c1.setName("Alice");
        c1.setAddress(a1);
        Customer c2 = new Customer();
        c2.setName("Bob");
        c2.setAddress(a2);

        Order o1 = newOrder("big", c1);
        Order o2 = newOrder("small", c1);
        Order o3 = newOrder("small", c2);
        c1.setOrders(new HashSet<>(Set.of(o1, o2)));
        c2.setOrders(new HashSet<>(Set.of(o3)));

        em.persist(a1);
        em.persist(a2);
        em.persist(c1);
        em.persist(c2);
        em.persist(o1);
        em.persist(o2);
        em.persist(o3);
        em.getTransaction().commit();
        em.clear();
    }

    private static Order newOrder(String name, Customer customer) {
        Order order = new Order();
        order.setName(name);
        order.setCustomer(customer);
        return order;
    }

    private void deleteData() {
        em.getTransaction().begin();
        em.createQuery("delete from Order o").executeUpdate();
        em.createQuery("delete from Customer o").executeUpdate();
        em.createQuery("delete from Address o").executeUpdate();
        em.getTransaction().commit();
    }

    @Test
    public void testParameterInOnOfSingularJoin() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Customer> c = cq.from(Customer.class);
        Join<Customer, Address> a = c.join("address");
        ParameterExpression<String> city = cb.parameter(String.class, "city");
        a.on(cb.equal(a.get("city"), city));
        cq.select(c.get("name"));

        assertEquals(Set.of(city), cq.getParameters());
        assertEquals(List.of("Alice"), em.createQuery(cq).setParameter("city", "Rome").getResultList());
        assertEquals(List.of("Bob"), em.createQuery(cq).setParameter("city", "Oslo").getResultList());
    }

    @Test
    public void testParameterInOnOfCollectionJoin() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Customer> c = cq.from(Customer.class);
        SetJoin<Customer, Order> o = c.joinSet("orders");
        ParameterExpression<String> name = cb.parameter(String.class, "name");
        o.on(cb.equal(o.get("name"), name));
        cq.select(c.get("name")).distinct(true);
        cq.orderBy(cb.asc(c.get("name")));

        assertEquals(List.of("Alice"), em.createQuery(cq).setParameter("name", "big").getResultList());
        assertEquals(List.of("Alice", "Bob"), em.createQuery(cq).setParameter("name", "small").getResultList());
    }

    @Test
    public void testPositionalParameterInOn() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Customer> c = cq.from(Customer.class);
        Join<Customer, Address> a = c.join("address");
        ParameterExpression<String> city = cb.parameter(String.class);
        a.on(cb.equal(a.get("city"), city));
        cq.select(c.get("name"));

        assertEquals(List.of("Bob"), em.createQuery(cq).setParameter(city, "Oslo").getResultList());
    }

    @Test
    public void testParameterInOnOfNestedJoin() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Customer> c = cq.from(Customer.class);
        SetJoin<Customer, Order> o = c.joinSet("orders");
        Join<Order, Customer> owner = o.join("customer");
        ParameterExpression<String> name = cb.parameter(String.class, "name");
        owner.on(cb.equal(owner.get("name"), name));
        cq.select(o.get("name"));
        cq.orderBy(cb.asc(o.get("name")));

        assertEquals(List.of("big", "small"), em.createQuery(cq).setParameter("name", "Alice").getResultList());
        assertEquals(List.of("small"), em.createQuery(cq).setParameter("name", "Bob").getResultList());
    }

    @Test
    public void testParameterInOnThatReferencesTheRoot() {
        CriteriaQuery<Customer> cq = cb.createQuery(Customer.class);
        Root<Customer> c = cq.from(Customer.class);
        SetJoin<Customer, Order> o = c.joinSet("orders");
        ParameterExpression<String> name = cb.parameter(String.class, "name");
        o.on(cb.equal(o.get("customer"), c), cb.equal(o.get("name"), name));
        cq.select(c);

        List<Customer> result = em.createQuery(cq).setParameter("name", "big").getResultList();
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
    }

    @Test
    public void testParameterInOnAndWhereIsRegisteredOnce() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Customer> c = cq.from(Customer.class);
        Join<Customer, Address> a = c.join("address");
        ParameterExpression<String> city = cb.parameter(String.class, "city");
        a.on(cb.equal(a.get("city"), city));
        cq.where(cb.notEqual(c.get("name"), city));
        cq.select(c.get("name"));

        assertEquals(1, cq.getParameters().size());
        assertEquals(List.of("Alice"), em.createQuery(cq).setParameter("city", "Rome").getResultList());
    }

    @Test
    public void testParameterInOnOfSubqueryJoin() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Customer> c = cq.from(Customer.class);
        Subquery<Long> sq = cq.subquery(Long.class);
        Root<Order> so = sq.from(Order.class);
        Join<Order, Customer> owner = so.join("customer");
        ParameterExpression<String> name = cb.parameter(String.class, "name");
        owner.on(cb.equal(owner.get("name"), name));
        sq.select(owner.<Long> get("id"));
        cq.where(c.get("id").in(sq));
        cq.select(c.get("name"));

        assertEquals(List.of("Bob"), em.createQuery(cq).setParameter("name", "Bob").getResultList());
    }

    @Test
    public void testParameterInOnOfJoinOfCorrelatedRoot() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Customer> c = cq.from(Customer.class);
        Subquery<Integer> sq = cq.subquery(Integer.class);
        Root<Customer> cc = sq.correlate(c);
        SetJoin<Customer, Order> o = cc.joinSet("orders");
        ParameterExpression<String> name = cb.parameter(String.class, "name");
        o.on(cb.equal(o.get("name"), name));
        sq.select(o.<Integer> get("id"));
        cq.where(cb.exists(sq));
        cq.select(c.get("name"));
        cq.orderBy(cb.asc(c.get("name")));

        assertEquals(List.of("Alice"), em.createQuery(cq).setParameter("name", "big").getResultList());
        assertEquals(List.of("Alice", "Bob"), em.createQuery(cq).setParameter("name", "small").getResultList());
    }

    @Test
    public void testParameterInOnOfJoinOfCorrelatedJoin() {
        CriteriaQuery<String> cq = cb.createQuery(String.class);
        Root<Customer> c = cq.from(Customer.class);
        Join<Customer, Order> o = c.join("orders");
        Subquery<Long> sq = cq.subquery(Long.class);
        Join<Customer, Order> co = sq.correlate(o);
        Join<Order, Customer> owner = co.join("customer");
        ParameterExpression<String> name = cb.parameter(String.class, "name");
        owner.on(cb.equal(owner.get("name"), name));
        sq.select(owner.<Long> get("id"));
        cq.where(cb.exists(sq));
        cq.select(c.get("name")).distinct(true);

        assertEquals(Set.of(name), cq.getParameters());
        assertEquals(List.of("Bob"), em.createQuery(cq).setParameter("name", "Bob").getResultList());
    }
}
