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

package org.apache.openjpa.jdbc.kernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.SelectExecutor;
import org.apache.openjpa.kernel.PreparedQuery;
import org.apache.openjpa.kernel.Query;
import org.apache.openjpa.lib.rop.ResultList;

public class PreparedQueryImpl implements PreparedQuery {
    private final String _id;
    private String _sql;
    
    // Post-compilation state of an executable query, populated on construction
    private Class _candidate;
    private boolean _subclasses;
    private boolean _isProjection;
    
    // Parameters of the query
    private List    _params;
    private List    _userParams;
    
    
    /**
     * Construct.
     * 
     * @param id an identifier for this query to be used as cache key
     * @param compiled a compiled query 
     */
    public PreparedQueryImpl(String id, Query compiled) {
        this(id, null, compiled);
    }
    
    /**
     * Construct.
     * 
     * @param id an identifier for this query to be used as cache key
     * @param corresponding data store language query string 
     * @param compiled a compiled query 
     */
    public PreparedQueryImpl(String id, String sql, Query compiled) {
        this._id = id;
        this._sql = sql;
        if (compiled != null) {
            _candidate    = compiled.getCandidateType();
            _subclasses   = compiled.hasSubclasses();
            _isProjection = compiled.getProjectionAliases().length > 0;
        }
    }
    
    public String getIdentifier() {
        return _id;
    }
    
    public String getOriginalQuery() {
        return getIdentifier();
    }
    
    public String getTargetQuery() {
        return _sql;
    }
    
    public void setDatastoreAction(String sql) {
        _sql = sql;
    }
    
    /**
     * Pours the post-compilation state held by this receiver to the given
     * query.
     */
    public void setInto(Query q) {
        if (!_isProjection)
            q.setCandidateType(_candidate, _subclasses);
    }
    
    public boolean initialize(Object result) {
        boolean initialized = false;
        if (result instanceof ResultList) {
            Object provider = ((ResultList)result).getUserObject();
            if (provider instanceof SelectResultObjectProvider) {
                SelectResultObjectProvider rop = 
                    (SelectResultObjectProvider)provider;
                SelectExecutor selector = rop.getSelect();
                SQLBuffer buffer = selector == null ? null : selector.getSQL();
                if (buffer != null && !selector.hasMultipleSelects()) {
                    setDatastoreAction(buffer.getSQL());
                    setUserParameters(buffer.getUserParameters());
                    setParameters(buffer.getParameters());
                    initialized = true;
                }
            }
        }
        return initialized;
    }
    
    /**
     * Merge the given user parameters with its own parameter.
     * 
     * @return key index starting from 1 and corresponding values.
     */
    public Map<Integer, Object> reparametrize(Map user) {
        Map<Integer, Object> result = new HashMap<Integer, Object>();
        for (int i = 0; i < _params.size(); i++) {
            result.put(i, _params.get(i));
        }
        if (user == null)
            return result;
        for (Object key : user.keySet()) {
            List<Integer> indices = findUserParameterPositions(key);
            for (int j : indices)
                result.put(j, user.get(key));
        }
        return result;
    }
    
    private List<Integer> findUserParameterPositions(Object key) {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 1; _userParams != null && i < _userParams.size(); i+=2) {
            if (_userParams.get(i).equals(key))
                result.add((Integer)_userParams.get(i-1));
        }
        return result;
    }
    
    void setUserParameters(List list) {
        _userParams = list;
    }
    
    void setParameters(List list) {
        _params = list;
    }
    
    public String toString() {
        return "PreparedQuery: [" + getOriginalQuery() + "] --> [" + 
               getTargetQuery() + "]";
    }

}
