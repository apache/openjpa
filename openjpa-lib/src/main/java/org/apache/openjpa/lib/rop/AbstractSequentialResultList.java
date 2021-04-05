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

import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;


/**
 * Abstract base class for sequential result lists. Unlike the
 * {@link AbstractSequentialList}, this class doesn't rely on the
 * {@link Collection#size} method.
 *
 * @author Abe White
 */
public abstract class AbstractSequentialResultList extends AbstractResultList {

    
    private static final long serialVersionUID = 1L;

    /**
     * Implement this method and {@link #size}.
     */
    protected abstract ListIterator itr(int index);

    @Override
    public boolean contains(Object o) {
        assertOpen();
        for (Iterator itr = itr(0); itr.hasNext();)
            if (Objects.equals(o, itr.next()))
                return true;
        return false;
    }

    @Override
    public boolean containsAll(Collection c) {
        assertOpen();
        for (Object o : c)
            if (!contains(o))
                return false;
        return true;
    }

    @Override
    public Object get(int index) {
        assertOpen();
        return itr(index).next();
    }

    @Override
    public int indexOf(Object o) {
        assertOpen();
        int index = 0;
        for (Iterator itr = itr(0); itr.hasNext(); index++)
            if (Objects.equals(o, itr.next()))
                return index;
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        assertOpen();
        int index = -1;
        int i = 0;
        for (Iterator itr = itr(0); itr.hasNext(); i++)
            if (Objects.equals(o, itr.next()))
                index = i;
        return index;
    }

    @Override
    public boolean isEmpty() {
        assertOpen();
        return !itr(0).hasNext();
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
        return new ResultListIterator(itr(index), this);
    }

    @Override
    public Object[] toArray() {
        assertOpen();
        ArrayList list = new ArrayList();
        for (Iterator itr = itr(0); itr.hasNext();)
            list.add(itr.next());
        return list.toArray();
    }

    @Override
    public Object[] toArray(Object[] a) {
        assertOpen();
        ArrayList list = new ArrayList();
        for (Iterator itr = itr(0); itr.hasNext();)
            list.add(itr.next());
        return list.toArray(a);
    }
}
