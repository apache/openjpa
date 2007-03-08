/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.persistence.test;

import java.util.*;

import javax.persistence.*;

import junit.framework.TestCase;
import junit.textui.TestRunner;
import org.apache.openjpa.persistence.*;

/**
 * A base test case that can be used to easily test scenarios where there
 * is only a single EntityManager at any given time.
 *
 * @author Marc Prud'hommeaux
 */
public abstract class SingleEMTest extends TestCase {

    protected EntityManagerFactory emf;
    protected EntityManager em;
    protected Class[] classes;

    public SingleEMTest(Class... classes) {
        this.classes = classes;
    }

    /** 
     * Can be overridden to return a list of classes that will be used
     * for this test.
     */
    protected Class[] getClasses() { 
        return classes;
    }

    /** 
     * Modify the properties that are used to create the EntityManagerFactory.
     * By default, this will set up the MetaDataFactory with the
     * persistent classes for this test case. This method can be overridden
     * to add more properties to the map.
     */
    protected void setEMFProps(Map props) {
        // if we have specified a list of persistent classes to examine,
        // then set it in the MetaDataFactory so that our automatic
        // schema generation will work.
        Class[] pclasses = getClasses();
        if (pclasses != null) {
            StringBuilder str = new StringBuilder();
            for (Class c : pclasses)
                str.append(str.length() > 0 ? ";" : "").append(c.getName());

            props.put("openjpa.MetaDataFactory", "jpa(Types=" + str + ")");
        }

        if (clearDatabaseInSetUp()) {
            props.put("openjpa.jdbc.SynchronizeMappings",
                "buildSchema(ForeignKeys=true," +
                    "SchemaAction='add,deleteTableContents')");
        }
    }

    protected boolean clearDatabaseInSetUp() {
        return false;
    }

    public void setUp() throws Exception {
        Map props = new HashMap(System.getProperties());
        setEMFProps(props);
        emf = Persistence.createEntityManagerFactory("test", props);
    }

    /** 
     * Rolls back the current transaction and closes the EntityManager. 
     */
    public void tearDown() throws Exception {
        rollback();
        close();
        closeEMF();
    }

    public EntityManagerFactory emf() {
        return emf;
    }

    /** 
     * Returns the current EntityManager, creating one from the
     * EntityManagerFactory if it doesn't already exist. 
     */
    public EntityManager em() {
        if (em == null) {
            em = emf().createEntityManager();
        }

        return em;
    }

    /** 
     * Start a new transaction if there isn't currently one active. 
     * @return  true if a transaction was started, false if one already existed
     */
    public boolean begin() {
        EntityTransaction tx = em().getTransaction();
        if (tx.isActive())
            return false;

        tx.begin();
        return true;
    }

    /** 
     * Commit the current transaction, if it is active. 
     * @return true if the transaction was committed
     */
    public boolean commit() {
        EntityTransaction tx = em().getTransaction();
        if (!tx.isActive())
            return false;

        tx.commit();
        return true;
    }

    /** 
     * Rollback the current transaction, if it is active. 
     * @return true if the transaction was rolled back
     */
    public boolean rollback() {
        EntityTransaction tx = em().getTransaction();
        if (!tx.isActive())
            return false;

        tx.commit();
        return true;
    }

    /** 
     * Closes the current EntityManager if it is open. 
     * @return false if the EntityManager was already closed.
     */
    public boolean close() {
        if (em == null)
            return false;

        rollback();

        if (!em.isOpen())
            return false;

        em.close();
        return !em.isOpen();
    }

    public boolean closeEMF() {
        if (emf == null)
            return false;

        close();

        if (!emf.isOpen())
            return false;

        emf.close();
        return !emf.isOpen();
    }

    /** 
     * Returns the entity name of the specified class. If the class
     * declares an @Entity, then it will be used, otherwise the base
     * name of the class will be returned.
     *
     * Note that this will not correctly return the entity name of
     * a class declared in an orm.xml file.
     */
    public String entityName(Class c) {
        Entity e = (Entity) c.getAnnotation(Entity.class);
        if (e != null && e.name() != null && e.name().length() > 0)
            return e.name();

        String name = c.getSimpleName();
        name = name.substring(name.lastIndexOf(".") + 1);
        return name;
    }

    /** 
     * Delete all of the instances.
     *
     * If no transaction is running, then one will be started and committed.
     * Otherwise, the operation will take place in the current transaction.
     */
    public void remove(Object... obs) {
        boolean tx = begin();
        for (Object ob : obs)
            em().remove(ob);
        if (tx) commit();
    }

    /** 
     * Persist all of the instances.
     *
     * If no transaction is running, then one will be started and committed.
     * Otherwise, the operation will take place in the current transaction.
     */
    public void persist(Object... obs) {
        boolean tx = begin();
        for (Object ob : obs)
            em().persist(ob);
        if (tx) commit();
    }

    /** 
     * Creates a query in the current EntityManager with the specified string. 
     */
    public Query query(String str) {
        return em().createQuery(str);
    }

    /** 
     * Create a query against the specified class, which will be aliased
     * as "x". For example, query(Person.class, "where x.age = 21") will
     * create the query "select x from Person x where x.age = 21".
     *  
     * @param  c  the class to query against
     * @param  str  the query suffix
     * @param  params  the parameters, if any
     * @return the Query object
     */
    public Query query(Class c, String str, Object... params) {
        String query = "select x from " + entityName(c) + " x "
            + (str == null ? "" : str);
        Query q = em().createQuery(query);
        for (int i = 0; params != null && i < params.length; i++)
            q.setParameter(i + 1, params[i]);
        return q;
    }

    /** 
     * Returns a list of all instances of the specific class in the database. 
     *
     * @param c the class to find
     * @param q the query string suffix to use
     * @param params the positional parameter list value
     *
     * @see #query(java.lang.Class,java.lang.String)
     */
    public <E> List<E> find(Class<E> c, String q, Object... params) {
        return Collections.checkedList(query(c, q, params).getResultList(), c);
    }

    public <E> List<E> find(Class<E> c) {
        return find(c, null);
    }

    /** 
     * Deletes all instances of the specific class from the database. 
     *
     * If no transaction is running, then one will be started and committed.
     * Otherwise, the operation will take place in the current transaction.
     *
     * @return the total number of instanes deleted
     */
    public int delete(Class... classes) {
        boolean tx = begin();
        int total = 0;
        for (Class c : classes) {
            total += query("delete from " + entityName(c) + " x").
                executeUpdate();
        }
        if (tx) commit();

        return total;
    }
}

