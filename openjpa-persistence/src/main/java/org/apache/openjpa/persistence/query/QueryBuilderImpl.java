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
package org.apache.openjpa.persistence.query;

import javax.persistence.DomainObject;
import javax.persistence.PathExpression;
import javax.persistence.QueryBuilder;
import javax.persistence.QueryDefinition;

/**
 * The factory for QueryDefinition.
 * 
 * 
 * @author Pinaki Poddar
 *
 */
public class QueryBuilderImpl implements QueryBuilder {
	/**
	 * Creates a QueryDefinition without a domain root.
	 */
	public QueryDefinition createQueryDefinition() {
		return new QueryDefinitionImpl(this);
	}

	/**
	 * Creates a QueryDefinition with given class as domain root.
	 */
	public DomainObject createQueryDefinition(Class root) {
		return new QueryDefinitionImpl(this).addRoot(root);
	}

	/**
	 * Creates a QueryDefinition that can be used as a subquery to some
	 * other query.
	 */
	public DomainObject createSubqueryDefinition(PathExpression path) {
		return new QueryDefinitionImpl(this).addSubqueryRoot(path);
	}
}
