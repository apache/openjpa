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
package org.apache.openjpa.lib.rop;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;


/**
 * Abstract base class for random-access result lists. Unlike the
 * {@link AbstractList}, this class doesn't rely on the
 * {@link Collection#size} method.
 *
 * @author Abe White
 */
public abstract class AbstractNonSequentialResultList
    extends AbstractResultList {

    
    private static final long serialVersionUID = 1L;
    protected static final Object PAST_END = new Object();

    /**
     * Implement this method and {@link #size}. Return {@link #PAST_END}
     * if the index is out of bounds.
     */
    protected abstract Object getInternal(int index);

    @Override
    public boolean contains(Object o) {
        assertOpen();
        Object obj;
        for (int i = 0; true; i++) {
            obj = getInternal(i);
            if (obj == PAST_END)
                break;
            if (Objects.equals(o, obj))
                return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection c) {
        assertOpen();
        for (Iterator itr = c.iterator(); itr.hasNext();)
            if (!contains(itr.next()))
                return false;
        return true;
    }

    @Override
    public Object get(int index) {
        assertOpen();
        Object obj = getInternal(index);
        if (obj == PAST_END)
            throw new NoSuchElementException();
        return obj;
    }

    @Override
    public int indexOf(Object o) {
        assertOpen();
        Object obj;
        for (int i = 0; true; i++) {
            obj = getInternal(i);
            if (obj == PAST_END)
                break;
            if (Objects.equals(o, obj))
                return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        assertOpen();
        int index = -1;
        Object obj;
        for (int i = 0; true; i++) {
            obj = getInternal(i);
            if (obj == PAST_END)
                break;
            if (Objects.equals(o, obj))
                index = i;
        }
        return index;
    }

    @Override
    public boolean isEmpty() {
        assertOpen();
        return getInternal(0) == PAST_END;
    }

    @Override
    public Iterator iterator() {
        return listIterator();
    }

    @Override
    public ListIterator listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator listIterator(int index) {
        return new ResultListIterator(new Itr(index), this);
    }

    @Override
    public Object[] toArray() {
        assertOpen();
        ArrayList list = new ArrayList();
        Object obj;
        for (int i = 0; true; i++) {
            obj = getInternal(i);
            if (obj == PAST_END)
                break;
            list.add(obj);
        }
        return list.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        assertOpen();
        ArrayList list = new ArrayList();
        Object obj;
        for (int i = 0; true; i++) {
            obj = getInternal(i);
            if (obj == PAST_END)
                break;
            list.add(obj);
        }
        return list.toArray(a);
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    private class Itr extends AbstractListIterator {

        private int _idx = 0;
        private Object _next = PAST_END;

        public Itr(int index) {
            _idx = index;
        }

        @Override
        public int nextIndex() {
            return _idx;
        }

        @Override
        public int previousIndex() {
            return _idx - 1;
        }

        @Override
        public boolean hasNext() {
            _next = getInternal(_idx);
            return _next != PAST_END;
        }

        @Override
        public boolean hasPrevious() {
            return _idx > 0;
        }

        @Override
        public Object previous() {
            if (_idx == 0)
                throw new NoSuchElementException();
            return getInternal(--_idx);
        }

        @Override
        public Object next() {
            if (!hasNext())
                throw new NoSuchElementException();
            _idx++;
            return _next;
        }
    }
}
