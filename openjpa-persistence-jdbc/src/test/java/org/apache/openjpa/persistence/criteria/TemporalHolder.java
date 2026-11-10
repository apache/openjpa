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

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Entity with nullable temporal fields of the various supported types, used to
 * test temporal values in COALESCE/CASE expressions.
 */
@Entity
public class TemporalHolder {
    @Id
    private int id;

    private String name;

    @Column(name = "due")
    private Timestamp due;

    private Date dueDate;

    private Time dueTime;

    private LocalDateTime dueLocalDateTime;

    private LocalDate dueLocalDate;

    private LocalTime dueLocalTime;

    private Character code;

    public TemporalHolder() {
    }

    public TemporalHolder(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Timestamp getDue() {
        return due;
    }

    public void setDue(Timestamp due) {
        this.due = due;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Time getDueTime() {
        return dueTime;
    }

    public void setDueTime(Time dueTime) {
        this.dueTime = dueTime;
    }

    public LocalDateTime getDueLocalDateTime() {
        return dueLocalDateTime;
    }

    public void setDueLocalDateTime(LocalDateTime dueLocalDateTime) {
        this.dueLocalDateTime = dueLocalDateTime;
    }

    public LocalDate getDueLocalDate() {
        return dueLocalDate;
    }

    public void setDueLocalDate(LocalDate dueLocalDate) {
        this.dueLocalDate = dueLocalDate;
    }

    public LocalTime getDueLocalTime() {
        return dueLocalTime;
    }

    public void setDueLocalTime(LocalTime dueLocalTime) {
        this.dueLocalTime = dueLocalTime;
    }

    public Character getCode() {
        return code;
    }

    public void setCode(Character code) {
        this.code = code;
    }
}
