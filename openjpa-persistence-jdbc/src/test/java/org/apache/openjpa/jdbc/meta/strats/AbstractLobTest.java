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

package org.apache.openjpa.jdbc.meta.strats;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.datacache.DataCachePCData;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DB2Dictionary;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.jdbc.sql.PostgresDictionary;
import org.apache.openjpa.jdbc.sql.SQLServerDictionary;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * This abstract class defines all the tests for LOBS.
 *
 * @author Ignacio Andreu
 * @since 1.1.0
 */

public abstract class AbstractLobTest extends SingleEMFTestCase {

    public void setUp() throws Exception {
        super.setUp(getLobEntityClass(), CLEAR_TABLES,
            "openjpa.DataCache", "true",
            "openjpa.RemoteCommitProvider", "sjvm");
    }

    public boolean isDatabaseSupported() {
        DBDictionary dict = ((JDBCConfiguration) emf.getConfiguration())
            .getDBDictionaryInstance();
        if (dict instanceof MySQLDictionary ||
            dict instanceof SQLServerDictionary ||
            dict instanceof OracleDictionary ||
            dict instanceof PostgresDictionary || 
            dict instanceof DB2Dictionary ) {
            return true;
        }
        return false;
    }

    public void insert(LobEntity le) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(le);
        em.getTransaction().commit();
        em.close();
    }

    public void testInsert() {
        if (!isDatabaseSupported()) return;
        insert(newLobEntity("oOOOOOo", 1));
    }

    public void testInsertAndSelect() throws IOException {
        if (!isDatabaseSupported()) return;
        String s = "oooOOOooo";
        insert(newLobEntity(s, 1));
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Query query = em.createQuery(getSelectQuery());
        LobEntity entity = (LobEntity) query.getSingleResult();
        assertNotNull(entity.getStream());
        assertEquals(s, getStreamContentAsString(entity.getStream()));
        em.getTransaction().commit();
        em.close();
    }

    public void testInsertNull() {
        if (!isDatabaseSupported()) return;
        insert(newLobEntity(null, 1));
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity le = (LobEntity) em.find(getLobEntityClass(), 1);
        assertNull(le.getStream());
        em.getTransaction().commit();
        em.close();
    }

    public void testUpdate() throws IOException {
        if (!isDatabaseSupported()) return;
        insert(newLobEntity("oOOOOOo", 1));
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity entity = (LobEntity) em.find(getLobEntityClass(), 1);
        String string = "iIIIIIi";
        changeStream(entity, string);
        em.getTransaction().commit();
        em.close();
        em = emf.createEntityManager();
        em.getTransaction().begin();
        entity = (LobEntity) em.find(getLobEntityClass(), 1);
        assertEquals(string, getStreamContentAsString(entity.getStream()));
        em.getTransaction().commit();
        em.close();
    }

    public void testUpdateWithNull() {
        if (!isDatabaseSupported()) return;
        insert(newLobEntity("oOOOOOo", 1));
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity entity = (LobEntity) em.find(getLobEntityClass(), 1);
        entity.setStream(null);
        em.getTransaction().commit();
        em.close();
        em = emf.createEntityManager();
        em.getTransaction().begin();
        entity = (LobEntity) em.find(getLobEntityClass(), 1);
        assertNull(entity.getStream());
        em.getTransaction().commit();
        em.close();
    }
    
    public void testUpdateANullObjectWithoutNull() throws IOException {
        if (!isDatabaseSupported()) return;
        insert(newLobEntity(null, 1));
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity entity = (LobEntity) em.find(getLobEntityClass(), 1);
        String string = "iIIIIIi";
        changeStream(entity, string);
        em.getTransaction().commit();
        em.close();
        em = emf.createEntityManager();
        em.getTransaction().begin();
        entity = (LobEntity) em.find(getLobEntityClass(), 1);
        assertEquals(string, getStreamContentAsString(entity.getStream()));
        em.getTransaction().commit();
        em.close();
    }
    
    public void testDelete() {
        if (!isDatabaseSupported()) return;
        insert(newLobEntity("oOOOOOo", 1));
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity entity = (LobEntity) em.find(getLobEntityClass(), 1);
        em.remove(entity);
        em.getTransaction().commit();
        em.close();
        em = emf.createEntityManager();
        em.getTransaction().begin();
        Query q = em.createQuery(getSelectQuery());
        assertEquals(0, q.getResultList().size());
        em.getTransaction().commit();
        em.close();
    }
    
    public void testLifeCycleInsertFlushModify() {
        if (!isDatabaseSupported()) return;
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity le = newLobEntity("oOOOOOo", 1);
        em.persist(le);
        em.flush();
        changeStream(le, "iIIIIIi");
        em.getTransaction().commit();
        em.close();
    }

    public void testLifeCycleLoadFlushModifyFlush() {
        if (!isDatabaseSupported()) return;
        insert(newLobEntity("oOOOOOo", 1));
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity entity = (LobEntity) em.find(getLobEntityClass(), 1);
        em.flush();
        changeStream(entity, "iIIIIIi");
        em.flush();
        em.getTransaction().commit();
        em.close();
    }

    public void testReadingMultipleTimesWithASingleConnection()
        throws IOException {
        if (!isDatabaseSupported()) return;
        insert(newLobEntity("oOOOOOo", 1));
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity le = (LobEntity) em.find(getLobEntityClass(), 1);
        String string = "iIIIIIi";
        changeStream(le, string);
        em.getTransaction().commit();
        em.close();
        em = emf.createEntityManager();
        em.getTransaction().begin();
        le = (LobEntity) em.find(getLobEntityClass(), 1);
        assertNotNull(le.getStream());
        LobEntity entity = newLobEntity("oOOOOOo", 2);
        em.persist(entity);
        assertEquals(string, getStreamContentAsString(le.getStream()));
        em.getTransaction().commit();
        em.close();
    }

    public void testDataCache() {
        if (!isDatabaseSupported()) return;
        OpenJPAEntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
        LobEntity le = newLobEntity("oOOOOOo", 1);
        em.persist(le);
        em.getTransaction().commit();
        OpenJPAConfiguration conf = emf.getConfiguration();
        Object o = em.getObjectId(le);
        ClassMetaData meta = JPAFacadeHelper.getMetaData(le);
        Object objectId = JPAFacadeHelper.toOpenJPAObjectId(meta, o);
        DataCachePCData pcd =
            conf.getDataCacheManagerInstance()
                .getSystemDataCache().get(objectId);
        assertFalse(pcd.isLoaded(meta.getField("stream").getIndex()));
        em.close();
    }

    public void testSetResetAndFlush() throws IOException {
        if (!isDatabaseSupported()) return;
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity le = newLobEntity("oOOOOOo", 1);
        em.persist(le);
        changeStream(le, "iIIIIIi");
        em.flush();
        em.getTransaction().commit();
        em.close();
        em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity entity = (LobEntity) em.find(getLobEntityClass(), 1);
        assertEquals("iIIIIIi", getStreamContentAsString(entity.getStream()));
        em.getTransaction().commit();
        em.close();
    }

    public void testSetFlushAndReset() throws IOException {
        if (!isDatabaseSupported()) return;
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        LobEntity le = newLobEntity("oOOOOOo", 1);
        em.persist(le);
        em.flush();
        changeStream(le, "iIIIIIi");
        LobEntity entity = (LobEntity) em.find(getLobEntityClass(), 1);
        assertEquals("iIIIIIi", getStreamContentAsString(entity.getStream()));
        em.getTransaction().commit();
        em.close();
    }

    protected abstract Class getLobEntityClass();

    protected abstract String getStreamContentAsString(Object o)
        throws IOException;

    protected abstract LobEntity newLobEntity(String s, int id);

    protected abstract LobEntity newLobEntityForLoadContent(String s, int id);

    protected abstract String getSelectQuery();

    protected abstract void changeStream(LobEntity le, String s);
}
