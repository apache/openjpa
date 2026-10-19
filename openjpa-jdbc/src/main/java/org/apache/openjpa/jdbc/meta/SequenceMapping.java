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
package org.apache.openjpa.jdbc.meta;

import java.io.File;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCSeqValue;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.DBIdentifier.DBIdentifierType;
import org.apache.openjpa.jdbc.kernel.ClassTableJDBCSeq;
import org.apache.openjpa.jdbc.kernel.NativeJDBCSeq;
import org.apache.openjpa.jdbc.kernel.TableJDBCSeq;
import org.apache.openjpa.jdbc.kernel.ValueTableJDBCSeq;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.kernel.Seq;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.lib.identifier.IdentifierUtil;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.util.MetaDataException;

/**
 * Specialization of sequence metadata for ORM.
 *
 * @author Abe White
 */
public class SequenceMapping extends SequenceMetaData {
    private static final long serialVersionUID = 1L;

    /**
     * {@link ValueTableJDBCSeq} alias.
     */
    public static final String IMPL_VALUE_TABLE = "value-table";

    /**
     * {@link TableJDBCSeq} alias.
     */
    public static final String IMPL_TABLE = "table";

    /**
     * {@link ClassTableJDBCSeq} alias.
     */
    public static final String IMPL_CLASS_TABLE = "class-table";

    // plugin property names for standard props
    private static final String PROP_TABLE = "Table";
    private static final String PROP_SEQUENCE_COL = "SequenceColumn";
    private static final String PROP_PK_COL = "PrimaryKeyColumn";
    private static final String PROP_PK_VALUE = "PrimaryKeyValue";
    private static final String PROP_UNIQUE = "UniqueColumns";
    private static final String PROP_UNIQUE_CONSTRAINT = "UniqueConstraintName";

    private static final Localizer _loc = Localizer.forPackage(SequenceMapping.class);

    private File _mapFile = null;
    private DBIdentifier _table = DBIdentifier.NULL;
    private DBIdentifier _sequenceColumn = DBIdentifier.NULL;
    private DBIdentifier _primaryKeyColumn = DBIdentifier.NULL;
    private String _primaryKeyValue = null;
    private DBIdentifier[] _uniqueColumns   = null;
    private DBIdentifier _uniqueConstraintName = DBIdentifier.NULL;

    /**
     * @deprecated
     * @param name
     * @param repos
     */
    @Deprecated
    public SequenceMapping(String name, MappingRepository repos) {
        super(name, repos);
    }

    /**
     * Sequence names are a kernel object so DBIdentifiers must be converted to
     * strings
     * @param name
     * @param repos
     */
    public SequenceMapping(DBIdentifier name, MappingRepository repos) {
        super(DBIdentifier.isNull(name) ? null : name.getName(), repos);
    }

    /**
     * Allow sequence to have a mapping file separate from its metadata
     * source file.
     */
    public File getMappingFile() {
        return _mapFile;
    }

    /**
     * Allow sequence to have a mapping file separate from its metadata
     * source file.
     */
    public void setMappingFile(File file) {
        _mapFile = file;
    }

    /**
     * Name of sequence table, if any.
     * @deprecated
     */
    @Deprecated
    public String getTable() {
        return getTableIdentifier().getName();
    }

    public DBIdentifier getTableIdentifier() {
        return _table == null ? DBIdentifier.NULL : _table ;
    }

    /**
     * Name of sequence table, if any.
     * @deprecated
     */
    @Deprecated
    public void setTable(String table) {
        setTableIdentifier(DBIdentifier.newTable(table));
    }

    public void setTableIdentifier(DBIdentifier table) {
        _table = table;
    }

    /**
     * Name of sequence column, if any.
     * @deprecated
     */
    @Deprecated
    public String getSequenceColumn() {
        return getSequenceColumnIdentifier().getName();
    }

    public DBIdentifier getSequenceColumnIdentifier() {
        return _sequenceColumn == null ? DBIdentifier.NULL : _sequenceColumn;
    }

    /**
     * Name of sequence column, if any.
     * @deprecated
     */
    @Deprecated
    public void setSequenceColumn(String sequenceColumn) {
        setSequenceColumnIdentifier(DBIdentifier.newColumn(sequenceColumn));
    }

    public void setSequenceColumnIdentifier(DBIdentifier sequenceColumn) {
        _sequenceColumn = sequenceColumn;
    }

    /**
     * Name of primary key column, if any.
     * @deprecated
     */
    @Deprecated
    public String getPrimaryKeyColumn() {
        return getPrimaryKeyColumnIdentifier().getName();
    }

    public DBIdentifier getPrimaryKeyColumnIdentifier() {
        return _primaryKeyColumn == null ? DBIdentifier.NULL : _primaryKeyColumn;
    }

    /**
     * Name of primary key column, if any.
     * @deprecated
     */
    @Deprecated
    public void setPrimaryKeyColumn(String primaryKeyColumn) {
        setPrimaryKeyColumnIdentifier(DBIdentifier.newColumn(primaryKeyColumn));
    }

