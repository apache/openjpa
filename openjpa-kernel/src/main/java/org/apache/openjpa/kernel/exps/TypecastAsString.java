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
import java.time.format.DateTimeFormatter;

import org.apache.openjpa.kernel.StoreContext;

/**
 * Casts a given value as string
 *
 */
class TypecastAsString extends Val {
    private static final long serialVersionUID = 1L;
    private final Val _val;

    /**
     * Constructor. Provide target field and the value.
     */
    public TypecastAsString(Val val) {
        _val = val;
    }

    @Override
    public Class getType() {
        return String.class;
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
        Class<?> clazz = r.getClass();
        if (r instanceof Time t) {
            return t.toLocalTime().format(DateTimeFormatter.ISO_TIME);
        } else if (r instanceof Timestamp t) {
            return t.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (r instanceof LocalDateTime d) {
            return d.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (r instanceof LocalTime t) {
            return t.format(DateTimeFormatter.ISO_LOCAL_TIME);
        } else if (r instanceof Instant i) {
            return LocalDateTime.ofInstant(i, ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (r instanceof Date d) {
        	return d.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (r instanceof java.util.Date d) {
            return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (r instanceof LocalDate d) {
            return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (r instanceof Number n) {
            return String.valueOf(n);
        } else if (r instanceof Boolean b) {
            return String.valueOf(b);
        }
        throw new IllegalArgumentException("Value is not supported for typecast: " + r);
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        visitor.exit(this);
    }
}
