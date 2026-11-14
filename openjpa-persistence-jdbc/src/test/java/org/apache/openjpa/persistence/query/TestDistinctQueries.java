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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.openjpa.persistence.models.company.fetchlazy.Address;
import org.apache.openjpa.persistence.models.company.fetchlazy.Company;
import org.apache.openjpa.persistence.models.company.fetchlazy.Customer;
import org.apache.openjpa.persistence.models.company.fetchlazy.Employee;
import org.apache.openjpa.persistence.models.company.fetchlazy.FullTimeEmployee;
import org.apache.openjpa.persistence.models.company.fetchlazy.LineItem;
import org.apache.openjpa.persistence.models.company.fetchlazy.PartTimeEmployee;
import org.apache.openjpa.persistence.models.company.fetchlazy.Person;
import org.apache.openjpa.persistence.models.company.fetchlazy.Product;
import org.apache.openjpa.persistence.models.company.fetchlazy.ProductOrder;
import org.apache.openjpa.persistence.test.SingleEMTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestDistinctQueries extends SingleEMTestCase {

    @BeforeEach
    public void setUp() {
        setUp(Address.class, Company.class, Customer.class, Employee.class,
            FullTimeEmployee.class, LineItem.class, PartTimeEmployee.class,
            Person.class, Product.class, ProductOrder.class, CLEAR_TABLES);

        ProductOrder order = new ProductOrder();
        LineItem item0 = new LineItem();
        LineItem item1 = new LineItem();
        LineItem item2 = new LineItem();
        order.getItems().add(item0);
        order.getItems().add(item1);
        order.getItems().add(item2);

        em.getTransaction().begin();
        em.persist(order);
        em.persist(item0);
        em.persist(item1);
        em.persist(item2);
        em.getTransaction().commit();
    }

    @Test
    void duplicateResultsInNonDistinctJoinFetchQuery() {
        List l = em.createQuery("select o from LAZ_ProductOrder o " +
            "left join fetch o.items").getResultList();
        assertEquals(3, l.size());
    }

    @Test
    void duplicateResultsInNonDistinctJoinQuery() {
        List l = em.createQuery("select o from LAZ_ProductOrder o " +
            "left join o.items item").getResultList();
        assertEquals(3, l.size());
    }

    @Test
    void noDuplicateResultsInDistinctQuery() {
        List l = em.createQuery("select distinct o from LAZ_ProductOrder o " +
            "left join o.items item").getResultList();
        assertEquals(1, l.size());
    }

    @Test
    void duplicateResultsInNonDistinctConstructorJoinQuery() {
        List l = em.createQuery("select new " +
            "org.apache.openjpa.persistence.query.TestDistinctQueries$Holder(" +
            "o.id) from LAZ_ProductOrder o " +
            "left join o.items item").getResultList();
        assertEquals(3, l.size());
    }

    @Test
    void noDuplicateResultsInDistinctConstructorQuery()
        throws Exception {
        List l = em.createQuery("select distinct new " +
            "org.apache.openjpa.persistence.query.TestDistinctQueries$Holder(" +
            "o.id) from LAZ_ProductOrder o " +
            "left join o.items item").getResultList();
        assertEquals(1, l.size());
    }

    public static class Holder {
        public Holder(long id) {
            // we don't actually do anything with the returned data
        }
    }
}
