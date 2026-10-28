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
package org.apache.openjpa.jdbc.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Common <code>char</code> and <code>Character</code> fields of the entities
 * used by {@link TestCharStorage}.
 */
@MappedSuperclass
public abstract class CharStorageBase {

    @Id
    private int id;

    @Column(name = "CHARDATA")
    private char charData;

    @Column(name = "CHARACTERDATA")
    private Character characterData;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public char getCharData() {
        return charData;
    }

    public void setCharData(char charData) {
        this.charData = charData;
    }

    public Character getCharacterData() {
        return characterData;
    }

    public void setCharacterData(Character characterData) {
        this.characterData = characterData;
    }
}
