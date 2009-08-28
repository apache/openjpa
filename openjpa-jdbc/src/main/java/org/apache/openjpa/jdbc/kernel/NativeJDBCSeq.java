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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.jdbc.schema.Schemas;
import org.apache.openjpa.jdbc.schema.Sequence;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.util.UserException;
import serp.util.Numbers;
import serp.util.Strings;

///////////////////////////////////////////////////////////
// NOTE: Do not change property names; see SequenceMetaData
// and SequenceMapping for standard property names.
////////////////////////////////////////////////////////////

/**
 * {@link JDBCSeq} implementation that uses a database sequences
 * to generate numbers.
 *
 * @see JDBCSeq
 * @see AbstractJDBCSeq
 */
public class NativeJDBCSeq
    extends AbstractJDBCSeq
    implements Configurable {

    public static final String ACTION_DROP = "drop";
    public static final String ACTION_ADD = "add";
    public static final String ACTION_GET = "get";

    private static Localizer _loc = Localizer.forPackage(NativeJDBCSeq.class);

    private JDBCConfiguration _conf = null;
    private String _seqName = "OPENJPA_SEQUENCE";
    private int _increment = 1;
    private int _initial = 1;
    private int _allocate = 0;
    private Sequence _seq = null;
    private String _select = null;

    // for deprecated auto-configuration support
    private String _format = null;
    private String _tableName = "DUAL";
    private boolean _subTable = false;

    /**
     * The sequence name. Defaults to <code>OPENJPA_SEQUENCE</code>.
     */
    public String getSequence() {
        return _seqName;
    }

    /**
     * The sequence name. Defaults to <code>OPENJPA_SEQUENCE</code>.
     */
    public void setSequence(String seqName) {
        _seqName = seqName;
    }

    /**
     * @deprecated Use {@link #setSequence}. Retained for
     * backwards-compatibility for auto-configuration.
     */
    public void setSequenceName(String seqName) {
        setSequence(seqName);
    }

    /**
     * @see Sequence#getInitialValue
     */
    public int getInitialValue() {
        return _initial;
    }

    /**
     * @see Sequence#setInitialValue
     */
    public void setInitialValue(int initial) {
        _initial = initial;
    }

    /**
     * @see Sequence#getAllocate
     */
    public int getAllocate() {
        return _allocate;
    }

    /**
     * @see Sequence#setAllocate
     */
    public void setAllocate(int allocate) {
        _allocate = allocate;
    }

    /**
     * @see Sequence#getIncrement
     */
    public int getIncrement() {
        return _increment;
    }

    /**
     * @see Sequence#setIncrement
     */
    public void setIncrement(int increment) {
        _increment = increment;
    }

    /**
     * @deprecated Retained for backwards-compatibility for auto-configuration.
     */
    public void setTableName(String table) {
        _tableName = table;
    }

    /**
     * @deprecated Retained for backwards-compatibility for auto-configuration.
     */
    public void setFormat(String format) {
        _format = format;
        _subTable = true;
    }

    public void addSchema(ClassMapping mapping, SchemaGroup group) {
        // sequence already exists?
        if (group.isKnownSequence(_seqName))
            return;

        String schemaName = Strings.getPackageName(_seqName);
        if (schemaName.length() == 0)
            schemaName = Schemas.getNewTableSchema(_conf);

        // create table in this group
        Schema schema = group.getSchema(schemaName);
        if (schema == null)
            schema = group.addSchema(schemaName);
        schema.importSequence(_seq);
    }

    public JDBCConfiguration getConfiguration() {
        return _conf;
    }
    
    public void setConfiguration(Configuration conf) {
        _conf = (JDBCConfiguration) conf;
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
        buildSequence();

        DBDictionary dict = _conf.getDBDictionaryInstance();
        if (_format == null) {
            _format = dict.nextSequenceQuery;
            if (_format == null)
                throw new MetaDataException(_loc.get("no-seq-sql", _seqName));
        }
        if (_tableName == null)
            _tableName = "DUAL";

        String name = dict.getFullName(_seq);
        Object[] subs = (_subTable) ? new Object[]{ name, _tableName }
            : new Object[]{ name };
        _select = MessageFormat.format(_format, subs);
    }
    
    protected Object nextInternal(JDBCStore store, ClassMapping mapping)
        throws SQLException {
        Connection conn = getConnection(store);
        try {
            return Numbers.valueOf(getSequence(conn));
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * Creates the sequence object.
     */
    private void buildSequence() {
        String seqName = Strings.getClassName(_seqName);
        String schemaName = Strings.getPackageName(_seqName);
        if (schemaName.length() == 0)
            schemaName = Schemas.getNewTableSchema(_conf);

        // build the sequence in one of the designated schemas
        SchemaGroup group = new SchemaGroup();
        Schema schema = group.addSchema(schemaName);

        _seq = schema.addSequence(seqName);
        _seq.setInitialValue(_initial);
        _seq.setIncrement(_increment);
        _seq.setAllocate(_allocate);
    }

    /**
     * Creates the sequence in the DB.
     */
    public void refreshSequence()
        throws SQLException {
        Log log = _conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
        if (log.isInfoEnabled())
            log.info(_loc.get("make-native-seq"));

        // create the sequence
        SchemaTool tool = new SchemaTool(_conf);
        tool.setIgnoreErrors(true);
        tool.createSequence(_seq);
    }

    /**
     * Drops the sequence in the DB.
     */
    public void dropSequence()
        throws SQLException {
        Log log = _conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
        if (log.isInfoEnabled())
            log.info(_loc.get("drop-native-seq"));

        // drop the table
        SchemaTool tool = new SchemaTool(_conf);
        tool.setIgnoreErrors(true);
        tool.dropSequence(_seq);
    }

    /**
     * Return the next sequence value.
     */
    private long getSequence(Connection conn)
        throws SQLException {
        PreparedStatement stmnt = null;
        ResultSet rs = null;
        try {
            stmnt = conn.prepareStatement(_select);
            synchronized(this) {
                rs = stmnt.executeQuery();
            }
            if (rs.next())
                return rs.getLong(1);

            // no row !?
            throw new UserException(_loc.get("invalid-seq-sql", _select));
        } finally {
            // clean up our resources
            if (rs != null)
                try { rs.close(); } catch (SQLException se) {}
            if (stmnt != null)
                try { stmnt.close(); } catch (SQLException se) {}
        }
    }

    /////////
    // Main
    /////////

    /**
     * Usage: java org.apache.openjpa.jdbc.schema.NativeJDBCSequence [option]*
     * -action/-a &lt;add | drop | get&gt;
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
     * <li><i>add</i>: Create the sequence.</li>
     * <li><i>drop</i>: Drop the sequence.</li>
     * <li><i>get</i>: Print the next sequence value.</li>
     * </ul>
     */
    public static void main(String[] args)
        throws Exception {
        Options opts = new Options();
        args = opts.setFromCmdLine(args);
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        try {
            if (!run(conf, args, opts))
                System.out.println(_loc.get("native-seq-usage"));
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
        if (args.length != 0)
            return false;

        NativeJDBCSeq seq = new NativeJDBCSeq();
        String props = Configurations.getProperties(conf.getSequence());
        Configurations.configureInstance(seq, conf, props);

        if (ACTION_DROP.equals(action))
            seq.dropSequence();
        else if (ACTION_ADD.equals(action))
            seq.refreshSequence();
        else if (ACTION_GET.equals(action)) {
            Connection conn = conf.getDataSource2(null).getConnection();
            try {
                long cur = seq.getSequence(conn);
                System.out.println(cur);
            } finally {
                try { conn.close(); } catch (SQLException se) {}
            }
        } else
            return false;
        return true;
    }
}
