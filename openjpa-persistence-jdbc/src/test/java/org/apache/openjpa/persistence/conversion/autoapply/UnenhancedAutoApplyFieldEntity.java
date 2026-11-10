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
package org.apache.openjpa.persistence.conversion.autoapply;

import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Field access. {@link Sku} and {@link Weight} are not serializable and carry
 * no mapping annotation; only their auto-apply converters make them basic.
 */
@Entity
@Table(name = "AA_FIELD")
public class UnenhancedAutoApplyFieldEntity {

    @Id
    private long id;

    private Sku sku;

    private Weight weight;

    @Convert(disableConversion = true)
    private Sku unconvertedSku;

    private Opaque opaque;

    @Embedded
    private UnenhancedPackaging packaging;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Sku getSku() {
        return sku;
    }

    public void setSku(Sku sku) {
        this.sku = sku;
    }

    public Weight getWeight() {
        return weight;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }

    public Sku getUnconvertedSku() {
        return unconvertedSku;
    }

    public void setUnconvertedSku(Sku unconvertedSku) {
        this.unconvertedSku = unconvertedSku;
    }

    public Opaque getOpaque() {
        return opaque;
    }

    public void setOpaque(Opaque opaque) {
        this.opaque = opaque;
    }

    public UnenhancedPackaging getPackaging() {
        return packaging;
    }

    public void setPackaging(UnenhancedPackaging packaging) {
        this.packaging = packaging;
    }
}
