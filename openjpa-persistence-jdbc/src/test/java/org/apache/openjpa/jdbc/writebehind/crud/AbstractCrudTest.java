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
package org.apache.openjpa.jdbc.writebehind.crud;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.openjpa.jdbc.writebehind.AbstractWriteBehindTestCase;
import org.apache.openjpa.jdbc.writebehind.entities.SimpleEntity;

public abstract class AbstractCrudTest extends AbstractWriteBehindTestCase {
    protected List<Integer> _ids;
    protected static int SLEEP_TIME = 6000;
    protected static int NUM_INSERTS = 5;
    protected static String[] NAMES = { "Able", "Baker", "Charlie" };

    /**
     * populate a set of SimpleEntities appropriate for this test.
     */
    protected void populate() {
        _ids = new ArrayList<Integer>();
        EntityManager em = _validatorEMF.createEntityManager();
        em.getTransaction().begin();
        SimpleEntity se = null;
        for (int i = 0; i < NUM_INSERTS; i++) {
            se = newEntityInstance();
            if (!idIsGenerated()) {
                se.setId(i + 1);
            }
            se.setForename(NAMES[i % NAMES.length]);
            se.setSurname(NAMES[(i + 1) % NAMES.length]);
            em.persist(se);
            em.flush();
            _ids.add(se.getId());
        }
        em.getTransaction().commit();
    }
    
    protected void cleanup() { 
        EntityManager vem = getValidatorEMF().createEntityManager();
        vem.getTransaction().begin();

        // TODO replace with getTableName();
        vem.createNativeQuery("Delete from " + getEntityTypeName())
            .executeUpdate();
        vem.getTransaction().commit();
        
        // clear l2 cache since the native query is outside of the wb cache.
        emf.getStoreCache().evictAll();
    }

    /**
     * Return the class of entities used by this testcase
     * @return
     */
    protected abstract Class<? extends SimpleEntity> getEntityType();

    /**
     * Get the type name for this entity could be different from the alias
     * 
     * @return
     */
    protected String getEntityTypeName() {
        return getEntityType().getSimpleName();
    }

    /**
     * Obtain a new instance of the entity used by this test
     * @return
     */
    protected SimpleEntity newEntityInstance() {
        try {
            return getEntityType().newInstance();
        } catch (IllegalAccessException e) {
            return null;
        } catch (InstantiationException e) {
            return null;
        }
    }

    /**
     * Indicate whether the entities used by this test have generated ids
     * 
     * @return true if the entity ID is generated, otherwise false
     */
    protected abstract boolean idIsGenerated();

    // Begin asserts
    /**
     * Assert that an entity "found" by a given EntityManager matches one
     * provided.
     * 
     * @param em
     *            The entity manager which will be used to find an entity.
     * @param se
     *            An entity instance (usually unmanaged) which is contains the
     *            expected state of the entity.
     * @throws AssertionFailedError
     *             an entity matching se's ID cannot be found or if any of the
     *             non-version fields do not match
     */
    public void assertEntityContents(EntityManager em, SimpleEntity se) {
        SimpleEntity found = em.find(getEntityType(), se.getId());
        assertNotNull(String.format("%s::%d should be found",
            getEntityTypeName(), 1), found);
        assertTrue(found instanceof SimpleEntity);
        if (se.getForename() != null && se.getForename().length() != 0) {
            assertEquals(se.getForename(), found.getForename());
        }
        if (se.getSurname() != null && se.getSurname().length() != 0) {
            assertEquals(se.getSurname(), found.getSurname());
        }
    }

    /**
     * Assert that all entities can not be found in the database. No L2 cache or
     * write behind cache is used.
     */
    public void assertEntitiesDeleted() {
        EntityManager validatorEM = getValidatorEMF().createEntityManager();
        SimpleEntity se = null;
        for (int i = 0; i < NUM_INSERTS; i++) {
            se = validatorEM.find(getEntityType(), _ids.get(i));
            assertNull(String.format("%s::%d should have been deleted",
                getEntityTypeName(),_ids.get(i)), se);
        }
        validatorEM.close();
    }

