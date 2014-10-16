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
package org.apache.openjpa.persistence.jdbc.dbcs;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/*
 * This test will verify that when a double byte charater set (DBCS) character
 * is used in an entity, that quotes are not added to the identifier.  See
 * JIRA OPENJPA-2535.
 */
public class TestDBCS extends SQLListenerTestCase {

    public void setUp() {
        setUp(MyDBCSEntity.class);
    }

    // Test test is disabled as most won't have their environemnt configured to support UTF-8 chars
    public void _test() {
        EntityManager em = emf.createEntityManager();
        String qStr = "SELECT m FROM MyDBCSEntity m WHERE m.閉塞フラグ = '0' ORDER BY m.業務id";

        Query query = em.createQuery(qStr);
        resetSQL();
        query.getResultList();

        //Prior to OPENJPA-2535 the identifies with DBCS characters would be quoted.
        //Verify the identifies don't contain quotes.
        assertContainsSQL(".業務id");
        assertContainsSQL(".閉塞フラグ");
    }
}

