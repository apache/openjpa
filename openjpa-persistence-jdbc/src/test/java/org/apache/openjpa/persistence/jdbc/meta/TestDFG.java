/*
 * TestDFG.java
 *
 * Created on October 2, 2006, 5:55 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

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
package org.apache.openjpa.persistence.jdbc.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityTransaction;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.jdbc.common.apps.DFGTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class TestDFG extends org.apache.openjpa.persistence.jdbc.kernel.BaseJDBCTest {

    private Object oid;

    @BeforeEach
    public void setUp() {
       deleteAll(DFGTest.class);
        OpenJPAEntityManager pm = (OpenJPAEntityManager)currentEntityManager();
        EntityTransaction t= pm.getTransaction();
        t.begin();
        DFGTest a = new DFGTest();
        pm.persist(a);
        a.setNonDFGField(2);
        t.commit();

        // modify a field so that if we're examining sql we can see
        // how much is flushed.
        t.begin();
        a.setDFGField(1);
        t.commit();

        oid = pm.getObjectId(a);
        pm.close();
    }

    @Test
    public void testDFG() {
        OpenJPAEntityManager pm = (OpenJPAEntityManager)currentEntityManager();

        DFGTest a = (DFGTest) pm.getObjectId(oid);

        // check the non-dfg fields

        OpenJPAStateManager sm = getStateManager(a,pm);
        FieldMetaData fmd = sm.getMetaData().getField("nonDFGField");
        assertTrue(!sm.getLoaded().get(fmd.getIndex()),
                "nonDFGField should not be loaded");

        fmd = sm.getMetaData().getField("dfgField");
        assertTrue(sm.getLoaded().get(fmd.getIndex()),
                "dfgField should be loaded");

        int val = a.getNonDFGField();
        assertTrue(sm.getLoaded().get(fmd.getIndex()),
                "nonDFGField should be loaded");
        assertEquals(2, val);

        pm.close();
    }

    // it'd be nice if we could actually automate this test. As it
    // stands, this is just here so that there's an easy place to look
    // at the SQL.
    @Test
    public void testDFGWrites() {
        OpenJPAEntityManager pm = (OpenJPAEntityManager)currentEntityManager();
        startTx(pm);


        DFGTest a = (DFGTest) pm.getObjectId(oid);
        a.setDFGField(3);
        endTx(pm);

        pm.close();
    }

}
