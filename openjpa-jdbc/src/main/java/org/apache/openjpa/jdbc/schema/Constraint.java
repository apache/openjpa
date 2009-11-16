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

/**
 * A table constraint. This class is closely aligned with the constraint
 * information available from {@link java.sql.DatabaseMetaData}.
 *
 * @author Abe White
 */
public abstract class Constraint
    extends ReferenceCounter {

    private String _name = null;
    private String _fullName = null;
    private Table _table = null;
    private String _tableName = null;
    private String _schemaName = null;
    private String _columnName = null;
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
     */
    Constraint(String name, Table table) {
        setName(name);
        if (table != null) {
            setTableName(table.getName());
            setSchemaName(table.getSchemaName());
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
     */
    public String getTableName() {
        return _tableName;
    }

    /**
     * Set the column's table name. You can only call this method on
     * columns whose table object is not set.
     */
    public void setTableName(String name) {
        if (getTable() != null)
            throw new IllegalStateException();
        _tableName = name;
        _fullName = null;
    }

    /**
     * Return the column table's schema name.
     */
    public String getSchemaName() {
        return _schemaName;
    }

    /**
     * Set the column table's schema name. You can only call this method on
     * columns whose tbale object is not set.
     */
    public void setSchemaName(String schema) {
        if (getTable() != null)
            throw new IllegalStateException();
        _schemaName = schema;
    }

    /**
     * Return the column's name.
     */
    public String getColumnName() {
        return _columnName;
    }

    /**
     * Set the column's name. You can only call this method on
     * columns whose table object is not set.
     */
    public void setColumnName(String name) {
        if (getTable() != null)
            throw new IllegalStateException();
        _columnName = name;
    }

    /**
     * Return the name of the constraint.
     */
    public String getName() {
        return _name;
    }

    /**
     * Set the name of the constraint. This method cannot be called if the
     * constraint already belongs to a table.
     */
    public void setName(String name) {
        if (getTable() != null)
            throw new IllegalStateException();
        _name = name;
        _fullName = null;
    }

    /**
     * Return the full name of the constraint.
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

    public String toString() {
        if (getName() != null)
            return getName();

        String name = getClass().getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        return "<" + name.toLowerCase() + ">";
    }
}
