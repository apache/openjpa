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

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.StoreException;

/**
 * Abstract sequence implementation. Handles obtaining the proper
 * connection to used based on whether the sequence is transactional and
 * whether a second datasource is configured.
 *
 * @author Abe White
 */
public abstract class AbstractJDBCSeq
    implements JDBCSeq {

    protected int type = TYPE_DEFAULT;
    protected Object current = null;

    // used to track current conn so that we can close it
    private Connection _conn = null;
    private boolean _commit = false;

    /**
     * Records the sequence type.
     */
    public void setType(int type) {
        this.type = type;
    }

    public synchronized Object next(StoreContext ctx, ClassMetaData meta) {
        JDBCStore store = getStore(ctx);
        try {
            current = nextInternal(store, (ClassMapping) meta);
            return current;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, store.getDBDictionary());
        } catch (Exception e) {
            throw new StoreException(e);
        } finally {
            closeConnection();
        }
    }

    public synchronized Object current(StoreContext ctx, ClassMetaData meta) {
        JDBCStore store = getStore(ctx);
        try {
            return currentInternal(store, (ClassMapping) meta);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, store.getDBDictionary());
        } catch (Exception e) {
            throw new StoreException(e);
        } finally {
            closeConnection();
        }
    }

    public synchronized void allocate(int additional, StoreContext ctx,
        ClassMetaData meta) {
        JDBCStore store = getStore(ctx);
        try {
            allocateInternal(additional, store, (ClassMapping) meta);
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se, store.getDBDictionary());
        } catch (Exception e) {
            throw new StoreException(e);
        } finally {
            closeConnection();
        }
    }

    /**
     * No-op.
     */
    public void addSchema(ClassMapping mapping, SchemaGroup group) {
    }

    /**
     * No-op.
     */
    public void close() {
    }

    /**
     * Return the next sequence object.
     */
    protected abstract Object nextInternal(JDBCStore store,
        ClassMapping mapping)
        throws Exception;

    /**
     * Return the current sequence object. By default returns the last
     * sequence value used, or null if no sequence values have been requested
     * yet.
     */
    protected Object currentInternal(JDBCStore store, ClassMapping mapping)
        throws Exception {
        return current;
    }

    /**
     * Allocate additional sequence values. Does nothing by default.
     */
    protected void allocateInternal(int additional, JDBCStore store,
        ClassMapping mapping)
        throws Exception {
    }

    /**
     * Extract the store from the given context.
     */
    private JDBCStore getStore(StoreContext ctx) {
        return (JDBCStore) ctx.getStoreManager().getInnermostDelegate();
    }

    /**
     * Return the connection to use based on the type of sequence. This
     * connection will automatically be closed; do not close it.
     */
    protected Connection getConnection(JDBCStore store)
        throws SQLException {
        // close previous connection if user is asking for another connection
        closeConnection();

        if (type == TYPE_TRANSACTIONAL || type == TYPE_CONTIGUOUS)
            _conn = store.getConnection();
        else {
            JDBCConfiguration conf = store.getConfiguration();
            DataSource ds = conf.getDataSource2(store.getContext());
            _conn = ds.getConnection();
            if (_conn.getAutoCommit())
                _conn.setAutoCommit(false);
            _commit = true;
        }
        return _conn;
    }

    /**
     * Close the current connection.
     */
    protected void closeConnection() {
        if (_conn == null)
            return;

        try {
            if (_commit)
                _conn.commit();
        } catch (SQLException se) {
            throw SQLExceptions.getStore(se);
        } finally {
            try {
                _conn.close();
            } catch (SQLException se) {
            }
            _conn = null;
            _commit = false;
        }
    }
}
