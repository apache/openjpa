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
package org.apache.openjpa.persistence.jdbc;

import javax.persistence.Query;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.InvalidStateException;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DB2Dictionary;
import org.apache.openjpa.jdbc.sql.HSQLDictionary;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;

public class TestIsolationLevelOverride
    extends SQLListenerTestCase {

    public void setUp() {
        setUp(AllFieldTypes.class,
            "openjpa.Optimistic", "false",
            "openjpa.LockManager", "pessimistic");
    }

    public void testIsolationOverrideViaFetchPlan() {
        testIsolationLevelOverride(false, false);
    }

    public void testIsolationOverrideViaHint() {
        testIsolationLevelOverride(true, false);
    }

    public void testIsolationOverrideViaStringHint() {
        testIsolationLevelOverride(true, true);
    }

    public void testIsolationLevelOverride(boolean useHintsAndQueries,
        boolean useStringHints) {
        OpenJPAEntityManager em =
            OpenJPAPersistence.cast(emf.createEntityManager());
        DBDictionary dict = ((JDBCConfiguration) em.getConfiguration())
            .getDBDictionaryInstance();

        // hsql doesn't support locking; circumvent the test
        if (dict instanceof HSQLDictionary)
            return;

        sql.clear();
        try {
            em.getTransaction().begin();
            if (useHintsAndQueries) {
                Query q = em.createQuery(
                "select o from AllFieldTypes o where o.intField = :p");
                q.setParameter("p", 0);
                if (useStringHints) {
                    q.setHint("openjpa.FetchPlan.Isolation", "SERIALIZABLE");
                } else {
                    q.setHint("openjpa.FetchPlan.Isolation",
                        IsolationLevel.SERIALIZABLE);
                }

                assertEquals(IsolationLevel.SERIALIZABLE,
                    ((JDBCFetchPlan) ((OpenJPAQuery) q).getFetchPlan())
                        .getIsolation());

                q.getResultList();
                if (dict instanceof DB2Dictionary) {
                    if ((((DB2Dictionary) dict).getDb2ServerType() == 1)
                        || (((DB2Dictionary) dict).getDb2ServerType()== 2)) {
                        assertEquals(1, sql.size());
                        assertSQL("SELECT t0.id, t0.booleanField, t0.byteField,"
                            + " t0.charField, t0.dateField, t0.doubleField,"
                            + " t0.floatField, t0.intField, t0.longField, "
                            + "t0.shortField, t0.stringField FROM "
                            + "AllFieldTypes t0 WHERE \\(t0.intField = \\?\\) "
                            + " FOR UPDATE OF");
                    }
                    // it is DB2 v82 or later
                    else if ((((DB2Dictionary) dict).getDb2ServerType() == 3)
                        || (((DB2Dictionary) dict).getDb2ServerType() == 4)) {
                        assertEquals(1, sql.size());
                        assertSQL("SELECT t0.id, t0.booleanField, t0.byteField,"
                            + " t0.charField, t0.dateField, t0.doubleField,"
                            + " t0.floatField, t0.intField, t0.longField, "
                            + "t0.shortField, t0.stringField FROM "
                            + "AllFieldTypes t0 WHERE \\(t0.intField = \\?\\) "
                            + " FOR READ ONLY WITH RR USE AND KEEP " 
                            + "UPDATE LOCKS");
                    }
                    else if (((DB2Dictionary) dict).getDb2ServerType() == 5) {
                        assertEquals(1, sql.size());
                        assertSQL("SELECT t0.id, t0.booleanField, t0.byteField,"
                            + " t0.charField, t0.dateField, t0.doubleField,"
                            + " t0.floatField, t0.intField, t0.longField, "
                            + "t0.shortField, t0.stringField FROM "
                            + "AllFieldTypes t0 WHERE \\(t0.intField = \\?\\) "
                            + " FOR READ ONLY WITH RR USE AND KEEP EXCLUSIVE " 
                            + "LOCKS");
                    }    
                    else {
                        fail("OpenJPA currently only supports " 
                            +"per-query isolation level configuration on the" 
                            +" following databases: DB2");
                    }
                }    
            } else {
                ((JDBCFetchPlan) em.getFetchPlan())
                    .setIsolation(IsolationLevel.SERIALIZABLE);
                em.find(AllFieldTypes.class, 0);
                if (dict instanceof DB2Dictionary ) {
                    if ((((DB2Dictionary) dict).getDb2ServerType() == 1)
                        || (((DB2Dictionary) dict).getDb2ServerType()== 2)) {
                        assertEquals(1, sql.size());
                        assertSQL("SELECT t0.booleanField, t0.byteField, "
                            + "t0.charField, t0.dateField, t0.doubleField,"
                            + " t0.floatField, t0.intField, t0.longField,"
                            + " t0.shortField, t0.stringField FROM "
                            + "AllFieldTypes t0 WHERE t0.id = \\? "
                            + " FOR UPDATE OF optimize for 1 row");
                    }
                    // it is DB2 v82 or later
                    else if ((((DB2Dictionary) dict).getDb2ServerType() == 3)
                        || (((DB2Dictionary) dict).getDb2ServerType() == 4)) {
                        assertEquals(1, sql.size());
                        assertSQL("SELECT t0.booleanField, t0.byteField, "
                            + "t0.charField, t0.dateField, t0.doubleField,"
                            + " t0.floatField, t0.intField, t0.longField,"
                            + " t0.shortField, t0.stringField FROM "
                            + "AllFieldTypes t0 WHERE t0.id = \\? "
                            + " FOR READ ONLY WITH RR USE AND KEEP UPDATE LOCKS" 
                            + " optimize for 1 row");
                    }
                    else if (((DB2Dictionary) dict).getDb2ServerType() == 5) {
                        assertEquals(1, sql.size());
                        assertSQL("SELECT t0.booleanField, t0.byteField, "
                            + "t0.charField, t0.dateField, t0.doubleField,"
                            + " t0.floatField, t0.intField, t0.longField,"
                            + " t0.shortField, t0.stringField FROM "
                            + "AllFieldTypes t0 WHERE t0.id = \\? "
                            + " FOR READ ONLY WITH RR USE AND KEEP EXCLUSIVE" 
                            + " LOCKS optimize for 1 row");
                    }    
                    else {
                        fail("OpenJPA currently only supports per-query" 
                            +" isolation level configuration on the following" 
                            +" databases: DB2");
                    }
                }    
            }
        } catch (InvalidStateException pe) {
            // if we're not using DB2, we expect an InvalidStateException.
            if (dict instanceof DB2Dictionary)
                throw pe;
        } finally {
            em.getTransaction().rollback();
            em.close();
        }
    }
}
