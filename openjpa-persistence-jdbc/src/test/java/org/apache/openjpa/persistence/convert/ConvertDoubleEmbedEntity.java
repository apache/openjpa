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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity embedding the same embeddable twice, with a converter override
 * on one of the two embeddings only.
 */
@Entity
@Table(name = "CONV_DOUBLE_EMBED")
public class ConvertDoubleEmbedEntity {

    @Id
    private String id;

    @Embedded
    @Converts(value = {
        @Convert(attributeName = "street", converter = DotConverter.class)
    })
    private ConvertAddress converted;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street",
            column = @Column(name = "PLAIN_STREET")),
        @AttributeOverride(name = "city",
            column = @Column(name = "PLAIN_CITY")),
        @AttributeOverride(name = "state",
            column = @Column(name = "PLAIN_STATE"))
    })
    private ConvertAddress plain;

    public ConvertDoubleEmbedEntity() {
    }

    public ConvertDoubleEmbedEntity(String id, ConvertAddress converted,
            ConvertAddress plain) {
        this.id = id;
        this.converted = converted;
        this.plain = plain;
    }

    public String getId() {
        return id;
    }

    public ConvertAddress getConverted() {
        return converted;
    }

    public void setConverted(ConvertAddress converted) {
        this.converted = converted;
    }

    public ConvertAddress getPlain() {
        return plain;
    }

    public void setPlain(ConvertAddress plain) {
        this.plain = plain;
    }
}
