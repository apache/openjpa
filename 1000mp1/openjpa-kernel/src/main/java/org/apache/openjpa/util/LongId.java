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

import serp.util.Numbers;

/**
 * {@link OpenJPAId} subclass appropriate for long fields.
 *
 * @author Steve Kim
 */
public final class LongId extends OpenJPAId {

    private final long key;

    public LongId(Class cls, Long key) {
        this(cls, (key == null) ? 0L : key.longValue());
    }

    public LongId(Class cls, String key) {
        this(cls, (key == null) ? 0L : Long.parseLong(key));
    }

    public LongId(Class cls, long key) {
        super(cls);
        this.key = key;
    }

    public LongId(Class cls, long key, boolean subs) {
        super(cls, subs);
        this.key = key;
    }

    public long getId() {
        return key;
    }

    public Object getIdObject() {
        return Numbers.valueOf(key);
    }

    protected int idHash() {
        return (int) (key ^ (key >>> 32));
    }

    protected boolean idEquals(OpenJPAId o) {
        return key == ((LongId) o).key;
    }
}
