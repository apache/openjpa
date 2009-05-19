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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Tests type-strict version of Criteria API.
 * The test scenarios are adapted from TestEJBQLCondExpression in 
 * org.apache.openjpa.persistence.jpql.expressions and
 * TestEJBQLFunction in org.apache.openjpa.persistence.jpql.functions. 
 * 
 */
public class TestTypeSafeCondExpression extends SQLListenerTestCase {

    private int userid1, userid2, userid3, userid4, userid5;
    CriteriaBuilder cb;
    EntityManager em;

    public void setUp() {
        super.setUp(DROP_TABLES,
               CompUser.class,
               Address.class,
               MaleUser.class,
               FemaleUser.class);
    
        setDictionary();
        cb = (CriteriaBuilder)emf.getQueryBuilder();
        em = emf.createEntityManager();
        startTx(em);

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

    void setDictionary() {
        JDBCConfiguration conf = (JDBCConfiguration)emf.getConfiguration();
        DBDictionary dict = conf.getDBDictionaryInstance();
        dict.requiresCastForComparisons = false;
        dict.requiresCastForMathFunctions = false;
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
        q.where(cb.in(c.get(CompUser_.age)).value(29).value(40).value(10).negate());
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
            "SELECT o.computerName FROM CompUser o WHERE o.name LIKE 'Sha%' AND " + 
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
            "SELECT o.computerName FROM CompUser o WHERE o.name LIKE 'Sha%o_' AND " + 
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
    
    public void testLikeExprUsingCriteria4() {
        String query = "SELECT o.name FROM CompUser o WHERE o.name LIKE ?1 ESCAPE '|'";
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

    public void testNullExprUsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.age IS NOT NULL AND o.computerName = 'PC' ";
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
    
    public void testNullExpr2UsingCriteria() {
        String query =
            "SELECT o.name FROM CompUser o WHERE o.address.country IS NULL";

        CriteriaQuery q = cb.create();
        Root<CompUser> c = q.from(CompUser.class);
        Parameter<String> param = cb.parameter(String.class);
        q.where(cb.equal(c.get(CompUser_.address).get(Address_.country), null));
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
                " (SELECT s FROM CompUser s WHERE s.address.country = o.address.country)";

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
                " SELECT s.computerName FROM CompUser s WHERE s.address.country IS NOT NULL )";

        CriteriaQuery q = cb.create();
        Root<CompUser> o = q.from(CompUser.class);
        q.select(o.get(CompUser_.name));
        Subquery<String> sq = q.subquery(String.class);
        Root<CompUser> s = sq.from(CompUser.class);
        sq.select(s.get(CompUser_.computerName));
        sq.where(cb.notEqual(s.get(CompUser_.address).get(Address_.country), null));
        q.where(cb.equal(o.get(CompUser_.address).get(Address_.zipCode), cb.any(sq)));
        assertEquivalence(q, query);
        List result = em.createQuery(q).getResultList();

        assertNotNull("list is null", result);
        assertEquals("they are not equal", 0, result.size());

        em.clear();
    }
    
    public void testConstructorExprUsingCriteria() {
        String query =
            "SELECT NEW org.apache.openjpa.persistence.common.apps.MaleUser(c.name, " + 
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
        assertEquivalence(q, query);
    
        em.clear();
    }
/*    
    public void testConcatSubStringFunc2() {
        String query = "select e.name From CompUser e where e.firstName = " +
            "CONCAT('Ablahum', SUBSTRING(e.name, LOCATE('e', e.name), 4)) ";
        em.clear();
    }

    public void testConcatSubStringFunc3() {
        String query = "select " +
            "CONCAT('XYZ', SUBSTRING(e.name, LOCATE('e', e.name))) " +
            "From CompUser e WHERE e.name='Ablahumeeth'";
        em.clear();
    }

    public void testConcatSubStringFunc4() {
        String query = "select e.name from CompUser e where e.firstName = " +
            "CONCAT('XYZ', SUBSTRING(e.name, LOCATE('e', e.name))) ";
        em.clear();
    }

    public void testConcatFunc() {
        String query = "select " +
            "CONCAT('', '') From CompUser WHERE e.name='Seetha'";
        em.clear();
    }

    public void testTrimFunc1() {
        String query = "select Trim(e.name) From CompUser e WHERE e.name='Shannon '";
        em.clear();
    }

    public void testTrimFunc2() {
        String query = "select e.name From CompUser e where Trim(e.name) ='Shannon'";
        em.clear();
    }

    public void testLowerFunc1() {
        String query = "select LOWER(e.name) From CompUser e WHERE e.name='Ugo'";
        em.clear();
    }

    public void testLowerFunc2() {
        String query = "select e.name From CompUser e where LOWER(e.name) ='ugo'";
        em.clear();
    }

    public void testUpperFunc1() {
        String query = "select UPPER(e.name) From CompUser e WHERE e.name='Ugo'";
        em.clear();
    }

    public void testUpperFunc2() {
        String query = "select e.name from CompUser e where UPPER(e.name)='UGO'";
        em.clear();
    }

    public void testLengthFunc() {
        String query = "SELECT o.name FROM CompUser o " +
            "WHERE LENGTH(o.address.country) = 3";
        em.clear();
    }

    public void testArithmFunc1() {
        String query =
            "select ABS(e.age) From CompUser e WHERE e.name='Seetha'";
    }
    
    public void testArithmFunc2() {
        String query =
            "select SQRT(e.age) From CompUser e WHERE e.name='Seetha'";
    }
    
    public void testArithmFunc3() {
        String query =
            "select MOD(e.age, 4) From CompUser e WHERE e.name='Seetha'";
    }
    
    public void testArithmFunc4() {
        String query = "SELECT e.name FROM CompUser e WHERE SIZE(e.nicknames) = 6";
        em.clear();
    }

    public void testGroupByHavingClause() {
        String query =
            "SELECT c.name FROM CompUser c GROUP BY c.name HAVING c.name LIKE 'S%'";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("Shannon "));
        assertTrue(result.contains("Shade"));
        assertTrue(result.contains("Seetha"));

        em.clear();
    }

    public void testOrderByClause() {
        String query =
            "SELECT c.name FROM CompUser c WHERE c.name LIKE 'S%' ORDER BY c.name";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("Shannon "));
        assertTrue(result.contains("Seetha"));
        assertTrue(result.contains("Shade"));

        em.clear();
    }

    public void testAVGAggregFunc() {
        //To be Tested: AVG, COUNT, MAX, MIN, SUM
        String query = "SELECT AVG(e.age) FROM CompUser e";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(25));

        em.clear();
    }

    public void testCOUNTAggregFunc() {
        String query = "SELECT COUNT(c.name) FROM CompUser c";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(6l));

        em.clear();
    }

    public void testMAXAggregFunc() {
        String query = "SELECT DISTINCT MAX(c.age) FROM CompUser c";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(36));

        em.clear();
    }

    public void testMINAggregFunc() {
        String query = "SELECT DISTINCT MIN(c.age) FROM CompUser c";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(10));

        em.clear();
    }

    public void testSUMAggregFunc() {
        String query = "SELECT SUM(c.age) FROM CompUser c";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(153l));

        em.clear();
    }
*/

    void startTx(EntityManager em) {
        em.getTransaction().begin();
    }
    
    void endTx(EntityManager em) {
        em.getTransaction().commit();
    }
    
    void assertEquivalence(CriteriaQuery c, String jpql) {
        
        sql.clear(); 
        List cList = em.createQuery(c).getResultList();
        assertEquals(1, sql.size()); 
        String cSQL = sql.get(0);
        
        sql.clear();
        List jList = em.createQuery(jpql).getResultList();
        assertEquals(1, sql.size());
        String jSQL = sql.get(0);

        assertEquals(jSQL, cSQL);
    }

    void assertEquivalence(CriteriaQuery c, String jpql, 
            Object[] params) {
         sql.clear();
        Query q = em.createQuery(c);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i+1, params[i]);
        }
        List cList = q.getResultList();
        assertEquals(1, sql.size());
        String cSQL = sql.get(0);

        sql.clear();
        Query q1 = em.createQuery(jpql);
        for (int i = 0; i < params.length; i++) {
            q1.setParameter(i+1, params[i]);
        }
        List jList = q1.getResultList();
        assertEquals(1, sql.size());
        String jSQL = sql.get(0);

        assertEquals(jSQL, cSQL);
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
