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

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

/**
 * <p>Defines how a {@code Boolean} or {@code boolean} value
 * gets stored in the database by default.</p>
 *
 * <p>The {@link DBDictionary} defines a default representation for {@code Boolean}
 * and {@code boolean} fields in JPA entities. The {@link org.apache.openjpa.jdbc.sql.OracleDictionary}
 * for example uses a {@code NUMBER(1)} with the values {@code (int) 1} and {@code (int) 0} by default.
 * However, sometimes you like to use a different default representation for Boolean values in your database.
 * If your application likes to store boolean values in a {@code CHAR(1)} field with {@code "T"} and
 * {@code "F"} values then you might configure the {@link org.apache.openjpa.jdbc.sql.DBDictionary}
 * to use  the {@link org.apache.openjpa.jdbc.sql.BooleanRepresentation.BooleanRepresentations#STRING_TF}
 * BooleanRepresentation:
 * <pre>
 * &lt;property name="openjpa.jdbc.DBDictionary"
 *     value="(BitTypeName=CHAR(1),BooleanTypeName=CHAR(1),BooleanRepresentation=STRING_10)"/&gt
 * </pre>
 *
 * Please note that you still need to adopt the mapping separately by setting the
 * {@code BitTypeName} and/or {@code BooleanTypeName} (depending on your database) to
 * the desired type in the database.
 * </p>
 *
 * <p>The following {@code BooleanRepresentation} configuration options are possible:
 * <ul>
 *     <li>One of the enum values of {@link org.apache.openjpa.jdbc.sql.BooleanRepresentation.BooleanRepresentations}
 *     , e.g.:
 *         <pre>
 * &lt;property name="openjpa.jdbc.DBDictionary" value="(BooleanRepresentation=STRING_YN)"/&gt
 *         </pre>
 *     </li>
 *     <li>
 *         Two slash ({@code '/'}) separated true/false value strings:
 *         <pre>
 * &lt;property name="openjpa.jdbc.DBDictionary" value="(BooleanRepresentation=oui/non)"/&gt
 *         </pre>
 *     </li>
 *     <li>
 *         A fully qualified class name of your own {@link org.apache.openjpa.jdbc.sql.BooleanRepresentation}
 *         implementation, e.g.:
 *         <pre>
 * &lt;property name="openjpa.jdbc.DBDictionary"
 *     value="(BooleanRepresentation=com.mycompany.MyOwnBoolRepresentation)"/&gt
 *         </pre>
 *     </li>
 * </ul>
 *
 * </p>
 *
 * <p>If a single column uses a different representation then they
 * still can tweak this for those columns with the
 * {@code org.apache.openjpa.persistence.ExternalValues} annotation.</p>
 */
public interface BooleanRepresentation {

    /**
     * Set the boolean value into the statement
     * @param stmnt
     * @param columnIndex
     * @param val the boolean value to set
     * @throws SQLException
     */
    public void setBoolean(PreparedStatement stmnt, int columnIndex, boolean val) throws SQLException;

    public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException;


    public static class Factory {
        public static BooleanRepresentation valueOf(String booleanRepresentationKey, ClassLoader cl) {
            BooleanRepresentation booleanRepresentation = null;

            // 1st step, try to lookup the BooleanRepresentation from the default ones
            try {
                booleanRepresentation = BooleanRepresentations.valueOf(booleanRepresentationKey);
            }
            catch (IllegalArgumentException iae) {
                // nothing to do
            }

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
                    //X TODO probably log some error?
                }
            }


            if (booleanRepresentation == null) {
                Localizer _loc = Localizer.forPackage(BooleanRepresentation.class);
                throw new UserException(_loc.get("unknown-booleanRepresentation",
                        new Object[]{booleanRepresentationKey,
                                Arrays.toString(BooleanRepresentation.BooleanRepresentations.values())}
                ));

            }
            else {
                //X TODO add logging about which one got picked up finally
            }

            return booleanRepresentation;
        }
    }

    /**
     * BooleanRepresentation which takes 2 strings for true and false representations
     * as constructor parameter;
     */
    public static class StringBooleanRepresentation implements BooleanRepresentation {
        private final String trueRepresentation;
        private final String falseRepresentation;

        public StringBooleanRepresentation(String trueRepresentation, String falseRepresentation) {
            this.trueRepresentation = trueRepresentation;
            this.falseRepresentation = falseRepresentation;
        }

        @Override
        public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException{
            stmnt.setString(idx, val ? trueRepresentation : falseRepresentation);
        }

        @Override
        public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
            return trueRepresentation.equals(rs.getString(columnIndex));
        }

        @Override
        public String toString() {
            return "StringBooleanRepresentation with the following values for true and false: "
                    + trueRepresentation + " / " + falseRepresentation;
        }
    }

    public enum BooleanRepresentations implements BooleanRepresentation {

        /**
         * Booleans are natively supported by this very database.
         * The database column is e.g. a NUMBER(1)
         * OpenJPA will use preparedStatement.setBoolean(..) for it
         */
        BOOLEAN {
            @Override
            public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException {
                stmnt.setBoolean(idx, val);
            }

            @Override
            public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
                return rs.getBoolean(columnIndex);
            }
        },

        /**
         * Booleans are stored as numeric int 1 and int 0 values.
         * The database column is e.g. a NUMBER(1)
         * OpenJPA will use preparedStatement.setInt(..) for it
         */
        INT_10 {
            @Override
            public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException{
                stmnt.setInt(idx, val ? 1 : 0);
            }

            @Override
            public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
                return rs.getInt(columnIndex) > 0;
            }
        },

        /**
         * Booleans are stored as String "1" for {@code true}
         * and String "0" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        STRING_10 {
            @Override
            public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException{
                stmnt.setString(idx, val ? "1" : "0");
            }

            @Override
            public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
                return "1".equals(rs.getString(columnIndex));
            }
        },

        /**
         * Booleans are stored as String "Y" for {@code true}
         * and String "N" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        STRING_YN {
            @Override
            public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException{
                stmnt.setString(idx, val ? "Y" : "N");
            }

            @Override
            public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
                return "Y".equals(rs.getString(columnIndex));
            }
        },

        /**
         * Booleans are stored as String "y" for {@code true}
         * and String "n" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        STRING_YN_LOWERCASE {
            @Override
            public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException{
                stmnt.setString(idx, val ? "y" : "n");
            }

            @Override
            public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
                return "y".equals(rs.getString(columnIndex));
            }
        },

        /**
         * Booleans are stored as String "T" for {@code true}
         * and String "F" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        STRING_TF {
            @Override
            public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException{
                stmnt.setString(idx, val ? "T" : "F");
            }

            @Override
            public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
                return "T".equals(rs.getString(columnIndex));
            }

        },

        /**
         * Booleans are stored as String "t" for {@code true}
         * and String "f" for {@code false}.
         * The database column is e.g. a CHAR(1) or VARCHAR(1)
         * OpenJPA will use preparedStatement.setString(..) for it
         */
        STRING_TF_LOWERCASE {
            @Override
            public void setBoolean(PreparedStatement stmnt, int idx, boolean val) throws SQLException{
                stmnt.setString(idx, val ? "t" : "f");
            }

            @Override
            public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
                return "t".equals(rs.getString(columnIndex));
            }
        };

    }

}
