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
package org.apache.openjpa.jdbc.schema;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.meta.JavaSQLTypes;
import org.apache.openjpa.jdbc.meta.VersionStrategy;
import org.apache.openjpa.meta.JavaTypes;
import serp.util.Numbers;

/**
 * Represents a database column. Closely aligned with the column
 * information available from {@link DatabaseMetaData}.
 *
 * @author Abe White
 * @author Stephen Kim
 */
public class Column
    extends ReferenceCounter {

    public static final int FLAG_UNINSERTABLE = 2 << 0;
    public static final int FLAG_UNUPDATABLE = 2 << 1;
    public static final int FLAG_DIRECT_INSERT = 2 << 2;
    public static final int FLAG_DIRECT_UPDATE = 2 << 3;
    public static final int FLAG_FK_INSERT = 2 << 4;
    public static final int FLAG_FK_UPDATE = 2 << 5;
    public static final int FLAG_PK_JOIN = 2 << 6;

    private String _name = null;
    private String _fullName = null;
    private Table _table = null;
    private String _tableName = null;
    private String _schemaName = null;
    private int _type = Types.OTHER;
    private String _typeName = null;
    private int _javaType = JavaTypes.OBJECT;
    private int _size = 0;
    private int _decimals = 0;
    private String _defaultStr = null;
    private Object _default = null;
    private Boolean _notNull = null;
    private boolean _autoAssign = false;
    private boolean _rel = false;
    private String _target = null;
    private String _targetField = null;
    private int _flags = 0;

    private int _index = 0;
    private boolean _pk = false;
    private VersionStrategy _versionStrategy = null;
    private String _comment = null;

    /**
     * Default constructor.
     */
    public Column() {
    }

    /**
     * Constructor.
     *
     * @param name the name of the column
     * @param table the column's table
     */
    public Column(String name, Table table) {
        setName(name);
        if (table != null) {
            setTableName(table.getName());
            setSchemaName(table.getSchemaName());
        }
        _table = table;
    }

    /**
     * Called when the column is removed from its table. Removes the column
     * from all table constraints and indexes, then invalidates it.
     */
    void remove() {
        Table table = getTable();
        if (table == null)
            return;

        Schema schema = table.getSchema();
        if (schema != null && schema.getSchemaGroup() != null) {
            Schema[] schemas = schema.getSchemaGroup().getSchemas();
            Table[] tabs;
            ForeignKey[] fks;
            Column[] cols;
            Column[] pks;
            for (int i = 0; i < schemas.length; i++) {
                tabs = schemas[i].getTables();
                for (int j = 0; j < tabs.length; j++) {
                    fks = tabs[j].getForeignKeys();
                    for (int k = 0; k < fks.length; k++) {
                        cols = fks[k].getColumns();
                        pks = fks[k].getPrimaryKeyColumns();
                        for (int l = 0; l < cols.length; l++)
                            if (this.equals(cols[l]) || this.equals(pks[l]))
                                fks[k].removeJoin(cols[l]);

                        cols = fks[k].getConstantColumns();
                        for (int l = 0; l < cols.length; l++)
                            if (this.equals(cols[l]))
                                fks[k].removeJoin(cols[l]);

                        pks = fks[k].getConstantPrimaryKeyColumns();
                        for (int l = 0; l < pks.length; l++)
                            if (this.equals(pks[l]))
                                fks[k].removeJoin(pks[l]);

                        if (fks[k].getColumns().length == 0
                            && fks[k].getConstantColumns().length == 0)
                            tabs[j].removeForeignKey(fks[k]);
                    }
                }
            }
        }

        Index[] idxs = table.getIndexes();
        for (int i = 0; i < idxs.length; i++)
            if (idxs[i].removeColumn(this) && idxs[i].getColumns().length == 0)
                table.removeIndex(idxs[i]);

        Unique[] unqs = table.getUniques();
        for (int i = 0; i < unqs.length; i++)
            if (unqs[i].removeColumn(this) && unqs[i].getColumns().length == 0)
                table.removeUnique(unqs[i]);

        PrimaryKey pk = table.getPrimaryKey();
        if (pk != null && pk.removeColumn(this) && pk.getColumns().length == 0)
            table.removePrimaryKey();

        _table = null;
    }

    /**
     * Return the table for the column.
     */
    public Table getTable() {
        return _table;
    }

    /**
     * The column's table name.
     */
    public String getTableName() {
        return _tableName;
    }

    /**
     * The column's table name. You can only call this method on columns
     * whose table object is not set.
     */
    public void setTableName(String name) {
        if (getTable() != null)
            throw new IllegalStateException();
        _tableName = name;
        _fullName = null;
    }
    
    /**
     * Reset the table name with the fully qualified table name which
     * includes the schema name
     */
    public void resetTableName(String name) {
        _tableName = name;
    }

    /**
     * The column's schema name.
     */
    public String getSchemaName() {
        return _schemaName;
    }

    /**
     * The column's schema name. You can only call this method on columns
     * whose table object is not set.
     */
    public void setSchemaName(String name) {
        if (getTable() != null)
            throw new IllegalStateException();
        _schemaName = name;
    }

    /**
     * Return the column's name.
     */
    public String getName() {
        return _name;
    }

    /**
     * Set the column's name. You can only call this method on columns
     * whose table object is not set.
     */
    public void setName(String name) {
        if (getTable() != null)
            throw new IllegalStateException();
        _name = name;
        _fullName = null;
    }

    /**
     * Return the column's full name, in the form &lt;table&gt;.&lt;name&gt;.
     */
    public String getFullName() {
        if (_fullName == null) {
            String name = getName();
            if (name == null)
                return null;
            String tname = getTableName();
            if (tname == null)
                return name;
            _fullName = tname + "." + name;
        }
        return _fullName;
    }

    /**
     * Return the column's SQL type. This will be one of the type constants
     * defined in {@link Types}.
     */
    public int getType() {
        return _type;
    }

    /**
     * Set the column's SQL type. This should be one of the type constants
     * defined in {@link Types}.
     */
    public void setType(int sqlType) {
        _type = sqlType;
    }

    /**
     * The database-specific SQL type of this column.
     */
    public String getTypeName() {
        return _typeName;
    }

    /**
     * The database-specific SQL type of this column.
     */
    public void setTypeName(String typeName) {
        _typeName = typeName;
    }

    /**
     * The Java type the data in this column is treated as, from
     * {@link JavaTypes} or {@link JavaSQLTypes}.
     */
    public int getJavaType() {
        return _javaType;
    }

    /**
     * The Java type the data in this column is treated as, from
     * {@link JavaTypes} or {@link JavaSQLTypes}.
     */
    public void setJavaType(int type) {
        _javaType = type;
    }

    /**
     * Return the column's size.
     */
    public int getSize() {
        return _size;
    }

    /**
     * Set the column's size.
     */
    public void setSize(int size) {
        _size = size;
    }

    /**
     * Return the number of decimal digits for the column, if applicable.
     */
    public int getDecimalDigits() {
        return _decimals;
    }

    /**
     * Set the number of decimal digits for the column.
     */
    public void setDecimalDigits(int digits) {
        _decimals = digits;
    }

    /**
     * Return the default value set for the column, if any.
     */
    public String getDefaultString() {
        return _defaultStr;
    }

    /**
     * Set the default value for the column.
     */
    public void setDefaultString(String def) {
        _defaultStr = def;
        _default = null;
    }

    /**
     * Return the default value set for this column, if any. If only a default
     * string has been set, attempts to convert it to the right type based
     * on the Java type set for this column.
     */
    public Object getDefault() {
        if (_default != null)
            return _default;
        if (_defaultStr == null)
            return null;

        switch (_javaType) {
            case JavaTypes.BOOLEAN:
            case JavaTypes.BOOLEAN_OBJ:
                _default = ("true".equals(_defaultStr)) ? Boolean.TRUE
                    : Boolean.FALSE;
                break;
            case JavaTypes.BYTE:
            case JavaTypes.BYTE_OBJ:
                _default = new Byte(_defaultStr);
                break;
            case JavaTypes.CHAR:
            case JavaTypes.CHAR_OBJ:
                _default = new Character(_defaultStr.charAt(0));
                break;
            case JavaTypes.DOUBLE:
            case JavaTypes.DOUBLE_OBJ:
                _default = new Double(_defaultStr);
                break;
            case JavaTypes.FLOAT:
            case JavaTypes.FLOAT_OBJ:
                _default = new Float(_defaultStr);
                break;
            case JavaTypes.INT:
            case JavaTypes.INT_OBJ:
                _default = Numbers.valueOf(Integer.parseInt(_defaultStr));
                break;
            case JavaTypes.LONG:
            case JavaTypes.LONG_OBJ:
                _default = Numbers.valueOf(Long.parseLong(_defaultStr));
                break;
            case JavaTypes.NUMBER:
            case JavaTypes.BIGDECIMAL:
                _default = new BigDecimal(_defaultStr);
                break;
            case JavaTypes.SHORT:
            case JavaTypes.SHORT_OBJ:
                _default = new Short(_defaultStr);
                break;
            case JavaTypes.DATE:
                _default = new java.util.Date(_defaultStr);
                break;
            case JavaTypes.BIGINTEGER:
                _default = new BigInteger(_defaultStr);
                break;
            case JavaSQLTypes.SQL_DATE:
                _default = Date.valueOf(_defaultStr);
                break;
            case JavaSQLTypes.TIMESTAMP:
                _default = Timestamp.valueOf(_defaultStr);
                break;
            case JavaSQLTypes.TIME:
                _default = Time.valueOf(_defaultStr);
                break;
            default:
                _default = _defaultStr;
        }
        return _default;
    }

    /**
     * Set the default value for the column.
     */
    public void setDefault(Object def) {
        _default = def;
        _defaultStr = (def == null) ? null : def.toString();
    }

    /**
     * Return true if this is a NOT NULL column.
     */
    public boolean isNotNull() {
        return _notNull == Boolean.TRUE;
    }

    /**
     * Set whether this is a NOT NULL column.
     */
    public void setNotNull(boolean notNull) {
        _notNull = (notNull) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Whether the not-null property has been set.
     */
    public boolean isNotNullExplicit() {
        return _notNull != null;
    }

    /**
     * Whether this column is auto-assigned a value on insert.
     */
    public boolean isAutoAssigned() {
        return _autoAssign;
    }

    /**
     * Whether this column is auto-incrementing.
     */
    public void setAutoAssigned(boolean autoAssign) {
        if (autoAssign != _autoAssign && getTable() != null)
            getTable().changeAutoAssigned(this);
        _autoAssign = autoAssign;
    }

    /**
     * Whether this column stores some form of serialized identity value for
     * a related record. This makes the column dependent on the knowing the
     * final identity of the relation before the column value is set.
     */
    public boolean isRelationId() {
        return _rel;
    }

    /**
     * Whether this column stores some form of serialized identity value for
     * a related record. This makes the column dependent on the knowing the
     * final identity of the relation before the column value is set.
     */
    public void setRelationId(boolean rel) {
        if (rel != _rel && getTable() != null)
            getTable().changeRelationId(this);
        _rel = rel;
    }

    /**
     * The name of the column this column joins to, if any. Used for mapping.
     */
    public String getTarget() {
        return _target;
    }

    /**
     * The name of the column this column joins to, if any. Used for mapping.
     */
    public void setTarget(String target) {
        _target = StringUtils.trimToNull(target);
    }

    /**
     * The name of the field this column joins to, if any. Used for mapping.
     */
    public String getTargetField() {
        return _targetField;
    }

    /**
     * The name of the field this column joins to, if any. Used for mapping.
     */
    public void setTargetField(String target) {
        if (target != null && target.length() == 0)
            target = null;
        _targetField = target;
    }

    /**
     * Flags are used for bookkeeping information. They are ignored at runtime.
     */
    public boolean getFlag(int flag) {
        return (_flags & flag) != 0;
    }

    /**
     * Flags are used for bookkeeping information. They are ignored at runtime.
     */
    public void setFlag(int flag, boolean on) {
        if (on)
            _flags |= flag;
        else
            _flags &= ~flag;
    }

    /**
     * Return true if this column belongs to the table's primary key.
     */
    public boolean isPrimaryKey() {
        return _pk;
    }

    /**
     * Set whether this column belongs to the table's primary key.
     */
    void setPrimaryKey(boolean pk) {
        _pk = pk;
    }

    /**
     * Return the column's 0-based index in the owning table.
     */
    public int getIndex() {
        if (getTable() != null)
            getTable().indexColumns();
        return _index;
    }

    /**
     * Set the column's 0-based index in the owning table.
     */
    void setIndex(int index) {
        _index = index;
    }

    /**
     * Whether this column is a LOB.
     */
    public boolean isLob() {
        switch (_type) {
            case Types.BINARY:
            case Types.BLOB:
            case Types.LONGVARBINARY:
            case Types.VARBINARY:
            case Types.CLOB:
                return true;
            default:
                return false;
        }
    }

    /**
     * Return true if this column is compatible with the given JDBC type
     * from {@link Types} and size.
     */
    public boolean isCompatible(int type, String typeName, int size, 
        int decimals) {
        if (type == Types.OTHER || getType() == Types.OTHER)
            return true;

        // note that the given size is currently ignored, but may be useful
        // to dynamically-populating subclasses
        switch (getType()) {
            case Types.BIT:
            case Types.TINYINT:
            case Types.BIGINT:
            case Types.INTEGER:
            case Types.NUMERIC:
            case Types.SMALLINT:
            case Types.DECIMAL:
            case Types.DOUBLE:
            case Types.FLOAT:
            case Types.REAL:
                switch (type) {
                    case Types.BIT:
                    case Types.TINYINT:
                    case Types.BIGINT:
                    case Types.INTEGER:
                    case Types.NUMERIC:
                    case Types.SMALLINT:
                    case Types.DECIMAL:
                    case Types.DOUBLE:
                    case Types.FLOAT:
                    case Types.REAL:
                        return true;
                    default:
                        return false;
                }
            case Types.BINARY:
            case Types.BLOB:
            case Types.LONGVARBINARY:
            case Types.VARBINARY:
            case Types.OTHER:
                switch (type) {
                    case Types.BINARY:
                    case Types.BLOB:
                    case Types.LONGVARBINARY:
                    case Types.VARBINARY:
                    case Types.OTHER:
                        return true;
                    default:
                        return false;
                }
            case Types.CLOB:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.VARCHAR:
                switch (type) {
                    case Types.CLOB:
                    case Types.CHAR:
                    case Types.LONGVARCHAR:
                    case Types.VARCHAR:
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        return true;
                    default:
                        return false;
                }
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
                switch (type) {
                    case Types.LONGVARCHAR:
                    case Types.CLOB:
                    case Types.VARCHAR:
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        return true;
                    default:
                        return false;
                }
            case 2007:  // Oracle-defined opaque type code for XMLType
                switch (type) {
                    case Types.CHAR:
                    case Types.LONGVARCHAR:
                    case Types.VARCHAR:
                    case Types.CLOB:
                    case Types.BLOB:
                        return true;
                     default:
                         return false;
                }
                
            default:
                return type == getType();
        }
    }

    /**
     * Returns the column name.
     */
    public String toString() {
        return getName();
    }

    /**
     * Useful for debugging.
     */
    public String getDescription() {
        StringBuffer buf = new StringBuffer();
        buf.append("Full Name: ").append(getFullName()).append("\n");
        buf.append("Type: ").append(Schemas.getJDBCName(getType())).
            append("\n");
        buf.append("Size: ").append(getSize()).append("\n");
        buf.append("Default: ").append(getDefaultString()).append("\n");
        buf.append("Not Null: ").append(isNotNull()).append("\n");
        return buf.toString();
    }

    /**
     * Tests compatibility.
     */
    public boolean equalsColumn(Column col) {
        if (col == this)
            return true;
        if (col == null)
            return false;

        if (!getFullName().equalsIgnoreCase(col.getFullName()))
            return false;
        if (!isCompatible(col.getType(), col.getTypeName(), col.getSize(),
            col.getDecimalDigits()))
            return false;
        if (getType() == Types.VARCHAR && getSize() > 0 && col.getSize() > 0
            && getSize() != col.getSize())
            return false;
        return true;
    }

    /**
     * Copy information from the given column to this one.
     */
    public void copy(Column from) {
        if (from == null)
            return;
        if (getName() == null)
            setName(from.getName());
        if (getType() == Types.OTHER)
            setType(from.getType());
        if (getTypeName() == null)
            setTypeName(from.getTypeName());
        if (getJavaType() == JavaTypes.OBJECT)
            setJavaType(from.getJavaType());
        if (getSize() == 0)
            setSize(from.getSize());
        if (getDecimalDigits() == 0)
            setDecimalDigits(from.getDecimalDigits());
        if (getDefaultString() == null)
            setDefaultString(from.getDefaultString());
        if (!isNotNullExplicit() && from.isNotNullExplicit())
            setNotNull(from.isNotNull());
        if (!isAutoAssigned())
            setAutoAssigned(from.isAutoAssigned());
        if (!isRelationId())
            setRelationId(from.isRelationId());
        if (getTarget() == null)
            setTarget(from.getTarget());
        if (getTargetField() == null)
            setTargetField(from.getTargetField());
        if (_flags == 0)
            _flags = from._flags;
    }
    
    /**
     * Whether this column is an XML type.
     */
    public boolean isXML() {
        return _typeName != null && _typeName.startsWith("XML");
    }

    public VersionStrategy getVersionStrategy() {
        return _versionStrategy;
    }

    public void setVersionStrategy(VersionStrategy strategy) {
        this._versionStrategy = strategy;
    }

    public boolean hasComment() {
        return _comment != null && !_comment.equalsIgnoreCase(_name);
    }

    public String getComment() {
        return _comment;
    }

    public void setComment(String comment) {
        _comment = comment;
    }
}
