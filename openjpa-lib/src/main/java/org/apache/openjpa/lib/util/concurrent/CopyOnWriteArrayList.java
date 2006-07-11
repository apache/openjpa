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
/*
 * Written by Dawid Kurzyniec, on the basis of public specifications and
 * public domain sources from JSR 166, and released to the public domain,
 * as explained at http://creativecommons.org/licenses/publicdomain.
 */
package org.apache.openjpa.lib.util.concurrent;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class CopyOnWriteArrayList implements List, Cloneable, Serializable {

    private static final long serialVersionUID = 8673264195747942595L;

    private volatile transient Object[] array;

    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }

    public CopyOnWriteArrayList(Collection c) {
        // must deal with concurrent collections
        Object[] array = c.toArray();
        // make sure the array is Object[] type
        if (array.getClass() != Object[].class) {
            array = Arrays.copyOf(array, array.length, Object[].class);
        }
        // assume that c.toArray() has returned a new array instance, as
        // required by the spec
        setArray(array);
    }

    public CopyOnWriteArrayList(Object[] array) {
        setArray(Arrays.copyOf(array, array.length, Object[].class));
    }

    final Object[] getArray() {
        return array;
    }

    final void setArray(Object[] array) {
        this.array = array;
    }

    public int size() {
        return getArray().length;
    }

    public boolean isEmpty() {
        return getArray().length == 0;
    }

    private static int search(Object[] array, Object subject, int pos,
        int end) {
        if (subject == null) {
            for (; pos < end; pos++) {
                if (array[pos] == null) return pos;
            }
        } else {
            for (; pos < end; pos++) {
                if (subject.equals(array[pos])) return pos;
            }
        }
        return -1;
    }

    private static int reverseSearch(Object[] array, Object subject, int start,
        int pos) {
        if (subject == null) {
            for (pos--; pos >= start; pos--) {
                if (array[pos] == null) return pos;
            }
        } else {
            for (pos--; pos >= start; pos--) {
                if (subject.equals(array[pos])) return pos;
            }
        }
        return -1;
    }

    public boolean contains(Object o) {
        Object[] array = getArray();
        return search(array, o, 0, array.length) >= 0;
    }

    public Iterator iterator() {
        return new COWIterator(getArray(), 0);
    }

    public Object[] toArray() {
        Object[] array = getArray();
        return Arrays.copyOf(array, array.length, Object[].class);
    }

    public Object[] toArray(Object[] a) {
        Object[] array = getArray();
        int length = array.length;
        if (a.length < length) {
            return Arrays.copyOf(array, length, a.getClass());
        } else {
            System.arraycopy(array, 0, a, 0, length);
            if (a.length > length) a[length] = null;
            return a;
        }
    }

    public boolean add(Object o) {
        synchronized (this) {
            Object[] oldarr = getArray();
            int length = oldarr.length;
            Object[] newarr = new Object[length + 1];
            System.arraycopy(oldarr, 0, newarr, 0, length);
            newarr[length] = o;
            setArray(newarr);
            return true;
        }
    }

    public boolean addIfAbsent(Object o) {
        synchronized (this) {
            Object[] oldarr = getArray();
            int length = oldarr.length;
            if (search(array, o, 0, length) >= 0) return false;
            Object[] newarr = new Object[length + 1];
            System.arraycopy(oldarr, 0, newarr, 0, length);
            newarr[length] = o;
            setArray(newarr);
            return true;
        }
    }

    public int addAllAbsent(Collection c) {
        Object[] arr = c.toArray();
        if (arr.length == 0) return 0;
        synchronized (this) {
            Object[] oldarr = getArray();
            int oldlength = oldarr.length;
            Object[] tmp = new Object[arr.length];
            int added = 0;
            for (int i = 0; i < arr.length; i++) {
                Object o = arr[i];
                if (search(oldarr, o, 0, oldlength) < 0
                    && search(tmp, o, 0, added) < 0) {
                    tmp[added++] = o;
                }
            }
            if (added == 0) return 0;
            Object[] newarr = new Object[oldlength + added];
            System.arraycopy(oldarr, 0, newarr, 0, oldlength);
            System.arraycopy(tmp, 0, newarr, oldlength, added);
            setArray(newarr);
            return added;
        }
    }

    public boolean remove(Object o) {
        synchronized (this) {
            Object[] array = getArray();
            int length = array.length;
            int pos = search(array, o, 0, length);
            if (pos < 0) return false;
            Object[] newarr = new Object[length - 1];
            int moved = length - pos - 1;
            if (pos > 0) System.arraycopy(array, 0, newarr, 0, pos);
            if (moved > 0) System.arraycopy(array, pos + 1, newarr, pos, moved);
            setArray(newarr);
            return true;
        }
    }

    public boolean containsAll(Collection c) {
        Object[] array = getArray();
        for (Iterator itr = c.iterator(); itr.hasNext();) {
            if (search(array, itr.next(), 0, array.length) < 0) return false;
        }
        return true;
    }

    public boolean addAll(Collection c) {
        // must deal with concurrent collections
        Object[] ca = c.toArray();
        if (ca.length == 0) return false;
        synchronized (this) {
            Object[] oldarr = getArray();
            int length = oldarr.length;
            Object[] newarr = new Object[length + ca.length];
            System.arraycopy(oldarr, 0, newarr, 0, length);
            int pos = length;
            System.arraycopy(ca, 0, newarr, pos, ca.length);
            setArray(newarr);
            return true;
        }
    }

    public boolean addAll(int index, Collection c) {
        // must deal with concurrent collections
        Object[] ca = c.toArray();
        synchronized (this) {
            Object[] oldarr = getArray();
            int length = oldarr.length;
            if (index < 0 || index > length) {
                throw new IndexOutOfBoundsException("Index: " + index +
                    ", Size: " + length);
            }
            if (ca.length == 0) return false;
            Object[] newarr = new Object[length + ca.length];
            int moved = length - index;
            System.arraycopy(oldarr, 0, newarr, 0, index);
            int pos = length;
            System.arraycopy(ca, 0, newarr, index, ca.length);
            if (moved > 0) {
                System
                    .arraycopy(oldarr, index, newarr, index + ca.length, moved);
            }
            setArray(newarr);
            return true;
        }
    }

    public boolean removeAll(Collection c) {
        if (c.isEmpty()) return false;
        synchronized (this) {
            Object[] array = getArray();
            int length = array.length;
            Object[] tmp = new Object[length];
            int newlen = 0;
            for (int i = 0; i < length; i++) {
                Object o = array[i];
                if (!c.contains(o)) tmp[newlen++] = o;
            }
            if (newlen == length) return false;
            Object[] newarr = new Object[newlen];
            System.arraycopy(tmp, 0, newarr, 0, newlen);
            setArray(newarr);
            return true;
        }
    }

    public boolean retainAll(Collection c) {
        synchronized (this) {
            Object[] array = getArray();
            int length = array.length;
            Object[] tmp = new Object[length];
            int newlen = 0;
            for (int i = 0; i < length; i++) {
                Object o = array[i];
                if (c.contains(o)) tmp[newlen++] = o;
            }
            if (newlen == length) return false;
            Object[] newarr = new Object[newlen];
            System.arraycopy(tmp, 0, newarr, 0, newlen);
            setArray(newarr);
            return true;
        }
    }

    public void clear() {
        setArray(new Object[0]);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof List)) return false;

        ListIterator itr = ((List) o).listIterator();
        Object[] array = getArray();
        int length = array.length;
        int idx = 0;
        while (idx < length && itr.hasNext()) {
            Object o1 = array[idx++];
            Object o2 = itr.next();
            if (!eq(o1, o2)) return false;
        }
        return (idx == length && !itr.hasNext());
    }

    public int hashCode() {
        int hashCode = 1;
        Object[] array = getArray();
        int length = array.length;
        for (int i = 0; i < length; i++) {
            Object o = array[i];
            hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
        }
        return hashCode;
    }

    public Object get(int index) {
        return getArray()[index];
    }

    public Object set(int index, Object element) {
        synchronized (this) {
            Object[] oldarr = getArray();
            int length = oldarr.length;
            // piggyback the array bounds check
            Object oldVal = oldarr[index];
            if (oldVal == element) {
                setArray(oldarr);
            } else {
                Object[] newarr = new Object[length];
                System.arraycopy(oldarr, 0, newarr, 0, length);
                newarr[index] = element;
                setArray(newarr);
            }
            return oldVal;
        }
    }

    public void add(int index, Object element) {
        synchronized (this) {
            Object[] oldarr = getArray();
            int length = oldarr.length;
            if (index < 0 || index > length) {
                throw new IndexOutOfBoundsException("Index: " + index +
                    ", Size: " + length);
            }
            Object[] newarr = new Object[length + 1];
            int moved = length - index;
            System.arraycopy(oldarr, 0, newarr, 0, index);
            newarr[index] = element;
            if (moved > 0) {
                System.arraycopy(oldarr, index, newarr, index + 1, moved);
            }
            setArray(newarr);
        }
    }

    public Object remove(int index) {
        synchronized (this) {
            Object[] array = getArray();
            int length = array.length;
            if (index < 0 || index >= length) {
                throw new IndexOutOfBoundsException("Index: " + index +
                    ", Size: " + length);
            }
            Object result = array[index];
            Object[] newarr = new Object[length - 1];
            int moved = length - index - 1;
            if (index > 0) System.arraycopy(array, 0, newarr, 0, index);
            if (moved > 0)
                System.arraycopy(array, index + 1, newarr, index, moved);
            setArray(newarr);
            return result;
        }
    }

    public int indexOf(Object o) {
        Object[] array = getArray();
        return search(array, o, 0, array.length);
    }

    public int indexOf(Object o, int index) {
        Object[] array = getArray();
        return search(array, o, index, array.length);
    }

    public int lastIndexOf(Object o) {
        Object[] array = getArray();
        return reverseSearch(array, o, 0, array.length);
    }

    public int lastIndexOf(Object o, int index) {
        Object[] array = getArray();
        return reverseSearch(array, o, 0, index);
    }

    public ListIterator listIterator() {
        return new COWIterator(getArray(), 0);
    }

    public ListIterator listIterator(int index) {
        Object[] array = getArray();
        if (index < 0 || index > array.length) {
            throw new IndexOutOfBoundsException("Index: " + index +
                ", Size: " + array.length);
        }
        return new COWIterator(array, index);
    }

    public List subList(int fromIndex, int toIndex) {
        Object[] array = getArray();
        if (fromIndex < 0 || toIndex > array.length || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }
        return new COWSubList(fromIndex, toIndex - fromIndex);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        Object[] array = getArray();
        int length = array.length;
        out.writeInt(length);
        for (int i = 0; i < length; i++)
            out.writeObject(array[i]);
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int length = in.readInt();
        Object[] array = new Object[length];
        for (int i = 0; i < length; i++) {
            array[i] = in.readObject();
        }
        setArray(array);
    }

    public String toString() {
        Object[] array = getArray();
        int length = array.length;
        StringBuffer buf = new StringBuffer();
        buf.append('[');
        for (int i = 0; i < length; i++) {
            if (i > 0) buf.append(", ");
            buf.append(array[i]);
        }
        buf.append(']');
        return buf.toString();
    }

    static class COWIterator implements ListIterator {

        final Object[] array;
        int cursor;

        COWIterator(Object[] array, int cursor) {
            this.array = array;
            this.cursor = cursor;
        }

        public boolean hasNext() {
            return cursor < array.length;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        public int nextIndex() {
            return cursor;
        }

        public Object next() {
            try {
                return array[cursor++];
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
            // todo: should decrement cursor on failure?...
        }

        public int previousIndex() {
            return cursor - 1;
        }

        public Object previous() {
            try {
                return array[--cursor];
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
            // todo: should decrement cursor on failure?...
        }

        public void add(Object val) {
            throw new UnsupportedOperationException();
        }

        public void set(Object val) {
            throw new UnsupportedOperationException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    class COWSubList implements Serializable, List {

        final int offset;
        int length;
        Object[] expectedArray;

        COWSubList(int offset, int length) {
            this.offset = offset;
            this.length = length;
            this.expectedArray = getArray();
        }

        public int size() {
            return length;
        }

        public boolean isEmpty() {
            return length == 0;
        }

        public boolean contains(Object o) {
            return search(getArray(), o, offset, offset + length) >= 0;
        }

        public Iterator iterator() {
            return listIterator();
        }

        public Object[] toArray() {
            Object[] array = getArray();
            Object[] newarr = new Object[length];
            System.arraycopy(array, offset, newarr, 0, length);
            return newarr;
        }

        public Object[] toArray(Object[] a) {
            Object[] array = getArray();
            if (a.length < length) {
                a = (Object[]) Array
                    .newInstance(a.getClass().getComponentType(),
                        length);
                System.arraycopy(array, offset, a, 0, length);
            } else {
                System.arraycopy(array, offset, a, 0, length);
                if (a.length > length) a[length] = null;
            }
            return a;
        }

        public boolean add(Object o) {
            add(length, o);
            return true;
        }

        public boolean remove(Object o) {
            synchronized (CopyOnWriteArrayList.this) {
                Object[] array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                int fullLength = array.length;
                int pos = search(array, o, offset, length);
                if (pos < 0) return false;
                Object[] newarr = new Object[fullLength - 1];
                int moved = length - pos - 1;
                if (pos > 0)
                    System.arraycopy(array, 0, newarr, 0, pos);
                if (moved > 0)
                    System.arraycopy(array, pos + 1, newarr, pos, moved);
                setArray(newarr);
                expectedArray = newarr;
                length--;
                return true;
            }
        }

        public boolean containsAll(Collection c) {
            Object[] array = getArray();
            for (Iterator itr = c.iterator(); itr.hasNext();) {
                if (search(array, itr.next(), offset, length) < 0) return false;
            }
            return true;
        }

        public boolean addAll(Collection c) {
            return addAll(length, c);
        }

        public boolean addAll(int index, Collection c) {
            int added = c.size();
            synchronized (CopyOnWriteArrayList.this) {
                if (index < 0 || index >= length) {
                    throw new IndexOutOfBoundsException("Index: " + index +
                        ", Size: " + length);
                }
                Object[] oldarr = getArray();
                if (oldarr != expectedArray)
                    throw new ConcurrentModificationException();
                if (added == 0) return false;
                int fullLength = oldarr.length;
                Object[] newarr = new Object[fullLength + added];
                int pos = offset + index;
                int newpos = pos;
                System.arraycopy(oldarr, 0, newarr, 0, pos);
                int rem = fullLength - pos;
                for (Iterator itr = c.iterator(); itr.hasNext();) {
                    newarr[newpos++] = itr.next();
                }
                if (rem > 0) System.arraycopy(oldarr, pos, newarr, newpos, rem);
                setArray(newarr);
                expectedArray = newarr;
                length += added;
                return true;
            }
        }

        public boolean removeAll(Collection c) {
            if (c.isEmpty()) return false;
            synchronized (CopyOnWriteArrayList.this) {
                Object[] array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                int fullLength = array.length;
                Object[] tmp = new Object[length];
                int retained = 0;
                for (int i = offset; i < offset + length; i++) {
                    Object o = array[i];
                    if (!c.contains(o)) tmp[retained++] = o;
                }
                if (retained == length) return false;
                Object[] newarr = new Object[fullLength + retained - length];
                int moved = fullLength - offset - length;
                if (offset > 0) System.arraycopy(array, 0, newarr, 0, offset);
                if (retained > 0)
                    System.arraycopy(tmp, 0, newarr, offset, retained);
                if (moved > 0)
                    System.arraycopy(array, offset + length, newarr,
                        offset + retained, moved);
                setArray(newarr);
                expectedArray = newarr;
                length = retained;
                return true;
            }
        }

        public boolean retainAll(Collection c) {
            synchronized (CopyOnWriteArrayList.this) {
                Object[] array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                int fullLength = array.length;
                Object[] tmp = new Object[length];
                int retained = 0;
                for (int i = offset; i < offset + length; i++) {
                    Object o = array[i];
                    if (c.contains(o)) tmp[retained++] = o;
                }
                if (retained == length) return false;
                Object[] newarr = new Object[fullLength + retained - length];
                int moved = fullLength - offset - length;
                if (offset > 0)
                    System.arraycopy(array, 0, newarr, 0, offset);
                if (retained > 0)
                    System.arraycopy(tmp, 0, newarr, offset, retained);
                if (moved > 0)
                    System.arraycopy(array, offset + length, newarr,
                        offset + retained, moved);
                setArray(newarr);
                expectedArray = newarr;
                length = retained;
                return true;
            }
        }

        public void clear() {
            synchronized (CopyOnWriteArrayList.this) {
                Object[] array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                int fullLength = array.length;
                Object[] newarr = new Object[fullLength - length];
                int moved = fullLength - offset - length;
                if (offset > 0) System.arraycopy(array, 0, newarr, 0, offset);
                if (moved > 0)
                    System.arraycopy(array, offset + length, newarr, offset,
                        moved);
                setArray(newarr);
                expectedArray = newarr;
                length = 0;
            }
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof List)) return false;
            Object[] array;
            int last;
            synchronized (CopyOnWriteArrayList.this) {
                array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                last = offset + length;
            }
            ListIterator itr = ((List) o).listIterator();
            int idx = offset;
            while (idx < last && itr.hasNext()) {
                Object o1 = array[idx];
                Object o2 = itr.next();
                if (!eq(o1, o2)) return false;
            }
            return (idx == last && !itr.hasNext());
        }

        public int hashCode() {
            int hashCode = 1;
            Object[] array;
            int last;
            synchronized (CopyOnWriteArrayList.this) {
                array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                last = offset + length;
            }
            for (int i = offset; i < last; i++) {
                Object o = array[i];
                hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
            }
            return hashCode;
        }

        public Object get(int index) {
            return getArray()[offset + index];
        }

        public Object set(int index, Object element) {
            synchronized (CopyOnWriteArrayList.this) {
                if (index < 0 || index >= length) {
                    throw new IndexOutOfBoundsException("Index: " + index +
                        ", Size: " + length);
                }
                Object[] oldarr = getArray();
                if (oldarr != expectedArray)
                    throw new ConcurrentModificationException();
                int fullLength = oldarr.length;
                // piggyback the array bounds check
                Object oldVal = oldarr[offset + index];
                if (oldVal == element) {
                    setArray(oldarr);
                } else {
                    Object[] newarr = new Object[fullLength];
                    System.arraycopy(oldarr, 0, newarr, 0, fullLength);
                    newarr[offset + index] = element;
                    setArray(newarr);
                    expectedArray = newarr;
                }
                return oldVal;
            }
        }

        public void add(int index, Object element) {
            synchronized (CopyOnWriteArrayList.this) {
                if (index < 0 || index > length) {
                    throw new IndexOutOfBoundsException("Index: " + index +
                        ", Size: " + length);
                }
                Object[] oldarr = getArray();
                if (oldarr != expectedArray)
                    throw new ConcurrentModificationException();
                int fullLength = oldarr.length;
                Object[] newarr = new Object[fullLength + 1];
                int pos = offset + index;
                int moved = fullLength - pos;
                System.arraycopy(oldarr, 0, newarr, 0, pos);
                newarr[pos] = element;
                if (moved > 0) {
                    System.arraycopy(oldarr, pos, newarr, pos + 1, moved);
                }
                setArray(newarr);
                expectedArray = newarr;
                length++;
            }
        }

        public Object remove(int index) {
            synchronized (CopyOnWriteArrayList.this) {
                if (index < 0 || index >= length) {
                    throw new IndexOutOfBoundsException("Index: " + index +
                        ", Size: " + length);
                }
                Object[] array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                int fullLength = array.length;
                int pos = offset + index;
                Object result = array[pos];
                Object[] newarr = new Object[fullLength - 1];
                int moved = fullLength - pos - 1;
                if (index > 0)
                    System.arraycopy(array, 0, newarr, 0, pos);
                if (moved > 0)
                    System.arraycopy(array, pos + 1, newarr, pos, moved);
                setArray(newarr);
                expectedArray = newarr;
                length--;
                return result;
            }
        }

        public int indexOf(Object o) {
            int pos = search(getArray(), o, offset, offset + length);
            return pos >= 0 ? pos - offset : -1;
        }

        public int indexOf(Object o, int index) {
            int pos =
                search(getArray(), o, offset + index, offset + length) - offset;
            return pos >= 0 ? pos - offset : -1;
        }

        public int lastIndexOf(Object o) {
            int pos = reverseSearch(getArray(), o, offset, offset + length)
                - offset;
            return pos >= 0 ? pos - offset : -1;
        }

        public int lastIndexOf(Object o, int index) {
            int pos =
                reverseSearch(getArray(), o, offset, offset + index) - offset;
            return pos >= 0 ? pos - offset : -1;
        }

        public ListIterator listIterator() {
            // must synchronize to atomically obtain the array and length
            synchronized (CopyOnWriteArrayList.this) {
                Object[] array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                return new COWSubIterator(array, offset, offset + length,
                    offset);
            }
        }

        public ListIterator listIterator(int index) {
            // must synchronize to atomically obtain the array and length
            synchronized (CopyOnWriteArrayList.this) {
                if (index < 0 || index >= length) {
                    throw new IndexOutOfBoundsException("Index: " + index +
                        ", Size: " + length);
                }
                Object[] array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                return new COWSubIterator(array, offset, offset + length,
                    offset + index);
            }
        }

        public List subList(int fromIndex, int toIndex) {
            if (fromIndex < 0 || toIndex > length || fromIndex > toIndex) {
                throw new IndexOutOfBoundsException();
            }
            return new COWSubList(offset + fromIndex, toIndex - fromIndex);
        }

        public String toString() {
            Object[] array;
            int last;
            synchronized (CopyOnWriteArrayList.this) {
                array = getArray();
                if (array != expectedArray)
                    throw new ConcurrentModificationException();
                last = offset + length;
            }
            StringBuffer buf = new StringBuffer();
            buf.append('[');
            for (int i = offset; i < last; i++) {
                if (i > offset) buf.append(", ");
                buf.append(array[i]);
            }
            buf.append(']');
            return buf.toString();
        }
    }

    static class COWSubIterator implements ListIterator {

        final Object[] array;
        int cursor;
        int first, last;

        COWSubIterator(Object[] array, int first, int last, int cursor) {
            this.array = array;
            this.first = first;
            this.last = last;
            this.cursor = cursor;
        }

        public boolean hasNext() {
            return cursor < last;
        }

        public boolean hasPrevious() {
            return cursor > first;
        }

        public int nextIndex() {
            return cursor - first;
        }

        public Object next() {
            if (cursor == last) throw new NoSuchElementException();
            return array[cursor++];
        }

        public int previousIndex() {
            return cursor - first - 1;
        }

        public Object previous() {
            if (cursor == first) throw new NoSuchElementException();
            return array[--cursor];
        }

        public void add(Object val) {
            throw new UnsupportedOperationException();
        }

        public void set(Object val) {
            throw new UnsupportedOperationException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean eq(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }
}
