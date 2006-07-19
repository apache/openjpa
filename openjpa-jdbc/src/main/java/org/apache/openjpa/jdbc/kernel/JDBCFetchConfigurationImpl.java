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

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.FetchConfigurationImpl;
import org.apache.openjpa.kernel.FetchState;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.rop.EagerResultList;
import org.apache.openjpa.lib.rop.ListResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.rop.SimpleResultList;
import org.apache.openjpa.lib.rop.SoftRandomAccessResultList;
import org.apache.openjpa.lib.rop.WindowResultList;
import org.apache.openjpa.lib.util.Localizer;

/**
 * JDBC extensions to OpenJPA's {@link FetchConfiguration}.
 *
 * @author Abe White
 * @nojavadoc
 */
public class JDBCFetchConfigurationImpl
    extends FetchConfigurationImpl
    implements JDBCFetchConfiguration {

    private static final String[] EMPTY_STRINGS = new String[0];

    private static final Localizer _loc = Localizer.forPackage
        (JDBCFetchConfigurationImpl.class);

    private int _eagerMode = 0;
    private int _subclassMode = 0;
    private int _type = 0;
    private int _direction = 0;
    private int _size = 0;
    private int _syntax = 0;

    private Set _joins = null;

    public void setContext(StoreContext ctx) {
        super.setContext(ctx);
        JDBCConfiguration conf = getJDBCConfiguration();
        if (conf == null)
            return;

        setEagerFetchMode(conf.getEagerFetchModeConstant());
        setSubclassFetchMode(conf.getSubclassFetchModeConstant());
        setResultSetType(conf.getResultSetTypeConstant());
        setFetchDirection(conf.getFetchDirectionConstant());
        setLRSSize(conf.getLRSSizeConstant());
        setJoinSyntax(conf.getDBDictionaryInstance().joinSyntax);
    }

    protected FetchConfigurationImpl newInstance() {
        JDBCFetchConfigurationImpl fetch = new JDBCFetchConfigurationImpl();
        return fetch;
    }

    public void copy(FetchConfiguration fetch) {
        super.copy(fetch);
        JDBCFetchConfiguration jf = (JDBCFetchConfiguration) fetch;
        setEagerFetchMode(jf.getEagerFetchMode());
        setSubclassFetchMode(jf.getSubclassFetchMode());
        setResultSetType(jf.getResultSetType());
        setFetchDirection(jf.getFetchDirection());
        setLRSSize(jf.getLRSSize());
        setJoinSyntax(jf.getJoinSyntax());
        addJoins(Arrays.asList(jf.getJoins()));
    }

    public int getEagerFetchMode() {
        return _eagerMode;
    }

    public JDBCFetchConfiguration setEagerFetchMode(int mode) {
        if (mode == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                mode = conf.getEagerFetchModeConstant();
        }
        if (mode != DEFAULT)
            _eagerMode = mode;
        return this;
    }

    public int getSubclassFetchMode() {
        return _subclassMode;
    }

    public int getSubclassFetchMode(ClassMapping cls) {
        if (cls == null)
            return _subclassMode;
        int mode = cls.getSubclassFetchMode();
        if (mode == DEFAULT)
            return _subclassMode;
        return Math.min(mode, _subclassMode);
    }

    public JDBCFetchConfiguration setSubclassFetchMode(int mode) {
        if (mode == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                mode = conf.getSubclassFetchModeConstant();
        }
        if (mode != DEFAULT)
            _subclassMode = mode;
        return this;
    }

    public int getResultSetType() {
        return _type;
    }

    public JDBCFetchConfiguration setResultSetType(int type) {
        if (type == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                _type = conf.getResultSetTypeConstant();
        } else
            _type = type;
        return this;
    }

    public int getFetchDirection() {
        return _direction;
    }

    public JDBCFetchConfiguration setFetchDirection(int direction) {
        if (direction == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                _direction = conf.getFetchDirectionConstant();
        } else
            _direction = direction;
        return this;
    }

    public int getLRSSize() {
        return _size;
    }

    public JDBCFetchConfiguration setLRSSize(int size) {
        if (size == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                _size = conf.getLRSSizeConstant();
        } else
            _size = size;
        return this;
    }

    public int getJoinSyntax() {
        return _syntax;
    }

    public JDBCFetchConfiguration setJoinSyntax(int syntax) {
        if (syntax == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                _syntax = conf.getDBDictionaryInstance().joinSyntax;
        } else
            _syntax = syntax;
        return this;
    }

    public ResultList newResultList(ResultObjectProvider rop) {
        // if built around a list, just use a simple wrapper
        if (rop instanceof ListResultObjectProvider)
            return new SimpleResultList(rop);

        // if built around a paging list, use a window provider with the
        // same window size
        if (rop instanceof PagingResultObjectProvider)
            return new WindowResultList(rop, ((PagingResultObjectProvider)
                rop).getPageSize());

        // if fetch size < 0 just read in all results immediately
        if (getFetchBatchSize() < 0)
            return new EagerResultList(rop);

        // if foward only or forward direction use a forward window
        if (_type == ResultSet.TYPE_FORWARD_ONLY
            || _direction == ResultSet.FETCH_FORWARD
            || !rop.supportsRandomAccess()) {
            if (getFetchBatchSize() > 0 && getFetchBatchSize() <= 50)
                return new WindowResultList(rop, getFetchBatchSize());
            return new WindowResultList(rop, 50);
        }

        // if skipping around use a caching random access list
        if (_direction == ResultSet.FETCH_UNKNOWN)
            return new SoftRandomAccessResultList(rop);

        // scrolling reverse... just use non-caching simple result list
        return new SimpleResultList(rop);
    }

    /**
     * Access JDBC configuration information. May return null if not a
     * JDBC back-end (possible to get a JDBCFetchConfiguration on non-JDBC
     * back end in remote client).
     */
    private JDBCConfiguration getJDBCConfiguration() {
        StoreContext ctx = getContext();
        if (ctx == null)
            return null;

        OpenJPAConfiguration conf = ctx.getConfiguration();
        if (!(conf instanceof JDBCConfiguration))
            return null;
        return (JDBCConfiguration) conf;
    }

    public synchronized String[] getJoins() {
        if (_joins == null || _joins.isEmpty())
            return EMPTY_STRINGS;
        return (String[]) _joins.toArray(new String[_joins.size()]);
    }

    public synchronized boolean hasJoin(String field) {
        return _joins != null && field != null && _joins.contains(field);
    }

    public synchronized JDBCFetchConfiguration addJoin(String field) {
        if (_joins == null)
            _joins = new HashSet();
        _joins.add(field);
        return this;
    }

    public synchronized JDBCFetchConfiguration addJoins(Collection fields) {
        if (fields.isEmpty())
            return this;
        if (_joins == null)
            _joins = new HashSet();
        _joins.addAll(fields);
        return this;
    }

    public synchronized JDBCFetchConfiguration removeJoin(String field) {
        if (_joins != null)
            _joins.remove(field);
        return this;
    }

    public synchronized JDBCFetchConfiguration removeJoins(Collection fields) {
        if (_joins != null)
            _joins.removeAll(fields);
        return this;
    }

    public synchronized JDBCFetchConfiguration clearJoins() {
        if (_joins != null)
            _joins.clear();
        return this;
    }

    public FetchState newFetchState() {
        return new JDBCFetchStateImpl(this);
    }
}
