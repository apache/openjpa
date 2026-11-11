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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.metamodel.Bindable;
import org.junit.jupiter.api.Test;

/**
 * Tests Criteria Queries that use Join.
 *
 * @author Pinaki Poddar
 *
 */
class TestJoinCondition extends JoinDomainTestCase {
    @Test
    void singleAttributeJoinModel() {
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b);
        assertInstanceOf(Bindable.class, b.getModel());
        assertSame(B.class, b.getJavaType());
    }

    @Test
    void collectionJoinModel() {
        CriteriaQuery<?> cq = cb.createQuery();
        Root<C> c = cq.from(C.class);
        CollectionJoin<C,D> d = c.join(C_.coll);
        assertSame(D.class, d.getJavaType());
    }

    @Test
    void setJoinModel() {
        CriteriaQuery<?> cq = cb.createQuery();
        Root<C> c = cq.from(C.class);
        SetJoin<C,D> d = c.join(C_.set);
        assertSame(D.class, d.getJavaType());
    }

    @Test
    void listJoinModel() {
        CriteriaQuery<?> cq = cb.createQuery();
        Root<C> c = cq.from(C.class);
        ListJoin<C,D> d = c.join(C_.list);
        assertSame(D.class, d.getJavaType());
    }

    @Test
    void innerJoinSingleAttributeWithoutCondition() {
        String jpql = "select a from A a INNER JOIN a.b b";
        CriteriaQuery<A> c = cb.createQuery(A.class);
        c.from(A.class).join(A_.b, JoinType.INNER);

        assertEquivalence(c, jpql);
    }

    @Test
    void crossJoinWithoutCondition() {
        String jpql = "select a from A a, C c";
        CriteriaQuery<A> cq = cb.createQuery(A.class);
        Root<A> a = cq.from(A.class);
        Root<C> c = cq.from(C.class);
        cq.select(a);
        assertEquivalence(cq, jpql);
    }

    @Test
    void crossJoinWithoutCondition1() {
        String jpql = "select a, c from A a, C c";
        CriteriaQuery<?> cq = cb.createQuery();
        Root<A> a = cq.from(A.class);
        Root<C> c = cq.from(C.class);
        cq.multiselect(a, c);

        assertEquivalence(cq, jpql);
    }

    @Test
    void crossJoin() {
        String jpql = "select a from A a, C c where a.name=c.name";
        CriteriaQuery<A> cq = cb.createQuery(A.class);
        Root<A> a = cq.from(A.class);
        Root<C> c = cq.from(C.class);
        cq.where(cb.equal(a.get(A_.name), c.get(C_.name)));
        cq.select(a);
        assertEquivalence(cq, jpql);
    }

    @Test
    void innerJoinSingleAttribute() {
        String jpql = "select a from A a INNER JOIN a.b b WHERE a.id=b.age";
        CriteriaQuery<A> cq = cb.createQuery(A.class);
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b);
        cq.where(cb.equal(a.get(A_.id), b.get(B_.age)));

        assertEquivalence(cq, jpql);
    }

    @Test
    void outerJoinSingleAttributeWithoutCondition() {
        String jpql = "select a from A a LEFT JOIN a.b b";
        CriteriaQuery<A> cq = cb.createQuery(A.class);
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b, JoinType.LEFT);

        assertEquivalence(cq, jpql);
    }

    @Test
    void outerJoinSingleAttribute() {
        String jpql = "select a from A a LEFT JOIN a.b b where a.id=b.age";
        CriteriaQuery<A> cq = cb.createQuery(A.class);
        Root<A> a = cq.from(A.class);
        Join<A,B> b = a.join(A_.b, JoinType.LEFT);
        cq.where(cb.equal(a.get(A_.id), b.get(B_.age)));

        assertEquivalence(cq, jpql);
    }

    @Test
    void setJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.set d";
        CriteriaQuery<C> c = cb.createQuery(C.class);
        c.from(C.class).join(C_.set);

        assertEquivalence(c, jpql);
    }

    @Test
    void listJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.list d";
        CriteriaQuery<C> c = cb.createQuery(C.class);
        c.from(C.class).join(C_.list);

        assertEquivalence(c, jpql);
    }

    @Test
    void collectionJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.coll d";
        CriteriaQuery<C> c = cb.createQuery(C.class);
        c.from(C.class).join(C_.coll);

        assertEquivalence(c, jpql);
    }

    @Test
    void mapJoinWithoutCondition() {
        String jpql = "select c from C c JOIN c.map d";
        CriteriaQuery<C> c = cb.createQuery(C.class);
        c.from(C.class).join(C_.map);

        assertEquivalence(c, jpql);
    }

    @Test
    void keyExpression() {
        String jpql = "select c from C c JOIN c.map d where KEY(d)=33";
        CriteriaQuery<C> cq = cb.createQuery(C.class);
        Root<C> c = cq.from(C.class);
        MapJoin<C,Integer,D> d = c.join(C_.map);
        cq.where(cb.equal(d.key(),33));

        assertEquivalence(cq, jpql);
    }

    @Test
    void valueExpression() {
        String jpql = "select c from C c JOIN c.map d where VALUE(d).name='xy'";
        CriteriaQuery<C> cq = cb.createQuery(C.class);
        Root<C> c = cq.from(C.class);
        MapJoin<C,Integer,D> d = c.join(C_.map);
        cq.where(cb.equal(d.value().get(D_.name),"xy"));

        assertEquivalence(cq, jpql);
    }

    @Test
    void fetchJoin() {
        String jpql = "select a from A a JOIN FETCH a.b";

        CriteriaQuery<A> cq = cb.createQuery(A.class);
        Root<A> a = cq.from(A.class);
        a.fetch(A_.b);

        assertEquivalence(cq, jpql);
    }

    @Test
    void outerFetchJoin() {
        String jpql = "select a from A a LEFT JOIN FETCH a.b";

        CriteriaQuery<A> cq = cb.createQuery(A.class);
        Root<A> a = cq.from(A.class);
        a.fetch(A_.b, JoinType.LEFT);

        assertEquivalence(cq, jpql);
    }
}
