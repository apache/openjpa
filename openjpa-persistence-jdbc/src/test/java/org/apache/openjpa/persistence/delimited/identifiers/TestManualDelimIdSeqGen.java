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
package org.apache.openjpa.persistence.delimited.identifiers;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestManualDelimIdSeqGen extends SQLListenerTestCase {
    OpenJPAEntityManager em;
    JDBCConfiguration conf;
    DBDictionary dict;
    boolean supportsNativeSequence = false;
    
    EntityE entityE;
    
    @Override
    public void setUp() throws Exception {
        super.setUp(EntityE.class,DROP_TABLES);
        assertNotNull(emf);
        
        conf = (JDBCConfiguration) emf.getConfiguration();
        dict = conf.getDBDictionaryInstance();
        supportsNativeSequence = dict.nextSequenceQuery != null;
        
        if (supportsNativeSequence) {
            em = emf.createEntityManager();
            assertNotNull(em);
        }
    }
    
    public void createEntityE() {
        entityE = new EntityE("e name");
    }
    
    // TODO: temp
//    public void testDBCapability() {
//        Connection conn = (Connection)em.getConnection();
//        try {
//            DatabaseMetaData meta = conn.getMetaData();
//            System.out.println("LC - " + 
//                meta.storesLowerCaseIdentifiers());
//            System.out.println("LCQ - " + 
//                meta.storesLowerCaseQuotedIdentifiers());
//            System.out.println("MC - " + 
//                meta.storesMixedCaseIdentifiers());
//            System.out.println("MCQ - " + 
//                meta.storesMixedCaseQuotedIdentifiers());
//            System.out.println("UC - " + 
//                meta.storesUpperCaseIdentifiers());
//            System.out.println("UCQ - " + 
//                meta.storesUpperCaseQuotedIdentifiers());
//            
//            System.out.println("db product name - " + 
//                meta.getDatabaseProductName());
//            System.out.println("db product version - " + 
//                meta.getDatabaseProductVersion());
//            System.out.println("driver name - " + 
//                meta.getDriverName());
//            System.out.println("driver version - " + 
//                meta.getDriverVersion());
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
    
    public void testSeqGen() {
        if (!supportsNativeSequence) {
            return;
        }
        createEntityE();
        
        em.getTransaction().begin();
        em.persist(entityE);
        em.getTransaction().commit();
        
        System.out.println(super.toString(sql));
        
        int genId = entityE.getId();
        System.out.println("generated id - " + genId);
        em.clear();
        em.getTransaction().begin();
        EntityE eA = em.find(EntityE.class, genId);
        assertEquals("e name", eA.getName());
        
        em.getTransaction().commit();
        em.close();
    }
}
