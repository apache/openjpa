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
package org.apache.openjpa.persistence.jdbc.annotations;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Test for sequence generator. Some databases currently (Derby) do not support
 * native sequences so this method is allowed to fail until such time when the
 * test corpus can support database or dictionary support specific 
 * configuration.
 *
 * @author Jeremy Bauer
 */
@AllowFailure(true)
public class TestSequenceGenerator extends SingleEMFTestCase {

    public void setUp()
        throws Exception {
        setUp(NativeSequenceEntity.class, 
            NativeORMSequenceEntity.class, 
            CLEAR_TABLES);
    }

    @Override
    protected String getPersistenceUnitName() {
        return "native-seq-pu";
    }

    /*
     * Test use of the schema attribute on a native sequence generator.  Some
     * databases do not support native sequences so this method is
     * currently allowed to fail. 
     */
    public void testSequenceSchema() {
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        NativeSequenceEntity nse = new NativeSequenceEntity();
        nse.setName("Test");
        em.getTransaction().begin();
        em.persist(nse);
        em.getTransaction().commit();
        em.refresh(nse);
        // Validate the id is >= the initial value 
        // Assert the sequence was created in the DB
        assertTrue(sequenceExists(em, NativeSequenceEntity.SCHEMA_NAME,
                NativeSequenceEntity.SEQ_NAME));
        // Assert the id is >= the initial value 
        assertTrue(nse.getId() >= 10);
        em.close();
    }

    /*
     * Test use of the schema element on a native sequence generator.  Some
     * databases do not support native sequences so this method is
     * currently allowed to fail. 
     */
    public void testORMSequenceSchema() {
        OpenJPAEntityManagerSPI em = emf.createEntityManager();
        NativeORMSequenceEntity nse = new NativeORMSequenceEntity();
        nse.setName("TestORM");
        em.getTransaction().begin();
        em.persist(nse);
        em.getTransaction().commit();
        em.refresh(nse);
        // Assert the sequence was created in the DB
        assertTrue(sequenceExists(em, NativeORMSequenceEntity.SCHEMA_NAME,
                NativeORMSequenceEntity.SEQ_NAME));        
        // Assert the id is >= the initial value 
        assertTrue(nse.getId() >= 2000);
        em.close();
    }

    /**
     * Method to verify a sequence was created for the given schema and 
     * sequence name. 
     */
    private boolean sequenceExists(OpenJPAEntityManagerSPI em, String schema,
        String sequence) {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        DBDictionary dict = conf.getDBDictionaryInstance();
        Connection conn = (Connection)em.getConnection();
        try {
            DatabaseMetaData dbmd = conn.getMetaData();
            Sequence[] seqs = dict.getSequences(dbmd, null, schema, 
                    sequence, conn);
            if (seqs != null && seqs.length == 1 && 
                    seqs[0].getName().equalsIgnoreCase(sequence) &&
                    seqs[0].getSchemaName().equalsIgnoreCase(schema))
                return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }        
        return false;
    }
}
