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
 * Data and helpers shared by the bulk delete tests.
 */
public abstract class AbstractBulkDeleteTestCase extends SQLListenerTestCase {

    protected static final int ITEM_COUNT = 3;
    protected static final int NICKNAME_COUNT = 3;
    protected static final int LABEL_COUNT = 2;
    protected static final int ADDRESS_COUNT = 3;
    protected static final int ALIAS_COUNT = 2;

    /**
     * Owner <code>1</code> owns rows in every owned table, owner
     * <code>2</code> owns none.
     */
    protected void createData() {
        try (EntityManager em = emf.createEntityManager()) {
	        em.getTransaction().begin();
	        BulkDeleteOwner owner = new BulkDeleteOwner();
	        owner.setId(1L);
	        owner.setName("owner");
	        for (int i = 0; i < ITEM_COUNT; i++) {
	            BulkDeleteItem item = new BulkDeleteItem();
	            item.setId(i + 1);
	            item.setName("item" + i);
	            em.persist(item);
	            owner.getItems().add(item);
	            owner.getNicknames().add("nick" + i);
	            owner.getAddresses().add(new BulkDeleteAddress("city" + i));
	        }
	        for (int i = 0; i < ALIAS_COUNT; i++)
	            owner.getAliases().put("alias" + i, new BulkDeleteAddress("a" + i));
	        for (int i = 0; i < LABEL_COUNT; i++)
	            owner.getDetails().getLabels().add("label" + i);
	        owner.getDetails().setNote("note");
	        em.persist(owner);
	
	        BulkDeleteOwner empty = new BulkDeleteOwner();
	        empty.setId(2L);
	        empty.setName("other");
	        em.persist(empty);
	        em.getTransaction().commit();
        }
    }

    protected int countRows(String table) {
        try  (EntityManager em = emf.createEntityManager()) {
            Object count = em.createNativeQuery("SELECT COUNT(*) FROM " + table)
                .getSingleResult();
            return ((Number) count).intValue();
        }
    }

    /**
     * No row of any table owned by <code>BulkDeleteOwner</code> is left, but
     * the related entities are untouched.
     */
    protected void assertOwnedTablesAreEmpty() {
        // JPA spec section 4.10: a bulk delete does not cascade to entities
        assertEquals(ITEM_COUNT, count(BulkDeleteItem.class));
        assertEquals(0, countRows("BULK_OWNER_ITEMS"));
        assertEquals(0, countRows("BULK_OWNER_NICKNAMES"));
        assertEquals(0, countRows("BULK_OWNER_LABELS"));
        assertEquals(0, countRows("BULK_OWNER_ADDRESSES"));
        assertEquals(0, countRows("BULK_OWNER_ALIASES"));
    }

    protected void assertOwnedTablesAreUntouched() {
        assertEquals(ITEM_COUNT, count(BulkDeleteItem.class));
        assertEquals(ITEM_COUNT, countRows("BULK_OWNER_ITEMS"));
        assertEquals(NICKNAME_COUNT, countRows("BULK_OWNER_NICKNAMES"));
        assertEquals(LABEL_COUNT, countRows("BULK_OWNER_LABELS"));
        assertEquals(ADDRESS_COUNT, countRows("BULK_OWNER_ADDRESSES"));
        assertEquals(ALIAS_COUNT, countRows("BULK_OWNER_ALIASES"));
    }

    /**
     * Execute the given bulk delete and return the number of deleted rows.
     */
    protected int delete(String jpql, Object... params) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            jakarta.persistence.Query q = em.createQuery(jpql);
            for (int i = 0; i < params.length; i += 2)
                q.setParameter((String) params[i], params[i + 1]);
            int deleted = q.executeUpdate();
            em.getTransaction().commit();
            return deleted;
        }
    }
}
