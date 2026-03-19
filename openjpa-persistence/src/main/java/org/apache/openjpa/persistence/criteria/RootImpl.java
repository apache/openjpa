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

import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

import org.apache.openjpa.kernel.exps.AbstractExpressionBuilder;
import org.apache.openjpa.kernel.exps.Context;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Path;
import org.apache.openjpa.kernel.exps.Subquery;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Types;

/**
 * A root path without a parent.
 *
 * @param <X> the type of the entity
 *
 * @author Pinaki Poddar
 * @author Fay Wang
 *
 * @since 2.0.0
 */
class RootImpl<X> extends FromImpl<X,X> implements Root<X> {
    private final Types.Entity<X> _entity;

    public RootImpl(Types.Entity<X> type) {
        super(type);
        _entity = type;
    }

    @Override
    public  EntityType<X> getModel() {
        return _entity;
    }

    public void addToContext(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
        String alias = q.getAlias(this);
        Value var = factory.newBoundVariable(alias, AbstractExpressionBuilder.TYPE_OBJECT);
        var.setMetaData(_entity.meta);
        Context currContext = q.ctx();
        currContext.addSchema(alias, _entity.meta);
        currContext.addVariable(alias, var);
        if (currContext.schemaAlias == null)
            currContext.schemaAlias = alias;
    }

    /**
     * Convert this path to a kernel path value.
     */
    @Override
    public Value toValue(ExpressionFactory factory, CriteriaQueryImpl<?> c) {
        SubqueryImpl<?> subquery = c.getDelegator();
        Path var = null;
        Value val = null;
        String alias = c.getAlias(this);
        if (c.ctx() != null && !alias.equalsIgnoreCase(c.ctx().schemaAlias)
            && (val = c.getRegisteredRootVariable(this)) != null) {
            // this is cross join
            var = factory.newPath(val);
        } else if (inSubquery(subquery)) {
            Subquery subQ = subquery.getSubQ();
            var = factory.newPath(subQ);
        } else {
            var = factory.newPath();
            var.setSchemaAlias(alias);
        }
        var.setMetaData(_entity.meta);
        return var;
    }

    /**
     * Convert this path to a kernel expression.
     *
     */
    @Override
    public org.apache.openjpa.kernel.exps.Expression toKernelExpression(
        ExpressionFactory factory, CriteriaQueryImpl<?> c) {
        Value path = toValue(factory, c);
        Value var = factory.newBoundVariable(c.getAlias(this),
             _entity.meta.getDescribedType());
        return factory.bindVariable(var, path);
    }

    @Override
    public StringBuilder asValue(AliasContext q) {
        Value v = q.getRegisteredRootVariable(this);
        if (v != null)
            return new StringBuilder(v.getAlias());
        v = q.getRegisteredValue(this);
        if (v != null)
            return new StringBuilder(v.getAlias());
        if (q.isRegistered(this))
            return new StringBuilder(q.getRegisteredValue(this).getName());
        return new StringBuilder().append(Character.toLowerCase(_entity.getName().charAt(0)));
    }

    @Override
    public StringBuilder asVariable(AliasContext q) {
        return new StringBuilder(_entity.getName()).append(" ").append(asValue(q));
    }

    /**
     * A root that represents a type-narrowed (treated) view of another root.
     * Used to implement {@code CriteriaBuilder.treat(Root, Class)}.
     * The treated root uses the subclass entity type for attribute resolution
     * but delegates path generation to the original root.
     *
     * @param <T> the target (subclass) type
     */
    static class TreatedRoot<T> extends RootImpl<T> {
        private final RootImpl<?> _original;

        TreatedRoot(Types.Entity<T> targetEntity, RootImpl<?> original) {
            super(targetEntity);
            _original = original;
        }

        /**
         * Returns the original (untreated) root.
         */
        RootImpl<?> getOriginal() {
            return _original;
        }

        /**
         * No-op: the original root is already registered in the query context.
         */
        @Override
        public void addToContext(ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl<?> q) {
            // Do nothing - the original root is already in context
        }

        /**
         * Returns the target (subclass) type that this treated root narrows to.
         */
        Class<T> getTreatedType() {
            return getJavaType();
        }

        /**
         * Produce a kernel path that uses the original root's alias but carries the subclass metadata.
         * Also registers this treated root on the query so that a type-restriction
         * predicate (discriminator check) can be added during query compilation.
         */
        @Override
        public Value toValue(ExpressionFactory factory, CriteriaQueryImpl<?> c) {
            // Register this treated root for discriminator filtering
            c.addTreatedRoot(this);

            // Get the alias of the original root, not the treated root
            String alias = c.getAlias(_original);
            SubqueryImpl<?> subquery = c.getDelegator();
            Path var = null;
            Value val = null;
            if (c.ctx() != null && !alias.equalsIgnoreCase(c.ctx().schemaAlias)
                && (val = c.getRegisteredRootVariable(_original)) != null) {
                var = factory.newPath(val);
            } else if (inSubquery(subquery)) {
                Subquery subQ = subquery.getSubQ();
                var = factory.newPath(subQ);
            } else {
                var = factory.newPath();
                var.setSchemaAlias(alias);
            }
            Types.Entity<T> entity = (Types.Entity<T>) getModel();
            var.setMetaData(entity.meta);
            return var;
        }

        @Override
        public org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, CriteriaQueryImpl<?> c) {
            Value path = toValue(factory, c);
            Types.Entity<T> entity = (Types.Entity<T>) getModel();
            Value var = factory.newBoundVariable(c.getAlias(_original),
                 entity.meta.getDescribedType());
            return factory.bindVariable(var, path);
        }

        @Override
        public StringBuilder asValue(AliasContext q) {
            return _original.asValue(q);
        }

        @Override
        public StringBuilder asVariable(AliasContext q) {
            return new StringBuilder(getModel().getName()).append(" ").append(asValue(q));
        }
    }

}
