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
package org.apache.openjpa.kernel.exps;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.Calendar;

import org.apache.openjpa.kernel.StoreContext;

/**
 * Extract the field value of a temporal type
 *
 */
class ExtractDateTimeField
    extends Val {

    
    private static final long serialVersionUID = 1L;
    private final DateTimeExtractField _field;
    private final Val _val;

    /**
     * Constructor. Provide target field and the value.
     */
    public ExtractDateTimeField(DateTimeExtractField field, Val val) {
        _field = field;
        _val = val;
    }

    @Override
    public Class getType() {
        if (_field == DateTimeExtractField.SECOND) {
            return float.class;
        } else {
            return int.class;
        }
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {

        Object r = _val.eval(candidate, orig, ctx, params);
        if (r == null) {
            return null;
        }
        Temporal t = toTemporal(r);
        if (t == null) {
            throw new IllegalArgumentException();
        }
        if (_field == DateTimeExtractField.SECOND) {
            int seconds = t.get(ChronoField.SECOND_OF_MINUTE);
            int nanos = t.get(ChronoField.NANO_OF_SECOND);
            return (float) (seconds + nanos / 1_000_000_000d);
        }
        return t.get(_field.getEquivalent());
    }

    /**
     * Convert the given value to a temporal whose date and time fields are
     * the local date and time the value stands for. A value that is an
     * instant on the time line is read in the default time zone, as the
     * JDBC driver writes it, a calendar in its own time zone.
     */
    private static Temporal toTemporal(Object r) {
        if (r instanceof Date) {
            return ((Date) r).toLocalDate();
        } else if (r instanceof Time) {
            return ((Time) r).toLocalTime();
        } else if (r instanceof Timestamp) {
            return ((Timestamp) r).toLocalDateTime();
        } else if (r instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) r).toInstant(),
                ZoneId.systemDefault());
        } else if (r instanceof Calendar) {
            Calendar cal = (Calendar) r;
            return LocalDateTime.ofInstant(cal.toInstant(),
                cal.getTimeZone().toZoneId());
        } else if (r instanceof Instant) {
            return LocalDateTime.ofInstant((Instant) r, ZoneId.systemDefault());
        } else if (r instanceof Temporal) {
            return (Temporal) r;
        }
        return null;
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        visitor.exit(this);
    }
}

