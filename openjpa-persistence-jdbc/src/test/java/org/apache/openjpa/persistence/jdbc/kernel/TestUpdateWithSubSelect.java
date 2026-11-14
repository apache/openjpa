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
package org.apache.openjpa.persistence.jdbc.kernel;

import jakarta.persistence.EntityManager;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.jdbc.sqlcache.Address;
import org.apache.openjpa.persistence.jdbc.sqlcache.Person;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests bulk update with join instead of subselect.
 * Reported in <A HREF="https://issues.apache.org/jira/browse/OPENJPA-459">OPENJPA-459</A>
 * that update in MySQL fails when the UPDATE SQL included a SubSelect because of
 * limitation mentioned in <A HREF="http://lists.mysql.com/mysql/185623">MySQL forum</A>.
 * <br>
 * The solution is {@link DBDictionary#toBulkOperation()} modified to accommodate
 * scenarios that selects from a single table but may involve joins of multiple tables.
 *
 * @author Pinaki Poddar
 *
 */
class TestUpdateWithSubSelect extends SQLListenerTestCase {

    @Override
    protected boolean isSupported(DBDictionary dict) {
        return dict.supportsSubselect && dict.allowsAliasInBulkClause;
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp(CLEAR_TABLES, Person.class, Address.class);
    }

    /**
     * Tests that a bulk update issues a single SQL and that uses a join.
     */
    @Test
    void updateBySubSelect() {
        String jpql = "UPDATE Person p SET p.age = :age WHERE p.address.city = :city";
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        sql.clear();
        em.createQuery(jpql)
          .setParameter("age", (short)25)
          .setParameter("city", "SFO")
          .executeUpdate();
        em.getTransaction().commit();
        assertEquals(1,sql.size());
        String sqlString = sql.get(0).toUpperCase(Locale.ROOT).trim();
        assertTrue(sqlString.startsWith("UPDATE"));
        // assert JOIN condition
        assertTrue(sqlString.indexOf("T0.ADDRESS_ID = T1.ID") != -1);
    }

}
