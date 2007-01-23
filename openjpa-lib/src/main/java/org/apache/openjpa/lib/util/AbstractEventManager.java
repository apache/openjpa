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
package org.apache.openjpa.lib.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Base event manager that handles adding/removing listeners
 * and firing events. This class is reentrant-safe; listeners can be added
 * and removed by other listeners when they receive events. The changes will
 * not be visible until the event fire that initiated the recursive sequence
 * of calls completes, however.
 *
 * @author Abe White
 */
public abstract class AbstractEventManager implements EventManager {

    private static Exception[] EMPTY_EXCEPTIONS = new Exception[0];

    private boolean _firing = false;
    private Collection _listeners = null;
    private Collection _newListeners = null;

    /**
     * Register an event listener.
     */
    public synchronized void addListener(Object listener) {
        if (listener == null)
            return;
        if (_firing) {
            if (_newListeners == null) {
                _newListeners = newListenerCollection();
                _newListeners.addAll(_listeners);
            }
            _newListeners.add(listener);
        } else {
            if (_listeners == null)
                _listeners = newListenerCollection();
            _listeners.add(listener);
        }
    }

    /**
     * Remove an event listener.
     */
    public synchronized boolean removeListener(Object listener) {
        if (listener == null)
            return false;
        if (_firing && _listeners.contains(listener)) {
            if (_newListeners == null) {
                _newListeners = newListenerCollection();
                _newListeners.addAll(_listeners);
            }
            return _newListeners.remove(listener);
        }
        return _listeners != null && _listeners.remove(listener);
    }

    /**
     * Return whether the given instance is in the list of listeners.
     */
    public synchronized boolean hasListener(Object listener) {
        return _listeners != null && _listeners.contains(listener);
    }

    /**
     * Return true if there are any registered listeners.
     */
    public synchronized boolean hasListeners() {
        return _listeners != null && !_listeners.isEmpty();
    }

    /**
     * Return a read-only list of listeners.
     */
    public synchronized Collection getListeners() {
        return (_listeners == null) ? Collections.EMPTY_LIST
            : Collections.unmodifiableCollection(_listeners);
    }

    /**
     * Fire the given event to all listeners.
     */
    public synchronized Exception[] fireEvent(Object event) {
        if (_listeners == null || _listeners.isEmpty())
            return EMPTY_EXCEPTIONS;

        boolean reentrant = _firing;
        _firing = true;
        List exceptions = null;
        for (Iterator itr = _listeners.iterator(); itr.hasNext();) {
            try {
                fireEvent(event, itr.next());
            } catch (Exception e) {
                if (exceptions == null)
                    exceptions = new LinkedList();
                exceptions.add(e);
            }
        }

        // if this wasn't a reentrant call, record that we're no longer
        // in the process of firing events and replace our initial listener
        // list with the set of new listeners
        if (!reentrant) {
            _firing = false;
            if (_newListeners != null)
                _listeners = _newListeners;
            _newListeners = null;
        }

        if (exceptions == null)
            return EMPTY_EXCEPTIONS;
        return (Exception[]) exceptions.toArray
            (new Exception[exceptions.size()]);
    }

    /**
     * Implement this method to fire the given event to the given listener.
     */
    protected abstract void fireEvent(Object event, Object listener)
        throws Exception;

    /**
     * Return a new container for listeners. Uses a linked list by default.
     */
    protected Collection newListenerCollection() {
        return new LinkedList();
    }
}
