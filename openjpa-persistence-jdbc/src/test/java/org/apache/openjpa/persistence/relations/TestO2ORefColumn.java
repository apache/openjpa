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
package org.apache.openjpa.persistence.relations;

import javax.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

import junit.framework.Assert;

/**
 * Unit test to verify the foreign key of a Join column in an association (aText), that is exposed as an
 * attribute, is updated after the entity is flushed or committed to the data store.
 * See AText -> ACase's foreign key also exposed a non-insertable and non-updatable attribute 'aCaseId'.
 *
 * In the test case, if the aCaseId is not updated with the foreign key (aCase.id) value, an association
 * of aText (aEvident) with JoinColumn(..referencedColumnName=..) overridden to non-standard foreign key,
 * a constraint violation will occur when aEvident is persisted to the data store.
 */
public class TestO2ORefColumn extends SingleEMFTestCase {

    @Override
    public void setUp () {
        setUp(CLEAR_TABLES,
                ACase.class, AText.class, AEvident.class,
                "openjpa.jdbc.MappingDefaults", "ForeignKeyDeleteAction=cascade,JoinForeignKeyDeleteAction=cascade"
                );
    }

    public void testRefColumnJoinEntities () {
        AEvident aEvident = new AEvident();
        aEvident.setName("Evident_A");

        AText aText = new AText();
        aText.setName("Text_A");
        aText.getAEvidents().add(aEvident);
        aEvident.setAText(aText);

        ACase aCase = new ACase();
        aCase.setName ("Case_A");
        aCase.setAText(aText);
        aText.setACase(aCase);

        EntityManager em = emf.createEntityManager ();
        em.getTransaction().begin ();
        em.persist(aEvident);
        em.persist(aText);
        em.persist(aCase);
        em.getTransaction ().commit ();

        verify(aCase, aText, aEvident);

        em.clear();

        ACase fACase = em.find(ACase.class, aCase.getId());
        AText fAText = fACase.getAText();
        AEvident fAEvident = fAText.getAEvidents().iterator().next();
        verify(fACase, fAText, fAEvident);

        em.close ();
    }

    private void verify(ACase aCase, AText aText, AEvident aEvident) {
        Assert.assertNotNull(aCase);
        Assert.assertNotNull(aText);
        Assert.assertNotNull(aEvident);

        Assert.assertTrue(aCase.getId() != 0);
        Assert.assertTrue(aText.getId() != 0);
        Assert.assertTrue(aEvident.getId() != 0);

        Assert.assertEquals("Case_A", aCase.getName());
        Assert.assertEquals("Text_A", aText.getName());
        Assert.assertEquals("Evident_A", aEvident.getName());

        Assert.assertNotNull(aCase.getAText());
        Assert.assertSame(aCase.getAText(), aText);
        Assert.assertNotNull(aText.getACase());
        Assert.assertSame(aCase, aText.getACase());

        Assert.assertEquals(aText.getACaseId(), aCase.getId());
        Assert.assertNotNull(aText.getAEvidents());
        Assert.assertTrue(aText.getAEvidents().iterator().hasNext());
        Assert.assertSame(aEvident, aText.getAEvidents().iterator().next());
        Assert.assertNotNull(aEvident.getAText());
        Assert.assertSame(aText, aEvident.getAText());
    }
}
