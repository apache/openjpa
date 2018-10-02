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
package org.apache.openjpa.tools.maven.test;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ItDropSchemaTest extends TestCase {

    /** contains the directory where all generated results are placed */
    private final static String TARGET_DIR = "target";

    /** the file containing the generated SQL syntax */
    private final static String SQL_FILE = "clear_database.sql";

    /** if the SQL generation has been successful, the following result should be in the SQL file */
    private final static String VALID_SQL = "DROP TABLE DropSchemaTestEntity;";

    /**
     * check if the generated SQL script is correct.
     * @throws Exception
     */
    public void testSqlGeneration() throws Exception
    {
        File sqlFile = new File( TARGET_DIR, SQL_FILE );
        BufferedReader in = new BufferedReader( new FileReader( sqlFile ) );
        String sqlIn = in.readLine();
        assertEquals( VALID_SQL, sqlIn );
    }

}
