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

/**
 * Extends specification interface to define contract for Visitor pattern.
 * 
 * @author Pinaki Poddar
 *
 * @param <X>
 */
public interface CriteriaExpression<X> extends Expression<X> {
    /**
     * Accept visit from the given visitor. The receiver is responsible 
     * to propagate the visitor to the constituent sub-nodes if any.
     * 
     * @param visitor a processor to walk the nodes of a tree.
     */
    void acceptVisit(CriteriaExpressionVisitor visitor);

}
