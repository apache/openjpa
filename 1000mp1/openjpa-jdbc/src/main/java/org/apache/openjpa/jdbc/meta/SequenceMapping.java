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

import org.apache.openjpa.jdbc.conf.JDBCSeqValue;
import org.apache.openjpa.jdbc.kernel.ClassTableJDBCSeq;
import org.apache.openjpa.jdbc.kernel.TableJDBCSeq;
import org.apache.openjpa.jdbc.kernel.ValueTableJDBCSeq;
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

    private File _mapFile = null;
    private String _table = null;
    private String _sequenceColumn = null;
    private String _primaryKeyColumn = null;
    private String _primaryKeyValue = null;

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

    protected PluginValue newPluginValue(String property) {
        return new JDBCSeqValue(property);
    }

    protected void addStandardProperties(StringBuffer props) {
        super.addStandardProperties(props);
        appendProperty(props, PROP_TABLE, _table);
        appendProperty(props, PROP_SEQUENCE_COL, _sequenceColumn);
        appendProperty(props, PROP_PK_COL, _primaryKeyColumn);
        appendProperty(props, PROP_PK_VALUE, _primaryKeyValue);
    }
}
