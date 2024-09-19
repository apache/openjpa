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
package org.apache.openjpa.persistence.criteria;

import java.util.Collection;

import jakarta.persistence.criteria.ParameterExpression;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.lib.util.OrderedMap;
import org.apache.openjpa.util.InternalException;

/**
 * Parameter of a criteria query.
 * <br>
 * A parameter in CriteriaQuery is always a named parameter but can be constructed with a null name.
 * Positional parameters are not allowed in CriteraQuery.
 * <br>
 *
 * @author Pinaki Poddar
 * @author Fay wang
 *
 * @param <T> the type of value held by this parameter.
 */
class ParameterExpressionImpl<T> extends ExpressionImpl<T>
    implements ParameterExpression<T>, BindableParameter {
    private final String _name;
    private int _index = 0; // index of the parameter as seen by the kernel, not position
    private Object value;

    /**
     * Construct a Parameter of given expected value type and name.
     *
     * @param cls expected value type
     * @param name name of the parameter which can be null.
     */
    public ParameterExpressionImpl(Class<T> cls, String name) {
        super(cls);
        if (name != null) {
            assertValidName(name);
        }

        _name = name;
    }

    /**
     * Gets the name of this parameter.
     * The name can be null.
     */
    @Override
    public final String getName() {
        return _name;
    }

    /**
     * Raises an internal exception because parameters of CriteriaQuery
     * are not positional.
     */
    @Override
    public final Integer getPosition() {
        throw new InternalException(this + " must not be asked for its position");
    }

    void setIndex(int index) {
        _index = index;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("ParameterExpression");
        buf.append("<" + getJavaType().getSimpleName() + ">");
        if (_name != null)
            buf.append("('"+ _name +"')");

        return buf.toString();
    }

    @Override
    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public Object value() {
        return value;
    }

    @Override
    public Value toValue(ExpressionFactory factory, CriteriaQueryImpl<?> q) {
        Class<?> clzz = getJavaType();
        Object paramKey = _name == null ? _index : _name;
        boolean isCollectionValued  = Collection.class.isAssignableFrom(clzz);
        org.apache.openjpa.kernel.exps.Parameter param = isCollectionValued
            ? factory.newCollectionValuedParameter(paramKey, clzz)
            : factory.newParameter(paramKey, clzz);

        int index = _name != null
            ? findIndexWithSameName(q)
            : _index;
        param.setIndex(index);

        return param;
    }

    private int findIndexWithSameName(CriteriaQueryImpl<?> q) {
        OrderedMap<Object, Class<?>> parameterTypes = q.getParameterTypes();
        int i = 0;
        for (Object k : parameterTypes.keySet()) {
            if (paramEquals(k)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    public StringBuilder asValue(AliasContext q) {
        return Expressions.asValue(q, ":", _name == null ? "param" : _name);
    }

    @Override
    public Class<T> getParameterType() {
        return getJavaType();
    }

    public boolean paramEquals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        ParameterExpressionImpl<?> that = (ParameterExpressionImpl<?>) o;

        // we treat parameters the same ONLY if they are
        // * either the same instance (tested above)
        // * or have the same parameter name in the same tree
        if (_name == null || !_name.equals(that._name)) {
            return false;
        }

        // if name is given, then we ignore the index
        if (_name == null && _index != that._index)
            return false;

        if (getParameterType() != ((ParameterExpressionImpl<?>) o).getParameterType() )
            return false;

        return value != null ? value.equals(that.value) : that.value == null;
    }

}
