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
package org.apache.openjpa.persistence.criteria.results;

import java.lang.reflect.Array;
import java.util.List;

import javax.persistence.PersistenceException;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.openjpa.persistence.criteria.CriteriaTest;
import org.apache.openjpa.persistence.criteria.Person;
import org.apache.openjpa.persistence.criteria.Person_;
import org.apache.openjpa.persistence.test.AllowFailure;
import org.apache.openjpa.util.UserException;


/**
 * Test variations of {@link CriteriaQuery#multiselect(java.util.List)} arguments.
 * 
 * @author Pinaki Poddar
 *
 */

@AllowFailure(value=false, message="Tests:16 Errors:1 Failure:1")
public class TestMultiselect extends CriteriaTest {
    private static boolean initialized = false;
    
    @Override
    public void setUp() {
        super.setUp();
        if (!initialized) {
            createData();
            initialized = true;
        }
    }
    
    void createData() {
        em.getTransaction().begin();
        Person p = new Person("Test Result Shape");
        em.persist(p);
        em.getTransaction().commit();
    }
    
    /**
    * If the type of the criteria query is CriteriaQuery<Tuple>
    * (i.e., a criteria query object created by either the 
    * createTupleQuery method or by passing a Tuple class argument 
    * to the createQuery method), a Tuple object corresponding to 
    * the elements of the list passed to the multiselect method 
    * will be instantiated and returned for each row that results 
    * from the query execution.
    * */
    
    public void testTupleQuery() {
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Person> p = q.from(Person.class); 
        q.multiselect(p.get(Person_.name), p.get(Person_.id));
        
        assertResult(q, Tuple.class);
    }
    
    public void testTupleQueryExplicit() {
        CriteriaQuery<Tuple> q = cb.createQuery(Tuple.class);
        Root<Person> p = q.from(Person.class); 
        q.multiselect(p.get(Person_.name), p.get(Person_.id));
        
        assertResult(q, Tuple.class);
    }
  
    //=======================================================================
    
    /**
     * If the type of the criteria query is CriteriaQuery<X> for
     * some user-defined class X (i.e., a criteria query object
     * created by passing a X class argument to the createQuery 
     * method), then the elements of the list passed to the
     * multiselect method will be passed to the X constructor and 
     * an instance of type X will be returned for each row.  
     */
    public void testUserResultQueryWithExplictProjectionOfConstructorArguments() {
        CriteriaQuery<Person> q = cb.createQuery(Person.class);
        Root<Person> p = q.from(Person.class); 
        q.multiselect(p.get(Person_.name));
        
        assertResult(q, Person.class);
    }
    
    public void testUserResultQueryWithImplicitProjection() {
        CriteriaQuery<Person> q = cb.createQuery(Person.class);
        Root<Person> p = q.from(Person.class); 
        
        assertResult(q, Person.class);
    }

    public void testUserResultQueryWithMismatchProjectionOfConstructorArguments() {
        CriteriaQuery<Person> q = cb.createQuery(Person.class);
        Root<Person> p = q.from(Person.class); 
        q.multiselect(p.get(Person_.name), p.get(Person_.id));
        
        fail("Person has no constrcutor with (name,id)", q);
    }
    
    // ======================================================================
    /**
     * If the type of the criteria query is CriteriaQuery<X[]> for
     * some class X, an instance of type X[] will be returned for 
     * each row.   The elements of the array will correspond to the 
     * elements of the list passed to the multiselect method.  
     */
    
    public void testUserClassArray() {
        CriteriaQuery<Person[]> q = cb.createQuery(Person[].class);
        Root<Person> p = q.from(Person.class); 
        q.multiselect(p,p);
        
        assertResult(q, Person[].class, Person.class, Person.class);
    }
    
    public void testBasicClassArray() {
        CriteriaQuery<String[]> q = cb.createQuery(String[].class);
        Root<Person> p = q.from(Person.class); 
        q.multiselect(p.get(Person_.name), p.get(Person_.name));
        
        assertResult(q, String[].class);
    }
    
