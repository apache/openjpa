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

/**
 * A bulk <code>DELETE</code> does not cascade to related entities, but it must
 * still remove the rows of the join tables and element collection tables owned
 * by the deleted entities, otherwise those rows dangle on deleted primary keys
 * and violate foreign key constraints.
 * <p>
 * The criteria are evaluated exactly once into a list of primary keys, so the
 * shape of the criteria - a subquery, an aggregate over an owned table, a
 * correlated <code>EXISTS</code> - cannot influence the result.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2990">OPENJPA-2990</A>
 */
public class TestBulkDeleteJoinTable extends AbstractBulkDeleteTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteOwner.class, BulkDeleteDetails.class,
            BulkDeleteAddress.class, BulkDeleteItem.class);
        createData();
        sql.clear();
    }

    /**
     * A delete without any criteria matches every candidate row, so every row
     * of every owned table belongs to a deleted candidate: the keys are not
     * materialized at all and the owned tables are emptied outright.
     */
    public void testBulkDeleteWithoutCriteriaEmptiesOwnedTables() {
        assertEquals(2, delete("delete from BulkDeleteOwner o"));

        assertEquals(0, count(BulkDeleteOwner.class));
        assertOwnedTablesAreEmpty();
        assertNotSQL("SELECT DISTINCT .*FROM .*BULK_OWNER.*");
        assertNotSQL("DELETE FROM .*BULK_OWNER.* WHERE .*");
        assertSQL("DELETE FROM .*BULK_OWNER_ITEMS( t[0-9]+)?");
        assertSQL("DELETE FROM .*BULK_OWNER_NICKNAMES( t[0-9]+)?");
        assertSQL("DELETE FROM .*BULK_OWNER_LABELS( t[0-9]+)?");
        assertSQL("DELETE FROM .*BULK_OWNER_ADDRESSES( t[0-9]+)?");
        assertSQL("DELETE FROM .*BULK_OWNER_ALIASES( t[0-9]+)?");
        assertSQL("DELETE FROM .*BULK_OWNER( t[0-9]+)?");
    }

    /**
     * Nothing matches: the keys are still materialized, but no delete follows
     * and the rows of the owned tables are untouched.
     */
    public void testBulkDeleteWithoutMatches() {
        assertEquals(0, delete("delete from BulkDeleteOwner o where o.name=:n",
            "n", "nobody"));

        assertEquals(2, count(BulkDeleteOwner.class));
        assertOwnedTablesAreUntouched();
        assertSQL("SELECT DISTINCT .*FROM .*BULK_OWNER.*");
        assertNotSQL("DELETE FROM .*");
    }

    public void testSingleDeleteRemovesRowsOfOwnedTables() {
        assertEquals(1, delete("delete from BulkDeleteOwner o where o.id=:id",
            "id", 1L));

        assertEquals(1, count(BulkDeleteOwner.class));
        assertOwnedTablesAreEmpty();
        assertOwnedTableDeletes();
    }

    /**
     * An element collection declared inside an embeddable is only reachable
     * through the embedded mapping.
     */
    public void testEmbeddedElementCollectionIsCleaned() {
        assertEquals(1, delete("delete from BulkDeleteOwner o where o.name=:n",
            "n", "owner"));

        assertEquals(0, countRows("BULK_OWNER_LABELS"));
        assertSQL("DELETE FROM .*BULK_OWNER_LABELS WHERE .*IN \\(.*");
    }

    /**
     * The table of an embeddable is the table it is embedded into, so the
     * collection table of an element collection of embeddables must not be
     * mistaken for the table of a related entity.
     */
    public void testElementCollectionOfEmbeddableIsCleaned() {
        assertEquals(1, delete("delete from BulkDeleteOwner o where o.name=:n",
            "n", "owner"));

        assertEquals(0, countRows("BULK_OWNER_ADDRESSES"));
        assertEquals(0, countRows("BULK_OWNER_ALIASES"));
        assertSQL("DELETE FROM .*BULK_OWNER_ADDRESSES WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_OWNER_ALIASES WHERE .*IN \\(.*");
    }

    /**
     * <code>SIZE</code> renders as an inline scalar subquery over the element
     * collection table, so it has to be evaluated before that table is emptied.
     */
    public void testSize() {
        assertEquals(1, delete(
            "delete from BulkDeleteOwner o where size(o.nicknames)=3"));

        assertEquals(1, count(BulkDeleteOwner.class));
        assertOwnedTablesAreEmpty();
        assertOwnedTableDeletes();
    }

    public void testIsNotEmpty() {
        assertEquals(1, delete(
            "delete from BulkDeleteOwner o where o.nicknames is not empty"));

        assertEquals(1, count(BulkDeleteOwner.class));
        assertOwnedTablesAreEmpty();
        assertOwnedTableDeletes();
    }

    /**
     * Only the owner without nicknames matches, so the rows of the other owner
     * must survive.
     */
    public void testIsEmpty() {
        assertEquals(1, delete(
            "delete from BulkDeleteOwner o where o.nicknames is empty"));

        assertEquals(1, count(BulkDeleteOwner.class));
        assertOwnedTablesAreUntouched();
    }

    public void testMemberOf() {
        assertEquals(1, delete(
            "delete from BulkDeleteOwner o where 'nick1' member of o.nicknames"));

        assertEquals(1, count(BulkDeleteOwner.class));
        assertOwnedTablesAreEmpty();
        assertOwnedTableDeletes();
    }

    public void testCorrelatedExistsSubquery() {
        assertEquals(1, delete("delete from BulkDeleteOwner o where exists "
            + "(select o2 from BulkDeleteOwner o2 where o2.id=o.id "
            + "and o2.name=:n)", "n", "owner"));

        assertEquals(1, count(BulkDeleteOwner.class));
        assertOwnedTablesAreEmpty();
        assertOwnedTableDeletes();
    }

    public void testUncorrelatedInSubquery() {
        assertEquals(1, delete("delete from BulkDeleteOwner o where o.id in "
            + "(select o2.id from BulkDeleteOwner o2 where o2.name=:n)",
            "n", "owner"));

        assertEquals(1, count(BulkDeleteOwner.class));
        assertOwnedTablesAreEmpty();
        assertOwnedTableDeletes();
    }

    /**
     * Pin the chosen path: the criteria are evaluated once into a list of
     * primary keys and every table is then deleted by that key.
     */
    private void assertOwnedTableDeletes() {
        assertSQL("SELECT DISTINCT .*FROM .*BULK_OWNER.*");
        assertSQL("DELETE FROM .*BULK_OWNER_ITEMS WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_OWNER_NICKNAMES WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_OWNER_LABELS WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_OWNER_ADDRESSES WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_OWNER_ALIASES WHERE .*IN \\(.*");
        assertSQL("DELETE FROM .*BULK_OWNER WHERE .*IN \\(.*");
    }
}
