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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import org.apache.openjpa.datacache.*;
import javax.persistence.*;
import org.apache.openjpa.persistence.*;


import org.apache.openjpa.persistence.datacache.common.apps.CacheObjectE;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;

public class TestQueryResultSize
    extends AbstractTestCase {

    public TestQueryResultSize(String test) {
        super(test, "datacachecactusapp");
    }

    private EntityManagerFactory _pmf;
    private OpenJPAEntityManager pm;

    public void setUp() {
        System.out.println("****Deleted Records "
            + deleteAll(CacheObjectE.class));
        Map propsMap = new HashMap();
        propsMap.put("openjpa.DataCache", "true");
        propsMap.put("openjpa.QueryCache", "true");
        propsMap.put("openjpa.RemoteCommitProvider", "sjvm");
        _pmf = getEmf(propsMap);
    }

    public void test() {
        CacheObjectE pc1 = new CacheObjectE();
        pc1.setStr("pc1");

        pm = (OpenJPAEntityManager) _pmf.createEntityManager();

        startTx(pm);
        pm.persist(pc1);
        endTx(pm);

        pm.getFetchPlan().setQueryResultCacheEnabled(false);
        OpenJPAQuery q = pm.createQuery(
            "select a FROM " + CacheObjectE.class.getSimpleName() +
                " a where a.str = 'pc1'");
        List res = (List) q.getResultList();
        assertEquals(0, getQueryCacheSize());
        endEm(pm);

        System.out.println("****Deleted Records " + 
            deleteAll(CacheObjectE.class));
    }

    private int getQueryCacheSize() {
        return ( ((ConcurrentQueryCache)(OpenJPAPersistence.cast(
            pm.getEntityManagerFactory()).getQueryResultCache().getDelegate())).
            getCacheMap().size());
    }
}
