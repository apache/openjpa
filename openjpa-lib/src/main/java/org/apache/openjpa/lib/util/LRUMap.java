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
package org.apache.openjpa.lib.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * Extension of the commons <code>LRUMap</code> that can change its
 * maximum size.
 *
 * @author Abe White
 */
public class LRUMap extends org.apache.openjpa.lib.util.collections.LRUMap
    implements SizedMap {

    
    private static final long serialVersionUID = 1L;
    private int _max = -1;

    public LRUMap() {
    }

    public LRUMap(int initCapacity) {
        super(initCapacity);
    }

    public LRUMap(int initCapacity, float loadFactor) {
        super(initCapacity, loadFactor);
    }

    public LRUMap(Map map) {
        super(map);
    }

    @Override
    public int getMaxSize() {
        return maxSize();
    }

    @Override
    public void setMaxSize(int max) {
        if (max < 0)
            throw new IllegalArgumentException(String.valueOf(max));
        _max = max;

        Object key;
        while (size() > _max) {
            key = lastKey();
            overflowRemoved(key, remove(key));
        }
    }

    @Override
    public void overflowRemoved(Object key, Object value) {
    }

    @Override
    public int maxSize() {
        return (_max == -1) ? super.maxSize() : _max;
    }

    @Override
    public boolean isFull() {
        return (_max == -1) ? super.isFull() : size() >= _max;
    }

    @Override
    protected boolean removeLRU(LinkEntry entry) {
        overflowRemoved(entry.getKey(), entry.getValue());
        return super.removeLRU(entry);
    }

    @Override
    protected void doWriteObject(ObjectOutputStream out) throws IOException {
        out.writeInt(_max);
        super.doWriteObject(out);
    }

    @Override
    protected void doReadObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        _max = in.readInt();
        super.doReadObject(in);
    }
}
