/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.jdbc.annotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestIndices extends SingleEMFTestCase {
    @BeforeEach
    public void setUp() {
        setUp(EntityWithIndices.class, CLEAR_TABLES
//            ,"openjpa.Log","SQL=trace"
        );
    }

    @Test
    public void testIndicesCreated() {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        ClassMapping cls = conf.getMappingRepositoryInstance().getMapping(EntityWithIndices.class, null, true);
        Table table = cls.getTable();
        Index idx1 = table.getIndex(DBIdentifier.newIndex("idx_index1"));
        assertNotNull(idx1, "Defined index should exist");
        assertFalse(idx1.isUnique());

        Index idx2 = table.getIndex(DBIdentifier.newIndex("idx_long"));
        assertNotNull(idx2, "Defined index should exist");
        assertTrue(idx2.isUnique());

        Set<String> indexedCols = new HashSet<>();
        for (Index idx : table.getIndexes()) {
            for (Column col : idx.getColumns()) {
                indexedCols.add(col.getIdentifier().getName());
            }
        }
        assertTrue(indexedCols.contains("INDEX1"));
        assertTrue(indexedCols.contains("LONG_NAME"));
        assertFalse(indexedCols.contains("NAME"));

        // test multi column index without spaces
        assertIndexColumns(table, "idx_wo_spaces", "INDEX1", "COL2", "COL3");
        // test multi column index without spaces
        assertIndexColumns(table, "idx_with_spaces", "LONG_NAME", "COL2", "COL3");
        // test indexes without defined name
        assertHasIndexWithColumns(table, "LONG_NAME", "COL2");
    }

    private void assertIndexColumns(Table table, String indexName, String... assertedColumnNames) {
        Index idx = table.getIndex(DBIdentifier.newIndex(indexName));
        assertNotNull(idx, "Defined index should exist");
        final List<String> indexColumnNames = Arrays.stream(idx.getColumns())
                .map(c -> c.getIdentifier().getName())
                .collect(Collectors.toList());
        for (String assertedColumnName : assertedColumnNames) {
            assertTrue(indexColumnNames.contains(assertedColumnName), "Column " + assertedColumnName + " does not exist in index " + indexName);
        }
    }

    private void assertHasIndexWithColumns(Table table, String...assertedColumnNames) {
        for (Index idx: table.getIndexes()) {
            Column[] cols = idx.getColumns();
            if (cols.length != assertedColumnNames.length) {
                continue;
            } else {
                String[] colNames = Arrays.stream(cols)
                    .map(Column::getIdentifier)
                    .map(DBIdentifier::getName)
                    .collect(Collectors.toList())
                    .toArray(String[]::new);
                if (Arrays.equals(colNames, assertedColumnNames)) {
                    return;
                }
            }
        }
        fail("Could not find an unnamed index with the given columns.");
    }
    
}
