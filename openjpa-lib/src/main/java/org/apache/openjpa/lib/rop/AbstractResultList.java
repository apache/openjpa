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
package org.apache.openjpa.lib.rop;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Abstract base class for read-only result lists.
 *
 * @author Abe White
 * @nojavadoc
 */
public abstract class AbstractResultList implements ResultList {

    private static final Localizer _loc = Localizer.forPackage
        (AbstractResultList.class);

    public void add(int index, Object element) {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public boolean add(Object o) {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public boolean addAll(int index, Collection c) {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public Object remove(int index) {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public Object set(int index, Object element) {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public void clear() {
        throw new UnsupportedOperationException(_loc.get("read-only"));
    }

    public List subList(int from, int to) {
        throw new UnsupportedOperationException();
    }

    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    protected void assertOpen() {
        if (isClosed())
            throw new NoSuchElementException(_loc.get("closed"));
    }
}
