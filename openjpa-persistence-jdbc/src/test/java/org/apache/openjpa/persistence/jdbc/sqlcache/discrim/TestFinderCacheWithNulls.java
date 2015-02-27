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
package org.apache.openjpa.persistence.jdbc.sqlcache.discrim;

import javax.persistence.EntityManager;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.FinderCache;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;


public class TestFinderCacheWithNulls extends SingleEMFTestCase {
    private FetchConfiguration fetchCfg;
    private FinderCache fndrCache;
    private ClassMapping clsMapping_UserData;
    private ClassMapping clsMapping_AbstractExtValue;

    @Override
    public void setUp() throws Exception {
        super.setUp(AbstractExtValue.class, ComposedPK.class, ExtValue1.class, UserData.class);
    }
    
    public void test() {

        init();
        initData();
        
        EntityManager em = this.emf.createEntityManager();
        
        assertNull(fndrCache.get(clsMapping_UserData, fetchCfg));
        
        UserData usrData=em.find(UserData.class, new ComposedPK(Short.valueOf("2"), null));
        assertNull(usrData);
        //FinderCache should be empty.  That is, since the previous find contained a NULL,
        //the cache shouldn't not contain the finder SQL.  However, prior to OPENJPA-2557, 
        //the finder cache contain the finder SQL with the NULL value.  With this 
        //JIRA, the cache should not contain the finder.
        assertNull(fndrCache.get(clsMapping_UserData, fetchCfg));
        em.clear();
        
        usrData=em.find(UserData.class, new ComposedPK(Short.valueOf("2"), 3));       
        //Prior to OPENJPA-2557, the UserData would not have been found because the previous 
        //find with a NULL would have been cached.  
        assertNotNull(usrData);
        assertNull(fndrCache.get(clsMapping_UserData, fetchCfg));  
        em.clear();

        ExtValue1 ev1 = em.find(ExtValue1.class, "A");       
        assertNotNull(ev1);
        assertNotNull(fndrCache.get(clsMapping_AbstractExtValue, fetchCfg));
        em.clear();
        
        fndrCache.invalidate(clsMapping_AbstractExtValue);
        assertNull(fndrCache.get(clsMapping_AbstractExtValue, fetchCfg));

        AbstractExtValue aev = em.find(AbstractExtValue.class, "A");       
        assertNotNull(aev);
        assertNotNull(fndrCache.get(clsMapping_AbstractExtValue, fetchCfg));

        em.close();
    }
    
    public void init(){
        EntityManager em = emf.createEntityManager();
        
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();        
        clsMapping_UserData = conf.getMappingRepositoryInstance().getMapping(UserData.class, null, true);
        clsMapping_AbstractExtValue = conf.getMappingRepositoryInstance().
                                           getMapping(AbstractExtValue.class, null, true);
        
        fetchCfg = ((org.apache.openjpa.persistence.EntityManagerImpl) em).getBroker().getFetchConfiguration();

        fndrCache = ((JDBCConfiguration) emf.getConfiguration()).getFinderCacheInstance();
        
        em.close();
    }

    public void initData() {
        EntityManager em = emf.createEntityManager();
        
        ExtValue1 extValue1 = new ExtValue1();
        extValue1.setCode("A");
        em.getTransaction().begin();
        em.persist(extValue1);
        em.flush();        
        
        ComposedPK pK = new ComposedPK((short) 2, 3);
        UserData userData = new UserData();
        userData.setPk(pK);
        userData.setExtValue(extValue1);
        em.persist(userData);
        em.getTransaction().commit();
        em.close();
    }
}
