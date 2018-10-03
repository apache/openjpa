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
package org.apache.openjpa.lib.rop;

import java.util.List;

import org.apache.openjpa.lib.util.Closeable;

/**
 * A result object provider wrapped around a normal list.
 *
 * @author Abe White
 */
public class ListResultObjectProvider implements ResultObjectProvider {

    private final List _list;
    private int _idx = -1;

    /**
     * Constructor. Supply delegate.
     */
    public ListResultObjectProvider(List list) {
        _list = list;
    }

    public List getDelegate() {
        return _list;
    }

    @Override
    public boolean supportsRandomAccess() {
        return true;
    }

    @Override
    public void open() throws Exception {
    }

    @Override
    public Object getResultObject() throws Exception {
        return _list.get(_idx);
    }

    @Override
    public boolean next() throws Exception {
        return absolute(_idx + 1);
    }

    @Override
    public boolean absolute(int pos) throws Exception {
        if (pos >= 0 && pos < _list.size()) {
            _idx = pos;
            return true;
        }
        return false;
    }

    @Override
    public int size() throws Exception {
        return _list.size();
    }

    @Override
    public void reset() throws Exception {
        _idx = -1;
    }

    @Override
    public void close() throws Exception {
        if (_list instanceof Closeable)
            try {
                ((Closeable) _list).close();
            } catch (Exception e) {
            }
    }

    @Override
    public void handleCheckedException(Exception e) {
        // shouldn't ever happen
        throw new RuntimeException(e);
    }
}
