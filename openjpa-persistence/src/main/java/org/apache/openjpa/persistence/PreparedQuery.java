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
package org.apache.openjpa.persistence;

import org.apache.openjpa.kernel.Query;
import org.apache.openjpa.kernel.QueryHints;

/**
 * A prepared query binds a compiled query to its target SQL. 
 * 
 * The target SQL is meant to be executed directly bypassing the critical 
 * cost of constructing the SQL on every query execution. As the subsequent 
 * execution of a cached query will bypass compilation as a JPQL query,
 * the post-compilation state of the original query is captured in this receiver
 * to be transferred to the executable query instance.  
 * 
 * The target SQL depends on context of query execution such as fetch plan or
 * lock group. No attempt is made to monitor and automatically invalidate a
 * prepared SQL when the same query is executed with different context 
 * parameters.
 * 
 * The user must set a {@link QueryHints#HINT_INVALIDATE_PREPARED_QUERY hint} to 
 * invalidate.
 * 
 * @author Pinaki Poddar
 *
 * @since 1.3.0
 * @nojavadoc
 */
public class PreparedQuery  {
	private final String _sql;
	private final String _id;
	
	// Post-compilation state of an executable query
	Class _candidate = null;
	boolean _subclasses = true;
	boolean _isProjection = false;
	
	public PreparedQuery(String id, String sql, Query compiled) {
		this._id = id;
		this._sql = sql;
		
		_candidate = compiled.getCandidateType();
		_subclasses = compiled.hasSubclasses();
		_isProjection = compiled.getProjectionAliases().length > 0;
	}
	
	public String getIdentifier() {
		return _id;
	}
	
	public String getSQL() {
		return _sql;
	}
	
	public String toString() {
		return "PreparedQuery " + _id + "==>" + _sql;
	}
	
	void setInto(Query q) {
//		q.setCandidateCollection(last.getCandidateCollection());
//		q.setCandidateExtent(last.getCandidateExtent());
		
		if (!_isProjection)
			q.setCandidateType(_candidate, _subclasses);
		
//		q.setIgnoreChanges(last.getIgnoreChanges());
//		q.setRange(last.getStartRange(), last.getEndRange());
//		q.setReadOnly(last.isReadOnly());
//		q.setResultMapping(last.getResultMappingScope(), 
//		last.getResultMappingName());
//		q.setResultType(last.getResultType());
//		q.setUnique(last.isUnique());
	}
}
