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
package org.apache.openjpa.persistence.conversion;

import java.util.UUID;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.apache.openjpa.persistence.Externalizer;
import org.apache.openjpa.persistence.Factory;
import org.apache.openjpa.persistence.Persistent;

/**
 * Converted attributes whose column type must follow the converter's
 * database type, compared with an attribute externalized by OpenJPA's own
 * Externalizer API.
 */
@Entity
public class ConvertedColumnTypeEntity {

    @Id
    private long id;

    @Persistent
    @Externalizer("toString")
    @Factory("UUID.fromString")
    private UUID externalized;

    @Convert(converter = UuidStringConverter.class)
    private UUID converted;

    @Convert(converter = CodeConverter.class)
    private Code code;

    @Convert(converter = InheritedCodeConverter.class)
    private Code inheritedCode;

    @Convert(converter = InterfaceCodeConverter.class)
    private Code interfaceCode;

    private Rank rank;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getExternalized() {
        return externalized;
    }

    public void setExternalized(UUID externalized) {
        this.externalized = externalized;
    }

    public UUID getConverted() {
        return converted;
    }

    public void setConverted(UUID converted) {
        this.converted = converted;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public Code getInheritedCode() {
        return inheritedCode;
    }

    public void setInheritedCode(Code inheritedCode) {
        this.inheritedCode = inheritedCode;
    }

    public Code getInterfaceCode() {
        return interfaceCode;
    }

    public void setInterfaceCode(Code interfaceCode) {
        this.interfaceCode = interfaceCode;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }
}
