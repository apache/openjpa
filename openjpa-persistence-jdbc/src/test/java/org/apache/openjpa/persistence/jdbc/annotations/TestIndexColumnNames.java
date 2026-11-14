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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the bug where the {@code columnNames} attribute of the
 * OpenJPA-specific field-level {@link org.apache.openjpa.persistence.jdbc.Index}
 * annotation was silently ignored during annotation parsing.
 *
 * <p>Root cause: {@code AnnotationPersistenceMappingParser.parseIndex(MappingInfo, Index)}
 * called the internal overload without passing {@code idx.columnNames()}, so the
 * specified column names were never added to the schema {@code Index} object.</p>
 */
class TestIndexColumnNames extends SingleEMFTestCase {

    private ClassMapping _mapping;

    @BeforeEach
    public void setUp() {
        setUp(EntityWithIndexColumnNames.class, CLEAR_TABLES);
        emf.createEntityManager().close();
        _mapping = (ClassMapping) JPAFacadeHelper.getMetaData(emf, EntityWithIndexColumnNames.class);
    }

    @Test
    void fieldIndexColumnNamesAreApplied() {
        FieldMapping fm = _mapping.getFieldMapping("colA");
        assertNotNull(fm, "Field mapping for 'colA' must exist");

        Index idx = fm.getValueIndex();
        assertNotNull(idx, "@Index on field 'colA' must create a schema index");

        Column[] columns = idx.getColumns();
        assertTrue(
                columns.length > 0,
                "Index defined with columnNames must contain at least one explicit column; "
                + "before the fix, columnNames was ignored and the column list was empty");

        Set<String> colNames = Arrays.stream(columns)
                .map(c -> c.getIdentifier().getName())
                .collect(Collectors.toSet());
        assertTrue(colNames.contains("COL_A"), "Index must contain COL_A (listed in columnNames)");
        assertTrue(colNames.contains("COL_B"), "Index must contain COL_B (listed in columnNames)");
    }
}
