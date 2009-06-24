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

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.PluralJoin;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.meta.Members;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Members.Member;

/**
 * 
 * @author Pinaki Poddar
 * 
 */
public abstract class Joins {
   
    /**
     * Join a single-valued attribute.
     * 
     *
     * @param <Z> type from which joining
     * @param <X> type of the attribute being joined
     */
    public static class SingularJoin<Z,X> extends FromImpl<Z,X> 
        implements Join<Z,X>{
        private final JoinType joinType;
        private boolean allowNull = false;
        
        public SingularJoin(FromImpl<?,Z> from, 
            Members.SingularAttributeImpl<? super Z, X> member, JoinType jt) {
            super(from, member, member.getJavaType());
            joinType = jt;
            allowNull = joinType != JoinType.INNER;
        }
        
        public JoinType getJoinType() {
            return joinType;
        }

        public FromImpl<?, Z> getParent() {
            return (FromImpl<?, Z>) _parent;
        }
        
        /**
         * Return the metamodel attribute corresponding to the join.
         * @return metamodel attribute type corresponding to the join
         */
//        Attribute<? super Z, ?> getAttribute();
        public Attribute<? super Z, ?> getAttribute() {
            return  (Attribute<? super Z, ?> )_member;
        }
        
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl c) {
            ClassMetaData meta = _member.fmd.getDeclaredTypeMetaData();
            org.apache.openjpa.kernel.exps.Path path = null;
            SubqueryImpl<?> subquery = c.getDelegator();
            PathImpl<?,?> parent = getInnermostParentPath();
            if (c.isRegistered(this))
                return c.getValue(this);
            else if (parent.inSubquery(subquery)) {
                org.apache.openjpa.kernel.exps.Subquery subQ = 
                    subquery.getSubQ();
                path = factory.newPath(subQ);
                path.setMetaData(subQ.getMetaData());
                //path.setSchemaAlias(c.getAlias(this));
            } else {
                path = 
                    (org.apache.openjpa.kernel.exps.Path)
                    _parent.toValue(factory, model, c);
                path.get(_member.fmd, allowNull);
                path.setMetaData(meta);
                path.setImplicitType(meta.getDescribedType());
            }
            return path;
        }
        
