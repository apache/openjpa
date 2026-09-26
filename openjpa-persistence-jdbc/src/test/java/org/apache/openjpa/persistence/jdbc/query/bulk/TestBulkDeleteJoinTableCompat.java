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
 * The compatibility option <code>CleanupOwnedTablesOnBulkDelete=false</code>
 * suppresses the cleanup of the owned tables and leaves their rows behind.
 *
 * @see <A HREF="https://issues.apache.org/jira/browse/OPENJPA-2990">OPENJPA-2990</A>
 */
public class TestBulkDeleteJoinTableCompat extends AbstractBulkDeleteTestCase {

    @Override
    public void setUp() {
        super.setUp(CLEAR_TABLES, BulkDeleteOwner.class, BulkDeleteDetails.class,
            BulkDeleteAddress.class, BulkDeleteItem.class,
            "openjpa.Compatibility", "CleanupOwnedTablesOnBulkDelete=false");
        createData();
        sql.clear();
    }

    public void testOwnedTableRowsAreLeftBehind() {
        assertEquals(2, delete("delete from BulkDeleteOwner o"));

        assertEquals(0, count(BulkDeleteOwner.class));
        assertOwnedTablesAreUntouched();
        assertNotSQL("DELETE FROM .*BULK_OWNER_NICKNAMES.*");
        assertNotSQL("DELETE FROM .*BULK_OWNER_ITEMS.*");
        assertNotSQL("DELETE FROM .*BULK_OWNER_LABELS.*");
        assertNotSQL("DELETE FROM .*BULK_OWNER_ADDRESSES.*");
        assertNotSQL("DELETE FROM .*BULK_OWNER_ALIASES.*");
    }
}
