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
package org.apache.openjpa.util;

import java.io.Serializable;

import org.apache.openjpa.lib.util.ReferenceHashSet;
import org.apache.openjpa.lib.util.ReferenceMap;
import org.apache.openjpa.lib.util.concurrent.ConcurrentReferenceHashMap;

/**
 * Identity class extended by builtin OpenJPA identity objects.
 *
 * @author Steve Kim
 */
public abstract class OpenJPAId
    implements Comparable, Serializable {

    protected Class type;
    protected boolean subs = true;

    // type has his based on the least-derived non-object class so that
    // user-given ids with non-exact types match ids with exact types
    private transient int _typeHash = 0;
    // cache the types' generated hashcodes
    private static ConcurrentReferenceHashMap _typeCache =
        new ConcurrentReferenceHashMap(ReferenceMap.HARD, ReferenceMap.WEAK);

    protected OpenJPAId() {
    }

    protected OpenJPAId(Class type) {
        this.type = type;
    }

    protected OpenJPAId(Class type, boolean subs) {
        this.type = type;
        this.subs = subs;
    }

    /**
     * Return the persitent class which this id instance represents.
     */
    public Class getType() {
        return type;
    }

    /**
     * Whether this oid might be for a subclass of the given type.
     * Defaults to true.
     */
    public boolean hasSubclasses() {
        return subs;
    }

    /**
     * Set the exact type of the described instance once it is known.
     */
    public void setManagedInstanceType(Class type) {
        this.type = type;
        this.subs = false;
    }

    /**
     * Return the identity value as an object.
     */
    public abstract Object getIdObject();

    /**
     * Return the id's hash code.
     */
    protected abstract int idHash();

    /**
     * Compare the id to the id of the given instance.
     */
    protected abstract boolean idEquals(OpenJPAId other);

    /**
     * Generate the hashcode for this Id.  Cache the type's generated hashcode
     * so that it doesn't have to be generated each time.
     */
    public int hashCode() {
        if (_typeHash == 0) {
            Integer typeHashInt = (Integer) _typeCache.get(type);
            if (typeHashInt == null) {
                Class base = type;
                Class superclass = base.getSuperclass();
                while (superclass != null && superclass != Object.class) {
                    base = base.getSuperclass();
                    superclass = base.getSuperclass();
                }
                _typeHash = base.hashCode();
                _typeCache.put(type, new Integer(_typeHash));
            } else {
                _typeHash = typeHashInt.intValue();
            }
        }
        return _typeHash ^ idHash();
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        OpenJPAId id = (OpenJPAId) o;
        return idEquals(id) && (id.type.isAssignableFrom(type)
            || (subs && type.isAssignableFrom(id.type)));
    }

    public String toString() {
        return type.getName() + "-" + getIdObject();
    }

    public int compareTo(Object other) {
        if (other == this)
            return 0;
        if (other == null)
            return 1;
        return ((Comparable) getIdObject()).compareTo(((OpenJPAId) other).
            getIdObject ());
	}
}
