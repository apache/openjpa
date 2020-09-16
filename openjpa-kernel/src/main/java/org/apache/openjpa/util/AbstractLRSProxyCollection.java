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

import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.collections.FilterIterator;
import org.apache.openjpa.lib.util.collections.IteratorChain;

/**
 * A collection proxy designed for collections backed by extremely large
 * result sets in which each call to {@link #iterator} may perform a database
 * query. Changes to the collection are tracked through a
 * {@link ChangeTracker}. This collection has the following limitations:
 * <ul>
 * <li>The <code>size</code> method may return {@link Integer#MAX_VALUE}.</li>
 * <li>The collection cannot contain duplicate elements.</li>
 * </ul>
 *
 * @author Abe White
 */
public abstract class AbstractLRSProxyCollection
    implements Set, LRSProxy, Predicate, CollectionChangeTracker {

    private static final Localizer _loc = Localizer.forPackage
        (AbstractLRSProxyCollection.class);

    private Class _elementType = null;
    private CollectionChangeTrackerImpl _ct = null;
    private OpenJPAStateManager _sm = null;
    private int _field = -1;
    private OpenJPAStateManager _origOwner = null;
    private int _origField = -1;
    private int _count = -1;
    private boolean _iterated = false;

    /**
     * Constructor.
     *
     * @param elementType the allowed type of elements, or null for no
     * restrictions
     * @param ordered true if this collection is ordered
     */
    public AbstractLRSProxyCollection(Class elementType, boolean ordered) {
        _elementType = elementType;
        _ct = new CollectionChangeTrackerImpl(this, false, ordered,false);
        _ct.setAutoOff(false);
    }

    @Override
    public void setOwner(OpenJPAStateManager sm, int field) {
        // can't transfer ownership of an lrs proxy
        if (sm != null && _origOwner != null
            && (_origOwner != sm || _origField != field)) {
            throw new InvalidStateException(_loc.get("transfer-lrs",
                _origOwner.getMetaData().getField(_origField)));
        }

        _sm = sm;
        _field = field;

        // keep track of original owner so we can detect transfer attempts
        if (sm != null) {
            _origOwner = sm;
            _origField = field;
        }
    }

    @Override
    public OpenJPAStateManager getOwner() {
        return _sm;
    }

    @Override
    public int getOwnerField() {
        return _field;
    }

    @Override
    public ChangeTracker getChangeTracker() {
        return this;
    }

    @Override
    public Object copy(Object orig) {
        // used to store fields for rollback; we don't store lrs fields
        return null;
    }

    @Override
    public boolean add(Object o) {
        Proxies.assertAllowedType(o, _elementType);
        Proxies.dirty(this, false);
        _ct.added(o);
        return true;
    }

    @Override
    public boolean addAll(Collection all) {
        Proxies.dirty(this, false);
        boolean added = false;
        Object add;
        for (Iterator itr = all.iterator(); itr.hasNext();) {
            add = itr.next();
            Proxies.assertAllowedType(add, _elementType);
            _ct.added(add);
            added = true;
        }
        return added;
    }

    @Override
    public boolean remove(Object o) {
        if (!contains(o))
            return false;
        Proxies.dirty(this, false);
        Proxies.removed(this, o, false);
        _ct.removed(o);
        return true;
    }

    @Override
    public boolean removeAll(Collection all) {
        Proxies.dirty(this, false);
        boolean removed = false;
        Object rem;
        for (Iterator itr = all.iterator(); itr.hasNext();) {
            rem = itr.next();
            if (remove(rem)) {
                Proxies.removed(this, rem, false);
                _ct.removed(rem);
                removed = true;
            }
        }
        return removed;
    }

    @Override
    public boolean retainAll(Collection all) {
        if (all.isEmpty()) {
            clear();
            return true;
        }

        Proxies.dirty(this, false);
        Itr itr = (Itr) iterator();
        try {
            boolean removed = false;
            Object rem;
            while (itr.hasNext()) {
                rem = itr.next();
                if (!all.contains(rem)) {
                    Proxies.removed(this, rem, false);
                    _ct.removed(rem);
                    removed = true;
                }
            }
            return removed;
        } finally {
            itr.close();
        }
    }

    @Override
    public void clear() {
        Proxies.dirty(this, false);
        Itr itr = (Itr) iterator();
        try {
            Object rem;
            while (itr.hasNext()) {
                rem = itr.next();
                Proxies.removed(this, rem, false);
                _ct.removed(rem);
            }
        }
        finally {
            itr.close();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (_elementType != null && !_elementType.isInstance(o))
            return false;
        if (_ct.getAdded().contains(o))
            return true;
        if (_ct.getRemoved().contains(o))
            return false;
        if (!has(o))
            return false;
        return true;
    }

    @Override
    public boolean containsAll(Collection all) {
        for (Iterator itr = all.iterator(); itr.hasNext();)
            if (!contains(itr.next()))
                return false;
        return true;
    }

    @Override
    public Object[] toArray() {
        return asList().toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        return asList().toArray(a);
    }

    private List asList() {
        Itr itr = (Itr) iterator();
        try {
            List list = new ArrayList();
            while (itr.hasNext())
                list.add(itr.next());
            return list;
        } finally {
            itr.close();
        }
    }

    @Override
    public int size() {
        if (_count == -1)
            _count = count();
        if (_count == Integer.MAX_VALUE)
            return _count;
        return _count + _ct.getAdded().size() - _ct.getRemoved().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Iterator iterator() {
        _iterated = true;

        IteratorChain chain = new IteratorChain();
        chain.addIterator(new FilterIterator(itr(), this));

        // note have to copy _ct.getAdded to prevent concurrent mod errors
        chain.addIterator(new ArrayList(_ct.getAdded()).iterator());
        return new Itr(chain);
    }

    /**
     * used in testing; we need to be able to make sure that OpenJPA does not
     * iterate lrs fields during standard crud operations
     */
    boolean isIterated() {
        return _iterated;
    }

    /**
     * used in testing; we need to be able to make sure that OpenJPA does not
     * iterate lrs fields during standard crud operations
     */
    void setIterated(boolean it) {
        _iterated = it;
    }

    protected Object writeReplace()
        throws ObjectStreamException {
        return asList();
    }

    /**
     * Implement this method to return an iterator over the contents of the
     * collection. This method may be invoked multiple times. The returned
     * iterator does not have to support the {@link Iterator#remove} method,
     * and may implement {@link org.apache.openjpa.lib.util.Closeable}.
     */
    protected abstract Iterator itr();

    /**
     * Return whether the collection contains the given element.
     */
    protected abstract boolean has(Object o);

    /**
     * Return the number of elements in the collection, or
     * {@link Integer#MAX_VALUE}.
     */
    protected abstract int count();

    ////////////////////////////
    // Predicate Implementation
    ////////////////////////////

    @Override
    public boolean test(Object o) {
        return !_ct.getRemoved().contains(o);
    }

    //////////////////////////////////////////
    // CollectionChangeTracker Implementation
    //////////////////////////////////////////

    @Override
    public boolean isTracking() {
        return _ct.isTracking();
    }

    @Override
    public void startTracking() {
        _ct.startTracking();
        reset();
    }

    @Override
    public void stopTracking() {
        _ct.stopTracking();
        reset();
    }

    private void reset() {
        if (_count != Integer.MAX_VALUE)
            _count = -1;
    }

    @Override
    public Collection getAdded() {
        return _ct.getAdded();
    }

    @Override
    public Collection getRemoved() {
        return _ct.getRemoved();
    }

    @Override
    public Collection getChanged() {
        return _ct.getChanged();
    }

    @Override
    public void added(Object val) {
        _ct.added(val);
    }

    @Override
    public void removed(Object val) {
        _ct.removed(val);
    }

    @Override
    public int getNextSequence() {
        return _ct.getNextSequence();
    }

    @Override
    public void setNextSequence(int seq) {
        _ct.setNextSequence(seq);
    }

    /**
     * Wrapper around our filtering iterator chain.
     */
    private class Itr
        implements Iterator, Closeable {

        private static final int OPEN = 0;
        private static final int LAST_ELEM = 1;
        private static final int CLOSED = 2;

        private final IteratorChain _itr;
        private Object _last = null;
        private int _state = OPEN;

        public Itr(IteratorChain itr) {
            _itr = itr;
        }

        @Override
        public boolean hasNext() {
            if (_state == CLOSED)
                return false;

            // close automatically if no more elements
            if (!_itr.hasNext()) {
                free();
                _state = LAST_ELEM;
                return false;
            }
            return true;
        }

        @Override
        public Object next() {
            if (_state != OPEN)
                throw new NoSuchElementException();
            _last = _itr.next();
            return _last;
        }

        @Override
        public void remove() {
            if (_state == CLOSED || _last == null)
                throw new NoSuchElementException();
            Proxies.dirty(AbstractLRSProxyCollection.this, false);
            _ct.removed(_last);
            Proxies.removed(AbstractLRSProxyCollection.this, _last, false);
            _last = null;
        }

        @Override
        public void close() {
            free();
            _state = CLOSED;
        }

        private void free() {
            if (_state != OPEN)
                return;

            for (Iterator itr = _itr; itr.hasNext();) {
                itr.next();
                if (itr instanceof FilterIterator)
                    itr = ((FilterIterator) itr).getIterator();
                ImplHelper.close(itr);
            }
        }

        @Override
        protected void finalize() {
            close();
		}
	}
}
