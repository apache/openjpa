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
package org.apache.openjpa.lib.util;

import java.io.*;

import java.util.*;


/**
 *  <p>Extension of the commons <code>LRUMap</code> that can change its
 *  maximum size.</p>
 *
 *  @author Abe White
 *  @nojavadoc */
public class LRUMap extends org.apache.commons.collections.map.LRUMap
    implements SizedMap {
    private int _max = -1;

    public LRUMap() {
    }

    public LRUMap(int maxSize) {
        super(maxSize);
    }

    public LRUMap(int maxSize, float loadFactor) {
        super(maxSize, loadFactor);
    }

    public LRUMap(Map map) {
        super(map);
    }

    public int getMaxSize() {
        return maxSize();
    }

    public void setMaxSize(int max) {
        if (max < 0) {
            throw new IllegalArgumentException(String.valueOf(max));
        }

        _max = max;

        Object key;

        while (size() > _max) {
            key = lastKey();
            overflowRemoved(key, remove(key));
        }
    }

    public void overflowRemoved(Object key, Object value) {
    }

    public int maxSize() {
        return (_max == -1) ? super.maxSize() : _max;
    }

    public boolean isFull() {
        return (_max == -1) ? super.isFull() : (size() >= _max);
    }

    protected boolean removeLRU(LinkEntry entry) {
        overflowRemoved(entry.getKey(), entry.getValue());

        return super.removeLRU(entry);
    }

    protected void doWriteObject(ObjectOutputStream out)
        throws IOException {
        out.writeInt(_max);
        super.doWriteObject(out);
    }

    protected void doReadObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        _max = in.readInt();
        super.doReadObject(in);
    }
}
