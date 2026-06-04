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
package org.apache.openjpa.jdbc.meta;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.schema.SchemaTool;
import org.apache.openjpa.util.ClassResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestSchemaTool {
    @Parameters(name = "{index}: {0} -> {1}")
    public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
          {"", 0},
          {" ", 0},
          {"org/apache/openjpa/jdbc/meta/testScript1", 0},
          {"org/apache/openjpa/jdbc/meta/testScript2", 1},
          {"org/apache/openjpa/jdbc/meta/testScript3", 1},
          {"org/apache/openjpa/jdbc/meta/testScript4", 0},
          {"org/apache/openjpa/jdbc/meta/testScriptMulti1", 1},
          {"org/apache/openjpa/jdbc/meta/testScriptMulti2", 0},
      });
    }
    @Parameter(0)
    public String sqlScript;
    @Parameter(1)
    public Integer resultingLines;

    @Test
    public void testExecuteScript() throws Exception {
        final ArrayList<String> sqlToRun = new ArrayList<>();
        JDBCConfiguration conf = new JDBCConfigurationImpl();
        conf.setClassResolver(new ClassResolver() {
            @Override
            public ClassLoader getClassLoader(Class<?> contextClass, ClassLoader envLoader) {
                return TestSchemaTool.class.getClassLoader();
            }
        });
        SchemaTool tool = new SchemaTool(conf, SchemaTool.ACTION_EXECUTE_SCRIPT) {
            protected boolean executeSQL(String[] sql) {
                // no-op
                for (String line : sql) {
                    sqlToRun.add(line);
                }
                return false;
            }
        };
        tool.setScriptToExecute(sqlScript);
        tool.run();
        assertEquals(resultingLines.intValue(), sqlToRun.size());
    }
}
