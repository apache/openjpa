package org.apache.openjpa.persistence.delimited.identifiers.noschema;

import java.util.List;

import javax.persistence.Query;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.MySQLDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
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
import org.apache.openjpa.persistence.delimited.identifiers.Animal;
import org.apache.openjpa.persistence.delimited.identifiers.Cat;
import org.apache.openjpa.persistence.delimited.identifiers.Dog;
import org.apache.openjpa.persistence.test.DatabasePlatform;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;

@DatabasePlatform(value="")
public class TestNoSchemaManualDelimInheritance extends SQLListenerTestCase {
    OpenJPAEntityManager em;
    int id = 0;
    Dog dog;
    Cat cat;
    JDBCConfiguration conf;
    DBDictionary dict;
    
    @Override
    public void setUp() throws Exception {
        
        // TODO: Delimiter support is currently limited to database that use
        // double quote as a delimiter.
        setUnsupportedDatabases(MySQLDictionary.class);
        if (isTestsDisabled())
            return;

        super.setUp(
            org.apache.openjpa.persistence.delimited.identifiers.Animal.class,
            org.apache.openjpa.persistence.delimited.identifiers.Dog.class,
            org.apache.openjpa.persistence.delimited.identifiers.Cat.class,
            DROP_TABLES);
        assertNotNull(emf);
        
        em = emf.createEntityManager();
        assertNotNull(em);
        
        conf = (JDBCConfiguration) emf.getConfiguration();
        dict = conf.getDBDictionaryInstance();
    }
    
    private void createDog(int id) {
        dog = new Dog(id);
        dog.setName("Spot");
        dog.setType("dog");
    }
    
    private void createCat(int id) {
        cat = new Cat(id);
        cat.setName("Puff");
        cat.setType("cat");
    }
    
    public void testCreate() {
        id++;
        createDog(id);
        id++;
        createCat(id);
        
        em.getTransaction().begin();
        em.persist(dog);
        em.persist(cat);
        em.getTransaction().commit();
        
        runQueries();
    }
    
    private void runQueries() {
        em.clear();
        queryCat();
        em.clear();
        queryDog();
    }
    
    private void queryCat() {
        String query =
            "SELECT DISTINCT a " +
            "FROM Animal a " +
            "WHERE a.type = 'cat'";
        Query q = em.createQuery(query);
        List<Animal> results = (List<Animal>)q.getResultList();
        assertEquals(1,results.size());
    }
    
    // Use native query
    private void queryDog() {
        String query = 
            "SELECT * " +
            "FROM \"Animal\" a " +
            "WHERE a.\"discr col\" = 'Dog'";
        Query q = em.createNativeQuery(query);
        List<Animal> results = (List<Animal>)q.getResultList();
        assertEquals(1,results.size());
    }
}
