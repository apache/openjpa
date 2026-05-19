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

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SecondPrecisionEntity {

    @Id
    private long id;

    @Column(secondPrecision = 3)
    private LocalDateTime createdAt;

    @Column(secondPrecision = 6)
    private LocalDateTime updatedAt;

    @Column(secondPrecision = 0)
    private LocalDateTime dayOnly;

    private LocalDateTime defaultPrecision;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDayOnly() {
        return dayOnly;
    }

    public void setDayOnly(LocalDateTime dayOnly) {
        this.dayOnly = dayOnly;
    }

    public LocalDateTime getDefaultPrecision() {
        return defaultPrecision;
    }

    public void setDefaultPrecision(LocalDateTime defaultPrecision) {
        this.defaultPrecision = defaultPrecision;
    }
}
