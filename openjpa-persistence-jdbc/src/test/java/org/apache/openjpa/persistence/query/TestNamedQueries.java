/*
 * TestNamedQueries.java
 *
 * Created on October 18, 2006, 1:17 PM
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
package org.apache.openjpa.persistence.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;

import jakarta.persistence.TypedQuery;

import org.apache.openjpa.DynamicEnhancementTest;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAEntityManagerSPI;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.query.common.apps.EntityInterface;
import org.apache.openjpa.persistence.query.common.apps.QueryTest1;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@DynamicEnhancementTest
class TestNamedQueries extends BaseQueryTest {
    @BeforeEach
    void setUp() {
        deleteAll(QueryTest1.class);

        OpenJPAEntityManager pm = getEM();
        startTx(pm);
        QueryTest1 pc = null;
        for (int i = 0; i < 10; i++) {
            pc = new QueryTest1();
            if (i < 5)
                pc.setNum(4);
            else
                pc.setNum(i + 10);
            pm.persist(pc);
        }
        endTx(pm);
        endEm(pm);
    }

    @Test
    void namedClassQuery() {
        OpenJPAEntityManager pm = getEM();

        OpenJPAQuery query = pm.createQuery("SELECT o FROM QueryTest1 o");
        query.setResultClass(QueryTest1.class);

        assertEquals(QueryTest1.class, query.getResultClass());
        assertEquals("SELECT o FROM QueryTest1 o", query.getQueryString());
        assertEquals(QueryTest1.class, query.getResultClass());
        //FIXME jthomas
        /*
        assertEquals("import java.util.*", query.getImports());
        assertEquals("int pnum", query.getParameters());
        assertEquals("QueryTest4 v4", query.getVariables());
        assertEquals("sum(num)", query.getResult());
        assertEquals("num", query.getGrouping());
        */
        QueryMetaData meta = ((OpenJPAEntityManagerSPI) pm).getConfiguration().
            getMetaDataRepositoryInstance().getQueryMetaData(null,
            "named", pm.getClassLoader(), true);
        assertNotNull(meta);
        assertEquals("SELECT o FROM QueryTest1 o", meta.getQueryString());
        assertNull(meta.getResultType());

        endEm(pm);
    }

    @Test
    void namespace() {
        OpenJPAEntityManager pm = getEM();
        OpenJPAQuery query = (OpenJPAQuery) pm.createNamedQuery("named");
        assertEquals("SELECT o FROM QueryTest1 o", query.getQueryString());
        query.closeAll();
        endEm(pm);
    }

    @Test
    void systemJDOQL() {
        // make sure local query metadata is parsed
        OpenJPAEntityManager pm = getEM();

        OpenJPAQuery query = (OpenJPAQuery) pm.createNamedQuery("named");
        assertEquals("SELECT o FROM QueryTest1 o", query.getQueryString());
        assertEquals(QueryTest1.class, query.getResultClass());
        assertEquals(10, ((Collection) query.getResultList()).size());
        query.closeAll();
        endEm(pm);
    }

    @Test
    void interfaceResultClass() {
        OpenJPAEntityManager pm = getEM();

        TypedQuery<EntityInterface> query = pm.createNamedQuery("named", EntityInterface.class);
        assertEquals(10, query.getResultList().size());
    }
}
