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
import org.apache.openjpa.util.InternalException;

/**
 * Size.
 *
 * @author Marc Prud'hommeaux
 */
class Size
    extends UnaryOp
    implements Val {

    public Size(Val val) {
        super(val);
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        // initialize the value with a null test
        getVal().initialize(sel, store, true);
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        getVal().calculateValue(sel, store, params, null, fetch);
        getVal().appendSize(sql, sel, store, params, fetch);
        sel.append(sql, getVal().getJoins());
        getVal().clearParameters();
    }

    protected Class getType(Class c) {
        return long.class;
    }

    protected String getOperator() {
        // since we override appendTo(), this method should never be called
        throw new InternalException();
    }
}
