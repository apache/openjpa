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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.kernel.OpenJPAStateManager;

/**
 * Extension of the {@link LinkedList} type that dirties the
 * persistent/transactional field it is assigned to on modification.
 * The need to dirty the field on <b>any</b> modification mandates that
 * this class must override all mutator methods of the base type.
 * This may lead to multiple calls to <code>dirty</code> for one state
 * change if one mutator method of the base type calls another.
 *
 * @author Abe White
 * @nojavadoc
 */
public class ProxyLinkedList
    extends LinkedList
    implements ProxyCollection {

    private transient Class _elementType = null;
    private transient OpenJPAStateManager _sm = null;
    private transient int _field = -1;
    private transient CollectionChangeTracker _ct = null;

    public ProxyLinkedList() {
    }

    public ProxyLinkedList(Class elementType, boolean trackChanges,
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
        return new LinkedList((Collection) orig);
    }

    public ProxyCollection newInstance(Class elementType, Comparator compare,
        boolean trackChanges, OpenJPAConfiguration conf) {
        return new ProxyLinkedList(elementType, trackChanges, conf);
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

    public void addFirst(Object value) {
        Proxies.assertAllowedType(value, _elementType);
        Proxies.dirty(this);
        super.addFirst(value);
        if (_ct != null)
            _ct.stopTracking();
    }

    public void addLast(Object value) {
        Proxies.assertAllowedType(value, _elementType);
        Proxies.dirty(this);
        super.addLast(value);
        if (_ct != null)
            _ct.added(value);
    }

    public boolean addAll(int index, Collection values) {
        for (Iterator itr = values.iterator(); itr.hasNext(); index++)
            add(index, itr.next());
        return values.size() > 0;
    }

    public boolean addAll(Collection values) {
        boolean added = false;
        for (Iterator itr = values.iterator(); itr.hasNext();)
            added = add(itr.next()) || added;
        return added;
    }

    public void clear() {
        Proxies.dirty(this);
        if (_ct != null)
            _ct.stopTracking();
        for (Iterator itr = super.iterator(); itr.hasNext();)
            Proxies.removed(this, itr.next(), false);
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

    public Object removeFirst() {
        Proxies.dirty(this);
        Object rem = super.removeFirst();
        if (_ct != null)
            _ct.removed(rem);
        Proxies.removed(this, rem, false);
        return rem;
    }

    public Object removeLast() {
        Proxies.dirty(this);
        Object rem = super.removeLast();
        if (_ct != null)
            _ct.removed(rem);
        Proxies.removed(this, rem, false);
        return rem;
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
