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
package org.apache.openjpa.kernel;

import org.apache.openjpa.meta.ClassMetaData;
import serp.util.Numbers;

////////////////////////////////////////////////////////////
// NOTE: Do not change property names; see SequenceMetaData 
// for standard property names.
////////////////////////////////////////////////////////////

/**
 * <p>A simplistic implementation of a {@link Seq} used
 * to provide datastore ids.  Starts with an id equal to the system's
 * current time in milliseconds and increments from there.</p>
 *
 * @author Greg Campbell
 */
public class TimeSeededSeq
    implements Seq {

    private long _id = System.currentTimeMillis();
    private int _increment = 1;

    public void setType(int type) {
    }

    public int getIncrement() {
        return _increment;
    }

    public void setIncrement(int increment) {
        _increment = increment;
    }

    public synchronized Object next(StoreContext ctx, ClassMetaData meta) {
        _id += _increment;
        return Numbers.valueOf(_id);
    }

    public synchronized Object current(StoreContext ctx, ClassMetaData meta) {
        return Numbers.valueOf(_id);
    }

    public void allocate(int additional, StoreContext ctx, ClassMetaData meta) {
    }

    public void close() {
    }
}