    /**
     * Assert that all entities can be found in the database. No L2 cache or
     * write behind cache is used.
     */
    public void assertEntitiesExist() {
        EntityManager validatorEM = getValidatorEMF().createEntityManager();
        SimpleEntity se = null;
        for (int i = 0; i < NUM_INSERTS; i++) {
            se = validatorEM.find(getEntityType(), _ids.get(i));
            assertNotNull(String.format("%s::%d should exist in the database",
                getEntityTypeName(), _ids.get(i)), se);
        }
        validatorEM.close();
    }

    /**
     * Assert that all entities can not be found in the database and the name
     * field has been updated . No L2 cache or write behind cache is used.
     */
    public void assertEntitiesUpdated() {
        EntityManager validatorEM = getValidatorEMF().createEntityManager();
        SimpleEntity se = null;
        for (int i = 0; i < NUM_INSERTS; i++) {
            se = newEntityInstance();
            se.setId(_ids.get(i));
            se.setForename(NAMES[i % NAMES.length] + " UPDATED");
            se.setSurname(NAMES[(i + 1) % NAMES.length] + " UPDATED");
            assertEntityContents(validatorEM, se);
        }
        validatorEM.close();
    }

    /**
     * Assert that all entities can not be found in the database and the name
     * fields are unmodified. No L2 cache or write behind cache is used.
     */
    public void assertEntitiesUnmodified() {
        EntityManager validatorEM = getValidatorEMF().createEntityManager();
        SimpleEntity se = null;
        for (int i = 0; i < NUM_INSERTS; i++) {
            se = newEntityInstance();
            se.setId(_ids.get(i));
            se.setForename(NAMES[i % NAMES.length]);
            assertEntityContents(validatorEM, se);
        }
        validatorEM.close();
    }

    // begin operations

    public void deleteEntities(boolean flushBetween) {
        assertEntitiesExist();
        em.getTransaction().begin();
        SimpleEntity se = null;
        for (int i = 0; i < NUM_INSERTS; i++) {
            se = em.find(getEntityType(),_ids.get(i));
            em.remove(se);
            if (flushBetween) {
                em.flush();
            }
        }
        em.getTransaction().commit();
    }

    public void updateEntities(boolean flushBetween) {
        em.getTransaction().begin();
        SimpleEntity se = null;
        assertEquals(NUM_INSERTS, _ids.size());
        try {
            for (Integer id : _ids) {
                se = em.find(getEntityType(), id);
                assertNotNull(String.format("%s::%d should be found",
                    getEntityTypeName(), id), se);
                se.setForename(se.getForename() + " UPDATED");
                if (flushBetween) {
                    em.flush();
                }
            }
        } finally {
            em.getTransaction().commit();
        }
        
        em.getTransaction().begin();
        try {
            for (Integer id : _ids) {
                se = em.find(getEntityType(), id);
                assertNotNull(String.format("%s::%d should be found",
                    getEntityTypeName(), id), se);
                se.setSurname(se.getSurname() + " UPDATED");
                if (flushBetween) {
                    em.flush();
                }
            }
        } finally {
            em.getTransaction().commit();
        }
    }

    public void insertEntities(boolean flushBetween) {
        _ids = new ArrayList<Integer>();
        em.getTransaction().begin();
        SimpleEntity se = null;
        for (int i = 0; i < NUM_INSERTS; i++) {
            se = newEntityInstance();
            if (!idIsGenerated()) {
                se.setId(i + 1);
            }
            se.setForename(NAMES[i % NAMES.length]);
            em.persist(se);
            if (flushBetween) {
                em.flush();
            }
            _ids.add(se.getId());
        }
        em.getTransaction().commit();
    }
}
