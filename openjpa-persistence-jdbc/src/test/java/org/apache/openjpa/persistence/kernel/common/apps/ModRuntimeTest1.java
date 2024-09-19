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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "Modrtest1")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ModRuntimeTest1 {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int id;

    @Transient
    public static final String someStaticField = "someField";

    private Locale localeField;

    private int intField;

    @Column(length = 35)
    private String stringField;

    @Column(length = 35)
    public String transString;

    @ManyToOne(cascade = { CascadeType.PERSIST })
    private ModRuntimeTest1 selfOneOne;

    @OneToMany(cascade = { CascadeType.PERSIST })
    private Set<ModRuntimeTest1> selfOneMany = new HashSet<>();

    public ModRuntimeTest1() {
    }

    public ModRuntimeTest1(String str, int i) {
        stringField = str;
        intField = i;
    }

    public int getId() {
        return id;
    }

    public int getIntField() {
        return this.intField;
    }

    public void setIntField(int intField) {
        this.intField = intField;
    }

    public String getStringField() {
        return this.stringField;
    }

    public void setStringField(String stringField) {
        this.stringField = stringField;
    }

    public ModRuntimeTest1 getSelfOneOne() {
        return this.selfOneOne;
    }

    public void setSelfOneOne(ModRuntimeTest1 selfOneOne) {
        this.selfOneOne = selfOneOne;
    }

    public Set getSelfOneMany() {
        return this.selfOneMany;
    }

    public void setSelfOneMany(Set selfOneMany) {
        this.selfOneMany = selfOneMany;
    }

    @Override
    public String toString() {
        return "IntField: " + intField + ", StringField: " + stringField + " .";
    }

    public Locale getLocaleField() {
        return localeField;
    }

    public void setLocaleField(Locale localeField) {
        this.localeField = localeField;
    }
}
