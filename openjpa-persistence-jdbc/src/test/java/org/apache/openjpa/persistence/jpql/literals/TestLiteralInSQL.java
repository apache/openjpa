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
package org.apache.openjpa.persistence.jpql.literals;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.PostgresDictionary;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestLiteralInSQL extends SQLListenerTestCase {
    public void setUp() {
        setUp(AllFieldTypes.class, "openjpa.jdbc.QuerySQLCache", "false");
    }

    public void testTrueInSQL() {
        EntityManager em = emf.createEntityManager();

        em = emf.createEntityManager();
        DBDictionary dict = ((JDBCConfiguration)emf.getConfiguration()).getDBDictionaryInstance();
        //Disable on Postgres for now....
        if (dict instanceof PostgresDictionary){
            setTestsDisabled(true);
            return;
        }

        resetSQL();
        Query q = em.createQuery("SELECT f FROM AllFieldTypes f WHERE f.booleanField=true");
        q.setHint("openjpa.hint.UseLiteralInSQL", "false");
        q.getResultList();
        // The literal should be converted to a parameter marker since UseLiteralInSQL is false.
        assertContainsSQL("booleanField = ?");

        resetSQL();
        q = em.createQuery("SELECT f FROM AllFieldTypes f WHERE f.booleanField=true");
        q.setHint("openjpa.hint.UseLiteralInSQL", "true");
        q.getResultList();
        // The literal should not be converted to a parameter marker since UseLiteralInSQL is true.
        // However, the literal should be converted to a 1 because we store boolean as int/smallint.
        assertContainsSQL("booleanField = 1");

    }
}
