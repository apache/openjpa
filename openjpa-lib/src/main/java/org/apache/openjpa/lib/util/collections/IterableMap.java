/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util.collections;

import java.util.Map;

/**
 * Defines a map that can be iterated directly without needing to create an entry set.
 * <p>
 * A map iterator is an efficient way of iterating over maps.
 * There is no need to access the entry set or use Map Entry objects.
 * </p>
 * <pre>
 * IterableMap&lt;String,Integer&gt; map = new HashedMap&lt;String,Integer&gt;();
 * MapIterator&lt;String,Integer&gt; it = map.mapIterator();
 * while (it.hasNext()) {
 *   String key = it.next();
 *   Integer value = it.getValue();
 *   it.setValue(value + 1);
 * }
 * </pre>
 *
 * @param <K> the type of the keys in this map
 * @param <V> the type of the values in this map
 *
 * @since 3.0
 */
public interface IterableMap<K, V> extends Map<K, V> {
    /**
     * @see Map#clear()
     */
    void clear();

    /**
     * Note that the return type is Object, rather than V as in the Map interface.
     * See the class Javadoc for further info.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <code>key</code>, or
     *         <code>null</code> if there was no mapping for <code>key</code>.
     *         (A <code>null</code> return can also indicate that the map
     *         previously associated <code>null</code> with <code>key</code>,
     *         if the implementation supports <code>null</code> values.)
     * @see Map#put(Object, Object)
     */
    V put(K key, V value);

    /**
     * @param t mappings to be stored in this map
     * @see Map#putAll(Map)
     */
    void putAll(Map<? extends K, ? extends V> t);

    /**
     * Obtains a <code>MapIterator</code> over the map.
     * <p>
     * A map iterator is an efficient way of iterating over maps.
     * There is no need to access the entry set or use Map Entry objects.
     * <pre>
     * IterableMap&lt;String,Integer&gt; map = new HashedMap&lt;String,Integer&gt;();
     * MapIterator&lt;String,Integer&gt; it = map.mapIterator();
     * while (it.hasNext()) {
     *   String key = it.next();
     *   Integer value = it.getValue();
     *   it.setValue(value + 1);
     * }
     * </pre>
     *
     * @return a map iterator
     */
    MapIterator<K, V> mapIterator();
}
