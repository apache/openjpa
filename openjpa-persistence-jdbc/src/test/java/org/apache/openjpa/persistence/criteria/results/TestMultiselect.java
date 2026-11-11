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

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Array;
import java.util.List;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import org.apache.openjpa.conf.Compatibility;
import org.apache.openjpa.persistence.criteria.CriteriaTest;
import org.apache.openjpa.persistence.criteria.Person;
import org.apache.openjpa.persistence.criteria.Person_;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Test variations of {@link CriteriaQuery#multiselect(java.util.List)} arguments.
 *
 * @author Pinaki Poddar
 *
 */

class TestMultiselect extends CriteriaTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        clearData();
        createData();
    }

    void clearData() {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Foo f").executeUpdate();
        em.createQuery("DELETE FROM Bar b").executeUpdate();
        em.createQuery("DELETE FROM Person p").executeUpdate();
        em.getTransaction().commit();
    }

    void createData() {
        em.getTransaction().begin();
        Person p = new Person("Test Result Shape");
        em.persist(p);

        Foo foo = new Foo(100L, "Test Foo");
        Bar bar = new Bar(200L, "Test Bar");
        foo.setBar(bar);
        em.persist(foo);
        em.persist(bar);
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

    @Test
    void tupleQuery() {
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name), p.get(Person_.id));

        assertResult(q, Tuple.class, String.class, Integer.class);
    }

    @Test
    void tupleQueryExplicit() {
        CriteriaQuery<Tuple> q = cb.createQuery(Tuple.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name), p.get(Person_.id));

        assertResult(q, Tuple.class, String.class, Integer.class);
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
    @Test
    void userResultQueryWithExplictProjectionOfConstructorArguments() {
        CriteriaQuery<Person> q = cb.createQuery(Person.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name));

        assertResult(q, Person.class);
    }

    @Test
    void userResultQueryWithImplicitProjection() {
        CriteriaQuery<Person> q = cb.createQuery(Person.class);
        q.from(Person.class);

        assertResult(q, Person.class);
    }

    @Test
    void userResultQueryWithMismatchProjectionOfConstructorArguments() {
        CriteriaQuery<Person> q = cb.createQuery(Person.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name), p.get(Person_.id));

        fail("Person has no constrcutor with (name,id)", q);
    }

    @Test
    void multipleConstructorWithAliasRepeatedAndInOrderingClause() {
        // SELECT NEW(p.name), p.id, NEW(p.name), p.name FROM Person p ORDER BY p.name
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Person> p = q.from(Person.class);
        q.multiselect(
            cb.construct(Person.class, p.get(Person_.name)),
            p.get(Person_.id),
            cb.construct(Person.class, p.get(Person_.name)),
            p.get(Person_.name))
            .orderBy(cb.asc(p.get(Person_.name)));

        List<Tuple> tuples = em.createQuery(q).getResultList();
        assertFalse(tuples.isEmpty());
        for (Tuple row : tuples) {
            assertEquals(4, row.getElements().size());

            assertEquals(Person.class,  row.get(0).getClass());
            assertEquals(Integer.class, row.get(1).getClass());
            assertEquals(Person.class,  row.get(2).getClass());
            assertEquals(String.class,  row.get(3).getClass());

            assertEquals(((Person)row.get(0)).getName(), ((Person)row.get(2)).getName());
            assertEquals(((Person)row.get(0)).getName(), row.get(3));
        }
    }

    // ======================================================================
    /**
     * If the type of the criteria query is CriteriaQuery<X[]> for
     * some class X, an instance of type X[] will be returned for
     * each row.   The elements of the array will correspond to the
     * elements of the list passed to the multiselect method.
     */

    @Test
    void userClassArray() {
        CriteriaQuery<Person[]> q = cb.createQuery(Person[].class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p,p);

        assertResult(q, Person[].class, Person.class, Person.class);
    }

    @Test
    void basicClassArray() {
        CriteriaQuery<String[]> q = cb.createQuery(String[].class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name), p.get(Person_.name));

        assertResult(q, String[].class);
    }

    @Test
    void tupleArray() {
        CriteriaQuery<Tuple[]> q = cb.createQuery(Tuple[].class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name), p.get(Person_.id), p.get(Person_.name));

        assertResult(q, Tuple[].class, String.class, Integer.class, String.class);
    }

