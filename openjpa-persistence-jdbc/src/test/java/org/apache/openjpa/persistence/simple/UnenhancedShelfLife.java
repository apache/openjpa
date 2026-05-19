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

import java.sql.Date;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

/**
 * Property-access embeddable mimicking TCK's ShelfLife.
 * Named "Unenhanced*" so the build-time enhancer skips it.
 */
@Embeddable
public class UnenhancedShelfLife implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Date inceptionDate;
    private Date soldDate;

    public UnenhancedShelfLife() {
    }

    public UnenhancedShelfLife(Date d1, Date d2) {
        inceptionDate = d1;
        soldDate = d2;
    }

    @Basic
    public Date getInceptionDate() {
        return inceptionDate;
    }

    public void setInceptionDate(Date d1) {
        inceptionDate = d1;
    }

    @Basic
    public Date getSoldDate() {
        return soldDate;
    }

    public void setSoldDate(Date d2) {
        soldDate = d2;
    }
}
