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
package org.apache.openjpa.util;

/**
 * {@link OpenJPAId} subclass appropriate for String fields.
 *
 * @author Steve Kim
 */
public final class StringId extends OpenJPAId {

    private final String key;

    public StringId(Class cls, String key) {
        super(cls);
        this.key = (key == null) ? "" : key;
    }

    public StringId(Class cls, String key, boolean subs) {
        super(cls, subs);
        this.key = (key == null) ? "" : key;
    }

    public String getId() {
        return key;
    }

    public Object getIdObject() {
        return key;
    }

    public String toString() {
        return key;
    }

    protected int idHash() {
        return key.hashCode();
    }

    protected boolean idEquals(OpenJPAId o) {
        return key.equals(((StringId) o).key);
    }
}
