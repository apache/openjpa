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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Map with predictable iteration order.
 *
 * @author Pinaki Poddar
 * @since 2.0.0
 */
public class OrderedMap<K, V> implements Map<K, V>, Serializable {
    private static final long serialVersionUID = 1L;
    private final LinkedHashMap<K, V> _del;

    /**
     * Construct with predictable insertion order.
     */
    public OrderedMap() {
        _del = new LinkedHashMap<>(6,0.1f,false);
    }

    /**
     * Construct with predictable iteration order.
     * @param lru if true the iterator order is based on last access, false for order of insertion.
     */
    public OrderedMap(boolean lru) {
        _del = new LinkedHashMap<>(6,0.1f,lru);
    }

    public int indexOf(Object key) {
        int i = 0;
        for (K k : _del.keySet()) {
            if (key.equals(k))
                return i;
            i++;
        }
        return -1;
    }

    @Override
    public void clear() {
    }


    @Override
    public boolean containsKey(Object key) {
        return _del.containsKey(key);
    }


    @Override
    public boolean containsValue(Object value) {
        return _del.containsValue(value);
    }


    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        return _del.entrySet();
    }


    @Override
    public V get(Object key) {
        return _del.get(key);
    }


    @Override
    public boolean isEmpty() {
        return _del.isEmpty();
    }


    @Override
    public Set<K> keySet() {
        return _del.keySet();
    }


    @Override
    public V put(K key, V value) {
        return _del.put(key, value);
    }


    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        _del.putAll(m);
    }


    @Override
    public V remove(Object key) {
        return _del.remove(key);
    }


    @Override
    public int size() {
        return _del.size();
    }


    @Override
    public Collection<V> values() {
        return _del.values();
    }

}
