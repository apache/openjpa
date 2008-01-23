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
package org.apache.openjpa.jdbc.kernel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.sql.PrimaryRow;
import org.apache.openjpa.jdbc.sql.Row;
import org.apache.openjpa.jdbc.sql.RowImpl;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.RowManagerImpl;
import org.apache.openjpa.jdbc.sql.SQLExceptions;
import org.apache.openjpa.kernel.OpenJPAStateManager;

/**
 * <P>Batch update manager that writes the SQL in object-level operation order. 
 * This update manager initiates a BatchPreparedStatementManagerImpl which 
 * will utilize the JDBC addBatch() and executeBatch() APIs to batch the 
 * statements for performance improvement.</P>
 * <P>This is the default plug-in class for UpdateManager to support statement 
 * batching. You can plug-in your own statement batch implementation through 
 * the following property: 
 * <PRE>
 *   < property name="openjpa.jdbc.UpdateManager" 
 *     value="org.apache.openjpa.jdbc.kernel.YourOperationOrderUpdateManager" />   
 * </PRE></P>
 * @author Teresa Kan
 */

public class BatchingConstraintUpdateManager extends ConstraintUpdateManager {

    protected PreparedStatementManager newPreparedStatementManager(
            JDBCStore store, Connection conn) {
        int batchLimit = dict.getBatchLimit();
        return new BatchingPreparedStatementManagerImpl(store, conn, batchLimit);
    }
}
