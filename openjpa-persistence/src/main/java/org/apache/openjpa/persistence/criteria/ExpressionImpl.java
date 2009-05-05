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
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Predicate in(Expression<?>... values) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Predicate in(Collection<?> values) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Predicate in(Expression<?> values) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Predicate isNotNull() {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Predicate isNull() {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }
}
