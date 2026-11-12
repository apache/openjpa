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
package org.apache.openjpa.persistence.dynamicschema.names;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

/**
 * Persistence units for different databases in one JVM each get the names
 * they get alone: whether a name that is a reserved word is delimited, and
 * so keeps its case and is not renamed, is decided by the dictionary of the
 * persistence unit, not by the dictionary configured last (OPENJPA-3076).
 * MySQL and MariaDB delimit such a name, the other dictionaries append a
 * <code>0</code> to it or leave it alone. The persistence units never
 * connect, see {@link NameDump}.
 */
public class TestNamesPerDictionary extends TestCase {

    public void testMySQLAndDerby() {
        Map<String, String> names = assertNamesPerDictionary("mysql", "derby");

        String mysql = names.get("mysql");
        assertTrue(mysql, mysql.contains("MAPPING Partition table=\"Partition\"|`Partition`\n"));
        assertTrue(mysql, mysql.contains("MAPPING Window table=\"Window\"|`Window`\n"));
        assertTrue(mysql, mysql.contains("FIELD window col=\"window\"|`window`\n"));
        assertTrue(mysql, mysql.contains("FIELD user col=\"user_id\"|`user_id`\n"));
        assertTrue(mysql, mysql.contains("FIELD attributes table=\"Order_attributes\"|`Order_attributes`"));

        String derby = names.get("derby");
        assertTrue(derby, derby.contains("MAPPING Partition table=Partition|Partition\n"));
        // Derby reserves WINDOW as well, and renames it rather than delimiting it
        assertTrue(derby, derby.contains("MAPPING Window table=WINDOW0|WINDOW0\n"));
        assertTrue(derby, derby.contains("FIELD window col=WINDOW0|WINDOW0\n"));
        assertTrue(derby, derby.contains("FIELD user col=USER_ID|USER_ID\n"));
        assertTrue(derby, derby.contains("FIELD attributes table=Order_attributes|Order_attributes"));
    }

    public void testMariaDBAndPostgreSQL() {
        Map<String, String> names = assertNamesPerDictionary("mariadb", "postgres");

        String mariadb = names.get("mariadb");
        assertTrue(mariadb, mariadb.contains("MAPPING Order table=\"Order\"|`Order`\n"));
        assertTrue(mariadb, mariadb.contains("FIELD offset col=\"offset\"|`offset`\n"));
        assertTrue(mariadb, mariadb.contains("FIELD window col=window|window\n"));

        String postgres = names.get("postgres");
        assertTrue(postgres, postgres.contains("MAPPING Order table=Order|Order\n"));
        assertTrue(postgres, postgres.contains("FIELD offset col=OFFSET0|OFFSET0\n"));
        assertTrue(postgres, postgres.contains("FIELD window col=WINDOW0|WINDOW0\n"));
    }

    /**
     * The persistence units of all setups of {@link NameDump}, mapped after
     * the dictionaries of all of them were configured, in either order, get
     * the names they get alone, including those of Derby, which renames the
     * words it reserves rather than delimiting them.
     */
    public void testAllSetups() {
        List<String> keys = new ArrayList<>(NameDump.SETUPS.keySet());
        Map<String, String> alone = new HashMap<>();
        for (String key : keys) {
            alone.put(key, NameDump.dumpInterleaved(List.of(key)).get(key));
        }
        for (int i = 0; i < 2; i++) {
            Map<String, String> names = NameDump.dumpInterleaved(keys);
            for (String key : keys) {
                assertEquals(key, alone.get(key), names.get(key));
            }
            Collections.reverse(keys);
        }
    }

    /**
     * Persistence units for different databases that are mapped at the same
     * time by different threads get the names they get alone as well.
     */
    public void testConcurrentPersistenceUnits() throws Exception {
        List<String> keys = List.of("mysql", "derby", "mariadb", "postgres", "mysql-compat", "sqlserver");
        Map<String, String> alone = new HashMap<>();
        for (String key : keys) {
            alone.put(key, NameDump.dump(key));
        }
        ExecutorService executor = Executors.newFixedThreadPool(keys.size());
        try {
            List<Future<String>> dumps = new ArrayList<>();
            List<String> dumped = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                for (String key : keys) {
                    dumps.add(executor.submit(() -> NameDump.dump(key)));
                    dumped.add(key);
                }
            }
            for (int i = 0; i < dumps.size(); i++) {
                assertEquals(dumped.get(i), alone.get(dumped.get(i)), dumps.get(i).get(5, TimeUnit.MINUTES));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * The names of the first setup, mapped once its dictionary was the only
     * one configured and once after the dictionary of the second setup was
     * configured, in either order, are the same, and so are those of the
     * second setup.
     *
     * @return the names of both setups
     */
    private static Map<String, String> assertNamesPerDictionary(String first, String second) {
        String firstAlone = NameDump.dumpInterleaved(List.of(first)).get(first);
        String secondAlone = NameDump.dumpInterleaved(List.of(second)).get(second);

        // the first is mapped after the second was configured
        Map<String, String> names = NameDump.dumpInterleaved(List.of(first, second));
        assertEquals(firstAlone, names.get(first));
        assertEquals(secondAlone, names.get(second));

        // the second is mapped after the first was configured
        names = NameDump.dumpInterleaved(List.of(second, first));
        assertEquals(firstAlone, names.get(first));
        assertEquals(secondAlone, names.get(second));
        return names;
    }
}
