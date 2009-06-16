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
import javax.persistence.metamodel.AbstractCollection;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Map;
import javax.persistence.metamodel.Type;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.meta.Members;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

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
            _member = new Members.Attribute(member.owner, fmd);
        } else 
            _member = member;
        if (parent == null)
            throw new NullPointerException("Null parent for member " + member);
        if (member == null)
            throw new NullPointerException("Null member for parent " + parent);
        if (_member.fmd.isEmbedded())
            isEmbedded = true;
    }
    
    public PathImpl<?,Z> getParentPath() {
        return _parent;
    }
    
    public PathImpl<?,?> getInnermostParentPath() {
        return (_parent == null) ? this : _parent.getInnermostParentPath();
    }

    protected FieldMetaData getEmbeddedFieldMetaData(FieldMetaData fmd) {
        Members.Member member = getInnermostMember(_parent,_member);
        ClassMetaData embeddedMeta = member.fmd.getEmbeddedMetaData();
        if (embeddedMeta != null)
            return embeddedMeta.getField(fmd.getName());
        else
            return fmd;
    }
    
    protected Members.Member getInnermostMember(PathImpl parent, 
        Members.Member member) {
        return member != null ? member : getInnermostMember(parent._parent,
            parent._member); 
    }
    
    
    /**
     * Convert this path to a kernel path.
     */
    @Override
    public Value toValue(
        ExpressionFactory factory, MetamodelImpl model,  CriteriaQueryImpl q) {
        if (q.isRegistered(this))
            return q.getValue(this);
        org.apache.openjpa.kernel.exps.Path path = null;
        SubqueryImpl<?> subquery = q.getContext();
        PathImpl<?,?> parent = getInnermostParentPath();
        if (parent.inSubquery(subquery)) {
            org.apache.openjpa.kernel.exps.Subquery subQ = subquery.getSubQ();
            path = factory.newPath(subQ);
            path.setMetaData(subQ.getMetaData());
            boolean allowNull = false;
            path.get(_member.fmd, allowNull);
        } else if (_parent != null) { 
            if (q.isRegistered(_parent)) {
                path = factory.newPath(q.getVariable(_parent));
                ClassMetaData meta = _member.fmd.getDeclaredTypeMetaData();
                path.setMetaData(meta);
            } else {
                path = (org.apache.openjpa.kernel.exps.Path)
                     _parent.toValue(factory, model, q);
            }
            boolean allowNull = _parent instanceof Join 
                    && ((Join<?,?>)_parent).getJoinType() != JoinType.INNER;
            path.get(_member.fmd, allowNull);
        } else {
            path = factory.newPath();
            path.setMetaData(model.repos.getCachedMetaData(getJavaType()));
        }
        if (_member != null) {
            int typeCode = _member.fmd.getDeclaredTypeCode();
            if (typeCode != JavaTypes.COLLECTION && typeCode != JavaTypes.MAP)
                path.setImplicitType(getJavaType());
        }
        path.setAlias(q.getAlias(this));
        return path;
    }
    
    /**
     * Affirms if this receiver occurs in the roots of the given subquery.
     */
    public boolean inSubquery(SubqueryImpl<?> subquery) {
        return subquery != null && subquery.getRoots().contains(this);
    }
    
    /**
     * Create a new path with this path as parent.
     */
    public <Y> Path<Y> get(Attribute<? super X, Y> attr) {
        return new PathImpl<X,Y>(this, (Members.Attribute<? super X, Y>)attr, 
            attr.getJavaType());
    }
    
    public <E, C extends java.util.Collection<E>> Expression<C>
        get(AbstractCollection<X, C, E> coll) {
        return new PathImpl<X,C>(this, 
            (Members.BaseCollection<? super X, C, E>)coll, 
            coll.getMemberJavaType());
    }

    public <K, V, M extends java.util.Map<K, V>> Expression<M> 
        get(Map<X, K, V> map) {
        return new PathImpl<X,M>(this, (Members.Map<? super X,K,V>)map, 
            (Class<M>) map.getMemberJavaType());
    }
    
    public <Y> Path<Y> get(String attName) {
        Members.Member<? super X, Y> next = null;
        Type<?> type = _member.getType();
        switch (type.getPersistenceType()) {
        case BASIC:
            throw new RuntimeException(attName + " not navigable from " + this);
            default: next = (Members.Member<? super X, Y>)
                ((ManagedType<?>)type).getAttribute(attName);
        }
        return new PathImpl<X,Y>(this, next, (Class<Y>)type.getClass());
    }
    
    
    
    /**
     * Gets the bindable object that corresponds to this path.
     */
  //TODO: what does this return for a collection key, value? null?
    public Bindable<X> getModel() { 
        return (Bindable<X>)_member.getType();
    }
    
    /**
     * Get the type() expression corresponding to this path. 
     */
    public Expression<Class<? extends X>> type() {
        return new Expressions.Type<X>(this);
    }   
}
