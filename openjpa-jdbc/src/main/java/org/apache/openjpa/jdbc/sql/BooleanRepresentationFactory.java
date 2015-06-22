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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

/**
 * Factory which is being used to create the active {@link org.apache.openjpa.jdbc.sql.BooleanRepresentation}.
*/
public class BooleanRepresentationFactory {

    public static BooleanRepresentation INT_10 = new Int10BooleanRepresentation();
    public static BooleanRepresentation BOOLEAN = new BooleanBooleanRepresentation();

    /**
     * {@link org.apache.openjpa.jdbc.sql.BooleanRepresentation}s built in by default.
     * Key is their config name, value is the BooleanRepresentation.
     */
    static final Map<String, BooleanRepresentation<?>> BUILTIN_BOOLEAN_REPRESENTATIONS;

    static {
        BUILTIN_BOOLEAN_REPRESENTATIONS = new HashMap<String, BooleanRepresentation<?>>();

        /**
         * Booleans are natively supported by this very database.
         * The database column is e.g. a NUMBER(1)
         * OpenJPA will use preparedStatement.setBoolean(..) for it
         */
        BUILTIN_BOOLEAN_REPRESENTATIONS.put("BOOLEAN", BOOLEAN);

        /**
         * Booleans are stored as numeric int 1 and int 0 values.
         * The database column is e.g. a NUMBER(1)
         * OpenJPA will use preparedStatement.setInt(..) for it
         */
        BUILTIN_BOOLEAN_REPRESENTATIONS.put("INT_10", INT_10);

        /**
         * Booleans are stored as String "1" for {@code true}
         * and String "0" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        BUILTIN_BOOLEAN_REPRESENTATIONS.put("STRING_10", new StringBooleanRepresentation("1", "0"));

        /**
         * Booleans are stored as String "Y" for {@code true}
         * and String "N" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        BUILTIN_BOOLEAN_REPRESENTATIONS.put("STRING_YN", new StringBooleanRepresentation("Y", "N"));

        /**
         * Booleans are stored as String "y" for {@code true}
         * and String "n" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        BUILTIN_BOOLEAN_REPRESENTATIONS.put("STRING_YN_LOWERCASE", new StringBooleanRepresentation("y", "n"));

        /**
         * Booleans are stored as String "T" for {@code true}
         * and String "F" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        BUILTIN_BOOLEAN_REPRESENTATIONS.put("STRING_TF", new StringBooleanRepresentation("T", "F"));

        /**
         * Booleans are stored as String "t" for {@code true}
         * and String "f" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        BUILTIN_BOOLEAN_REPRESENTATIONS.put("STRING_TF_LOWERCASE", new StringBooleanRepresentation("t", "f"));

    }


    public static BooleanRepresentation valueOf(String booleanRepresentationKey, ClassLoader cl) {
        // 1st step, try to lookup the BooleanRepresentation from the default ones
        BooleanRepresentation booleanRepresentation = BUILTIN_BOOLEAN_REPRESENTATIONS.get(booleanRepresentationKey);

        if (booleanRepresentation == null && booleanRepresentationKey.contains("/")) {
            // if the key contains a '/' then the first value is the key for 'true', the 2nd value is for 'false'
            String[] vals = booleanRepresentationKey.split("/");
            if (vals.length == 2) {
                booleanRepresentation = new StringBooleanRepresentation(vals[0], vals[1]);
            }
        }
        else {
            // or do a class lookup for a custom BooleanRepresentation
            try {
                Class<? extends BooleanRepresentation> booleanRepresentationClass
                        = (Class<? extends BooleanRepresentation>) cl.loadClass(booleanRepresentationKey);
                booleanRepresentation = booleanRepresentationClass.newInstance();
            }
            catch (Exception e) {
                // nothing to do
            }
        }

        if (booleanRepresentation == null) {
            Localizer _loc = Localizer.forPackage(BooleanRepresentation.class);
            throw new UserException(_loc.get("unknown-booleanRepresentation",
                    new Object[]{booleanRepresentationKey,
                            Arrays.toString(BUILTIN_BOOLEAN_REPRESENTATIONS.keySet().toArray(new String[]{}))}
            ));

        }

        return booleanRepresentation;
    }



    /**
     * BooleanRepresentation which takes 2 strings for true and false representations
     * as constructor parameter;
     */
    public static class StringBooleanRepresentation implements BooleanRepresentation<String> {
        private final String trueRepresentation;
        private final String falseRepresentation;

        public StringBooleanRepresentation(String trueRepresentation, String falseRepresentation) {
            this.trueRepresentation = trueRepresentation;
            this.falseRepresentation = falseRepresentation;
        }

        @Override
        public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException {
            stmnt.setString(idx, val ? trueRepresentation : falseRepresentation);
        }

        @Override
        public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
            return trueRepresentation.equals(rs.getString(columnIndex));
        }

        @Override
        public String getRepresentation(boolean bool) {
            return bool ? trueRepresentation : falseRepresentation;
        }

        @Override
        public String toString() {
            return "StringBooleanRepresentation with the following values for true and false: "
                    + trueRepresentation + " / " + falseRepresentation;
        }
    }

    public static class BooleanBooleanRepresentation implements BooleanRepresentation<Boolean> {
        @Override
        public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException {
            stmnt.setBoolean(idx, val);
        }

        @Override
        public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getBoolean(columnIndex);
        }

        @Override
        public Boolean getRepresentation(boolean bool) {
            return bool;
        }

        @Override
        public String toString() {
            return "BooleanBooleanRepresentation";
        }
    }

    public static class Int10BooleanRepresentation implements BooleanRepresentation<Integer> {
        @Override
        public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException{
            stmnt.setInt(idx, val ? 1 : 0);
        }

        @Override
        public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getInt(columnIndex) > 0;
        }

        @Override
        public Integer getRepresentation(boolean bool) {
            return bool ? 1 : 0;
        }

        @Override
        public String toString() {
            return "Int10BooleanRepresentation";
        }
    }

}
