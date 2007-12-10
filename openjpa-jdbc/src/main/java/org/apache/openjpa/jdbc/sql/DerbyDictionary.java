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

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.openjpa.util.StoreException;

/**
 * Dictionary for Apache Derby (formerly Cloudscape).
 */
public class DerbyDictionary
    extends AbstractDB2Dictionary {

    /**
     * If true, we will shutdown the embedded database when closing
     * the DataSource.
     */
    public boolean shutdownOnClose = true;

    public DerbyDictionary() {
        platform = "Apache Derby";
        validationSQL = "VALUES(1)";
        stringLengthFunction = "LENGTH({0})";
        substringFunctionName = "SUBSTR";

        maxConstraintNameLength = 18;
        maxIndexNameLength = 128;
        maxColumnNameLength = 30;
        maxTableNameLength = 128;

        useGetBytesForBlobs = true;
        useSetBytesForBlobs = true;

        allowsAliasInBulkClause = false;
        supportsDeferredConstraints = false;
        supportsSelectForUpdate = true;
        supportsDefaultDeleteAction = false;
        requiresCastForMathFunctions = true;
        requiresCastForComparisons = true;

        fixedSizeTypeNameSet.addAll(Arrays.asList(new String[]{
            "BIGINT", "INTEGER",
        }));
        reservedWordSet.addAll(Arrays.asList(new String[]{
            "ALIAS", "BIGINT", "BOOLEAN", "CALL", "CLASS",
            "COPY", "DB2J_DEBUG", "EXECUTE", "EXPLAIN",
            "FILE", "FILTER", "GETCURRENTCONNECTION", "INDEX",
            "INSTANCEOF", "KEY", "METHOD", "NEW", "OFF", "OUT", "PROPERTIES",
            "PUBLICATION", "RECOMPILE", "REFRESH", "RENAME",
            "RUNTIMESTATISTICS", "STATEMENT", "STATISTICS",
            "TIMING", "WAIT", "XML",
        }));
    }

    public void closeDataSource(DataSource dataSource) {
        super.closeDataSource(dataSource);

        if (!shutdownOnClose)
            return;

        // as well as closing the DataSource, we also need to
        // shut down the instance if we are using an embedded database, which
        // can only be done by connecting to the same URL with the
        // ";shutdown=true" string appended to the end
        // see: http://db.apache.org/derby/docs/dev/devguide/tdevdvlp40464.html
        if (conf != null && conf.getConnectionDriverName() != null &&
            conf.getConnectionDriverName().indexOf("EmbeddedDriver") != -1) {
            try {
                DriverManager.getConnection(conf.getConnectionURL()
                    + ";shutdown=true");
            } catch (SQLException e) {
                // we actuall expect a SQLException to be thrown here:
                // Derby strangely uses that as a mechanism to report
                // a successful shutdown
            }
        }
    }
    
    /**
     * Adds extra SQLState code that Derby JDBC Driver uses. In JDBC 4.0,
     * SQLState will follow either XOPEN or SQL 2003 convention. A compliant
     * driver can be queries via DatabaseMetaData.getSQLStateType() to detect
     * the convention type.<br>
     * This method is overwritten to highlight that a) the SQL State is ideally
     * uniform across JDBC Drivers but not practically and b) the overwritten
     * method must crate a new list to return as the super classes list is
     * unmodifable.
     */
    public List getSQLStates(int exceptionType) {
    	List original = super.getSQLStates(exceptionType);
    	if (exceptionType == StoreException.LOCK) {
    		// Can not add new codes to unmodifable list of the super class
    		List newStates = new ArrayList(original);
    		newStates.add("40XL1");
    		return newStates;
    	}
    	return original;
    }
    
}
