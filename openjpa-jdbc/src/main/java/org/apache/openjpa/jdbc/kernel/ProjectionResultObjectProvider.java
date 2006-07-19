/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.jdbc.kernel;

import org.apache.openjpa.jdbc.kernel.exps.Val;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SelectExecutor;
import org.apache.openjpa.kernel.exps.QueryExpressions;

/**
 * Object provider implementation wrapped around a projection select.
 *
 * @author Abe White
 */
class ProjectionResultObjectProvider
    extends SelectResultObjectProvider {

    private final QueryExpressions[] _exps;

    /**
     * Constructor.
     *
     * @param sel the select to execute
     * @param store the store manager to delegate loading to
     * @param fetch the fetch configuration
     * @param exps the query expressions
     */
    public ProjectionResultObjectProvider(SelectExecutor sel, JDBCStore store,
        JDBCFetchState fetchState, QueryExpressions exps) {
        this(sel, store, fetchState, new QueryExpressions[]{ exps });
    }

    /**
     * Constructor.
     *
     * @param sel the select to execute
     * @param store the store manager to delegate loading to
     * @param fetch the fetch configuration
     * @param exps the query expressions
     */
    public ProjectionResultObjectProvider(SelectExecutor sel, JDBCStore store,
        JDBCFetchState fetchState, QueryExpressions[] exps) {
        super(sel, store, fetchState);
        _exps = exps;
    }

    public Object getResultObject()
        throws Exception {
        Result res = getResult();
        int idx = res.indexOf();
        Object[] arr = new Object[_exps[idx].projections.length];
        for (int i = 0; i < _exps[idx].projections.length; i++)
            arr[i] = ((Val) _exps[idx].projections[i]).load(res, getStore(),
                getFetchState());
        return arr;
    }
}
