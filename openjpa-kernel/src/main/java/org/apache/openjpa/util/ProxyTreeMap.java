/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.util;

import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;

/**
 * Extension of the {@link TreeMap} type that dirties the
 * persistent/transactional field it is assigned to on modification.
 * The need to dirty the field on <b>any</b> modification mandates that
 * this class must override all mutator methods of the base type.
 * This may lead to multiple calls to <code>dirty</code> for one state
 * change if one mutator method of the base type calls another.
 *
 * @author Abe White
 * @nojavadoc
 */
public class ProxyTreeMap extends TreeMap implements ProxyMap {

    private transient Class _keyType = null;
    private transient Class _valueType = null;
    private transient OpenJPAStateManager _sm = null;
    private transient int _field = -1;
    private transient MapChangeTracker _ct = null;

    public ProxyTreeMap() {
    }

    public ProxyTreeMap(Class keyType, Class valueType, boolean trackChanges,
        OpenJPAConfiguration conf) {
        _keyType = keyType;
        _valueType = valueType;
        if (trackChanges)
            _ct = new MapChangeTrackerImpl(this, conf);
    }

    public ProxyTreeMap(Class keyType, Class valueType, Comparator compare,
        boolean trackChanges, OpenJPAConfiguration conf) {
        super(compare);
        _keyType = keyType;
        _valueType = valueType;
        if (trackChanges)
            _ct = new MapChangeTrackerImpl(this, conf);
    }

    public void setOwner(OpenJPAStateManager sm, int field) {
        _sm = sm;
        _field = field;
    }

    public OpenJPAStateManager getOwner() {
        return _sm;
    }

    public int getOwnerField() {
        return _field;
    }

    public ChangeTracker getChangeTracker() {
        return _ct;
    }

    public Object copy(Object orig) {
        if (orig instanceof SortedMap)
            return new TreeMap((SortedMap) orig);
        return new TreeMap((Map) orig);
    }

    public ProxyMap newInstance(Class keyType, Class valueType,
        Comparator compare, boolean trackChanges, OpenJPAConfiguration conf) {
        if (compare == null)
            return new ProxyTreeMap(keyType, valueType, trackChanges, conf);
        return new ProxyTreeMap(keyType, valueType, compare, trackChanges,
            conf);
    }

    public void clear() {
        Proxies.dirty(this);
        if (_ct != null)
            _ct.stopTracking();
        Map.Entry entry;
        for (Iterator itr = super.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            Proxies.removed(this, entry.getKey(), true);
            Proxies.removed(this, entry.getValue(), false);
        }
        super.clear();
    }

    public Set keySet() {
        return Proxies.entrySet(this, super.entrySet(), Proxies.MODE_KEY);
    }

    public Collection values() {
        return Proxies.entrySet(this, super.entrySet(), Proxies.MODE_VALUE);
    }

    public Set entrySet() {
        return Proxies.entrySet(this, super.entrySet(), Proxies.MODE_ENTRY);
    }

    public Object put(Object key, Object value) {
        Proxies.assertAllowedType(key, _keyType);
        Proxies.assertAllowedType(value, _valueType);
        Proxies.dirty(this);
        boolean had = containsKey(key);
        Object old = super.put(key, value);
        if (had) {
            if (_ct != null)
                _ct.changed(key, old, value);
            Proxies.removed(this, old, false);
        } else if (_ct != null)
            _ct.added(key, value);
        return old;
    }

    public void putAll(Map m) {
        Map.Entry entry;
        for (Iterator itr = m.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public Object remove(Object key) {
        Proxies.dirty(this);
        boolean had = containsKey(key);
        Object old = super.remove(key);
        if (had) {
            if (_ct != null)
                _ct.removed(key, old);
            Proxies.removed(this, key, true);
            Proxies.removed(this, old, false);
        }
        return old;
    }

    protected Object writeReplace() throws ObjectStreamException {
        if (_sm != null && _sm.isDetached())
            return this;
        return copy(this);
    }
}
