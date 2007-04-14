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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.lib.util.Localizer;

/**
 * Dictionary for MS SQLServer.
 */
public class SQLServerDictionary
    extends AbstractSQLServerDictionary {

    public static final String VENDOR_MICROSOFT = "microsoft";
    public static final String VENDOR_NETDIRECT = "netdirect";
    public static final String VENDOR_JTDS = "jtds";

    private static final Localizer _loc = Localizer.forPackage
        (SQLServerDictionary.class);

    /**
     * Flag whether to treat UNIQUEIDENTIFIER as VARBINARY or VARCHAR
     */
    public boolean uniqueIdentifierAsVarbinary = true;

    public SQLServerDictionary() {
        platform = "Microsoft SQL Server";

        // SQLServer locks on a table-by-table basis
        forUpdateClause = null;
        tableForUpdateClause = "WITH (UPDLOCK)";

        supportsNullTableForGetColumns = false;
        requiresAliasForSubselect = true;

        stringLengthFunction = "LEN({0})";
    }

    public void connectedConfiguration(Connection conn)
        throws SQLException {
        super.connectedConfiguration(conn);

        DatabaseMetaData meta = conn.getMetaData();
        String driverName = meta.getDriverName();
        String url = meta.getURL();
        if (driverVendor == null) {
            if ("NetDirect JSQLConnect".equals(driverName))
                driverVendor = VENDOR_NETDIRECT;
            else if (driverName != null && driverName.startsWith("jTDS"))
                driverVendor = VENDOR_JTDS;
            else if ("SQLServer".equals(driverName)) {
                if (url != null && url.startsWith("jdbc:microsoft:sqlserver:"))
                    driverVendor = VENDOR_MICROSOFT;
                else if (url != null
                    && url.startsWith("jdbc:datadirect:sqlserver:"))
                    driverVendor = VENDOR_DATADIRECT;
                else
                    driverVendor = VENDOR_OTHER;
            } else
                driverVendor = VENDOR_OTHER;
        }

        // warn about using cursors
        if ((VENDOR_MICROSOFT.equalsIgnoreCase(driverVendor)
            || VENDOR_DATADIRECT.equalsIgnoreCase(driverVendor))
            && url.toLowerCase().indexOf("selectmethod=cursor") == -1)
            log.warn(_loc.get("sqlserver-cursor", url));

        // warn about prepared statement caching if using ms driver
        String props = conf.getConnectionFactoryProperties();
        if (props == null)
            props = "";
        if (VENDOR_MICROSOFT.equalsIgnoreCase(driverVendor)
            && props.toLowerCase().indexOf("maxcachedstatements=0") == -1)
            log.warn(_loc.get("sqlserver-cachedstmnts"));
    }

    public Column[] getColumns(DatabaseMetaData meta, String catalog,
        String schemaName, String tableName, String columnName, Connection conn)
        throws SQLException {
        Column[] cols = super.getColumns(meta, catalog, schemaName, tableName,
            columnName, conn);

        // for opta driver, which reports nvarchar as unknown type
        for (int i = 0; cols != null && i < cols.length; i++) {
            String typeName = cols[i].getTypeName();
            if (typeName == null)
                continue;

            typeName = typeName.toUpperCase();

            if ("NVARCHAR".equals(typeName))
                cols[i].setType(Types.VARCHAR);
            else if ("UNIQUEIDENTIFIER".equals(typeName)) {
                if (uniqueIdentifierAsVarbinary)
                    cols[i].setType(Types.VARBINARY);
                else
                    cols[i].setType(Types.VARCHAR);
            } else if ("NCHAR".equals(typeName))
                cols[i].setType(Types.CHAR);
            else if ("NTEXT".equals(typeName))
                cols[i].setType(Types.CLOB);
        }
        return cols;
    }
}
