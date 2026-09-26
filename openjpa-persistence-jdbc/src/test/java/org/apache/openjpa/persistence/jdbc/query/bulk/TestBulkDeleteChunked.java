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
 * The keys of the candidates are deleted in chunks of the size the database
 * accepts in an <code>IN</code> list.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2990">OPENJPA-2990</A>
 */
public class TestBulkDeleteChunked extends AbstractBulkDeleteTestCase {

    /** Two keys per statement, so the chunking is exercised by a handful of rows. */
    private static final int CHUNK_SIZE = 2;

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteOwner.class, BulkDeleteDetails.class,
            BulkDeleteAddress.class, BulkDeleteItem.class,
            "openjpa.jdbc.DBDictionary", "inClauseLimit=" + CHUNK_SIZE);
    }

    /**
     * Five candidates do not divide by the chunk size, so the last statement
     * carries a shorter <code>IN</code> list.
     */
    public void testPartialLastChunk() {
        assertChunkedDelete(5);
    }

    /**
     * An exact multiple of the chunk size must not issue a trailing statement
     * with an empty <code>IN</code> list.
     */
    public void testExactMultipleOfChunkSize() {
        assertChunkedDelete(2 * CHUNK_SIZE);
    }

    private void assertChunkedDelete(int owners) {
        createOwners(owners);
        sql.clear();

        assertEquals(owners, delete(
            "delete from BulkDeleteOwner o where o.name like :n", "n", "own%"));

        assertEquals(0, count(BulkDeleteOwner.class));
        assertEquals(0, countRows("BULK_OWNER_NICKNAMES"));
        assertEquals(0, countRows("BULK_OWNER_ITEMS"));
        assertEquals(0, countRows("BULK_OWNER_ADDRESSES"));
        assertSQL("DELETE FROM .*BULK_OWNER_NICKNAMES WHERE .*IN \\(.*");
        // one statement per chunk, and no trailing statement for an exact
        // multiple of the chunk size
        int chunks = (owners + CHUNK_SIZE - 1) / CHUNK_SIZE;
        assertEquals(chunks,
            countSQL("DELETE FROM .*BULK_OWNER WHERE .*IN \\(.*"));
        assertEquals(chunks,
            countSQL("DELETE FROM .*BULK_OWNER_NICKNAMES WHERE .*IN \\(.*"));
    }

    private int countSQL(String regex) {
        int hits = 0;
        for (String statement : sql)
            if (statement.matches(regex))
                hits++;
        return hits;
    }

    private void createOwners(int owners) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (int i = 0; i < owners; i++) {
            BulkDeleteOwner owner = new BulkDeleteOwner();
            owner.setId(i + 1);
            owner.setName("owner" + i);
            BulkDeleteItem item = new BulkDeleteItem();
            item.setId(i + 1);
            item.setName("item" + i);
            em.persist(item);
            owner.getItems().add(item);
            owner.getNicknames().add("nick" + i);
            owner.getAddresses().add(new BulkDeleteAddress("city" + i));
            em.persist(owner);
        }
        em.getTransaction().commit();
        em.close();
    }
}
