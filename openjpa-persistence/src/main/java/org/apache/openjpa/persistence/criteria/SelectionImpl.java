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

import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Selection;

import org.apache.openjpa.persistence.TupleElementImpl;

/**
 * An item selected in the projection clause of  Criteria query.
 * 
 * @author Pinaki Poddar
 *
 * @param <X>
 */
public class SelectionImpl<X> extends TupleElementImpl<X> 
    implements Selection<X>, CriteriaExpression {
    
    public SelectionImpl(Class<X> cls) {
        super(cls);
    }

    public Selection<X> alias(String alias) {
        super.setAlias(alias);
        return this;
    }

    public List<Selection<?>> getCompoundSelectionItems() {
        throw new IllegalStateException(this + " is not a compound selection");
    }

    public boolean isCompoundSelection() {
        return false;
    }
    
    public StringBuilder asValue(CriteriaQueryImpl<?> q) {
        throw new IllegalStateException(this.getClass().getSimpleName() + " can not be rendered as value");
    }
    
    public StringBuilder asVariable(CriteriaQueryImpl<?> q) {
        throw new IllegalStateException(this.getClass().getSimpleName() + " can not be rendered as variable");
    }
    
    public void acceptVisit(CriteriaExpressionVisitor visitor) {
        Expressions.acceptVisit(visitor, this, (Expression<?>[])null);
    }

}
