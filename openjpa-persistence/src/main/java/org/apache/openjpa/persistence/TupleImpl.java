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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.openjpa.kernel.ExpressionStoreQuery;
import org.apache.openjpa.lib.util.Localizer;

public class TupleImpl implements Tuple {
    private static final Localizer _loc = Localizer.forPackage(TupleImpl.class);
    List<TupleElement<?>> elements = new ArrayList<TupleElement<?>>();

    /**
     * Get the value of the specified tuple element.
     * 
     * @param tupleElement
     *            tuple element
     * @return value of tuple element
     * @throws IllegalArgumentException
     *             if tuple element does not correspond to an element in the query result tuple
     */
    public <X> X get(TupleElement<X> tupleElement) {
        if (!elements.contains(tupleElement)) {
            throw new IllegalArgumentException(_loc.get(
                "tuple-element-not-found",
                new Object[] { tupleElement, elements }).getMessage());
        }
        TupleElementImpl<X> impl = (TupleElementImpl<X>) tupleElement;
        return impl.getValue();
    }

    /**
     * Get the value of the tuple element to which the specified alias has been assigned.
     * 
     * @param alias
     *            alias assigned to tuple element
     * @param type
     *            of the tuple element
     * @return value of the tuple element
     * @throws IllegalArgumentException
     *             if alias does not correspond to an element in the query result tuple or element cannot be assigned to
     *             the specified type
     */
    @SuppressWarnings("unchecked")
    public <X> X get(String alias, Class<X> type) {
        if (type == null) {
            throw new IllegalArgumentException(_loc.get("tuple-was-null", "type").getMessage());
        }
        Object rval = get(alias);
        if (!type.isAssignableFrom(rval.getClass())) {
            throw new IllegalArgumentException(_loc.get(
                "tuple-element-wrong-type",
                new Object[] { alias, type, rval.getClass() }).getMessage());
        }
        return (X) rval;
    }

    /**
     * Get the value of the tuple element to which the specified alias has been assigned.
     * 
     * @param alias
     *            alias assigned to tuple element
     * @return value of the tuple element
     * @throws IllegalArgumentException
     *             if alias does not correspond to an element in the query result tuple
     */
    public Object get(String alias) {
        if (alias == null) {
            // TODO MDD determine if we can support this. 
            throw new IllegalArgumentException(_loc.get("typle-was-null", "alias").getMessage());
        }
        for (TupleElement<?> te : elements) {
            if (alias.equals(te.getAlias())) {
                return ((TupleElementImpl<?>) te).getValue();
            }
        }

        List<String> knownAliases = new ArrayList<String>();
        for(TupleElement<?> te : elements) { 
            knownAliases.add(te.getAlias());
        }
        throw new IllegalArgumentException(_loc.get("tuple-alias-not-found",
            new Object[] { alias, knownAliases }).getMessage());
    }

    /**
     * Get the value of the element at the specified position in the result tuple. The first position is 0.
     * 
     * @param i
     *            position in result tuple
     * @param type
     *            type of the tuple element
     * @return value of the tuple element
     * @throws IllegalArgumentException
     *             if i exceeds length of result tuple or element cannot be assigned to the specified type
     */
    @SuppressWarnings("unchecked")
    public <X> X get(int i, Class<X> type) {
        if (type == null) {
            throw new IllegalArgumentException(_loc.get("tuple-was-null", "type").getMessage());
        }
        Object rval = get(i);
        if(! type.isAssignableFrom(rval.getClass())) { 
            throw new IllegalArgumentException(_loc.get(
                "tuple-element-wrong-type",
                new Object[] { "position", i, type, type.getClass() }).getMessage());
        }
        return (X) rval;
    }

    /**
     * Get the value of the element at the specified position in the result tuple. The first position is 0.
     * 
     * @param i
     *            position in result tuple
     * @return value of the tuple element
     * @throws IllegalArgumentException
     *             if i exceeds length of result tuple
     */
    public Object get(int i) {
        if (i > elements.size()) {
            throw new IllegalArgumentException(_loc.get("tuple-exceeded-size",
                new Object[] { i, elements.size() }).getMessage());
        }
        if (i <= -1) {
            throw new IllegalArgumentException(_loc.get("tuple-stop-thinking-in-python").getMessage());
        }
        return toArray()[i];
    }

    /**
     * Return the values of the result tuple elements as an array.
     * 
     * @return tuple element values
     */
    public Object[] toArray() {
        Object[] rval = new Object[elements.size()];
        int i = 0;
        for (TupleElement<?> tupleElement : elements) {
            rval[i] = ((TupleElementImpl<?>) tupleElement).getValue();
            i++;
        }
        return rval;
    }

    /**
     * Return the tuple elements
     * 
     * @return tuple elements
     */
    public List<TupleElement<?>> getElements() {
        return elements;
    }

    @SuppressWarnings("unchecked")
    public void put(Object key, Object value) {
        // TODO check for duplicate aliases? 
        TupleElementImpl<?> element = new TupleElementImpl(value.getClass());
        element.setAlias((String) key);
        element.setValue(value);
        elements.add(element);
    }
}
