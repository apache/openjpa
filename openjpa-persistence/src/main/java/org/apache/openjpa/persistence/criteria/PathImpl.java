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

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.persistence.meta.Members;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * Represents a simple or compound attribute path from a 
 * bound type or collection, and is a "primitive" expression.
 * @param <X>  Type referenced by the path
 */
/**
 * Path is an expression often representing a persistent member traversed
 * from another (parent) path.
 * 
 * @author Pinaki Poddar
 * @author Fay Wang
 * 
 * @param <Z> the type of the parent path 
 * @param <X> the type of this path
 */
public class PathImpl<Z,X> extends ExpressionImpl<X> implements Path<X> {
    protected final PathImpl<?,Z> _parent;
    protected final Members.Member<? super Z,?> _member;
    private boolean isEmbedded = false;
    protected PathImpl<?,?> _correlatedPath;
    
    /**
     * Protected. use by root path which neither represent a member nor has a
     * parent. 
     */
    protected PathImpl(Class<X> cls) {
        super(cls);
        _parent = null;
        _member = null;
    }
    
    /**
     * Create a path from the given non-null parent representing the the given 
     * non-null member. The given class denotes the type expressed by this
     * path.
     */
    public PathImpl(PathImpl<?,Z> parent, Members.Member<? super Z, ?> member, 
        Class<X> cls) {
        super(cls);
        _parent = parent;
        if (_parent.isEmbedded) {
            FieldMetaData fmd = getEmbeddedFieldMetaData(member.fmd);
            _member = new Members.SingularAttributeImpl(member.owner, fmd);
        } else {
            _member = member;
        }
        isEmbedded = _member.fmd.isEmbedded();
    }

    /** 
     * Returns the bindable object that corresponds to the path expression.
     *  
     */
    public Bindable<X> getModel() { 
        return (Bindable<X>)_member;
    }
    
    /**
     *  Return the parent "node" in the path or null if no parent.
     */
    public Path<Z> getParentPath() {
        return _parent;
    }
    
    public PathImpl<?,?> getInnermostParentPath() {
        return (_parent == null) ? this : _parent.getInnermostParentPath();
    }

    protected FieldMetaData getEmbeddedFieldMetaData(FieldMetaData fmd) {
        Members.Member<?,?> member = getInnermostMember(_parent,_member);
        ClassMetaData embeddedMeta = member.fmd.getEmbeddedMetaData();
        if (embeddedMeta != null)
            return embeddedMeta.getField(fmd.getName());
        else
            return fmd;
    }
    
    protected Members.Member<?,?> getInnermostMember(PathImpl<?,?> parent, Members.Member<?,?> member) {
        return member != null ? member : getInnermostMember(parent._parent,
            parent._member); 
    }
    
    public void setCorrelatedPath(PathImpl<?,?> correlatedPath) {
        _correlatedPath = correlatedPath;
    }
    public PathImpl<?,?> getCorrelatedPath() {
        return _correlatedPath;
    }
    
    /**
     * Convert this path to a kernel path.
     */
    @Override
    public Value toValue(
        ExpressionFactory factory, MetamodelImpl model,  CriteriaQueryImpl<?> q) {
        if (q.isRegistered(this))
            return q.getValue(this);
        org.apache.openjpa.kernel.exps.Path path = null;
        SubqueryImpl<?> subquery = q.getDelegator();
        boolean allowNull = _parent == null ? false : _parent instanceof Join 
            && ((Join<?,?>)_parent).getJoinType() != JoinType.INNER;
        PathImpl<?,?> corrJoin = getCorrelatedJoin(this);
        PathImpl<?,?> corrRoot = getCorrelatedRoot(subquery);
        if (_parent != null && q.isRegistered(_parent)) {
            path = factory.newPath(q.getVariable(_parent));
            //path.setSchemaAlias(q.getAlias(_parent));
            path.get(_member.fmd, allowNull);
        } else if (corrJoin != null || corrRoot != null) {
            org.apache.openjpa.kernel.exps.Subquery subQ = subquery.getSubQ();
            path = factory.newPath(subQ);
            path.setMetaData(subQ.getMetaData());
            //path.setSchemaAlias(q.getAlias(_parent));
            traversePath(_parent, path, _member.fmd);
        } else if (_parent != null) {
            path = (org.apache.openjpa.kernel.exps.Path)
            _parent.toValue(factory, model, q);
            path.get(_member.fmd, allowNull);
        } else if (_parent == null) {
            path = factory.newPath();
            path.setMetaData(model.repos.getCachedMetaData(getJavaType()));
        }
        if (_member != null && !_member.isCollection()) {
            path.setImplicitType(getJavaType());
        }
        path.setAlias(q.getAlias(this));
        return path;
    }
    
