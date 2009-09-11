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
package org.apache.openjpa.persistence.identity;

import java.math.BigDecimal;
import javax.persistence.EntityManager;

import junit.textui.TestRunner;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * @author <a href="mailto:mnachev@gmail.com">Miroslav Nachev</a>
 */
public class TestSQLBigDecimalId
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(SQLBigDecimalIdEntity.class, DROP_TABLES);
    }

    public void testPersist() {
        long time = ((long) (System.currentTimeMillis() / 1000)) * 1000;
        BigDecimal decimal = new BigDecimal(time);

        SQLBigDecimalIdEntity e = new SQLBigDecimalIdEntity();
        e.setId(decimal);
        e.setData(1);

        // trigger schema definition
        JDBCConfiguration jdbccfg = (JDBCConfiguration)emf.getConfiguration();
        DBDictionary dict = jdbccfg.getDBDictionaryInstance();
        //currently BigDecimal is mapped to NUMERIC column type. This causes
        //truncation error from MySQL. Without knowing the implication of changing the 
        //mapping of BigDecimal universally to DOUBLE, I will just change the mapping
        //for this test case. 
        if (dict instanceof MySQLDictionary) {
            dict.numericTypeName = "DOUBLE";
        }
        
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(e);
        em.getTransaction().commit();
        assertEquals(time, e.getId().longValue());
        em.close();

        em = emf.createEntityManager();
        e = em.find(SQLBigDecimalIdEntity.class, decimal);
        assertEquals(1, e.getData());
        em.close();
    }

    public static void main(String[] args) {
        TestRunner.run(TestSQLBigDecimalId.class);
    }
}
