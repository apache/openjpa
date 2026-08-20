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
package org.apache.openjpa.persistence.simple;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Reproduces the TCK createCustomerData flush ordering failure.
 *
 * The TCK pre-creates the schema with explicit FK constraints via
 * DDL (derby.ddl.persistence.sql), then OpenJPA's buildSchema runs
 * on top. When persisting a Customer with cascaded Addresses, the
 * FK constraints require Address rows to be inserted BEFORE
 * Customer rows.
 *
 * This mimics the TCK schema30 scenario where:
 * - Schema is pre-created with FK constraints (like the TCK DDL)
 * - Entities use PROPERTY access (annotations on getters)
 * - Entities are NOT statically enhanced (runtime subclassing)
 * - Customer is persisted with Address objects set on it
 * - flush() is called after each persist
 */
public class TestCascadeOneToOneFlushOrder {

    /**
     * Creates the schema with explicit FK constraints,
     * mimicking the TCK's derby.ddl.persistence.sql.
     */
    private void createSchemaWithFKConstraints(
            EntityManagerFactory emf) throws Exception {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            Connection conn = em.unwrap(Connection.class);
            Statement stmt = conn.createStatement();

            // Drop tables if they exist (ignore errors)
            try {
                stmt.execute("ALTER TABLE UTCK_CUSTOMER "
                    + "DROP CONSTRAINT FK5_FOR_CUST");
            } catch (Exception e) { /* ignore */ }
            try {
                stmt.execute("ALTER TABLE UTCK_CUSTOMER "
                    + "DROP CONSTRAINT FK6_FOR_CUST");
            } catch (Exception e) { /* ignore */ }
            try {
                stmt.execute("DROP TABLE UTCK_CUSTOMER");
            } catch (Exception e) { /* ignore */ }
            try {
                stmt.execute("DROP TABLE UTCK_ADDRESS");
            } catch (Exception e) { /* ignore */ }

            // Create tables like the TCK DDL
            stmt.execute("CREATE TABLE UTCK_ADDRESS ("
                + "ID VARCHAR(255) PRIMARY KEY NOT NULL, "
                + "STREET VARCHAR(255), "
                + "CITY VARCHAR(255), "
                + "STATE VARCHAR(255), "
                + "ZIP VARCHAR(255))");

            stmt.execute("CREATE TABLE UTCK_CUSTOMER ("
                + "ID VARCHAR(255) PRIMARY KEY NOT NULL, "
                + "NAME VARCHAR(255), "
                + "country VARCHAR(255), "
                + "code VARCHAR(255), "
                + "FK5_FOR_CUSTOMER_TABLE VARCHAR(255), "
                + "FK6_FOR_CUSTOMER_TABLE VARCHAR(255))");

            // Add FK constraints like the TCK does
            stmt.execute("ALTER TABLE UTCK_CUSTOMER "
                + "ADD CONSTRAINT FK5_FOR_CUST "
                + "FOREIGN KEY (FK5_FOR_CUSTOMER_TABLE) "
                + "REFERENCES UTCK_ADDRESS (ID)");
            stmt.execute("ALTER TABLE UTCK_CUSTOMER "
                + "ADD CONSTRAINT FK6_FOR_CUST "
                + "FOREIGN KEY (FK6_FOR_CUSTOMER_TABLE) "
                + "REFERENCES UTCK_ADDRESS (ID)");

            stmt.close();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private EntityManagerFactory createEMF() {
        Map<String, Object> props = new HashMap<>();
        // Use buildSchema but the schema already exists with FKs
        props.put("openjpa.jdbc.SynchronizeMappings",
            "buildSchema");
        props.put("openjpa.RuntimeUnenhancedClasses", "supported");
        props.put("openjpa.DynamicEnhancementAgent", "false");
        props.put("openjpa.MetaDataFactory",
            "jpa(Types="
                + UnenhancedTCKCustomer.class.getName() + ";"
                + UnenhancedTCKAddress.class.getName() + ";"
                + UnenhancedTCKCountry.class.getName()
                + ")");
        return Persistence.createEntityManagerFactory("test", props);
    }

    /**
     * Mimics the TCK createCustomerData pattern:
     * 1. Schema pre-created with FK constraints (like TCK DDL)
     * 2. Persist customer (with cascaded addresses)
     * 3. Flush — must insert Address rows BEFORE Customer
     */
    @Test
    public void testPersistCustomerWithCascadedAddresses()
            throws Exception {
        EntityManagerFactory emf = createEMF();

        // Pre-create schema with FK constraints like the TCK
        createSchemaWithFKConstraints(emf);

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            UnenhancedTCKAddress home = new UnenhancedTCKAddress(
                "1", "1 Oak Road", "Bedford", "MA", "02155");
            UnenhancedTCKAddress work = new UnenhancedTCKAddress(
                "2", "1 Network Drive", "Burlington", "MA",
                "00252");
            UnenhancedTCKCountry country = new UnenhancedTCKCountry(
                "United States", "USA");

            UnenhancedTCKCustomer customer =
                new UnenhancedTCKCustomer(
                    "1", "Alan E. Frechette", home, work, country);

            // Only persist the customer — addresses should be
            // cascaded automatically via cascade=ALL
            em.persist(customer);

            // TCK calls doFlush() after each persist.
            // With FK constraints, this MUST insert Address
            // before Customer.
            em.flush();

            em.getTransaction().commit();
            em.clear();

            // Verify the data was persisted correctly
            UnenhancedTCKCustomer found = em.find(
                UnenhancedTCKCustomer.class, "1");
            assertNotNull("Customer should be found", found);
            assertEquals("Alan E. Frechette", found.getName());
            assertNotNull("Home address should be loaded",
                found.getHome());
            assertEquals("1", found.getHome().getId());
            assertNotNull("Work address should be loaded",
                found.getWork());
            assertEquals("2", found.getWork().getId());
            assertNotNull("Country should be loaded",
                found.getCountry());
            assertEquals("USA", found.getCountry().getCode());
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
            emf.close();
        }
    }

