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
import java.util.Set;

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

        // Derby name length restriction has been relaxed 
        // http://www.archivum.info/derby-dev@db.apache.org/2004-12/msg00270.html
        maxConstraintNameLength = 128;
        maxIndexNameLength = 128;
        maxColumnNameLength = 128;
        maxTableNameLength = 128;

        useGetBytesForBlobs = true;
        useSetBytesForBlobs = true;

        allowsAliasInBulkClause = false;
        supportsDeferredConstraints = false;
        supportsSelectForUpdate = true;
        supportsDefaultDeleteAction = false;
        requiresCastForMathFunctions = true;
        requiresCastForComparisons = true;
        supportsSimpleCaseExpression = false;

        supportsComments = true;

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
    
    @Override
    protected Boolean matchErrorState(int subtype, Set<String> errorStates,
        SQLException ex) {
        Boolean recoverable = null;
        String errorState = ex.getSQLState();
        int errorCode = ex.getErrorCode();
        if (errorStates.contains(errorState)) {
            recoverable = Boolean.FALSE;
            if (subtype == StoreException.LOCK && errorCode < 30000) {
                recoverable = Boolean.TRUE;
            }
        }
        return recoverable;
    }
}
