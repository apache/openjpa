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
package org.apache.openjpa.persistence.compatible;

import java.sql.Types;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.strats.ClobValueHandler;
import org.apache.openjpa.jdbc.meta.strats.FullClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.MaxEmbeddedClobFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.NoneClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.RelationFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.StringFieldStrategy;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.meta.ValueStrategies;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * <p>Test embeddable superclasses</p>
 *
 *
 * <b>Compatible testcases</b> are used to test various backwards compatibility scenarios between JPA 2.0 and JPA 1.2
 * 
 * <p>The following scenarios are tested:
 * <ol>
 * <li>RelationFieldStrategy
 * </ol>
 * <p> 
 * <b>Note(s):</b>
 * <ul>
 * <li>The proper openjpa.Compatibility value(s) must be provided in order for the testcase(s) to succeed
 * </ul>
 */
public class TestEmbeddableSuperclass
    extends SingleEMFTestCase {

    public void setUp() {
        setUp(EmbeddableSuper.class, EmbeddableSuperSub.class, CLEAR_TABLES);
    }

    public void testRelationMappings() {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        ClassMapping cls = conf.getMappingRepositoryInstance().
            getMapping(EmbeddableSuperSub.class, null, true);
        FieldMapping fm = cls.getFieldMapping("sub");
        assertTrue(fm.getStrategy() instanceof RelationFieldStrategy);

        fm = cls.getFieldMapping("sup");
        // OPENJPA-1214 - Starting with OpenJPA 2, the below will return
        // an EmbedFieldStrategy instead.
        assertTrue(fm.getStrategy() instanceof RelationFieldStrategy);
    } 
}
