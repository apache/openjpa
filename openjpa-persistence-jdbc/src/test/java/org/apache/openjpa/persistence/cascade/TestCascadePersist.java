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
package org.apache.openjpa.persistence.cascade;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

public class TestCascadePersist extends SQLListenerTestCase {
    @Override
    public void setUp() throws Exception {
        setUp(DROP_TABLES, CascadePersistEntity.class, "openjpa.Compatibility",
            "CheckDatabaseForCascadePersistToDetachedEntity=false");
    }

    public void testCascadePersistToDetachedFailure() {
        long startId = System.currentTimeMillis();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        CascadePersistEntity cpe1 = new CascadePersistEntity(startId);
        em.persist(cpe1);
        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();
        CascadePersistEntity cpe2 = new CascadePersistEntity(startId + 1);
        CascadePersistEntity cpe3 = new CascadePersistEntity(startId);

        cpe2.setOther(cpe3);
        em.persist(cpe2);
        try {
            em.getTransaction().commit();
        } catch (RollbackException re) {
            // We expect this failure because we are trying to cascade a persist to an existing Entity. Changing
            // CheckDatabaseForCascadePersistToDetachedEntity=true would avoid this exception and revert back to pre
            // 2.2.x behavior.
        }
    }

    public void testCascadePersistToManagedEntity() {
        long startId = System.currentTimeMillis();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        CascadePersistEntity cpe1 = new CascadePersistEntity(startId);
        em.persist(cpe1);

        em.flush();
        CascadePersistEntity cpe2 = new CascadePersistEntity(startId + 1);

        cpe2.setOther(cpe1);
        em.persist(cpe2);
        // Since cpe1 is managed, it should be ignored by the cascaded persist operation.
        em.getTransaction().commit();
    }
    
    /*
     * Prior to OPENJPA-1986, an extra SELECT was executed in this scenario.
     */
    public void testCascaseExtraneousSQL(){
        long id = System.currentTimeMillis();
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        CascadePersistEntity cpe = new CascadePersistEntity(id);
        CascadePersistEntity cpe2 = new CascadePersistEntity(id+1);
        cpe.setOther(cpe2);

        //Clear all SQL statements to make sure that during
        //the persist we don't execute a select.
        resetSQL();
        em.persist(cpe);

        em.getTransaction().commit();
        em.close();        

        //There should be no selects at this point, only
        //inserts.
        assertNotSQL("SELECT .*");
    }    
}
