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
 * A candidate with cascade-delete relations is bulk deleted in SQL, whether
 * the relation is marked with OpenJPA's own <code>@Dependent</code> or carries
 * <code>CascadeType.REMOVE</code>.
 * <p>
 * A single-valued relation of either kind used to force the whole delete onto
 * the in-memory path: the removed guard rejected a field whose <em>field</em>
 * value had a cascade delete, which is what <code>@Dependent</code>
 * (<code>CASCADE_AUTO</code>) and a to-one <code>CascadeType.REMOVE</code>
 * (<code>CASCADE_IMMEDIATE</code>) set. A collection never triggered the guard
 * itself - <code>@ElementDependent</code> and the JPA cascades of a
 * <code>@OneToMany</code> set the cascade on the element value, which the
 * guard did not read - but it was carried onto the in-memory path along with
 * the rest of the candidate, and that path cleaned up the join table and the
 * element collection table as a side effect of removing every instance.
 * <p>
 * Those rows are not entities: they are removed by the bulk delete itself. The
 * rows of the related entities are not, because a bulk delete does not cascade
 * (specification section 4.10).
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2965">OPENJPA-2965</A>
 */
public class TestBulkDeleteDependentFields extends AbstractBulkDeleteTestCase {

    private static final int PART_COUNT = 3;
    private static final int NOTE_COUNT = 2;
    private static final int TAG_COUNT = 2;
    // the parts plus the profile and the category
    private static final int CHILD_COUNT = PART_COUNT + 2;

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteDependentOwner.class,
            BulkDeleteDependentChild.class, BulkDeleteDependentNote.class);
        createDependentData();
        sql.clear();
    }

    private void createDependentData() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            BulkDeleteDependentOwner owner = new BulkDeleteDependentOwner();
            owner.setId(1L);
            owner.setName("owner");

            BulkDeleteDependentChild profile = new BulkDeleteDependentChild();
            profile.setId(100L);
            profile.setName("profile");
            em.persist(profile);
            owner.setProfile(profile);

            BulkDeleteDependentChild category = new BulkDeleteDependentChild();
            category.setId(101L);
            category.setName("category");
            em.persist(category);
            owner.setCategory(category);

            for (int i = 0; i < PART_COUNT; i++) {
                BulkDeleteDependentChild part = new BulkDeleteDependentChild();
                part.setId(i + 1);
                part.setName("part" + i);
                em.persist(part);
                owner.getParts().add(part);
            }
            for (int i = 0; i < TAG_COUNT; i++)
                owner.getTags().add("tag" + i);
            em.persist(owner);

            for (int i = 0; i < NOTE_COUNT; i++) {
                BulkDeleteDependentNote note = new BulkDeleteDependentNote();
                note.setId(i + 1);
                note.setText("note" + i);
                note.setOwner(owner);
                owner.getNotes().add(note);
                em.persist(note);
            }
            em.getTransaction().commit();
        }
    }

    /**
     * The cascade-delete fields do not prevent the delete from being executed
     * as a bulk statement, and the rows of the tables the candidate owns go
     * with it: the join table of the dependent collection and the element
     * collection table.
     */
    public void testFilteredDeleteCleansOwnedTablesOfDependentFields() {
        assertEquals(1, delete(
            "delete from BulkDeleteDependentOwner o where o.name=:n",
            "n", "owner"));

        assertEquals(0, count(BulkDeleteDependentOwner.class));
        assertEquals(0, countRows("BULK_DEP_OWNER_PARTS"));
        assertEquals(0, countRows("BULK_DEP_OWNER_TAGS"));
        assertDependentEntitiesSurvive();

        assertSQL("SELECT DISTINCT .*FROM .*BULK_DEP_OWNER.*");
        assertSQL("DELETE FROM .*BULK_DEP_OWNER_PARTS WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_DEP_OWNER_TAGS WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_DEP_OWNER WHERE .*IN \\(.*");
        // the in-memory path would delete the candidates one by one
        assertNotSQL("DELETE FROM .*BULK_DEP_OWNER WHERE .*= \\?");
    }

    /**
     * A delete without criteria empties the owned tables outright and still
     * leaves the dependent entities alone.
     */
    public void testUnfilteredDeleteCleansOwnedTablesOfDependentFields() {
        assertEquals(1, delete("delete from BulkDeleteDependentOwner o"));

        assertEquals(0, count(BulkDeleteDependentOwner.class));
        assertEquals(0, countRows("BULK_DEP_OWNER_PARTS"));
        assertEquals(0, countRows("BULK_DEP_OWNER_TAGS"));
        assertDependentEntitiesSurvive();

        assertNotSQL("SELECT DISTINCT .*FROM .*BULK_DEP_OWNER.*");
        assertSQL("DELETE( t[0-9]+)? FROM .*BULK_DEP_OWNER_PARTS( t[0-9]+)?");
        assertSQL("DELETE( t[0-9]+)? FROM .*BULK_DEP_OWNER_TAGS( t[0-9]+)?");
        assertSQL("DELETE( t[0-9]+)? FROM .*BULK_DEP_OWNER( t[0-9]+)?");
    }

    /**
     * The rows of the related entities are not touched: neither the target of
     * the direct dependent relation, nor the target of the direct
     * <code>CascadeType.REMOVE</code> relation, nor the elements of the
     * dependent join table collection, nor the notes, whose foreign key lives
     * in a table the candidate does not own.
     */
    private void assertDependentEntitiesSurvive() {
        assertEquals(CHILD_COUNT, count(BulkDeleteDependentChild.class));
        assertEquals(NOTE_COUNT, count(BulkDeleteDependentNote.class));
        assertNotSQL("DELETE FROM .*BULK_DEP_CHILD.*");
        assertNotSQL("DELETE FROM .*BULK_DEP_NOTE.*");
    }
}
