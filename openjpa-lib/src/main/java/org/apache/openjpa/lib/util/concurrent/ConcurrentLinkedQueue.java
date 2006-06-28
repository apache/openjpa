/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */
package org.apache.openjpa.lib.util.concurrent;

import java.io.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An unbounded thread-safe {@linkplain Queue queue} based on linked nodes.
 * This queue orders elements FIFO (first-in-first-out).
 * The <em>head</em> of the queue is that element that has been on the
 * queue the longest time.
 * The <em>tail</em> of the queue is that element that has been on the
 * queue the shortest time. New elements
 * are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 * A <tt>ConcurrentLinkedQueue</tt> is an appropriate choice when
 * many threads will share access to a common collection.
 * This queue does not permit <tt>null</tt> elements.
 *
 * <p>This implementation employs an efficient &quot;wait-free&quot;
 * algorithm based on one described in <a
 * href="http://www.cs.rochester.edu/u/michael/PODC96.html"> Simple,
 * Fast, and Practical Non-Blocking and Blocking Concurrent Queue
 * Algorithms</a> by Maged M. Michael and Michael L. Scott.
 *
 * <p>Beware that, unlike in most collections, the <tt>size</tt> method
 * is <em>NOT</em> a constant-time operation. Because of the
 * asynchronous nature of these queues, determining the current number
 * of elements requires a traversal of the elements.
 *
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 *
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code ConcurrentLinkedQueue}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code ConcurrentLinkedQueue} in another thread.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ConcurrentLinkedQueue extends AbstractQueue implements Queue,
    java.io.Serializable {
    private static final long serialVersionUID = 196745693267521676L;
    private final Object headLock = new SerializableLock();
    private final Object tailLock = new SerializableLock();

    /**
     * Pointer to header node, initialized to a dummy node.  The first
     * actual node is at head.getNext().
     */
    private transient volatile Node head = new Node(null, null);

    /** Pointer to last node on list **/
    private transient volatile Node tail = head;

    /**
     * Creates a <tt>ConcurrentLinkedQueue</tt> that is initially empty.
     */
    public ConcurrentLinkedQueue() {
    }

    /**
     * Creates a <tt>ConcurrentLinkedQueue</tt>
     * initially containing the elements of the given collection,
     * added in traversal order of the collection's iterator.
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public ConcurrentLinkedQueue(Collection c) {
        for (Iterator it = c.iterator(); it.hasNext();)
            add(it.next());
    }

    private boolean casTail(Node cmp, Node val) {
        synchronized (tailLock) {
            if (tail == cmp) {
                tail = val;

                return true;
            } else {
                return false;
            }
        }
    }

    private boolean casHead(Node cmp, Node val) {
        synchronized (headLock) {
            if (head == cmp) {
                head = val;

                return true;
            } else {
                return false;
            }
        }
    }

    // Have to override just to update the javadoc

    /**
     * Inserts the specified element at the tail of this queue.
     *
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(Object e) {
        return offer(e);
    }

    /**
     * Inserts the specified element at the tail of this queue.
     *
     * @return <tt>true</tt> (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(Object e) {
        if (e == null) {
            throw new NullPointerException();
        }

        Node n = new Node(e, null);

        for (;;) {
            Node t = tail;
            Node s = t.getNext();

            if (t == tail) {
                if (s == null) {
                    if (t.casNext(s, n)) {
                        casTail(t, n);

                        return true;
                    }
                } else {
                    casTail(t, s);
                }
            }
        }
    }

    public Object poll() {
        for (;;) {
            Node h = head;
            Node t = tail;
            Node first = h.getNext();

            if (h == head) {
                if (h == t) {
                    if (first == null) {
                        return null;
                    } else {
                        casTail(t, first);
                    }
                } else if (casHead(h, first)) {
                    Object item = first.getItem();

                    if (item != null) {
                        first.setItem(null);

                        return item;
                    }

                    // else skip over deleted item, continue loop,
                }
            }
        }
    }

    public Object peek() { // same as poll except don't remove item

        for (;;) {
            Node h = head;
            Node t = tail;
            Node first = h.getNext();

            if (h == head) {
                if (h == t) {
                    if (first == null) {
                        return null;
                    } else {
                        casTail(t, first);
                    }
                } else {
                    Object item = first.getItem();

                    if (item != null) {
                        return item;
                    } else { // remove deleted node and continue
                        casHead(h, first);
                    }
                }
            }
        }
    }

    /**
     * Returns the first actual (non-header) node on list.  This is yet
     * another variant of poll/peek; here returning out the first
     * node, not element (so we cannot collapse with peek() without
     * introducing race.)
     */
    Node first() {
        for (;;) {
            Node h = head;
            Node t = tail;
            Node first = h.getNext();

            if (h == head) {
                if (h == t) {
                    if (first == null) {
                        return null;
                    } else {
                        casTail(t, first);
                    }
                } else {
                    if (first.getItem() != null) {
                        return first;
                    } else { // remove deleted node and continue
                        casHead(h, first);
                    }
                }
            }
        }
    }

    /**
     * Returns <tt>true</tt> if this queue contains no elements.
     *
     * @return <tt>true</tt> if this queue contains no elements
     */
    public boolean isEmpty() {
        return first() == null;
    }

    /**
     * Returns the number of elements in this queue.  If this queue
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these queues, determining the current
     * number of elements requires an O(n) traversal.
     *
     * @return the number of elements in this queue
     */
    public int size() {
        int count = 0;

        for (Node p = first(); p != null; p = p.getNext()) {
            if (p.getItem() != null) {
                // Collections.size() spec says to max out
                if (++count == Integer.MAX_VALUE) {
                    break;
                }
            }
        }

        return count;
    }

    /**
     * Returns <tt>true</tt> if this queue contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this queue contains
     * at least one element <tt>e</tt> such that <tt>o.equals(e)</tt>.
     *
     * @param o object to be checked for containment in this queue
     * @return <tt>true</tt> if this queue contains the specified element
     */
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }

        for (Node p = first(); p != null; p = p.getNext()) {
            Object item = p.getItem();

            if ((item != null) && o.equals(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element <tt>e</tt> such
     * that <tt>o.equals(e)</tt>, if this queue contains one or more such
     * elements.
     * Returns <tt>true</tt> if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     *
     * @param o element to be removed from this queue, if present
     * @return <tt>true</tt> if this queue changed as a result of the call
     */
    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }

        for (Node p = first(); p != null; p = p.getNext()) {
            Object item = p.getItem();

            if ((item != null) && o.equals(item) && p.casItem(item, null)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns an iterator over the elements in this queue in proper sequence.
     * The returned iterator is a "weakly consistent" iterator that
     * will never throw {@link ConcurrentModificationException},
     * and guarantees to traverse elements as they existed upon
     * construction of the iterator, and may (but is not guaranteed to)
     * reflect any modifications subsequent to construction.
     *
     * @return an iterator over the elements in this queue in proper sequence
     */
    public Iterator iterator() {
        return new Itr();
    }

    /**
     * Save the state to a stream (that is, serialize it).
     *
     * @serialData All of the elements (each an <tt>E</tt>) in
     * the proper order, followed by a null
     * @param s the stream
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out any hidden stuff
        s.defaultWriteObject();

        // Write out all elements in the proper order.
        for (Node p = first(); p != null; p = p.getNext()) {
            Object item = p.getItem();

            if (item != null) {
                s.writeObject(item);
            }
        }

        // Use trailing null as sentinel
        s.writeObject(null);
    }

    /**
     * Reconstitute the Queue instance from a stream (that is,
     * deserialize it).
     * @param s the stream
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in capacity, and any hidden stuff
        s.defaultReadObject();

        head = new Node(null, null);
        tail = head;

        // Read in all elements and place in queue
        for (;;) {
            Object item = s.readObject();

            if (item == null) {
                break;
            } else {
                offer(item);
            }
        }
    }

    /*
     * This is a straight adaptation of Michael & Scott algorithm.
     * For explanation, read the paper.  The only (minor) algorithmic
     * difference is that this version supports lazy deletion of
     * internal nodes (method remove(Object)) -- remove CAS'es item
     * fields to null. The normal queue operations unlink but then
     * pass over nodes with null item fields. Similarly, iteration
     * methods ignore those with nulls.
     *
     * Also note that like most non-blocking algorithms in this
     * package, this implementation relies on the fact that in garbage
     * collected systems, there is no possibility of ABA problems due
     * to recycled nodes, so there is no need to use "counted
     * pointers" or related techniques seen in versions used in
     * non-GC'ed settings.
     */
    private static class Node {
        private volatile Object item;
        private volatile Node next;

        Node(Object x) {
            item = x;
        }

        Node(Object x, Node n) {
            item = x;
            next = n;
        }

        Object getItem() {
            return item;
        }

        synchronized boolean casItem(Object cmp, Object val) {
            if (item == cmp) {
                item = val;

                return true;
            } else {
                return false;
            }
        }

        synchronized void setItem(Object val) {
            item = val;
        }

        Node getNext() {
            return next;
        }

        synchronized boolean casNext(Node cmp, Node val) {
            if (next == cmp) {
                next = val;

                return true;
            } else {
                return false;
            }
        }

        synchronized void setNext(Node val) {
            next = val;
        }
    }

    private class Itr implements Iterator {
        /**
         * Next node to return item for.
         */
        private Node nextNode;

        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         */
        private Object nextItem;

        /**
         * Node of the last returned item, to support remove.
         */
        private Node lastRet;

        Itr() {
            advance();
        }

        /**
         * Moves to next valid node and returns item to return for
         * next(), or null if no such.
         */
        private Object advance() {
            lastRet = nextNode;

            Object x = nextItem;

            Node p = (nextNode == null) ? first() : nextNode.getNext();

            for (;;) {
                if (p == null) {
                    nextNode = null;
                    nextItem = null;

                    return x;
                }

                Object item = p.getItem();

                if (item != null) {
                    nextNode = p;
                    nextItem = item;

                    return x;
                } else { // skip over nulls
                    p = p.getNext();
                }
            }
        }

        public boolean hasNext() {
            return nextNode != null;
        }

        public Object next() {
            if (nextNode == null) {
                throw new NoSuchElementException();
            }

            return advance();
        }

        public void remove() {
            Node l = lastRet;

            if (l == null) {
                throw new IllegalStateException();
            }

            // rely on a future traversal to relink.
            l.setItem(null);
            lastRet = null;
        }
    }

    private static class SerializableLock implements Serializable {
    }
}
