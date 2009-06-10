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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

public class PredicateImpl extends ExpressionImpl<Boolean> 
implements Predicate {
    List<Expression<Boolean>> _exps;
    BooleanOperator _op;
    boolean _negated = false;

    protected PredicateImpl() {
        super(Boolean.class);
    }

    protected PredicateImpl(BooleanOperator op) {
        this();
        _op = op;
    }

    protected PredicateImpl(BooleanOperator op, Predicate...restrictions) {
        this(op);
        if (restrictions != null) {
            for (Predicate p : restrictions)
                add((PredicateImpl)p);
        }
    }

    public PredicateImpl add(Expression<Boolean> s) {
        if (_exps == null)
            _exps = new ArrayList<Expression<Boolean>>();
        _exps.add(s);
        return this;
    }

    public List<Expression<Boolean>> getExpressions() {
        return _exps;
    }

    public BooleanOperator getOperator() {
        return _op;
    }

    public boolean isNegated() {
        return _negated;
    }

    public PredicateImpl negate() {
        PredicateImpl not = clone();
        not._negated = true;
        return not;
    }

    public PredicateImpl clone() {
        PredicateImpl clone = new PredicateImpl(_op);
        if (_exps != null)
            clone._exps = new ArrayList<Expression<Boolean>>(this._exps);
        return clone;
    }

    @Override
    org.apache.openjpa.kernel.exps.Expression toKernelExpression(
        ExpressionFactory factory, MetamodelImpl model, 
        CriteriaQueryImpl q) {
        if (_exps == null || _exps.isEmpty())
            return factory.emptyExpression();
        if (_exps.size() == 1)
            return ((ExpressionImpl<?>)_exps.get(0))
            .toKernelExpression(factory, model, q);
        ExpressionImpl<?> e1 = (ExpressionImpl<?>)_exps.get(0);
        ExpressionImpl<?> e2 = (ExpressionImpl<?>)_exps.get(1);
        org.apache.openjpa.kernel.exps.Expression ke1 = 
            e1.toKernelExpression(factory, model, q);
        org.apache.openjpa.kernel.exps.Expression ke2 = 
            e2.toKernelExpression(factory, model, q);
        org.apache.openjpa.kernel.exps.Expression result = 
            _op == BooleanOperator.AND 
            ? factory.and(ke1,ke2) : factory.or(ke1, ke2);

            for (int i = 2; i < _exps.size(); i++) {
                ExpressionImpl<?> e = (ExpressionImpl<?>)_exps.get(i);
                result = _op == BooleanOperator.AND 
                ? factory.and(result, e.toKernelExpression(factory, model, q))
                : factory.or(result, e.toKernelExpression(factory,model,q));
            }
            return _negated ? factory.not(result) : result;
    }

    public static class And extends PredicateImpl {
        public And(Expression<Boolean> x, Expression<Boolean> y) {
            super(BooleanOperator.AND);
            add(x).add(y);
        }

        public And(Predicate...restrictions) {
            super(BooleanOperator.AND, restrictions);
        }
    }

    public static class Or extends PredicateImpl {
        public Or(Expression<Boolean> x, Expression<Boolean> y) {
            super(BooleanOperator.OR);
            add(x).add(y);
        }

        public Or(Predicate...restrictions) {
            super(BooleanOperator.OR, restrictions);
        }
    }
}
