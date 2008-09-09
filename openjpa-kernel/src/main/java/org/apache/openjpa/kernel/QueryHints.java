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
 * Standard query hint keys.
 */
public interface QueryHints {
    
    /** 
     * Hint to specify the number of rows to optimize for.
     */
    public static final String HINT_RESULT_COUNT =
        "openjpa.hint.OptimizeResultCount";
    
    /**
     * Hints to signal that the JPQL/SQL query string contains a parameter
     * marker <code>?</code> character. By default, the query string is parsed
     * to count number of parameters assuming that all <code>?</code> characters
     * designate a bind parameter. This assumption makes the parse faster.
     */
    public static final String HINT_PARAM_MARKER_IN_QUERY =
    	"openjpa.hint.ParameterMarkerInQuery";
    
    /**
     * A directive to invalidate any prepared SQL that might have been cached
     * against a JPQL query. The target SQL corresponding to a JPQL depends on
     * several context parameters such as fetch configuration, lock mode etc.
     * If a query is executed repeatedly and hence its SQL is cached for faster
     * execution then if any of the contextual parameters change across query
     * execution then the user must supply this hint to invalidate the cached
     * SQL query. 
     * The alternative to monitor any such change for automatic invalidation 
     * has a constant performance penalty for the frequent use case where a 
     * query is repeatedly executed in different persistent context with the 
     * same fetch plan or locking.  
     */
    public static final String HINT_INVALIDATE_PREPARED_QUERY =
    	"openjpa.hint.InvalidatePreparedQuery";
}
