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
package org.apache.openjpa.kernel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a savepoint where operations afterwards can be rolled
 * back and restored to this point
 *
 * @author Steve Kim
 * @since 3.4
 */
public class OpenJPASavepoint {

    private final Broker _broker;
    private final String _name;
    private final boolean _copy;
    // <StateManagerImpl, SavepointFieldManager>
    private Map _saved;

    /**
     * Constructor. Indicate whether to copy field data into memory.
     */
    public OpenJPASavepoint(Broker broker, String name, boolean copy) {
        _broker = broker;
        _name = name;
        _copy = copy;
    }

    /**
     * Return the Broker associated with this savepoint.
     */
    public Broker getBroker() {
        return _broker;
    }

    /**
     * Return the name for this savepoint.
     */
    public String getName() {
        return _name;
    }

    /**
     * Whether this savepoint copies the field values of retained instances.
     */
    public boolean getCopyFieldState() {
        return _copy;
    }

    /**
     * Return the map of states to savepoint data.
     */
    protected Map getStates() {
        return _saved;
    }

    /**
     * Set this savepoint, saving any state for the passed-in
     * {@link OpenJPAStateManager}s as necessary.
     */
    public void save(Collection states) {
        if (_saved != null)
            throw new IllegalStateException();
        _saved = new HashMap((int) (states.size() * 1.33 + 1));
        StateManagerImpl sm;
        for (Iterator i = states.iterator(); i.hasNext();) {
            sm = (StateManagerImpl) i.next();
            _saved.put(sm, new SavepointFieldManager(sm, _copy));
        }
    }

    /**
     * Release this savepoint and any associated resources. Releases
     * will happen in reverse order of creation.
     *
     * @param user if true, user initiated, otherwise a side effect of
     *             another savepoint's release/rollback
     */
    public void release(boolean user) {
        _saved = null;
    }

    /**
     * Handle the rolled back state, returning saved data.
     * Subclasses should return the collection returned from this method.
     *
     * @param previous previous savepoints set in the transaction
     */
    public Collection rollback(Collection previous) {
        Map saved;
        if (previous.isEmpty())
            saved = _saved;
        else {
            // merge all changes into one collection, allowing for later
            // SavepointFieldManagers to replace previous ones.
            saved = new HashMap();
            for (Iterator i = previous.iterator(); i.hasNext();)
                saved.putAll(((OpenJPASavepoint) i.next()).getStates());
            saved.putAll(_saved);
        }
        _saved = null;
        return saved.values();
    }
}
