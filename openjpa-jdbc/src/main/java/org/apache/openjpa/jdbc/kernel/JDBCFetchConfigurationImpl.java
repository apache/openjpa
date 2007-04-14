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

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.FetchConfigurationImpl;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.lib.rop.EagerResultList;
import org.apache.openjpa.lib.rop.ListResultObjectProvider;
import org.apache.openjpa.lib.rop.ResultList;
import org.apache.openjpa.lib.rop.ResultObjectProvider;
import org.apache.openjpa.lib.rop.SimpleResultList;
import org.apache.openjpa.lib.rop.SoftRandomAccessResultList;
import org.apache.openjpa.lib.rop.WindowResultList;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.util.UserException;

/**
 * JDBC extensions to OpenJPA's {@link FetchConfiguration}.
 *
 * @author Abe White
 * @nojavadoc
 */
public class JDBCFetchConfigurationImpl
    extends FetchConfigurationImpl
    implements JDBCFetchConfiguration {

    private static final Localizer _loc = Localizer.forPackage
        (JDBCFetchConfigurationImpl.class);

    /**
     * Configurable JDBC state shared throughout a traversal chain.
     */
    private static class JDBCConfigurationState implements Serializable {
        public int eagerMode = 0;
        public int subclassMode = 0;
        public int type = 0;
        public int direction = 0;
        public int size = 0;
        public int syntax = 0;
        public Set joins = null;
        public int isolationLevel = -1;
    }

    private final JDBCConfigurationState _state;

    public JDBCFetchConfigurationImpl() {
        this(null, null);
    }

    private JDBCFetchConfigurationImpl(ConfigurationState state, 
        JDBCConfigurationState jstate) {
        super(state);
        _state = (jstate == null) ? new JDBCConfigurationState() : jstate;
    }

    protected FetchConfigurationImpl newInstance(ConfigurationState state) {
        JDBCConfigurationState jstate = (state == null) ? null : _state;
        return new JDBCFetchConfigurationImpl(state, jstate);
    }

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

    public void copy(FetchConfiguration fetch) {
        super.copy(fetch);
        JDBCFetchConfiguration jf = (JDBCFetchConfiguration) fetch;
        setEagerFetchMode(jf.getEagerFetchMode());
        setSubclassFetchMode(jf.getSubclassFetchMode());
        setResultSetType(jf.getResultSetType());
        setFetchDirection(jf.getFetchDirection());
        setLRSSize(jf.getLRSSize());
        setJoinSyntax(jf.getJoinSyntax());
        addJoins(jf.getJoins());
    }

    public int getEagerFetchMode() {
        return _state.eagerMode;
    }

    public JDBCFetchConfiguration setEagerFetchMode(int mode) {
        if (mode == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                mode = conf.getEagerFetchModeConstant();
        }
        if (mode != DEFAULT)
            _state.eagerMode = mode;
        return this;
    }

    public int getSubclassFetchMode() {
        return _state.subclassMode;
    }

    public int getSubclassFetchMode(ClassMapping cls) {
        if (cls == null)
            return _state.subclassMode;
        int mode = cls.getSubclassFetchMode();
        if (mode == DEFAULT)
            return _state.subclassMode;
        return Math.min(mode, _state.subclassMode);
    }

    public JDBCFetchConfiguration setSubclassFetchMode(int mode) {
        if (mode == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                mode = conf.getSubclassFetchModeConstant();
        }
        if (mode != DEFAULT)
            _state.subclassMode = mode;
        return this;
    }

    public int getResultSetType() {
        return _state.type;
    }

    public JDBCFetchConfiguration setResultSetType(int type) {
        if (type == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                _state.type = conf.getResultSetTypeConstant();
        } else
            _state.type = type;
        return this;
    }

    public int getFetchDirection() {
        return _state.direction;
    }

    public JDBCFetchConfiguration setFetchDirection(int direction) {
        if (direction == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                _state.direction = conf.getFetchDirectionConstant();
        } else
            _state.direction = direction;
        return this;
    }

    public int getLRSSize() {
        return _state.size;
    }

    public JDBCFetchConfiguration setLRSSize(int size) {
        if (size == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                _state.size = conf.getLRSSizeConstant();
        } else
            _state.size = size;
        return this;
    }

    public int getJoinSyntax() {
        return _state.syntax;
    }

    public JDBCFetchConfiguration setJoinSyntax(int syntax) {
        if (syntax == DEFAULT) {
            JDBCConfiguration conf = getJDBCConfiguration();
            if (conf != null)
                _state.syntax = conf.getDBDictionaryInstance().joinSyntax;
        } else
            _state.syntax = syntax;
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
        if (_state.type == ResultSet.TYPE_FORWARD_ONLY
            || _state.direction == ResultSet.FETCH_FORWARD
            || !rop.supportsRandomAccess()) {
            if (getFetchBatchSize() > 0 && getFetchBatchSize() <= 50)
                return new WindowResultList(rop, getFetchBatchSize());
            return new WindowResultList(rop, 50);
        }

        // if skipping around use a caching random access list
        if (_state.direction == ResultSet.FETCH_UNKNOWN)
            return new SoftRandomAccessResultList(rop);

        // scrolling reverse... just use non-caching simple result list
        return new SimpleResultList(rop);
    }

    public Set getJoins() {
        return (_state.joins == null) ? Collections.EMPTY_SET : _state.joins;
    }

    public boolean hasJoin(String field) {
        return _state.joins != null && _state.joins.contains(field);
    }

    public JDBCFetchConfiguration addJoin(String join) {
        if (StringUtils.isEmpty(join))
            throw new UserException(_loc.get("null-join"));
        
        lock();
        try {
            if (_state.joins == null)
                _state.joins = new HashSet();
            _state.joins.add(join);
        } finally {
            unlock();
        }
        return this;
    }

    public JDBCFetchConfiguration addJoins(Collection joins) {
        if (joins == null || joins.isEmpty())
            return this;
        for (Iterator itr = joins.iterator(); itr.hasNext();)
            addJoin((String) itr.next());
        return this;
    }

    public JDBCFetchConfiguration removeJoin(String field) {
        lock();
        try {
            if (_state.joins != null)
                _state.joins.remove(field);
        } finally {
            unlock();
        }
        return this;
    }

    public JDBCFetchConfiguration removeJoins(Collection joins) {
        lock();
        try {
            if (_state.joins != null)
                _state.joins.removeAll(joins);
        } finally {
            unlock();
        }
        return this;
    }

    public JDBCFetchConfiguration clearJoins() {
        lock();
        try {
            if (_state.joins != null)
                _state.joins.clear();
        } finally {
            unlock();
        }
        return this;
    }

    public int getIsolation() {
        return _state.isolationLevel;
    }

    public JDBCFetchConfiguration setIsolation(int level) {
        if (level != -1 && level != DEFAULT
            && level != Connection.TRANSACTION_NONE
            && level != Connection.TRANSACTION_READ_UNCOMMITTED
            && level != Connection.TRANSACTION_READ_COMMITTED
            && level != Connection.TRANSACTION_REPEATABLE_READ
            && level != Connection.TRANSACTION_SERIALIZABLE)
            throw new IllegalArgumentException(
                _loc.get("bad-level", Integer.valueOf(level)).getMessage());

        if (level == DEFAULT)
            _state.isolationLevel = -1;
        else
            _state.isolationLevel = level;
        return this;
    }

    public JDBCFetchConfiguration traverseJDBC(FieldMetaData fm) {
        return (JDBCFetchConfiguration) traverse(fm);
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
}
