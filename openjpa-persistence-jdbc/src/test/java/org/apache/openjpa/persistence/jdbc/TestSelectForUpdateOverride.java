/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.jdbc;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.apache.openjpa.persistence.simple.AllFieldTypes;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.openjpa.persistence.FetchPlan;

public class TestSelectForUpdateOverride
    extends SQLListenerTestCase {

    public void setUp() {
        setUp(AllFieldTypes.class,
            "openjpa.Optimistic", "false",
            "openjpa.LockManager", "pessimistic",
            "openjpa.ReadLockLevel", "none");
    }

    public void testSelectForUpdateOverride() {
        EntityManager em = emf.createEntityManager();
        sql.clear();
        try {
            em.getTransaction().begin();
            OpenJPAPersistence.cast(em).getFetchPlan()
                .setReadLockMode(LockModeType.WRITE);
            em.find(AllFieldTypes.class, 0);

            assertEquals(1, sql.size());
            assertSQL(".*FOR UPDATE.*");
        } finally {
            em.getTransaction().rollback();
            em.close();
        }
    }
}
