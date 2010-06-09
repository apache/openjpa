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
package org.apache.openjpa.persistence.exception;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;

import org.apache.openjpa.persistence.test.PersistenceTestCase;

public class TestParameterLogging extends PersistenceTestCase {

    String _regex = ".*params=.*1,.*]";

    /*
     * Persist the same row twice in the same transaction - will throw an exception with the failing SQL statement
     */
    private RollbackException getRollbackException(Object... props) {
        EntityManagerFactory emf = createEMF(props);
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();

        PObject p1, p2;
        p1 = new PObject();
        p2 = new PObject();

        p1.setId(1);
        p2.setId(1);

        try {
            tran.begin();
            em.persist(p1);
            em.persist(p2);
            tran.commit();
            em.close();
            fail("Expected a RollbackException");
            return null;
        } catch (RollbackException re) {
            return re;
        } finally {
            if (tran.isActive()) {
                tran.rollback();
            }
            if (em.isOpen()) {
                em.close();
            }
            if (emf.isOpen()) {
                emf.close();
            }
        }
    }

    /*
     * Ensure that parameter values are not included in exception text by default.
     */
    public void testNoParamsByDefault() {
        RollbackException e = getRollbackException(PObject.class, CLEAR_TABLES);

        assertFalse(Pattern.matches(_regex, e.toString()));
        Throwable nested = e.getCause();
        while (nested != null) {
            if (Pattern.matches(".*INSERT.*", nested.toString())) {
                // only check if the message contains the insert statement.
                assertFalse(Pattern.matches(_regex, nested.toString()));
            }
            nested = nested.getCause();
        }
    }

    /*
     * If the EMF is created with PrintParameters=true the parameter values will be logged in exception text.
     */
    public void testParamsEnabledByConfig() {
        RollbackException e =
            getRollbackException(PObject.class, CLEAR_TABLES, "openjpa.ConnectionFactoryProperties",
                "PrintParameters=true");
        assertFalse(Pattern.matches(_regex, e.toString()));
        Throwable nested = e.getCause();
        assertNotNull(nested); // expecting at least one nested exception.
        while (nested != null) {
            if (Pattern.matches(".*INSERT.*", nested.toString())) {
                // only check if the message contains the insert statement.
                assertTrue(Pattern.matches(_regex, nested.toString()));
            }
            nested = nested.getCause();
        }
    }

    /*
     * If the EMF is created with PrintParameters=false and trace is enabled for SQL the parameter values will not be
     * logged in exception text.
     */
    public void testParamsDisbledWithLogging() throws Exception {
        RollbackException e =
            getRollbackException(PObject.class, CLEAR_TABLES, "openjpa.ConnectionFactoryProperties",
                "PrintParameters=false", "openjpa.Log", "SQL=TRACE,File=temp.txt");
        assertFalse(Pattern.matches(_regex, e.toString()));
        Throwable nested = e.getCause();
        assertNotNull(nested); // should be at least one nested exception
        while (nested != null) {
            if (Pattern.matches(".*INSERT.*", nested.toString())) {
                // only check if the message contains the insert statement.
                assertFalse(Pattern.matches(_regex, nested.toString()));
            }
            nested = nested.getCause();
        }
        checkAndDeleteLogFile("temp.txt", false);
    }

    /*
     * If the EMF is created with PrintParameters=false and trace is enabled for SQL the parameter values will not be
     * logged in exception text.
     */
    public void testParamsEnabledWithLogging() throws Exception {
        RollbackException e =
            getRollbackException(PObject.class, CLEAR_TABLES, "openjpa.ConnectionFactoryProperties",
                "PrintParameters=true", "openjpa.Log", "SQL=TRACE,File=temp.txt");
        assertFalse(Pattern.matches(_regex, e.toString()));
        Throwable nested = e.getCause();
        assertNotNull(nested); // should be at least one nested exception
        while (nested != null) {
            if (Pattern.matches(".*INSERT.*", nested.toString())) {
                // only check if the message contains the insert statement.
                assertTrue(Pattern.matches(_regex, nested.toString()));
            }
            nested = nested.getCause();
        }
        checkAndDeleteLogFile("temp.txt", true);
    }

    private void checkAndDeleteLogFile(String filename, boolean containsParams) throws Exception {
        File f = null;
        FileReader fr = null;
        BufferedReader br = null;
        try {
            f = new File(filename);
            fr = new FileReader(f);
            br = new BufferedReader(fr);

            String s = br.readLine();
            while (s != null) {
                if (Pattern.matches(".*INSERT.*", s)) {
                    if (containsParams) {
                        assertTrue(Pattern.matches(_regex, s));
                    } else {
                        assertFalse(Pattern.matches(_regex, s));
                    }
                }
                s = br.readLine();
            }
        } finally {
            if (br != null) {
                br.close();
            }
            if (fr != null) {
                fr.close();
            }
            if (f != null) {
                f.delete();
            }
        }
    }
}
