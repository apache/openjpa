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
package org.apache.openjpa.jdbc.sql;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.kernel.StoreContext;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TestHerdDBDictionary {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    final JDBCConfiguration mockConfiguration = context.mock(JDBCConfiguration.class);
    final Statement mockStatement = context.mock(Statement.class);
    final Connection mockConnection = context.mock(Connection.class);
    final ResultSet mockRS = context.mock(ResultSet.class);
    final DataSource mockDS = context.mock(DataSource.class);
    final DatabaseMetaData mockMetaData = context.mock(DatabaseMetaData.class);

    final StoreContext sc = null;

    @Test
    public void testBootDBDictionary() throws Exception {
        // Expected method calls on the mock objects above. If any of these are
        // do not occur, or if any other methods are invoked on the mock objects
        // an exception will be thrown and the test will fail.
        context.checking(new Expectations()
        {
            {
                allowing(mockConfiguration);
            }
        });

        DBDictionary dict = new HerdDBDictionary();
        dict.setConfiguration(mockConfiguration);
        assertNull(dict.getDefaultSchemaName());

        assertTrue(dict.supportsForeignKeys);
        assertTrue(dict.supportsUniqueConstraints);
        assertTrue(dict.supportsCascadeDeleteAction);
        assertFalse(dict.supportsCascadeUpdateAction);
    }

}