// =================================================================
    /**
     * If the type of the criteria query is CriteriaQuery<Object>
     * or if the criteria query was created without specifying a
     * type, and the list passed to the multiselect method contains
     * only a single element, an instance of type Object will be
     * returned for each row.
     */
    @Test
    void singleObject() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p);

        assertResult(q, Object.class);
    }

    @Test
    void singleObjectViaConstructor() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(cb.construct(Person.class, p.get(Person_.name)));

        assertResult(q, Object.class);
    }

    @Test
    void singleObjectAsProperty() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name));

        assertResult(q, Object.class);
    }

    @Test
    void singleObjectImplicit() {
        CriteriaQuery<?> q = cb.createQuery();
        Root<Person> p = q.from(Person.class);
        q.multiselect(p);

        assertResult(q, Object.class);
    }

    @Test
    void singleObjectViaConstructorImplicit() {
        CriteriaQuery<?> q = cb.createQuery();
        Root<Person> p = q.from(Person.class);
        q.multiselect(cb.construct(Person.class, p.get(Person_.name)));

        assertResult(q, Object.class);
    }

    @Test
    void singleObjectAsPropertyImplicit() {
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
    @Test
    void singleObjectMultipleProjections() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(p.get(Person_.name), p.get(Person_.id));

        assertResult(q, Object[].class, String.class, Integer.class);
    }

    @Test
    void singleObjectMultipleProjectionsAndConstructor() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Person> p = q.from(Person.class);
        q.multiselect(cb.construct(Person.class, p.get(Person_.name)), p.get(Person_.id), p.get(Person_.name));

        assertResult(q, Object[].class, Person.class, Integer.class, String.class);
    }

    /**
     * JPA 3.2 Section 6.9.1: An element of the list passed to the multiselect method
     * must not be a tuple- or array-valued compound selection item.
     * Verify that nesting tuples throws IllegalArgumentException.
     */
    @Test
    void tupleCanNotBeNested() {
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Person> p = q.from(Person.class);

        CompoundSelection<Tuple> tuple1 = cb.tuple(p.get(Person_.name), p.get(Person_.id));
        CompoundSelection<Tuple> tuple2 = cb.tuple(p.get(Person_.id), p.get(Person_.name));

        try {
            cb.tuple(tuple1, tuple2);
            Assertions.fail("Expected IllegalArgumentException when nesting tuple compound selections");
        } catch (IllegalArgumentException e) {
            // expected per JPA 3.2 spec
        }
    }

    @Test
    void multiConstructor() {
        // SELECT NEW Foo(f.fid,f.fint), b, NEW FooBar(f.fid, b.bid) from Foo f JOIN f.bar b WHERE f.b=b
        CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
        Root<Foo> f = q.from(Foo.class);
        Join<Foo, Bar> b = f.join(Foo_.bar);
        q.multiselect(cb.construct(Foo.class, f.get(Foo_.fid), f.get(Foo_.fstring)),
                      b,
                      cb.construct(FooBar.class, f.get(Foo_.fid), b.get(Bar_.bid)));
        q.where(cb.equal(f.get(Foo_.fid), 100L));

        List<Object[]> result = em.createQuery(q).getResultList();
        assertFalse(result.isEmpty());
        for (Object[] row : result) {
            assertEquals(3, row.length);
            assertInstanceOf(Foo.class, row[0], "0-th element " + row[0].getClass() + " is not Foo");
            assertInstanceOf(Bar.class, row[1], "1-st element " + row[1].getClass() + " is not Bar");
            assertInstanceOf(FooBar.class, row[2], "2-nd element " + row[2].getClass() + " is not FooBar");
        }
    }

    @Test
    void selectSingleTermWithMultiselectObjectArray() {
        CriteriaQuery<Object[]> q = cb.createQuery(Object[].class);
        Root<Foo> f = q.from(Foo.class);
        q.multiselect(f);

        assertResult(q, Object[].class, Foo.class);
    }

    @Test
    void selectSingleTermWithMultiselectObject() {
        CriteriaQuery<Object> q = cb.createQuery(Object.class);
        Root<Foo> f = q.from(Foo.class);
        q.multiselect(f);

        assertResult(q, Foo.class);
    }

    @Test
    void selectSingleTermWithMultiselectTuple() {
        CriteriaQuery<Tuple> q = cb.createQuery(Tuple.class);
        Root<Foo> f = q.from(Foo.class);
        q.multiselect(f);

        assertResult(q, Tuple.class, Foo.class);
    }

    @Test
    void selectSingleTermWithMultiselectTupleArray() {
        CriteriaQuery<Tuple[]> q = cb.createQuery(Tuple[].class);
        Root<Foo> f = q.from(Foo.class);
        q.multiselect(f);

        assertResult(q, Tuple[].class, Foo.class);
    }

    @Test
    void sanity() {
        CriteriaQuery<Foo> q = cb.createQuery(Foo.class);
        Root<Foo> f = q.from(Foo.class);

        assertResult(q, Foo.class);
    }

    @Test
    void sanity2() {
        CriteriaQuery<Foo> q = cb.createQuery(Foo.class);
        Root<Foo> f = q.from(Foo.class);
        q.select(f);
        assertResult(q, Foo.class);
    }

    /**
     * JPA 3.2 Section 6.9.1: compound selections (tuple, array) must not
     * contain other compound selections. Nesting cb.tuple() inside cb.array()
     * must throw IllegalArgumentException.
     */
    @Test
    void deeplyNestedShape() {
        CriteriaQuery<Tuple> q = cb.createQuery(Tuple.class);
        Root<Foo> foo = q.from(Foo.class);

        try {
            cb.array(foo.get(Foo_.fint), cb.tuple(foo.get(Foo_.fstring)));
            Assertions.fail("Expected IllegalArgumentException when nesting tuple inside array");
        } catch (IllegalArgumentException e) {
            // expected per JPA 3.2 spec - compound selections cannot contain
            // other compound selections
        }
    }

    /**
     * With the compatibility option <code>AllowNestedCompoundSelection</code> enabled,
     * OpenJPA's historic extension supporting arbitrarily nested tuple and array selections
     * is available again.
     */
    @Test
    void deeplyNestedShapeWithCompatibilityOption() {
        Compatibility compat = emf.getConfiguration().getCompatibilityInstance();
        boolean allowed = compat.getAllowNestedCompoundSelection();
        compat.setAllowNestedCompoundSelection(true);
        try {
            CriteriaQuery<Tuple> q = cb.createQuery(Tuple.class);
            Root<Foo> foo = q.from(Foo.class);
            q.multiselect(cb.construct(Foo.class, foo.get(Foo_.flong), foo.get(Foo_.fstring)),
                     cb.tuple(foo, cb.array(foo.get(Foo_.fint), cb.tuple(foo.get(Foo_.fstring)))));
            List<Tuple> result = em.createQuery(q).getResultList();
            assertFalse(result.isEmpty());
            Tuple tuple = result.get(0);

            assertEquals(Foo.class,   tuple.get(0).getClass());
            assertTrue(Tuple.class.isAssignableFrom(tuple.get(1).getClass()));
            Tuple tuple2 = (Tuple)tuple.get(1);
            assertEquals(Foo.class,   tuple2.get(0).getClass());
            assertEquals(Object[].class, tuple2.get(1).getClass());
            Object[] level3 = (Object[])tuple2.get(1);
            assertEquals(Integer.class, level3[0].getClass());
            assertTrue(Tuple.class.isAssignableFrom(level3[1].getClass()));
            Tuple tuple4 = (Tuple)level3[1];
            assertEquals(String.class, tuple4.get(0).getClass());
        } finally {
            compat.setAllowNestedCompoundSelection(allowed);
        }
    }

    @Test
    void constructorFailsFast() {
        CriteriaQuery<Tuple> q = cb.createQuery(Tuple.class);
        Root<Foo> foo = q.from(Foo.class);
        try {
            q.multiselect(cb.construct(Foo.class, foo.get(Foo_.flong)));
            Assertions.fail("Expected IllegalArgumentException becuase Foo(long) is not a valid constructor");
        } catch (IllegalArgumentException e) {
            // good -- but print the error message to check it is informative enough
            e.printStackTrace();
        }
        try {
            q.multiselect(cb.construct(Foo.class));
            Assertions.fail("Expected IllegalArgumentException becuase Foo() is not a valid constructor");
        } catch (IllegalArgumentException e) {
            // good -- but print the error message to check it is informative enough
            e.printStackTrace();
        }

    }

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
            assertTrue(resultClass.isInstance(row),
                toClass(row) + " does not match actual result " + toString(resultClass));
            if (resultClass.isArray() && arrayElementClasses != null) {
                for (int i = 0; i < arrayElementClasses.length; i++) {
                    Object element = Array.get(row, i);
                    if (Tuple.class.isInstance(element)) {
                        assertEquals(arrayElementClasses[i], Tuple.class.cast(element).get(0).getClass());
                    } else {
                    assertTrue(arrayElementClasses[i].isInstance(element), i + "-th array element " + toString(arrayElementClasses[i]) +
                       " does not match actual result " + toClass(element));
                    }
                }
            }
            if (resultClass == Tuple.class && arrayElementClasses != null) {
                Tuple tuple = (Tuple)row;
                for (int i = 0; i < arrayElementClasses.length; i++) {
                    Object element = tuple.get(i);
                    assertTrue(arrayElementClasses[i].isInstance(element), i + "-th tuple element " + toString(arrayElementClasses[i]) +
                       " does not match actual result " + toClass(element));
                }
            }
        }
    }


    void fail(String msg, CriteriaQuery<?> q) {
        try {
            em.createQuery(q).getResultList();
            Assertions.fail("Expected to fail " + msg);
        } catch (Exception e) {
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