        @Override
        public org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl c) {
            ClassMetaData meta = _member.fmd.getDeclaredTypeMetaData();
            org.apache.openjpa.kernel.exps.Path path = null;
            SubqueryImpl<?> subquery = c.getDelegator();
            PathImpl<?,?> parent = getInnermostParentPath();
            org.apache.openjpa.kernel.exps.Expression filter = null;
            PathImpl correlatedParent = null;
            boolean bind = true;
            if (parent.inSubquery(subquery)) {
                org.apache.openjpa.kernel.exps.Subquery subQ = 
                    subquery.getSubQ();
                path = factory.newPath(subQ);
                correlatedParent = _parent.getCorrelatedParent();
                if (correlatedParent == null) {
                    path.setMetaData(subQ.getMetaData());
                    path.get(_member.fmd, allowNull);
                    //path.setSchemaAlias(c.getAlias(_parent));
                } else 
                    bind = false;
            } else if (c.isRegistered(_parent)) {
               Value var = c.getVariable(_parent);
               path = factory.newPath(var);
               path.setMetaData(meta);
               path.get(_member.fmd, false);
            } else
               path = 
                (org.apache.openjpa.kernel.exps.Path)toValue(factory, model, c);
            org.apache.openjpa.kernel.exps.Expression join = null;
            if (bind) {
                Value var = factory.newBoundVariable(c.getAlias(this), 
                    meta.getDescribedType());
                join = factory.bindVariable(var, path);
                c.registerVariable(this, var, path);
            }
            if (getJoins() != null) {
                for (Join<?, ?> join1 : getJoins()) {
                    filter = CriteriaExpressionBuilder.and(factory, 
                        ((FromImpl<?,?>)join1).
                        toKernelExpression(factory, model, c), filter);
                }
            }
            org.apache.openjpa.kernel.exps.Expression expr = 
                CriteriaExpressionBuilder.and(factory, join, filter);
            
            if (correlatedParent == null)
                return expr;
            else {
                org.apache.openjpa.kernel.exps.Path parentPath =
                    (org.apache.openjpa.kernel.exps.Path)
                    correlatedParent.toValue(factory, model, c);
                parentPath.get(_member.fmd, allowNull);
                //parentPath.setSchemaAlias(c.getAlias(correlatedParent));
                path.setMetaData(meta);
                //filter = bindVariableForKeyPath(path, alias, filter);
                filter = factory.equal(parentPath, path);
                return CriteriaExpressionBuilder.and(factory, expr, filter);
            }
        }
    }
    
    /**
     * Join a multi-valued attribute.
     * 
     * @param Z type being joined from
     * @param C Java collection type of the container
     * @param E type of the element being joined to
     * 
     */
    public static abstract class AbstractCollection<Z,C,E> extends FromImpl<Z,E> 
        implements PluralJoin<Z, C, E> {
        final JoinType joinType;
        boolean allowNull = false;
        
        public AbstractCollection(FromImpl<?,Z> from, 
            Members.PluralAttributeImpl<? super Z, C, E> member, JoinType jt) {
            super(from, member, member.getBindableJavaType());
            joinType = jt;
            allowNull = joinType != JoinType.INNER;
        }
        
        public JoinType getJoinType() {
            return joinType;
        }

        public FromImpl<?, Z> getParent() {
            return (FromImpl<?, Z>) _parent;
        }
        
        @Override
        public FromImpl<?, Z> getParentPath() {
            return (FromImpl<?, Z>) _parent;
        }
        
        public Attribute<? super Z, E> getAttribute() {
            return (Member<? super Z, E>)_member;
        }
        
        public PluralAttribute<? super Z, C, E> getModel() {
            return (PluralAttribute<? super Z, C, E>) _member.getType();
        }
        
        public ClassMetaData getMemberClassMetaData() {
            return _member.fmd.isElementCollection() 
                ? _member.fmd.getEmbeddedMetaData()
                : _member.fmd.getElement().getDeclaredTypeMetaData();
        }

        /**
         * Convert this path to a kernel path (value).
         */
        @Override
        public Value toValue(ExpressionFactory factory, MetamodelImpl model,
            CriteriaQueryImpl c) {
            ClassMetaData meta = getMemberClassMetaData(); 
            org.apache.openjpa.kernel.exps.Path path = null;
            SubqueryImpl<?> subquery = c.getDelegator();
            PathImpl<?,?> parent = getInnermostParentPath();
            
            if (c.isRegistered(this)) {
                Value var = c.getVariable(this);
                path = factory.newPath(var);
            } else if (parent.inSubquery(subquery)) {
                org.apache.openjpa.kernel.exps.Subquery subQ = 
                    subquery.getSubQ();
                path = factory.newPath(subQ);
                path.setMetaData(subQ.getMetaData());
                //path.setSchemaAlias(c.getAlias(this));
            } else {
                path = (org.apache.openjpa.kernel.exps.Path)
                    _parent.toValue(factory, model, c);
                path.get(_member.fmd, allowNull);
            }
            return path;
        }

        /**
         * Convert this path to a join expression.
         * 
         */
        @Override
        public org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl c) {
            ClassMetaData meta = getMemberClassMetaData(); 
            org.apache.openjpa.kernel.exps.Path path = null;
            SubqueryImpl<?> subquery = c.getDelegator();
            PathImpl<?,?> parent = getInnermostParentPath();
            org.apache.openjpa.kernel.exps.Expression filter = null;
            PathImpl correlatedParent = null;
            boolean bind = true;
            if (parent.inSubquery(subquery)) {
                org.apache.openjpa.kernel.exps.Subquery subQ = 
                    subquery.getSubQ();
                path = factory.newPath(subQ);
                correlatedParent = _parent.getCorrelatedParent();
                if (correlatedParent == null) {
                    path.setMetaData(subQ.getMetaData());
                    path.get(_member.fmd, allowNull);
                    //path.setSchemaAlias(c.getAlias(_parent));
                } else
                    bind = false;
            } else if (c.isRegistered(_parent)) {
                Value var = c.getVariable(_parent);
                path = factory.newPath(var);
                path.setMetaData(meta);
                path.get(_member.fmd, false);
            } else           
                path = (org.apache.openjpa.kernel.exps.Path)
                    toValue(factory, model, c);
        
            org.apache.openjpa.kernel.exps.Expression join = null;
            if (bind) {
                Value var = factory.newBoundVariable(c.getAlias(this), 
                        meta.getDescribedType());
                join = factory.bindVariable(var, path);
                c.registerVariable(this, var, path);
            }
            if (getJoins() != null) {
                for (Join<?, ?> join1 : getJoins()) {
                    filter = CriteriaExpressionBuilder.and(factory, 
                        ((FromImpl<?,?>)join1).
                        toKernelExpression(factory, model, c), filter);
                }
            }
            org.apache.openjpa.kernel.exps.Expression expr = 
                CriteriaExpressionBuilder.and(factory, join, filter);
            if (correlatedParent == null)
                return expr;
            else {
                org.apache.openjpa.kernel.exps.Path parentPath =
                    (org.apache.openjpa.kernel.exps.Path)
                    correlatedParent.toValue(factory, model, c);
                parentPath.get(_member.fmd, allowNull);
                //parentPath.setSchemaAlias(c.getAlias(correlatedParent));
                path.setMetaData(meta);
                //filter = bindVariableForKeyPath(path, alias, filter);
                filter = factory.equal(parentPath, path);
                return CriteriaExpressionBuilder.and(factory, expr, filter);
            }
        }
    }
    
    /**
     * Join a java.util.Collection type attribute.
     *
     * @param <Z>
     * @param <E>
     */
    public static class Collection<Z,E> 
        extends AbstractCollection<Z,java.util.Collection<E>,E> 
        implements CollectionJoin<Z,E>{
        public Collection(FromImpl<?,Z> parent, 
            Members.CollectionAttributeImpl<? super Z, E> member, JoinType jt) {
            super(parent, member, jt);
        }
        
        public CollectionAttribute<? super Z, E> getModel() {
            return (CollectionAttribute<? super Z, E>)
               _member.getType();
        }
    }
    
    /**
     * Join a java.util.Set type attribute.
     *
     * @param <Z>
     * @param <E>
     */
    public static class Set<Z,E> 
        extends AbstractCollection<Z,java.util.Set<E>,E> 
        implements SetJoin<Z,E>{
        public Set(FromImpl<?,Z> parent, Members.SetAttributeImpl<? super Z, E> member, JoinType jt) {
            super(parent, member, jt);
        }
        
        public SetAttribute<? super Z, E> getModel() {
            return (SetAttribute<? super Z, E>)_member;
        }
    }
    
    /**
     * Join a java.util.List type attribute.
     *
     * @param <Z>
     * @param <E>
     */
    
    public static class List<Z,E> 
        extends AbstractCollection<Z,java.util.List<E>,E> 
        implements ListJoin<Z,E> {
        
        public List(FromImpl<?,Z> parent, 
            Members.ListAttributeImpl<? super Z, E> member, JoinType jt) {
            super(parent, member, jt);
        }
        
        public ListAttribute<? super Z, E> getModel() {
            return (ListAttribute<? super Z, E>)_member.getType();
        }
        
        public Expression<Integer> index() {
            return new Expressions.Index(this);
        }
    }
    
    /**
     * Join a java.util.Map type attribute.
     *
     * @param <Z>
     * @param <E>
     */
    
    public static class Map<Z,K,V> 
        extends AbstractCollection<Z,java.util.Map<K,V>,V> 
        implements MapJoin<Z,K,V> {
        
        public Map(FromImpl<?,Z> parent, Members.MapAttributeImpl<? super Z, K,V> member, JoinType jt) {
            super(parent, member, jt);
        }
        
        public MapAttribute<? super Z, K,V> getModel() {
            return (MapAttribute<? super Z, K,V>) _member.getType();
        }
        
        public Join<java.util.Map<K, V>, K> joinKey() {
            throw new AbstractMethodError();
        }
        
        public Join<java.util.Map<K, V>, K> joinKey(JoinType jt) {
            throw new AbstractMethodError();
        }
        
        public Expression<java.util.Map.Entry<K, V>> entry() {
            throw new AbstractMethodError();
        }
        
        public Path<K> key() {
            return new MapKey<Z,K>(this);
        }
        
        public Path<V> value() {
            return this;
        }
        
        @Override
        public org.apache.openjpa.kernel.exps.Expression toKernelExpression(
            ExpressionFactory factory, MetamodelImpl model, 
            CriteriaQueryImpl c) {
            org.apache.openjpa.kernel.exps.Value path = toValue
               (factory, model, c);
            
            ClassMetaData meta = _member.fmd.isElementCollection() 
                ? _member.fmd.getEmbeddedMetaData()
                : _member.fmd.getElement().getDeclaredTypeMetaData();
                
            Value var = factory.newBoundVariable(c.getAlias(this), 
                meta.getDescribedType());
            org.apache.openjpa.kernel.exps.Expression join = factory
                .bindValueVariable(var, path);
            c.registerVariable(this, var, path);
            return join;
        }
    }
    
       
   public static class MapKey<Z,K> extends PathImpl<Z,K> {
       Map<?,K,?> map;
       public MapKey(Map<Z,K,?> joinMap){
           super(((MapAttribute<Z, K, ?>)joinMap.getAttribute())
                   .getKeyJavaType());
           this.map = joinMap;
       }
       
       /**
        * Convert this path to a join expression.
        * 
        */
       @Override
       public Value toValue(ExpressionFactory factory, MetamodelImpl model, 
           CriteriaQueryImpl c) {
           org.apache.openjpa.kernel.exps.Path path = 
               factory.newPath(c.getVariable(map));
           return factory.getKey(path);
       }
   }
       
}
