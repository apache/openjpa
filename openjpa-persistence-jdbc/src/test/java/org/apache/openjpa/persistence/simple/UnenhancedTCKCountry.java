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

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;

/**
 * Mimics the TCK schema30 Country embeddable.
 * Named with "Unenhanced" prefix to skip build-time enhancement.
 */
@Embeddable
public class UnenhancedTCKCountry implements java.io.Serializable {

    private String country;
    private String code;

    public UnenhancedTCKCountry() {
    }

    public UnenhancedTCKCountry(String country, String code) {
        this.country = country;
        this.code = code;
    }

    @Basic
    public String getCountry() {
        return country;
    }

    public void setCountry(String v) {
        country = v;
    }

    @Basic
    public String getCode() {
        return code;
    }

    public void setCode(String v) {
        code = v;
    }
}
