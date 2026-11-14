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
package org.apache.openjpa.persistence.test;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

@Target({TYPE, METHOD})
@Retention(RUNTIME)
@ExtendWith(DisabledBasedOnTest.class)
@interface DisabledBasedOnTestCondition {
}

class DisabledBasedOnTest implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        AbstractPersistenceTestCase test = (AbstractPersistenceTestCase)context.getTestInstance().orElse(null);
        if (test != null && test.skipTest()) {
            return ConditionEvaluationResult.disabled("Skipping test: test skipped due to skipTest == true");
        }
        if (test != null && !test.isRunsOnCurrentPlatform()) {
            return ConditionEvaluationResult.disabled("Skipping test: platform is not supported");
        }
        if (test != null && test.isTestsDisabled()) {
            return ConditionEvaluationResult.disabled("Skipping test: it is disabled");
        }
        return ConditionEvaluationResult.enabled(null);
    }
}
/**
 * Base class for Persistence TestCases. This class contains utility methods but does not maintain an EntityManager or
 * EntityManagerFactory - these tasks are left for subclasses to handle.
 * Performs NO automatic clean up of EMFs created by createEMF() or createNamedEMF().
 */
@DisabledBasedOnTestCondition
public abstract class AbstractPersistenceTestCase {
    public static final String FRESH_EMF = "Creates new EntityManagerFactory";
    public static final String RETAIN_DATA = "Retain data after test run";
    private boolean retainDataOnTearDown;
    protected boolean _fresh = false;
    private TestInfo info = null;

    private static final Logger logger = Logger.getLogger(AbstractPersistenceTestCase.class.getCanonicalName());

    /**
     * Marker object you pass to {@link #setUp} to indicate that the database table rows should be cleared.
     */
    protected static final Object CLEAR_TABLES = new Object();

    /**
     * Marker object you pass to {@link #setUp} to indicate that the database table should be dropped and then
     * recreated.
     */
    protected static final Object DROP_TABLES = new Object();

    protected String getTestName() {
        return Optional.ofNullable(info)
            .flatMap(TestInfo::getTestMethod)
            .map(Member::getName)
            .orElse("Unknown Test");
    }
    /**
     * Create an entity manager factory. Put {@link #CLEAR_TABLES} in this list to tell the test framework to delete all
     * table contents before running the tests.
     * NOTE: Caller must close the returned EMF.
     *
     * @param props
     *            list of persistent types used in testing and/or configuration values in the form
     *            key,value,key,value...
     */
    protected OpenJPAEntityManagerFactorySPI createEMF(final Object... props) {
        return createNamedEMF(getPersistenceUnitName(), props);
    }

    /**
     * The name of the persistence unit that this test class should use by default. This defaults to "test".
     */
    protected String getPersistenceUnitName() {
        return "test";
    }

    /**
     * Create an entity manager factory for persistence unit <code>pu</code>. Put {@link #CLEAR_TABLES} in this list to
     * tell the test framework to delete all table contents before running the tests.
     * NOTE: Caller must close the returned EMF.
     *
     * @param props
     *            list of persistent types used in testing and/or configuration values in the form
     *            key,value,key,value...
     */
    protected OpenJPAEntityManagerFactorySPI createNamedEMF(final String pu, Object... props) {
        Map<String, Object> map = getPropertiesMap(props);
        Map<Object, Object> config = new HashMap<>(System.getProperties());
        config.putAll(map);
        EntityManagerFactory oemf = Persistence.createEntityManagerFactory(pu, config);
        if (oemf == null) {
            throw new NullPointerException("Expected an entity manager factory for the persistence unit named: \""
                + pu + "\"");
        }
        return (OpenJPAEntityManagerFactorySPI)oemf;
    }

    /**
     * Create an entity manager factory for persistence unit <code>pu</code>. Put {@link #CLEAR_TABLES} in this list to
     * tell the test framework to delete all table contents before running the tests.
     * NOTE: Caller must close the returned EMF.
     *
     * @param props
     *            list of persistent types used in testing and/or configuration values in the form
     *            key,value,key,value...
     */
    protected OpenJPAEntityManagerFactorySPI createNamedOpenJPAEMF(final String pu,
            String res, Map<String,Object> props) {
        OpenJPAEntityManagerFactorySPI oemf = null;
        Map<Object, Object> config = new HashMap<>(System.getProperties());
        if (props != null)
            config.putAll(props);
        oemf = (OpenJPAEntityManagerFactorySPI) OpenJPAPersistence.createEntityManagerFactory(pu, res, props);
        if (oemf == null) {
            throw new NullPointerException("Expected an OpenJPA entity manager factory " +
                "for the persistence unit named: \"" + pu + "\"");
        }
        return oemf;
    }

