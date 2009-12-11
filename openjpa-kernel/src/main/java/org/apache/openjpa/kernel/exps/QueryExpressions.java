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
package org.apache.openjpa.kernel.exps;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.ResultShape;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.kernel.exps.Context;
import org.apache.openjpa.lib.util.OrderedMap;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;

/**
 * Struct to hold the state of a parsed expression query.
 *
 * @author Abe White
 * @since 0.3.2
 * @nojavadoc
 */
@SuppressWarnings("serial")
public class QueryExpressions
    implements Serializable {

    public static final int DISTINCT_AUTO = 2 << 0;
    public static final int DISTINCT_TRUE = 2 << 1;
    public static final int DISTINCT_FALSE = 2 << 2;
    public static final Value[] EMPTY_VALUES = new Value[0];

    /**
     * Map of {@link FieldMetaData},{@link Value} for update statements.
     */
    public Map<Path, Value> updates = Collections.emptyMap();
    public int distinct = DISTINCT_AUTO;
    public String alias = null;
    public Value[] projections = EMPTY_VALUES;
    public String[] projectionClauses = StoreQuery.EMPTY_STRINGS;
    public String[] projectionAliases = StoreQuery.EMPTY_STRINGS;
    public Class<?> resultClass = null;
    public Expression filter = null;
    public Value[] grouping = EMPTY_VALUES;
    public String[] groupingClauses = StoreQuery.EMPTY_STRINGS;
    public Expression having = null;
    public Value[] ordering = EMPTY_VALUES;
    public boolean[] ascending = StoreQuery.EMPTY_BOOLEANS;
    public String[] orderingClauses = StoreQuery.EMPTY_STRINGS;
    public String[] orderingAliases = StoreQuery.EMPTY_STRINGS;
    public OrderedMap<Object,Class<?>> parameterTypes = StoreQuery.EMPTY_PARAMS;
    public int operation = QueryOperations.OP_SELECT;
    public ClassMetaData[] accessPath = StoreQuery.EMPTY_METAS;
    public String[] fetchPaths = StoreQuery.EMPTY_STRINGS;
    public String[] fetchInnerPaths = StoreQuery.EMPTY_STRINGS;
    public Value[] range = EMPTY_VALUES;
    private Boolean _aggregate = null;
    private Stack<Context> _contexts = null;
    public Object state;
    public ResultShape<?> shape;
    
    /**
     * Set reference to the JPQL query contexts.
     * @param contexts
     */
    public void setContexts(Stack<Context> contexts) {
        _contexts = contexts;
    }

    /**
     * Returns the current JPQL query context.
     * @return
     */
    public Context ctx() {
        return _contexts.peek();
    }

    /**
     * Whether this is an aggregate results.
     */
    public boolean isAggregate() {
        if (projections.length == 0)
            return false; 
        if (_aggregate == null)
            _aggregate = (AggregateExpressionVisitor.isAggregate(projections))
                ? Boolean.TRUE : Boolean.FALSE;
        return _aggregate.booleanValue();    
    }

    /**
     * Add an update.
     */
    public void putUpdate(Path path, Value val) {
        if (updates == Collections.EMPTY_MAP)
            updates = new LinkedHashMap<Path, Value>();
        updates.put(path, val);
    }

    /**
     * Visitor to determine whether our projections are aggregates.
     */
    private static class AggregateExpressionVisitor
        extends AbstractExpressionVisitor {
        
        private Value _sub = null;
        private boolean _agg = false;

        /**
         * Return whether the given values include projections.
         */
        public static boolean isAggregate(Value[] vals) {
            if (vals.length == 0)
                return false;
            AggregateExpressionVisitor v = new AggregateExpressionVisitor();
            for (int i = 0; i < vals.length && !v._agg; i++)
                vals[i].acceptVisit(v);
            return v._agg;
        }

        public void enter(Value val) {
            if (_agg)
                return;
            if (_sub == null) {
                if (val.isAggregate())
                    _agg = true;
            } else if (val instanceof Subquery)
                _sub = val;
        }

        public void exit(Value val) {
            if (val == _sub)
                _sub = null;
        }
    }
}
