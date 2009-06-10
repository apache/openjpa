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

import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Entity;

import org.apache.openjpa.kernel.exps.Expression;
import org.apache.openjpa.kernel.exps.ExpressionFactory;
import org.apache.openjpa.kernel.exps.Path;
import org.apache.openjpa.kernel.exps.Subquery;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.persistence.meta.MetamodelImpl;
import org.apache.openjpa.persistence.meta.Types;

/**
 * A root path without a parent.
 * 
 * @author Pinaki Poddar
 *
 * @param <X> the type of the entity
 */
public class RootImpl<X> extends FromImpl<X,X> implements Root<X> {
    private final Types.Entity<X> _entity;
    private RootImpl<X> _correlatedParent;
        
    public RootImpl(Types.Entity<X> type) {
        super(type);
        _entity = type;
    }
    
    public  Entity<X> getModel() {
        return _entity;
    }
    
    public void setCorrelatedParent(RootImpl<X> correlatedParent) {
        _correlatedParent = correlatedParent;
    }
    
    public RootImpl<X> getCorrelatedParent() {
        return _correlatedParent;
    }
    
    /**
     * Convert this path to a kernel path value.
     */
    @Override
    public Value toValue(ExpressionFactory factory, MetamodelImpl model, 
        CriteriaQueryImpl c) {
        SubqueryImpl<?> subquery = c.getContext();
        Path var = null;
        if (inSubquery(subquery)) {
            Subquery subQ = subquery.getSubQ();
            var = factory.newPath(subQ);
        } else {
            var = factory.newPath();
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
        ExpressionFactory factory, MetamodelImpl model, CriteriaQueryImpl c) {
        Value path = toValue(factory, model, c);
        Value var = factory.newBoundVariable(c.getAlias(this), 
            _entity.meta.getDescribedType());
        Expression exp = factory.bindVariable(var, path);
        
        if (_correlatedParent == null) 
            return exp;
        Value path1 = _correlatedParent.toValue(factory, model, c);
        return factory.equal(path1, path);
    }
    
    public String toString() {
        return _entity.toString();
    }
}
