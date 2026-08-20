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
package org.apache.openjpa.persistence.cascade.mx1;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "UCMXA_UNI")
public class UnenhancedCascadeA implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    protected String id;

    @Basic
    protected String name;

    @Basic
    protected int value;

    public UnenhancedCascadeA() {
    }

    public UnenhancedCascadeA(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public String getAId() {
        return id;
    }

    public String getAName() {
        return name;
    }

    public int getAValue() {
        return value;
    }
}
