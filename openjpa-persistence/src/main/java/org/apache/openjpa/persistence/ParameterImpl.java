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

import javax.persistence.Parameter;

import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.lib.util.Localizer;

/**
 * A user-defined parameter of a query.
 * 
 * A parameter is uniquely identified within the scope of a query by either
 * its name or integral position. The integral position refers to the integer
 * key as specified by the user. The index of this parameter during execution
 * in a datastore query may be different.  
 * <br>
 * A value can be bound to this parameter. This behavior of a parameter carrying
 * its own value is a change from earlier versions (where no explicit abstraction
 * existed for a query parameter).   
 * 
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 * 
 * @param <T> type of value carried by this parameter.
 * 
 */
public class ParameterImpl<T> implements QueryParameter<T> {
    private static final Localizer _loc = Localizer.forPackage(ParameterImpl.class);
    
    private final String _name;
    private final Integer _position;
    private final Class<?> _expectedValueType;
    private T _value;
    private boolean _bound; 
    
    /**
     * Construct a positional parameter with the given position as key and
     * no expected value type.
     */
    public ParameterImpl(int position) {
        this(position, null);
    }
    
    /**
     * Construct a positional parameter with the given position as key and
     * given expected value type.
     */
    public ParameterImpl(int position, Class<?> expectedValueType) {
        _name = null;
        _position = position;
        _expectedValueType = expectedValueType;
    }
    
    /**
     * Construct a named parameter with the given name as key and
     * no expected value type.
     */
    public ParameterImpl(String name) {
        this(name, null);
    }
    
    /**
     * Construct a named parameter with the given name as key and
     * given expected value type.
     */
    public ParameterImpl(String name, Class<?> expectedValueType) {
        _name = name;
        _position = null;
        _expectedValueType = expectedValueType;
    }
    
    public final String getName() {
        return _name;
    }
        
    public final Integer getPosition() {
        return _position;
    }
    
    public final boolean isNamed() {
        return _name != null;
    }
    
    public final boolean isPositional() {
        return _position != null;
    }
    
    /**
     * Affirms if the given value can be assigned to this parameter.
     * 
     * If no expected value type is set, then always returns true.
     */
    public boolean isValueAssignable(Object v) {
        if (_expectedValueType == null)
            return true;
        if (v == null)
            return !_expectedValueType.isPrimitive();
        return Filters.canConvert(v.getClass(), _expectedValueType, true);
    }
    
    /**
     * Affirms if this parameter has been bound to a value.
     */
    public boolean isBound() {
        return _bound;
    }
    
    /**
     * Binds the given value to this parameter.
     * 
     * @exception if the given value is not permissible for this parameter.
     */
    public QueryParameter<T> bindValue(T v) {
        if (!isValueAssignable(v)) {
            if (v == null)
                throw new IllegalArgumentException(_loc.get("param-null-not-assignable", this).getMessage());
            else
                throw new IllegalArgumentException(_loc.get("param-value-not-assignable", this, v, v.getClass())
                        .getMessage());
        }
        _value = v;
        _bound = true;
        return this;
    }
    
    /**
     * Gets the current value irrespective of whether a value has been bound or not.
     */
    public Object getValue() {
        return getValue(false);
    }
    
    /**
     * Gets the current value only if a value has been bound or not.
     */
    public Object getValue(boolean mustBeBound) {
        if (mustBeBound && !isBound())
            throw new IllegalStateException(_loc.get("param-not-bound", this).getMessage());
        return _value;
    }
    
    /**
     * Clears bound value.
     */
    public void clearBinding() {
        _bound = false;
        _value = null;
    }
    
    /**
     * Gets (immutable) type of expected value of this parameter. 
     */
    public Class<?> getExpectedValueType() {
        return _expectedValueType;
    }
    
    /**
     * Equals if the other parameter has the same name or position.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Parameter))
            return false;
        Parameter<?> that = (Parameter<?>)other;
        if (isNamed())
            return this.getName().equals(that.getName());
        if (isPositional())
            return getPosition().equals(that.getPosition());
        return false;
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder("Parameter");
        buf.append("<" + (getExpectedValueType() == null ? "?" : getExpectedValueType().getName()) + ">");
        if (isNamed()) {
            buf.append("('" + getName() + "':");
        } else if (isPositional()) {
            buf.append("(" + getPosition() + ":");
        }
        buf.append((isBound() ? getValue() : "UNBOUND") + ")");

        return buf.toString();
    }

    public Class<T> getJavaType() {
        Class<?> cls = _value.getClass();
        return (Class<T>) cls;
    }
}
