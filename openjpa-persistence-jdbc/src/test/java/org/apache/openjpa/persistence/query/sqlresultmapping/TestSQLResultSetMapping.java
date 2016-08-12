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
package org.apache.openjpa.persistence.query.sqlresultmapping;

import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.openjpa.persistence.querycache.QCEntity;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/*
 * Test for OPENJPA-2651.
 */
public class TestSQLResultSetMapping extends SingleEMFTestCase {

    public void setUp() {
        super.setUp(DROP_TABLES, CrtOperacaoEntity.class,
            CrtRequisicaoEntity.class, CrtRequisicaoChequePersEntity.class);

        // Set up necessary test data:
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        CrtOperacaoEntity op = new CrtOperacaoEntity();
        op.setId(25384);
        op.setDataHora(Timestamp.valueOf("2014-12-16 15:24:54.0"));
        em.persist(op);

        CrtOperacaoEntity op2 = new CrtOperacaoEntity();
        op2.setId(23409);
        op2.setDataHora(Timestamp.valueOf("2014-10-27 16:12:53.0"));
        em.persist(op2);

        CrtRequisicaoChequePersEntity reqCheq =
            new CrtRequisicaoChequePersEntity();
        reqCheq.setId(500006164);
        reqCheq.setCrtOperacaoByOperacaoRecepcaoServCent(op);
        reqCheq.setCrtOperacaoByOperacaoRequisicao(op2);
        em.persist(reqCheq);

        em.getTransaction().commit();
        em.close();
    }

    /*
     * Prior to OPENJPA-2651, this test would result in the following exception:
     * 
     * PersistenceException: Column '0' not found.
     *   FailedObject: 
     *   org.apache.openjpa.persistence.query.sqlresultmapping.CrtOperacaoEntity-500006164 [java.lang.String]
     *        at org.apache.openjpa.jdbc.sql.DBDictionary.narrow(DBDictionary.java:4998)
     *   .....
     *   Caused by: java.sql.SQLException: Column '0' not found.         
     */
    public void testMappingNoException() {

        String sql = "SELECT t0.ID, t2.DATA_HORA as opRecepcaoServCentraisDataHora, t2.ID as opRecepcaoServCentraisId"
                + " FROM CRT_REQUISICAO_CHEQUE_PERS t0, CRT_OPERACAO t2 WHERE t0.ID = 500006164 and t2.ID = 25384";

        EntityManager em = emf.createEntityManager();

        Query query = em.createNativeQuery(sql, "MyResultMapping");
        List<CrtRequisicaoChequePersEntity> res = query.getResultList();

        assertEquals(res.size(), 1);
        assertEquals(500006164, res.get(0).getId());
        assertEquals(25384, res.get(0).getCrtOperacaoByOperacaoRecepcaoServCent().getId());        
        assertEquals(Timestamp.valueOf("2014-12-16 15:24:54.0"), 
            res.get(0).getCrtOperacaoByOperacaoRecepcaoServCent().getDataHora());

        em.close();
    }

    /*
     * Prior to OPENJPA-2651, this test would result in the wrong id provided in the 
     * CrtOperacaoEntity.  Specifically, the ID in CrtOperacaoEntity would contain
     * '500006164', which is the ID for the CrtRequisicaoEntity.
     */
    public void testMappingCorrectID() {

        String sql =
            "SELECT t0.ID, t1.ID as opRecepcaoServCentraisId, t1.DATA_HORA as opRecepcaoServCentraisDataHora, "
                + "t2.ID, t2.DATA_HORA, t3.ID, t4.ID, t4.OPERACAO_RECEPCAO_SERV_CENT, "
                + "t4.OPERACAO_REQUISICAO FROM CRT_REQUISICAO_CHEQUE_PERS t0 LEFT OUTER JOIN "
                + "CRT_OPERACAO t1 ON t0.OPERACAO_RECEPCAO_SERV_CENT = t1.ID LEFT OUTER JOIN "
                + "CRT_OPERACAO t2 ON t0.OPERACAO_REQUISICAO = t2.ID "
                + "LEFT OUTER JOIN CRT_REQUISICAO t3 "
                + "ON t0.ID = t3.ID INNER JOIN CRT_REQUISICAO t5 "
                + "ON t0.ID = t5.ID LEFT OUTER JOIN "
                + "CRT_REQUISICAO_CHEQUE_PERS t4 "
                + "ON t3.ID = t4.ID WHERE t0.ID = 500006164";

        EntityManager em = emf.createEntityManager();

        Query query = em.createNativeQuery(sql, "MyResultMapping");
        List<CrtRequisicaoChequePersEntity> res = query.getResultList();
        
        assertEquals(res.size(), 1);
        assertEquals(500006164, res.get(0).getId());
        assertEquals(25384, res.get(0).getCrtOperacaoByOperacaoRecepcaoServCent().getId());
        assertEquals(Timestamp.valueOf("2014-12-16 15:24:54.0"), 
            res.get(0).getCrtOperacaoByOperacaoRecepcaoServCent().getDataHora());

        em.close();
    }
}
