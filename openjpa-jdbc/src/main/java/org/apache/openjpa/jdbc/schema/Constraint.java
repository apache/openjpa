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

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.identifier.QualifiedDBIdentifier;

/**
 * A table constraint. This class is closely aligned with the constraint
 * information available from {@link java.sql.DatabaseMetaData}.
 *
 * @author Abe White
 */
public abstract class Constraint extends ReferenceCounter {
    private static final long serialVersionUID = 1L;
    private DBIdentifier _name = DBIdentifier.NULL;
    private QualifiedDBIdentifier _fullPath = null;
    private Table _table = null;
    private DBIdentifier _tableName = DBIdentifier.NULL;
    private DBIdentifier _schemaName = DBIdentifier.NULL;
    private DBIdentifier _columnName = DBIdentifier.NULL;
    private boolean _deferred = false;

    /**
     * Default constructor.
     */
    Constraint() {
    }

    /**
     * Constructor.
     *
     * @param name the name of the constraint, or null if none
     * @param table the local table of the constraint
     * @deprecated
     */
    @Deprecated
    Constraint(String name, Table table) {
        this(DBIdentifier.newConstant(name), table);
    }

    Constraint(DBIdentifier name, Table table) {
        setIdentifier(name);
        if (table != null) {
            setTableIdentifier(table.getIdentifier());
            setSchemaIdentifier(table.getSchemaIdentifier());
        }
        _table = table;
    }

    /**
     * Called when the constraint is removed from the owning table.
     * Invalidates the constraint.
     */
    void remove() {
        _table = null;
    }

    /**
     * Return the table of this constraint.
     */
    public Table getTable() {
        return _table;
    }

    /**
     * Return the column's table name.
     * @deprecated
     */
    @Deprecated
    public String getTableName() {
        return getTableIdentifier().getName();
    }

    public DBIdentifier getTableIdentifier() {
        return _tableName == null ? DBIdentifier.NULL : _tableName;
    }

    /**
     * Set the column's table name. You can only call this method on
     * columns whose table object is not set.
     * @deprecated
     */
    @Deprecated
    public void setTableName(String name) {
        setTableIdentifier(DBIdentifier.newTable(name));
    }

      public void setTableIdentifier(DBIdentifier name) {
          if (getTable() != null)
              throw new IllegalStateException();
          _tableName = name;
          _fullPath = null;
      }


    /**
     * Return the column table's schema name.
     * @deprecated
     */
    @Deprecated
    public String getSchemaName() {
        return getSchemaIdentifier().getName();
    }

    public DBIdentifier getSchemaIdentifier() {
        return _schemaName == null ? DBIdentifier.NULL : _schemaName;
    }

    /**
     * Set the column table's schema name. You can only call this method on
     * columns whose table object is not set.
     * @deprecated
     */
    @Deprecated
    public void setSchemaName(String schema) {
        setSchemaIdentifier(DBIdentifier.newSchema(schema));
    }

    public void setSchemaIdentifier(DBIdentifier schema) {
        if (getTable() != null)
            throw new IllegalStateException();
        _schemaName = schema;
    }

    /**
     * Return the column's name.
     * @deprecated
     */
    @Deprecated
    public String getColumnName() {
        return getColumnIdentifier().getName();
    }

    public DBIdentifier getColumnIdentifier() {
        return _columnName == null ? DBIdentifier.NULL : _columnName;
    }

    /**
     * Set the column's name. You can only call this method on
     * columns whose table object is not set.
     * @deprecated
     */
    @Deprecated
    public void setColumnName(String name) {
        setColumnIdentifier(DBIdentifier.newColumn(name));
    }

    public void setColumnIdentifier(DBIdentifier name) {
        if (getTable() != null)
            throw new IllegalStateException();
        _columnName = name;
    }

    /**
     * Return the name of the constraint.
     * @deprecated
     */
    @Deprecated
    public String getName() {
        return getIdentifier().getName();
    }

    public DBIdentifier getIdentifier() {
        return _name == null ? DBIdentifier.NULL : _name;
    }


    /**
     * Set the name of the constraint. This method cannot be called if the
     * constraint already belongs to a table.
     * @deprecated
     */
    @Deprecated
    public void setName(String name) {
        setIdentifier(DBIdentifier.newConstraint(name));
    }

    public void setIdentifier(DBIdentifier name) {
        if (getTable() != null)
            throw new IllegalStateException();
        _name = name;
        _fullPath = null;
    }

    /**
     * Return the full name of the constraint.
     * @deprecated
     */
    @Deprecated
    public String getFullName() {
        return getFullIdentifier().getName();
    }

    public QualifiedDBIdentifier getQualifiedPath() {
        if (_fullPath == null) {
            _fullPath = QualifiedDBIdentifier.newPath(getTableIdentifier(), getIdentifier());
        }
        return _fullPath;
    }

    public DBIdentifier getFullIdentifier() {
        return getQualifiedPath().getIdentifier();
    }


    /**
     * Return whether this constraint is a logical constraint only; i.e.
     * if it does not exist in the database.
     */
    public abstract boolean isLogical();

    /**
     * Return true if this is a deferred constraint.
     */
    public boolean isDeferred() {
        return _deferred;
    }

    /**
     * Make this constrain deferred.
     */
    public void setDeferred(boolean deferred) {
        _deferred = deferred;
    }

    @Override
    public String toString() {
        if (!getIdentifier().isNull())
            return getIdentifier().getName();

        String name = getClass().getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        return "<" + name.toLowerCase() + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Constraint that = (Constraint) o;

        if (_deferred != that._deferred) return false;
        if (_name != null ? !_name.equals(that._name) : that._name != null) return false;
        if (_fullPath != null ? !_fullPath.equals(that._fullPath) : that._fullPath != null) return false;
        if (_table != null ? !_table.equals(that._table) : that._table != null) return false;
        if (_tableName != null ? !_tableName.equals(that._tableName) : that._tableName != null) return false;
        if (_schemaName != null ? !_schemaName.equals(that._schemaName) : that._schemaName != null) return false;
        return _columnName != null ? _columnName.equals(that._columnName) : that._columnName == null;
    }

    @Override
    public int hashCode() {
        int result = _name != null ? _name.hashCode() : 0;
        result = 31 * result + (_fullPath != null ? _fullPath.hashCode() : 0);
        result = 31 * result + (_table != null ? _table.hashCode() : 0);
        result = 31 * result + (_tableName != null ? _tableName.hashCode() : 0);
        result = 31 * result + (_schemaName != null ? _schemaName.hashCode() : 0);
        result = 31 * result + (_columnName != null ? _columnName.hashCode() : 0);
        result = 31 * result + (_deferred ? 1 : 0);
        return result;
    }
}
