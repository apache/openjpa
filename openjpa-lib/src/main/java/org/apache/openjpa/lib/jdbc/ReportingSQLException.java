/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.jdbc;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * A {@link SQLException} that contains information about
 * the {@link Statement} SQL that caused the exception.
 *
 * @author Marc Prud'hommeaux
 * @nojavadoc
 */
public class ReportingSQLException extends SQLException {

    private final transient Statement _stmnt;
    private final SQLException _sqle;

    public ReportingSQLException(SQLException sqle, Statement stmnt,
        String sql) {
        super(getExceptionMessage(sqle, stmnt, sql));
        this._sqle = sqle;
        this._stmnt = stmnt;
        setNextException(sqle);
    }

    public ReportingSQLException(SQLException sqle, String sql) {
        this(sqle, null, sql);
    }

    public ReportingSQLException(SQLException sqle, Statement stmnt) {
        this(sqle, stmnt, null);
    }

    /**
     * Returns the SQL state of the underlying {@link SQLException}.
     */
    public String getSQLState() {
        return _sqle.getSQLState();
    }

    /**
     * Returns the error code of the underlying {@link SQLException}.
     */
    public int getErrorCode() {
        return _sqle.getErrorCode();
    }

    /**
     * Returns the {@link Statement} that caused the exception.
     */
    public Statement getStatement() {
        return _stmnt;
    }

    private static String getExceptionMessage(SQLException sqle,
        Statement stmnt, String sql) {
        try {
            if (stmnt != null)
                return sqle.getMessage() + " {" + stmnt + "} "
                    + "[code=" + sqle.getErrorCode() + ", state="
                    + sqle.getSQLState() + "]";
            else if (sql != null)
                return sqle.getMessage() + " {" + sql + "} "
                    + "[code=" + sqle.getErrorCode() + ", state="
                    + sqle.getSQLState() + "]";
            else
                return sqle.getMessage() + " "
                    + "[code=" + sqle.getErrorCode() + ", state="
                    + sqle.getSQLState() + "]";
        } catch (Throwable t) {
            return sqle.getMessage();
        }
    }
}

