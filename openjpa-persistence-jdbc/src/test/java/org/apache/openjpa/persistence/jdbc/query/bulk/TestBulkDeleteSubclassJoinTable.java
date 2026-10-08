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
 * The tables owned by a subclass of a single table hierarchy have to be cleaned
 * as well when the hierarchy is bulk deleted through its base class.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2990">OPENJPA-2990</A>
 */
public class TestBulkDeleteSubclassJoinTable extends AbstractBulkDeleteTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteBase.class, BulkDeleteSub.class,
            BulkDeleteItem.class);
        createHierarchy();
        sql.clear();
    }

    /**
     * The whole hierarchy is deleted without criteria, so the owned tables are
     * emptied outright.
     */
    public void testBulkDeleteThroughBaseClass() {
        assertEquals(2, delete("delete from BulkDeleteBase b"));

        assertEquals(0, count(BulkDeleteBase.class));
        assertEquals(ITEM_COUNT, count(BulkDeleteItem.class));
        assertEquals(0, countRows("BULK_SUB_TAGS"));
        assertEquals(0, countRows("SUBLINK"));
        assertNotSQL("SELECT DISTINCT .*FROM .*BULK_BASE.*");
        assertSQL("DELETE( t[0-9]+)? FROM .*BULK_SUB_TAGS( t[0-9]+)?");
        assertSQL("DELETE( t[0-9]+)? FROM .*SUBLINK( t[0-9]+)?");
        assertSQL("DELETE( t[0-9]+)? FROM .*BULK_BASE( t[0-9]+)?");
    }

    /**
     * A delete of a subclass of a single table hierarchy carries a
     * discriminator condition, so it must not empty the owned tables outright:
     * the keys of the matching rows are materialized and the rows of the base
     * class survive.
     */
    public void testBulkDeleteOfSubclassKeepsBaseRows() {
        assertEquals(1, delete("delete from BulkDeleteSub s"));

        assertEquals(1, count(BulkDeleteBase.class));
        assertEquals(1, countRows("BULK_BASE"));
        assertEquals(ITEM_COUNT, count(BulkDeleteItem.class));
        assertEquals(0, countRows("BULK_SUB_TAGS"));
        assertEquals(0, countRows("SUBLINK"));
        assertSQL("SELECT DISTINCT .*FROM .*BULK_BASE.*");
        assertSQL("DELETE FROM .*BULK_SUB_TAGS WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*SUBLINK WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_BASE WHERE .*IN \\(.*");
    }

    private void createHierarchy() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        BulkDeleteBase base = new BulkDeleteBase();
        base.setId(1L);
        base.setName("base");
        em.persist(base);

        BulkDeleteSub sub = new BulkDeleteSub();
        sub.setId(2L);
        sub.setName("sub");
        for (int i = 0; i < ITEM_COUNT; i++) {
            BulkDeleteItem item = new BulkDeleteItem();
            item.setId(i + 1);
            item.setName("item" + i);
            em.persist(item);
            sub.getItems().add(item);
            sub.getTags().add("tag" + i);
        }
        em.persist(sub);
        em.getTransaction().commit();
        em.close();
    }
}
