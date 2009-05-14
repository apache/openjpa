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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCSeqValue;
import org.apache.openjpa.jdbc.kernel.ClassTableJDBCSeq;
import org.apache.openjpa.jdbc.kernel.TableJDBCSeq;
import org.apache.openjpa.jdbc.kernel.ValueTableJDBCSeq;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.meta.SequenceMetaData;

/**
 * Specialization of sequence metadata for ORM.
 *
 * @author Abe White
 */
public class SequenceMapping
    extends SequenceMetaData {

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

    private File _mapFile = null;
    private String _table = null;
    private String _sequenceColumn = null;
    private String _primaryKeyColumn = null;
    private String _primaryKeyValue = null;
    private String[] _uniqueColumns   = null;
    
    public SequenceMapping(String name, MappingRepository repos) {
        super(name, repos);
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
     */
    public String getTable() {
        return _table;
    }

    /**
     * Name of sequence table, if any.
     */
    public void setTable(String table) {
        _table = table;
    }

    /**
     * Name of sequence column, if any.
     */
    public String getSequenceColumn() {
        return _sequenceColumn;
    }

    /**
     * Name of sequence column, if any.
     */
    public void setSequenceColumn(String sequenceColumn) {
        _sequenceColumn = sequenceColumn;
    }

    /**
     * Name of primary key column, if any.
     */
    public String getPrimaryKeyColumn() {
        return _primaryKeyColumn;
    }

    /**
     * Name of primary key column, if any.
     */
    public void setPrimaryKeyColumn(String primaryKeyColumn) {
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

    public void setUniqueColumns(String[] cols) {
    	_uniqueColumns = cols;
    }
    
    public String[] getUniqueColumns() {
    	return _uniqueColumns;
    }
    
    protected PluginValue newPluginValue(String property) {
        return new JDBCSeqValue(property);
    }

    protected void addStandardProperties(StringBuffer props) {
        super.addStandardProperties(props);
        // Quotes are conditionally added to the following because the props
        // are eventually passed to the Configurations.parseProperties()
        // method, which strips off quotes. This is a problem when these
        // properties are intentionally delimited with quotes. So, an extra
        // set preserves the intended ones. While this is an ugly solution,
        // it's less ugly than other ones.
        String table = _table;
        if (table != null && table.startsWith("\"")
                && table.endsWith("\"")) {
            table = "\"" + table + "\"";
        }
        String sequenceColumn = _sequenceColumn;
        if (sequenceColumn != null && sequenceColumn.startsWith("\"")
                && sequenceColumn.endsWith("\"")) {
            sequenceColumn = "\"" + sequenceColumn + "\"";
        }
        String primaryKeyColumn = _primaryKeyColumn;
        if (primaryKeyColumn !=null && primaryKeyColumn.startsWith("\"")
                && primaryKeyColumn.endsWith("\"")) {
            primaryKeyColumn = "\"" + primaryKeyColumn + "\"";
        }
        String primaryKeyValue = _primaryKeyValue;
        if (primaryKeyValue != null && primaryKeyValue.startsWith("\"")
                && primaryKeyValue.endsWith("\"")) {
            primaryKeyValue = "\"" + primaryKeyValue + "\"";
        }
        
        appendProperty(props, PROP_TABLE, table);
        appendProperty(props, PROP_SEQUENCE_COL, sequenceColumn);
        appendProperty(props, PROP_PK_COL, primaryKeyColumn);
        appendProperty(props, PROP_PK_VALUE, primaryKeyValue);
        // Array of unique column names are passed to configuration
        // as a single string "x|y|z". The configurable (TableJDBCSeq) must
        // parse it back.
        if (_uniqueColumns != null && _uniqueColumns.length > 0)
        	appendProperty(props, PROP_UNIQUE, 
        			StringUtils.join(_uniqueColumns,'|'));
    }
}
