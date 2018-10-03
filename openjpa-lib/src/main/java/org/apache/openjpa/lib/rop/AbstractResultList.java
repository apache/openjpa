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
import java.util.NoSuchElementException;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Abstract base class for read-only result lists.
 *
 * @author Abe White
 */
public abstract class AbstractResultList<E> implements ResultList<E> {
    private static final long serialVersionUID = 1L;

    private transient Object _userObject;

    private static final Localizer _loc = Localizer.forPackage
        (AbstractResultList.class);

    @Override
    public void add(int index, Object element) {
        throw readOnly();
    }

    private UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException(_loc.get("read-only")
            .getMessage());
    }

    @Override
    public boolean add(Object o) {
        throw readOnly();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw readOnly();
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw readOnly();
    }

    @Override
    public E remove(int index) {
        throw readOnly();
    }

    @Override
    public boolean remove(Object o) {
        throw readOnly();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw readOnly();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw readOnly();
    }

    @Override
    public E set(int index, Object element) {
        throw readOnly();
    }

    @Override
    public void clear() {
        throw readOnly();
    }

    protected void assertOpen() {
        if (isClosed())
            throw new NoSuchElementException(_loc.get("closed").getMessage());
    }

    @Override
    public final Object getUserObject() {
        return _userObject;
    }

    @Override
    public final void setUserObject(Object opaque) {
        _userObject = opaque;
    }

}
