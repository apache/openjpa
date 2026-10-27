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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQueries;

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
        if (r == null) {
            return null;
        }
        Temporal t = ExtractDateTimeField.toTemporal(r);
        if (t != null) {
            if (_part == DateTimeExtractPart.TIME) {
                LocalTime time = t.query(TemporalQueries.localTime());
                if (time != null) {
                    return Time.valueOf(time);
                }
            } else if (_part == DateTimeExtractPart.DATE) {
                LocalDate date = t.query(TemporalQueries.localDate());
                if (date != null) {
                    return Date.valueOf(date);
                }
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

