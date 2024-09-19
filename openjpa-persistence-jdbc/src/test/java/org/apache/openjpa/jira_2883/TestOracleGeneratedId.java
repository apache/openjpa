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
package org.apache.openjpa.jira_2883;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.persistence.test.DatabasePlatform;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that entity with generated id created as expected
 *
 *   <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2883">OPENJPA-2883</A>
 *
 */
@DatabasePlatform("oracle.jdbc.driver.OracleDriver")
public class TestOracleGeneratedId extends SingleEMFTestCase {
    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, OracleGeneratedIdEntity.class,
                "openjpa.jdbc.DBDictionary", "oracle(UseTriggersForAutoAssign=true, MaxAutoAssignNameLength=28, BatchLimit=100)");
    }

    public void testGeneratedId() {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            assertFalse("'supportsAutoAssign' should be turned OFF", (((JDBCConfiguration) emf.getConfiguration()).
                    getDBDictionaryInstance().supportsAutoAssign));
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

}
