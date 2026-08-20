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
package org.apache.openjpa.persistence.embed.xmloverride;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.persistence.test.SingleEMFTestCase;

/**
 * Tests that a class declared as embeddable ONLY in orm.xml (without
 * @Embeddable annotation) is correctly treated as an embedded type.
 *
 * Regression test for OPENJPA-2940: XML-declared embeddables should be
 * mapped as embedded fields, not serialized as BLOBs.
 */
public class TestXmlEmbeddableOverride extends SingleEMFTestCase {

    @Override
    public void setUp() {
        setUp(CLEAR_TABLES);
    }

    @Override
    protected String getPersistenceUnitName() {
        return "xml-embeddable-override-pu";
    }

    /**
     * Verifies that a field of an XML-declared embeddable type can be
     * persisted and retrieved correctly with its individual fields mapped
     * to columns (not serialized as a BLOB).
     */
    public void testXmlEmbeddableOverride() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            XmlApplicant applicant = new XmlApplicant();
            applicant.setName("John Doe");
            applicant.setAddress("123 Main St");

            ComplaintEntity complaint = new ComplaintEntity();
            complaint.setId(1);
            complaint.setComplaintNumber(42);
            complaint.setApplicant(applicant);

            em.persist(complaint);
            em.flush();
            em.clear();

            // Retrieve and verify
            ComplaintEntity retrieved = em.find(ComplaintEntity.class, 1);
            assertNotNull("Complaint should be found", retrieved);
            assertEquals(42, retrieved.getComplaintNumber());

            XmlApplicant retrievedApplicant = retrieved.getApplicant();
            assertNotNull("Applicant should not be null", retrievedApplicant);
            assertEquals("John Doe", retrievedApplicant.getName());
            assertEquals("123 Main St", retrievedApplicant.getAddress());

            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}
