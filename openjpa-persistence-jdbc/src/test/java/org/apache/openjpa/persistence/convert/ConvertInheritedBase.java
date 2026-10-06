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
package org.apache.openjpa.persistence.convert;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * MappedSuperclass declaring an attribute that subclasses may override
 * with a class-level {@code @Convert}.
 */
@MappedSuperclass
public abstract class ConvertInheritedBase {

    @Id
    protected String id;

    protected String street;

    protected ConvertInheritedBase() {
    }

    protected ConvertInheritedBase(String id, String street) {
        this.id = id;
        this.street = street;
    }

    public String getId() {
        return id;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }
}
