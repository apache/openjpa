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

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.AbstractCollection;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Map;
import javax.persistence.metamodel.Member;
import javax.persistence.metamodel.Type;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.meta.Members;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * Path is an expression often representing a persistent member traversed
 * from another (parent) path.
 * 
 * @author Pinaki Poddar
 *
 * @param <Z> the type of the parent path 
 * @param <X> the type of this path
 */
public class PathImpl<Z,X> extends ExpressionImpl<X> implements Path<X> {
    protected final PathImpl<?,Z> _parent;
    protected final Members.Member<? super Z,?> _member;
    
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
     * Create a path from the given parent representing the the given member.
     */
    public PathImpl(PathImpl<?,Z> parent, Members.Member<? super Z, ?> member, 
        Class<X> cls) {
        super(cls);
        _parent = parent;
        _member = member;
        if (parent == null)
            throw new NullPointerException("Null parent for member " + member);
        if (member == null)
            throw new NullPointerException("Null member for parent " + parent);
        
    }
    
    public PathImpl<?,Z> getParentPath() {
        return _parent;
    }
    
    /**
     * Convert this path to a kernel path value.
     */
    @Override
    public Value toValue(ExpressionFactory factory, MetamodelImpl model,
        CriteriaQuery q) {
        Value var = null;
        if (_parent != null) { 
            org.apache.openjpa.kernel.exps.Path path = 
                (org.apache.openjpa.kernel.exps.Path)
                _parent.toValue(factory, model, q);
            boolean allowNull = false;
            path.get(_member.fmd, allowNull);
            var = path;
        } else {
            var = factory.newPath();//getJavaType());
            var.setMetaData(model.repos.getMetaData(getJavaType(), null, true));
        }
        if (_member != null) {
            int typeCode = _member.fmd.getDeclaredTypeCode();
            if (typeCode != JavaTypes.COLLECTION && typeCode != JavaTypes.MAP)
                var.setImplicitType(getJavaType());
        }
        var.setAlias(getAlias());
        return var;
    }

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
