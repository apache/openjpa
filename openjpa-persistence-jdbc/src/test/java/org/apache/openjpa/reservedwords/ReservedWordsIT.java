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
package org.apache.openjpa.reservedwords;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.test.AbstractPersistenceTestCase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Run this test manually or as part of the integration test suite to
 * generate the list of reserved words for a specific Database
 */
public class ReservedWordsIT extends AbstractPersistenceTestCase {
    private static OpenJPAEntityManagerFactory _emf;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        _emf = createEMF(new Object[0]);
    }

    @Override
    public void tearDown() throws Exception {
        closeEMF(_emf);
        _emf = null;
        super.tearDown();
    }


    public void testReservedColumnWords() throws Exception {
        List<String> reservedColumnWords = new ArrayList<>();
        DBDictionary dict = ((JDBCConfiguration)_emf.getConfiguration()).getDBDictionaryInstance();

        EntityManager em = null;
        for (String word : getReservedWords()) {
            try {
                em = _emf.createEntityManager();
                em.getTransaction().begin();
                Query qryCreate = em.createNativeQuery("CREATE TABLE RESERVEDW_TST (" + word + " CHAR(2))");
                qryCreate.executeUpdate();
                em.getTransaction().commit();

                em.getTransaction().begin();
                Query qryDrop = em.createNativeQuery("DROP TABLE RESERVEDW_TST");
                qryDrop.executeUpdate();
                em.getTransaction().commit();
                em.close();
            } catch (Exception e) {
                reservedColumnWords.add(word);
                em.getTransaction().rollback();
            }
            finally {
                if (em != null && em.isOpen()) {
                    em.close();
                }
            }
        }

        Log log = getLog();

        try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("target/reserved_words_" +
                dict.getClass().getSimpleName() + ".java"))) {
            log.info("FOUND " + reservedColumnWords.size() + " RESERVED WORDS for Dictionary " + dict.getClass().getName());
            osw.append(
                    "// reservedWordSet subset that CANNOT be used as valid column names\n" +
                    "// (i.e., without surrounding them with double-quotes)\n" +
                    "// generated at " + LocalDateTime.now() + " via " + ReservedWordsIT.class.getName() + "\n" +
                    "invalidColumnWordSet.addAll(Arrays.asList(new String[] {\n");
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            for (String reservedColumnWord : reservedColumnWords) {
                log.info(reservedColumnWord);
                sb.append('"').append(reservedColumnWord).append("\", ");
                if (sb.length() > 110) {
                    osw.append(sb.toString().trim()).append('\n');
                    sb.setLength(0);
                }
            }

            // also add the rest if any
            if (sb.length() > 0) {
                osw.append(sb.toString().trim()).append('\n');
            }

            osw.append("}));");
            osw.flush();
        }
        log.info("******* END RESERVED WORDS *******");
    }

    private List<String> getReservedWords() {
        List<String> reservedWords = new ArrayList<>(2000);

        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/apache/openjpa/reservedwords/sql_reserved_words.txt")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                if (line.contains(" ")) {
                    throw new RuntimeException("only one reserved word per line please! got: " + line);
                }
                reservedWords.add(line);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return reservedWords;
    }

    private Log getLog() {
        return _emf.getConfiguration().getLog("Tests");
    }


}
