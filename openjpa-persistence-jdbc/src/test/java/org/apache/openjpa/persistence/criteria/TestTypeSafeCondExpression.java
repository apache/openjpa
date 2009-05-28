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

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.apache.openjpa.persistence.test.AllowFailure;

/**
 * Tests type-strict version of Criteria API. The test scenarios are adapted
 * from TestEJBQLCondExpression in
 * org.apache.openjpa.persistence.jpql.expressions and TestEJBQLFunction in
 * org.apache.openjpa.persistence.jpql.functions.
 * 
 */

public class TestTypeSafeCondExpression extends CriteriaTest {

    private int userid1, userid2, userid3, userid4, userid5;
    public void setUp() {
        // super setUp() initializes a fixed domain model
        super.setUp((Object[])null); 
        createData();
    }
    
    void createData() {
        Address[] add =
            new Address[]{ new Address("43 Sansome", "SF", "USA", "94104"),
                new Address("24 Mink", "ANTIOCH", "USA", "94513"),
                new Address("23 Ogbete", "CoalCamp", "NIGERIA", "00000"),
                new Address("10 Wilshire", "Worcester", "CANADA", "80080"),
                new Address("23 Bellflower", "Ogui", "NIGERIA", "02000") };

        CompUser user1 = createUser("Seetha", "MAC", add[0], 40, true);
        CompUser user2 = createUser("Shannon", "PC", add[1], 36, false);
        CompUser user3 = createUser("Ugo", "PC", add[2], 19, true);
        CompUser user4 = createUser("Jacob", "LINUX", add[3], 10, true);
        CompUser user5 = createUser("Famzy", "UNIX", add[4], 29, false);

        startTx(em);
        em.persist(user1);
        userid1 = user1.getUserid();
        em.persist(user2);
        userid2 = user2.getUserid();
        em.persist(user3);
        userid3 = user3.getUserid();
        em.persist(user4);
        userid4 = user4.getUserid();
        em.persist(user5);
        userid5 = user5.getUserid();

        endTx(em);
        em.clear();
    }

    public void testNothingUsingCriteria() {
        String query = "SELECT o FROM CompUser o";
        CriteriaQuery c = cb.create();
        c.from(CompUser.class);
        assertEquivalence(c, query);
        List result = em.createQuery(c).getResultList();

        assertNotNull("the list is null", result);
        assertEquals("the size of the list is not 5", 5, result.size());

        em.clear();
    }

    public void testBetweenExprUsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.age BETWEEN 19 AND 40 AND " +
            "o.computerName = 'PC'";
        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        q.where(cb.and(cb.between(c.get(CompUser_.age), 19, 40), 
                cb.equal(c.get(CompUser_.computerName), "PC")));
        q.select(c.get(CompUser_.name));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("the list is null", result);
        assertEquals("they are not equal", 2, result.size());
        assertTrue("result dont contain shannon", result.contains("Shannon"));
        assertTrue("result dont contain ugo", result.contains("Ugo"));

