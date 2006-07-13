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
package org.apache.openjpa.persistence;

import org.apache.openjpa.kernel.DelegatingSeq;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.meta.ClassMetaData;

/**
 * Represents a store sequence.
 *
 * @author Abe White
 * @since 4.0
 * @published
 */
public class Generator {

    public static final String UUID_HEX = "uuid-hex";
    public static final String UUID_STRING = "uuid-string";

    private final DelegatingSeq _seq;
    private final String _name;
    private final StoreContext _ctx;
    private final ClassMetaData _meta;

    /**
     * Constructor; supply delegate.
     */
    public Generator(Seq seq, String name, StoreContext ctx,
        ClassMetaData meta) {
        _seq = new DelegatingSeq(seq, PersistenceExceptions.TRANSLATOR);
        _name = name;
        _ctx = ctx;
        _meta = meta;
    }

    /**
     * Delegate.
     */
    public Seq getDelegate() {
        return _seq.getDelegate();
    }

    /**
     * The sequence name.
     */
    public String getName() {
        return _name;
    }

    /**
     * The next sequence value.
     */
    public Object next() {
        return _seq.next(_ctx, _meta);
    }

    /**
     * The current sequence value, or null if the sequence does not
     * support current values.
     */
    public Object current() {
        return _seq.current(_ctx, _meta);
    }

    /**
     * Hint to the sequence to allocate additional values up-front for
     * efficiency.
     */
    public void allocate(int additional) {
        _seq.allocate(additional, _ctx, _meta);
    }

    public int hashCode() {
        return _seq.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof Generator))
            return false;
        return _seq.equals(((Generator) other)._seq);
	}
}