    /**
     * Tests persisting multiple customers with flush after each,
     * mimicking the TCK's loop in createCustomerData.
     * Schema pre-created with FK constraints.
     */
    @Test
    public void testPersistMultipleCustomersWithFlushAfterEach()
            throws Exception {
        EntityManagerFactory emf = createEMF();

        // Pre-create schema with FK constraints like the TCK
        createSchemaWithFKConstraints(emf);

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            for (int i = 1; i <= 5; i++) {
                UnenhancedTCKAddress home = new UnenhancedTCKAddress(
                    String.valueOf(i * 2 - 1),
                    i + " Home St", "City" + i, "ST",
                    "0000" + i);
                UnenhancedTCKAddress work = new UnenhancedTCKAddress(
                    String.valueOf(i * 2),
                    i + " Work Ave", "City" + i, "ST",
                    "1000" + i);
                UnenhancedTCKCountry country =
                    new UnenhancedTCKCountry(
                        "Country" + i, "C" + i);

                UnenhancedTCKCustomer customer =
                    new UnenhancedTCKCustomer(
                        String.valueOf(i), "Customer " + i,
                        home, work, country);

                em.persist(customer);
                em.flush();
            }

            em.getTransaction().commit();
            em.clear();

            // Verify all 5 customers
            for (int i = 1; i <= 5; i++) {
                UnenhancedTCKCustomer found = em.find(
                    UnenhancedTCKCustomer.class,
                    String.valueOf(i));
                assertNotNull("Customer " + i + " should exist",
                    found);
                assertNotNull("Customer " + i + " home address",
                    found.getHome());
                assertNotNull("Customer " + i + " work address",
                    found.getWork());
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
            emf.close();
        }
    }
}
