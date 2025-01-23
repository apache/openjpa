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
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

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
        Temporal t = null;
        if (Date.class.isAssignableFrom(r.getClass())) {
            t = ((Date) r).toLocalDate();
        } else if (Time.class.isAssignableFrom(r.getClass())) {
            t = ((Time) r).toLocalTime();
        } else if (Timestamp.class.isAssignableFrom(r.getClass())) {
            t = ((Timestamp) r).toInstant();
        } else if (Temporal.class.isAssignableFrom(r.getClass())) {
            t = (Temporal) r;
        }
        if (t == null) {
            throw new IllegalArgumentException();
        }
        switch (_field) {
            case QUARTER:
                int month = t.get(ChronoField.MONTH_OF_YEAR);
                return (int) Math.round(Math.ceil(month/3f));
            case SECOND:
                int seconds = t.get(ChronoField.SECOND_OF_MINUTE);
                int mili = t.get(ChronoField.MILLI_OF_SECOND);
                return (float) seconds + (float) (mili/1000f);
            default:
                return t.get(_field.getEquivalent());
        }
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _val.acceptVisit(visitor);
        visitor.exit(this);
    }
}

