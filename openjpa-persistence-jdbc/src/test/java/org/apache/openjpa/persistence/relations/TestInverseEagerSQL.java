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
package org.apache.openjpa.persistence.relations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.query.Magazine;
import org.apache.openjpa.persistence.query.Publisher;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

import junit.textui.TestRunner;


public class TestInverseEagerSQL
    extends SQLListenerTestCase {

    public int numCustomers = 1;
    public int numOrdersPerCustomer = 4;

    public int _nPeople = 3;
    public int _nPhones = 3;

    @Override
    public void setUp() {
        setUp(Customer.class, Customer.CustomerKey.class, Order.class,
            EntityAInverseEager.class, EntityA1InverseEager.class,
            EntityA2InverseEager.class, EntityBInverseEager.class,
            EntityCInverseEager.class, EntityDInverseEager.class,
            Publisher.class, Magazine.class,
            PPerson.class, PPhone.class,
            DROP_TABLES);

        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
        	return;
      	}
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Customer.CustomerKey ck = new Customer.CustomerKey("USA", 1);
        Customer c = new Customer();
        c.setCid(ck);
        c.setName("customer1");
        em.persist(c);

        for (int i = 0; i < numOrdersPerCustomer; i++) {
            Order order = new Order();
            order.setCustomer(c);
            em.persist(order);
        }

        EntityAInverseEager a = new EntityAInverseEager("a");
        em.persist(a);

        EntityA1InverseEager a1 = new EntityA1InverseEager("a1");
        em.persist(a1);

        EntityA2InverseEager a2 = new EntityA2InverseEager("a2");
        em.persist(a2);

        for (int i = 0; i < 4; i++) {
            EntityBInverseEager b = new EntityBInverseEager("b" + i);
            a.addB(b);
            b.setA(a);
            em.persist(b);
        }

        for (int i = 4; i < 8; i++) {
            EntityBInverseEager b = new EntityBInverseEager("b" + i);
            a1.addB(b);
            b.setA(a1);
            em.persist(b);
        }

        for (int i = 8; i < 12; i++) {
            EntityBInverseEager b = new EntityBInverseEager("b" + i);
            a2.addB(b);
            b.setA(a2);
            em.persist(b);
        }

        for (int i = 0; i < 4; i++) {
            EntityCInverseEager c1 = new EntityCInverseEager("c"+i, i, i);
            em.persist(c1);

            EntityDInverseEager d1 = new EntityDInverseEager("d" + i, "d" + i,
                    i, i);
            em.persist(d1);

            c1.setD(d1);
            d1.setC(c1);
        }

        Publisher p1 = new Publisher();
        p1.setName("publisher1");
        em.persist(p1);

        for (int i = 0; i < 4; i++) {
            Magazine magazine = new Magazine();
            magazine.setIdPublisher(p1);
            magazine.setName("magagine"+i+"_"+p1.getName());
            em.persist(magazine);
        }

        Publisher p2 = new Publisher();
        p2.setName("publisher2");
        em.persist(p2);

        for (int i = 0; i < 4; i++) {
            Magazine magazine = new Magazine();
            magazine.setIdPublisher(p2);
            magazine.setName("magagine"+i+"_"+p2.getName());
            em.persist(magazine);
        }

        PPerson person;
        PPhone phone;
        for(int i =0; i < _nPeople; i++) {
            person = new PPerson();
            person.setPhones(new ArrayList<>());
            em.persist(person);
            for(int j = 0; j < _nPhones; j++) {
                phone = new PPhone();
                phone.setPeople(new ArrayList<>());
                phone.getPeople().add(person);
                person.getPhones().add(phone);
                em.persist(phone);
            }
        }

        em.flush();
        em.getTransaction().commit();
        em.close();
    }

    public void testOneToManyInverseEagerQuery() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
        	return;
      	}
        sql.clear();

        OpenJPAEntityManager em = emf.createEntityManager();
        OpenJPAQuery q = em.createQuery("SELECT c FROM Customer c ");
        List<Customer> res = q.getResultList();

        assertEquals(1, res.size());

        for (Customer re : res) {
            Customer c = (Customer) re;
            Collection<Order> orders = c.getOrders();
            for (Order order : orders) {
                assertEquals(order.getCustomer(), c);
            }
        }

        assertEquals(2, sql.size());
        em.close();
    }

    public void testOneToOneInverseEagerQuery() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
			return;
		}
        sql.clear();

        OpenJPAEntityManager em = emf.createEntityManager();
        String query = "select c FROM EntityCInverseEager c";
        Query q = em.createQuery(query);
        List<EntityCInverseEager> res = q.getResultList();
        assertEquals(4, res.size());

        for (EntityCInverseEager re : res) {
            EntityCInverseEager c = (EntityCInverseEager) re;
            EntityDInverseEager d = c.getD();
            assertEquals(c, d.getC());
        }

        assertEquals(1, sql.size());
        em.close();
    }

    public void testOneToManyInheritanceQuery() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
			return;
		}
        sql.clear();

        OpenJPAEntityManager em = emf.createEntityManager();
        String query = "select a FROM EntityA1InverseEager a";
        Query q = em.createQuery(query);
        List list = q.getResultList();
        assertEquals(1, list.size());
        for (Object element : list) {
            EntityA1InverseEager a1 = (EntityA1InverseEager) element;
            Collection<EntityBInverseEager> listB = a1.getListB();
            assertEquals(4, listB.size());
            for (EntityBInverseEager b : listB) {
                EntityAInverseEager a = b.getA();
                assertEquals(a1, a);
            }
        }
        assertEquals(3, sql.size());
        sql.clear();

        query = "select a FROM EntityA2InverseEager a";
        q = em.createQuery(query);
        list = q.getResultList();
        assertEquals(1, list.size());
        for (Object item : list) {
            EntityA2InverseEager a2 = (EntityA2InverseEager) item;
            Collection listB = a2.getListB();
            assertEquals(4, listB.size());
            for (Object o : listB) {
                EntityBInverseEager b = (EntityBInverseEager) o;
                EntityAInverseEager a = b.getA();
                assertEquals(a2, a);
            }
        }
        assertEquals(3, sql.size());
        sql.clear();

        query = "select a FROM EntityAInverseEager a";
        q = em.createQuery(query);
        list = q.getResultList();
        assertEquals(3, list.size());
        for (Object value : list) {
            EntityAInverseEager a0 = (EntityAInverseEager) value;
            Collection listB = a0.getListB();
            assertEquals(4, listB.size());
            for (Object o : listB) {
                EntityBInverseEager b = (EntityBInverseEager) o;
                EntityAInverseEager a = b.getA();
                assertEquals(a0, a);
            }
        }

        assertEquals(2, sql.size());
        em.close();
    }

    public void testOneToManyEagerInverseLazyQuery() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
            return;
        }
        sql.clear();

        OpenJPAEntityManager em = emf.createEntityManager();
        String query = "select p FROM Publisher p";
        Query q = em.createQuery(query);
        List list = q.getResultList();
        assertEquals(2, list.size());
        assertEquals(2, sql.size());

        sql.clear();
        em.clear();
        for (Object o : list) {
            Publisher p = (Publisher) o;
            Set<Magazine> magazines = p.getMagazineCollection();
            assertEquals(4, magazines.size());
            for (Magazine m : magazines) {
                Publisher mp = m.getIdPublisher();
                assertEquals(p, mp);
            }
        }

        assertEquals(0, sql.size());
        em.close();
    }

    public void testManyToManyEagerEagerInverseLazyQuery() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
            return;
        }
        sql.clear();

        OpenJPAEntityManager em = emf.createEntityManager();
        String query = "select p FROM PPerson p";
        Query q = em.createQuery(query);
        List list = q.getResultList();
        assertEquals(_nPeople, list.size());
        assertEquals(7, sql.size());

        sql.clear();
        em.clear();
        for (Object o : list) {
            PPerson p = (PPerson) o;
            Collection<PPhone> phones = p.getPhones();
            assertEquals(_nPhones, phones.size());
            for (PPhone phone : p.getPhones()) {
                assertNotNull(phone.getPeople());
                assertTrue(phone.getPeople().contains(p));
            }
        }
        assertEquals(0, sql.size());
        em.close();
    }

    public void testTargetOrphanRemoval() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
			return;
		}
        OpenJPAEntityManager em = emf.createEntityManager();
        int count = count(Order.class);
        assertEquals(numOrdersPerCustomer * numCustomers, count);

        Customer.CustomerKey ck = new Customer.CustomerKey("USA", 1);
        Customer c = em.find(Customer.class, ck);
        Collection<Order> orders = c.getOrders();
        assertEquals(numOrdersPerCustomer, orders.size());

        // OrphanRemoval: remove target: the order will be deleted from db
        for (Order order : orders) {
            orders.remove(order);
            break;
        }
        em.getTransaction().begin();
        em.persist(c);
        em.flush();
        em.getTransaction().commit();
        em.clear();

        c = em.find(Customer.class, ck);
        orders = c.getOrders();
        assertEquals(numOrdersPerCustomer - 1, orders.size());
        count = count(Order.class);
        assertEquals(numOrdersPerCustomer * numCustomers - 1, count);
        em.clear();

        // OrphanRemoval: remove target: setOrders to null
        c = em.find(Customer.class, ck);
        c.setOrders(null);
        em.getTransaction().begin();
        em.persist(c);
        em.flush();
        em.getTransaction().commit();
        em.clear();

        count = count(Order.class);
        assertEquals(numOrdersPerCustomer * (numCustomers - 1), count);
        c = em.find(Customer.class, ck);
        orders = c.getOrders();
        if (orders != null)
            assertEquals(0, orders.size());
        em.close();
    }

    public void testSourceOrphanRemoval() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
			return;
		}
        OpenJPAEntityManager em = emf.createEntityManager();
        // OrphanRemoval: remove source
        Customer.CustomerKey ck = new Customer.CustomerKey("USA", 1);
        Customer c = em.find(Customer.class, ck);
        em.getTransaction().begin();
        em.remove(c);
        em.flush();
        em.getTransaction().commit();
        em.clear();

        int count = count(Order.class);
        assertEquals(numOrdersPerCustomer * (numCustomers - 1), count);
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestInverseEagerSQL.class);
    }
}

