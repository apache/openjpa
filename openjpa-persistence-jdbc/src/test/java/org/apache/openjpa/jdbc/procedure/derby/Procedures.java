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
package org.apache.openjpa.jdbc.procedure.derby;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Assertions;

public final class Procedures {
    public static boolean simpleCalled;

    public static void simple() {
        simpleCalled = true;
    }

    public static int inParamsInteger;
    public static String inParamsString;

    public static void inParams(int integer, String string) {
        inParamsInteger = integer;
        inParamsString = string;
    }

    public static void x2(int integer, int[] out) {
        out[0] = integer * 2;
    }

    public static void inout(int[] p) {
        p[0] = p[0] * 2;
    }

    /**
     * Deletes the marked rows of SPQ_TX_ROW through the connection of the caller
     * and returns their number.
     */
    public static void deleteMarked(long[] deleted) throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:default:connection");
             Statement s = c.createStatement()) {
            deleted[0] = s.executeUpdate("DELETE FROM SPQ_TX_ROW WHERE MARKED = 1");
        }
    }

    public static void mapping(ResultSet[] r0, ResultSet[] r1) {
        Assertions.assertDoesNotThrow(() -> {
            Connection c = DriverManager.getConnection("jdbc:default:connection");
            r0[0] = c.createStatement().executeQuery("SELECT * FROM EntityWithStoredProcedure order by id");
            r1[0] = c.createStatement().executeQuery("SELECT * FROM EntityWithStoredProcedure where id = 2");
        });
    }
}
