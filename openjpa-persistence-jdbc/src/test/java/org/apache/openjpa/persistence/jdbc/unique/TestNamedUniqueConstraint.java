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
package org.apache.openjpa.persistence.jdbc.unique;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.jdbc.SQLSniffer;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestNamedUniqueConstraint extends SQLListenerTestCase {
    @Override
    public void setUp(Object... props) {
        super.setUp(DROP_TABLES, NamedUniqueA.class, NamedUniqueB.class);
    }
    
    public void testMapping() {
        
        // If the database does not support unique constraints, exit
        if (!supportsUniqueConstraints())
            return;
        
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.getTransaction().commit();
        em.close();
        // The above should trigger schema definition
        
        List<String> sqls = super.sql;
        
        assertSQLFragnments(sqls, "CREATE TABLE N_UNIQUE_A",
            "uca_f1_f2 UNIQUE .*\\(f1, f2\\)", 
            "uca_f3_f4 UNIQUE .*\\(f3, f4\\).*");
        assertSQLFragnments(sqls, "CREATE TABLE N_UNIQUE_B",
            "ucb_f1_f2 UNIQUE .*\\(f1, f2\\).*");
        assertSQLFragnments(sqls, "CREATE TABLE N_UNIQUE_SECONDARY",
            "uca_sf1 UNIQUE .*\\(sf1\\)");
        assertSQLFragnments(sqls, "CREATE TABLE N_UNIQUE_GENERATOR",
            "ucb_gen1_gen2 UNIQUE .*\\(GEN1, GEN2\\)");
        assertSQLFragnments(sqls, "CREATE TABLE N_UNIQUE_JOINTABLE",
            "uca_fka_fkb UNIQUE .*\\(FK_A, FK_B\\)");
        assertSQLFragnments(sqls, "CREATE TABLE N_U_COLL_TBL",
            "ucb_f3 UNIQUE .*\\(f3\\).*");
    }
        
    private boolean supportsUniqueConstraints() {
        OpenJPAEntityManagerFactorySPI emfs = (OpenJPAEntityManagerFactorySPI)emf;
        JDBCConfiguration jdbccfg = (JDBCConfiguration)emfs.getConfiguration();
        return jdbccfg.getDBDictionaryInstance().supportsUniqueConstraints;
    }

    void assertSQLFragnments(List<String> list, String... keys) {
        if (SQLSniffer.matches(list, keys))
            return;
        fail("None of the following " + sql.size() + " SQL \r\n" + 
                toString(sql) + "\r\n contains all keys \r\n"
                + toString(Arrays.asList(keys)));
    }
}
