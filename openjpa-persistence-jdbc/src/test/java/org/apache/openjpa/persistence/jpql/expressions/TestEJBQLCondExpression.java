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
package org.apache.openjpa.persistence.jpql.expressions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.common.apps.Address;
import org.apache.openjpa.persistence.common.apps.CompUser;
import org.apache.openjpa.persistence.common.apps.FemaleUser;
import org.apache.openjpa.persistence.common.apps.MaleUser;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestEJBQLCondExpression extends AbstractTestCase {

    private int userid1, userid2, userid3, userid4, userid5;

    public TestEJBQLCondExpression() {
        super("jpqlclausescactusapp");
    }

    @BeforeEach
    void setUp() {
        deleteAll(CompUser.class);
        EntityManager em = currentEntityManager();
        startTx(em);

        Address[] add =
            new Address[]{ new Address("43 Sansome", "SF", "USA", "94104"),
                new Address("24 Mink", "ANTIOCH", "USA", "94513"),
                new Address("23 Ogbete", "CoalCamp", "NIGERIA", "00000"),
                new Address("10 Wilshire", "Worcester", "CANADA", "80080"),
                new Address("23 Bellflower", "Ogui", "NIGERIA", "02000"),
                new Address("24 Bellflower", "Ogui", "NIGERIA", "02000")};


        CompUser user1 = createUser("Seetha", "MAC", add[0], 40, true);
        CompUser user2 = createUser("Shannon", "PC", add[1], 36, false);
        CompUser user3 = createUser("Ugo", "PC", add[2], 19, true);
        CompUser user4 = createUser("Jacob", "LINUX", add[3], 10, true);
        CompUser user5 = createUser("Famzy", "UNIX", add[4], 29, false);
        CompUser user6 = createUser("tes\\ter", "Test", add[5], 10, true);

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
        em.persist(user6);

        endTx(em);
        endEm(em);
    }

    @Test
    void nothing() {
        EntityManager em = currentEntityManager();
        String query = "SELECT o FROM CompUser o";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "the list is null");
        assertEquals(6, result.size(), "the size of the list is not 6");

        endEm(em);
    }

    @Test
    void betweenExpr() {
        EntityManager em = currentEntityManager();
        String query = "SELECT o.name FROM CompUser o WHERE" +
            " o.age BETWEEN 19 AND 40 AND o.computerName = 'PC'";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "the list is null");
        assertEquals(2, result.size(), "they are not equal");
        assertTrue(result.contains("Shannon"), "result dont contain shannon");
        assertTrue(result.contains("Ugo"), "result dont contain ugo");

        endEm(em);
    }

    @Test
    void notBetweenExpr() {
        EntityManager em = currentEntityManager();
        String query = "SELECT o.name FROM CompUser o " +
            "WHERE o.age NOT BETWEEN 19 AND 40 AND o.computerName= 'PC'";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "the list is null");
        assertEquals(0, result.size(), "they are not equal");

        endEm(em);
    }

    @Test
    void inExpr() {
        EntityManager em = currentEntityManager();
        String query =
            "SELECT o.name FROM CompUser o WHERE o.age IN (29, 40, 10)";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "the list is null");
        assertEquals(4, result.size());
        assertTrue(result.contains("Seetha"), "seetha is not in the list");
        assertTrue(result.contains("Jacob"), "jacob is not in the list");
        assertTrue(result.contains("Famzy"), "famzy is not in the list");

        endEm(em);
    }

    @Test
    void notInExpr() {
        EntityManager em = currentEntityManager();
        String query =
            "SELECT o.name FROM CompUser o WHERE o.age NOT IN (29, 40, 10)";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("Ugo"));
        assertTrue(result.contains("Shannon"));

        endEm(em);
    }

    @Test
    void likeExpr() {
        EntityManager em = currentEntityManager();

        String query = "SELECT o.computerName FROM CompUser o WHERE o.name " +
            "LIKE 'Sha%' AND o.computerName NOT IN ('PC')";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(0, result.size());

        query = "SELECT o.computerName FROM CompUser o WHERE o.name " +
            "LIKE 'Sha%o_' AND o.computerName NOT IN ('UNIX')";

        result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());

        query = "SELECT o.computerName FROM CompUser o WHERE o.name " +
                "LIKE 'tes\\%'";

        result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(1, result.size());

        query = "SELECT o.name FROM CompUser o WHERE o.name LIKE '_J%'";

        result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(0, result.size());

        query = "SELECT o.name FROM CompUser o WHERE o.name LIKE ?1 ESCAPE '|'";

        result = em.createQuery(query).setParameter(1, "%|_%").getResultList();

        assertNotNull(result);
        assertEquals(0, result.size());

        endEm(em);
    }

    /**
     * A LIKE pattern without an ESCAPE clause has no escape character at
     * all, so a backslash in it is an ordinary character on every database
     * (OPENJPA-3009). The same has to hold for a bound parameter, and it has
     * to keep holding when the query is executed again from the prepared
     * query cache.
     */
    @Test
    void likeWithoutEscapeTreatsBackslashLiterally() {
        EntityManager em = currentEntityManager();

        String literal = "SELECT o.name FROM CompUser o WHERE o.name LIKE 'tes\\%'";
        String param = "SELECT o.name FROM CompUser o WHERE o.name LIKE ?1";

        for (int i = 0; i < 2; i++) {
            List result = em.createQuery(literal).getResultList();
            assertEquals(1, result.size(), "literal pattern, run " + i);
            assertEquals("tes\\ter", result.get(0));

            result = em.createQuery(param).setParameter(1, "tes\\%").getResultList();
            assertEquals(1, result.size(), "bound pattern, run " + i);
            assertEquals("tes\\ter", result.get(0));

            // a lone backslash is not a wildcard either
            result = em.createQuery(param).setParameter(1, "tes\\").getResultList();
            assertEquals(0, result.size(), "non-wildcard pattern, run " + i);

            // '_' is still a wildcard and matches the backslash
            result = em.createQuery(param).setParameter(1, "tes_ter").getResultList();
            assertEquals(1, result.size(), "single wildcard, run " + i);

            // an empty ESCAPE clause names no escape character either
            result = em.createQuery(param + " ESCAPE ''").
                setParameter(1, "tes\\%").getResultList();
            assertEquals(1, result.size(), "empty escape clause, run " + i);
            assertEquals("tes\\ter", result.get(0));
        }

        endEm(em);
    }

    @Test
    void nullExpr() {
        EntityManager em = currentEntityManager();

        String query = "SELECT o.name FROM CompUser o WHERE o.age IS NOT NULL" +
            " AND o.computerName = 'PC' ";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "the list is null");
        assertEquals(2, result.size(), "the list size is not 2");
        assertTrue(result.contains("Ugo"), "the result doesnt contain ugo");
        assertTrue(result.contains("Shannon"),
            "the result doesnt contain shannon");

        endEm(em);
    }

    @Test
    void nullExpr2() {
        EntityManager em = currentEntityManager();

        String query =
            "SELECT o.name FROM CompUser o WHERE o.address.country IS NULL";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "the list is null");
        assertEquals(0, result.size(), "they are not equal");

        endEm(em);
    }

    @Test
    void isEmptyExpr() {
        EntityManager em = currentEntityManager();

        String query =
            "SELECT o.name FROM CompUser o WHERE o.nicknames IS NOT EMPTY";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "the list is null");
        assertEquals(0, result.size(), "they are not equal");

        endEm(em);
    }

    @Test
    void isEmptyExpr2() {
        EntityManager em = currentEntityManager();

        String query =
            "SELECT o.name FROM CompUser o WHERE o.nicknames IS EMPTY";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "the list is null");
        assertEquals(6, result.size(), "they are not equal");

        endEm(em);
    }

    /**
     * TO BE TESTED LATER WITH A DIFF DATABASE
     * public void testMemberOfExpr(){}
     */

    @Test
    void existExpr() {
        EntityManager em = currentEntityManager();

        String query = "SELECT DISTINCT o.name FROM CompUser o WHERE EXISTS" +
            " (SELECT c FROM Address c WHERE c = o.address )";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "the list is null");
        assertEquals(6, result.size(), "they are not equal");
        assertTrue(result.contains("Seetha"), "Seetha is not list");
        assertTrue(result.contains("Shannon"), "Shannon is not list");
        assertTrue(result.contains("Jacob"), "jacob is not list");
        assertTrue(result.contains("Ugo"), "ugo is not list");

        endEm(em);
    }

    @Test
    void notExistExpr() {
        EntityManager em = currentEntityManager();

        String query =
            "SELECT DISTINCT o.name FROM CompUser o WHERE NOT EXISTS" +
                " (SELECT s FROM CompUser s " +
                "WHERE s.address.country = o.address.country)";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "list is null");
        assertEquals(0, result.size(), "they are not equal");

        endEm(em);
    }

    @Test
    void anyExpr() {
        EntityManager em = currentEntityManager();

        String query =
            "SELECT o.name FROM CompUser o WHERE o.address.zipcode = ANY (" +
                " SELECT s.computerName FROM CompUser s " +
                "WHERE s.address.country IS NOT NULL )";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result, "list is null");
        assertEquals(0, result.size(), "they are not equal");

        endEm(em);
    }

    @Test
    void constructorExpr() {
        EntityManager em = currentEntityManager();

        String query = "SELECT NEW org.apache.openjpa.persistence.common.apps" +
               ".MaleUser(c.name, c.computerName, c.address, c.age, c.userid)" +
               " FROM CompUser c WHERE c.name = 'Seetha'";

        MaleUser male = (MaleUser) em.createQuery(query).getSingleResult();

        assertNotNull(male, "the list is null");
        assertEquals("Seetha", male.getName(), "the names dont match");
        assertEquals("MAC", male.getComputerName(),
            "computer names dont match");
        assertEquals(40, male.getAge(), "the ages dont match");

        endEm(em);
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
            user.setNameAsLob(name);
        } else {
            user = new FemaleUser();
            user.setName(name);
            user.setComputerName(cName);
            user.setAddress(add);
            user.setAge(age);
            user.setNameAsLob(name);
        }
        return user;
	}
}
