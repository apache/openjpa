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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.jdbc.schema.Schemas;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.InvalidStateException;
import serp.util.Numbers;
import serp.util.Strings;

////////////////////////////////////////////////////////////
// NOTE: Do not change property names; see SequenceMetaData
// and SequenceMapping for standard property names.
////////////////////////////////////////////////////////////

/**
 * {@link JDBCSeq} implementation that uses a database table
 * for sequence number generation. This base implementation uses a single
 * row for a global sequence number.
 *
 * @author Abe White
 */
public class TableJDBCSeq
    extends AbstractJDBCSeq
    implements Configurable {

    public static final String ACTION_DROP = "drop";
    public static final String ACTION_ADD = "add";
    public static final String ACTION_GET = "get";
    public static final String ACTION_SET = "set";

    private static final Localizer _loc = Localizer.forPackage
        (TableJDBCSeq.class);

    private transient JDBCConfiguration _conf = null;
    private transient Log _log = null;
    private int _alloc = 50;
    private int _intValue = 1;
    private final HashMap _stat = new HashMap();

    private String _table = "OPENJPA_SEQUENCE_TABLE";
    private String _seqColumnName = "SEQUENCE_VALUE";
    private String _pkColumnName = "ID";

    private Column _seqColumn = null;
    private Column _pkColumn = null;
    private int _schemasIdx = 0;    

    /**
     * The sequence table name. Defaults to <code>OPENJPA_SEQUENCE_TABLE</code>.
     * By default, the table will be placed in the first schema listed in your
     * <code>openjpa.jdbc.Schemas</code> property, or in the default schema if
     * the property is not given. If you specify a table name in the form
     * <code>&lt;schema&gt;.&lt;table&gt;</code>, then the given schema
     * will be used.
     */
    public String getTable() {
        return _table;
    }

    /**
     * The sequence table name. Defaults to <code>OPENJPA_SEQUENCE_TABLE</code>.
     * By default, the table will be placed in the first schema listed in your
     * <code>openjpa.jdbc.Schemas</code> property, or in the default schema if
     * the property is not given. If you specify a table name in the form
     * <code>&lt;schema&gt;.&lt;table&gt;</code>, then the given schema
     * will be used.
     */
    public void setTable(String name) {
        _table = name;
    }

    /**
     * @deprecated Use {@link #setTable}. Retained for
     * backwards-compatibility	with auto-configuration.
     */
    public void setTableName(String name) {
        setTable(name);
    }

    /**
     * The name of the column that holds the sequence value. Defaults
     * to <code>SEQUENCE_VALUE</code>.
     */
    public String getSequenceColumn() {
        return _seqColumnName;
    }

    /**
     * The name of the column that holds the sequence value. Defaults
     * to <code>SEQUENCE_VALUE</code>.
     */
    public void setSequenceColumn(String sequenceColumn) {
        _seqColumnName = sequenceColumn;
    }

    /**
     * The name of the table's primary key column. Defaults to
     * <code>ID</code>.
     */
    public String getPrimaryKeyColumn() {
        return _pkColumnName;
    }

    /**
     * The name of the table's primary key column. Defaults to
     * <code>ID</code>.
     */
    public void setPrimaryKeyColumn(String primaryKeyColumn) {
        _pkColumnName = primaryKeyColumn;
    }

    /**
     * Return the number of sequences to allocate for each update of the
     * sequence table. Sequence numbers will be grabbed in blocks of this
     * value to reduce the number of transactions that must be performed on
     * the sequence table.
     */
    public int getAllocate() {
        return _alloc;
    }

    /**
     * Return the number of sequences to allocate for each update of the
     * sequence table. Sequence numbers will be grabbed in blocks of this
     * value to reduce the number of transactions that must be performed on
     * the sequence table.
     */
    public void setAllocate(int alloc) {
        _alloc = alloc;
    }
    
    /**
     * Return the number as the initial number for the 
     * GeneratedValue.TABLE strategy to start with. 
     * @return an initial number
     */
    public int getInitialValue() {        
        return _intValue;
    }

    /**
     * Set the initial number in the table for the GeneratedValue.TABLE
     * strategy to use as initial number. 
     * @param intValue. The initial number
     */
    public void setInitialValue(int intValue) {
        _intValue = intValue;
    }

    /**
     * @deprecated Use {@link #setAllocate}. Retained for backwards
     * compatibility of auto-configuration.
     */
    public void setIncrement(int inc) {
        setAllocate(inc);
    }

    public JDBCConfiguration getConfiguration() {
        return _conf;
    }

    public void setConfiguration(Configuration conf) {
        _conf = (JDBCConfiguration) conf;
        _log = _conf.getLog(JDBCConfiguration.LOG_RUNTIME);
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
        buildTable();
    }
    

    public void addSchema(ClassMapping mapping, SchemaGroup group) {
        // Since the table is created by openjpa internally
        // we can create the table for each schema within the PU
        // in here.
        
        Schema[] schemas = group.getSchemas();
        for (int i = 0; i < schemas.length; i++) {
            String schemaName = Strings.getPackageName(_table);
            if (schemaName.length() == 0)
                schemaName = Schemas.getNewTableSchema(_conf);
            if (schemaName == null)
                schemaName = schemas[i].getName();

            // create table in this group
            Schema schema = group.getSchema(schemaName);
            if (schema == null)
                schema = group.addSchema(schemaName);
            
            schema.importTable(_pkColumn.getTable());
            // we need to reset the table name in the column with the
            // fully qualified name for matching the table name from the
            // Column.
            _pkColumn.resetTableName(schemaName + "."
                    + _pkColumn.getTableName());
            // some databases require to create an index for the sequence table
            _conf.getDBDictionaryInstance().createIndexIfNecessary(schema,
                    _table, _pkColumn);
         
        }
    }

    protected Object nextInternal(JDBCStore store, ClassMapping mapping)
        throws Exception {
        // if needed, grab the next handful of ids
        Status stat = getStatus(mapping);
        if (stat == null)
            throw new InvalidStateException(_loc.get("bad-seq-type",
                getClass(), mapping));

        while (true) {
            synchronized (stat) {
                // make sure seq is at least 1, since autoassigned ids of 0 can
                // conflict with uninitialized values
                stat.seq = Math.max(stat.seq, 1);
                if (stat.seq < stat.max)
                    return Numbers.valueOf(stat.seq++);
            }
            allocateSequence(store, mapping, stat, _alloc, true);
        }
    }

    protected Object currentInternal(JDBCStore store, ClassMapping mapping)
        throws Exception {
        if (current == null) {
            Connection conn = getConnection(store);
            try {
                long cur = getSequence(mapping, conn);
                if (cur != -1)
                    current = Numbers.valueOf(cur);
            } finally {
                closeConnection(conn);
            }
        }
        return super.currentInternal(store, mapping);
    }

    protected void allocateInternal(int count, JDBCStore store,
        ClassMapping mapping)
        throws SQLException {
        Status stat = getStatus(mapping);
        if (stat == null)
            return;

        while (true) {
            int available;
            synchronized (stat) {
                available = (int) (stat.max - stat.seq);
                if (available >= count)
                    return;
            }
            allocateSequence(store, mapping, stat, count - available, false);
        }
    }

    /**
     * Return the appropriate status object for the given class, or null
     * if cannot handle the given class. The mapping may be null.
     */
    protected Status getStatus(ClassMapping mapping) {  
        Status status = (Status)_stat.get(mapping);        
        if (status == null){ 
            status = new Status();
            _stat.put(mapping, status);
        }
        return status;
            
    }

    /**
     * Add the primary key column to the given table and return it.
     */
    protected Column addPrimaryKeyColumn(Table table) {
        DBDictionary dict = _conf.getDBDictionaryInstance();
        Column pkColumn = table.addColumn(dict.getValidColumnName
            (getPrimaryKeyColumn(), table));
        pkColumn.setType(dict.getPreferredType(Types.TINYINT));
        pkColumn.setJavaType(JavaTypes.INT);
        return pkColumn;
    }

    /**
     * Return the primary key value for the sequence table for the given class.
     */
    protected Object getPrimaryKey(ClassMapping mapping) {
        return Numbers.valueOf(0);
    }

    /**
     * Creates the object-level representation of the sequence table.
     */
    private void buildTable() {
        String tableName = Strings.getClassName(_table);
        String schemaName = Strings.getPackageName(_table);
        if (schemaName.length() == 0)
            schemaName = Schemas.getNewTableSchema(_conf);

        SchemaGroup group = new SchemaGroup();
        Schema schema = group.addSchema(schemaName);

        Table table = schema.addTable(tableName);
        _pkColumn = addPrimaryKeyColumn(table);
        PrimaryKey pk = table.addPrimaryKey();
        pk.addColumn(_pkColumn);

        DBDictionary dict = _conf.getDBDictionaryInstance();
        _seqColumn = table.addColumn(dict.getValidColumnName
            (_seqColumnName, table));
        _seqColumn.setType(dict.getPreferredType(Types.BIGINT));
        _seqColumn.setJavaType(JavaTypes.LONG);
    }

    /**
     * Updates the max available sequence value.
     */
    private void allocateSequence(JDBCStore store, ClassMapping mapping,
        Status stat, int alloc, boolean updateStatSeq) 
        throws SQLException {
        Connection conn = getConnection(store);
        try { 
            if (setSequence(mapping, stat, alloc, updateStatSeq, conn))
                return;
        } catch (SQLException se) {
            throw SQLExceptions.getStore(_loc.get("bad-seq-up", _table),
                se, _conf.getDBDictionaryInstance());
        } finally {
            closeConnection(conn);
        }
        
        try {
            // possible that we might get errors when inserting if
            // another thread/process is inserting same pk at same time
            SQLException err = null; 
            // ### why does this not call getConnection() / closeConnection()?
            conn = _conf.getDataSource2(store.getContext()).getConnection();
            try {
                insertSequence(mapping, conn);
            } catch (SQLException se) {
                err = se;
            } finally {
                try { conn.close(); } catch (SQLException se) {}
            }

            // now we should be able to update...
            conn = getConnection(store);
            try {
                if (!setSequence(mapping, stat, alloc, updateStatSeq, conn))
                    throw (err != null) ? err : new SQLException(_loc.get
                        ("no-seq-row", mapping, _table).getMessage());
            } finally {
                closeConnection(conn);
            }
        } catch (SQLException se2) {
            throw SQLExceptions.getStore(_loc.get("bad-seq-up", _table),
                se2, _conf.getDBDictionaryInstance());
        } 
    }

    /**
     * Inserts the initial sequence information into the database, if any.
     */
    private void insertSequence(ClassMapping mapping, Connection conn)
        throws SQLException {
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("insert-seq"));

        Object pk = getPrimaryKey(mapping);
        if (pk == null)
            throw new InvalidStateException(_loc.get("bad-seq-type",
                getClass(), mapping));

        DBDictionary dict = _conf.getDBDictionaryInstance();
        String tableName = resolveTableName(mapping, _pkColumn.getTable());
        SQLBuffer insert = new SQLBuffer(dict).append("INSERT INTO ").
            append(tableName).append(" (").
            append(_pkColumn).append(", ").append(_seqColumn).
            append(") VALUES (").
            appendValue(pk, _pkColumn).append(", ").
            appendValue(_intValue, _seqColumn).append(")");
        
        boolean wasAuto = conn.getAutoCommit();
        if (!wasAuto && !suspendInJTA())
            conn.setAutoCommit(true);

        PreparedStatement stmnt = null;
        try {
            stmnt = insert.prepareStatement(conn);
            stmnt.executeUpdate();
        } finally {
            if (stmnt != null)
                try { stmnt.close(); } catch (SQLException se) {}
            if (!wasAuto && !suspendInJTA())
                conn.setAutoCommit(false);
        }
    }

    /**
     * Return the current sequence value, or -1 if unattainable.
     */
    protected long getSequence(ClassMapping mapping, Connection conn)
        throws SQLException {
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("get-seq"));

        Object pk = getPrimaryKey(mapping);
        if (pk == null)
            return -1;

        DBDictionary dict = _conf.getDBDictionaryInstance();
        SQLBuffer sel = new SQLBuffer(dict).append(_seqColumn);
        SQLBuffer where = new SQLBuffer(dict).append(_pkColumn).append(" = ").
            appendValue(pk, _pkColumn);
        String tableName = resolveTableName(mapping, _seqColumn.getTable());
        SQLBuffer tables = new SQLBuffer(dict).append(tableName);

        SQLBuffer select = dict.toSelect(sel, null, tables, where, null, null,
                null, false, dict.supportsSelectForUpdate, 0, Long.MAX_VALUE,
                false, true);

        PreparedStatement stmnt = select.prepareStatement(conn);
        ResultSet rs = null;
        try {
            rs = stmnt.executeQuery();
            if (!rs.next())
                return -1;
            return dict.getLong(rs, 1);
        } finally {
            if (rs != null)
                try { rs.close(); } catch (SQLException se) {}
            try { stmnt.close(); } catch (SQLException se) {}
        }
    }

    /**
     * Grabs the next handful of sequence numbers.
     *
     * @return true if the sequence was updated, false if no sequence
     * row existed for this mapping
     */
    protected boolean setSequence(ClassMapping mapping, Status stat, int inc,
        boolean updateStatSeq, Connection conn)
        throws SQLException {
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("update-seq"));

        Object pk = getPrimaryKey(mapping);
        if (pk == null)
            throw new InvalidStateException(_loc.get("bad-seq-type",
                getClass(), mapping));

        DBDictionary dict = _conf.getDBDictionaryInstance();        
        SQLBuffer where = new SQLBuffer(dict).append(_pkColumn).append(" = ").
            appendValue(pk, _pkColumn);

        // loop until we have a successful atomic select/update sequence
        long cur = 0;
        PreparedStatement stmnt;
        ResultSet rs;
        SQLBuffer upd;
        for (int updates = 0; updates == 0;) {
            stmnt = null;
            rs = null;
            try {
                cur = getSequence(mapping, conn);
                if (cur == -1)
                    return false;

                // update the value
                upd = new SQLBuffer(dict);
                String tableName = resolveTableName(mapping, _seqColumn.getTable());
                upd.append("UPDATE ").append(tableName).
                    append(" SET ").append(_seqColumn).append(" = ").
                    appendValue(Numbers.valueOf(cur + inc), _seqColumn).
                    append(" WHERE ").append(where).append(" AND ").
                    append(_seqColumn).append(" = ").
                    appendValue(Numbers.valueOf(cur), _seqColumn);

                stmnt = upd.prepareStatement(conn);
                updates = stmnt.executeUpdate();
            } finally {
                if (rs != null) 
                    try { rs.close(); } catch (SQLException se) {}
                if (stmnt != null)
                    try { stmnt.close(); } catch (SQLException se) {}
            }
        }

        // setup new sequence range        
        synchronized (stat) {
            if (updateStatSeq && stat.seq < cur)
                stat.seq = cur;
            if (stat.max < cur + inc)
                stat.max = cur + inc;
        }
        return true;
    }
    /**
     * Resolve a fully qualified table name
     * 
     * @param class
     *            mapping to get the schema name
     */
    public String resolveTableName(ClassMapping mapping, Table table) {
        String sName = mapping.getTable().getSchemaName();
        String tableName;
        if (sName == null)
            tableName = table.getFullName();
        else if (table.getSchemaName() != null)
            tableName = table.getFullName();
        else
            tableName = sName + "." + table.getName();
        return tableName;
    }

    /**
     * Creates the sequence table in the DB.
     */
    public void refreshTable()
        throws SQLException {
        if (_log.isInfoEnabled())
            _log.info(_loc.get("make-seq-table"));

        // create the table
        SchemaTool tool = new SchemaTool(_conf);
        tool.setIgnoreErrors(true);
        tool.createTable(_pkColumn.getTable());
    }

    /**
     * Drops the sequence table in the DB.
     */
    public void dropTable()
        throws SQLException {
        if (_log.isInfoEnabled())
            _log.info(_loc.get("drop-seq-table"));

        // drop the table
        SchemaTool tool = new SchemaTool(_conf);
        tool.setIgnoreErrors(true);
        tool.dropTable(_pkColumn.getTable());
    }

    /////////
    // Main
    /////////

    /**
     * Usage: java org.apache.openjpa.jdbc.schema.TableJDBCSequence [option]*
     * -action/-a &lt;add | drop | get | set&gt; [value]
     *  Where the following options are recognized.
     * <ul>
     * <li><i>-properties/-p &lt;properties file or resource&gt;</i>: The
     * path or resource name of a OpenJPA properties file containing
     * information such as the license key	and connection data as
     * outlined in {@link JDBCConfiguration}. Optional.</li>
     * <li><i>-&lt;property name&gt; &lt;property value&gt;</i>: All bean
     * properties of the OpenJPA {@link JDBCConfiguration} can be set by
     * using their	names and supplying a value. For example:
     * <code>-licenseKey adslfja83r3lkadf</code></li>
     * </ul>
     *  The various actions are as follows.
     * <ul>
     * <li><i>add</i>: Create the sequence table.</li>
     * <li><i>drop</i>: Drop the sequence table.</li>
     * <li><i>get</i>: Print the current sequence value.</li>
     * <li><i>set</i>: Set the sequence value.</li>
     * </ul>
     */
    public static void main(String[] args)
        throws Exception {
        Options opts = new Options();
        args = opts.setFromCmdLine(args);
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        try {
            if (!run(conf, args, opts))
                System.out.println(_loc.get("seq-usage"));
        } finally {
            conf.close();
        }
    }

    /**
     * Run the tool. Returns false if invalid options were given.
     */
    public static boolean run(JDBCConfiguration conf, String[] args,
        Options opts)
        throws Exception {
        if (opts.containsKey("help") || opts.containsKey("-help"))
            return false;

        String action = opts.removeProperty("action", "a", null);
        Configurations.populateConfiguration(conf, opts);
        return run(conf, args, action);
    }

    /**
     * Run the tool. Return false if an invalid option was given.
     */
    public static boolean run(JDBCConfiguration conf, String[] args,
        String action)
        throws Exception {
        if (args.length > 1 || (args.length != 0
            && !ACTION_SET.equals(action)))
            return false;

        TableJDBCSeq seq = new TableJDBCSeq();
        String props = Configurations.getProperties(conf.getSequence());
        Configurations.configureInstance(seq, conf, props);

        if (ACTION_DROP.equals(action))
            seq.dropTable();
        else if (ACTION_ADD.equals(action))
            seq.refreshTable();
        else if (ACTION_GET.equals(action) || ACTION_SET.equals(action)) {
            Connection conn = conf.getDataSource2(null).getConnection();
            try {
                long cur = seq.getSequence(null, conn);
                if (ACTION_GET.equals(action))
                    System.out.println(cur);
                else {
                    long set;
                    if (args.length > 0)
                        set = Long.parseLong(args[0]);
                    else
                        set = cur + seq.getAllocate();
                    if (set < cur)
                        set = cur;
                    else {
                        Status stat = seq.getStatus(null);
                        seq.setSequence(null, stat, (int) (set - cur), true,
                            conn);
                        set = stat.seq;
                    }
                    System.err.println(set);
                }
            }
            catch (NumberFormatException nfe) {
                return false;
            } finally {
                try { conn.close(); } catch (SQLException se) {}
            }
        } else
            return false;
        return true;
    }

    /**
     * Helper struct to hold status information.
     */
    protected static class Status
        implements Serializable {

        public long seq = 1L;
        public long max = 0L;
    }
}