    public PathImpl<?,?> getCorrelatedRoot(SubqueryImpl<?> subquery) {
        if (subquery == null)
            return null;
        PathImpl<?,?> root = getInnermostParentPath();
        if (subquery.getRoots() != null && subquery.getRoots().contains(this))
            return root;
        return null;
    }
    
    
    public PathImpl<?,?> getCorrelatedJoin(PathImpl<?,?> path) {
        if (path._correlatedPath != null)
            return path._correlatedPath;
        if (path._parent == null)
            return null;
        return getCorrelatedJoin(path._parent);
    }
    
    /**
     * Affirms if this receiver occurs in the roots of the given subquery.
     */
    public boolean inSubquery(SubqueryImpl<?> subquery) {
        return subquery != null && (subquery.getRoots() == null ? false : subquery.getRoots().contains(this));
    }
    
    protected void traversePath(PathImpl<?,?> parent,  org.apache.openjpa.kernel.exps.Path path, FieldMetaData fmd) {
        boolean allowNull = parent == null ? false : parent instanceof Join 
            && ((Join<?,?>)parent).getJoinType() != JoinType.INNER;
        FieldMetaData fmd1 = parent._member == null ? null : parent._member.fmd;
        PathImpl<?,?> parent1 = parent._parent;
        if (parent1 == null || parent1.getCorrelatedPath() != null) {
            if (fmd != null) 
                path.get(fmd, allowNull);
            return;
        }
        traversePath(parent1, path, fmd1);
        if (fmd != null) 
            path.get(fmd, allowNull);
    }
    
    /**
     *  Return the path corresponding to the referenced 
     *  single-valued attribute.
     *  @param atttribute single-valued attribute
     *  @return path corresponding to the referenced attribute
     */
    /**
     * Create a new path with this path as parent.
     */
    public <Y> Path<Y> get(SingularAttribute<? super X, Y> attr) {
        return new PathImpl<X,Y>(this, (Members.SingularAttributeImpl<? super X, Y>)attr, 
            attr.getJavaType());
    }
    
    /**
     *  Return the path corresponding to the referenced 
     *  collection-valued attribute.
     *  @param collection collection-valued attribute
     *  @return expression corresponding to the referenced attribute
     */
    public <E, C extends java.util.Collection<E>> Expression<C>  get(PluralAttribute<X, C, E> coll) {
        return new PathImpl<X,C>(this, (Members.Member<? super X, C>)coll, coll.getJavaType());
    }

    /**
     *  Return the path corresponding to the referenced 
     *  map-valued attribute.
     *  @param map map-valued attribute
     *  @return expression corresponding to the referenced attribute
     */
    public <K, V, M extends java.util.Map<K, V>> Expression<M> get(MapAttribute<X, K, V> map) {
        return new PathImpl<X,M>(this, (Members.MapAttributeImpl<? super X,K,V>)map, (Class<M>)map.getJavaType());
    }
    
    public <Y> Path<Y> get(String attName) {
        Members.Member<? super X, Y> next = null;
        Type<?> type = _member.getType();
        switch (type.getPersistenceType()) {
        case BASIC:
            throw new RuntimeException(attName + " not navigable from " + this);
            default: next = (Members.Member<? super X, Y>) ((ManagedType<?>)type).getAttribute(attName);
        }
        return new PathImpl<X,Y>(this, next, (Class<Y>)type.getClass());
    }
    
    /**
     * Get the type() expression corresponding to this path. 
     */
    public Expression<Class<? extends X>> type() {
        return new Expressions.Type<X>(this);
    }   
}
