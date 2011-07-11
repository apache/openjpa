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

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;

/**
 * Base class for OpenJPA-specific Test Case.
 * Provides utilities for configuration setup and persistent entity 
 * registration during setUp() method.
 * Derived classes can access protected EntityManagerFactory to create 
 * EntityManager. The protected EntityManagerFactory is declared to be
 * OpenJPA-extended SPI interface <code>OpenJPAEntityManagerFactorySPI</code>
 * so that derived classes can access internal mapping/metadata/configuration
 * and other structures.  
 *   
 */
public abstract class SingleEMFTestCase
    extends PersistenceTestCase {

    protected OpenJPAEntityManagerFactorySPI emf;

    /**
     * Call {@link #setUp(Object...)} with no arguments so that the emf
     * set-up happens even if <code>setUp()</code> is not called from the
     * subclass.
     */
    public void setUp() throws Exception {
        setUp(new Object[0]);
    }

    /**
     * Initialize entity manager factory. Put {@link #CLEAR_TABLES} in
     * this list to tell the test framework to delete all table contents
     * before running the tests.
     *
     * @param props list of persistent types used in testing and/or 
     * configuration values in the form key,value,key,value...
     */
    protected void setUp(Object... props) {
        emf = createEMF(props);
    }

    /**
     * Closes the entity manager factory.
     */
    public void tearDown() throws Exception {
        super.tearDown();

        if (emf == null)
            return;

        try {
            clear(emf);
        } catch (Exception e) {
            // if a test failed, swallow any exceptions that happen
            // during tear-down, as these just mask the original problem.
            if (testResult.wasSuccessful())
                throw e;
        } finally {
            closeEMF(emf);
        }
    }
    
    /**
     * Get the class mapping for a given entity
     * 
     * @param name The Entity's name.
     * 
     * @return If the entity is a known type the ClassMapping for the Entity
     *         will be returned. Otherwise null
     */
    protected ClassMapping getMapping(String name) {
        return (ClassMapping) emf.getConfiguration()
                .getMetaDataRepositoryInstance().getMetaData(name,
                        getClass().getClassLoader(), true);
    }
    
    /**
     * Get the class mapping for a given entity
     * 
     * @param entityClass an entity class.
     * 
     * @return If the entity is a known type the ClassMapping for the Entity
     *         will be returned. Otherwise null
     */
    protected ClassMapping getMapping(Class<?> entityClass) {
        return (ClassMapping) emf.getConfiguration()
                .getMetaDataRepositoryInstance().getMetaData(entityClass,
                        getClass().getClassLoader(), true);
    }
    
    /**
     * Get number of instances by an aggregate query with the given alias.
     */
    public int count(String alias) {
    	return ((Number)emf.createEntityManager()
    					   .createQuery("SELECT COUNT(p) FROM " + alias + " p")
    					   .getSingleResult()).intValue();
    }
    
    /**
     * Count number of instances of the given class assuming that the alias
     * for the class is its simple name.
     */
    public int count(Class c) {
    	return count(c.getSimpleName());
    }

    protected Log getLog() {
        return emf.getConfiguration().getLog("Tests");
    }
    
    /**
     * Get all the instances of given type.
     * The returned instances are obtained without a persistence context. 
     */
    public <T> List<T> getAll(Class<T> t) {
    	String alias = t.getSimpleName();
    	return (List<T>)emf.createEntityManager()
				   .createQuery("SELECT p FROM " + alias + " p")
				   .getResultList();
    }
    
    /**
     * Get all the instances of given type.
     * The returned instances are obtained within the given persistence context. 
     */
    public <T> List<T> getAll(EntityManager em, Class<T> t) {
    	String alias = t.getSimpleName();
    	return (List<T>)em.createQuery("SELECT p FROM " + alias + " p")
				   .getResultList();
    }
}
