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

import java.util.UUID;

/**
 * Identity type appropriate for UUID primary key fields and shared
 * id classes.
 *
 * @author Abe White
 */
public final class UuidId
    extends OpenJPAId {

    
    private static final long serialVersionUID = 1L;
    private UUID _key;

    public UuidId(Class<?> cls, UUID key) {
        super(cls);
        _key = key;
    }

    public UuidId(Class<?> cls, UUID key, boolean subs) {
        super(cls, subs);
        _key = key;
    }

    public UUID getId() {
        return _key;
    }

    /**
     * Allow utilities in this package to mutate id.
     */
    void setId(UUID id) {
        _key = id;
    }

    @Override
    public Object getIdObject() {
        return _key;
    }

    @Override
    protected int idHash() {
        return (_key == null) ? 0 : _key.hashCode();
    }

    @Override
    protected boolean idEquals(OpenJPAId o) {
        Object key = ((UuidId) o)._key;
        return (_key == null) ? key == null : _key.equals(key);
    }
}
