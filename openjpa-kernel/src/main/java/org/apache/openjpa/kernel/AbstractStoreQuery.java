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

import java.util.Map;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.openjpa.datacache.DataCache;
import org.apache.openjpa.kernel.exps.AggregateListener;
import org.apache.openjpa.kernel.exps.FilterListener;
import org.apache.openjpa.kernel.exps.QueryExpressions;
import org.apache.openjpa.kernel.exps.Value;
import org.apache.openjpa.lib.util.OrderedMap;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.UnsupportedException;

/**
 * Abstract {@link StoreQuery} that implements most methods as no-ops.
 *
 * @author Abe White
 * @since 0.4.0
 */
public abstract class AbstractStoreQuery implements StoreQuery {
    private static final long serialVersionUID = 1L;
    protected QueryContext ctx = null;

    @Override
    public QueryContext getContext() {
        return ctx;
    }

    @Override
    public void setContext(QueryContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean setQuery(Object query) {
        return false;
    }

    @Override
    public FilterListener getFilterListener(String tag) {
        return null;
    }

    @Override
    public AggregateListener getAggregateListener(String tag) {
        return null;
    }

    @Override
    public Object newCompilationKey() {
        return null;
    }

    @Override
    public Object newCompilation() {
        return null;
    }

    @Override
    public Object getCompilation() {
        return null;
    }

    @Override
    public void populateFromCompilation(Object comp) {
    }

    @Override
    public void invalidateCompilation() {
    }

    @Override
    public boolean supportsDataStoreExecution() {
        return false;
    }

    @Override
    public boolean supportsInMemoryExecution() {
        return false;
    }

    @Override
    public Executor newInMemoryExecutor(ClassMetaData meta, boolean subs) {
        throw new InternalException();
    }

    @Override
    public Executor newDataStoreExecutor(ClassMetaData meta, boolean subs) {
        throw new InternalException();
    }

    @Override
    public boolean supportsAbstractExecutors() {
        return false;
    }

    @Override
    public boolean requiresCandidateType() {
        return true;
    }

    @Override
    public boolean requiresParameterDeclarations() {
        return true;
    }

    @Override
    public boolean supportsParameterDeclarations() {
        return true;
    }

    @Override
    public Object evaluate(Object value, Object ob, Object[] params,
        OpenJPAStateManager sm) {
        throw new UnsupportedException();
    }

    /**
     * Abstract {@link Executor} that implements most methods as no-ops.
     */
    public static abstract class AbstractExecutor
        implements Executor {

        @Override
        public Number executeDelete(StoreQuery q, Object[] params) {
            try {
                return q.getContext().deleteInMemory(q, this, params);
            } finally {
                for (ClassMetaData cmd : getAccessPathMetaDatas(q)) {
                    DataCache cache = cmd.getDataCache();
                    if (cache != null && cache.getEvictOnBulkUpdate()) {
                        cache.removeAll(cmd.getDescribedType(), true);
                    }
                }
            }
        }

        @Override
        public Number executeUpdate(StoreQuery q, Object[] params) {
            try {
                return q.getContext().updateInMemory(q, this, params);
            } finally {
                for (ClassMetaData cmd : getAccessPathMetaDatas(q)) {
                    DataCache cache = cmd.getDataCache();
                    if (cache != null && cache.getEvictOnBulkUpdate()) {
                        cache.removeAll(cmd.getDescribedType(), true);
                    }
                }
            }
        }

        @Override
        public String[] getDataStoreActions(StoreQuery q, Object[] params,
            Range range) {
            return EMPTY_STRINGS;
        }

        @Override
        public void validate(StoreQuery q) {
        }


        @Override
        public QueryExpressions[] getQueryExpressions() {
            return null;
        }

        @Override
        public ResultShape<?> getResultShape(StoreQuery q) {
            return null;
        }

        @Override
        public void getRange(StoreQuery q, Object[] params, Range range) {
        }

        @Override
        public Object getOrderingValue(StoreQuery q, Object[] params,
            Object resultObject, int orderIndex) {
            return null;
        }

        @Override
        public boolean[] getAscending(StoreQuery q) {
            return EMPTY_BOOLEANS;
        }

        @Override
        public boolean isPacking(StoreQuery q) {
            return false;
        }

        @Override
        public String getAlias(StoreQuery q) {
            return null;
        }

        @Override
        public String[] getProjectionAliases(StoreQuery q) {
            return EMPTY_STRINGS;
        }

        @Override
        public Class<?>[] getProjectionTypes(StoreQuery q) {
            return EMPTY_CLASSES;
        }

        @Override
        public ClassMetaData[] getAccessPathMetaDatas(StoreQuery q) {
            return EMPTY_METAS;
        }

        @Override
        public int getOperation(StoreQuery q) {
            return OP_SELECT;
        }

        @Override
        public boolean isAggregate(StoreQuery q) {
            return false;
        }

        @Override
        public boolean isDistinct(StoreQuery q) {
            return false;
        }

        @Override
        public boolean hasGrouping(StoreQuery q) {
            return false;
        }

        @Override
        public OrderedMap<Object,Class<?>> getOrderedParameterTypes(StoreQuery q) {
            return EMPTY_ORDERED_PARAMS;
        }

        @Override
        public LinkedMap getParameterTypes(StoreQuery q) {
            LinkedMap result = new LinkedMap();
            result.putAll(getOrderedParameterTypes(q));
            return result;
        }

        @Override
        public Class<?> getResultClass(StoreQuery q) {
            return null;
        }

        @Override
        public Map<FieldMetaData,Value> getUpdates(StoreQuery q) {
            return null;
        }
    }
}
