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

import java.util.HashSet;
import java.util.Set;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;
import org.junit.Test;

public class TestIndices extends SingleEMFTestCase {
	@Override
	public void setUp() {
		setUp(EntityWithIndices.class, CLEAR_TABLES
//			,"openjpa.Log","SQL=trace"
		);
	}

	@Test
	public void testIndicesCreated() {
		JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
		ClassMapping cls = conf.getMappingRepositoryInstance().getMapping(EntityWithIndices.class, null, true);
		Table table = cls.getTable();
		Index idx1 = table.getIndex(DBIdentifier.newIndex("idx_index1"));
		assertNotNull("Defined index should exist", idx1);
		assertFalse(idx1.isUnique());

		Index idx2 = table.getIndex(DBIdentifier.newIndex("idx_long"));
		assertNotNull("Defined index should exist", idx2);
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
	}
}
