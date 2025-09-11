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
class TypecastAsString
    extends Val {

    
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
        Class<?> clazz = r.getClass();
        if (Time.class.isAssignableFrom(clazz)) {
            return ((Time) r).toLocalTime().format(DateTimeFormatter.ISO_TIME);
        } else if (Timestamp.class.isAssignableFrom(clazz)) {
            return ((Timestamp) r).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (LocalDateTime.class.isAssignableFrom(clazz)) {
            return ((LocalDateTime) r).toLocalTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (LocalTime.class.isAssignableFrom(clazz)) {
            return ((LocalTime) r).format(DateTimeFormatter.ISO_LOCAL_TIME);
        } else if (Instant.class.isAssignableFrom(clazz)) {
            return LocalDateTime.ofInstant((Instant) r, ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (Date.class.isAssignableFrom(clazz)) {
            return LocalDateTime.ofInstant(((Date) r).toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } else if (LocalDate.class.isAssignableFrom(clazz)) {
            return ((LocalDate) r).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (Number.class.isAssignableFrom(clazz)) {
        	return String.valueOf((Number) r);
        } else if (Boolean.class.isAssignableFrom(clazz)) {
        	return String.valueOf((Boolean) r);
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

