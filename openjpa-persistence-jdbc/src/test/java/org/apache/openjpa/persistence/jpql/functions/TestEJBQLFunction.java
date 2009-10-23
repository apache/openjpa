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
package org.apache.openjpa.persistence.jpql.functions;

import javax.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.common.apps.CompUser;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestEJBQLFunction extends SingleEMFTestCase {

    private int userid3, userid5;

    public void setUp() {
        setUp(CompUser.class, CLEAR_TABLES);

        CompUser user1 = createUser("Seetha", "MAC", 36, true);
        CompUser user2 = createUser("Shannon ", "PC", 36, false);
        CompUser user3 = createUser("Ugo", "PC", 19, true);
        CompUser user4 = createUser("_Jacob", "LINUX", 10, true);
        CompUser user5 = createUser("Famzy", "UNIX", 29, false);
        CompUser user6 = createUser("Shade", "UNIX", 23, false);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(user1);
        em.persist(user2);
        em.persist(user3);
        userid3 = user3.getUserid();
        em.persist(user4);
        em.persist(user5);
        userid5 = user5.getUserid();
        em.persist(user6);
        em.getTransaction().commit();
        em.clear();
        em.close();
    }

    
    public void testLowerFunc() {
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        DBDictionary dict = ((JDBCConfiguration) em.getConfiguration()).getDBDictionaryInstance();
        if (dict instanceof MySQLDictionary ) {
            // This testcase requires OPENJPA-612 to execute on MySQL. 
            return;
        }
        em.getTransaction().begin();

        CompUser user = em.find(CompUser.class, userid3);
        assertNotNull(user);
        assertEquals("Ugo", user.getName());

        String query = "UPDATE CompUser e SET " +
            "e.name = LOWER(e.name) WHERE e.name='Ugo'";

        int result = em.createQuery(query).executeUpdate();

        user = em.find(CompUser.class, userid3);
        em.refresh(user);
        assertNotNull(user);
        assertEquals("ugo", user.getName());

        em.getTransaction().commit();
        em.clear();
        em.close();
    }

    public void testLowerClobFunc() {
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        // some databases do not support case conversion on LOBs,
        // just skip this test case
        DBDictionary dict = ((JDBCConfiguration) em.getConfiguration())
            .getDBDictionaryInstance();
        if (!dict.supportsCaseConversionForLob) {
            return;
        }
        em.getTransaction().begin();

        CompUser user = em.find(CompUser.class, userid5);
        assertNotNull(user);
        assertEquals("Famzy", user.getName());

        String query = "UPDATE CompUser e SET " +
                "e.name = LOWER(e.name) WHERE LOWER(e.nameAsLob)='famzy'";

        int result = em.createQuery(query).executeUpdate();

        user = em.find(CompUser.class, userid5);
        em.refresh(user);
        assertNotNull(user);
        assertEquals("famzy", user.getName());

        em.getTransaction().commit();
        em.clear();
        em.close();
    }

    public void testUpperFunc() {
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        DBDictionary dict = ((JDBCConfiguration) em.getConfiguration()).getDBDictionaryInstance();
        if (dict instanceof MySQLDictionary ) {
            // This testcase requires OPENJPA-612 to execute on MySQL. 
            return;
        }
        em.getTransaction().begin();

        CompUser user = em.find(CompUser.class, userid3);
        assertNotNull(user);
        assertEquals("Ugo", user.getName());

        String query = "UPDATE CompUser e SET " +
            "e.name = UPPER(e.name) WHERE e.name='Ugo'";

        int result = em.createQuery(query).executeUpdate();

        user = em.find(CompUser.class, userid3);
        em.refresh(user);
        assertNotNull(user);
        assertEquals("UGO", user.getName());

        em.getTransaction().commit();
        em.clear();
        em.close();
    }

    public void testUpperClobFunc() {
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        // some databases do not support case conversion on LOBs,
        // just skip this test case
        DBDictionary dict = ((JDBCConfiguration) em.getConfiguration())
            .getDBDictionaryInstance();
        if (!dict.supportsCaseConversionForLob) {
            return;
        }
        em.getTransaction().begin();

        CompUser user = em.find(CompUser.class, userid5);
        assertNotNull(user);
        assertEquals("Famzy", user.getName());

        String query = "UPDATE CompUser e SET " +
                "e.name = UPPER(e.name) WHERE UPPER(e.nameAsLob)='FAMZY'";

        int result = em.createQuery(query).executeUpdate();

        user = em.find(CompUser.class, userid5);
        em.refresh(user);
        assertNotNull(user);
        assertEquals("FAMZY", user.getName());

        em.getTransaction().commit();
        em.clear();
        em.close();
    }

    public CompUser createUser(String name, String cName, int age,
        boolean isMale) {
        CompUser user = new CompUser();
        user.setName(name);
        user.setComputerName(cName);
        user.setAge(age);
        user.setNameAsLob(name);
        return user;
    }
}