        em.clear();
    }

    public void testNotBetweenExprUsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.age NOT BETWEEN 19 AND 40 " +
            "AND o.computerName= 'PC'";

        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        q.where(cb.and(cb.between(c.get(CompUser_.age), 19, 40).negate(), 
                cb.equal(c.get(CompUser_.computerName), "PC")));
        q.select(c.get(CompUser_.name));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("the list is null", result);
        assertEquals("they are not equal", 0, result.size());

        em.clear();
    }

    public void testInExprUsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.age IN (29, 40, 10)";
        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        q.where(cb.in(c.get(CompUser_.age)).value(29).value(40).value(10));
        q.select(c.get(CompUser_.name));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("the list is null", result);
        assertEquals(3, result.size());
        assertTrue("seetha is not in the list", result.contains("Seetha"));
        assertTrue("jacob is not in the list", result.contains("Jacob"));
        assertTrue("famzy is not in the list", result.contains("Famzy"));

        em.clear();
    }

    public void testNotInUsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.age NOT IN (29, 40, 10)";

        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        q.where(cb.in(c.get(CompUser_.age)).value(29).value(40).value(10)
            .negate());
        q.select(c.get(CompUser_.name));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Ugo"));
        assertTrue(result.contains("Shannon"));

        em.clear();
    }

    public void testLikeExprUsingCriteria1() {
        String query =
            "SELECT o.computerName FROM CompUser o WHERE o.name LIKE 'Sha%'" +
            " AND " + 
            "o.computerName NOT IN ('PC')";

        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        q.where(cb.and(
                    cb.like(c.get(CompUser_.name),"Sha%"), 
                    cb.in(c.get(CompUser_.computerName)).value("PC").negate()
                ));
        
        q.select(c.get(CompUser_.computerName));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(0, result.size());
        em.clear();
    }
    
    public void testLikeExprUsingCriteria2() {
        String query =
            "SELECT o.computerName FROM CompUser o WHERE o.name LIKE 'Sha%o_'" +
            " AND " + 
            "o.computerName NOT IN ('UNIX')";

        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        q.where(cb.and(
                    cb.like(c.get(CompUser_.name),"Sha%o_"), 
                    cb.in(c.get(CompUser_.computerName)).value("UNIX").negate()
                ));
        q.select(c.get(CompUser_.computerName));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        em.clear();
    }
    
    public void testLikeExprUsingCriteria3() {
        String query = "SELECT o.name FROM CompUser o WHERE o.name LIKE '_J%'";

        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        q.where(cb.like(c.get(CompUser_.name),"_J%"));
        q.select(c.get(CompUser_.name));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(0, result.size());
        em.clear();
    }
    
    @AllowFailure(message="Parameter processing is broken")
    public void testLikeExprUsingCriteria4() {
        String query = "SELECT o.name FROM CompUser o WHERE o.name LIKE ?1 " +
        		"ESCAPE '|'";
        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        Parameter<String> param = cb.parameter(String.class);
        q.where(cb.like(c.get(CompUser_.name), param, '|'));
        q.select(c.get(CompUser_.name));
        assertEquivalence(q, query, new Object[] {"%|_%"});
        List result = em.createQuery(q).setParameter(1, "%|_%").getResultList();

        assertNotNull(result);
        assertEquals(0, result.size());

        em.clear();
    }

    @AllowFailure(message="JPQL generates two queries, Criteria only one")
    public void testNullExprUsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.age IS NOT NULL AND " +
            "o.computerName = 'PC' ";
        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        Parameter<String> param = cb.parameter(String.class);
        q.where(cb.and(cb.notEqual(c.get(CompUser_.age), null), 
                cb.equal(c.get(CompUser_.computerName), "PC")));
        q.select(c.get(CompUser_.name));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("the list is null", result);
        assertEquals("the list size is not 2", 2, result.size());
        assertTrue("the result doesnt contain ugo", result.contains("Ugo"));
        assertTrue("the result doesnt contain shannon",
            result.contains("Shannon"));

        em.clear();
    }
    
    @AllowFailure(message="Invalid SQL for Criteria")
    public void testNullExpr2UsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.address.country IS NULL";

        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        Parameter<String> param = cb.parameter(String.class);
        q.where(cb.equal(c.get(CompUser_.address).get(Address_.country),
            null));
        q.select(c.get(CompUser_.name));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("the list is null", result);
        assertEquals("they are not equal", 0, result.size());

        em.clear();
    }
    
    // do not support isEmpty for array fields
    @AllowFailure
    public void testIsEmptyExprUsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.nicknames IS NOT EMPTY";

        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        //q.where(cb.isNotEmpty(c.get(CompUser_.nicknames)));
        q.select(c);
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("the list is null", result);
        assertEquals("they are not equal", 0, result.size());

        em.clear();
    }

    @AllowFailure
    public void testExistExprUsingCriteria() {
        String query = "SELECT DISTINCT o.name FROM CompUser o WHERE EXISTS" +
            " (SELECT c FROM Address c WHERE c = o.address )";

        CriteriaQuery q = cb.create();
        Root<CompUser> o = q.from(CompUser.class);
        Subquery<Address> sq = q.subquery(Address.class);
        sq.correlate(o);
        Root<Address> c = sq.from(Address.class);
        sq.select(c);
        sq.where(cb.equal(c, o.get(CompUser_.address)));
        q.where(cb.exists(sq));
        q.select(o.get(CompUser_.name)).distinct(true);
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("the list is null", result);
        assertEquals("they are not equal", 5, result.size());
        assertTrue("Seetha is not list", result.contains("Seetha"));
        assertTrue("Shannon is not list", result.contains("Shannon"));
        assertTrue("jacob is not list", result.contains("Jacob"));
        assertTrue("ugo is not list", result.contains("Ugo"));

        em.clear();
    }
    
    @AllowFailure
    public void testNotExistExprUsingCriteria() {
        String query =
            "SELECT DISTINCT o.name FROM CompUser o WHERE NOT EXISTS" +
                " (SELECT s FROM CompUser s WHERE s.address.country = " +
                "o.address.country)";

        CriteriaQuery q = cb.create();
        Root<CompUser> o = q.from(CompUser.class);
        Subquery<CompUser> sq = q.subquery(CompUser.class);
        sq.correlate(o);
        Root<CompUser> s = sq.from(CompUser.class);
        sq.select(s);
        sq.where(cb.equal(s.get(CompUser_.address).get(Address_.country), 
                o.get(CompUser_.address).get(Address_.country)));
        q.where(cb.exists(sq).negate());
        q.select(o.get(CompUser_.name)).distinct(true);
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("list is null", result);
        assertEquals("they are not equal", 0, result.size());

        em.clear();
    }

    @AllowFailure
    public void testAnyExprUsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.address.zipcode = ANY (" +
                " SELECT s.computerName FROM CompUser s WHERE " +
                "s.address.country IS NOT NULL )";

        CriteriaQuery q = cb.create();
        Root<CompUser> o = q.from(CompUser.class);
        q.select(o.get(CompUser_.name));
        Subquery<String> sq = q.subquery(String.class);
        Root<CompUser> s = sq.from(CompUser.class);
        sq.select(s.get(CompUser_.computerName));
        sq.where(cb.notEqual(s.get(CompUser_.address).get(Address_.country),
            null));
        q.where(cb.equal(o.get(CompUser_.address).get(Address_.zipCode), 
            cb.any(sq)));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("list is null", result);
        assertEquals("they are not equal", 0, result.size());

        em.clear();
    }
    
    @AllowFailure(message="new() in projection is badly broken")
    public void testConstructorExprUsingCriteria() {
        String query =
            "SELECT NEW org.apache.openjpa.persistence.common.apps.MaleUser(" +
            "c.name, " + 
            "c.computerName, c.address, c.age, c.userid)" +
            " FROM CompUser c WHERE c.name = 'Seetha'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        q.where(cb.equal(c.get(CompUser_.name), "Seetha"));
        q.select(cb.select(MaleUser.class, c.get(CompUser_.name), 
            c.get(CompUser_.computerName), c.get(CompUser_.address),
            c.get(CompUser_.age), c.get(CompUser_.userid)));
        
        MaleUser male = (MaleUser) em.createQuery(q).getSingleResult();

        assertNotNull("the list is null", male);
        assertEquals("the names dont match", "Seetha", male.getName());
        assertEquals("computer names dont match", "MAC",
            male.getComputerName());
        assertEquals("the ages dont match", 40, male.getAge());

        em.clear();
    }

    @AllowFailure
    public void testConcatSubStringFunc1() {
        String query = "select " +
            "CONCAT('Ablahum', SUBSTRING(e.name, LOCATE('e', e.name), 4)) " +
            "From CompUer e WHERE e.name='Seetha'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(
            cb.concat("Ablahum", 
                cb.substring(
                    e.get(CompUser_.name), 
                    cb.locate(e.get(CompUser_.name), "e"), 
                    cb.literal(4)
                 )
             )
        );
        q.where(cb.equal(e.get(CompUser_.name), "Seetha"));
        assertEquivalence(q, query);
        em.clear();
    }
    
    @AllowFailure
    public void testConcatSubStringFunc2() {
        String query = "select e.address From CompUser e where " +
        		"e.computerName = " +
            "CONCAT('Ablahum', SUBSTRING(e.name, LOCATE('e', e.name), 4)) ";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e.get(CompUser_.address));
        q.where(cb.equal(
            e.get(CompUser_.computerName),
            cb.concat("Ablahum", 
                cb.substring(
                    e.get(CompUser_.name), 
                    cb.locate(e.get(CompUser_.name), "e"), 
                    cb.literal(4)
                 )
            ))
         );
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testConcatSubStringFunc3() {
        String query = "select " +
            "CONCAT('XYZ', SUBSTRING(e.name, LOCATE('e', e.name))) " +
            "From CompUser e WHERE e.name='Ablahumeeth'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(
            cb.concat("XYZ", 
                cb.substring(
                    e.get(CompUser_.name), 
                    cb.locate(e.get(CompUser_.name), "e") 
                )
            )
        );
        q.where(cb.equal(e.get(CompUser_.name), "Ablahumeeth"));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testConcatSubStringFunc4() {
        String query = "select e.nicknames from CompUser e where e.name = " +
            "CONCAT('XYZ', SUBSTRING(e.name, LOCATE('e', e.name))) ";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e.get(CompUser_.nicknames));
        q.where(cb.equal(
            e.get(CompUser_.name),
            cb.concat("XYZ", 
                cb.substring(
                    e.get(CompUser_.name), 
                    cb.locate(e.get(CompUser_.name), "e") 
                )
            ))
        );
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testConcatFunc() {
        String query = "select " +
            "CONCAT('', '') From CompUser WHERE e.name='Seetha'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.concat("", cb.literal("")));
        q.where(cb.equal(e.get(CompUser_.name), "Seetha"));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testTrimFunc1() {
        String query = "select Trim(e.computerName) From CompUser e " +
        		"WHERE e.name='Shannon '";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.trim(e.get(CompUser_.computerName)));
        q.where(cb.equal(e.get(CompUser_.name), "Shannon "));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testTrimFunc2() {
        String query = "select e.name From CompUser e where Trim(e.name) =" +
        		"'Shannon'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.where(cb.equal(cb.trim(e.get(CompUser_.computerName)), "Shannon"));
        q.select(e.get(CompUser_.name));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testLowerFunc1() {
        String query = "select LOWER(e.name) From CompUser e WHERE " +
        		"e.computerName='UNIX'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.lower(e.get(CompUser_.name)));
        q.where(cb.equal(e.get(CompUser_.computerName), "UNIX"));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testLowerFunc2() {
        String query = "select e.age From CompUser e where LOWER(e.name)" +
        		" ='ugo'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.where(cb.equal(cb.lower(e.get(CompUser_.name)), "ugo"));
        q.select(e.get(CompUser_.age));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testUpperFunc1() {
        String query = "select UPPER(e.name) From CompUser e WHERE " +
        		"e.computerName='PC'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.upper(e.get(CompUser_.name)));
        q.where(cb.equal(e.get(CompUser_.computerName), "PC"));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testUpperFunc2() {
        String query = "select e.nicknames from CompUser e where " +
        		"UPPER(e.name)='UGO'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.where(cb.equal(cb.upper(e.get(CompUser_.name)), "UGO"));
        q.select(e.get(CompUser_.nicknames));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testLengthFunc() {
        String query = "SELECT o.name FROM CompUser o " + 
            "WHERE LENGTH(o.address.country) = 3";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.where(cb.equal(cb.length(e.get(CompUser_.name)), 3));
        q.select(e.get(CompUser_.name));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testArithmFunc1() {
        String query =
            "select ABS(e.age) From CompUser e WHERE e.name='Seetha'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.abs(e.get(CompUser_.age)));
        q.where(cb.equal(e.get(CompUser_.name), "Seetha"));
        assertEquivalence(q, query);
        em.clear();
    }
    
    @AllowFailure
    public void testArithmFunc2() {
        String query =
            "select SQRT(e.age) From CompUser e WHERE e.name='Seetha'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.sqrt(e.get(CompUser_.age)));
        q.where(cb.equal(e.get(CompUser_.name), "Seetha"));
        assertEquivalence(q, query);
        em.clear();
    }
    
    @AllowFailure
    public void testArithmFunc3() {
        String query =
            "select MOD(e.age, 4) From CompUser e WHERE e.name='Seetha'";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.mod(e.get(CompUser_.age), 4));
        q.where(cb.equal(e.get(CompUser_.name), "Seetha"));
        assertEquivalence(q, query);
        em.clear();
    }
    
    // size method can not be applied to an array field
    @AllowFailure
    public void testArithmFunc4() {
        String query = "SELECT e.name FROM CompUser e WHERE " +
        		"SIZE(e.nicknames) = 6";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        //q.where(cb.equal(cb.size(e.get(CompUser_.nicknames)), 6));
        q.select(e.get(CompUser_.name));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testGroupByHavingClause() {
        String query =
            "SELECT c.name FROM CompUser c GROUP BY c.name HAVING c.name " +
            "LIKE 'S%'";

        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.groupBy(e.get(CompUser_.name));
        q.having(cb.like(e.get(CompUser_.name), "S%"));
        q.select(e.get(CompUser_.name));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("Shannon "));
        assertTrue(result.contains("Shade"));
        assertTrue(result.contains("Seetha"));

        em.clear();
    }

    @AllowFailure
    public void testOrderByClause() {
        String query =
            "SELECT c.name FROM CompUser c WHERE c.name LIKE 'S%' " +
            "ORDER BY c.name";

        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.where(cb.like(e.get(CompUser_.name), "S%"));
        q.select(e.get(CompUser_.name));
        q.orderBy(cb.asc(e.get(CompUser_.name)));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("Shannon "));
        assertTrue(result.contains("Seetha"));
        assertTrue(result.contains("Shade"));

        em.clear();
    }

    @AllowFailure
    public void testAVGAggregFunc() {
        //To be Tested: AVG, COUNT, MAX, MIN, SUM
        String query = "SELECT AVG(e.age) FROM CompUser e";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.avg(e.get(CompUser_.age)));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(25));

        em.clear();
    }

    @AllowFailure
    public void testCOUNTAggregFunc() {
        String query = "SELECT COUNT(c.name) FROM CompUser c";

        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.count(e.get(CompUser_.name)));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(6l));

        em.clear();
    }

    @AllowFailure
    public void testMAXAggregFunc() {
        String query = "SELECT DISTINCT MAX(c.age) FROM CompUser c";

        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.max(e.get(CompUser_.age))).distinct(true);
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(36));

        em.clear();
    }

    @AllowFailure
    public void testMINAggregFunc() {
        String query = "SELECT DISTINCT MIN(c.age) FROM CompUser c";

        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.min(e.get(CompUser_.age))).distinct(true);
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(10));

        em.clear();
    }

    @AllowFailure
    public void testSUMAggregFunc() {
        String query = "SELECT SUM(c.age) FROM CompUser c";

        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.sum(e.get(CompUser_.age)));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(153l));

        em.clear();
    }

    // can not do TYPE with parameter in the IN clause
    @AllowFailure
    public void testTypeExpression1() {
        String query = "SELECT e FROM CompUser e where TYPE(e) in (?1, ?2) " +
        		"ORDER By e.name";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e);
        Parameter<Class> param1 = cb.parameter(Class.class);
        Parameter<Class> param2 = cb.parameter(Class.class);
        // q.where(cb.in(e.type()).value(param1).value(param2));
        q.orderBy(cb.asc(e.get(CompUser_.name)));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testTypeExpression2() {
        String query = "SELECT TYPE(e) FROM CompUser e where TYPE(e) <> ?1";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        Parameter<Class> param1 = cb.parameter(Class.class);
        q.select(e.type());
        q.where(cb.equal(e.type(), param1).negate());
        assertEquivalence(q, query, new Object[] { MaleUser.class });
        em.clear();
    }

    // Type literal
    // this Cartesian problem can not be rewritten to use JOIN
    @AllowFailure
    public void testTypeExpression3() {
        String query = "SELECT e, FemaleUser, a FROM Address a, FemaleUser e "
                + " where e.address IS NOT NULL";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<Address> a = q.from(Address.class);
        // Join<Address,FemaleUser> e = a.join(Address_.user);
        // q.select(cb.literal(FemaleUser.class), e.get(CompUser_.address));
        // q.where(cb.equal(e.type(), null).negate());
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testTypeExpression4() {
        String query = "SELECT e FROM CompUser e where TYPE(e) = MaleUser";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e);
        q.where(cb.equal(e.type(), cb.literal(MaleUser.class)));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testTypeExpression5() {
        String query = "SELECT e FROM CompUser e where TYPE(e) in (MaleUser)";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e);
        q.where(cb.in(e.type()).value(MaleUser.class));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testTypeExpression6() {
        String query = "SELECT e FROM CompUser e where TYPE(e) not in " +
        		"(MaleUser, FemaleUser)";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e);
        q.where(cb.in(e.type()).value(MaleUser.class).value(FemaleUser.class)
                .negate());
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testTypeExpression7() {
        String query = "SELECT TYPE(a.user) FROM Address a";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<Address> a = q.from(Address.class);
        q.select(a.get(Address_.user).type());
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testTypeExpression8() {
        String query = "SELECT MaleUser FROM Address a";
        CriteriaQuery q = cb.create();
        q = cb.create();
        Root<Address> a = q.from(Address.class);
        q.select(cb.literal(MaleUser.class));
        assertEquivalence(q, query);
        em.clear();
    }

    @AllowFailure
    public void testTypeExpression9() {
        String query = "SELECT "
                + " CASE TYPE(e) WHEN FemaleUser THEN 'Female' "
                + " ELSE 'Male' END FROM CompUser e";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.selectCase(e.type()).when(FemaleUser.class, "Female")
                .otherwise("Male"));
        assertEquivalence(q, query);

        em.clear();
    }

    @AllowFailure
    public void testCoalesceExpressions() {
        startTx(em);
        String query = "SELECT e.name, "
                + "COALESCE (e.address.country, 'Unknown')"
                + " FROM CompUser e ORDER BY e.name DESC";

        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e.get(CompUser_.name), cb.coalesce().value(
                e.get(CompUser_.address).get(Address_.country))
                .value("Unknown"));
        q.orderBy(cb.desc(e.get(CompUser_.name)));
        assertEquivalence(q, query);
        List rs = em.createQuery(q).getResultList();
        Object[] result = (Object[]) rs.get(rs.size() - 1);
        assertEquals("the name is not famzy", "Famzy", result[0]);
        assertEquals("Unknown", result[1]);

        endTx(em);
        em.clear();
    }

    @AllowFailure
    public void testNullIfExpressions() {
        startTx(em);
        String query = "SELECT e.name, NULLIF (e.address.country, 'USA')"
                + " FROM CompUser e ORDER BY e.name DESC";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e.get(CompUser_.name), cb.nullif(e.get(CompUser_.address).get(
                Address_.country), "USA"));
        q.orderBy(cb.desc(e.get(CompUser_.name)));
        assertEquivalence(q, query);

        List rs = em.createQuery(q).getResultList();
        Object[] result = (Object[]) rs.get(1);
        assertEquals("the name is not shannon ", "Shannon ", result[0]);
        assertNull("is not null", result[1]);

        endTx(em);
        em.clear();
    }

    @AllowFailure
    public void testSimpleCaseExpression1() {
        String query = "SELECT e.name, e.age+1 as cage, "
                + "CASE e.address.country WHEN 'USA' THEN 'us' "
                + " ELSE 'non-us' END as d2, e.address.country "
                + " FROM CompUser e ORDER BY cage, d2 DESC";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        Expression<Integer> cage = cb.sum(e.get(CompUser_.age), 1);
        Expression d2 = cb.selectCase(
                e.get(CompUser_.address).get(Address_.country)).when("USA",
                "us").otherwise("non-us");
        q.select(e.get(CompUser_.name), cage, d2, e.get(CompUser_.address).get(
                Address_.country));
        q.orderBy(cb.asc(cage), cb.desc(d2));
        assertEquivalence(q, query);

        List rs = em.createQuery(q).getResultList();
        Object[] result = (Object[]) rs.get(rs.size() - 1);
        assertEquals("the name is not seetha", "Seetha", result[0]);
    }

    @AllowFailure
    public void testSimpleCaseExpression2() {
        String query = "SELECT e.name, e.age+1 as cage, "
                + "CASE e.address.country WHEN 'USA'"
                + " THEN 'United-States' "
                + " ELSE e.address.country  END as d2," + " e.address.country "
                + " FROM CompUser e ORDER BY cage, d2 DESC";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        Expression cage = cb.sum(e.get(CompUser_.age), 1);
        Expression d2 = cb.selectCase(
                e.get(CompUser_.address).get(Address_.country)).when("USA",
                "United-States").otherwise(
                e.get(CompUser_.address).get(Address_.country));
        q.select(e.get(CompUser_.name), cage, d2, e.get(CompUser_.address).get(
                Address_.country));
        q.orderBy(cb.asc(cage), cb.desc(d2));
        assertEquivalence(q, query);
        List rs = em.createQuery(q).getResultList();
        Object[] result = (Object[]) rs.get(rs.size() - 1);
        assertEquals("the name is not seetha", "Seetha", result[0]);
    }

    @AllowFailure
    public void testSimpleCaseExpression3() {
        String query = "SELECT e.name, "
                + " CASE TYPE(e) WHEN FemaleUser THEN 'Female' "
                + " ELSE 'Male' END as result"
                + " FROM CompUser e WHERE e.name like 'S%' "
                + " ORDER BY e.name DESC";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(cb.selectCase(e.type()).when(FemaleUser.class, "Female")
                .otherwise("Male"));
        q.where(cb.like(e.get(CompUser_.name), "S%"));
        q.orderBy(cb.asc(e.get(CompUser_.name)));
        assertEquivalence(q, query);
        List rs = em.createQuery(q).getResultList();
        Object[] result = (Object[]) rs.get(0);
        assertEquals("the result is not female", "Female", result[1]);
        assertEquals("the name is not shannon", "Shannon ", result[0]);
        result = (Object[]) rs.get(2);
        assertEquals("the result is not male", "Male", result[1]);
        assertEquals("the name is not seetha", "Seetha", result[0]);
    }

    @AllowFailure
    public void testSimpleCaseExpression4() {
        // boolean literal in case expression
        String query = "SELECT e.name, CASE e.address.country WHEN 'USA'"
                + " THEN true ELSE false  END as b,"
                + " e.address.country FROM CompUser e order by b";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        Expression b = cb.selectCase(
                e.get(CompUser_.address).get(Address_.country)).when("USA",
                true).otherwise(false);
        q.select(e.get(CompUser_.name), b, e.get(CompUser_.address).get(
                Address_.country));
        q.where(cb.like(e.get(CompUser_.name), "S%"));
        q.orderBy(cb.asc(b));
        assertEquivalence(q, query);
        List rs = em.createQuery(q).getResultList();

        Object[] result = (Object[]) rs.get(rs.size() - 1);
        assertEquals(result[1], 1);
    }

    @AllowFailure
    public void testGeneralCaseExpression1() {
        String query = "SELECT e.name, e.age, "
                + " CASE WHEN e.age > 30 THEN e.age - 1 "
                + " WHEN e.age < 15 THEN e.age + 1 ELSE e.age + 0 "
                + " END AS cage FROM CompUser e ORDER BY cage";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        Expression cage = cb.selectCase().when(cb.gt(e.get(CompUser_.age), 30),
                cb.diff(e.get(CompUser_.age), 1)).when(
                cb.lt(e.get(CompUser_.age), 15),
                cb.sum(e.get(CompUser_.age), 1)).otherwise(
                cb.sum(e.get(CompUser_.age), 0));
        q.select(e.get(CompUser_.name), e.get(CompUser_.age), cage);
        q.orderBy(cb.asc(cage));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testGeneralCaseExpression2() {
        String query = "SELECT e.name, e.age+1 as cage, "
                + "CASE WHEN e.address.country = 'USA' "
                + " THEN 'United-States' "
                + " ELSE 'Non United-States'  END as d2,"
                + " e.address.country "
                + " FROM CompUser e ORDER BY cage, d2 DESC";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        Expression d2 = cb.selectCase()
                .when(
                        cb.equal(
                                e.get(CompUser_.address).get(Address_.country),
                                "USA"), "United-States").otherwise(
                        "Non United-States");
        Expression cage = cb.sum(e.get(CompUser_.age), 1);
        q.select(e.get(CompUser_.name), cage, d2, e.get(CompUser_.address).get(
                Address_.country));
        q.orderBy(cb.asc(cage), cb.desc(d2));
        assertEquivalence(q, query);

        List rs = em.createQuery(q).getResultList();
        Object[] result = (Object[]) rs.get(rs.size() - 1);
        assertEquals("the name is not seetha", "Seetha", result[0]);
        assertEquals("the country is not 'Non United-States'",
                "Non United-States", result[2]);
    }

    @AllowFailure
    public void testGeneralCaseExpression3() {
        String query = " select e.name, "
                + "CASE WHEN e.age = 11 THEN "
                + "org.apache.openjpa.persistence.criteria.CompUser$" +
                		"CreditRating.POOR"
                + " WHEN e.age = 35 THEN "
                + "org.apache.openjpa.persistence.criteria.CompUser$" +
                		"CreditRating.GOOD"
                + " ELSE "
                + "org.apache.openjpa.persistence.criteria.CompUser$" +
                		"CreditRating.EXCELLENT"
                + " END FROM CompUser e ORDER BY e.age";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e.get(CompUser_.name), cb.selectCase().when(
                cb.equal(e.get(CompUser_.age), 11), CompUser.CreditRating.POOR)
                .when(cb.equal(e.get(CompUser_.age), 35),
                        CompUser.CreditRating.GOOD).otherwise(
                        CompUser.CreditRating.EXCELLENT));

        q.orderBy(cb.asc(e.get(CompUser_.age)));
        assertEquivalence(q, query);
        List rs = em.createQuery(q).getResultList();
        Object[] result = (Object[]) rs.get(0);
        assertEquals("the name is not Jacob", "Jacob", result[0]);
        assertEquals("the credit rating is not 'POOR'", "POOR", result[1]);
    }

    // not sure how to write CriteriaQuery for
    // Subquery.select(SimpleCase/GeneralCase)
    @AllowFailure
    public void testGeneralCaseExpression4() {
        String query = "select e.name, e.creditRating from CompUser e "
                + "where e.creditRating = "
                + "(select "
                + "CASE WHEN e1.age = 11 THEN "
                + "org.apache.openjpa.persistence.criteria.CompUser$" +
                		"CreditRating.POOR"
                + " WHEN e1.age = 35 THEN "
                + "org.apache.openjpa.persistence.criteria.CompUser$" +
                		"CreditRating.GOOD"
                + " ELSE "
                + "org.apache.openjpa.persistence.criteria.CompUser$" +
                		"CreditRating.EXCELLENT"
                + " END from CompUser e1"
                + " where e.userid = e1.userid) ORDER BY e.age";
        CriteriaQuery q = cb.create();
        Root<CompUser> e = q.from(CompUser.class);
        q.select(e.get(CompUser_.name), e.get(CompUser_.creditRating));
        q.orderBy(cb.asc(e.get(CompUser_.age)));
        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<CompUser> e1 = sq.from(CompUser.class);
        sq.where(cb.equal(e.get(CompUser_.userid), e1.get(CompUser_.userid)));

        q.where(cb.equal(e.get(CompUser_.creditRating),
        // sq.select(
                cb.selectCase().when(cb.equal(e1.get(CompUser_.age), 11),
                        CompUser.CreditRating.POOR).when(
                        cb.equal(e1.get(CompUser_.age), 35),
                        CompUser.CreditRating.GOOD).otherwise(
                        CompUser.CreditRating.EXCELLENT)));

        q.orderBy(cb.asc(e.get(CompUser_.age)));
        assertEquivalence(q, query);
        List rs = em.createQuery(q).getResultList();
        Object[] result = (Object[]) rs.get(0);
        assertEquals("the name is not Ugo", "Ugo", result[0]);
        assertEquals("the credit rating is not 'EXCELLENT'", "EXCELLENT",
                ((CompUser.CreditRating) result[1]).name());
    }

    @AllowFailure
    public void testSubquery1() {
        String query = "select o1.id from Order o1 where o1.id in "
                + " (select distinct o.id from LineItem i, Order o"
                + " where i.quantity > 10 and o.count > 1000 and i.lid = o.id)";
        CriteriaQuery q = cb.create();
        Root<Order> o1 = q.from(Order.class);
        q.select(o1.get(Order_.id));

        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<LineItem> i = sq.from(LineItem.class);
        Join<LineItem, Order> o = i.join(LineItem_.order);
        sq.where(cb.and(cb.and(cb.gt(i.get(LineItem_.quantity), 10), cb.gt(o
                .get(Order_.count), 1000)), cb.equal(i.get(LineItem_.id), o
                .get(Order_.id))));
        sq.select(o.get(Order_.id)).distinct(true);
        q.where(cb.in(o1.get(Order_.id)).value(
                sq.select(o.get(Order_.id)).distinct(true)));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery2() {
        String query = "select o.id from Order o where o.customer.balanceOwed ="
                + " (select max(o2.customer.balanceOwed) from Order o2"
                + " where o.customer.id = o2.customer.id)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));
        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<Order> o2 = sq.from(Order.class);
        sq.where(cb.equal(o.get(Order_.customer).get(Customer_.id), o2.get(
                Order_.customer).get(Customer_.id)));
        q.where(cb.equal(o.get(Order_.customer).get(Customer_.balanceOwed), sq
                .select(cb.max(o2.get(Order_.customer).get(
                        Customer_.balanceOwed)))));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery3() {
        String query = "select o from Order o where o.customer.balanceOwed ="
                + " (select max(o2.customer.balanceOwed) from Order o2"
                + " where o.customer.id = o2.customer.id)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o);
        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<Order> o2 = sq.from(Order.class);
        sq.where(cb.equal(o.get(Order_.customer).get(Customer_.id), o2.get(
                Order_.customer).get(Customer_.id)));
        q.where(cb.equal(o.get(Order_.customer).get(Customer_.balanceOwed), sq
                .select(cb.max(o2.get(Order_.customer).get(
                        Customer_.balanceOwed)))));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery4() {
        String query = "select o.id from Order o where o.quantity >"
                + " (select count(i) from o.lineitems i)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));
        Subquery<Long> sq = q.subquery(Long.class);
        Root<Order> osq = sq.correlate(o);
        Join<Order, LineItem> i = osq.join(Order_.lineItems);
        q.where(cb.gt(o.get(Order_.quantity), sq.select(cb.count(i))));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery5() {
        String query = "select o.id from Order o where o.quantity >"
                + " (select count(o.quantity) from Order o)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));
        Subquery<Long> sq = q.subquery(Long.class);
        Root<Order> o2 = sq.from(Order.class);
        q.where(cb.gt(o.get(Order_.quantity), sq.select(cb.count(o2
                .get(Order_.quantity)))));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery6() {
        String query = "select o.id from Order o where o.quantity >"
                + " (select count(o.id) from Order o)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));
        Subquery<Long> sq = q.subquery(Long.class);
        Root<Order> o2 = sq.from(Order.class);
        q.where(cb.gt(o.get(Order_.quantity), sq.select(cb.count(o2
                .get(Order_.id)))));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery7() {
        String query = "select o.id from Order o where o.quantity >"
                + " (select avg(o.quantity) from Order o)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));
        Subquery<Double> sq = q.subquery(Double.class);
        Root<Order> o2 = sq.from(Order.class);
        q.where(cb.gt(o.get(Order_.quantity), sq.select(cb.avg(o2
                .get(Order_.quantity)))));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery8() {
        String query = "select c.name from Customer c where exists"
                + " (select o from c.orders o where o.id = 1) or exists"
                + " (select o from c.orders o where o.id = 2)";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.select(c.get(Customer_.name));
        Subquery<Order> sq1 = q.subquery(Order.class);
        Root<Customer> c1 = sq1.correlate(c);
        SetJoin<Customer, Order> o1 = c1.join(Customer_.orders);
        sq1.where(cb.equal(o1.get(Order_.id), 1)).select(o1);

        Subquery<Order> sq2 = q.subquery(Order.class);
        Root<Customer> c2 = sq2.correlate(c);
        SetJoin<Customer, Order> o2 = c2.join(Customer_.orders);
        sq2.where(cb.equal(o2.get(Order_.id), 2)).select(o2);

        q.where(cb.or(cb.exists(sq1), cb.exists(sq2)));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery9() {
        String query = "select c.name from Customer c, in(c.orders) o "
                + "where o.quantity between "
                + "(select max(o.quantity) from Order o) and "
                + "(select avg(o.quantity) from Order o) ";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.select(c.get(Customer_.name));

        Subquery<Integer> sq1 = q.subquery(Integer.class);
        Root<Order> o1 = sq1.from(Order.class);
        sq1.select(cb.max(o1.get(Order_.quantity)));

        Subquery<Double> sq2 = q.subquery(Double.class);
        Root<Order> o2 = sq2.from(Order.class);
        sq2.select(cb.avg(o2.get(Order_.quantity)));

        SetJoin<Customer, Order> o = c.join(Customer_.orders);
        // not sure how to do call between of integer(quantity)
        // between integer (max quantity) and double (avg quantity)
        // q.where(cb.between(o.get(Order_.quantity), sq1, sq2));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery10() {
        String query = "select o.id from Order o where o.quantity >"
                + " (select sum(o2.quantity) from Customer c, " 
                + "in(c.orders) o2) ";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));

        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<Customer> c = sq.from(Customer.class);
        SetJoin<Customer, Order> o2 = c.join(Customer_.orders);
        sq.select(cb.sum(o2.get(Order_.quantity)));

        q.where(cb.gt(o2.get(Order_.quantity), sq));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery11() {
        String query = "select o.id from Order o where o.quantity between"
                + " (select avg(o2.quantity) from Customer c, in(c.orders) o2)"
                + " and (select min(o2.quantity) from Customer c, in(c.orders)"
                + " o2)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));

        Subquery<Double> sq1 = q.subquery(Double.class);
        Root<Customer> c = sq1.from(Customer.class);
        SetJoin<Customer, Order> o2 = c.join(Customer_.orders);
        sq1.select(cb.avg(o2.get(Order_.quantity)));

        Subquery<Integer> sq2 = q.subquery(Integer.class);
        Root<Customer> c2 = sq2.from(Customer.class);
        SetJoin<Customer, Order> o3 = c2.join(Customer_.orders);
        sq2.select(cb.min(o3.get(Order_.quantity)));

        // do not know how to call between for double and integer
        // q.where(cb.between(o2.get(Order_.quantity), sq1, sq2));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery12() {
        String query =
            "select o.id from Customer c, in(c.orders)o "
                + "where o.quantity > (select sum(o2.quantity)"
                + " from c.orders o2)";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        SetJoin<Customer, Order> o = c.join(Customer_.orders);
        q.select(o.get(Order_.id));

        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<Customer> sqc = sq.correlate(c);
        SetJoin<Customer, Order> o2 = sqc.join(Customer_.orders);
        sq.select(cb.sum(o2.get(Order_.quantity)));
        q.where(cb.gt(o.get(Order_.quantity), sq));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery13() {
        String query = "select o1.id, c.name from Order o1, Customer c"
                + " where o1.quantity = "
                + " any(select o2.quantity from in(c.orders) o2)";
        CriteriaQuery q = cb.create();
        Root<Order> o1 = q.from(Order.class);
        Join<Order, Customer> c = o1.join(Order_.customer);
        q.select(o1.get(Order_.id), c.get(Customer_.name));

        Subquery<Integer> sq = q.subquery(Integer.class);
        Join<Order, Customer> sqc = sq.correlate(c);
        SetJoin<Customer, Order> o2 = sqc.join(Customer_.orders);
        sq.select(o2.get(Order_.quantity));

        q.where(cb.equal(o1.get(Order_.quantity), cb.any(sq)));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery14() {
        String query = "SELECT p, m FROM Publisher p "
            + "LEFT OUTER JOIN p.magazineCollection m "
            + "WHERE m.id = (SELECT MAX(m2.id) FROM Magazine m2 "
            + "WHERE m2.idPublisher.id = p.id AND m2.datePublished = "
            //+ "(SELECT MAX(m3.datePublished) FROM Magazine m3 "
            + "(SELECT MAX(m3.id) FROM Magazine m3 "
            + "WHERE m3.idPublisher.id = p.id)) ";
        CriteriaQuery q = cb.create();
        Root<Publisher> p = q.from(Publisher.class);
        Join<Publisher, Magazine> m = p.join(Publisher_.magazineCollection,
            JoinType.LEFT);
        q.select(p, m);

        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<Magazine> m2 = sq.from(Magazine.class);
        q.where(cb.equal(m.get(Magazine_.id), sq.select(cb.max(m2
            .get(Magazine_.id)))));

        Subquery<Integer> sq2 = q.subquery(Integer.class);
        Root<Magazine> m3 = sq2.from(Magazine.class);
        
        sq2.where(cb.equal(m3.get(Magazine_.idPublisher).get(Publisher_.id), 
            p.get(Publisher_.id)));
        
        sq.where(cb.and(cb.equal(m2.get(Magazine_.idPublisher).get(
            Publisher_.id), p.get(Publisher_.id)), cb.equal(m2
            .get(Magazine_.datePublished), sq2.select(cb.max(m3
            .get(Magazine_.id))))));
        assertEquivalence(q, query);
    }

    // outstanding problem subqueries:
    // "select o from Order o where o.amount > (select count(o) from Order o)",
    // "select o from Order o where o.amount > (select count(o2) from Order o2)
    //",
    // "select c from Customer c left join c.orders p where not exists"
    // + " (select o2 from c.orders o2 where o2 = o",

    // not sure how to write CriteriaQuery for
    // Subquery.select(SimpleCase/GeneralCase)
    @AllowFailure
    public void testSubquery15() {
        String query = "select o.id from Order o where o.delivered =(select "
                + "   CASE WHEN o2.quantity > 10 THEN true"
                + "     WHEN o2.quantity = 10 THEN false "
                + "     ELSE false END from Order o2"
                + " where o.customer.id = o2.customer.id)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));

        Subquery<Boolean> sq = q.subquery(Boolean.class);
        Root<Order> o2 = sq.from(Order.class);
        sq.where(cb.equal(o.get(Order_.customer).get(Customer_.id), o2.get(
                Order_.customer).get(Customer_.id)));

        q.where(cb.equal(o.get(Order_.delivered),
        // sq.select(
                cb.selectCase().when(cb.gt(o2.get(Order_.quantity), 10), true)
                        .when(cb.equal(o2.get(Order_.quantity), 10), false)
                        .otherwise(false)
        // )
                ));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery16() {
        String query = "select o1.oid from Order o1 where o1.quantity > "
                + " (select o.quantity*2 from LineItem i, Order o"
                + " where i.quantity > 10 and o.quantity > 1000 and i.id = " +
                		"o.id)";
        CriteriaQuery q = cb.create();
        Root<Order> o1 = q.from(Order.class);
        q.select(o1.get(Order_.id));

        Subquery<Integer> sq = q.subquery(Integer.class);
        Root<LineItem> i = sq.from(LineItem.class);
        Join<LineItem, Order> o = i.join(LineItem_.order);
        sq.where(cb.and(cb.and(cb.gt(i.get(LineItem_.quantity), 10), cb.gt(o
                .get(Order_.quantity), 1000)), cb.equal(i.get(LineItem_.id), o
                .get(Order_.id))));

        q.where(cb.gt(o1.get(Order_.quantity), sq.select(cb.prod(o
                .get(Order_.quantity), 2))));

        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery17() {
        String query = "select o.id from Order o where o.customer.name ="
                + " (select substring(o2.customer.name, 3) from Order o2"
                + " where o.customer.id = o2.customer.id)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.customer).get(Customer_.name));

        Subquery<String> sq = q.subquery(String.class);
        Root<Order> o2 = sq.from(Order.class);
        sq.where(cb.equal(o.get(Order_.customer).get(Customer_.id), o2.get(
                Order_.customer).get(Customer_.id)));

        q.where(cb.equal(o.get(Order_.customer).get(Customer_.name), sq
                .select(cb.substring(o2.get(Order_.customer)
                        .get(Customer_.name), 3))));

        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery18() {
        String query = "select o.id from Order o where o.orderTs >"
                + " (select CURRENT_TIMESTAMP from o.lineitems i)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));

        Subquery<Timestamp> sq = q.subquery(Timestamp.class);
        Root<Order> o2 = sq.correlate(o);
        ListJoin<Order, LineItem> i = o2.join(Order_.lineItems);

        // q.where(cb.gt(
        // o.get(Order_.orderTs),
        // sq.select(cb.currentTimestamp())));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery19() {
        String query = "select o.id from Order o where o.quantity >"
                + " (select SQRT(o.quantity) from Order o where o.delivered" +
                		" = true)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));

        Subquery<Double> sq = q.subquery(Double.class);
        Root<Order> o2 = sq.from(Order.class);
        sq.where(cb.equal(o2.get(Order_.delivered), true));

        q.where(cb.gt(o.get(Order_.quantity), sq.select(cb.sqrt(o2
                .get(Order_.quantity)))));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery20() {
        String query = "select o.id from Order o where o.customer.name in"
                + " (select CONCAT(o.customer.name, 'XX') from Order o"
                + " where o.quantity > 10)";
        CriteriaQuery q = cb.create();
        Root<Order> o = q.from(Order.class);
        q.select(o.get(Order_.id));

        Subquery<String> sq = q.subquery(String.class);
        Root<Order> o2 = sq.from(Order.class);
        sq.where(cb.gt(o2.get(Order_.quantity), 10));

        q.where(cb.in(o.get(Order_.customer).get(Customer_.name)).value(
                sq.select(cb.concat(
                        o2.get(Order_.customer).get(Customer_.name), "XX"))));
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery21() {
        String query = "select c from Customer c where c.creditRating ="
                + " (select "
                + "   CASE WHEN o2.quantity > 10 THEN "
                + "org.apache.openjpa.persistence.criteria.Customer$" +
                		"CreditRating.POOR"
                + "     WHEN o2.quantity = 10 THEN "
                + "org.apache.openjpa.persistence.criteria.Customer$" +
                		"CreditRating.GOOD "
                + "     ELSE "
                + "org.apache.openjpa.persistence.criteria.Customer$" +
                		"CreditRating.EXCELLENT "
                + "     END from Order o2"
                + " where c.id = o2.customer.id)";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.select(c);

        Subquery<String> sq = q.subquery(String.class);
        Root<Order> o2 = sq.from(Order.class);
        sq.where(cb.equal(c.get(Customer_.id), o2.get(Order_.customer).get(
            Customer_.id)));

        q.where(cb.equal(c.get(Customer_.creditRating), 
            //sq.select(
            cb.selectCase().when(cb.gt(o2.get(Order_.quantity), 10),
                Customer.CreditRating.POOR).when(
                cb.equal(o2.get(Order_.quantity), 10),
                Customer.CreditRating.GOOD).otherwise(
                Customer.CreditRating.EXCELLENT)
        // )
            ));
        assertEquivalence(q, query);
    }

    // Coalesce for Enum type
    @AllowFailure
    public void testSubquery22() {
        String query = "select c from Customer c "
                + "where c.creditRating = (select COALESCE (c1.creditRating, "
                + "org.apache.openjpa.persistence.criteria.Customer$" +
                		"CreditRating.POOR) "
                + "from Customer c1 where c1.name = 'Famzy') order by c.name " +
                		"DESC";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.select(c);
        q.orderBy(cb.desc(c.get(Customer_.name)));        

        Subquery<Customer.CreditRating> sq =
            q.subquery(Customer.CreditRating.class);
        Root<Customer> c1 = sq.from(Customer.class);
        sq.where(cb.equal(c1.get(Customer_.name), "Famzy"));
        
        //q.where(cb.equal(c.get(Customer_.creditRating),
        //    sq.select(cb.coalesce().value(c1.get(Customer_.creditRating).
        //        value(Customer.CreditRating.POOR)))));    
        assertEquivalence(q, query);
    }

    @AllowFailure
    public void testSubquery23() {
        String query =
            "select c from Customer c "
                + "where c.creditRating = (select NULLIF (c1.creditRating, "
                + "org.apache.openjpa.persistence.criteria."
                + "Customer$CreditRating.POOR) "
                + "from Customer c1 where c1.name = 'Famzy') "
                + "order by c.name DESC";
        CriteriaQuery q = cb.create();
        Root<Customer> c = q.from(Customer.class);
        q.select(c);
        q.orderBy(cb.desc(c.get(Customer_.name)));        

        Subquery<Customer.CreditRating> sq =
            q.subquery(Customer.CreditRating.class);
        Root<Customer> c1 = sq.from(Customer.class);
        sq.where(cb.equal(c1.get(Customer_.name), "Famzy"));
        
        q.where(cb.equal(c.get(Customer_.creditRating),
            sq.select(cb.nullif(c1.get(Customer_.creditRating),
                Customer.CreditRating.POOR))));    
        assertEquivalence(q, query);
    }

    /**
     * Verify a sub query can contain MAX and additional date comparisons
     * without losing the correct alias information. This sort of query
     * originally caused problems for DBDictionaries which used DATABASE syntax.
     */
    // Not sure how to do Cartesian join when Employee can not 
    // navigate to Dependent
    @AllowFailure
    public void testSubSelectMaxDateRange() {
        String query = "SELECT e,d from Employee e, Dependent d "
            + "WHERE e.empId = :empid "
            + "AND d.id.empid = (SELECT MAX (e2.empId) FROM Employee e2) "
            + "AND d.id.effDate > :minDate "
            + "AND d.id.effDate < :maxDate ";
    }

    void startTx(EntityManager em) {
        em.getTransaction().begin();
    }
    
    void endTx(EntityManager em) {
        em.getTransaction().commit();
    }
    
    public CompUser createUser(String name, String cName, Address add, int age,
        boolean isMale) {
        CompUser user = null;
        if (isMale) {
            user = new MaleUser();
            user.setName(name);
            user.setComputerName(cName);
            user.setAddress(add);
            user.setAge(age);
        } else {
            user = new FemaleUser();
            user.setName(name);
            user.setComputerName(cName);
            user.setAddress(add);
            user.setAge(age);
        }
        return user;
	}
}
