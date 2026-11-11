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
package org.apache.openjpa.persistence.callbacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.persistence.Query;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestMessageListeners extends SingleEMFTestCase {

    @BeforeEach
    public void setUp() {
        setUp(DROP_TABLES);
    }

    @Override
    protected String getPersistenceUnitName() {
        return "listener-pu";
    }

    @Test
    void updateInPrePersist() {
        // Create a new EntityManager from the EntityManagerFactory. The
        // EntityManager is the main object in the persistence API, and is
        // used to create, delete, and query objects, as well as access
        // the current transaction
        OpenJPAEntityManager em = emf.createEntityManager();
        try {
            // Begin a new local transaction so that we can persist a new entity
            em.getTransaction().begin();

            MessageListenerImpl.resetCounters();

            // Create and persist a new Message entity
            Message message = new Message("Hello Persistence!");
            assertNull(message
                .getCreated(), "Test message's created field to be null.");
            assertNull(message
                .getUpdated(), "Test message's updated field to be null.");

            em.persist(message);

            // Pre-persist invoked, created and updated fields set
            assertStatus(1, 0, 0, 0, 0, 0, 0);
            assertNotNull(message
                .getCreated(), "Test message's created field being set.");
            assertNotNull(message
                .getUpdated(), "Test message's updated field being set.");

            em.flush();
            // Post-persist invoked
            assertStatus(1, 1, 0, 0, 0, 0, 0);

            em.clear();

            // Perform a simple query to get the Message
            Query q = em.createQuery("select m from Message m where m.id="
                + message.getId());
            Message m = (Message) q.getSingleResult();

            assertEquals("Hello Persistence!", m.getMessage(),
                "Test first expected message.");
            assertNotNull(m
                .getCreated(), "Test message's created field being set.");
            assertNotNull(m
                .getUpdated(), "Test message's updated field being set.");

            // query trigger a load because em is cleared.
            assertStatus(1, 1, 0, 0, 0, 0, 1);

            em.getTransaction().commit();

            // since data is flushed, commit data with no event fired.
            assertStatus(1, 1, 0, 0, 0, 0, 1);
        } finally {
            if (em != null && em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em != null && em.isOpen())
                em.close();
        }
    }

    @Test
    void updateInPreUpdate() {
        // Create a new EntityManager from the EntityManagerFactory. The
        // EntityManager is the main object in the persistence API, and is
        // used to create, delete, and query objects, as well as access
        // the current transaction
        OpenJPAEntityManager em = emf.createEntityManager();
        try {
            // Begin a new local transaction so that we can persist a new entity
            em.getTransaction().begin();

            MessageListenerImpl.resetCounters();

            // Create and persist a new Message entity
            Message message = new Message("Hello Persistence!");
            assertNull(message
                .getCreated(), "Test message's created field to be null.");
            assertNull(message
                .getUpdated(), "Test message's updated field to be null.");

            em.persist(message);

            // Pre-persist invoked, created and updated fields set
            assertStatus(1, 0, 0, 0, 0, 0, 0);
            assertNotNull(message
                .getCreated(), "Test message's created field being set.");
            assertNotNull(message
                .getUpdated(), "Test message's updated field being set.");

            // Perform a simple query to get the Message
            Query q = em.createQuery("select m from Message m where m.id="
                + message.getId());
            Message m = (Message) q.getSingleResult();
            assertEquals("Hello Persistence!", m.getMessage(),
                "Test first expected message.");
            assertNotNull(m
                .getCreated(), "Test message's created field being set.");
            assertNotNull(m
                .getUpdated(), "Test message's updated field being set.");

            // Query cause flush to occur, hence fire the postPersist event
            assertStatus(1, 1, 0, 0, 0, 0, 0);

            // Create and persist another new Message entity
            message = new Message("Hello Persistence 2!");
            assertNull(message
                .getCreated(), "Test message's created field to be null.");
            assertNull(message
                .getUpdated(), "Test message's updated field to be null.");

            em.persist(message);

            // Pre-persist invoked, created and updated fields set
            assertStatus(2, 1, 0, 0, 0, 0, 0);
            assertNotNull(message
                .getCreated(), "Test message's created field being set.");
            assertNotNull(message
                .getUpdated(), "Test message's updated field being set.");

            em.getTransaction().commit();

            // Complete the 2nd @postPersist
            assertStatus(2, 2, 0, 0, 0, 0, 0);

            // Make an update to trigger the pre/postUpdater callbacks
            em.getTransaction().begin();
            message = em.find(Message.class,message.getId());
            message.setMessage("Update field and trigger pre/postUpdate");
            em.getTransaction().commit();

            // Complete the 2nd @postPersist
            assertStatus(2, 2, 1, 1, 0, 0, 0);

        } finally {
            if (em != null && em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em != null && em.isOpen())
                em.close();
        }
    }

    private void assertStatus(int prePersist, int postPersist, int preUpdate,
        int postUpdate, int preRemove, int postRemove, int postLoad) {
        assertEquals(prePersist, MessageListenerImpl.prePersistCount);
        assertEquals(postPersist, MessageListenerImpl.postPersistCount);
        assertEquals(preUpdate, MessageListenerImpl.preUpdateCount);
        assertEquals(postUpdate, MessageListenerImpl.postUpdateCount);
        assertEquals(preRemove, MessageListenerImpl.preRemoveCount);
        assertEquals(postRemove, MessageListenerImpl.postRemoveCount);
        assertEquals(postLoad, MessageListenerImpl.postLoadCount);
    }
}
