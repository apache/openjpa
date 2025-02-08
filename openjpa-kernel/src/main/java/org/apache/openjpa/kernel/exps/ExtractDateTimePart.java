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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

import org.apache.openjpa.kernel.StoreContext;

/**
 * Extract the part value of a temporal type
 *
 */
class ExtractDateTimePart
    extends Val {

    
    private static final long serialVersionUID = 1L;
    private final DateTimeExtractPart _part;
    private final Val _val;

    /**
     * Constructor. Provide target field and the value.
     */
    public ExtractDateTimePart(DateTimeExtractPart part, Val val) {
        _part = part;
        _val = val;
    }

    @Override
    public Class getType() {
        if (_part == DateTimeExtractPart.TIME) {
            return Time.class;
        } else if (_part == DateTimeExtractPart.DATE) {
            return Date.class;
        }
        throw new IllegalStateException();
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    protected Object eval(Object candidate, Object orig,
        StoreContext ctx, Object[] params) {

        Object r = _val.eval(candidate, orig, ctx, params);
        Class<?> clazz = r.getClass();
        if (_part == DateTimeExtractPart.TIME) {
            if (Time.class.isAssignableFrom(clazz)) {
                return (Time) r;
            } else if (Timestamp.class.isAssignableFrom(clazz)) {
                return Time.valueOf(((Timestamp) r).toLocalDateTime().toLocalTime());
            } else if (LocalDateTime.class.isAssignableFrom(clazz)) {
                return Time.valueOf(((LocalDateTime) r).toLocalTime());
            } else if (LocalTime.class.isAssignableFrom(clazz)) {
                return Time.valueOf((LocalTime) r);
            } else if (Instant.class.isAssignableFrom(clazz)) {
                LocalDateTime ldt = LocalDateTime.ofInstant((Instant) r, ZoneId.systemDefault());
                return Time.valueOf(ldt.toLocalTime());
            }
        } else if (_part == DateTimeExtractPart.DATE) {
            if (Date.class.isAssignableFrom(clazz)) {
                return (Date) r;
            } else if (Timestamp.class.isAssignableFrom(clazz)) {
                return Date.valueOf(((Timestamp) r).toLocalDateTime().toLocalDate());
            } else if (LocalDateTime.class.isAssignableFrom(clazz)) {
                return Date.valueOf(((LocalDateTime) r).toLocalDate());
            } else if (LocalDate.class.isAssignableFrom(clazz)) {
                return Date.valueOf((LocalDate) r);
            } else if (Instant.class.isAssignableFrom(clazz)) {
                LocalDateTime ldt = LocalDateTime.ofInstant((Instant) r, ZoneId.systemDefault());
                return Date.valueOf(ldt.toLocalDate());
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        visitor.exit(this);
    }

}

