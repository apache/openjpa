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
package org.apache.openjpa.persistence.jdbc.schema;

import java.util.*;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.schema.Index;
import org.apache.openjpa.jdbc.schema.PrimaryKey;
import org.apache.openjpa.jdbc.schema.Schema;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.schema.Table;

import org.apache.openjpa.persistence.jdbc.common.apps.*;

import java.lang.annotation.Annotation;
import junit.framework.*;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.jdbc.kernel.BaseJDBCTest;

public class TestSchemaMultithreaded extends BaseJDBCTest {

    private int THREADS = 10;
    private int LOOPCOUNT = 1000000;
    private int THREAD = 0;

    public TestSchemaMultithreaded() {
    }

    public TestSchemaMultithreaded(String test) {
        super(test);
    }

    public void testSchemaSequence() throws InterruptedException {
        // final Schema _schema = new SchemaGroup().addSchema("schema");
        final Schema _schema = new Schema();
        final SchemaGroup _schemagroup = new SchemaGroup();

        Thread[] threads = new Thread[THREADS];

        for (int i = 0; i < THREADS; i++) {
            if (i % 2 == 0) {
                THREAD = i;
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        for (int i = 0; i < LOOPCOUNT; i++) {
                            _schema.addSequence("Table:" + i + " " + THREAD);
                        }
                    }
                });
            } else {
                THREAD = i;
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        for (int i = 0; i < LOOPCOUNT; i++) {
                            _schema.getSequence("Table:" + i + " "
                                + (THREAD - 1));
                        }
                    }
                });
            }
        }

        for (int threadCount = 0; threadCount < THREADS; threadCount++) {
            threads[threadCount].start();
        }

        for (int threadCount = 0; threadCount < THREADS; threadCount++) {
            threads[threadCount].join();
        }
    }
}
