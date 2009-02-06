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

import java.util.List;
import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.common.apps.*;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;

public class TestJPQLScalarExpressions extends AbstractTestCase {

    private int userid1, userid2, userid3, userid4, userid5, userid6;

    public TestJPQLScalarExpressions(String name) {
        super(name, "jpqlclausescactusapp");
    }

    public void setUp() {
        deleteAll(CompUser.class);
        EntityManager em = currentEntityManager();
        startTx(em);

        Address[] add = new Address[]{
            new Address("43 Sansome", "SF", "United-Kingdom", "94104"),
            new Address("24 Mink", "ANTIOCH", "USA", "94513"),
            new Address("23 Ogbete", "CoalCamp", "NIGERIA", "00000"),
            new Address("10 Wilshire", "Worcester", "CANADA", "80080"),
            new Address("23 Bellflower", "Ogui", null, "02000"),
            new Address("22 Montgomery", "SF", null, "50054") };

        CompUser user1 = createUser("Seetha", "MAC", add[0], 36, true);
        CompUser user2 = createUser("Shannon ", "PC", add[1], 36, false);
        CompUser user3 = createUser("Ugo", "PC", add[2], 19, true);
        CompUser user4 = createUser("Jacob", "LINUX", add[3], 10, true);
        CompUser user5 = createUser("Famzy", "UNIX", add[4], 29, false);
        CompUser user6 = createUser("Shade", "UNIX", add[5], 23, false);

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
        userid6 = user6.getUserid();

        endTx(em);
        endEm(em);
    }

    @SuppressWarnings("unchecked")
    public void testCoalesceExpressions() {
        EntityManager em = currentEntityManager();
        startTx(em);

        String query = "SELECT e.name, " +
            "COALESCE (e.address.country, 'Unknown')" +
            " FROM CompUser e ORDER BY e.name DESC";
        List rs = em.createQuery(query).getResultList();
        Object[] result = (Object[]) rs.get(rs.size()-1);
        assertEquals("the name is not famzy", "Famzy", result[0]);        
        assertEquals("Unknown", result[1]);

        endTx(em);
        endEm(em);
    }

    @SuppressWarnings("unchecked")
    public void testNullIfExpressions() {
        EntityManager em = currentEntityManager();
        startTx(em);

        String query = "SELECT e.name, " +
            "NULLIF (e.address.country, 'USA')" +
            " FROM CompUser e ORDER BY e.name DESC";

        List rs = em.createQuery(query).getResultList();
        Object[] result = (Object[]) rs.get(1);
        assertEquals("the name is not shannon ", "Shannon ", result[0]);        
        assertNull("is not null", result[1]);
        
        endTx(em);
        endEm(em);
    }

    @SuppressWarnings("unchecked")
    public void testSimpleCaseExpressions() {
        EntityManager em = currentEntityManager();

        CompUser user = em.find(CompUser.class, userid1);
        assertNotNull("user is null", user);
        assertEquals("the name is not seetha", "Seetha", user.getName());
        String query = "SELECT e.name, e.age+1 as cage, " +
            "CASE e.address.country WHEN 'USA'" +
            " THEN 'us' " +
            " ELSE 'non-us'  END as d2," +
            " e.address.country " +
            " FROM CompUser e ORDER BY cage, d2 DESC";
        List rs = em.createQuery(query).getResultList();
        Object[] result = (Object[]) rs.get(rs.size()-1);
        assertEquals("the name is not seetha", "Seetha", result[0]);

        String query2 = "SELECT e.name, e.age+1 as cage, " +
            "CASE e.address.country WHEN 'USA'" +
            " THEN 'United-States' " +
            " ELSE e.address.country  END as d2," +
            " e.address.country " +
            " FROM CompUser e ORDER BY cage, d2 DESC";
        List rs2 = em.createQuery(query2).getResultList();
        Object[] result2 = (Object[]) rs2.get(rs2.size()-1);
        assertEquals("the name is not seetha", "Seetha", result2[0]);

        String query3 = "SELECT e.name, " +
            " CASE TYPE(e) WHEN FemaleUser THEN 'Female' " +
            " ELSE 'Male' " +
            " END as result" +
            " FROM CompUser e";
        List rs3 = em.createQuery(query3).getResultList();
        Object[] result3 = (Object[]) rs3.get(rs3.size()-1);
        assertEquals("the result is not female", "Female", result3[1]);
        assertEquals("the name is not shade", "Shade", result3[0]);
        result3 = (Object[]) rs3.get(0);
        assertEquals("the result is not male", "Male", result3[1]);
        assertEquals("the name is not seetha", "Seetha", result3[0]);

        endEm(em);
    }

    @SuppressWarnings("unchecked")
    public void testGeneralCaseExpressions() {
        EntityManager em = currentEntityManager();
        startTx(em);

        CompUser user = em.find(CompUser.class, userid1);
        assertNotNull("user is null", user);
        assertEquals("the name is not seetha", "Seetha", user.getName());

        String query = "SELECT e.name, e.age, " +
            " CASE WHEN e.age > 30 THEN e.age - 1 " +
            " WHEN e.age < 15 THEN e.age + 1 " +
            " ELSE e.age + 0 " +
            " END AS cage " +
            " FROM CompUser e ORDER BY cage";
        
        List rs = em.createQuery(query).getResultList();

        String update = "UPDATE CompUser e SET e.age = " +
            "CASE WHEN e.age > 30 THEN e.age - 1 " +
            "WHEN e.age < 15 THEN e.age + 1 " +
            "ELSE e.age + 0 " +
            "END";

        int result = em.createQuery(update).executeUpdate();

        assertEquals("the result is not 6", 6, result);
        endTx(em);
        endEm(em);
    }

    @SuppressWarnings("unchecked")
    public void testMathFuncOrderByAlias() {
        EntityManager em = currentEntityManager();

        String query = "SELECT e.age * 2 as cAge FROM CompUser e ORDER BY cAge";

        List result = em.createQuery(query).getResultList();

        assertNotNull(result);
        assertEquals(6, result.size());

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
