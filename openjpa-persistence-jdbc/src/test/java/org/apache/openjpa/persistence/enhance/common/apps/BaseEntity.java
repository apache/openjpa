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
package org.apache.openjpa.persistence.enhance.common.apps;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Version;

/**
 * @see TestPCSubclasser
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class BaseEntity
    implements SubclassTestInstance {

    private long _id;
    private int _version;
    private String _s;
    private short _short;

    @Id
    @GeneratedValue
    public long getId() {
        return _id;
    }

    public void setId(long id) {
        _id = id;
    }

    @Version
    public int getVersion() {
        return _version;
    }

    public void setVersion(int version) {
        _version = version;
    }

    @Override
    public void setStringField(String s) {
        _s = s;
    }

    @Override
    public String getStringField() {
        return _s;
    }

    public short getShortField() {
        return _short;
    }

    public void setShortField(short aShort) {
        _short = aShort;
    }
}
