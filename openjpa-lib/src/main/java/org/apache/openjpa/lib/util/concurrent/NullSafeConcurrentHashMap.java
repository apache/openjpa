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
import java.util.Enumeration;
import java.util.Set;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.AbstractCollection;

/**
 * A subclass of {@link ConcurrentHashMap} that allows null keys and values.
 * In exchange, it weakens the contract of {@link #putIfAbsent} and the other
 * concurrent methods added in {@link #ConcurrentHashMap}.
 *
 * @since 1.1.0
 */
public class NullSafeConcurrentHashMap extends ConcurrentHashMap {

    private enum Null {
        MARKER
    }

    public NullSafeConcurrentHashMap(int size, float load,
        int concurrencyLevel) {
        super(size, load, concurrencyLevel);
    }

    public NullSafeConcurrentHashMap() {
    }

    /**
     * Returns internal representation for object.
     */
    private static Object maskNull(Object o) {
        return (o == null ? Null.MARKER : o);
    }

    /**
     * Returns object represented by specified internal representation.
     */
    private static Object unmaskNull(Object o) {
        return (o == Null.MARKER ? null : o);
    }

    @Override
    public Object remove(Object key) {
        return unmaskNull(super.remove(maskNull(key)));
    }

    @Override
    public boolean remove(Object key, Object value) {
        return super.remove(maskNull(key), maskNull(value));
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        return super.replace(maskNull(key), maskNull(oldValue),
            maskNull(newValue));
    }

    @Override
    public Object replace(Object key, Object value) {
        return unmaskNull(super.replace(maskNull(key), maskNull(value)));
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        return unmaskNull(super.putIfAbsent(maskNull(key), maskNull(value)));
    }

    @Override
    public Object put(Object key, Object value) {
        return unmaskNull(super.put(maskNull(key), maskNull(value)));
    }

    @Override
    public Object get(Object key) {
        return unmaskNull(super.get(maskNull(key)));
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(maskNull(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return super.containsValue(maskNull(value));
    }

    @Override
    public boolean contains(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration elements() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set entrySet() {
        return new TranslatingSet(super.entrySet()) {
            protected Object unmask(Object internal) {
                final Entry e = (Entry) internal;
                return new Entry() {

                    public Object getKey() {
                        return unmaskNull(e.getKey());
                    }

                    public Object getValue() {
                        return unmaskNull(e.getValue());
                    }

                    public Object setValue(Object value) {
                        return unmaskNull(e.setValue(maskNull(value)));
                    }

                    @Override
                    public int hashCode() {
                        return e.hashCode();
                    }
                };
            }
        };
    }

    @Override
    public Enumeration keys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set keySet() {
        return new TranslatingSet(super.keySet()) {
            protected Object unmask(Object internal) {
                return unmaskNull(internal);
            }
        };
    }

    @Override
    public void putAll(Map t) {
        super.putAll(t);
    }

    @Override
    public Collection values() {
        return new TranslatingCollection(super.values()) {

            protected Object unmask(Object internal) {
                return unmaskNull(internal);
            }
        };
    }

    private abstract class TranslatingSet extends AbstractSet {

        private Set backingSet;

        private TranslatingSet(Set backing) {
            this.backingSet = backing;
        }

        protected abstract Object unmask(Object internal);

        public Iterator iterator() {
            final Iterator iterator = backingSet.iterator();
            return new Iterator() {
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public Object next() {
                    return unmask(iterator.next());
                }

                public void remove() {
                    iterator.remove();
                }
            };
        }

        public int size() {
            return backingSet.size();
        }
    }

    private abstract class TranslatingCollection extends AbstractCollection {

        private Collection backingCollection;

        private TranslatingCollection(Collection backing) {
            this.backingCollection = backing;
        }

        protected abstract Object unmask(Object internal);

        public Iterator iterator() {
            final Iterator iterator = backingCollection.iterator();
            return new Iterator() {
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public Object next() {
                    return unmask(iterator.next());
                }

                public void remove() {
                    iterator.remove();
                }
            };
        }

        public int size() {
            return backingCollection.size();
        }
    }
}
