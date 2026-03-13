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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.exps.Context;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.meta.AbstractManagedType;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * CriteriaUpdate implementation for bulk update operations.
 *
 * @param <T> the entity type that is the target of the update
 *
 * @since 3.2.0
 */
class CriteriaUpdateImpl<T> implements CriteriaUpdate<T> {

    private final MetamodelImpl _model;
    private final Class<T> _targetClass;
    private Root<T> _root;

    // Stored SET assignments as (path expression, value expression) pairs
    private final List<SetItem> _updates = new ArrayList<>();

    // Internal CriteriaQueryImpl used for expression evaluation context
    private final CriteriaQueryImpl<T> _internalQuery;

    CriteriaUpdateImpl(MetamodelImpl model, Class<T> targetClass) {
        _model = model;
        _targetClass = targetClass;
        _internalQuery = new CriteriaQueryImpl<>(model, targetClass);
    }

    @Override
    public Root<T> from(Class<T> entityClass) {
        _root = _internalQuery.from(entityClass);
        return _root;
    }

    @Override
    public Root<T> from(EntityType<T> entity) {
        _root = _internalQuery.from(entity);
        return _root;
    }

    @Override
    public Root<T> getRoot() {
        return _root;
    }

    @Override
    public <Y, X extends Y> CriteriaUpdate<T> set(SingularAttribute<? super T, Y> attribute, X value) {
        Path<Y> path = _root.get(attribute);
        _updates.add(new SetItem((ExpressionImpl<?>) path, new Expressions.Constant<>(value)));
        return this;
    }

    @Override
    public <Y> CriteriaUpdate<T> set(SingularAttribute<? super T, Y> attribute, Expression<? extends Y> value) {
        Path<Y> path = _root.get(attribute);
        _updates.add(new SetItem((ExpressionImpl<?>) path, (ExpressionImpl<?>) value));
        return this;
    }

    @Override
    public <Y, X extends Y> CriteriaUpdate<T> set(Path<Y> attribute, X value) {
        _updates.add(new SetItem((ExpressionImpl<?>) attribute,
            value == null ? new Expressions.Constant<>(Object.class, null) : new Expressions.Constant<>(value)));
        return this;
    }

    @Override
    public <Y> CriteriaUpdate<T> set(Path<Y> attribute, Expression<? extends Y> value) {
        _updates.add(new SetItem((ExpressionImpl<?>) attribute, (ExpressionImpl<?>) value));
        return this;
    }

    @Override
    public CriteriaUpdate<T> set(String attributeName, Object value) {
        Path<?> path = _root.get(attributeName);
        _updates.add(new SetItem((ExpressionImpl<?>) path,
            value == null ? new Expressions.Constant<>(Object.class, null) : new Expressions.Constant<>(value)));
        return this;
    }

    @Override
    public CriteriaUpdate<T> where(Expression<Boolean> restriction) {
        _internalQuery.where(restriction);
        return this;
    }

    @Override
    public CriteriaUpdate<T> where(Predicate... restrictions) {
        _internalQuery.where(restrictions);
        return this;
    }

    @Override
    public Predicate getRestriction() {
        return _internalQuery.getRestriction();
    }

    @Override
    public <U> Subquery<U> subquery(Class<U> type) {
        return _internalQuery.subquery(type);
    }

    @Override
    public <U> Subquery<U> subquery(EntityType<U> type) {
        return _internalQuery.subquery(type.getJavaType());
    }

    @Override
    public Set<ParameterExpression<?>> getParameters() {
        return _internalQuery.getParameters();
    }

    void compile() {
        _internalQuery.assertRoot();
        // Collect parameters
        _internalQuery.getParameters();
    }

    Class<T> getTargetClass() {
        return _targetClass;
    }

    CriteriaQueryImpl<T> getInternalQuery() {
        return _internalQuery;
    }

    QueryExpressions getQueryExpressions(ExpressionFactory factory) {
        QueryExpressions exps = new QueryExpressions();
        Stack<Context> contexts = _internalQuery.getContexts();
        Context context = new Context(null, null, null);
        contexts.push(context);
        exps.setContexts(contexts);

        // Access paths
        Set<ClassMetaData> metas = new HashSet<>();
        metas.add(((AbstractManagedType<?>) _root.getModel()).meta);
        exps.accessPath = metas.toArray(new ClassMetaData[0]);

        // Add root to context
        ((RootImpl<?>) _root).addToContext(factory, _model, _internalQuery);

        // Filter
        org.apache.openjpa.kernel.exps.Expression filter = null;
        for (Join<?, ?> join : _root.getJoins()) {
            filter = Expressions.and(factory,
                ((ExpressionImpl<?>) join).toKernelExpression(factory, _internalQuery), filter);
        }
        PredicateImpl where = _internalQuery.getRestriction();
        if (where != null) {
            filter = Expressions.and(factory, where.toKernelExpression(factory, _internalQuery), filter);
        }
        if (filter == null) {
            filter = factory.emptyExpression();
        }
        exps.filter = filter;

        // SET assignments
        for (SetItem item : _updates) {
            org.apache.openjpa.kernel.exps.Path path =
                (org.apache.openjpa.kernel.exps.Path) item.path.toValue(factory, _internalQuery);
            Value val = item.value.toValue(factory, _internalQuery);
            exps.putUpdate(path, val);
        }

        // Operation
        exps.operation = QueryOperations.OP_UPDATE;

        // Empty projections/ordering/grouping
        exps.projections = new Value[0];
        exps.projectionAliases = new String[0];
        exps.projectionClauses = new String[0];
        exps.ordering = new Value[0];
        exps.ascending = new boolean[0];
        exps.orderingClauses = new String[0];
        exps.orderingAliases = new String[0];
        exps.range = QueryExpressions.EMPTY_VALUES;
        exps.resultClass = _targetClass;
        exps.parameterTypes = _internalQuery.getParameterTypes();

        return exps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("UPDATE ");
        if (_root != null) {
            sb.append(_root.getModel().getName());
        }
        if (!_updates.isEmpty()) {
            sb.append(" SET ");
            for (int i = 0; i < _updates.size(); i++) {
                if (i > 0) sb.append(", ");
                SetItem item = _updates.get(i);
                sb.append(item.path.asValue(_internalQuery));
                sb.append(" = ");
                sb.append(item.value.asValue(_internalQuery));
            }
        }
        PredicateImpl where = _internalQuery.getRestriction();
        if (where != null) {
            sb.append(" WHERE ").append(where.asValue(_internalQuery));
        }
        return sb.toString();
    }

    /**
         * Internal record for a SET assignment pair.
         */
        private record SetItem(ExpressionImpl<?> path, ExpressionImpl<?> value) {
    }
}
