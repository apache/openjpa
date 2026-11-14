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
package org.apache.openjpa.kernel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.enhance.ManagedClassSubclasser;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.enhance.UnenhancedFieldAccess;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.query.SimpleEntity;
import org.apache.openjpa.persistence.test.AbstractCachedEMFTestCase;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@AllowFailure(message = "excluded")
class TestDynamicClassRegistration
    extends AbstractCachedEMFTestCase {

    private OpenJPAEntityManagerFactorySPI emf1;

    @BeforeEach
    void setUp() throws Exception {
        emf1 = createNamedEMF("empty-pu");
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        clear(emf1);
        closeEMF(emf1);
    }

    @Test
    void enhancedDynamicClassRegistration()
        throws Exception {
        assertTrue(
            PersistenceCapable.class.isAssignableFrom(SimpleEntity.class));

        // trigger class initialization. We could just do 'new SimpleEntity()'.
        Class.forName(SimpleEntity.class.getName(), true,
            getClass().getClassLoader());

        ClassMetaData meta =
            JPAFacadeHelper.getMetaData(emf1, SimpleEntity.class);
        assertNotNull(meta);
        EntityManager em = emf1.createEntityManager();
        jakarta.persistence.Query q = em.createQuery("select o from simple o");
        em.close();
    }

    @Test
    void unenhancedDynamicClassRegistration() {
        assertFalse(PersistenceCapable.class.isAssignableFrom(
            UnenhancedFieldAccess.class));

        // trigger class initialization
        ManagedClassSubclasser.prepareUnenhancedClasses(
            emf1.getConfiguration(),
            Collections.singleton(UnenhancedFieldAccess.class),
            null);

        ClassMetaData meta =
            JPAFacadeHelper.getMetaData(emf1, UnenhancedFieldAccess.class);
        assertNotNull(meta);
        EntityManager em = emf1.createEntityManager();
        jakarta.persistence.Query q = em.createQuery(
            "select o from UnenhancedFieldAccess o");
        em.close();
    }
}
