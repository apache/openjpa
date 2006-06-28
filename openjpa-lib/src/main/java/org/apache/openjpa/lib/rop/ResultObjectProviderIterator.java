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
package org.apache.openjpa.lib.rop;

import org.apache.openjpa.lib.util.*;

import java.util.*;


/**
 *  <p>Iterator wrapped around a {@link ResultObjectProvider}.</p>
 *
 *  @author Abe White
 *  @nojavadoc */
public class ResultObjectProviderIterator implements Iterator, Closeable {
    private final ResultObjectProvider _rop;
    private Boolean _hasNext = null;
    private Boolean _open = null;

    /**
     *  Constructor.  Supply object provider.
     */
    public ResultObjectProviderIterator(ResultObjectProvider rop) {
        _rop = rop;
    }

    /**
     *  Close the underlying result object provider.
     */
    public void close() {
        if (_open == Boolean.TRUE) {
            try {
                _rop.close();
            } catch (Exception e) {
            }

            _open = Boolean.FALSE;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        if (_open == Boolean.FALSE) {
            return false;
        }

        if (_hasNext == null) {
            try {
                if (_open == null) {
                    _rop.open();
                    _open = Boolean.TRUE;
                }

                _hasNext = (_rop.next()) ? Boolean.TRUE : Boolean.FALSE;
            } catch (RuntimeException re) {
                close();
                throw re;
            } catch (Exception e) {
                close();
                _rop.handleCheckedException(e);

                return false;
            }
        }

        // close if we reach the end of the list
        if (!_hasNext.booleanValue()) {
            close();

            return false;
        }

        return true;
    }

    public Object next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        try {
            Object ret = _rop.getResultObject();
            _hasNext = null;

            return ret;
        } catch (RuntimeException re) {
            close();
            throw re;
        } catch (Exception e) {
            close();
            _rop.handleCheckedException(e);

            return null;
        }
    }

    protected void finalize() {
        close();
    }
}
