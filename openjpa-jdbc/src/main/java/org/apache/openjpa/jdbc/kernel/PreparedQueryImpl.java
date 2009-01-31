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

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.SelectExecutor;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.PreparedQuery;
import org.apache.openjpa.kernel.Query;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.InternalException;

/**
 * Implements {@link PreparedQuery} for SQL queries.
 * 
 * @author Pinaki Poddar
 *
 */
public class PreparedQueryImpl implements PreparedQuery {
    private final String _id;
    private String _sql;
    
    // Post-compilation state of an executable query, populated on construction
    private Class _candidate;
    private boolean _subclasses;
    private boolean _isProjection;
    
    // Parameters of the query
    private List    _params;
    // Position of the user defined parameters in the _params list
    private Map<Object, int[]>    _userParamPositions;
    
    
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
    
    /**
     * Get the original query string which is same as the identifier of this 
     * receiver.
     */
    public String getOriginalQuery() {
        return getIdentifier();
    }
    
    public String getTargetQuery() {
        return _sql;
    }
    
    void setTargetQuery(String sql) {
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
    
    /**
     * Initialize this receiver with post-execution result.
     * The input argument is processed only if it is a {@link ResultList} with
     * an attached {@link SelectResultObjectProvider} as its
     * {@link ResultList#getUserObject() user object}. 
     */
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
                    setTargetQuery(buffer.getSQL());
                    setParameters(buffer.getParameters());
                    setUserParameterPositions(buffer.getUserParameters());
                    initialized = true;
                }
            }
        }
        return initialized;
    }
    
    /**
     * Merge the given user parameters with its own parameter. The given map
     * must be compatible with the user parameters extracted during 
     * {@link #initialize(Object) initialization}. 
     * 
     * @return 0-based parameter index mapped to corresponding values.
     */
    public Map<Integer, Object> reparametrize(Map user, Broker broker) {
        Map<Integer, Object> result = new HashMap<Integer, Object>();
        for (int i = 0; i < _params.size(); i++) {
            result.put(i, _params.get(i));
        }
        if (user == null || user.isEmpty())
            return result;
        for (Object key : user.keySet()) {
            int[] indices = _userParamPositions.get(key);
            if (indices == null)
                continue;
            Object value = user.get(key);
            if (ImplHelper.isManageable(value)) {
                setPersistenceCapableParameter(result, value, indices, broker);
            } else {
                for (int j : indices)
                    result.put(j, value);
            }
        }
        return result;
    }
    
    /**
     * Calculate primary key identity value(s) of the given managable instance
     * and fill in the given map.
     * 
     * @param values a map of integer parameter index to parameter value
     * @param pc a manageable instance
     * @param indices the indices of the column values
     * @param broker used to obtain the primary key values
     */
    private void setPersistenceCapableParameter(Map<Integer,Object> result, 
        Object pc, int[] indices, Broker broker) {
        JDBCStore store = (JDBCStore)broker.getStoreManager()
            .getInnermostDelegate();
        MappingRepository repos = store.getConfiguration()
            .getMappingRepositoryInstance();
        ClassMapping mapping = repos.getMapping(pc.getClass(), 
            broker.getClassLoader(), true);
        Column[] pks = mapping.getPrimaryKeyColumns();
        Object cols = mapping.toDataStoreValue(pc, pks, store);
        if (cols instanceof Object[]) {
            Object[] array = (Object[])cols;
            int n = array.length;
            if (n > indices.length || indices.length%n != 0)
                throw new InternalException();
            int k = 0;
            for (int j : indices) {
                result.put(j, array[k%n]);
                k++;
            }
        } else {
            for (int j : indices) {
                result.put(j, cols);
            }
        } 
    }
    
    /**
     * Marks the positions of user parameters.
     * 
     * @param list odd elements are numbers representing the position of a 
     * user parameter in the _param list. Even elements are the user parameter
     * key. A user parameter key may appear more than once.
     */
    void setUserParameterPositions(List list) {
        _userParamPositions = new HashMap<Object, int[]>();
        for (int i = 1; list != null && i < list.size(); i+=2) {
            Object key = list.get(i);
            int p = (Integer)list.get(i-1);
            int[] positions = _userParamPositions.get(key);
            if (positions == null) {
                positions = new int[]{p};
            } else {
                int[] temp = new int[positions.length+1];
                System.arraycopy(positions, 0, temp, 0, positions.length);
                temp[positions.length] = p;
                positions = temp;
            }
            _userParamPositions.put(key, positions);
        }
    }
    
    void setParameters(List list) {
        _params = new ArrayList();
        _params.addAll(list);
    }
    
    public String toString() {
        return "PreparedQuery: [" + getOriginalQuery() + "] --> [" + 
               getTargetQuery() + "]";
    }

}
