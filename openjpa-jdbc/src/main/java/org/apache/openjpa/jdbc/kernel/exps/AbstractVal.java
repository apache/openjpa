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

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;

/**
 * Abstract value for easy extension.
 *
 * @author Marc Prud'hommeaux
 */
abstract class AbstractVal
    implements Val {

    protected static final String TRUE = "1 = 1";
    protected static final String FALSE = "1 <> 1";

    public boolean isVariable() {
        return false;
    }

    public boolean isAggregate() {
        return false;
    }

    public void appendIsEmpty(SQLBuffer sql, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        sql.append(FALSE);
    }

    public void appendIsNotEmpty(SQLBuffer sql, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        sql.append(TRUE);
    }

    public void appendIsNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        appendTo(sql, 0, sel, store, params, fetch);
        sql.append(" IS ").appendValue(null);
    }

    public void appendIsNotNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        appendTo(sql, 0, sel, store, params, fetch);
        sql.append(" IS NOT ").appendValue(null);
    }

    public void appendSize(SQLBuffer sql, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        sql.append("1");
    }

    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        visitor.exit(this);
    }
}

