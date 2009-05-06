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

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.QueryBuilder.In;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * Expression for Criteria query.
 * 
 * @author Pinaki Poddar
 *
 * @param <X> the type of the value this expression represents.
 */
public abstract class ExpressionImpl<X> extends SelectionImpl<X> 
    implements Expression<X> {

    Value toValue(ExpressionFactory factory, MetamodelImpl model) {
        throw new AbstractMethodError(this.getClass().getName());
    }
    
    org.apache.openjpa.kernel.exps.Expression toKernelExpression(
        ExpressionFactory factory, MetamodelImpl model) {
        throw new AbstractMethodError(this.getClass().getName());
    }
    
    /**
     * @param cls the type of the evaluated result of the expression
     */
    public ExpressionImpl(Class<X> cls) {
        super(cls);
    }

    public <Y> Expression<Y> as(Class<Y> type) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Predicate in(Object... values) {
        In<X> result = new Expressions.In<X>(this);
        for (Object v : values)
        	result.value((X)v);
        return result;
    }

    public Predicate in(Expression<?>... values) {
        In<X> result = new Expressions.In<X>(this);
        for (Expression<?> e : values)
        	result.value((Expression<? extends X>)e);
        return result;
    }

    public Predicate in(Collection<?> values) {
        In<X> result = new Expressions.In<X>(this);
        for (Object e : values)
        	result.value((X)e);
        return result;
    }

    public Predicate in(Expression<?> values) {
        In<X> result = new Expressions.In<X>(this);
        result.value((Expression<? extends X>)values);
        return result;
    }

    public Predicate isNotNull() {
    	return new Expressions.IsNotNull(this);
    }

    public Predicate isNull() {
    	return new Expressions.IsNull(this);
    }
}
