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
package org.apache.openjpa.kernel.exps;

import java.util.Map;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;

/**
 * Struct to hold the state of a parsed expression query.
 *
 * @author Abe White
 * @since 3.2
 * @nojavadoc
 */
public class QueryExpressions {

    public static final int DISTINCT_AUTO = 2 << 0;
    public static final int DISTINCT_TRUE = 2 << 1;
    public static final int DISTINCT_FALSE = 2 << 2;
    public static final Value[] EMPTY_VALUES = new Value[0];

    public boolean aggregate = false;
    public int distinct = DISTINCT_AUTO;
    public String alias = null;
    public Value[] projections = EMPTY_VALUES;
    public String[] projectionClauses = StoreQuery.EMPTY_STRINGS;
    public String[] projectionAliases = StoreQuery.EMPTY_STRINGS;
    public Class resultClass = null;
    public Expression filter = null;
    public Value[] grouping = EMPTY_VALUES;
    public String[] groupingClauses = StoreQuery.EMPTY_STRINGS;
    public Expression having = null;
    public Value[] ordering = EMPTY_VALUES;
    public boolean[] ascending = StoreQuery.EMPTY_BOOLEANS;
    public String[] orderingClauses = StoreQuery.EMPTY_STRINGS;
    public LinkedMap parameterTypes = StoreQuery.EMPTY_PARAMS;
    public int operation = QueryOperations.OP_SELECT;
    public ClassMetaData[] accessPath = StoreQuery.EMPTY_METAS;
    public String[] fetchPaths = StoreQuery.EMPTY_STRINGS;

    /**
     * Map of {@link FieldMetaData},{@link Value} for update statements.
     */
    public Map updates = null;
}
