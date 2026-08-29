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
package org.apache.openjpa.persistence.criteria;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.LocalDateField;
import jakarta.persistence.criteria.LocalDateTimeField;
import jakarta.persistence.criteria.LocalTimeField;
import jakarta.persistence.criteria.TemporalField;

import org.apache.openjpa.kernel.exps.DateTimeExtractField;
import org.junit.Test;

/**
 * Tests that EXTRACT resolves each temporal field constant of the specification to its
 * kernel equivalent, rather than deriving it from the constant's textual representation.
 */
public class TestExtractTemporalField {

    private final CriteriaBuilderImpl cb = new CriteriaBuilderImpl();

    @Test
    public void testLocalDateFields() {
        assertExtracts(DateTimeExtractField.YEAR, LocalDateField.YEAR, LocalDate.class);
        assertExtracts(DateTimeExtractField.QUARTER, LocalDateField.QUARTER, LocalDate.class);
        assertExtracts(DateTimeExtractField.MONTH, LocalDateField.MONTH, LocalDate.class);
        assertExtracts(DateTimeExtractField.WEEK, LocalDateField.WEEK, LocalDate.class);
        assertExtracts(DateTimeExtractField.DAY, LocalDateField.DAY, LocalDate.class);
    }

    @Test
    public void testLocalTimeFields() {
        assertExtracts(DateTimeExtractField.HOUR, LocalTimeField.HOUR, LocalTime.class);
        assertExtracts(DateTimeExtractField.MINUTE, LocalTimeField.MINUTE, LocalTime.class);
        assertExtracts(DateTimeExtractField.SECOND, LocalTimeField.SECOND, LocalTime.class);
    }

    @Test
    public void testLocalDateTimeFields() {
        assertExtracts(DateTimeExtractField.YEAR, LocalDateTimeField.YEAR, LocalDateTime.class);
        assertExtracts(DateTimeExtractField.QUARTER, LocalDateTimeField.QUARTER, LocalDateTime.class);
        assertExtracts(DateTimeExtractField.MONTH, LocalDateTimeField.MONTH, LocalDateTime.class);
        assertExtracts(DateTimeExtractField.WEEK, LocalDateTimeField.WEEK, LocalDateTime.class);
        assertExtracts(DateTimeExtractField.DAY, LocalDateTimeField.DAY, LocalDateTime.class);
        assertExtracts(DateTimeExtractField.HOUR, LocalDateTimeField.HOUR, LocalDateTime.class);
        assertExtracts(DateTimeExtractField.MINUTE, LocalDateTimeField.MINUTE, LocalDateTime.class);
        assertExtracts(DateTimeExtractField.SECOND, LocalDateTimeField.SECOND, LocalDateTime.class);
    }

    /**
     * DATE and TIME are extracted as a part rather than as a field; OpenJPA supports them in
     * JPQL only, so the criteria API must reject them with a meaningful error.
     */
    @Test
    public void testUnsupportedFieldIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> extract(LocalDateTimeField.DATE, LocalDateTime.class));
        assertThrows(IllegalArgumentException.class,
            () -> extract(LocalDateTimeField.TIME, LocalDateTime.class));
        assertThrows(IllegalArgumentException.class,
            () -> extract(new TemporalField<Integer, LocalDate>() { }, LocalDate.class));
    }

    private <N, T extends Temporal> void assertExtracts(DateTimeExtractField expected,
            TemporalField<N, T> field, Class<T> temporalType) {
        Expression<N> e = extract(field, temporalType);
        assertSame(field.toString(), expected, ((Expressions.ExtractField<N>) e).getField());
        assertEquals(field.toString(),
            expected == DateTimeExtractField.SECOND ? Double.class : Integer.class, e.getJavaType());
    }

    private <N, T extends Temporal> Expression<N> extract(TemporalField<N, T> field, Class<T> temporalType) {
        return cb.extract(field, new Expressions.Constant<>(temporalType, null));
    }
}
