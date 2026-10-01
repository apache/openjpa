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

import org.apache.openjpa.persistence.test.SQLListenerTestCase;

/**
 * A collection table that is shared by more than one mapping and discriminated
 * by a constant join column is not owned outright by any of them: the cleanup
 * must never empty it, no matter whether the delete carries criteria or not.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2990">OPENJPA-2990</A>
 */
public class TestBulkDeleteSharedCollectionTable extends SQLListenerTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteSharedA.class,
            BulkDeleteSharedB.class);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        BulkDeleteSharedA a = new BulkDeleteSharedA();
        a.setId(1L);
        a.setName("a");
        a.getValues().add("a0");
        a.getValues().add("a1");
        em.persist(a);
        BulkDeleteSharedB b = new BulkDeleteSharedB();
        b.setId(1L);
        b.setName("b");
        b.getValues().add("b0");
        em.persist(b);
        em.getTransaction().commit();
        em.close();
        sql.clear();
    }

    /**
     * A delete without criteria matches every candidate of its own type, but
     * the rows of the other mapping share the table and must survive.
     */
    public void testDeleteWithoutCriteriaKeepsRowsOfTheOtherMapping() {
        assertSharedTableIsCleanedUp("delete from BulkDeleteSharedA a");
    }

    public void testDeleteWithCriteriaKeepsRowsOfTheOtherMapping() {
        assertSharedTableIsCleanedUp(
            "delete from BulkDeleteSharedA a where a.name='a'");
    }

    private void assertSharedTableIsCleanedUp(String jpql) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            assertEquals(1, em.createQuery(jpql).executeUpdate());
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        assertEquals(0, count(BulkDeleteSharedA.class));
        assertEquals(1, count(BulkDeleteSharedB.class));
        assertEquals(1, countRows("BULK_SHARED_COLL"));
        assertNotSQL("DELETE FROM .*BULK_SHARED_COLL( t[0-9]+)?");
    }

    private int countRows(String table) {
        EntityManager em = emf.createEntityManager();
        try {
            Object count = em.createNativeQuery("SELECT COUNT(*) FROM " + table)
                .getSingleResult();
            return ((Number) count).intValue();
        } finally {
            em.close();
        }
    }
}
