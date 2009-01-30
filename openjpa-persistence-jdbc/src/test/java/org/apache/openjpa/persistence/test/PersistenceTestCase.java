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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.JPAFacadeHelper;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;

/**
 * Base test class providing persistence utilities.
 * 
 * EntityManagerFactory created by this receiver is statically cached indexed
 * by a key which is a combination of the persistence unit name and the user
 * configuration. 
 * If a extended test does not want cached EntityManagerFactory
 * then it must specify FRESH_EMF as one of the arguments of #setUp(Object[]).
 * 
 * 
 */
public abstract class PersistenceTestCase
    extends TestCase {
    private static FixedMap _emfs = new FixedMap();
    public static final String FRESH_EMF = "Creates new EntityManagerFactory";
    /**
     * Marker object you pass to {@link #setUp} to indicate that the
     * database table rows should be cleared.
     */
    protected static final Object CLEAR_TABLES = new Object();

    /**
     * Marker object you pass to {@link #setUp} to indicate that the
     * database table should be dropped and then recreated.
     */
    protected static final Object DROP_TABLES = new Object();

    /**
     * The {@link TestResult} instance for the current test run.
     */
    protected TestResult testResult;

    /**
     * Create an entity manager factory. Put {@link #CLEAR_TABLES} in
     * this list to tell the test framework to delete all table contents
     * before running the tests.
     *
     * @param props list of persistent types used in testing and/or
     * configuration values in the form key,value,key,value...
     */
    protected OpenJPAEntityManagerFactorySPI createEMF(Object... props) {
        return createNamedEMF(getPersistenceUnitName(), props);
    }

    /**
     * The name of the persistence unit that this test class should use
     * by default. This defaults to "test".
     */
    protected String getPersistenceUnitName() {
        return "test";
    }

    /**
     * Create an entity manager factory for persistence unit <code>pu</code>.
     * Put {@link #CLEAR_TABLES} in
     * this list to tell the test framework to delete all table contents
     * before running the tests.
     *
     * @param props list of persistent types used in testing and/or
     * configuration values in the form key,value,key,value...
     */
    protected OpenJPAEntityManagerFactorySPI createNamedEMF(String pu,
        Object... props) {
        Map map = new HashMap();//System.getProperties());
        List<Class> types = new ArrayList<Class>();
        boolean prop = false;
        boolean fresh = false;
        for (int i = 0; i < props.length; i++) {
            if (props[i] == FRESH_EMF) {
                fresh = true;
                continue;
            }
            if (prop) {
                map.put(props[i - 1], props[i]);
                prop = false;
            } else if (props[i] == CLEAR_TABLES) {
                map.put("openjpa.jdbc.SynchronizeMappings",
                    "buildSchema(ForeignKeys=true," 
                    + "SchemaAction='add,deleteTableContents')");
            } else if (props[i] == DROP_TABLES) {
                map.put("openjpa.jdbc.SynchronizeMappings",
                    "buildSchema(ForeignKeys=true," 
                    + "SchemaAction='drop,add')");
            } else if (props[i] instanceof Class)
                types.add((Class) props[i]);
            else if (props[i] != null)
                prop = true;
        }

        if (!types.isEmpty()) {
            StringBuffer buf = new StringBuffer();
            for (Class c : types) {
                if (buf.length() > 0)
                    buf.append(";");
                buf.append(c.getName());
            }
            String oldValue = map.containsKey("openjpa.MetaDataFactory") 
                ? ","+map.get("openjpa.MetaDataFactory").toString() : "";
            map.put("openjpa.MetaDataFactory",
                "jpa(Types=" + buf.toString() + oldValue + ")");
        }
        EMFKey key = new EMFKey(pu, map);
        OpenJPAEntityManagerFactorySPI oemf = _emfs.get(key);
        if (fresh || oemf == null) {
            Map config = new HashMap(System.getProperties());
            config.putAll(map);
            oemf = (OpenJPAEntityManagerFactorySPI) 
                Persistence.createEntityManagerFactory(pu, config);
            if (oemf == null) {
                throw new NullPointerException(
                    "Expected an entity manager factory " +
                    "for the persistence unit named: \"" + pu + "\"");
            } else if (!fresh) {
                _emfs.put(key, oemf);
            }
        }
        return oemf;
    }

    @Override
    public void run(TestResult testResult) {
        this.testResult = testResult;
        super.run(testResult);
    }

    @Override
    public void tearDown() throws Exception {
        try {
            super.tearDown();
        } catch (Exception e) {
            // if a test failed, swallow any exceptions that happen
            // during tear-down, as these just mask the original problem.
            if (testResult.wasSuccessful())
                throw e;
        }
    }

    /**
     * Safely close the given factory.
     */
    protected boolean closeEMF(EntityManagerFactory emf) {
        if (emf == null || !emf.isOpen())
            return false;
        
        closeAllOpenEMs(emf);
        emf.close();
        return !emf.isOpen();
    }

    /**
     * Closes all open entity managers after first rolling back any open 
     * transactions.
     */
    protected void closeAllOpenEMs(EntityManagerFactory emf) {
        if (emf == null || !emf.isOpen())
            return;

        for (Iterator iter = ((AbstractBrokerFactory) JPAFacadeHelper
            .toBrokerFactory(emf)).getOpenBrokers().iterator();
            iter.hasNext(); ) {
            Broker b = (Broker) iter.next();
            if (b != null && !b.isClosed()) {
                EntityManager em = JPAFacadeHelper.toEntityManager(b);
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
                em.close();
            }
        }
    }

    /**
     * Delete all instances of the given types using bulk delete queries,
     * but do not close any open entity managers.
     */
    protected void clear(EntityManagerFactory emf, Class... types) {
        if (emf == null || types.length == 0)
            return;

        List<ClassMetaData> metas = new ArrayList<ClassMetaData>(types.length);
        for (Class c : types) {
            ClassMetaData meta = JPAFacadeHelper.getMetaData(emf, c);
            if (meta != null)
                metas.add(meta);
        }
        clear(emf, false, metas.toArray(new ClassMetaData[metas.size()]));
    }

    /**
     * Delete all instances of the persistent types registered with the given
     * factory using bulk delete queries, after first closing all open entity
     * managers (and rolling back any open transactions).
     */
    protected void clear(EntityManagerFactory emf) {
        if (emf == null)
            return;
        clear(emf, true, ((OpenJPAEntityManagerFactorySPI) emf)
        		.getConfiguration()
        		.getMetaDataRepositoryInstance().getMetaDatas());
    }

    /**
     * Delete all instances of the given types using bulk delete queries.
     * @param closeEMs TODO
     */
    private void clear(EntityManagerFactory emf, boolean closeEMs, 
    		ClassMetaData... types) {
        if (emf == null || types.length == 0)
            return;
        
        // prevent deadlock by closing the open entity managers 
        // and rolling back any open transactions 
        // before issuing delete statements on a new entity manager.
        if (closeEMs)
            closeAllOpenEMs(emf);

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (ClassMetaData meta : types) {
            if (!meta.isMapped() || meta.isEmbeddedOnly() 
                || Modifier.isAbstract(meta.getDescribedType().getModifiers())
                    && !isBaseManagedInterface(meta, types))
                continue;
            em.createQuery("DELETE FROM " + meta.getTypeAlias() + " o").
                executeUpdate();
        }
        em.getTransaction().commit();
        em.close();
    }

    /**
     * Return the entity name for the given type.   
     */
    protected String entityName(EntityManagerFactory emf, Class c) {
        ClassMetaData meta = JPAFacadeHelper.getMetaData(emf, c);
        return (meta == null) ? null : meta.getTypeAlias();
    }
    
    
    /**
     * Determines if the class assocated with the provided metadata is
     * a managed interface and does not extend another managed interface.
     * @param meta class metadata for the class to examine
     * @param types array of class meta data for persistent types
     * @return true if the cmd is for an interface and the interface does not
     * extend another managed interface
     */
    private boolean isBaseManagedInterface(ClassMetaData meta, 
        ClassMetaData... types) {
        
        if (Modifier.isInterface(meta.getDescribedType().getModifiers()) &&
            !isExtendedManagedInterface(meta, types))
            return true;
        return false;
    }

    /**
     * Determines if the class assocated with the provided metadata is
     * an interface and if it extends another managed interface. 
     * @param meta class metadata for the class to examine
     * @param types array of class meta data for persistent types
     * @return true if the cmd is for an interface and the interface extends
     * another managed interface
     */
    private boolean isExtendedManagedInterface(ClassMetaData meta, 
        ClassMetaData... types) {
        
        if (!Modifier.isInterface(meta.getDescribedType().getModifiers()))
            return false;

        // Run through the interface this class extends.  If any of them
        // are managed/have class metadata, return true.
        Class[] ifaces = meta.getDescribedType().getInterfaces();
        for (int i = 0; ifaces != null && i < ifaces.length; i++) {
            for (ClassMetaData meta2 : types) {
                if (ifaces[i].equals(meta2.getDescribedType()))
                    return true;
            }            
        }
        return false;
    }

    public static void assertNotEquals(Object o1, Object o2) {
        if (o1 == o2)
            fail("expected args to be different; were the same instance.");
        else if (o1 == null || o2 == null)
            return;
        else if (o1.equals(o2))
            fail("expected args to be different; compared equal.");
    }

    /**
     * Round-trip a serializable object to bytes.
     */
    public static Object roundtrip(Object o) 
        throws ClassNotFoundException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(o);
        out.flush();
        ObjectInputStream in = new ObjectInputStream(
            new ByteArrayInputStream(bytes.toByteArray()));
        return in.readObject();
    }
    
    // ================================================ 
    // Utility methods for exception handling
    // ================================================
    /**
	 * Asserts that the given targetType is assignable from given actual 
	 * Throwable.
	 */
    protected void assertException(final Throwable actual, Class targetType) {
		assertException(actual, targetType, null);
	}
	
	/**
	 * Asserts that the given targetType is assignable from given actual 
	 * Throwable. Asserts that the nestedType is nested (possibly recursively) 
	 * within the given actual Throwable.
	 * 
	 * @param actual is the actual throwable to be tested
	 * @param targetType is expected type or super type of actual. If null, then
	 * the check is omitted.
	 * @param nestedTargetType is expected type of exception nested within
	 * actual. If null this search is omitted. 
	 * 
	 */
    protected void assertException(final Throwable actual, Class targetType,
			Class nestedTargetType) {
		assertNotNull(actual);
		Class actualType = actual.getClass();
		if (targetType != null && !targetType.isAssignableFrom(actualType)) {
			actual.printStackTrace();
			fail(targetType.getName() + " is not assignable from "
					+ actualType.getName());
		}

		if (nestedTargetType != null) {
			Throwable nested = actual.getCause();
			Class nestedActualType = (nested == null) ? null : nested.getClass();
			while (nestedActualType != null) {
				if (nestedTargetType.isAssignableFrom(nestedActualType)) {
					return;
				} else {
					Throwable next = nested.getCause();
					if (next == null || next == nested)
						break;
					nestedActualType = next.getClass();
					nested     = next;
				}
			}
			actual.printStackTrace();
			fail("No nested type " + nestedTargetType + " in " + actual);
		}
	}

	/**
	 * Assert that each of given keys are present in the message of the given
	 * Throwable.
	 */
    protected void assertMessage(Throwable actual, String... keys) {
		if (actual == null || keys == null)
			return;
		String message = actual.getMessage();
		for (String key : keys) {
			assertTrue(key + " is not in " + message, message.contains(key));
		}
	}
    
    public void printException(Throwable t) {
    	printException(t, 2);
    }
    
    public void printException(Throwable t, int tab) {
		if (t == null) return;
		for (int i=0; i<tab*4;i++) System.out.print(" ");
		String sqlState = (t instanceof SQLException) ? 
			"(SQLState=" + ((SQLException)t).getSQLState() + ":" 
				+ t.getMessage() + ")" : "";
		System.out.println(t.getClass().getName() + sqlState);
		if (t.getCause() == t) 
			return;
		printException(t.getCause(), tab+2);
	}
    
    /**
     * Overrides to allow tests annotated with @AllowFailure to fail. 
     * If the test is in error then the normal pathway is executed.
     */
    @Override
    public void runBare() throws Throwable {
        try {
            super.runBare();
        } catch (Throwable t) {
            if (allowFailure()) {
                System.err.println("*** FAILED (but ignored):" + this);
                System.err.println("Stacktrace of failure");
                t.printStackTrace();
            } else {
                throw t;
            }
        }
    }
    
    /**
     * Affirms if the test case or the test method is annotated with 
     * @AllowFailure. Method level annotation has higher precedence than Class
     * level annotation.
     */
    protected boolean allowFailure() {
		try {
            Method runMethod = getClass().getMethod(getName(), (Class[])null);
            AllowFailure anno = runMethod.getAnnotation(AllowFailure.class);
	    	if (anno != null)
	    		return anno.value();
		} catch (SecurityException e) {
			//ignore
		} catch (NoSuchMethodException e) {
			//ignore
		}
		AllowFailure anno = getClass().getAnnotation(AllowFailure.class);
    	if (anno != null) 
            return anno.value();
    	return false;
    }
    
    private static class FixedMap extends LinkedHashMap<EMFKey, OpenJPAEntityManagerFactorySPI> {
        public boolean removeEldestEntry(Map.Entry<EMFKey, OpenJPAEntityManagerFactorySPI> entry) {
            return this.size() > 2;
        }
    }
    
    private static class EMFKey {
        final String unit;
        final Map config;
        EMFKey(String unit, Map config) {
            this.unit = unit;
            this.config = config;
        }
        
        public int hashCode() {
            return unit.hashCode() + config.hashCode();
        }
        
        public boolean equals(Object other) {
            EMFKey that = (EMFKey)other;
            return unit.equals(that.unit) && config.equals(that.config);
        }
    }
}
