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
package org.apache.openjpa.persistence.blob.mysql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
public class ClobColumnEntity {
    @Id
    private int id;

    @Lob
    @Column(length = 20)
    protected String smallLob;

    @Lob
    @Column(length = 66000)
    protected String medLob;

    @Lob
    @Column(length = 16777216)
    protected String longLob;

    @Lob
    protected String defaultLob;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSmallLob() {
        return smallLob;
    }

    public void setSmallLob(String smallLob) {
        this.smallLob = smallLob;
    }

    public String getMedLob() {
        return medLob;
    }

    public void setMedLob(String medLob) {
        this.medLob = medLob;
    }

    public String getLongLob() {
        return longLob;
    }

    public void setLongLob(String longLob) {
        this.longLob = longLob;
    }

    public String getDefaultLob() {
        return defaultLob;
    }

    public void setDefaultLob(String defaultLob) {
        this.defaultLob = defaultLob;
    }
}
