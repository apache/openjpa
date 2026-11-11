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
package org.apache.openjpa.persistence.jdbc.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.test.CombinatorialTestHelper;
import org.apache.openjpa.persistence.test.CombinatorialTestHelper.CombinatorialOption;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for SQL statement ordering capabilities of different update strategies
 * for a Parent-Child model against different physical database constraints.
 *
 * SQL statement ordering is influenced by
 * 1. In-memory schema model: The in-memory schema model can be aware of logical
 * or physical foreign keys.
 * a) This is configured by <code>jdbc.SchemaFactory</code> property setting
 * to <code>native(ForeignKeys=true|false)</code> which makes OpenJPA to
 * read physical foreign key information from database
 * b) @ForeignKey annotation on the relation -- OpenJPA then considers
 * logical foreign key
 *
 * 2. Physical Schema: The database schema can be defined with physical
 * foreign keys. This is configured by <code>jdbc.MappingDefaults</code>
 * property setting to <code>ForeignKeyDeleteAction</code>
 *
 * 3. Update Strategy: the update manager is configured by
 * <code>jdbc.UpdateManager</code>
 *
 * 4. Order of persistence operation: The order in which the application calls
 * persistence operations such as persist() or remove(). In this test, we
 * control this by PersistOrder enum. This application order is maintained
 * if the update manager is set to <code>'operation-order'</code>.
 *
 *
 * This test case also demonstrates how to write a test case that runs with
 * multiple combination of configurations. Each configuration discussed above
 * has multiple possible values and testing all possible combination can be
 * an arduous task. The {@link CombinatorialPersistenceTestCase combinatorial}
 * test case utility helps to auto-generate these multiple configurations and
 * execute the same test with all the combinations.
 *
 * @author Pinaki Poddar
 *
 */
public class TestParentChild extends SQLListenerTestCase {
    // Each of these property keys can take multiple possible values
    private static String Key_UpdateManager = "openjpa.jdbc.UpdateManager";
    private static String Key_SchemaFactory = "openjpa.jdbc.SchemaFactory";
    private static String Key_MappingDefaults = "openjpa.jdbc.MappingDefaults";
    private static String Key_PersistOrder = "persist-order";

    private static String[] Option_MappingDefaults = {
            "ForeignKeyDeleteAction=restrict, JoinForeignKeyDeleteAction=restrict",
            "ForeignKeyDeleteAction=none, JoinForeignKeyDeleteAction=none" };

    private static String[] Option_SchemaFactory = {
            "native(ForeignKeys=false)",
            "native(ForeignKeys=true)" };

    private static String[] Option_UpdateManager = {
            "operation-order",
            "constraint" };

    private static enum PersistOrder {
        IMPLICIT_CASCADE,
        CHILD_THEN_PARENT,
        PARENT_THEN_CHILD
    }

    private static CombinatorialTestHelper helper = new CombinatorialTestHelper()
            .withOption(Key_MappingDefaults, Option_MappingDefaults)
            .withOption(Key_SchemaFactory, Option_SchemaFactory)
            .withOption(Key_UpdateManager, Option_UpdateManager)
            .withOption(Key_PersistOrder, PersistOrder.values(), true);

    private static Stream<Arguments> provideOptions() {
        return helper.provideArguments().map(Arguments::of);
    }

    private CombinatorialTestHelper.CombinatorialOption current = null;

    @BeforeEach
    public void setUp() {
        // only clear is here, actual setul is called from test method
        sql.clear();
    }

    /**
     * This test will run in 2*2*2*3 = 24 times with different configurations.
     */
    @ParameterizedTest
    @MethodSource("provideOptions")
    public void testInsert(CombinatorialOption option) {
        current = option;
        sql.clear();
        super.setUp(option.getProps(DROP_TABLES, Parent.class, Child.class));

        Parent parent = createData(getPersistOrder(), 3);
        validateData(parent.getId(), 3);

        // verification can be challenging under multiple configuration options
        // see the methods as exemplars how verification can vary based on
        // configuration.
        assertLogicalOrPhysicalForeignKey();
        assertPhysicalForeignKeyCreation();
    }

    Parent createData(PersistOrder order, int nChild) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();

