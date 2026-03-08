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

import java.io.Serializable;
import java.util.Calendar;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Mirrors TCK entity ee.jakarta.tck.persistence.core.annotations.temporal.A2_Property.
 * Uses property access with Calendar @Id and @Temporal(DATE).
 * Named "Unenhanced*" so the build-time enhancer skips it,
 * forcing runtime enhancement like the TCK.
 */
@Entity
@Table(name = "UNENHANCED_CAL_ID")
public class UnenhancedCalendarIdPropertyEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Calendar id;

    protected String stringVersion;

    public UnenhancedCalendarIdPropertyEntity() {
    }

    public UnenhancedCalendarIdPropertyEntity(Calendar id) {
        this.id = id;
    }

    public UnenhancedCalendarIdPropertyEntity(Calendar id, String stringVersion) {
        this.id = id;
        this.stringVersion = stringVersion;
    }

    @Id
    @Temporal(TemporalType.DATE)
    public Calendar getId() {
        return id;
    }

    public void setId(Calendar id) {
        this.id = id;
    }

    public String getStringVersion() {
        return this.stringVersion;
    }

    public void setStringVersion(String stringVersion) {
        this.stringVersion = stringVersion;
    }
}