    @AllowFailure(message="TupleArray needs special processing at multiselect")
    public void testTupleArray() {
        CriteriaQuery<Tuple[]> q = cb.createQuery(Tuple[].class);
        Root<Person> p = q.from(Person.class); 
        q.multiselect(p.get(Person_.name), p.get(Person_.name));
        
        assertResult(q, Tuple[].class);
    }
// =================================================================    
    /**
     * If the type of the criteria query is CriteriaQuery<Object>
     * or if the criteria query was created without specifying a 
     * type, and the list passed to the multiselect method contains 
     * only a single element, an instance of type Object will be 
     * returned for each row.
     */
    public void testSingleObject() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p);
        
        assertResult(q, Object.class);
    }
    
    public void testSingleObjectViaConstructor() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(cb.construct(Person.class, p.get(Person_.name)));
        
        assertResult(q, Object.class);
    }
    
    public void testSingleObjectAsProperty() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name));
        
        assertResult(q, Object.class);
    }
    
    public void testSingleObjectImplicit() {
        CriteriaQuery<?> q = cb.createQuery();
        Root<Person> p = q.from(Person.class);
        q.multiselect(p);
        
        assertResult(q, Object.class);
    }
    
    public void testSingleObjectViaConstructorImplicit() {
        CriteriaQuery<?> q = cb.createQuery();
        Root<Person> p = q.from(Person.class);
        q.multiselect(cb.construct(Person.class, p.get(Person_.name)));
        
        assertResult(q, Object.class);
    }
    
    public void testSingleObjectAsPropertyImplicit() {
        CriteriaQuery<?> q = cb.createQuery();
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name));
        
        assertResult(q, Object.class);
    }

// ================================================================================
    /**
     * If the type of the criteria query is CriteriaQuery<Object>
     * or if the criteria query was created without specifying a 
     * type, and the list passed to the multiselect method contains 
     * more than one element, an instance of type Object[] will be 
     * instantiated and returned for each row.  The elements of the 
     * array will correspond to the elements of the list passed to
     * the multiselect method.
     */
    public void testSingleObjectMultipleProjections() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name), p.get(Person_.id));
        
        assertResult(q, Object[].class, String.class, Integer.class);
    }
    
    @AllowFailure(message="Mixing constructor with other projections get CriteriaExpressionBuilder.getProjections() " +
            "all messed up")
    public void testSingleObjectMultipleProjectionsAndConstructor() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(cb.construct(Person.class, p.get(Person_.name)), p.get(Person_.id), p.get(Person_.name));
        
        assertResult(q, Object[].class, Person.class, Integer.class, String.class);
    }
    
    /**
     * An element of the list passed to the multiselect method 
     * must not be a tuple- or array-valued compound selection item. 
     */
    
    
// =============== assertions by result types ========================
    
    void assertResult(CriteriaQuery<?> q, Class<?> resultClass) {
        assertResult(q, resultClass, (Class<?>[])null);
    }
    /**
     * Assert the query result elements by their types 
     */
    void assertResult(CriteriaQuery<?> q, Class<?> resultClass, Class<?>... arrayElementClasses) {
        List<?> result = em.createQuery(q).getResultList();
        assertFalse(result.isEmpty());
        for (Object row : result) {
            assertTrue(toClass(row) + " does not match actual result " + toString(resultClass), 
                resultClass.isInstance(row));
            if (resultClass.isArray() && arrayElementClasses != null) {
                for (int i = 0; i < arrayElementClasses.length; i++) {
                    Object element = Array.get(row, i);
                    assertTrue(i + "-th array element " + toString(arrayElementClasses[i]) + 
                       " does not match actual result " + toClass(element), arrayElementClasses[i].isInstance(element));
                }
            }
        }
    }
    
    void fail(String msg, CriteriaQuery<?> q) {
        try {
            em.createQuery(q).getResultList();
            fail("Expected to fail " + msg);
        } catch (UserException e) {
            // this is an expected exception
        }
    }
    
    String toClass(Object o) {
       return toString(o.getClass());
    }
    
    String toString(Class<?> cls) {
        return cls.isArray() ? toString(cls.getComponentType())+"[]" : cls.toString();
    }
}
