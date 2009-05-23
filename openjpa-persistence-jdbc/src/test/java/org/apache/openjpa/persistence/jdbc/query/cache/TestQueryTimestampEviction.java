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
package org.apache.openjpa.persistence.jdbc.query.cache;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.datacache.ConcurrentQueryCache;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.QueryResultCacheImpl;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.apache.openjpa.util.CacheMap;

public class TestQueryTimestampEviction extends SingleEMFTestCase {

    private boolean deleteData = false;
    private boolean recreateData = true;

    public void setUp() throws Exception {
        super.setUp(Part.class, PartBase.class, PartComposite.class,
                Supplier.class, Usage.class,
                "openjpa.DataCache", "true",
                "openjpa.QueryCache",
                "CacheSize=1000, EvictPolicy='timestamp'",
                "openjpa.RemoteCommitProvider", "sjvm");

        // Not all databases support GenerationType.IDENTITY column(s)
        if (((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign && recreateData) {
            // deletes any data leftover data in the database due to the failed
            // last run of this testcase
            deleteAllData(); 
            reCreateData();
        }
    }

    public void testLoadQueries() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
        	return;
        }                                 
        loadQueryCache();
        int cacheSizeBeforeUpdate = queryCacheGet();
        updateAnEntity();
        int cacheSizeAfterUpdate = queryCacheGet();

        // If evictPolicy is timestamp the querycache size should be equal to
        // cacheSizeBeforeUpdate value.
        assertEquals(cacheSizeBeforeUpdate, cacheSizeAfterUpdate);

        this.recreateData = false;
    }

    public void testEviction() {
        // Not all databases support GenerationType.IDENTITY column(s)
        if (!((JDBCConfiguration) emf.getConfiguration()).
            getDBDictionaryInstance().supportsAutoAssign) {
            return;
        }
        loadQueryCache();
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateAnEntity();

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        String insert1 = "insert into part(partno,parttype,name,cost,mass)" +
            " values(13,'PartBase','breakes',1000.0,100.0)";
        em.createNativeQuery(insert1).executeUpdate();
        String insert2 = "insert into supplier_part(suppliers_sid," +
            "supplies_partno) values(1,13)";
        em.createNativeQuery(insert2).executeUpdate();

        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        em.getTransaction().begin();

        String sql = "select partno from part where cost > 120 ";
        Query nativeq = em.createNativeQuery(sql);
        int nativelistSize = nativeq.getResultList().size();

        em.getTransaction().commit();
        em.close();

        em = emf.createEntityManager();
        em.getTransaction().begin();
        Query q = em.createQuery("select p from PartBase p where p.cost>?1");
        q.setParameter(1, new Double(120));
        int jpalistSize = q.getResultList().size();

        em.getTransaction().commit();
        em.close();

        // The resultlist of nativelist and jpalist should be the same 
        // in both eviction policies(dafault/timestamp)
        assertEquals(nativelistSize,jpalistSize);

        this.deleteData = true;
        this.recreateData = true;
    }

    private void loadQueryCache() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        String qry = "select p from PartBase p where p.cost > ?1";
        for (int i=120; i<155; i++) {
            Query q = em.createQuery(qry);
            q.setParameter(1, new Double(i));
            q.getResultList();
        }
        em.getTransaction().commit();
        em.close();
    }

    private void updateAnEntity() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        //Update entity
        PartBase p = em.find(PartBase.class,11);
        double oldcost = p.getCost();
        if (p != null) 
            p.setCost((oldcost + 10.0));

        em.getTransaction().commit();
        em.close();
    }

    private ConcurrentQueryCache getQueryCache() {
        OpenJPAEntityManagerFactory oemf = OpenJPAPersistence.cast(emf);
        QueryResultCacheImpl scache = (QueryResultCacheImpl) oemf.
        getQueryResultCache();

        return (ConcurrentQueryCache ) scache.getDelegate();
    }

    private int  queryCacheGet() {
        ConcurrentQueryCache dcache = getQueryCache();
        CacheMap map = dcache.getCacheMap();
        return map.size();
    }

    public void tearDown() throws Exception {
        if (deleteData)
            deleteAllData();
    }

    private void reCreateData() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Supplier s1 = new Supplier(1, "S1");
        em.persist(s1);
        Supplier s2 = new Supplier(2, "S2");
        em.persist(s2);
        Supplier s3 = new Supplier(3, "S3");
        em.persist(s3);

        PartBase p1 = new PartBase(10, "Wheel", 150, 15.00);
        em.persist(p1);
        PartBase p2 = new PartBase(11, "Frame", 550.00, 25.00);
        em.persist(p2);
        PartBase p3 = new PartBase(12, "HandleBar", 125.00, 80.00);
        em.persist(p3);

        s1.addPart(p1).addPart(p2).addPart(p3);
        s2.addPart(p1).addPart(p3);

        PartComposite p4 = new PartComposite(20, "Bike", 180, 1.0);
        em.persist(p4);
        p4.addSubPart(em, 2, p1).addSubPart(em, 1, p2).addSubPart(em, 1, p3);

        em.getTransaction().commit();
        em.close();
    }

    private void deleteAllData() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        em.createNativeQuery("delete from supplier_part").executeUpdate();
        em.createQuery("delete from PartBase s").executeUpdate();
        em.createQuery("delete from Supplier s").executeUpdate();
        em.createQuery("delete from Usage u").executeUpdate();
        em.createQuery("delete from Part p").executeUpdate();

        em.getTransaction().commit();
        em.close();
    }
}
