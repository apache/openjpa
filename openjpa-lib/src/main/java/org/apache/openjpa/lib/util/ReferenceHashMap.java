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
package org.apache.openjpa.lib.util;

import org.apache.openjpa.lib.util.collections.AbstractReferenceMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Map in which the key, value, or both may be weak/soft references.
 *
 * @author Abe White
 * @since 0.4.0
 */
public class ReferenceHashMap
    extends org.apache.openjpa.lib.util.collections.ReferenceMap
    implements ReferenceMap, SizedMap {

    
    private static final long serialVersionUID = 1L;
    private int _maxSize = Integer.MAX_VALUE;

    public ReferenceHashMap(ReferenceStrength keyType, ReferenceStrength valueType) {
        super(keyType, valueType);
    }

    public ReferenceHashMap(ReferenceStrength keyType, ReferenceStrength valueType, int capacity,
        float loadFactor) {
        super(keyType, valueType, capacity, loadFactor);
    }

    @Override
    public int getMaxSize() {
        return _maxSize;
    }

    @Override
    public void setMaxSize(int maxSize) {
        _maxSize = (maxSize < 0) ? Integer.MAX_VALUE : maxSize;
        if (_maxSize != Integer.MAX_VALUE)
            removeOverflow(_maxSize);
    }

    @Override
    public boolean isFull() {
        return size() >= _maxSize;
    }

    @Override
    public void overflowRemoved(Object key, Object value) {
    }

    @Override
    public void valueExpired(Object key) {
    }

    @Override
    public void keyExpired(Object value) {
    }

    @Override
    public void removeExpired() {
        purge();
    }

    /**
     * Remove any entries over max size.
     */
    private void removeOverflow(int maxSize) {
        Object key;
        while (size() > maxSize) {
            key = keySet().iterator().next();
            overflowRemoved(key, remove(key));
        }
    }

    @Override
    protected void addMapping(int hashIndex, int hashCode, Object key,
        Object value) {
        if (_maxSize != Integer.MAX_VALUE)
            removeOverflow(_maxSize - 1);
        super.addMapping(hashIndex, hashCode, key, value);
    }

    @Override
    protected ReferenceEntry createEntry(HashEntry next, int hashCode, Object key,
        Object value) {
        return new AccessibleEntry(this, next, hashCode, key, value);
    }

    @Override
    protected void doWriteObject(ObjectOutputStream out) throws IOException {
        out.writeInt(_maxSize);
        super.doWriteObject(out);
    }

    @Override
    protected void doReadObject(ObjectInputStream in)
        throws ClassNotFoundException, IOException {
        _maxSize = in.readInt();
        super.doReadObject(in);
    }

    /**
     * Extension of the base entry type that allows our outer class to access
     * protected state.
     */
    private static class AccessibleEntry extends ReferenceEntry {
        private final ReferenceHashMap parent;

        public AccessibleEntry(AbstractReferenceMap map, HashEntry next,
                               int hashCode, Object key, Object value) {
            super(map, next, hashCode, key, value);
            parent = (ReferenceHashMap)map;
        }

        public Object key() {
            return key;
        }

        public Object value() {
            return value;
        }

        public AccessibleEntry nextEntry() {
            return (AccessibleEntry) next;
        }

        public void setNextEntry(AccessibleEntry next) {
            this.next = next;
        }

        @Override
        protected void onPurge() {
            if (parent.isKeyType(ReferenceStrength.HARD)) {
                parent.valueExpired(key);
            } else if (parent.isValueType(ReferenceStrength.HARD)) {
                parent.keyExpired(value);
            }
        }
    }
}
