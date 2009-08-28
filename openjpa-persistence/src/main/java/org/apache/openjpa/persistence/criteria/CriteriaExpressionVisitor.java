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
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * A visitor for Criteria Expression nodes.
 * 
 * @author Pinaki Poddar
 * @since 2.0.0
 *
 */
public interface CriteriaExpressionVisitor {
    /**
     * Enter the given expression.
     */
    void enter(CriteriaExpression<?> expr);
    
    /**
     * Exit the given expression.
     */
    void exit(CriteriaExpression<?> expr);
    
    boolean isVisited(CriteriaExpression<?> expr);
    
    /**
     * An abstract implementation that can detect cycles during traversal.
     *  
     */
    public static abstract class AbstractVisitor implements CriteriaExpressionVisitor {
        protected final Set<CriteriaExpression<?>> _visited = new HashSet<CriteriaExpression<?>>();
        
        /**
         * Affirms if this expression has been visited before.
         * Remembers the given node as visited.
         */
        public boolean isVisited(CriteriaExpression<?> expr) {
            return _visited.contains(expr);
        }
    }
    
    /**
     * A visitor to register Parameter expression of a query.
     *
     */
    public static class ParameterVisitor extends AbstractVisitor {
        private final CriteriaQueryImpl<?> query;
        
        public ParameterVisitor(CriteriaQueryImpl<?> q) {
            query = q;
        }
        
        public void enter(CriteriaExpression<?> expr) {
            if (expr != null && expr instanceof ParameterExpressionImpl) {
                query.registerParameter((ParameterExpressionImpl<?>)expr);
            }
        }

        public void exit(CriteriaExpression<?> expr) {
            _visited.add(expr);
        }
        
    }
}
