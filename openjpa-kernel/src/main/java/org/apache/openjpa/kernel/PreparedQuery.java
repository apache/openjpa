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
package org.apache.openjpa.kernel;


/**
 * A prepared query associates a compiled query to a <em>parsed state</em> that
 * can be executed possibly with more efficiency. An obvious example is to 
 * associate a compiled query to an executable SQL string. 
 * 
 * The query expressed in target language can be executed directly bypassing 
 * the critical translation cost to the data store target language on every 
 * execution. 
 * 
 * As the subsequent execution of a cached query will bypass normal query 
 * compilation, the post-compilation state of the original query is captured by 
 * this receiver to be transferred to the executable query instance.  
 * 
 * This receiver must not hold any context-sensitive reference or dependency.
 * Because the whole idea of preparing a query (for a cost) is to be able to
 * execute the same logical query in different persistence contexts. However,
 * a prepared query may not be valid when some parameters of execution context  
 * such as lock group or fetch plan changes in a way that will change the target
 * query. Refer to {@link PreparedQueryCache} for invalidation mechanism on
 * possible actions under such circumstances.
 * 
 * The query execution model <em>does</em> account for context changes that do 
 * not impact the target query e.g. bind variables. 
 * 
 * @author Pinaki Poddar
 *
 * @since 1.3.0
 */
public class PreparedQuery  {
	private final String _ex;
	private final String _id;
	
	// Post-compilation state of an executable query, populated on construction
	private Class _candidate;
	private boolean _subclasses;
	private boolean _isProjection;
	
	/**
	 * 
	 * @param id an identifier for this query to be used as cache key
	 * @param corresponding data store language expression 
	 * @param compiled a compiled query 
	 */
	public PreparedQuery(String id, String sql, Query compiled) {
		this._id = id;
		this._ex = sql;
		
		if (compiled != null) {
			_candidate    = compiled.getCandidateType();
			_subclasses   = compiled.hasSubclasses();
			_isProjection = compiled.getProjectionAliases().length > 0;
		}
	}
	
	public String getIdentifier() {
		return _id;
	}
	
	public String getDatastoreAction() {
		return _ex;
	}
	
	/**
	 * Pours the post-compilation state held by this receiver to the given
	 * query.
	 */
	public void setInto(Query q) {
		if (!_isProjection)
			q.setCandidateType(_candidate, _subclasses);
	}
	
	public String toString() {
		return "PreparedQuery: [" + _id + "] --> [" + _ex + "]";
	}
}