    public void setPrimaryKeyColumnIdentifier(DBIdentifier primaryKeyColumn) {
        _primaryKeyColumn = primaryKeyColumn;
    }

    /**
     * Primary key value, if not auto-determined.
     */
    public String getPrimaryKeyValue() {
        return _primaryKeyValue;
    }

    /**
     * Primary key value, if not auto-determined.
     */
    public void setPrimaryKeyValue(String primaryKeyValue) {
        _primaryKeyValue = primaryKeyValue;
    }

    /**
     * @deprecated
     * @param cols
     */
    @Deprecated
    public void setUniqueColumns(String[] cols) {
        setUniqueColumnsIdentifier(DBIdentifier.toArray(cols, DBIdentifierType.COLUMN));
    }

    public void setUniqueColumnsIdentifier(DBIdentifier[] cols) {
        _uniqueColumns = cols;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String[] getUniqueColumns() {
        return DBIdentifier.toStringArray(getUniqueColumnsIdentifier());
    }

    public DBIdentifier[] getUniqueColumnsIdentifier() {
    	return _uniqueColumns;
    }


    @Override
    protected PluginValue newPluginValue(String property) {
        return new JDBCSeqValue(property);
    }

    /**
     * A generator that asks for a native sequence cannot be configured on a
     * database that has no sequence objects: {@link NativeJDBCSeq} needs
     * {@link DBDictionary#nextSequenceQuery}, and refuses to configure
     * without it. Jakarta Persistence leaves the realisation of a generator
     * to the provider, so rather than fail, such a generator is backed by a
     * {@link ValueTableJDBCSeq} - a counter row keyed by the sequence name,
     * which keeps the identifiers unique and honours
     * <code>initialValue</code> and <code>allocationSize</code>.
     *<p>
     * The substitution happens here, where the sequence is resolved, so the
     * runtime and the mapping tool agree on which sequence backs the
     * generator, and it produces a separate metadata instance rather than
     * mutating this one, which is shared.
     *
     * @see org.apache.openjpa.conf.Compatibility#getEmulateNativeSequences()
     */
    @Override
    protected Seq instantiate(ClassLoader envLoader) {
        SequenceMapping fallback = tableSequenceFallback();
        if (fallback == null) {
            return super.instantiate(envLoader);
        }
        JDBCConfiguration conf = (JDBCConfiguration) getRepository().getConfiguration();
        Log log = conf.getLog(OpenJPAConfiguration.LOG_METADATA);
        if (log.isWarnEnabled()) {
            log.warn(_loc.get("seq-table-fallback", getName(),
                conf.getDBDictionaryInstance().platform,
                fallback.getPrimaryKeyValue()));
        }
        return fallback.instantiate(envLoader);
    }

    /**
     * Return a copy of this metadata backed by a table sequence, or null if
     * this sequence is to be instantiated as declared.
     */
    private SequenceMapping tableSequenceFallback() {
        // the system sequence is the configured openjpa.Sequence, not a plugin
        if (NAME_SYSTEM.equals(getName())) {
            return null;
        }
        OpenJPAConfiguration conf = getRepository().getConfiguration();
        if (!(conf instanceof JDBCConfiguration)
            || !conf.getCompatibilityInstance().getEmulateNativeSequences()) {
            return null;
        }

        // only a plain request for a native sequence is substituted; a plugin
        // string that configures NativeJDBCSeq itself is left alone
        PluginValue plugin = newPluginValue("sequence-plugin");
        plugin.setString(getSequencePlugin());
        if (!NativeJDBCSeq.class.getName().equals(plugin.getClassName())
            || !StringUtil.isEmpty(plugin.getProperties())) {
            return null;
        }

        JDBCConfiguration jconf = (JDBCConfiguration) conf;
        DBDictionary dict = jconf.getDBDictionaryInstance();
        if (dict.nextSequenceQuery != null) {
            return null;
        }
        if (!dict.isConnected()) {
            // A dictionary may only learn from the server version whether the
            // database has sequences - MariaDB has them from 10.3 on - and a
            // dictionary identified by the connection URL alone has not seen a
            // connection yet. Ask for the connection factory, which installs
            // the dictionary's connected configuration, so that the decision
            // does not depend on whether anything else has touched the
            // database first. This is reached only for a dictionary that does
            // not already declare a NextSequenceQuery, so the dictionaries
            // that set one in their constructor - Oracle, DB2, PostgreSQL, H2,
            // HSQL, Informix, Firebird, Ingres - are resolved without a
            // connection, as they were before OPENJPA-3014.
            try {
                jconf.getConnectionFactory();
            } catch (RuntimeException | LinkageError e) {
                // Which backing store a generator gets is cached on the
                // metadata for the life of the factory, and guessing it here
                // cannot help: the table fallback needs a connection of its
                // own to advance its counter, so nothing works without one
                // either way. Guessing can harm, though - a server that does
                // have sequences would hand out identifiers from a counter
                // other nodes of the same deployment do not use. Fail with
                // the cause instead of substituting silently; a missing
                // driver class arriving as a LinkageError is wrapped too.
                throw new MetaDataException(_loc.get("seq-native-no-connection",
                    getName(), dict.platform)).setCause(e);
            }
            if (dict.nextSequenceQuery != null) {
                return null;
            }
        }

        SequenceMapping sub = new SequenceMapping(DBIdentifier.newSequence(getName()),
            (MappingRepository) getRepository());
        sub.setSequencePlugin(IMPL_VALUE_TABLE);
        // the sequence name identifies the counter, so two generators naming
        // the same sequence share it, as they would natively; the schema and
        // catalog of the sequence that does not exist qualify that name rather
        // than the table, which is an internal one in the default schema
        sub.setPrimaryKeyValue(emulatedSequenceName());
        sub.setInitialValue(getInitialValue());
        sub.setAllocate(getAllocate());
        // NOT the increment: TableJDBCSeq.setIncrement is a deprecated alias
        // for setAllocate and would overwrite the allocation size
        sub.setType(getType());
        sub.setSequenceFactory(getSequenceFactory());
        sub.setTableIdentifier(getTableIdentifier());
        sub.setSequenceColumnIdentifier(getSequenceColumnIdentifier());
        sub.setPrimaryKeyColumnIdentifier(getPrimaryKeyColumnIdentifier());
        sub.setUniqueColumnsIdentifier(getUniqueColumnsIdentifier());
        sub.setUniqueConstraintIdentifier(getUniqueConstraintIdentifier());
        sub.setSource(getSourceFile(), getSourceScope(), getSourceType());
        return sub;
    }

    /**
     * The name identifying the emulated counter: the sequence name the
     * generator asked for, qualified by the catalog and schema of this
     * sequence, or the generator name if it named no sequence at all.
     *<p>
     * The schema is the one a native sequence object would have been created
     * in, which is the generator's own <code>schema</code> if it declared one
     * and otherwise the persistence unit default schema that
     * {@link org.apache.openjpa.meta.SequenceMetaData#setSchema} substitutes
     * for an empty one. The emulated counter is therefore scoped exactly as
     * the sequence it stands in for: changing the schema moves the counter,
     * just as it would move the sequence object on a database that has them.
     */
    private String emulatedSequenceName() {
        String seq = getSequence();
        if (StringUtil.isEmpty(seq)) {
            return getName();
        }
        StringBuilder name = new StringBuilder();
        if (!StringUtil.isEmpty(getCatalog())) {
            name.append(getCatalog()).append('.');
        }
        if (!StringUtil.isEmpty(getSchema())) {
            name.append(getSchema()).append('.');
        }
        return name.append(seq).toString();
    }

    @Override
    protected void addStandardProperties(StringBuilder props) {
        super.addStandardProperties(props);
        // Quotes are conditionally added to the following because the props
        // are eventually passed to the Configurations.parseProperties()
        // method, which strips off quotes. This is a problem when these
        // properties are intentionally delimited with quotes. So, an extra
        // set preserves the intended ones. While this is an ugly solution,
        // it's less ugly than other ones.

        appendProperty(props, PROP_TABLE, addQuotes(_table.getName()));
        appendProperty(props, PROP_SEQUENCE_COL, addQuotes(_sequenceColumn.getName()));
        appendProperty(props, PROP_PK_COL, addQuotes(_primaryKeyColumn.getName()));
        appendProperty(props, PROP_PK_VALUE, addQuotes(_primaryKeyValue));
        // Array of unique column names are passed to configuration
        // as a single string "x|y|z". The configurable (TableJDBCSeq) must
        // parse it back.
        if (!DBIdentifier.isNull(_uniqueConstraintName) && _uniqueConstraintName.getName().length() > 0) {
            appendProperty(props, PROP_UNIQUE_CONSTRAINT, addQuotes(_uniqueConstraintName.getName()));
        }

        if (_uniqueColumns != null && _uniqueColumns.length > 0) {
            appendProperty(props, PROP_UNIQUE, StringUtil.join(_uniqueColumns, "|"));
        }
    }

    private String addQuotes(String name) {
        if (name != null && name.contains(IdentifierUtil.DOUBLE_QUOTE)) {
            return IdentifierUtil.DOUBLE_QUOTE + name + IdentifierUtil.DOUBLE_QUOTE;
        }
        return name;
    }

    /**
     * @deprecated
     * @param name
     */
    @Deprecated
    public void setUniqueConstraintName(String name) {
        _uniqueConstraintName = DBIdentifier.newConstraint(name);
    }

    public void setUniqueConstraintIdentifier(DBIdentifier name) {
        _uniqueConstraintName = name;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getUniqueConstraintName() {
        return getUniqueConstraintIdentifier().getName();
    }

    public DBIdentifier getUniqueConstraintIdentifier() {
        return _uniqueConstraintName == null ? DBIdentifier.NULL : _uniqueConstraintName;

    }
}
