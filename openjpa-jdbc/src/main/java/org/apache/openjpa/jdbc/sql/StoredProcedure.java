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
package org.apache.openjpa.jdbc.sql;

import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Schemas;
import org.apache.openjpa.meta.MultiQueryMetaData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Holds metadata about a Database Stored Procedure. 
 * This is different than {@link MultiQueryMetaData} which holds the metadata
 * about what the user has specified.
 * <br>
 * An instance of this class can be constructed either by reading from database meta data
 * or by programatic assignment. If an instance if created programmatically, then
 * its SQL body or parameters can be added.
 * <br>
 * This class can generate the SQL statement to create, drop or delete this procedure.
 *
 *
 * @author Pinaki Poddar
 *
 */
public class StoredProcedure {
    private DBIdentifier _catalog;
    private DBIdentifier _schema;
    private DBIdentifier _name;
    private List<Column> _cols = new ArrayList<Column>();
    private List<String> _params = new ArrayList<String>();


    private List<String> _sql = new ArrayList<String>();
    private final boolean _fromDatabase;

    /**
     * An enumeration on type of parameter for a Stored Procedure.
     * The enumerted values has the same ordinal numbers as found
     * in corresponding integer values in {@link DatabaseMetaData}.
     */
    public enum PARAM {UNKNOW, IN, INOUT, RESULT, OUT, RETURN}

    public enum SQL {NONE,MODIFY,READ, CONTAINS};

    /**
     * Create a procedure of the given name.
     */
    public StoredProcedure(String name) {
        _name = DBIdentifier.newProcedure(name);
        _fromDatabase = false;
    }

    /**
     * <pre>
     *
     * 	1. PROCEDURE_CAT - String - the procedure catalog name
     * 	2. PROCEDURE_SCHEM - String - the procedure schema name (possibly null)
     * 	3. PROCEDURE_NAME - String - the procedure name
     * 	4. COLUMN_NAME - String - the name of the column
     * 	5. COLUMN_TYPE - short - the kind of column or parameter, as follows:
     * 			DatabaseMetaData.procedureColumnUnknown - type unknown
     * 			DatabaseMetaData.procedureColumnIn - an IN parameter
     * 			DatabaseMetaData.procedureColumnInOut - an INOUT parameter
     * 			DatabaseMetaData.procedureColumnOut - an OUT parameter
     * 			DatabaseMetaData.procedureColumnReturn - a return value
     * 			DatabaseMetaData.procedureReturnsResult - a result column in a result set
     * 	6. DATA_TYPE - int - the SQL type of the data, as in java.sql.Types
     * 	7. TYPE_NAME - String - the SQL type name, for a UDT it is fully qualified
     * 	8. PRECISION - int - the precision
     * 	9. LENGTH - int - the length of the data in bytes
     * 	10.SCALE - short - the scale for numeric types
     * 	11.RADIX - short - the Radix for numeric data (typically 2 or 10)
     * 	12.NULLABLE - short - can the data contain null:
     * 			DatabaseMetaData.procedureNoNulls - NULLs not permitted
     * 			DatabaseMetaData.procedureNullable - NULLs are permitted
     * 			DatabaseMetaData.procedureNullableUnknown - NULL status unknown
     * 	13.REMARKS - String - an explanatory comment about the data item
     * </pre>
     **/

    public StoredProcedure(ResultSet rs) throws SQLException {
        _fromDatabase = true;
        int i = 0;
        do {
            if (i == 0) {
                // get stored procedure metadata
                _catalog = DBIdentifier.newCatalog(rs.getString(1));
                _schema = DBIdentifier.newSchema(rs.getString(2));
                _name = DBIdentifier.newIdentifier(rs.getString(3), DBIdentifier.DBIdentifierType.PROCEDURE, false);
            }
            Column col = new Column();
            _cols.add(col);
            col.setIdentifier(DBIdentifier.newColumn(rs.getString(4)));
            col.setFlag(rs.getShort(5), true);
            col.setType(rs.getInt(6));
            col.setTypeIdentifier(DBIdentifier.newConstant(rs.getString(7)));
            col.setPrecision(rs.getInt(8));
            col.setSize(rs.getInt(9));
            col.setScale(rs.getInt(10));
            col.setRadix(rs.getShort(11));
            col.setNullability(rs.getShort(12));
            col.setComment(rs.getString(13));
            col.setIndex(i);
            _params.add(col.getIdentifier().getName() + " " + col.getTypeIdentifier().getName());
            i++;
        } while (rs.next());
    }

    public void setCatalog(DBIdentifier catalog) {
        this._catalog = catalog;
    }

    public void setSchema(DBIdentifier schema) {
        this._schema = schema;
    }

    public void setName(String name) {
        this._name = DBIdentifier.newIdentifier(name, DBIdentifier.DBIdentifierType.PROCEDURE, false);
    }

    public Column[] getInColumns() {
        return getColumns((short)DatabaseMetaData.procedureColumnIn);
    }

    public Column[] getInOutColumns() {
        return getColumns((short)DatabaseMetaData.procedureColumnInOut);
    }

