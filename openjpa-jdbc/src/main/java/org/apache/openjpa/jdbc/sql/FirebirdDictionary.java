/*
 * Copyright 2006 The Apache Software Foundation.
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
package org.apache.openjpa.jdbc.sql;

import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;

/**
 * Dictionary for Firebird.
 */
public class FirebirdDictionary
    extends InterbaseDictionary {

    public FirebirdDictionary() {
        platform = "Firebird";

        // Firebird 1.5+ locking statement
        supportsLockingWithMultipleTables = false;
        forUpdateClause = "FOR UPDATE WITH LOCK";
    }

    public String getPlaceholderValueString(Column col) {
        return super.getPlaceholderValueString(col)
            + " AS " + getTypeName(col);
    }

    public void substring(SQLBuffer buf, FilterValue str, FilterValue start,
        FilterValue end) {
        // SUBSTRING in Firebird is of the form:
        // SELECT SUBSTRING(SOME_COLUMN FROM 1 FOR 5)
        buf.append("SUBSTRING(");
        str.appendTo(buf);
        buf.append(" FROM ");
        start.appendTo(buf);
        buf.append(" + 1");
        if (end != null) {
            buf.append(" FOR ");
            end.appendTo(buf);
            buf.append(" - (");
            start.appendTo(buf);
            buf.append(")");
        }
        buf.append(")");
    }
}
