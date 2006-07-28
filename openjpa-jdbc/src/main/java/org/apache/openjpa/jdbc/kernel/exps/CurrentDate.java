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

import java.util.Date;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;

/**
 * A literal current DATE/TIME/TIMESTAMP value in a filter.
 *
 * @author Marc Prud'hommeaux
 */
class CurrentDate
    extends Const {

    static final int DATE = 1;
    static final int TIME = 2;
    static final int TIMESTAMP = 3;

    private final int _type;

    CurrentDate(int type) {
        _type = type;
    }

    public Class getType() {
        return Date.class;
    }

    public void setImplicitType(Class type) {
    }

    public Object getValue() {
        return new Date();
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        if (_type == DATE)
            sql.append(store.getDBDictionary().currentDateFunction);
        else if (_type == TIME)
            sql.append(store.getDBDictionary().currentTimeFunction);
        else if (_type == TIMESTAMP)
            sql.append(store.getDBDictionary().currentTimestampFunction);
    }

    public void clearParameters() {
    }
}