    public Column[] getOutColumns() {
        return getColumns((short)DatabaseMetaData.procedureColumnOut);
    }

    public Column[] getReturnColumns() {
        return getColumns((short)DatabaseMetaData.procedureColumnReturn);
    }

    public Column[] getResultColumns() {
        return getColumns((short)DatabaseMetaData.procedureColumnResult);
    }

    public Column[] getColumns() {
        return _cols.toArray(new Column[_cols.size()]);
    }

    /**
     * Counts the number of columns with the given flag.
     * @param flag
     * @return
     */
    int countColumn(short flag) {
        int count = 0;
        for (Column col : _cols) {
            if (col.getFlag(flag)) count++;
        }
        return count;
    }

    Column[] getColumns(short flag) { // TODO: cache?
        List<Column> cols = null;
        for (Column col : _cols) {
            if (col.getFlag(flag)) {
                if (cols == null) cols = new ArrayList<Column>();
                cols.add(col);
            }
        }
        return cols == null ? Schemas.EMPTY_COLUMNS : cols.toArray(new Column[cols.size()]);
    }


    /**
     * Gets the name of this procedure.
     */
    public String getName() {
        return _name.getName();
    }

    /**
     * Adds an {@code IN} parameter of the given name and type.
     * @param var name of the variable
     * @param typeName name of the SQL type e.g. {@code VARCAR(32)}
     * @return this procedure instance
     */
    public StoredProcedure addParameter(String var, String typeName) {
        return addParameter(PARAM.IN, var, typeName);
    }
    /**
     * Adds the given parameter declaration.
     *
     * @param param type of parameter.
     * @param var name of the variable
     * @param typeName name of the SQL type e.g. {@code VARCAR(32)}
     * @return this procedure instance
     */
    public StoredProcedure addParameter(PARAM param, String var, String typeName) {
        assertMutable();

        _params.add(param + " " + var + " " + typeName);
        return this;
    }

    public StoredProcedure setLanguage(String language) {
        _sql.add("LANGUAGE " + language);
        return this;
    }

    /**
     * Gets the SQL for creating this procedure.
     */
    public String getCreateSQL() {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE PROCEDURE ");
        buf.append(_name);
        buf.append(" (");
        for (Iterator<String> p = _params.iterator(); p.hasNext();) {
            buf.append(p.next());
            buf.append(p.hasNext() ? "," : "");
        }
        buf.append(") ");
        //buf.append("(");
        for (String s : _sql) buf.append(s).append(" ");
        //buf.append(")");

        return buf.toString().trim();
    }

    /**
     * Gets the SQL for dropping this procedure.
     */
    public String getDropSQL() {
        return "DROP PROCEDURE " + _name;
    }

    /**
     * Gets the SQL for calling this procedure.
     */
    public String getCallSQL() {
        StringBuilder buf = new StringBuilder();
        buf.append("CALL ");
        buf.append(_name); buf.append(" (");
        for (Iterator<String> p = _params.iterator(); p.hasNext();) {
            p.next();
            buf.append("?");
            if (p.hasNext()) buf.append(",");
        }
        buf.append(")");
        return buf.toString().trim();
    }

    /**
     * Adds a read SQL statement via an external method.
     */
    public StoredProcedure setSQL(SQL sql) {
        switch (sql) {
            case CONTAINS : _sql.add("CONTAINS SQL"); break;
            case NONE     : _sql.add("NO SQL"); break;
            case MODIFY   : _sql.add("MODIFIES SQL DATA"); break;
            case READ     : _sql.add("READS SQL DATA"); break;
        }
        return this;
    }

    /**
     * Sets the language whose parameter passing convention will be used to pass paramater values.
     * @param lang
     * @return
     */
    public StoredProcedure setParameterStyle(String lang) {
        _sql.add("PARAMETER STYLE " + lang);
        return this;
    }

    public StoredProcedure setExternalName(Class<?> cls, String method, Class<?>... paramTypes) {
        assertStaticMethod(cls, method, paramTypes);
        _sql.add("EXTERNAL NAME '" + cls.getName() + '.' + method + "'");
        return this;
    }

    public StoredProcedure setResult(int i) {
        return setResult(i, false);
    }

    public StoredProcedure setResult(int i, boolean dynamic) {
        assertMutable();
        _sql.add((dynamic ? "DYNAMIC " : "") + "RESULT SETS " + i);
        return this;
    }

    private void assertStaticMethod(Class<?> cls, String method, Class<?>...paramTypes) {
        try {
            Method m = cls.getMethod(method, paramTypes);
            if (m == null || !Modifier.isStatic(m.getModifiers())) {
                throw new RuntimeException("No static method " + method + " with arguments "
                        + Arrays.toString(paramTypes) + " in " + cls);
            }
        } catch (Exception ex) {
            throw new RuntimeException("No static method " + method + " with arguments "
                    + Arrays.toString(paramTypes) + " in " + cls, ex);
        }
    }

    private void assertMutable() {
        if (_fromDatabase) {
            throw new IllegalStateException(this + " is not mutable");
        }
    }

    public String toString() {
        return getName();
    }
}
