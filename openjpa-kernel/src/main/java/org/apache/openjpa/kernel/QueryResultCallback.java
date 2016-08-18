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

import org.apache.openjpa.kernel.StoreQuery.Executor;
import org.apache.openjpa.kernel.StoreQuery.Range;
import org.apache.openjpa.lib.rop.BatchedResultObjectProvider;

/**
 * A callabck is used when a query results in multiple non-identical result sets.
 * Designed to use with Stored Procedure Query.
 *
 * @author ppoddar
 */
public class QueryResultCallback {
    private final StoreQuery storeQuery;
    private final StoreQuery.Executor executor;
    private final BatchedResultObjectProvider parent;
    private final StoreQuery.Range range;
    private final QueryImpl kernel;

    public QueryResultCallback(QueryImpl kernel, StoreQuery storeQuery, Executor executor,
                               BatchedResultObjectProvider parent, Range range) {
        super();
        this.kernel = kernel;
        this.storeQuery = storeQuery;
        this.executor = executor;
        this.parent = parent;
        this.range = range;
    }

    public Object callback() throws Exception {
        return kernel.toResult(storeQuery, executor, parent.getResultObject(), range);
    }

    public boolean hasMoreResults() {
        return parent.hasMoreResults();
    }

    public boolean getExecutionResult() {
        return parent.getExecutionResult();
    }

    public int getUpdateCount() {
        return parent.getUpdateCount();
    }

    public Object getOut(int position) {
        return parent.getOut(position);
    }

    public Object getOut(String name) {
        return parent.getOut(name);
    }
}
