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
package org.apache.openjpa.persistence.annotations;

import java.util.Collection;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.HerdDBDictionary;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.jdbc.sql.PostgresDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.annotations.common.apps.annotApp.annotype.AnnoTest1;
import org.apache.openjpa.persistence.annotations.common.apps.annotApp.annotype.AnnoTest2;

import static org.junit.Assume.assumeFalse;


/**
 * Test for 1-m
 *
 * @author Steve Kim
 */
public class TestOneToMany extends AnnotationTestCase
{
	public TestOneToMany(String name)
	{
		super(name, "annotationcactusapp");
	}

    @Override
    public void setUp() {
        deleteAll(AnnoTest1.class);
        deleteAll(AnnoTest2.class);
        JDBCConfiguration conf = (JDBCConfiguration) getEmf().getConfiguration();
        DBDictionary dict = conf.getDBDictionaryInstance();
        assumeFalse(dict instanceof HerdDBDictionary);
    }

    public void testOneToMany() {
        OpenJPAEntityManager em = (OpenJPAEntityManager) currentEntityManager();
        startTx(em);
        AnnoTest1 pc = new AnnoTest1(5);
        pc.getOneMany().add(new AnnoTest2(15, "foo"));
        pc.getOneMany().add(new AnnoTest2(20, "foobar"));
        em.persist(pc);
        em.persistAll(pc.getOneMany());
        endTx(em);
        endEm(em);

        em = (OpenJPAEntityManager) currentEntityManager();
        pc = em.find(AnnoTest1.class, em.getObjectId(pc));
        Collection<AnnoTest2> many = pc.getOneMany();
        assertEquals(2, many.size());
        for (AnnoTest2 pc2 : many) {
            switch ((int) pc2.getPk1()) {
                case 15:
                    assertEquals("foo", pc2.getPk2());
                    break;
                case 20:
                    assertEquals("foobar", pc2.getPk2());
                    break;
                default:
                    fail("unknown element:" + pc2.getPk1());
            }
        }
        endEm(em);
    }

    public void testInverseOwnerOneToMany() {
        OpenJPAEntityManager em = (OpenJPAEntityManager) currentEntityManager();
        startTx(em);
        AnnoTest1 pc = new AnnoTest1(5);
        AnnoTest2 pc2 = new AnnoTest2(15, "foo");
        pc.getInverseOwnerOneMany().add(pc2);
        pc2.setOneManyOwner(pc);
        pc2 = new AnnoTest2(20, "foobar");
        pc.getInverseOwnerOneMany().add(pc2);
        pc2.setOneManyOwner(pc);
        em.persist(pc);
        em.persistAll(pc.getInverseOwnerOneMany());
        endTx(em);
        endEm(em);

        em = (OpenJPAEntityManager)currentEntityManager();
        pc = em.find(AnnoTest1.class, em.getObjectId(pc));
        Collection<AnnoTest2> many = pc.getInverseOwnerOneMany();
        assertEquals(2, many.size());
        for (AnnoTest2 pc3 : many) {
            assertEquals(pc, pc3.getOneManyOwner());
            switch ((int) pc3.getPk1()) {
                case 15:
                    assertEquals("foo", pc3.getPk2());
                    break;
                case 20:
                    assertEquals("foobar", pc3.getPk2());
                    break;
                default:
                    fail("unknown element:" + pc3.getPk1());
            }
        }
        endEm(em);
    }
}
