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
package org.apache.openjpa.util;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.openjpa.conf.OpenJPAConfiguration;

/**
 * <p>Default {@link CollectionChangeTracker}.</p>
 *
 * @author Abe White
 * @nojavadoc
 */
public class CollectionChangeTrackerImpl
    extends AbstractChangeTracker
    implements CollectionChangeTracker {

    private final Collection _coll;
    private final boolean _dups;
    private final boolean _order;

    /**
     * Constructor.
     *
     * @param    coll    the collection to delegate to
     * @param    dups    true if the collection allows duplicates, false
     * otherwise
     * @param    order    true if the collection is ordered, false otherwise
     */
    public CollectionChangeTrackerImpl(Collection coll, boolean dups,
        boolean order, OpenJPAConfiguration conf) {
        super(conf);
        _coll = coll;
        _dups = dups;
        _order = order;
    }

    public void added(Object elem) {
        super.added(elem);
    }

    public void removed(Object elem) {
        super.removed(elem);
    }

    protected int initialSequence() {
        if (_order)
            return _coll.size();
        return super.initialSequence();
    }

    protected void add(Object elem) {
        if (rem == null || !rem.remove(elem)) {
            // after a point it's inefficient to keep tracking
            if (getAutoOff()
                && getAdded().size() + getRemoved().size() >= _coll.size())
                stopTracking();
            else {
                if (add == null) {
                    if (_dups || _order)
                        add = new ArrayList();
                    else
                        add = newSet();
                }
                add.add(elem);
            }
        } else if (_order)
            stopTracking();
        else {
            if (change == null)
                change = newSet();
            change.add(elem);
        }
    }

    protected void remove(Object elem) {
        // if the collection contains multiple copies of the elem, we can't
        // use change tracking because some back-ends can't just delete a
        // single copy of a elem
        if (_dups && getAutoOff() && _coll.contains(elem))
            stopTracking();
        else if (add == null || !add.remove(elem)) {
            // after a point it's inefficient to keep tracking
            if (getAutoOff()
                && getRemoved().size() + getAdded().size() >= _coll.size())
                stopTracking();
            else {
                if (rem == null)
                    rem = newSet();
                rem.add(elem);
            }
        }
    }

    protected void change(Object elem) {
        throw new InternalException();
    }
}
