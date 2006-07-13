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
package org.apache.openjpa.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataDefaults;

/**
 * Manager that can be used to track and notify listeners on lifecycle events.
 *  This class is optimized for event firing rather than for adding and
 * removing listeners, which are O(n) operations. This class also does not
 * maintain perfect set semantics for listeners; it is possible to wind up
 * having the same listener invoked multiple times for a single event if it
 * is added to this manager multiple times with different classes, or with
 * a base class and its subclass.
 *
 * @author Steve Kim
 * @author Abe White
 * @since 3.3
 * @nojavadoc
 */
public class LifecycleEventManager
    implements CallbackModes {

    private static final Exception[] EMPTY_EXCEPTIONS = new Exception[0];

    private Map _classListeners = null; // class -> listener list
    private ListenerList _listeners = null;
    private List _addListeners = new LinkedList();
    private List _remListeners = new LinkedList();
    private List _exceps = new LinkedList();
    private boolean _firing = false;
    private boolean _fail = false;

    /**
     * Register a lifecycle listener for the given classes. If the classes
     * array is null, register for all classes.
     */
    public synchronized void addListener(Object listener, Class[] classes) {
        if (listener == null)
            return;
        if (classes != null && classes.length == 0)
            return;
        if (_firing) {
            _addListeners.add(listener);
            _addListeners.add(classes);
            return;
        }

        if (classes == null) {
            if (_listeners == null)
                _listeners = new ListenerList(5);
            _listeners.add(listener);
            return;
        }

        if (_classListeners == null)
            _classListeners = new HashMap();
        ListenerList listeners;
        for (int i = 0; i < classes.length; i++) {
            listeners = (ListenerList) _classListeners.get(classes[i]);
            if (listeners == null) {
                listeners = new ListenerList(3);
                _classListeners.put(classes[i], listeners);
            }
            listeners.add(listener);
        }
    }

    /**
     * Remove the given listener.
     */
    public synchronized void removeListener(Object listener) {
        if (_firing) {
            _remListeners.add(listener);
            return;
        }

        if (_listeners != null && _listeners.remove(listener))
            return;
        if (_classListeners != null) {
            ListenerList listeners;
            for (Iterator itr = _classListeners.values().iterator();
                itr.hasNext();) {
                listeners = (ListenerList) itr.next();
                listeners.remove(listener);
            }
        }
    }

    /**
     * Return whether there are listeners or callbacks for the given source.
     */
    public boolean hasPersistListeners(Object source, ClassMetaData meta) {
        return hasCallbacks(source, meta, LifecycleEvent.BEFORE_PERSIST)
            || hasCallbacks(source, meta, LifecycleEvent.AFTER_PERSIST)
            || hasListeners(source, meta, LifecycleEvent.AFTER_PERSIST);
    }

    /**
     * Return whether there are listeners or callbacks for the given source.
     */
    public boolean hasDeleteListeners(Object source, ClassMetaData meta) {
        return hasCallbacks(source, meta, LifecycleEvent.BEFORE_DELETE)
            || hasCallbacks(source, meta, LifecycleEvent.AFTER_DELETE)
            || hasListeners(source, meta, LifecycleEvent.AFTER_DELETE);
    }

    /**
     * Return whether there are listeners or callbacks for the given source.
     */
    public boolean hasClearListeners(Object source, ClassMetaData meta) {
        return hasCallbacks(source, meta, LifecycleEvent.BEFORE_CLEAR)
            || hasCallbacks(source, meta, LifecycleEvent.AFTER_CLEAR)
            || hasListeners(source, meta, LifecycleEvent.AFTER_CLEAR);
    }

    /**
     * Return whether there are listeners or callbacks for the given source.
     */
    public boolean hasLoadListeners(Object source, ClassMetaData meta) {
        return hasCallbacks(source, meta, LifecycleEvent.AFTER_LOAD)
            || hasListeners(source, meta, LifecycleEvent.AFTER_LOAD);
    }

    /**
     * Return whether there are listeners or callbacks for the given source.
     */
    public boolean hasStoreListeners(Object source, ClassMetaData meta) {
        return hasCallbacks(source, meta, LifecycleEvent.BEFORE_STORE)
            || hasCallbacks(source, meta, LifecycleEvent.AFTER_STORE)
            || hasListeners(source, meta, LifecycleEvent.AFTER_STORE);
    }

    /**
     * Return whether there are listeners or callbacks for the given source.
     */
    public boolean hasDirtyListeners(Object source, ClassMetaData meta) {
        return hasCallbacks(source, meta, LifecycleEvent.BEFORE_DIRTY)
            || hasCallbacks(source, meta, LifecycleEvent.AFTER_DIRTY)
            || hasCallbacks(source, meta, LifecycleEvent.BEFORE_DIRTY_FLUSHED)
            || hasCallbacks(source, meta, LifecycleEvent.AFTER_DIRTY_FLUSHED)
            || hasListeners(source, meta, LifecycleEvent.AFTER_DIRTY);
    }

    /**
     * Return whether there are listeners or callbacks for the given source.
     */
    public boolean hasDetachListeners(Object source, ClassMetaData meta) {
        return hasCallbacks(source, meta, LifecycleEvent.BEFORE_DETACH)
            || hasCallbacks(source, meta, LifecycleEvent.AFTER_DETACH)
            || hasListeners(source, meta, LifecycleEvent.BEFORE_DETACH)
            || hasListeners(source, meta, LifecycleEvent.AFTER_DETACH);
    }

    /**
     * Return whether there are listeners or callbacks for the given source.
     */
    public boolean hasAttachListeners(Object source, ClassMetaData meta) {
        return hasCallbacks(source, meta, LifecycleEvent.BEFORE_ATTACH)
            || hasCallbacks(source, meta, LifecycleEvent.AFTER_ATTACH)
            || hasListeners(source, meta, LifecycleEvent.BEFORE_ATTACH)
            || hasListeners(source, meta, LifecycleEvent.AFTER_ATTACH);
    }

    /**
     * Return true if any callbacks are registered for the given source and
     * event type.
     */
    private boolean hasCallbacks(Object source, ClassMetaData meta, int type) {
        LifecycleCallbacks[] callbacks = meta.getLifecycleMetaData().
            getCallbacks(type);
        if (callbacks.length == 0)
            return false;
        for (int i = 0; i < callbacks.length; i++)
            if (callbacks[i].hasCallback(source, type))
                return true;
        return false;
    }

    /**
     * Return true if any listeners are registered for the given source and
     * event type.
     */
    private synchronized boolean hasListeners(Object source,
        ClassMetaData meta, int type) {
        if (meta.getLifecycleMetaData().getIgnoreSystemListeners())
            return false;
        boolean failFast = (meta.getRepository().getMetaDataFactory().
            getDefaults().getCallbackMode() & CALLBACK_FAIL_FAST) != 0;
        if (fireEvent(null, source, null, type, _listeners, true, failFast,
            null) == Boolean.TRUE)
            return true;
        ListenerList system = meta.getRepository().getSystemListeners();
        if (!system.isEmpty() && fireEvent(null, source, null, type, system,
            true, failFast, null) == Boolean.TRUE)
            return true;
        if (_classListeners != null) {
            Class c = source == null ? meta.getDescribedType() :
                source.getClass();
            do {
                if (fireEvent(null, source, null, type, (ListenerList)
                    _classListeners.get(c), true, failFast, null)
                    == Boolean.TRUE)
                    return true;
                c = c.getSuperclass();
            } while (c != null && c != Object.class);
        }
        return false;
    }

    /**
     * Fire lifecycle event to all registered listeners without an argument.
     */
    public Exception[] fireEvent(Object source,
        ClassMetaData meta, int type) {
        return fireEvent(source, null, meta, type);
    }

    /**
     * Fire lifecycle event to all registered listeners.
     */
    public synchronized Exception[] fireEvent(Object source, Object related,
        ClassMetaData meta, int type) {
        boolean reentrant = _firing;
        _firing = true;
        List exceptions = (reentrant) ? new LinkedList() : _exceps;
        MetaDataDefaults def = meta.getRepository().getMetaDataFactory().
            getDefaults();

        boolean callbacks = def.getCallbacksBeforeListeners(type);
        boolean failFast = (def.getCallbackMode() & CALLBACK_FAIL_FAST) != 0;

        if (callbacks)
            makeCallbacks(source, related, meta, type, failFast, exceptions);

        LifecycleEvent ev = (LifecycleEvent) fireEvent(null, source, related,
            type, _listeners, false, failFast, exceptions);

        if (_classListeners != null) {
            Class c = source == null ? meta.getDescribedType() :
                source.getClass();
            do {
                ev = (LifecycleEvent) fireEvent(ev, source, related, type,
                    (ListenerList) _classListeners.get(c), false, failFast,
                    exceptions);
                c = c.getSuperclass();
            } while (c != null && c != Object.class);
        }

        // make system listeners
        if (!meta.getLifecycleMetaData().getIgnoreSystemListeners()) {
            ListenerList system = meta.getRepository().getSystemListeners();
            fireEvent(ev, source, related, type, system, false, failFast,
                exceptions);
        }

        if (!callbacks)
            makeCallbacks(source, related, meta, type, failFast, exceptions);

        // create return array before clearing exceptions
        Exception[] ret;
        if (exceptions.isEmpty())
            ret = EMPTY_EXCEPTIONS;
        else
            ret = (Exception[]) exceptions.toArray
                (new Exception[exceptions.size()]);

        // if this wasn't a reentrant call, catch up with calls to add
        // and remove listeners made while firing
        if (!reentrant) {
            _firing = false;
            _fail = false;
            if (!_addListeners.isEmpty())
                for (Iterator itr = _addListeners.iterator(); itr.hasNext();)
                    addListener(itr.next(), (Class[]) itr.next());
            if (!_remListeners.isEmpty())
                for (Iterator itr = _remListeners.iterator(); itr.hasNext();)
                    removeListener(itr.next());
            _addListeners.clear();
            _remListeners.clear();
            _exceps.clear();
        }
        return ret;
    }

    /**
     * Make callbacks, recording any exceptions in the given collection.
     */
    private void makeCallbacks(Object source, Object related,
        ClassMetaData meta, int type, boolean failFast, Collection exceptions) {
        // make lifecycle callbacks
        LifecycleCallbacks[] callbacks = meta.getLifecycleMetaData().
            getCallbacks(type);
        for (int i = 0; !_fail && i < callbacks.length; i++) {
            try {
                callbacks[i].makeCallback(source, related, type);
            }
            catch (Exception e) {
                exceptions.add(e);
                if (failFast)
                    _fail = true;
            }
        }
    }

    /**
     * Fire an event with the given source and type to the given list of
     * listeners. The event may have already been constructed.
     */
    private Object fireEvent(LifecycleEvent ev, Object source, Object rel,
        int type, ListenerList listeners, boolean mock, boolean failFast,
        List exceptions) {
        if (listeners == null || !listeners.hasListeners(type))
            return null;

        Object listener;
        boolean responds;
        for (int i = 0, size = listeners.size(); !_fail && i < size; i++) {
            listener = listeners.get(i);
            if (size == 1)
                responds = true;
            else if (listener instanceof ListenerAdapter) {
                responds = ((ListenerAdapter) listener).respondsTo(type);
                if (!responds)
                    continue;
            } else
                responds = false;

            try {
                switch (type) {
                    case LifecycleEvent.BEFORE_CLEAR:
                    case LifecycleEvent.AFTER_CLEAR:
                        if (responds || listener instanceof ClearListener) {
                            if (mock)
                                return Boolean.TRUE;
                            if (ev == null)
                                ev = new LifecycleEvent(source, type);
                            if (type == LifecycleEvent.BEFORE_CLEAR)
                                ((ClearListener) listener).beforeClear(ev);
                            else
                                ((ClearListener) listener).afterClear(ev);
                        }
                        break;
                    case LifecycleEvent.BEFORE_PERSIST:
                    case LifecycleEvent.AFTER_PERSIST:
                        if (responds || listener instanceof PersistListener) {
                            if (mock)
                                return Boolean.TRUE;
                            if (ev == null)
                                ev = new LifecycleEvent(source, type);
                            if (type == LifecycleEvent.BEFORE_PERSIST)
                                ((PersistListener) listener).beforePersist(ev);
                            else
                                ((PersistListener) listener).afterPersist(ev);
                        }
                        break;
                    case LifecycleEvent.BEFORE_DELETE:
                    case LifecycleEvent.AFTER_DELETE:
                        if (responds || listener instanceof DeleteListener) {
                            if (mock)
                                return Boolean.TRUE;
                            if (ev == null)
                                ev = new LifecycleEvent(source, type);
                            if (type == LifecycleEvent.BEFORE_DELETE)
                                ((DeleteListener) listener).beforeDelete(ev);
                            else
                                ((DeleteListener) listener).afterDelete(ev);
                        }
                        break;
                    case LifecycleEvent.BEFORE_DIRTY:
                    case LifecycleEvent.AFTER_DIRTY:
                    case LifecycleEvent.BEFORE_DIRTY_FLUSHED:
                    case LifecycleEvent.AFTER_DIRTY_FLUSHED:
                        if (responds || listener instanceof DirtyListener) {
                            if (mock)
                                return Boolean.TRUE;
                            if (ev == null)
                                ev = new LifecycleEvent(source, type);
                            switch (type) {
                                case LifecycleEvent.BEFORE_DIRTY:
                                    ((DirtyListener) listener).beforeDirty(ev);
                                    break;
                                case LifecycleEvent.AFTER_DIRTY:
                                    ((DirtyListener) listener).afterDirty(ev);
                                    break;
                                case LifecycleEvent.BEFORE_DIRTY_FLUSHED:
                                    ((DirtyListener) listener)
                                        .beforeDirtyFlushed(ev);
                                    break;
                                case LifecycleEvent.AFTER_DIRTY_FLUSHED:
                                    ((DirtyListener) listener)
                                        .afterDirtyFlushed(ev);
                                    break;
                            }
                        }
                        break;
                    case LifecycleEvent.AFTER_LOAD:
                    case LifecycleEvent.AFTER_REFRESH:
                        if (responds || listener instanceof LoadListener) {
                            if (mock)
                                return Boolean.TRUE;
                            if (ev == null)
                                ev = new LifecycleEvent(source, type);
                            if (type == LifecycleEvent.AFTER_LOAD)
                                ((LoadListener) listener).afterLoad(ev);
                            else
                                ((LoadListener) listener).afterRefresh(ev);
                        }
                        break;
                    case LifecycleEvent.BEFORE_STORE:
                    case LifecycleEvent.AFTER_STORE:
                        if (responds || listener instanceof StoreListener) {
                            if (mock)
                                return Boolean.TRUE;
                            if (ev == null)
                                ev = new LifecycleEvent(source, type);
                            if (type == LifecycleEvent.BEFORE_STORE)
                                ((StoreListener) listener).beforeStore(ev);
                            else
                                ((StoreListener) listener).afterStore(ev);
                        }
                        break;
                    case LifecycleEvent.BEFORE_DETACH:
                    case LifecycleEvent.AFTER_DETACH:
                        if (responds || listener instanceof DetachListener) {
                            if (mock)
                                return Boolean.TRUE;
                            if (ev == null)
                                ev = new LifecycleEvent(source, rel, type);
                            if (type == LifecycleEvent.BEFORE_DETACH)
                                ((DetachListener) listener).beforeDetach(ev);
                            else
                                ((DetachListener) listener).afterDetach(ev);
                        }
                        break;
                    case LifecycleEvent.BEFORE_ATTACH:
                    case LifecycleEvent.AFTER_ATTACH:
                        if (responds || listener instanceof AttachListener) {
                            if (mock)
                                return Boolean.TRUE;
                            if (ev == null)
                                ev = new LifecycleEvent(source, rel, type);
                            if (type == LifecycleEvent.BEFORE_ATTACH)
                                ((AttachListener) listener).beforeAttach(ev);
                            else
                                ((AttachListener) listener).afterAttach(ev);
                        }
                        break;
                }
            }
            catch (Exception e) {
                exceptions.add(e);
                if (failFast)
                    _fail = true;
            }
        }
        return ev;
    }

    /**
     * Interface that facades to other lifecycle listener interfaces can
     * implement to choose which events to respond to based on their delegate.
     * This is more efficient than registering as a listener for all events
     * but only responding to some.
     */
    public static interface ListenerAdapter {

        /**
         * Return whether this instance responds to the given event type from
         * {@link LifecycleEvent}.
         */
        public boolean respondsTo(int eventType);
    }

    /**
     * Extended list that tracks what event types its elements care about.
     * Maintains set semantics as well.
     */
    public static class ListenerList
        extends ArrayList {

        private int _types = 0;

        public ListenerList(int size) {
            super(size);
        }

        public ListenerList(ListenerList copy) {
            super(copy);
            _types = copy._types;
        }

        public boolean hasListeners(int type) {
            return (_types & (2 << type)) > 0;
        }

        public boolean add(Object listener) {
            if (contains(listener))
                return false;
            super.add(listener);
            _types |= getEventTypes(listener);
            return true;
        }

        public boolean remove(Object listener) {
            if (!super.remove(listener))
                return false;

            // recompute types mask
            _types = 0;
            for (int i = 0; i < size(); i++)
                _types |= getEventTypes(get(i));
            return true;
        }

        /**
         * Return a mask of the event types the given listener processes.
         */
        private static int getEventTypes(Object listener) {
            int types = 0;
            if (listener instanceof ListenerAdapter) {
                ListenerAdapter adapter = (ListenerAdapter) listener;
                for (int i = 0; i < LifecycleEvent.ALL_EVENTS.length; i++)
                    if (adapter.respondsTo(LifecycleEvent.ALL_EVENTS[i]))
                        types |= 2 << LifecycleEvent.ALL_EVENTS[i];
                return types;
            }

            if (listener instanceof PersistListener)
                types |= 2 << LifecycleEvent.AFTER_PERSIST;
            if (listener instanceof ClearListener) {
                types |= 2 << LifecycleEvent.BEFORE_CLEAR;
                types |= 2 << LifecycleEvent.AFTER_CLEAR;
            }
            if (listener instanceof DeleteListener) {
                types |= 2 << LifecycleEvent.BEFORE_DELETE;
                types |= 2 << LifecycleEvent.AFTER_DELETE;
            }
            if (listener instanceof DirtyListener) {
                types |= 2 << LifecycleEvent.BEFORE_DIRTY;
                types |= 2 << LifecycleEvent.AFTER_DIRTY;
                types |= 2 << LifecycleEvent.BEFORE_DIRTY_FLUSHED;
                types |= 2 << LifecycleEvent.AFTER_DIRTY_FLUSHED;
            }
            if (listener instanceof LoadListener)
                types |= 2 << LifecycleEvent.AFTER_LOAD;
            if (listener instanceof StoreListener) {
                types |= 2 << LifecycleEvent.BEFORE_STORE;
                types |= 2 << LifecycleEvent.AFTER_STORE;
            }
            if (listener instanceof DetachListener) {
                types |= 2 << LifecycleEvent.BEFORE_DETACH;
                types |= 2 << LifecycleEvent.AFTER_DETACH;
            }
            if (listener instanceof AttachListener) {
                types |= 2 << LifecycleEvent.BEFORE_ATTACH;
                types |= 2 << LifecycleEvent.AFTER_ATTACH;
			}
			return types;
		}
	}
}
