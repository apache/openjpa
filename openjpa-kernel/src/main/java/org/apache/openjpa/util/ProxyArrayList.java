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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;

/**
 * Extension of the {@link ArrayList} type that dirties the
 * persistent/transactional field it is assigned to on modification.
 * The need to dirty the field on <b>any</b> modification mandates that
 * this class must override all mutator methods of the base type.
 * This may lead to multiple calls to <code>dirty</code> for one state
 * change if one mutator method of the base type calls another.
 *
 * @author Abe White
 * @nojavadoc
 */
public class ProxyArrayList
    extends ArrayList
    implements ProxyCollection {

    private transient Class _elementType = null;
    private transient OpenJPAStateManager _sm = null;
    private transient int _field = -1;
    private transient CollectionChangeTracker _ct = null;

    public ProxyArrayList() {
    }

    public ProxyArrayList(Class elementType, boolean trackChanges,
        OpenJPAConfiguration conf) {
        _elementType = elementType;
        if (trackChanges)
            _ct = new CollectionChangeTrackerImpl(this, true, true, conf);
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
        return new ArrayList((Collection) orig);
    }

    public ProxyCollection newInstance(Class elementType, Comparator compare,
        boolean trackChanges, OpenJPAConfiguration conf) {
        return new ProxyArrayList(elementType, trackChanges, conf);
    }

    public void add(int index, Object value) {
        Proxies.assertAllowedType(value, _elementType);
        Proxies.dirty(this);
        if (_ct != null)
            _ct.stopTracking();
        super.add(index, value);
    }

    public boolean add(Object value) {
        Proxies.assertAllowedType(value, _elementType);
        Proxies.dirty(this);
        if (super.add(value)) {
            if (_ct != null)
                _ct.added(value);
            return true;
        }
        return false;
    }

    public boolean addAll(int index, Collection values) {
        ensureCapacity(size() + values.size());
        for (Iterator itr = values.iterator(); itr.hasNext(); index++)
            add(index, itr.next());
        return values.size() > 0;
    }

    public boolean addAll(Collection values) {
        ensureCapacity(size() + values.size());
        boolean added = false;
        for (Iterator itr = values.iterator(); itr.hasNext();)
            added = add(itr.next()) || added;
        return added;
    }

    public void clear() {
        Proxies.dirty(this);
        if (_ct != null)
            _ct.stopTracking();
        for (int i = 0; i < size(); i++)
            Proxies.removed(this, get(i), false);
        super.clear();
    }

    public Iterator iterator() {
        return Proxies.iterator(this, super.iterator());
    }

    public ListIterator listIterator() {
        return Proxies.listIterator(this, super.listIterator(), _elementType);
    }

    public ListIterator listIterator(int index) {
        return Proxies.listIterator(this, super.listIterator(index),
            _elementType);
    }

    public Object remove(int index) {
        Proxies.dirty(this);
        Object rem = super.remove(index);
        if (_ct != null)
            _ct.removed(rem);
        Proxies.removed(this, rem, false);
        return rem;
    }

    public boolean remove(Object o) {
        Proxies.dirty(this);
        if (super.remove(o)) {
            if (_ct != null)
                _ct.removed(o);
            Proxies.removed(this, o, false);
            return true;
        }
        return false;
    }

    public boolean removeAll(Collection c) {
        boolean removed = false;
        for (Iterator itr = c.iterator(); itr.hasNext();)
            removed = remove(itr.next()) || removed;
        return removed;
    }

    public boolean retainAll(Collection c) {
        int size = size();
        for (Iterator itr = iterator(); itr.hasNext();)
            if (!c.contains(itr.next()))
                itr.remove();
        return size() < size;
    }

    public Object set(int index, Object element) {
        Proxies.assertAllowedType(element, _elementType);
        Proxies.dirty(this);
        if (_ct != null)
            _ct.stopTracking();
        Object rem = super.set(index, element);
        if (rem != element)
            Proxies.removed(this, rem, false);
        return rem;
    }

    protected Object writeReplace()
        throws ObjectStreamException {
        if (_sm != null && _sm.isDetached())
            return this;
        return copy(this);
    }
}
