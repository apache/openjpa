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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A basic {@link ResultList} implementation that wraps a normal list.
 *
 * @author Abe White
 */
public class ListResultList extends AbstractResultList {

    
    private static final long serialVersionUID = 1L;
    private final List _list;
    private boolean _closed = false;

    /**
     * Constructor. Supply delegate.
     */
    public ListResultList(List list) {
        _list = list;
    }

    /**
     * Return the wrapped list.
     */
    public List getDelegate() {
        return _list;
    }

    @Override
    public boolean isProviderOpen() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return _closed;
    }

    @Override
    public void close() {
        _closed = true;
    }

    @Override
    public boolean contains(Object o) {
        assertOpen();
        return _list.contains(o);
    }

    @Override
    public boolean containsAll(Collection c) {
        assertOpen();
        return _list.containsAll(c);
    }

    @Override
    public Object get(int index) {
        assertOpen();
        return _list.get(index);
    }

    @Override
    public int indexOf(Object o) {
        assertOpen();
        return _list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        assertOpen();
        return _list.lastIndexOf(o);
    }

    @Override
    public int size() {
        assertOpen();
        return _list.size();
    }

    @Override
    public boolean isEmpty() {
        assertOpen();
        return _list.isEmpty();
    }

    @Override
    public Iterator iterator() {
        return listIterator();
    }

    @Override
    public ListIterator listIterator() {
        return new ResultListIterator(_list.listIterator(), this);
    }

    @Override
    public ListIterator listIterator(int index) {
        return new ResultListIterator(_list.listIterator(index), this);
    }

    @Override
    public Object[] toArray() {
        assertOpen();
        return _list.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        assertOpen();
        return _list.toArray(a);
    }

    public Object writeReplace() {
        return _list;
    }

    @Override
    public String toString() {
    	return _list.toString();
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        assertOpen();
        return _list.subList(fromIndex, toIndex);
    }
}
