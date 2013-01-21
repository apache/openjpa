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
     * A boolean directive to generate literal directly into the SQL statement instead of using position parameter,
     * if possible.
     */
    public static final String HINT_USE_LITERAL_IN_SQL = "openjpa.hint.UseLiteralInSQL";
}
