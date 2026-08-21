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
package org.apache.openjpa.persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.TypedQueryReference;

/**
 * Immutable implementation of the JPA 3.2 {@link TypedQueryReference}. Instances are handed out by
 * {@link EntityManagerFactoryImpl#getNamedQueries(Class)} and can be passed back to
 * {@link jakarta.persistence.EntityManager#createQuery(TypedQueryReference)}.
 *
 * @param <R> the result type of the referenced named query
 *
 * @since 4.2.0
 */
public class TypedQueryReferenceImpl<R> implements TypedQueryReference<R> {

    private final String _name;
    private final Class<? extends R> _resultType;
    private final Map<String, Object> _hints;

    /**
     * Constructor.
     *
     * @param name the name of the named query, must not be null
     * @param resultType the declared result type of the named query, must not be null
     * @param hints the query hints, may be null or empty. A defensive, unmodifiable copy is taken.
     */
    public TypedQueryReferenceImpl(String name, Class<? extends R> resultType, Map<String, Object> hints) {
        if (name == null)
            throw new IllegalArgumentException("name is required");
        if (resultType == null)
            throw new IllegalArgumentException("resultType is required");
        _name = name;
        _resultType = resultType;
        // deliberately not Map.copyOf(): its iteration order is randomized per JVM run, while the hints
        // must be replayed in metadata declaration order (see EntityManager#createQuery(TypedQueryReference)),
        // and it would reject null keys or values that this constructor has always tolerated
        _hints = (hints == null || hints.isEmpty())
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(hints));
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Class<? extends R> getResultType() {
        return _resultType;
    }

    /**
     * The hints declared for this named query. The returned map is unmodifiable.
     */
    @Override
    public Map<String, Object> getHints() {
        return _hints;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof TypedQueryReferenceImpl))
            return false;
        TypedQueryReferenceImpl<?> o = (TypedQueryReferenceImpl<?>) other;
        return _name.equals(o._name) && _resultType.equals(o._resultType) && _hints.equals(o._hints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_name, _resultType, _hints);
    }

    @Override
    public String toString() {
        return "TypedQueryReference[name=" + _name + ", resultType=" + _resultType.getName() + "]";
    }
}
