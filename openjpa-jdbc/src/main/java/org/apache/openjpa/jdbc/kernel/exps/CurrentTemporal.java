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
package org.apache.openjpa.jdbc.kernel.exps;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;

import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.util.InternalException;

/**
 * A literal current LOCALDATE, LOCALTIME or LOCALDATETIME value in a filter.
 *
 */
class CurrentTemporal
    extends Const {

    
    private static final long serialVersionUID = 1L;
    private final Class<? extends Temporal> _type;

    public CurrentTemporal(Class<? extends Temporal> type) {
        _type = type;
    }

    @Override
    public Class<? extends Temporal> getType() {
        return _type;
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    public Object load(ExpContext ctx, ExpState state, Result res) throws SQLException {
        if (LocalDateTime.class.isAssignableFrom(_type)) {
            return LocalDateTime.ofInstant(res.getTimestamp(this, null).toInstant(), ZoneId.systemDefault());
        } else if (LocalTime.class.isAssignableFrom(_type)) {
            return res.getTime(this, null).toLocalTime();
        } else if (LocalDate.class.isAssignableFrom(_type)) {
            return res.getDate(this, null).toLocalDate();
        } else {
            throw new InternalException();
        }
    }

    @Override
    public Object getValue(Object[] params) {
        if (LocalDateTime.class.isAssignableFrom(_type)) {
            return LocalDateTime.now();
        } else if (LocalDate.class.isAssignableFrom(_type)) {
            return LocalDate.now();
        } else if (LocalTime.class.isAssignableFrom(_type)) {
            return LocalTime.now();
        }
        return null;
    }

    @Override
    public void appendTo(Select sel, ExpContext ctx, ExpState state, SQLBuffer sql, int index) {
        if (LocalDateTime.class.isAssignableFrom(_type)) {
            sql.append(ctx.store.getDBDictionary().currentTimestampFunction);
        } else if (LocalTime.class.isAssignableFrom(_type)) {
            sql.append(ctx.store.getDBDictionary().currentTimeFunction);
        } else if (LocalDate.class.isAssignableFrom(_type)) {
            sql.append(ctx.store.getDBDictionary().currentDateFunction);
        } else {
            throw new InternalException();
        }
    }
}
