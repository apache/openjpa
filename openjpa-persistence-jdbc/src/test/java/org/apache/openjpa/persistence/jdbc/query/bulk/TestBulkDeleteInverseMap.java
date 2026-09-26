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
 * The inverse side of a map is not a table owned by the map: its rows are
 * entity rows and a bulk delete never cascades to them, not even when the
 * <code>mappedBy</code> attribute is declared by a joined superclass of the
 * declared value type.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2990">OPENJPA-2990</A>
 */
public class TestBulkDeleteInverseMap extends AbstractBulkDeleteTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteMapOwner.class,
            BulkDeleteMapBase.class, BulkDeleteMapSub.class);
        createMapData();
        sql.clear();
    }

    public void testRelatedEntityRowsSurviveBulkDelete() {
        assertEquals(1, delete("delete from BulkDeleteMapOwner o"));

        assertEquals(0, count(BulkDeleteMapOwner.class));
        // JPA spec section 4.10: a bulk delete does not cascade to entities
        assertEquals(2, count(BulkDeleteMapSub.class));
        assertEquals(2, countRows("BULK_MAP_BASE"));
        assertEquals(2, countRows("BULK_MAP_SUB"));
        assertNotSQL("DELETE FROM .*BULK_MAP_BASE.*");
        assertNotSQL("DELETE FROM .*BULK_MAP_SUB.*");
    }

    private void createMapData() {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        BulkDeleteMapOwner owner = new BulkDeleteMapOwner();
        owner.setId(1L);
        owner.setName("owner");
        em.persist(owner);
        for (int i = 0; i < 2; i++) {
            BulkDeleteMapSub sub = new BulkDeleteMapSub();
            sub.setId(i + 1);
            sub.setName("child" + i);
            sub.setOwner(owner);
            owner.getChildren().put(sub.getName(), sub);
            em.persist(sub);
        }
        em.getTransaction().commit();
        em.close();
    }
}
