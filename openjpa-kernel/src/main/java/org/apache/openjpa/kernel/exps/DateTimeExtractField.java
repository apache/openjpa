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

import java.time.temporal.ChronoField;

/**
 * Type identifiers used by EXTRACT function
 */
public enum DateTimeExtractField {

    /**
     * Means the calendar year
     */
    YEAR(ChronoField.YEAR),
    /**
     * Means the calendar quarter, numbered from 1 to 4
     */
    QUARTER,
    /**
     * Means the calendar month of the year, numbered from 1
     */
    MONTH(ChronoField.MONTH_OF_YEAR),
    /**
     * Means the ISO-8601 week number
     */
    WEEK(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR),
    /**
     * Means the calendar day of the month, numbered from 1
     */
    DAY(ChronoField.DAY_OF_MONTH),
    /**
     * Means the hour of the day in 24-hour time, numbered from 0 to 23
     */
    HOUR(ChronoField.HOUR_OF_DAY),
    /**
     * Mans the minute of the hour, numbered from 0 to 59
     */
    MINUTE(ChronoField.MINUTE_OF_HOUR),
    /**
     * Means the second of the minute, numbered from 0 to 59, including a fractional part representing fractions of a second.
     */
    SECOND(ChronoField.SECOND_OF_MINUTE);
    
    private final ChronoField equivalent;

    private DateTimeExtractField() {
        this.equivalent = null;
    }

    private DateTimeExtractField(ChronoField equivalent) {
        this.equivalent = equivalent;
    }

    public ChronoField getEquivalent() {
        return equivalent;
    }

}
