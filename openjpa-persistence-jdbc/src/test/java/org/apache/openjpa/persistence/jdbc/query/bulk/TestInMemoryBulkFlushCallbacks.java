/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.jdbc.query.bulk;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * The remove callbacks of a candidate are one of the reasons a bulk delete is
 * executed in memory - <code>JDBCStoreQuery.executeDelete()</code> gives up on
 * the single statement so the callbacks are invoked at all. The flush that
 * follows the in-memory operation therefore also decides when
 * <code>@PostRemove</code> fires: inside <code>executeUpdate()</code> by
 * default and at commit when the flush is suppressed.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2951">OPENJPA-2951</A>
 */
public class TestInMemoryBulkFlushCallbacks extends SQLListenerTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkCallbackEntity.class);
    }

    private void createData(OpenJPAEntityManagerFactorySPI factory) {
        try (EntityManager em = factory.createEntityManager()) {
            em.getTransaction().begin();
            BulkCallbackEntity e = new BulkCallbackEntity();
            e.setId(1L);
            e.setName("name");
            em.persist(e);
            em.getTransaction().commit();
        }
        BulkCallbackEntity.CALLBACKS.clear();
        sql.clear();
    }

    /**
     * By default the trailing flush runs inside
     * <code>executeUpdate()</code>, so both callbacks have fired by the time
     * it returns.
     */
    public void testPostRemoveFiresDuringExecuteUpdate() {
        createData(emf);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            assertEquals(1, em.createQuery(
                "delete from BulkCallbackEntity e where e.name='name'")
                .executeUpdate());
            assertEquals("the callbacks that fired during executeUpdate()",
                "[preRemove, postRemove]",
                BulkCallbackEntity.CALLBACKS.toString());
            em.getTransaction().commit();
        }
        assertEquals("[preRemove, postRemove]",
            BulkCallbackEntity.CALLBACKS.toString());
    }

    /**
     * With the flush suppressed only <code>@PreRemove</code> has fired when
     * <code>executeUpdate()</code> returns - <code>@PostRemove</code> waits
     * for the commit, which is the reordering the compatibility option brings
     * back.
     */
    public void testPostRemoveFiresAtCommitWithoutTheFlush() {
        OpenJPAEntityManagerFactorySPI factory = createEMF(CLEAR_TABLES,
            BulkCallbackEntity.class, "openjpa.Compatibility",
            "FlushAfterInMemoryBulkOperation=false");
        try {
            createData(factory);
            try (EntityManager em = factory.createEntityManager()) {
                em.getTransaction().begin();
                assertEquals(1, em.createQuery(
                    "delete from BulkCallbackEntity e where e.name='name'")
                    .executeUpdate());
                assertEquals("the callbacks that fired during executeUpdate()",
                    "[preRemove]",
                    BulkCallbackEntity.CALLBACKS.toString());
                em.getTransaction().commit();
            }
            assertEquals("[preRemove, postRemove]",
                BulkCallbackEntity.CALLBACKS.toString());
        } finally {
            closeEMF(factory);
        }
    }
}
