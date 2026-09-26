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

/**
 * The rows of an owned table can only be deleted by a single key column. A
 * candidate with a composite primary key therefore falls back to the in-memory
 * path, which loads the matching instances and removes them one by one - the
 * safety valve that keeps the owned rows from dangling when the SQL cleanup
 * cannot be expressed.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2990">OPENJPA-2990</A>
 */
public class TestBulkDeleteCompositeId extends AbstractBulkDeleteTestCase {

    private static final int TAG_COUNT = 3;

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteCompositeOwner.class);
        createCompositeData();
        sql.clear();
    }

    public void testCompositeKeyCandidateIsDeletedInMemory() {
        assertEquals(1, delete(
            "delete from BulkDeleteCompositeOwner o where o.name=:n",
            "n", "owner"));

        assertEquals(1, count(BulkDeleteCompositeOwner.class));
        assertEquals(0, countRows("BULK_CID_TAGS"));
        // the in-memory path loads the candidates and removes them one by one
        assertNotSQL("DELETE FROM .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_CID_TAGS WHERE .*");
        assertSQL("DELETE FROM .*BULK_CID_OWNER WHERE .*");
    }

    /**
     * Without criteria no key is needed, so even a composite key candidate is
     * deleted in SQL and its owned table is emptied outright.
     */
    public void testCompositeKeyCandidateWithoutCriteria() {
        assertEquals(2, delete("delete from BulkDeleteCompositeOwner o"));

        assertEquals(0, count(BulkDeleteCompositeOwner.class));
        assertEquals(0, countRows("BULK_CID_TAGS"));
        assertNotSQL("DELETE FROM .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_CID_TAGS( t[0-9]+)?");
        assertSQL("DELETE FROM .*BULK_CID_OWNER( t[0-9]+)?");
    }

    private void createCompositeData() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        BulkDeleteCompositeOwner owner = new BulkDeleteCompositeOwner();
        owner.setTenant(1L);
        owner.setNumber(1L);
        owner.setName("owner");
        for (int i = 0; i < TAG_COUNT; i++)
            owner.getTags().add("tag" + i);
        em.persist(owner);

        BulkDeleteCompositeOwner other = new BulkDeleteCompositeOwner();
        other.setTenant(1L);
        other.setNumber(2L);
        other.setName("other");
        em.persist(other);
        em.getTransaction().commit();
        em.close();
    }
}
