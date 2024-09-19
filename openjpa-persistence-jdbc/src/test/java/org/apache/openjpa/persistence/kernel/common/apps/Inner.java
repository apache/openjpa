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
package org.apache.openjpa.persistence.kernel.common.apps;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nullvalue")
public class Inner {

    @Basic
    private Integer none = null;

    @Basic(optional = false)
    @Column(name="exception_col")
    private Integer exception = null;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int Id;

    public Inner() {
    }

    public int getId() {
        return Id;
    }

    public Integer getNone() {
        return none;
    }

    public Integer getException() {
        return exception;
    }

    public void setNone(Integer val) {
        none = val;
    }

    public void setException(Integer val) {
        exception = val;
    }
}
