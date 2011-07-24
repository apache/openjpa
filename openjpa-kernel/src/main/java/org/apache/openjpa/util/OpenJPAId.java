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
package org.apache.openjpa.util;

import java.io.Serializable;
import java.security.AccessController;

import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.ReferenceMap;
import org.apache.openjpa.lib.util.concurrent.ConcurrentReferenceHashMap;

/**
 * Identity class extended by built-in OpenJPA identity objects.
 *
 * @author Steve Kim
 */
@SuppressWarnings("serial")
public abstract class OpenJPAId implements Comparable, Serializable {
    public static final char TYPE_VALUE_SEP = '-';
    
    // cache the types' generated hash codes
    private static ConcurrentReferenceHashMap _typeCache =
        new ConcurrentReferenceHashMap(ReferenceMap.WEAK, ReferenceMap.HARD);

    private transient Class<?> _type;
    private String _typeStr;
    
    protected boolean _subs = true;

    // type hash is based on the least-derived non-object class so that
    // user-given ids with non-exact types match ids with exact types
    private transient int _typeHash = 0;

    protected OpenJPAId() {
    }

    protected OpenJPAId(Class<?> type) {
        _type = type;
        _typeStr = type.getName();
        
    }

    protected OpenJPAId(Class<?> type, boolean subs) {
        _type = type;
        _typeStr = type.getName();
        _subs = subs;
    }

    /**
     * Return the persistent class which this id instance represents.
     */
    public final Class<?> getType() {
        if (_type == null) {
            ClassLoader ccl = AccessController.doPrivileged(J2DoPrivHelper.getContextClassLoaderAction());
            try {
                _type = ccl.loadClass(_typeStr);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return _type;
    }

    /**
     * Whether this oid might be for a subclass of the given type.
     * Defaults to true.
     */
    public boolean hasSubclasses() {
        return _subs;
    }

    /**
     * Set the exact type of the described instance once it is known.
     */
    public void setManagedInstanceType(Class<?> type) {
        setManagedInstanceType(type, false);
    }

    /**
     * Set the exact type of the described instance once it is known.
     */
    public void setManagedInstanceType(Class<?> type, boolean subs) {
        _type = type;
        _subs = subs;
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
     * Generate the hash code for this Id.  Cache the type's generated hash code
     * so that it doesn't have to be generated each time.
     */
    public int hashCode() {
        if (_typeHash == 0) {
            Integer typeHashInt = (Integer) _typeCache.get(getType());
            if (typeHashInt == null) {
                Class<?> base = getType();
                Class<?> superclass = base.getSuperclass();
                while (superclass != null && superclass != Object.class) {
                    base = base.getSuperclass();
                    superclass = base.getSuperclass();
                }
                _typeHash = base.hashCode();
                _typeCache.put(getType(), Integer.valueOf(_typeHash));
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
        return idEquals(id)
            && (id.getType().isAssignableFrom(getType()) || (_subs && getType().isAssignableFrom(id.getType())));
    }

    public String toString() {
        return getType().getName() + TYPE_VALUE_SEP + getIdObject();
    }

    public int compareTo(Object other) {
        if (other == this)
            return 0;
        if (other == null)
            return 1;
        return ((Comparable) getIdObject()).compareTo(((OpenJPAId) other).getIdObject());
    }
}
