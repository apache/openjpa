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
package org.apache.openjpa.persistence.datacache;

import java.util.Map;

import javax.persistence.EntityManager;
import org.apache.openjpa.persistence.datacache.common.apps.M2MEntityE;
import org.apache.openjpa.persistence.datacache.common.apps.M2MEntityF;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestM2MInDataCache extends SingleEMFTestCase {
    public void setUp() {
        setUp("openjpa.DataCache", "true", 
            "openjpa.RemoteCommitProvider", "sjvm", 
            M2MEntityE.class,
            M2MEntityF.class, CLEAR_TABLES);
    }

    /**
     * Test if child list is in order after new child list is added in setup().
     *
     */
    public void testM2MDataCache(){
    	EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        M2MEntityE e1 = new M2MEntityE();
        e1.setId(1);
        e1.setName("ABC");
        em.persist(e1);
        M2MEntityE e2 = new M2MEntityE();
        e2.setId(2);
        e2.setName("DEF");
        em.persist(e2);
        
        M2MEntityF f1 = new M2MEntityF();
        f1.setId(10);
        em.persist(f1);
        M2MEntityF f2 = new M2MEntityF();
        f2.setId(20);
        em.persist(f2);
        
        e1.getEntityF().put(f1.getId(), f1);
        e1.getEntityF().put(f2.getId(), f2);
        e2.getEntityF().put(f1.getId(), f1);
        e2.getEntityF().put(f2.getId(), f2);
        
        f1.getEntityE().put(e1.getName(), e1);
        f1.getEntityE().put(e2.getName(), e2);
        f2.getEntityE().put(e1.getName(), e1);
        f2.getEntityE().put(e2.getName(), e2);
        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        M2MEntityE e1a = em.find(M2MEntityE.class, 1);
        Map entityf1 = e1a.getEntityF();
        M2MEntityE e2a = em.find(M2MEntityE.class, 2);
        Map entityf2 = e2a.getEntityF();
        M2MEntityF f1a = em.find(M2MEntityF.class, 10);
        Map entitye1 = f1a.getEntityE();
        M2MEntityF f2a = em.find(M2MEntityF.class, 20);
        Map entitye2 = f2a.getEntityE();
    }
}
