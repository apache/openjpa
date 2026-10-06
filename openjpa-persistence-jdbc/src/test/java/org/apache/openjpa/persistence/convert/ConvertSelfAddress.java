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

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

/**
 * Embeddable that declares a converter on its own attribute. Such a
 * converter belongs to the embeddable itself and must therefore apply to
 * every entity embedding it.
 */
@Embeddable
public class ConvertSelfAddress implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Convert(converter = DotConverter.class)
    @Column(name = "SELF_STREET")
    protected String street;

    public ConvertSelfAddress() {
    }

    public ConvertSelfAddress(String street) {
        this.street = street;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @Override
    public String toString() {
        return "ConvertSelfAddress[street=" + street + "]";
    }
}
