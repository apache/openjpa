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

/**
 * A parameter for a query. 
 * 
 * @author Pinaki Poddar
 * @since 2.0.0
 * 
 * @param <T> the type of value that can be bound to this parameter.
 */
public interface QueryParameter<T> extends Parameter<T> {
    /**
     * Affirms if this parameter key is a String.
     */
    public abstract boolean isNamed();

    /**
     * Affirms if this parameter key is a integer.
     */
    public abstract boolean isPositional();

    /**
     * Affirms if this parameter can be set to the given value.
     */
    public abstract boolean isValueAssignable(Object v);

    /**
     * Affirms if a value has been bound to this parameter.
     */
    public abstract boolean isBound();

    /**
     * Binds the given value to this parameter.
     */
    public abstract QueryParameter<T> bindValue(Object v);

    /**
     * Gets the value of this parameter without checking whether the any value has been
     * bound. Hence it can not be distinguished whether the value of a parameter is 
     * explicitly set to null or may not been set at all.
     * To distinguish between these two cases, 
     * @see #getValue(boolean)
     * 
     * @return the current value.
     */
    public abstract Object getValue();

    /**
     * Gets the value of this parameter.
     * The value of a parameter can be explicitly set to null or may not been set at all.
     * To distinguish between these two cases, either check whether a value is bound
     * {@linkplain #isBound()} prior to this call or use the boolean argument to make
     * this check.
     * 
     * @param mustBeBound if true then exception is raised if no value is bound to this parameter.
     * Otherwise, the current value is returned without any check. 
     * 
     * @return the current value.
     */
    public abstract Object getValue(boolean mustBeBound);

    /**
     * Clears the bound value, if any.
     */
    public abstract void clearBinding();

    /**
     * Gets the type of the value expected to be bound to this parameter.
     */
    public abstract Class<?> getExpectedValueType();

}
