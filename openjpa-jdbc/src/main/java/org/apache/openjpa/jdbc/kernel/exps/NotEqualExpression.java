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
package org.apache.openjpa.jdbc.kernel.exps;

import org.apache.openjpa.jdbc.kernel.JDBCFetchState;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * Compares two values.
 *
 * @author Abe White
 */
class NotEqualExpression
    extends CompareEqualExpression {

    /**
     * Constructor. Supply values to compare.
     */
    public NotEqualExpression(Val val1, Val val2) {
        super(val1, val2);
    }

    public void appendTo(SQLBuffer buf, Select sel, JDBCStore store,
        Object[] params, JDBCFetchState fetchState, boolean val1Null,
        boolean val2Null) {
        if (val1Null && val2Null)
            buf.appendValue(null).append(" IS NOT ").appendValue(null);
        else if (val1Null || val2Null) {
            Val val = (val1Null) ? getValue2() : getValue1();
            if (!isDirectComparison()) {
                int len = val.length();
                for (int i = 0; i < len; i++) {
                    if (i > 0)
                        buf.append(" AND ");
                    val.appendTo(buf, i, sel, store, params, fetchState);
                    buf.append(" IS NOT ").appendValue(null);
                }
            } else
                val.appendIsNotNull(buf, sel, store, params, fetchState);
        } else {
            Val val1 = getValue1();
            Val val2 = getValue2();
            if (val1.length() == 1 && val2.length() == 1) {
                store.getDBDictionary().comparison(buf, "<>",
                    new FilterValueImpl(val1, sel, store, params, fetchState),
                    new FilterValueImpl(val2, sel, store, params, fetchState));
            } else {
                int len = java.lang.Math.max(val1.length(), val2.length());
                buf.append("(");
                for (int i = 0; i < len; i++) {
                    if (i > 0)
                        buf.append(" OR ");
                    val1.appendTo(buf, i, sel, store, params, fetchState);
                    buf.append(" <> ");
                    val2.appendTo(buf, i, sel, store, params, fetchState);
                }
                buf.append(")");
            }
        }
    }
}
