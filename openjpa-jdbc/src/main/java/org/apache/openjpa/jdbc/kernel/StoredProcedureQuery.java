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

import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.meta.QueryResultMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.StoredProcedure;
import org.apache.openjpa.kernel.AbstractStoreQuery;
import org.apache.openjpa.kernel.QueryContext;
import org.apache.openjpa.kernel.QueryOperations;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MultiQueryMetaData;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.util.InternalException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Executes a stored procedure.
 *
 * @author ppoddar
 *
 */
@SuppressWarnings("serial")
public class StoredProcedureQuery extends AbstractStoreQuery {
    private static final Object[] NO_PARAM = new Object[0];

    JDBCStore _store;
    StoredProcedure _proc;
    private MultiQueryMetaData _meta;

    public StoredProcedureQuery(JDBCStore store) {
        _store = store;
    }

    public int getOperation() {
        return QueryOperations.OP_SELECT;
    }

    public StoredProcedure getProcedure() {
        return _proc;
    }

    public DBDictionary getDictionary() {
        return _store.getDBDictionary();
    }

    @Override
    public boolean setQuery(Object meta) {
        if (meta == null || meta instanceof MultiQueryMetaData) {
            _meta = (MultiQueryMetaData)meta;
            return true;
        } else {
            throw new InternalException("Unknown " + meta);
        }
    }

    public Executor newDataStoreExecutor(ClassMetaData meta,  boolean subclasses) {
        List<QueryResultMapping> mappings = null;
        List<Class<?>> classes = null;
        if (_meta != null) {
            List<QueryMetaData> parts = _meta.getComponents();
            if (parts != null && !parts.isEmpty()) {
                mappings = new ArrayList<QueryResultMapping>();
                classes = new ArrayList<Class<?>>();
                MappingRepository repos = _store.getConfiguration().getMappingRepositoryInstance();
                for (QueryMetaData part : parts) {
                    QueryResultMapping mapping = repos.getQueryResultMapping(ctx.getResultMappingScope(),
                            part.getResultSetMappingName(),
                            null, true);
                    if (mapping != null) {
                        mappings.add(mapping);
                    }
                    if (part.getResultType() != null) {
                        classes.add(part.getResultType());
                    }
                }
            }
        }
        return new StoredProcedureQueryExecutor(this, mappings, classes);
    }

    public boolean supportsParameterDeclarations() {
        return false;
    }

    public boolean supportsDataStoreExecution() {
        return true;
    }

    public boolean requiresCandidateType() {
        return false;
    }

    public boolean requiresParameterDeclarations() {
        return false;
    }


    public class StoredProcedureQueryExecutor  extends AbstractExecutor {
        private final List<Class<?>> _resultClasses;
        private final List<QueryResultMapping> _resultMappings;

        public StoredProcedureQueryExecutor(StoredProcedureQuery q, List<QueryResultMapping> resultMapping,
                                            List<Class<?>> classes) {
            QueryContext ctx = q.getContext();
            _resultMappings = resultMapping;
            _resultClasses = classes;
            // Look for the named Stored Procedure in the database
            String procName = ctx.getQueryString();
            _proc = getStoredProcedure(_store.getConnection(), _store.getDBDictionary(), procName);
            if (_proc == null) {
                throw new RuntimeException("Can not find stored procedure " + procName);
            }
        }

        StoredProcedure getStoredProcedure(Connection conn, DBDictionary dict, String procedureName) {
            try {
                StoredProcedure sp = dict.getStoredProcedure(conn.getMetaData(), null, null, procedureName);
                if (sp != null) {
                    return sp;
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            } finally {
                try {
                    conn.close();
                } catch (SQLException ex) {

                }
            }
            throw new RuntimeException("Procedure [" + procedureName + "] not found");
        }

        @Override
        public ResultObjectProvider executeQuery(StoreQuery q, Object[] params, Range range) {
            try {
                DBDictionary dict = _store.getDBDictionary();
                Connection conn = _store.getConnection();
                CallableStatement stmnt = conn.prepareCall(_proc.getCallSQL());

                final StoredProcedureQuery spq = StoredProcedureQuery.class.cast(q);
                for (Column c : spq.getProcedure().getInColumns()) {
                    dict.setUnknown(stmnt, c.getIndex() + 1, params[c.getIndex()], c);
                }
                for (Column c : spq.getProcedure().getInOutColumns()) {
                    final int index = c.getIndex() + 1;
                    stmnt.registerOutParameter(index, c.getType());
                    dict.setUnknown(stmnt, index, params[index - 1], c);
                }
                for (Column c : spq.getProcedure().getOutColumns()) {
                    stmnt.registerOutParameter(c.getIndex() + 1, c.getType());
                }

                JDBCFetchConfiguration fetch = (JDBCFetchConfiguration)q.getContext().getFetchConfiguration();
                ResultObjectProvider rop = new XROP(_resultMappings, _resultClasses, _store, fetch, stmnt);
                rop.open();
                return rop;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object[] toParameterArray(StoreQuery q, Map<?, ?> userParams) {
            if (userParams == null) return NO_PARAM;
            Object[] array = new Object[userParams.size()];
            int i = 0;
            StoredProcedureQuery storedProcedureQuery = StoredProcedureQuery.class.cast(q);
            for (final Column[] columns : asList(
                    storedProcedureQuery.getProcedure().getInColumns(),
                    storedProcedureQuery.getProcedure().getInOutColumns())) {
                for (Column c : columns) {
                    array[i] = userParams.get(c.getIdentifier().getName());
                    if (array[i++] == null) {
                        userParams.get(c.getIndex());
                    }
                }
            }
            return array;
        }

    }
}
