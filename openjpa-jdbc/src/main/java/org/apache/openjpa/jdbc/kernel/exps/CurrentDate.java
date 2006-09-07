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

import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.util.InternalException;

/**
 * A literal current DATE/TIME/TIMESTAMP value in a filter.
 *
 * @author Marc Prud'hommeaux
 */
class CurrentDate
    extends Const {

    private final int _type;

    public CurrentDate(int type) {
        _type = type;
    }

    public Class getType() {
        return Date.class;
    }

    public void setImplicitType(Class type) {
    }

    public Object getValue(Object[] params) {
        return new Date();
    }

    public void appendTo(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer sql, int index) {
        switch (_type) {
            case JavaSQLTypes.DATE:
                sql.append(ctx.store.getDBDictionary().currentDateFunction);
                break;
            case JavaSQLTypes.TIME:
                sql.append(ctx.store.getDBDictionary().currentTimeFunction);
                break;
            case JavaSQLTypes.TIMESTAMP:
                sql.append(ctx.store.getDBDictionary().
                    currentTimestampFunction);
                break;
            default:
                throw new InternalException();
        }
    }
}
