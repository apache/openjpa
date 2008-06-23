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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.TestCase;
import org.apache.openjpa.kernel.AbstractBrokerFactory;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactory;
import org.apache.openjpa.persistence.OpenJPAPersistence;

/**
 * Base test class providing persistence utilities.
 */
public abstract class PersistenceTestCase
    extends TestCase {

    /**
     * Marker object you an pass to {@link #setUp} to indicate that the
     * database tables should be cleared.
     */
    protected static final Object CLEAR_TABLES = new Object();

    /**
     * Create an entity manager factory. Put {@link #CLEAR_TABLES} in
     * this list to tell the test framework to delete all table contents
     * before running the tests.
     *
     * @param props list of persistent types used in testing and/or 
     * configuration values in the form key,value,key,value...
     */
    protected OpenJPAEntityManagerFactory createEMF(Object... props) {
        Map map = new HashMap(System.getProperties());
        List<Class> types = new ArrayList<Class>();
        boolean prop = false;
        for (int i = 0; i < props.length; i++) {
            if (prop) {
                map.put(props[i - 1], props[i]);
                prop = false;
            } else if (props[i] == CLEAR_TABLES) {
                map.put("openjpa.jdbc.SynchronizeMappings",
                    "buildSchema(ForeignKeys=true," 
                    + "SchemaAction='add,deleteTableContents')");
            } else if (props[i] instanceof Class)
                types.add((Class) props[i]);
            else
                prop = true;
        }

        if (!types.isEmpty()) {
            StringBuffer buf = new StringBuffer();
            for (Class c : types) {
                if (buf.length() > 0)
                    buf.append(";");
                buf.append(c.getName());
            }
            map.put("openjpa.MetaDataFactory",
                "jpa(Types=" + buf.toString() + ")");
        }

        return (OpenJPAEntityManagerFactory) Persistence.
            createEntityManagerFactory("test", map);
    }

    /**
     * Safely close the given factory.
     */
    protected boolean closeEMF(EntityManagerFactory emf) {
        if (emf == null)
            return false;
        if (!emf.isOpen())
            return false;

        for (Iterator iter = ((AbstractBrokerFactory) OpenJPAPersistence
            .toBrokerFactory(emf)).getOpenBrokers().iterator();
            iter.hasNext(); ) {
            Broker b = (Broker) iter.next();
            if (b != null && !b.isClosed()) {
                EntityManager em = OpenJPAPersistence.toEntityManager(b);
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
                em.close();
            }
        }

        emf.close();
        return !emf.isOpen();
    }

    /**
     * Delete all instances of the given types using bulk delete queries.
     */
    protected void clear(EntityManagerFactory emf, Class... types) {
        if (emf == null || types.length == 0)
            return;

        List<ClassMetaData> metas = new ArrayList<ClassMetaData>(types.length);
        for (Class c : types) {
            ClassMetaData meta = OpenJPAPersistence.getMetaData(emf, c);
            if (meta != null)
                metas.add(meta);
        }
        clear(emf, metas.toArray(new ClassMetaData[metas.size()]));
    }

    /**
     * Delete all instances of the persistent types registered with the given
     * factory using bulk delete queries.
     */
    protected void clear(EntityManagerFactory emf) {
        if (emf == null)
            return;
        clear(emf, ((OpenJPAEntityManagerFactory) emf).getConfiguration().
            getMetaDataRepositoryInstance().getMetaDatas());
    }

    /**
     * Delete all instances of the given types using bulk delete queries.
     */
    private void clear(EntityManagerFactory emf, ClassMetaData... types) {
        if (emf == null || types.length == 0)
            return;

        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        for (ClassMetaData meta : types) {
            if (!meta.isMapped() || meta.isEmbeddedOnly() 
                || Modifier.isAbstract(meta.getDescribedType().getModifiers()))
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
        ClassMetaData meta = OpenJPAPersistence.getMetaData(emf, c);
        return (meta == null) ? null : meta.getTypeAlias();
    }
}
