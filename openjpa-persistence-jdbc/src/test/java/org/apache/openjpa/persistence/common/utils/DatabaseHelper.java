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
package org.apache.openjpa.persistence.common.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.DerbyDictionary;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.persistence.JPAFacadeHelper;

import jakarta.persistence.EntityManager;

/**
 * Allows augmentation of databases, if they don't have support to some 
 * necessary functions, such as DerbyDb's lack of POWER and ROUND
 */
public class DatabaseHelper {

    private static final String CREATE_DERBYDB_POWER_FUNCTION_SQL = "CREATE FUNCTION POWER(a DOUBLE, b DOUBLE) " + 
            "RETURNS DOUBLE PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA EXTERNAL NAME 'java.lang.Math.pow'";

    private static final String DROP_DERBYDB_POWER_FUNCTION_SQL = "DROP FUNCTION POWER";
    
    private static final String CREATE_DERBYDB_ROUND_FUNCTION_SQL = "CREATE FUNCTION ROUND(a DOUBLE, b INTEGER) " + 
            "RETURNS DOUBLE PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA " + 
            "EXTERNAL NAME 'org.apache.openjpa.persistence.common.utils.DatabaseHelper.roundFunction'";
    
    private static final String DROP_DERBYDB_ROUND_FUNCTION_SQL = "DROP FUNCTION ROUND";

    /**
     * Creates the POWER function on DerbyDB, ignoring exceptions if it already exists.
     * 
     */
    public static void createPowerFunctionIfNecessary(EntityManager em, DBDictionary dict) {
        if (dict instanceof DerbyDictionary) {
            try {
                exec(em, true, 10, CREATE_DERBYDB_POWER_FUNCTION_SQL);
            } catch (Exception ex) {
                // swallowing because the function probably already exists and any exceptions
                // should have been ignored on exec.
            }
        }
    }

    /**
     * Drops the POWER function on DerbyDB.
     * 
     */
    public static void dropPowerFunction(EntityManager em, DBDictionary dict) {
        if (dict instanceof DerbyDictionary) {
            try {
                exec(em, true, 10, DROP_DERBYDB_POWER_FUNCTION_SQL);
            } catch (Exception ex) {
                // swallowing because this is just a clean-up
            }
        }
    }

    /**
     * Creates the ROUND function on DerbyDB, ignoring exceptions if it already exists.
     * 
     */
    public static void createRoundFunctionIfNecessary(EntityManager em, DBDictionary dict) {
        if (dict instanceof DerbyDictionary) {
            try {
                exec(em, true, 10, CREATE_DERBYDB_ROUND_FUNCTION_SQL);
            } catch (Exception ex) {
                // swallowing because the function probably already exists and any exceptions
                // should have been ignored on exec.
            }
        }
    }

    public static void dropRoundFunction(EntityManager em, DBDictionary dict) {
        if (dict instanceof DerbyDictionary) {
            try {
                exec(em, true, 10, DROP_DERBYDB_ROUND_FUNCTION_SQL);
            } catch (Exception ex) {
                // swallowing because this is just a clean-up
            }
        }

    }

    /**
     * Rounds the number to given precision.
     */
    public static double roundFunction(Double num, int precision) {
        BigDecimal db = new BigDecimal(Double.toString(num));
        return db.setScale(precision, RoundingMode.HALF_EVEN).doubleValue();
    }

    /**
     * Convenience method to execute SQL statements. Does not close EntityManager
     * after executing.
     * 
     * @param em EntityManager whose connection will be extracted
     * @param ignoreExceptions indicate if exceptions should be ignored during executions
     * @param timeoutSecs timeout, in seconds, of execution
     * @param sql SQL to be executed
     * @throws SQLException
     */
    static void exec(EntityManager em, boolean ignoreExceptions, int timeoutSecs, String sql)
        throws SQLException {
        Statement s = null;
        try {
            assertNotNull(em);
            Broker broker = JPAFacadeHelper.toBroker(em);
            Connection conn = (Connection) broker.getConnection();
            s = conn.createStatement();
            if (timeoutSecs > 0) {
                s.setQueryTimeout(timeoutSecs);
            }
            s.execute(sql);
        } catch (SQLException sqe) {
            if (!ignoreExceptions) {
                // fail(sqe.toString());
                throw sqe;
            }
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }


}
