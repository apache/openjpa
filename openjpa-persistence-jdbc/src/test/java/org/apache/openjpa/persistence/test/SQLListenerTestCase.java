/*
 * Copyright 2007 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.test;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;

/**
 * Base class for tests that need to check generated SQL.
 *
 * @author Patrick Linskey
 */
public abstract class SQLListenerTestCase
    extends SingleEMFTestCase {

    protected List<String> sql = new ArrayList<String>();

    @Override
    public void setUp(Object... props) {
        Object[] copy = new Object[props.length + 2];
        System.arraycopy(props, 0, copy, 0, props.length);
        copy[copy.length - 2] = "openjpa.jdbc.JDBCListeners";
        copy[copy.length - 1] = new JDBCListener[] { new Listener() };
        super.setUp(copy); 
    }

    /**
     * Confirm that the specified SQL has been executed.
     *
     * @param sqlExp the SQL expression. E.g., "SELECT FOO .*"
     */
    public void assertSQL(String sqlExp) {
        for (String statement : sql) {
            if (statement.matches(sqlExp))
                return;
        }

        fail("Expected regular expression <" + sqlExp + "> to have"
            + " existed in SQL statements: " + sql);
    }

    /**
     * Confirm that the specified SQL has not been executed.
     *
     * @param sqlExp the SQL expression. E.g., "SELECT BADCOLUMN .*"
     */
    public void assertNotSQL(String sqlExp) {
        boolean failed = false;

        for (String statement : sql) {
            if (statement.matches(sqlExp))
                failed = true;
        }

        if (failed)
            fail("Regular expression <" + sqlExp + ">"
                + " should not have been executed in SQL statements: " + sql);
    }

    public class Listener
        extends AbstractJDBCListener {

        @Override
        public void beforeExecuteStatement(JDBCEvent event) {
            if (event.getSQL() != null && sql != null)
                sql.add(event.getSQL());
		}
	}
}
