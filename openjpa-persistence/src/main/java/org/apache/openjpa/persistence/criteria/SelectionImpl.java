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

/**
 * An item selected in the projection clause of Criteria query.
 * Base implementation for all concrete expressions.
 * 
 * @author Pinaki Poddar
 *
 * @param <X>
 */
public abstract class SelectionImpl<X> implements Selection<X>, CriteriaExpression {
    private final Class<X> _cls;
    private String _alias;
    private Boolean _autoAliased;
    
    /**
     * Construct with the immutable type represented by this selection term.
     */
    public SelectionImpl(Class<X> cls) {
        _cls = cls;
    }
    
    /**
     * Gets the immutable type represented by this selection term.
     */
    public Class<X> getJavaType() {
        return _cls;
    }
    
    /**
     * Gets the alias set of this selection term.
     */
    public String getAlias() {
        return _alias; 
    }
    
    /**
     * Sets the alias on this selection term.
     */
    public Selection<X> alias(String alias) {
        _alias = alias;
        _autoAliased = false;
        return this;
    }
    
    /**
     * Sets the alias of this expression internally. Only valid if the expression is not aliased explicitly
     * by calling {@linkplain #alias(String)}.
     */
    void setAutoAlias(String alias) {
        if (Boolean.FALSE.equals(_autoAliased))
            throw new IllegalStateException(this + " has been aliased. Can not set alias automatically");
        _alias = alias;
        _autoAliased = true;
    }
    
    /**
     * Affirms if the alias of this expression is assigned automatically.
     */
    boolean isAutoAliased() {
        return _autoAliased == null ? true : _autoAliased.booleanValue();
    }  

    /**
     * Throws IllegalStateException because a selection term, by default, consists of single value.
     */
    public List<Selection<?>> getCompoundSelectionItems() {
        throw new IllegalStateException(this + " is not a compound selection");
    }

    /**
     * Returns false because a selection term, by default, consists of single value.
     */
    public boolean isCompoundSelection() {
        return false;
    }
    
    //  ------------------------------------------------------------------------------------
    //  Contract for CriteriaExpression implemented mostly as a no-op for easier derivation.
    //  ------------------------------------------------------------------------------------
    
    public StringBuilder asValue(AliasContext q) {
        throw new IllegalStateException(this.getClass().getSimpleName() + " can not be rendered as value");
    }
    
    public StringBuilder asVariable(AliasContext q) {
        throw new IllegalStateException(this.getClass().getSimpleName() + " can not be rendered as variable");
    }
    
    public final StringBuilder asProjection(AliasContext q) {
        String as = (isAutoAliased() ? "" : " AS " + getAlias());
        return asValue(q).append(as);
    }
    
    public void acceptVisit(CriteriaExpressionVisitor visitor) {
        Expressions.acceptVisit(visitor, this, (Expression<?>[])null);
    }
}