        Parent parent = new Parent();
        parent.setName("parent");
        for (int i = 1; i <= nChild; i++) {
            parent.newChild("Child-" + i);
        }
        switch (order) {
            case IMPLICIT_CASCADE:
                em.persist(parent);
                break;
            case CHILD_THEN_PARENT:
                for (Child child : parent.getChildren()) {
                    em.persist(child);
                }
                em.persist(parent);
                break;
            case PARENT_THEN_CHILD:
                em.persist(parent);
                for (Child child : parent.getChildren()) {
                    em.persist(child);
                }
                break;
            default:
                throw new RuntimeException("Bad order " + order);
        }
        em.getTransaction().commit();
        em.clear();
        return parent;
    }

    void validateData(Object pid, int childCount) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Parent parent = em.find(Parent.class, pid);
        assertNotNull(parent);
        assertEquals(childCount, parent.getChildren().size());
        em.getTransaction().rollback();
    }

    /**
     * Asserts that foreign key constraint will be defined on the database
     * for certain combinations of configurations.
     */
    void assertPhysicalForeignKeyCreation() {
        String regex = "ALTER TABLE .* ADD FOREIGN KEY \\(PARENT_ID\\) "
                + "REFERENCES Parent \\(id\\)(\\sDEFERRABLE)?";
        if (getMappingDefaults().contains("restrict")) {
            assertSQL(regex);
        } else {
            assertNotSQL(regex);
        }
    }

    /**
     * Asserts that update SQL will be issued to set the foreign key value
     * after the insert under some configuration.
     */
    void assertPostInsertUpdate() {
        if (PersistOrder.CHILD_THEN_PARENT.equals(getPersistOrder())
                && getMappingDefaults().contains("restrict"))
        {
            assertSQL("UPDATE .* SET PARENT_ID .* WHERE .*");
        }
    }

    /**
     * Asserts that foreign key will be logical or physical under different
     * combination of configuration.
     */
    void assertLogicalOrPhysicalForeignKey() {
        ForeignKey fk = getChildParentForeignKey();
        boolean physicalKeyExists = getMappingDefaults().contains("restrict");
        boolean keyRead = getSchemaFactory().contains("ForeignKeys=true");
        if (physicalKeyExists && keyRead)
            assertFalse(fk.isLogical());
        else if (keyRead)
            assertTrue(fk.isLogical());
    }

    ForeignKey getChildParentForeignKey() {
        MetaDataRepository repos = emf.getConfiguration().getMetaDataRepositoryInstance();
        ClassMetaData child = repos.getCachedMetaData(Child.class);
        FieldMapping parent = (FieldMapping) child.getField("parent");
        return parent.getForeignKey();
    }

    PersistOrder getPersistOrder() {
        return (PersistOrder) current.getOption(Key_PersistOrder);
    }

    String getMappingDefaults() {
        return current.getOptionAsString(Key_MappingDefaults);
    }

    String getSchemaFactory() {
        return current.getOptionAsString(Key_SchemaFactory);
    }
}

/*
 * FIXME TODO SOLOMAX
 * public abstract class CombinatorialPersistenceTestCase extends
 * SQLListenerTestCase {
 * private static final Logger logger =
 * Logger.getLogger(CombinatorialPersistenceTestCase.class.getCanonicalName());
 *
 * protected static CombinatorialTestHelper helper;
 *
 * @BeforeEach
 * public void setUp(Object...props) {
 * super.setUp(getHelper().setCombinatorialOption(props));
 * }
 *
 * @Override
 * public int countTestCases() {
 * return Assertions.countTestCases() * getHelper().getCombinationSize();
 * }
 *
 * @Override
 * public void runBare() throws Throwable {
 * Map<String, Throwable> errors = new HashMap<>();
 * Map<String, AssertionFailedError> failures =
 * new HashMap<>();
 * do {
 * try {
 * super.runBare();
 * } catch (Throwable t) {
 * if (t instanceof AssertionFailedError) {
 * failures.put(getHelper().getOptionsAsString(),
 * (AssertionFailedError)t);
 * testResult.addFailure(this, (AssertionFailedError)t);
 * } else {
 * errors.put(getHelper().getOptionsAsString(), t);
 * testResult.addError(this, t);
 * }
 * }
 * } while (getHelper().hasMoreCombination());
 *
 * if (testResult.errorCount() + testResult.failureCount() > 0) {
 * if (!failures.isEmpty())
 * logger.warning(failures.size() + " assertion failures");
 * for (String o : failures.keySet()) {
 * logger.warning("Combination:\r\n" + o);
 * failures.get(o).printStackTrace();
 * }
 * if (!errors.isEmpty())
 * logger.warning(errors.size() + " errors");
 * for (String o : errors.keySet()) {
 * logger.warning("Combination:\r\n" + o);
 * errors.get(o).printStackTrace();
 * }
 * throw new Throwable(getTestName() + ": "
 * + getHelper().getCombinationSize() + " combinations, "
 * + errors.size() + " errors, " + failures.size()
 * + " failures\r\n"
 * + "Stack trace for each error/failure is printed on console");
 * }
 * }
 *
 *
 *
 * public static CombinatorialTestHelper getHelper() {
 * if (helper == null)
 * helper = new CombinatorialTestHelper();
 * return helper;
 * }
 *
 * @Override
 * public void assertSQL(String sqlExp) {
 * try {
 * super.assertSQL(sqlExp);
 * } catch (AssertionFailedError e) {
 * String newMessage = "Combination\r\n"
 * + getHelper().getOptionsAsString()
 * + " failed \r\n " + e.getMessage();
 * fail(newMessage);
 * }
 * }
 * }
 *
 */
