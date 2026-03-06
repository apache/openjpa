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
package org.apache.openjpa.persistence.simple;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.util.Date;

/**
 * Java8 DateTime types which are required by the JPA-3.2 spec
 */
@Entity
public class Java8TimeTypes {

    @Id
    private int id;

    private java.util.Date oldDateField;

    private LocalTime localTimeField;
    private LocalDate localDateField;
    private LocalDateTime localDateTimeField;
    private OffsetTime offsetTimeField;
    private OffsetDateTime offsetDateTimeField;
    private Instant instantField;
    private Year yearField;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getOldDateField() {
        return oldDateField;
    }

    public void setOldDateField(Date oldDateField) {
        this.oldDateField = oldDateField;
    }

    public LocalTime getLocalTimeField() {
        return localTimeField;
    }

    public void setLocalTimeField(LocalTime localTimeField) {
        this.localTimeField = localTimeField;
    }

    public LocalDate getLocalDateField() {
        return localDateField;
    }

    public void setLocalDateField(LocalDate localDateField) {
        this.localDateField = localDateField;
    }

    public LocalDateTime getLocalDateTimeField() {
        return localDateTimeField;
    }

    public void setLocalDateTimeField(LocalDateTime localDateTimeField) {
        this.localDateTimeField = localDateTimeField;
    }

    public OffsetTime getOffsetTimeField() {
        return offsetTimeField;
    }

    public void setOffsetTimeField(OffsetTime offsetTimeField) {
        this.offsetTimeField = offsetTimeField;
    }

    public OffsetDateTime getOffsetDateTimeField() {
        return offsetDateTimeField;
    }

    public void setOffsetDateTimeField(OffsetDateTime offsetDateTimeField) {
        this.offsetDateTimeField = offsetDateTimeField;
    }

    public Instant getInstantField() {
        return instantField;
    }

    public void setInstantField(Instant instantField) {
        this.instantField = instantField;
    }

    public Year getYearField() {
        return yearField;
    }

    public void setYearField(Year yearField) {
        this.yearField = yearField;
    }
}
