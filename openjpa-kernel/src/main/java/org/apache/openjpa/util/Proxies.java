/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.util;

import java.io.ObjectStreamException;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.Localizer;

/**
 * Utility methods for managing proxies.
 *
 * @author Abe White
 */
public class Proxies {

    public static final int MODE_ENTRY = 0;
    public static final int MODE_KEY = 1;
    public static final int MODE_VALUE = 2;

    private static final Localizer _loc = Localizer.forPackage(Proxies.class);

    /**
     * Used by proxy types to check if the given owners and field names
     * are equivalent.
     */
    public static boolean isOwner(Proxy proxy, OpenJPAStateManager sm,
        int field) {
        return proxy.getOwner() == sm && proxy.getOwnerField() == field;
    }

    /**
     * Used by proxy types to check that an attempt to add a new value is legal.
     */
    public static void assertAllowedType(Object value, Class allowed) {
        if (value != null && allowed != null && !allowed.isInstance(value)) {
            throw new UserException(_loc.get("bad-elem-type", new Object[]{
                allowed.getClassLoader(),
                allowed,
                value.getClass().getClassLoader(),
                value.getClass()
            }));
        }
    }

    /**
     * Used by proxy types to dirty their owner.
     */
    public static void dirty(Proxy proxy) {
        if (proxy.getOwner() != null)
            proxy.getOwner().dirty(proxy.getOwnerField());
    }

    /**
     * Used by proxy types to notify their owner that an element has been
     * removed.
     */
    public static void removed(Proxy proxy, Object removed, boolean key) {
        if (proxy.getOwner() != null && removed != null)
            proxy.getOwner().removed(proxy.getOwnerField(), removed, key);
    }

    /**
     * Return an iterator that dirties its owner on calls to remove. This
     * iterator assumes that the given proxy collection uses a
     * {@link CollectionChangeTracker}.
     */
    public static Iterator iterator(final ProxyCollection proxy,
        final Iterator itr) {
        return new Iterator() {
            private Object _last = null;

            public boolean hasNext() {
                return itr.hasNext();
            }

            public Object next() {
                _last = itr.next();
                return _last;
            }

            public void remove() {
                dirty(proxy);
                itr.remove();
                if (proxy.getChangeTracker() != null)
                    ((CollectionChangeTracker) proxy.getChangeTracker()).
                        removed(_last);
                removed(proxy, _last, false);
            }
        };
    }

    /**
     * Return a proxy iterator that dirties its owner on remove, set, and
     * add. This iterator assumes that the given proxy collection uses a
     * {@link CollectionChangeTracker}.
     */
    public static ListIterator listIterator(final ProxyCollection proxy,
        final ListIterator itr, final Class allowed) {
        return new ListIterator() {
            private Object _last = null;

            public boolean hasNext() {
                return itr.hasNext();
            }

            public int nextIndex() {
                return itr.nextIndex();
            }

            public Object next() {
                _last = itr.next();
                return _last;
            }

            public boolean hasPrevious() {
                return itr.hasPrevious();
            }

            public int previousIndex() {
                return itr.previousIndex();
            }

            public Object previous() {
                _last = itr.previous();
                return _last;
            }

            public void set(Object o) {
                assertAllowedType(o, allowed);
                dirty(proxy);
                itr.set(o);
                if (proxy.getChangeTracker() != null)
                    proxy.getChangeTracker().stopTracking();
                removed(proxy, _last, false);
                _last = o;
            }

            public void add(Object o) {
                assertAllowedType(o, allowed);
                dirty(proxy);
                itr.add(o);
                if (proxy.getChangeTracker() != null) {
                    if (hasNext())
                        proxy.getChangeTracker().stopTracking();
                    else
                        ((CollectionChangeTracker) proxy.getChangeTracker()).
                            added(o);
                }
                _last = o;
            }

            public void remove() {
                dirty(proxy);
                itr.remove();
                if (proxy.getChangeTracker() != null)
                    ((CollectionChangeTracker) proxy.getChangeTracker()).
                        removed(_last);
                removed(proxy, _last, false);
            }
        };
    }

    /**
     * Return a proxy for the given map key or entry set.
     */
    public static Set entrySet(final ProxyMap proxy, final Set set,
        final int mode) {
        return new AbstractSet() {
            public int size() {
                return set.size();
            }

            public boolean remove(Object o) {
                if (mode != MODE_KEY)
                    throw new UnsupportedOperationException();

                Map map = (Map) proxy;
                if (!map.containsKey(o))
                    return false;
                map.remove(o);
                return true;
            }

            public Iterator iterator() {
                final Iterator itr = set.iterator();
                return new Iterator() {
                    private Map.Entry _last = null;

                    public boolean hasNext() {
                        return itr.hasNext();
                    }

                    public Object next() {
                        _last = (Map.Entry) itr.next();
                        switch (mode) {
                            case MODE_KEY:
                                return _last.getKey();
                            case MODE_VALUE:
                                return _last.getValue();
                            default:
                                return _last;
                        }
                    }

                    public void remove() {
                        dirty(proxy);
                        itr.remove();
                        if (proxy.getChangeTracker() != null)
                            ((MapChangeTracker) proxy.getChangeTracker()).
                                removed(_last.getKey(), _last.getValue());
                        removed(proxy, _last.getKey(), true);
                        removed(proxy, _last.getValue(), false);
                    }
                };
            }

            protected Object writeReplace()
                throws ObjectStreamException {
                switch (mode) {
                    case MODE_KEY:
                        return ((Map) proxy).keySet();
                    case MODE_VALUE:
                        return ((Map) proxy).values();
                    default:
                        return ((Map) proxy).entrySet();
                }
			}
		};
	}
}