    protected Map<String, Object> getPropertiesMap(Object... props) {
        Map<String, Object> map = new HashMap<>();
        map.put("openjpa.DynamicEnhancementAgent", "false");
        List<Class<?>> types = new ArrayList<>();
        boolean prop = false;

        for (int i = 0; props != null && i < props.length; i++) {
            if (props[i] == FRESH_EMF) {
                _fresh = true;
                continue;
            }
            if (props[i] == RETAIN_DATA) {
                retainDataOnTearDown = true;
                continue;
            }
            if (prop) {
                map.put((String) props[i - 1], props[i]);
                prop = false;
            } else if (props[i] == CLEAR_TABLES) {
                map.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true,"
                    + "SchemaAction='add,deleteTableContents')");
            } else if (props[i] == DROP_TABLES) {
                map.put("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true,"
                    + "SchemaAction='drop,add')");
            } else if (props[i] instanceof Class<?>) {
                types.add((Class<?>) props[i]);
            }
            else if (props[i] instanceof Class<?>[]) {
                for(Class<?> clss : (Class<?>[]) props[i]) {
                    types.add(clss);
                }
            }
            else if (props[i] != null) {
                prop = true;
            }
        }

        if (!types.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            for (Class<?> c : types) {
                if (buf.length() > 0) {
                    buf.append(";");
                }
                buf.append(c.getName());
            }
            String oldValue =
                map.containsKey("openjpa.MetaDataFactory") ? "," + map.get("openjpa.MetaDataFactory").toString() : "";
            map.put("openjpa.MetaDataFactory", "jpa(Types=" + buf.toString() + oldValue + ")");
        } else {
            map.put("openjpa.MetaDataFactory", "jpa");
        }
        return map;
    }

    @BeforeEach
    void check(TestInfo info) {
        this.info = info;
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    /**
     * Safely close the given factory.
     */
    protected boolean closeEMF(EntityManagerFactory emf) {
        boolean brc;
        if (emf == null || !emf.isOpen()) {
            return false;
        }
        try {
            closeAllOpenEMs(emf);
        } finally {
            emf.close();
            brc = !emf.isOpen();
            if (!brc) {
                logger.warning("AbstractPersistenceTestCase().closeEMF() - EMF is still open.");
            }
        }
        return brc;
    }

    /**
     * Safely close the given EM
     *
     * @param em
     */
    protected boolean closeEM(EntityManager em) {
        if (em == null || !em.isOpen()) {
            return false;
        }
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        em.close();
        boolean brc = !em.isOpen();
        if (!brc) {
            logger.warning("AbstractPersistenceTestCase().closeEM() - EM is still open.");
        }
        return brc;
    }

    /**
     * Closes all open entity managers after first rolling back any open transactions.
     */
    protected void closeAllOpenEMs(EntityManagerFactory emf) {
        if (emf == null || !emf.isOpen()) {
            return;
        }

        for (Broker b : ((AbstractBrokerFactory) JPAFacadeHelper.toBrokerFactory(emf)).getOpenBrokers()) {
            if (b != null && !b.isClosed()) {
                EntityManager em = JPAFacadeHelper.toEntityManager(b);
                if( em.getTransaction().isActive() ) {
                    try {
                        em.getTransaction().rollback();
                    } catch (Exception e) {
                    }
                }
                closeEM(em);
            }
        }
    }

    /**
     * Delete all instances of the given types using bulk delete queries, but do not close any open entity managers.
     */
    protected void clear(EntityManagerFactory emf, Class<?>... types) {
        if (emf == null || types.length == 0) {
            return;
        }

        List<ClassMetaData> metas = new ArrayList<>(types.length);
        for (Class<?> c : types) {
            ClassMetaData meta = JPAFacadeHelper.getMetaData(emf, c);
            if (meta != null) {
                metas.add(meta);
            }
        }
        clear(emf, false, metas.toArray(new ClassMetaData[metas.size()]));
    }

    /**
     * Delete all instances of the persistent types registered with the given factory using bulk delete queries, after
     * first closing all open entity managers (and rolling back any open transactions).
     */
    protected void clear(EntityManagerFactory emf) {
        if (emf == null) {
            return;
        }
        clear(emf, true, ((OpenJPAEntityManagerFactorySPI) emf).getConfiguration().getMetaDataRepositoryInstance()
            .getMetaDatas());
    }

    /**
     * Delete all instances of the given types using bulk delete queries.
     *
     * @param emf
     *            The EntityManagerFactory to use. A new EntityManager will be created from this EMF and used to execute
     *            bulk updates.
     * @param closeEMs
     *            Whether any open EMs should be closed
     * @param types
     *            the types that will be cleared.
     */
    private void clear(EntityManagerFactory emf, boolean closeEMs, ClassMetaData... types) {
        if (emf == null || types.length == 0) {
            return;
        }

        // prevent deadlock by closing the open entity managers
        // and rolling back any open transactions
        // before issuing delete statements on a new entity manager.
        if (closeEMs) {
            closeAllOpenEMs(emf);
        }
        if (retainDataOnTearDown) {
            return;
        }
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            for (ClassMetaData meta : types) {
                if (!meta.isMapped() || meta.isEmbeddedOnly()
                    || Modifier.isAbstract(meta.getDescribedType().getModifiers())
                    && !isBaseManagedInterface(meta, types)) {
                    continue;
                }
                em.createQuery("DELETE FROM " + meta.getTypeAlias() + " o").executeUpdate();
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            // ignore
        } finally {
            closeEM(em);
        }
    }

    protected DBDictionary getDbDictionary(EntityManagerFactory emf) {
        return ((JDBCConfiguration)((OpenJPAEntityManagerFactory) emf).getConfiguration()).getDBDictionaryInstance();
    }


    /**
     * Return the entity name for the given type.
     */
    protected String entityName(EntityManagerFactory emf, Class<?> c) {
        ClassMetaData meta = JPAFacadeHelper.getMetaData(emf, c);
        return (meta == null) ? null : meta.getTypeAlias();
    }

    /**
     * Determines if the class associated with the provided {@link ClassMetaData} is a managed interface and does not
     * extend another managed interface.
     *
     * @param meta
     *            {@link ClassMetaData} for the class to examine
     * @param types
     *            array of class meta data for persistent types
     * @return true if the {@link ClassMetaData} is for an interface and the interface does not extend another managed
     *         interface
     */
    private boolean isBaseManagedInterface(ClassMetaData meta, ClassMetaData... types) {

        if (Modifier.isInterface(meta.getDescribedType().getModifiers()) && !isExtendedManagedInterface(meta, types)) {
            return true;
        }
        return false;
    }

    /**
     * Determines if the class associated with the provided {@link ClassMetaData} is an interface and if it extends
     * another managed interface.
     *
     * @param meta
     *            {@link ClassMetaData} for the class to examine
     * @param types
     *            array of class meta data for persistent types
     * @return true if the {@link ClassMetaData} is for an interface and the interface extends another managed interface
     */
    private boolean isExtendedManagedInterface(ClassMetaData meta, ClassMetaData... types) {

        if (!Modifier.isInterface(meta.getDescribedType().getModifiers())) {
            return false;
        }

        // Run through the interface this class extends. If any of them
        // are managed/have class metadata, return true.
        Class<?>[] ifaces = meta.getDescribedType().getInterfaces();
        for (int i = 0; ifaces != null && i < ifaces.length; i++) {
            for (ClassMetaData meta2 : types) {
                if (ifaces[i].equals(meta2.getDescribedType())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Round-trip a serializable object to bytes.
     */
    @SuppressWarnings("unchecked")
    public static <T> T roundtrip(T o) throws ClassNotFoundException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(o);
        out.flush();
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        return (T) in.readObject();
    }

    // ================================================
    // Utility methods for exception handling
    // ================================================
    /**
     * Asserts that the given targetType is assignable from given actual Throwable.
     */
    protected void assertException(final Throwable actual, Class<?> targetType) {
        assertException(actual, targetType, null);
    }

    /**
     * Asserts that the given targetType is assignable from given actual Throwable. Asserts that the nestedType is
     * nested (possibly recursively) within the given actual Throwable.
     *
     * @param actual
     *            is the actual throwable to be tested
     * @param targetType
     *            is expected type or super type of actual. If null, then the check is omitted.
     * @param nestedTargetType
     *            is expected type of exception nested within actual. If null this search is omitted.
     *
     */
    protected void assertException(final Throwable actual, Class<?> targetType, Class<?> nestedTargetType) {
        assertNotNull(actual);
        Class<?> actualType = actual.getClass();
        if (targetType != null && !targetType.isAssignableFrom(actualType)) {
            actual.printStackTrace();
            fail(targetType.getName() + " is not assignable from " + actualType.getName());
        }

        if (nestedTargetType != null) {
            Throwable nested = actual.getCause();
            Class<?> nestedActualType = (nested == null) ? null : nested.getClass();
            while (nestedActualType != null) {
                if (nestedTargetType.isAssignableFrom(nestedActualType)) {
                    return;
                } else {
                    Throwable next = nested.getCause();
                    if (next == null || next == nested) {
                        break;
                    }
                    nestedActualType = next.getClass();
                    nested = next;
                }
            }
            actual.printStackTrace();
            fail("No nested type " + nestedTargetType + " in " + actual);
        }
    }

    /**
     * Asserts that the given targetType is assignable from given actual Throwable and that the exception message
     * contains the specified message or message fragments.
     */
    protected void assertExceptionMessage(final Throwable actual, Class<?> targetType, String... messages) {
        assertException(actual, targetType, null);
        assertMessage(actual, messages);
    }

    /**
     * Assert that each of given keys are present in the message of the given Throwable.
     */
    protected void assertMessage(Throwable actual, String... keys) {
        if (actual == null || keys == null) {
            return;
        }
        String message = actual.getMessage();
        for (String key : keys) {
            assertTrue(message.contains(key), key + " is not in " + message);
        }
    }

    /**
     * Affirms if either this test has been annotated with @DatabasePlatform and at least one of the specified driver is
     * available in the classpath, or no such annotation is used.
     *
     */
    protected boolean isRunsOnCurrentPlatform() {
        DatabasePlatform anno = getClass().getAnnotation(DatabasePlatform.class);
        if (anno == null) {
            return true;
        }
        if (anno != null) {
            String value = anno.value();
            if (value == null || value.trim().length() == 0) {
                return true;
            }
            String[] drivers = value.split("\\,");
            for (String driver : drivers) {
                try {
                    Class.forName(driver.trim(), false, Thread.currentThread().getContextClassLoader());
                    return true;
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
        return false;
    }

    /**
     * Determines whether specified platform is the target database platform in use by the test framework.
     *
     * @param target
     *            platform name (derby, db2, oracle, etc.)
     * @return true if the specified platform matches the platform in use
     */
    public boolean isTargetPlatform(String target) {
        String url = getPlatform();
        return url != null && url.indexOf(target) != -1;
    }

    /**
     * Returns the platform in use by the test framework
     *
     * @return the database platform
     */
    public String getPlatform() {
        return System.getProperty("platform", "derby");
    }

    /**
     * Assert whether the Cache contains an instance of the specified class and id.
     *
     * @param cache
     *            The JPA Cache to verify
     * @param clss
     *            The Entity type
     * @param id
     *            ID of the entity
     * @param expected
     *            Whether the class should be found in the cache
     */
    protected void assertCached(Cache cache, Class<?> clss, Object id, boolean expected) {
        if (expected) {
            assertTrue(cache.contains(clss, id), String.format("Expected %s:%s to exist in cache", clss, id));
        } else {
            assertFalse(cache.contains(clss, id), String.format("Expected %s:%s not to exist in cache", clss, id));
        }
    }

    protected boolean isTestsDisabled() {
        return false;
    }

    protected boolean skipTest() {
        return false;
    }

    protected Class<?> resolveEntityClass(JPAEntityClassEnum enumerationRef)
        throws ClassNotFoundException
    {
        if (enumerationRef == null)
        {
            throw new IllegalArgumentException("Null value passed into the constructNewEntityObject method.");
        }
        String className = enumerationRef.getEntityClassName();
        if (className == null)
        {
            throw new IllegalArgumentException("Enumeration toString() method implementation returned a null value.");
        }

        return Class.forName(className);
    }

    protected Object constructNewEntityObject(JPAEntityClassEnum enumerationRef)
        throws ClassNotFoundException, SecurityException, NoSuchMethodException,
        IllegalArgumentException, InstantiationException,
        IllegalAccessException, InvocationTargetException
    {
        Class<?> classType = resolveEntityClass(enumerationRef);
        Class<?> constructorArgSig[] = new Class[] {};
        Object constructorArgs[] = new Object[] {};

        Constructor<?> classConstructor = classType.getConstructor(constructorArgSig);
        Object newEntity = classConstructor.newInstance(constructorArgs);

        return newEntity;
    }

    protected Object constructNewEntityObject(Class<?> entityClass)
        throws SecurityException, NoSuchMethodException,
        IllegalArgumentException, InstantiationException,
        IllegalAccessException, InvocationTargetException
    {
        Class<?> constructorArgSig[] = new Class[] {};
        Object constructorArgs[] = new Object[] {};

        Constructor<?> classConstructor = entityClass.getConstructor(constructorArgSig);
        Object newEntity = classConstructor.newInstance(constructorArgs);

        return newEntity;
    }

}
