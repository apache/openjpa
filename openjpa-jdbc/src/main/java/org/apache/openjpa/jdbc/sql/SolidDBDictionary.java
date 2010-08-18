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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.exps.FilterValue;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Table;
import org.apache.openjpa.jdbc.schema.Unique;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Dictionary for SolidDB database.
 */
public class SolidDBDictionary
    extends DBDictionary {

    /**
     * Sets whether tables are to be located in-memory or on disk.
     * Creating in-memory tables should append "STORE MEMORY" to the 
     * "CREATE TABLE" statement. Creating disk-based tables should 
     * append "STORE DISK". Since cursor hold over commit can not apply 
     * to M-tables (which will cause SOLID Table Error 13187: The cursor 
     * cannot continue accessing M-tables after the transaction has committed 
     * or aborted. The statement must be re-executed.), the default is 
     * STORE DISK.
     * The default concurrency control mechanism depends on the table type:
     *    Disk-based tables (D-tables) are by default optimistic.
     *    Main-memory tables (M-tables) are always pessimistic.
     * Since OpenJPA applications expects lock waits (as usually is done with 
     * normal pessimistic databases), the server should be set to the pessimistic mode. 
     * The optimistic mode is about not waiting for the locks at all. That increases 
     * concurrency but requires more programming. The pessimistic mode with the 
     * READ COMMITTED isolation level (default) should get as much concurrency as one 
     * might need. The pessimistic locking mode can be set in solid.ini:  
     *    [General]
     *        Pessimistic=yes
     *    
     * 
     */
    public boolean storeIsMemory = false;
    
    /**
     * If true, then simulate auto-assigned values in SolidDB by
     * using a trigger that inserts a sequence value into the
     * primary key value when a row is inserted.
     */
    public boolean useTriggersForAutoAssign = true;

    /**
     * The global sequence name to use for autoassign simulation.
     */
    public String autoAssignSequenceName = null;

    /**
     * Flag to use OpenJPA 0.3 style naming for auto assign sequence name and
     * trigger name for backwards compatibility.
     */
    public boolean openjpa3GeneratedKeyNames = false;


    private static final Localizer _loc = Localizer.forPackage
        (SolidDBDictionary.class);

    public SolidDBDictionary() {
        platform = "SolidDB";
        bitTypeName = "TINYINT";
        blobTypeName = "LONG VARBINARY";
        booleanTypeName = "TINYINT";
        clobTypeName = "LONG VARCHAR";
        doubleTypeName = "DOUBLE PRECISION";
        
        allowsAliasInBulkClause = false;
        useGetStringForClobs = true;
        useSetStringForClobs = true;
        supportsDeferredConstraints = false;
        supportsNullUniqueColumn = false;
        
        concatenateFunction = "CONCAT({0},{1})";
        trimLeadingFunction = "LTRIM({0})";
        trimTrailingFunction = "RTRIM({0})";
        trimBothFunction = "TRIM({0})";

        currentDateFunction = "CURDATE()";
        currentTimeFunction = "CURTIME()";
        currentTimestampFunction = "NOW()";
        
        reservedWordSet.addAll(Arrays.asList(new String[]{
            "BIGINT", "BINARY", "DATE", "TIME", 
            "TINYINT", "VARBINARY"
        }));
    }

    @Override
    public String[] getCreateTableSQL(Table table) {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE ").append(getFullName(table, false)).append(" (");
        Column[] cols = table.getColumns();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0)
                buf.append(", ");
            buf.append(getDeclareColumnSQL(cols[i], false));
        }

        PrimaryKey pk = table.getPrimaryKey();
        String pkStr;
        if (pk != null) {
            pkStr = getPrimaryKeyConstraintSQL(pk);
            if (!StringUtils.isEmpty(pkStr))
                buf.append(", ").append(pkStr);
        }

        Unique[] unqs = table.getUniques();
        String unqStr;
        for (int i = 0; i < unqs.length; i++) {
            unqStr = getUniqueConstraintSQL(unqs[i]);
            if (unqStr != null)
                buf.append(", ").append(unqStr);
        }

        buf.append(") STORE ");
        if (storeIsMemory)
            buf.append("MEMORY");
        else
            buf.append("DISK");
        
        String[] create = new String[]{ buf.toString() };
        if (!useTriggersForAutoAssign)
            return create;

        List seqs = null;
        String seq, trig;
        for (int i = 0; cols != null && i < cols.length; i++) {
            if (!cols[i].isAutoAssigned())
                continue;
            if (seqs == null)
                seqs = new ArrayList(4);

            seq = autoAssignSequenceName;
            if (seq == null) {
                if (openjpa3GeneratedKeyNames)
                    seq = getOpenJPA3GeneratedKeySequenceName(cols[i]);
                else
                    seq = getGeneratedKeySequenceName(cols[i]);
                seqs.add("CREATE SEQUENCE " + seq);
            }
            if (openjpa3GeneratedKeyNames)
                trig = getOpenJPA3GeneratedKeyTriggerName(cols[i]);
            else
                trig = getGeneratedKeyTriggerName(cols[i]);

            // create the trigger that will insert new values into
            // the table whenever a row is created
            // CREATE TRIGGER TRIG01 ON table1 
            //     BEFORE INSERT 
            //     REFERENCING NEW COL1 AS NEW_COL1
            // BEGIN
            //     EXEC SEQUENCE seq1 NEXT INTO NEW_COL1;
            // END;

            seqs.add("CREATE TRIGGER " + trig
                + " ON " + toDBName(table.getIdentifier())
                + " BEFORE INSERT REFERENCING NEW " + toDBName(cols[i].getIdentifier())
                + " AS NEW_COL1 BEGIN EXEC SEQUENCE " + seq + " NEXT INTO NEW_COL1; END");
        }
        if (seqs == null)
            return create;

        // combine create table sql and create seqences sql
        String[] sql = new String[create.length + seqs.size()];
        System.arraycopy(create, 0, sql, 0, create.length);
        for (int i = 0; i < seqs.size(); i++)
            sql[create.length + i] = (String) seqs.get(i);
        return sql;
    }

    /**
     * Trigger name for simulating auto-assign values on the given column.
     */
    protected String getGeneratedKeyTriggerName(Column col) {
        // replace trailing _SEQ with _TRG
        String seqName = getGeneratedKeySequenceName(col);
        return seqName.substring(0, seqName.length() - 3) + "TRG";
    }

    /**
     * Returns a OpenJPA 3-compatible name for an auto-assign sequence.
     */
    protected String getOpenJPA3GeneratedKeySequenceName(Column col) {
        Table table = col.getTable();
        DBIdentifier sName = DBIdentifier.preCombine(table.getIdentifier(), "SEQ");
        return toDBName(getNamingUtil().makeIdentifierValid(sName, table.getSchema().
            getSchemaGroup(), maxTableNameLength, true));
    }

    /**
     * Returns a OpenJPA 3-compatible name for an auto-assign trigger.
     */
    protected String getOpenJPA3GeneratedKeyTriggerName(Column col) {
        Table table = col.getTable();        
        DBIdentifier sName = DBIdentifier.preCombine(table.getIdentifier(), "TRIG");
        return toDBName(getNamingUtil().makeIdentifierValid(sName, table.getSchema().
            getSchemaGroup(), maxTableNameLength, true));
    }

    @Override
    public String convertSchemaCase(DBIdentifier objectName) {
        if (objectName != null && objectName.getName() == null)
            return "";
        return super.convertSchemaCase(objectName);
    }
    
    @Override
    public void substring(SQLBuffer buf, FilterValue str, FilterValue start,
            FilterValue end) {
        if (end != null) {
            super.substring(buf, str, start, end);
        } else {
            buf.append(substringFunctionName).append("(");
            str.appendTo(buf);
            buf.append(", ");
            if (start.getValue() instanceof Number) {
                long startLong = toLong(start);
                buf.append(Long.toString(startLong + 1));
            } else {
                buf.append("(");
                start.appendTo(buf);
                buf.append(" + 1)");
            }
            buf.append(", ");
            if (start.getValue() instanceof Number) {
                long startLong = toLong(start);
                long endLong = Integer.MAX_VALUE; //2G
                buf.append(Long.toString(endLong - startLong));
            } else {
                buf.append(Integer.toString(Integer.MAX_VALUE));
                buf.append(" - (");
                start.appendTo(buf);
                buf.append(")");
            }
            buf.append(")");
        }
    }
    
    @Override
    public void indexOf(SQLBuffer buf, FilterValue str, FilterValue find,
        FilterValue start) {
        buf.append("(LOCATE(");
        find.appendTo(buf);
        buf.append(", ");
        str.appendTo(buf);
        if (start != null) {
            buf.append(", ");
            if (start.getValue() instanceof Number) {
                long startLong = toLong(start);
                buf.append(Long.toString(startLong + 1));
            } else {
                buf.append("(");
                start.appendTo(buf);
                buf.append(" + 1)");
            }
        }
        buf.append(") - 1)");
    }
   
    @Override
    public boolean isSystemIndex(DBIdentifier name, Table table) {
        // names starting with "$$" are reserved for SolidDB internal use
        String strName = DBIdentifier.isNull(name) ? null : name.getName();
        boolean startsWith$$ = false;
        if (strName != null) {
            startsWith$$ = name.isDelimited() ? strName.startsWith("\"$$") :
                strName.startsWith("$$");
        }
        return super.isSystemIndex(name, table) || startsWith$$; 
    }

    @Override
    public void setBigDecimal(PreparedStatement stmnt, int idx, BigDecimal val,
            Column col) throws SQLException {
        int type = (val == null || col == null) ? JavaTypes.BIGDECIMAL
                : col.getJavaType();
        switch (type) {
        case JavaTypes.DOUBLE:
        case JavaTypes.DOUBLE_OBJ:
            setDouble(stmnt, idx, val.doubleValue(), col);
            break;
        case JavaTypes.FLOAT:
        case JavaTypes.FLOAT_OBJ:
            setFloat(stmnt, idx, val.floatValue(), col);
            break;
        case JavaTypes.LONG:
        case JavaTypes.LONG_OBJ:
            setLong(stmnt, idx, val.longValue(), col);
            break;
        default:
            super.setBigDecimal(stmnt, idx, val, col);
        }
    }

    @Override
    public void setDouble(PreparedStatement stmnt, int idx, double val,
            Column col) throws SQLException {
        int type = (col == null) ? JavaTypes.DOUBLE
                : col.getJavaType();
        switch (type) {
        case JavaTypes.DOUBLE:
        case JavaTypes.DOUBLE_OBJ:
            super.setDouble(stmnt, idx, val, col);
            break;
        case JavaTypes.FLOAT:
        case JavaTypes.FLOAT_OBJ:
            setFloat(stmnt, idx, new Double(val).floatValue(), col);
            break;
        case JavaTypes.LONG:
        case JavaTypes.LONG_OBJ:
            setLong(stmnt, idx, new Double(val).longValue(), col);
            break;
        }
    }
    
    @Override
    public boolean needsToCreateIndex(Index idx, Table table) {
       // SolidDB will automatically create a unique index for the 
       // constraint, so don't create another index again
       PrimaryKey pk = table.getPrimaryKey();
       if (pk != null && idx.columnsMatch(pk.getColumns()))
           return false;
       return true;
    }
    
}
