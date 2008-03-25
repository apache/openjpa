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
package org.apache.openjpa.lib.util.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.apache.openjpa.lib.util.SizedMap;

/**
 * An implementation of {@link SizedMap} that uses JDK1.5 concurrency primitives
 *
 * @since 1.1.0
 */
public class SizedConcurrentHashMap
    extends NullSafeConcurrentHashMap
    implements SizedMap, ConcurrentMap, Serializable {

    private int maxSize;

    /**
     * @param size the maximum size of this map. If additional elements are
     * put into the map, overflow will be removed via calls to
     * {@link #overflowRemoved}.
     * @param load the load factor for the underlying map
     * @param concurrencyLevel the concurrency level for the underlying map
     *
     * @see ConcurrentHashMap
     */
    public SizedConcurrentHashMap(int size, float load, int concurrencyLevel) {
        super(size, load, concurrencyLevel);
        setMaxSize(size);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        Object o = super.putIfAbsent(key, value);
        if (maxSize != Integer.MAX_VALUE)
            removeOverflow();
        return o;
    }

    @Override
    public Object put(Object key, Object value) {
        Object o = super.put(key, value);
        if (maxSize != Integer.MAX_VALUE)
            removeOverflow();
        return o;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int max) {
        if (max < 0)
            throw new IllegalArgumentException(String.valueOf(max));
        maxSize = max;

        removeOverflow();
    }

    protected void removeOverflow() {
        while (size() > maxSize) {
            Entry entry = removeRandom();
            // if removeRandom() returns null, break out of the loop. Of course,
            // since we're not locking, the size might not actually be null
            // when we do this. But this prevents weird race conditions from
            // putting this thread into more loops.
            if (entry == null)
                break;
            overflowRemoved(entry.getKey(), entry.getValue());
        }
    }

    public boolean isFull() {
        return size() >= maxSize;
    }

    public Map.Entry removeRandom() {
        // this isn't really random, but is concurrent.
        while (true) {
            if (size() == 0)
                return null;
            Set entries = entrySet();
            Entry e = (Entry) entries.iterator().next();
            final Object key = e.getKey();
            final Object val = e.getValue();
            if (remove(key) != null)
                // create a new Entry instance because the ConcurrentHashMap
                // implementation's one is "live" so does not behave as desired
                // after removing the entry.
                return new Entry() {
                    public Object getKey() {
                        return key;
                    }

                    public Object getValue() {
                        return val;
                    }

                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException();
                    }
                };
        }
    }

    public Iterator randomEntryIterator() {
        // this isn't really random, but is concurrent.
        return entrySet().iterator();
    }

    /**
     * This implementation does nothing.
     */
    public void overflowRemoved(Object key, Object value) {
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(maxSize);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        maxSize = in.readInt();
    }
}
