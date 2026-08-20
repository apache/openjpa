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

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.exps.Context;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.meta.AbstractManagedType;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * CriteriaDelete implementation for bulk delete operations.
 *
 * @param <T> the entity type that is the target of the delete
 */
class CriteriaDeleteImpl<T> implements CriteriaDelete<T> {

    private final MetamodelImpl _model;
    private final Class<T> _targetClass;
    private Root<T> _root;

    // Internal CriteriaQueryImpl used for expression evaluation context
    private final CriteriaQueryImpl<T> _internalQuery;

    CriteriaDeleteImpl(MetamodelImpl model, Class<T> targetClass) {
        _model = model;
        _targetClass = targetClass;
        _internalQuery = new CriteriaQueryImpl<>(model, targetClass);
    }

    /**
     * Creates a snapshot of this CriteriaDelete that captures the current state.
     * Per JPA spec, createQuery() should capture the query state at that point.
     * Subsequent modifications to the original CriteriaDelete should NOT affect
     * the already-created Query.
     *
     * The snapshot shares the same root, model, and internal query reference
     * but captures a private copy of the where clause (restriction) at the
     * time of the snapshot.
     */
    CriteriaDeleteImpl<T> snapshot() {
        CriteriaDeleteImpl<T> copy = new CriteriaDeleteImpl<>(_model, _targetClass);
        // Share the same root
        copy._root = this._root;
        // Add the root to the copy's internal query (for alias context)
        copy._internalQuery.addRoot((RootImpl<?>) this._root);
        // Snapshot the current where clause
        PredicateImpl where = this._internalQuery.getRestriction();
        if (where != null) {
            copy._internalQuery.where(where);
        }
        // Share transient evaluation state so that subqueries (which reference
        // the original _internalQuery via their _parent pointer) see the same
        // context stack as the copy's _internalQuery during evaluation.
        copy._internalQuery.shareEvalState(this._internalQuery);
        return copy;
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
    public CriteriaDelete<T> where(Expression<Boolean> restriction) {
        _internalQuery.where(restriction);
        return this;
    }

    @Override
    public CriteriaDelete<T> where(Predicate... restrictions) {
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

        // Operation
        exps.operation = QueryOperations.OP_DELETE;

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
        StringBuilder sb = new StringBuilder("DELETE FROM ");
        if (_root != null) {
            sb.append(_root.getModel().getName());
        }
        PredicateImpl where = _internalQuery.getRestriction();
        if (where != null) {
            sb.append(" WHERE ").append(where.asValue(_internalQuery));
        }
        return sb.toString();
    }
}
