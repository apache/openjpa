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
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.AbstractCollection;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.Map;

import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.persistence.meta.Members;
import org.apache.openjpa.persistence.meta.MetamodelImpl;

/**
 * Path from another (parent) path.
 * 
 * @author ppoddar
 *
 * @param <X>
 */
public class PathImpl<X> extends ExpressionImpl<X> implements Path<X> {
    private PathImpl<?> _parent;
    private Members.Member<?,X> member;
    private boolean _isTypeExpr;
    
    /**
     * 
     * @param cls
     */
    protected PathImpl(Class<X> cls) {
        super(cls);
    }
    
    public <Z> PathImpl(Members.Member<Z, X> member) {
        super(member.getMemberJavaType());
        this.member = member;
    }
    
    public <Z> PathImpl(PathImpl<Z> parent, 
        Members.Member<? super Z, X> member) {
        super(member.getMemberJavaType());
        _parent = parent;
        this.member = member;
    }
    
    @Override
    public Value toValue(ExpressionFactory factory, MetamodelImpl model) {
        Value var = null;
        if (_parent != null) { 
            org.apache.openjpa.kernel.exps.Path path = 
                (org.apache.openjpa.kernel.exps.Path)
                _parent.toValue(factory, model);
            path.get(member.fmd, false);
            var = path;
        } else {
            var = factory.newPath();//getJavaType());
            var.setMetaData(model.repos.getMetaData(getJavaType(), null, true));
        }
        if (member != null) {
            int typeCode = member.fmd.getDeclaredTypeCode();
            if (typeCode != JavaTypes.COLLECTION && typeCode != JavaTypes.MAP)
                var.setImplicitType(getJavaType());
        }
        var.setAlias(getAlias());
        if (_isTypeExpr) 
            var = factory.type(var);
        return var;
    }

    public <Y> Path<Y> get(Attribute<? super X, Y> attr) {
        return new PathImpl(this, (Members.Member<? super X, Y>)attr);
    }

    public Expression get(AbstractCollection collection) {
        return new PathImpl(this, (Members.BaseCollection) collection);
    }

    public Expression get(Map collection) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }

    public Path get(String attName) {
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }
  //TODO: what does this return for a collection key, value? null?
    public Bindable<X> getModel() { 
        // TODO Auto-generated method stub
        throw new AbstractMethodError();
    }
    
    public Path<?> getParentPath() {
        return _parent;
    }

    public Expression<Class<? extends X>> type() {
        PathImpl<X> path = new PathImpl(getJavaType());
        path.setTypeExpr(true);
        return (Expression<Class<? extends X>>) path;
    }

    public void setTypeExpr(boolean isTypeExpr) {
        _isTypeExpr = isTypeExpr;
    }
    
    public boolean isTypeExpr() {
        return _isTypeExpr;
    }
}
