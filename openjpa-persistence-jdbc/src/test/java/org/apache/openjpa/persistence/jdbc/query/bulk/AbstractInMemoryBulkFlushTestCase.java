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

import java.util.Collections;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * Data and helpers shared by the tests of the context flush that follows a
 * bulk operation which had to be executed in memory.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2951">OPENJPA-2951</A>
 */
public abstract class AbstractInMemoryBulkFlushTestCase extends SQLListenerTestCase {

    protected static final String DELETE_IN_MEMORY =
        "delete from BulkDeleteCompositeOwner o where o.name=:name";

    protected static final String UPDATE_ALL =
        "update BulkDeleteCompositeOwner o set o.name='changed'";

    /**
     * An owner with a composite primary key, whose owned collection table
     * cannot be deleted by a single key column, so a filtered bulk delete
     * falls back to the in-memory path, plus an unrelated entity that is not
     * on the access path of those queries.
     */
    protected void createData() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            BulkDeleteCompositeOwner owner = new BulkDeleteCompositeOwner();
            owner.setTenant(1L);
            owner.setNumber(1L);
            owner.setName("owner");
            owner.getTags().add("tag0");
            em.persist(owner);

            BulkDeleteItem unrelated = new BulkDeleteItem();
            unrelated.setId(1L);
            unrelated.setName("item");
            em.persist(unrelated);
            em.getTransaction().commit();
        }
    }

    protected Object ownerId() {
        return new BulkDeleteCompositeId(1L, 1L);
    }

    /**
     * Execute the bulk update in memory by handing the candidates to the
     * query, which is the only way to reach the in-memory update path on a
     * database that can express the bulk statement itself.
     */
    protected int updateInMemory(EntityManager em) {
        Object candidate = em.find(BulkDeleteCompositeOwner.class, ownerId());
        OpenJPAQuery<?> q = OpenJPAPersistence.cast(em).createQuery(UPDATE_ALL);
        q.setCandidateCollection(Collections.singleton(candidate));
        return q.executeUpdate();
    }

    protected int deleteInMemory(EntityManager em) {
        return em.createQuery(DELETE_IN_MEMORY)
            .setParameter("name", "owner").executeUpdate();
    }

    protected String ownerName() {
        try (EntityManager em = emf.createEntityManager()) {
            BulkDeleteCompositeOwner owner =
                em.find(BulkDeleteCompositeOwner.class, ownerId());
            return owner == null ? null : owner.getName();
        }
    }

    protected String unrelatedName() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(BulkDeleteItem.class, 1L).getName();
        }
    }
}
