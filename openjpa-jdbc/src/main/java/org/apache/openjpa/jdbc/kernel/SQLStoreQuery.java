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

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.meta.QueryResultMapping;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.ResultSetResult;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.AbstractStoreQuery;
import org.apache.openjpa.kernel.QueryContext;
import org.apache.openjpa.kernel.StoreQuery;
import org.apache.openjpa.lib.rop.RangeResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.UserException;

/**
 * A SQL query.
 *
 * @author Abe White
 * @nojavadoc
 */
public class SQLStoreQuery
    extends AbstractStoreQuery {

    private static final Localizer _loc = Localizer.forPackage
        (SQLStoreQuery.class);

    private transient final JDBCStore _store;

    /**
     * Construct a query managed by the given context.
     */
    public SQLStoreQuery(JDBCStore store) {
        _store = store;
    }

    public JDBCStore getStore() {
        return _store;
    }

    /**
     * Utility method to substitute '?num' for parameters in the given SQL
     * statement, and re-order the <code>params</code> array to match
     * the order of the specified parameters.
     */
    private static String substituteParams(String sql, List params)
        throws IOException {
        // if there's no "?1" positional parameter, then we don't need to
        // perform the parsing process
        if (sql.indexOf("?1") == -1)
            return sql;

        List paramOrder = new ArrayList();
        StreamTokenizer tok = new StreamTokenizer(new StringReader(sql));
        tok.resetSyntax();
        tok.quoteChar('\'');
        tok.wordChars('0', '9');
        tok.wordChars('?', '?');

        StringBuffer buf = new StringBuffer(sql.length());
        for (int ttype; (ttype = tok.nextToken()) != StreamTokenizer.TT_EOF;) {
            switch (ttype) {
                case StreamTokenizer.TT_WORD:
                    // a token is a positional parameter if it starts with
                    // a "?" and the rest of the token are all numbers
                    if (tok.sval.startsWith("?") && tok.sval.length() > 1 &&
                        tok.sval.substring(1).indexOf("?") == -1) {
                        buf.append("?");
                        paramOrder.add(Integer.valueOf(tok.sval.substring(1)));
                    } else
                        buf.append(tok.sval);
                    break;
                case'\'':
                    buf.append('\'');
                    if (tok.sval != null) {
                        buf.append(tok.sval);
                        buf.append('\'');
                    }
                    break;
                default:
                    buf.append((char) ttype);
            }
        }

        // now go through the paramOrder list and re-order the params array
        List translated = new ArrayList();
        for (Iterator i = paramOrder.iterator(); i.hasNext();) {
            int index = ((Number) i.next()).intValue() - 1;
            if (index >= params.size())
                throw new UserException(_loc.get("sqlquery-missing-params",
                    sql, String.valueOf(index), params));
            translated.add(params.get(index));
        }

        // transfer the translated list into the original params list
        params.clear();
        params.addAll(translated);
        return buf.toString();
    }

    public boolean supportsParameterDeclarations() {
        return false;
    }

    public boolean supportsDataStoreExecution() {
        return true;
    }

    public Executor newDataStoreExecutor(ClassMetaData meta,
        boolean subclasses) {
        return new SQLExecutor(this, meta);
    }

    public boolean requiresCandidateType() {
        return false;
    }

    public boolean requiresParameterDeclarations() {
        return false;
    }

    /**
     * Executes the filter as a SQL query.
     */
    private static class SQLExecutor
        extends AbstractExecutor {

        private final ClassMetaData _meta;
        private final boolean _select;
        private final QueryResultMapping _resultMapping;

        public SQLExecutor(SQLStoreQuery q, ClassMetaData candidate) {
            QueryContext ctx = q.getContext();
            String resultMapping = ctx.getResultMappingName();
            if (resultMapping == null)
                _resultMapping = null;
            else {
                ClassLoader envLoader = ctx.getStoreContext().getClassLoader();
                MappingRepository repos = q.getStore().getConfiguration().
                    getMappingRepository();
                _resultMapping = repos.getQueryResultMapping
                    (ctx.getResultMappingScope(), resultMapping, envLoader,
                        true);
            }
            _meta = candidate;

            String sql = ctx.getQueryString();
            if (sql != null)
                sql = sql.trim();
            if (sql == null || sql.length() == 0)
                throw new UserException(_loc.get("no-sql"));
            _select = sql.length() > 6
                && sql.substring(0, 6).equalsIgnoreCase("select");
        }

        public ResultObjectProvider executeQuery(StoreQuery q,
            Object[] params, boolean lrs, long startIdx, long endIdx) {
            JDBCStore store = ((SQLStoreQuery) q).getStore();
            DBDictionary dict = store.getDBDictionary();
            String sql = q.getContext().getQueryString();

            List paramList;
            if (params.length > 0) {
                paramList = new ArrayList(Arrays.asList(params));
                try {
                    sql = substituteParams(sql, paramList);
                } catch (IOException ioe) {
                    throw new UserException(ioe);
                }
            } else
                paramList = Collections.EMPTY_LIST;

            SQLBuffer buf = new SQLBuffer(dict).append(sql);
            Connection conn = store.getConnection();
            JDBCFetchConfiguration fetch = (JDBCFetchConfiguration)
                q.getContext().getFetchConfiguration();

            ResultObjectProvider rop;
            PreparedStatement stmnt = null;
            try {
                // use the right method depending on sel vs. proc, lrs setting
                if (_select && !lrs)
                    stmnt = buf.prepareStatement(conn);
                else if (_select)
                    stmnt = buf.prepareStatement(conn, fetch, -1, -1);
                else if (!lrs)
                    stmnt = buf.prepareCall(conn);
                else
                    stmnt = buf.prepareCall(conn, fetch, -1, -1);

                int index = 0;
                for (Iterator i = paramList.iterator(); i.hasNext();)
                    dict.setUnknown(stmnt, ++index, i.next(), null);

                ResultSetResult res = new ResultSetResult(conn, stmnt,
                    stmnt.executeQuery(), store);
                if (_resultMapping != null)
                    rop = new MappedQueryResultObjectProvider(_resultMapping,
                        store, fetch, res);
                else if (q.getContext().getCandidateType() != null)
                    rop = new GenericResultObjectProvider((ClassMapping) _meta,
                        store, fetch, res);
                else
                    rop = new SQLProjectionResultObjectProvider(store, fetch,
                        res, q.getContext().getResultType());
            } catch (SQLException se) {
                if (stmnt != null)
                    try {
                        stmnt.close();
                    } catch (SQLException se2) {
                    }
                try {
                    conn.close();
                } catch (SQLException se2) {
                }
                throw SQLExceptions.getStore(se, dict);
            }

            if (startIdx != 0 || endIdx != Long.MAX_VALUE)
                rop = new RangeResultObjectProvider(rop, startIdx, endIdx);
            return rop;
        }

        public String[] getDataStoreActions(StoreQuery q, Object[] params,
            long startIdx, long endIdx) {
            return new String[]{ q.getContext().getQueryString() };
        }

        public boolean isPacking(StoreQuery q) {
            return q.getContext().getCandidateType() == null;
        }
    }
}
