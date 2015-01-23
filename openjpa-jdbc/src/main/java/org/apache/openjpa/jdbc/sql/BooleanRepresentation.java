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
 * to use  the {@code "STRING_TF"} BooleanRepresentation:
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
 *     <li>One of the values of
 *     {@link org.apache.openjpa.jdbc.sql.BooleanRepresentationFactory#BUILTIN_BOOLEAN_REPRESENTATIONS}
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
 * @param <REPRESENTATION_TYPE> the java type which is used to store the Boolean in the database,
 *                             e.g. {@code String} or {@code Integer}
 */
public interface BooleanRepresentation<REPRESENTATION_TYPE> {

    /**
     * Set the boolean value into the statement
     */
    public void setBoolean(PreparedStatement stmnt, int columnIndex, boolean val) throws SQLException;

    /**
     * Read the boolean from the given ResultSet
     */
    public boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException;


    /**
     * @return return the representation for {@code true} and {@code false}
     */
    public REPRESENTATION_TYPE getRepresentation(boolean bool);

}
