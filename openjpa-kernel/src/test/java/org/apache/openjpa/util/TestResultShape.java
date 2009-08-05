package org.apache.openjpa.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.kernel.ResultShape;

import junit.framework.TestCase;
import static org.apache.openjpa.kernel.ResultShape.FillStrategy.*;

public class TestResultShape extends TestCase {

    public void testPrimitiveShapeIsImmutable() {
        ResultShape<Object> shape = new ResultShape<Object>(Object.class, true);
        assertCategory(shape, true, false, false);
        assertEquals(ASSIGN, shape.getStrategy());
        
        try {
            shape.add(int.class);
            fail(shape + " should not allow adding other shapes");
        } catch (UnsupportedOperationException e) {
        }
        try {
            shape.nest(Object[].class, ARRAY, int.class, double.class);
            fail(shape + " should not allow nesting other shapes");
        } catch (UnsupportedOperationException e) {
        }
    }
    
    public void testArrayIsMutable() {
        ResultShape<Object[]> shape = new ResultShape<Object[]>(Object[].class);
        assertCategory(shape, false, true, false);
        assertEquals(ARRAY, shape.getStrategy());
        
        shape.add(int.class, double.class); // will add primitive shapes
        assertCategory(shape, false, true, false);
        
        ResultShape<Object> primitiveShape = new ResultShape<Object>(Object.class, true);
        shape.nest(primitiveShape);
        assertCategory(shape, false, true, false);
        
        ResultShape<Object[]> nonPrimitiveShape = new ResultShape<Object[]>(Object[].class);
        nonPrimitiveShape.add(int.class, double.class);
        assertCategory(nonPrimitiveShape, false, true, false);
        shape.nest(nonPrimitiveShape);
        assertCategory(shape, false, true, true);
    }

    public void testMethodImpliesMapStrategy() {
        ResultShape<Map> mapShape = new ResultShape<Map>(Map.class, 
                method(Map.class, "put", Object.class, Object.class));
        assertCategory(mapShape, true, false, false);
        assertEquals(MAP, mapShape.getStrategy());
    }

    public void testShapeWithConstrcutorStrategy() {
        ResultShape<List> listShape = new ResultShape<List>(List.class, constructor(ArrayList.class, int.class));
        assertCategory(listShape, false, true, false);
        assertEquals(CONSTRUCTOR, listShape.getStrategy());
    }

    public void testGetCompositeTypes() {
        ResultShape<Object[]> root = new ResultShape<Object[]>(Object[].class);
        ResultShape<Bar> bar1 = new ResultShape<Bar>(Bar.class, CONSTRUCTOR, false);
        bar1.add(int.class);
        ResultShape<Foo> fooBarConstructor = new ResultShape<Foo>(Foo.class, 
                constructor(Foo.class, short.class, Bar.class));
        fooBarConstructor.add(short.class);
        fooBarConstructor.nest(bar1);
        root.add(Foo.class, Object.class);
        root.nest(fooBarConstructor);
        ResultShape<Bar> bar2 = new ResultShape<Bar>(Bar.class, CONSTRUCTOR, false);
        root.nest(bar2);
        assertEquals("Object[]{Foo, Object, Foo{short, Bar{int}}, Bar}", root.toString());
        assertEquals(Arrays.asList(Foo.class, Object.class, short.class, int.class, Bar.class), 
                root.getCompositeTypes());
        assertEquals(Arrays.asList(Foo.class, Object.class, Foo.class, Bar.class), root.getTypes());
        assertEquals(5, root.argLength());
        assertEquals(4, root.length());
    }

    public void testRecursiveNestingIsNotAllowed() {
        ResultShape<Object[]> root = new ResultShape<Object[]>(Object[].class);
        ResultShape<Bar> bar1 = new ResultShape<Bar>(Bar.class, CONSTRUCTOR, false);
        bar1.add(int.class);
        ResultShape<Foo> fooBarConstructor = new ResultShape<Foo>(Foo.class, 
                constructor(Foo.class, short.class, Bar.class));
        fooBarConstructor.add(short.class);
        fooBarConstructor.nest(bar1);
        root.add(Foo.class, Object.class);
        root.nest(fooBarConstructor);
        ResultShape<Bar> bar2 = new ResultShape<Bar>(Bar.class, CONSTRUCTOR, false);
        root.nest(bar2);
        
        try {
            bar1.nest(fooBarConstructor);
            fail("Expecetd recursive nesting error in nest " + fooBarConstructor + " in " + bar1);
        } catch (IllegalArgumentException e) {
            
        }
    }


    public void testFill() {
        //Fill this shape: Foo{short, Bar{String, Double}};
        ResultShape<Foo> foo = new ResultShape<Foo>(Foo.class, CONSTRUCTOR, false);
        ResultShape<Bar> bar = new ResultShape<Bar>(Bar.class, CONSTRUCTOR, false);
        bar.add(String.class, Double.class);
        foo.add(short.class);
        foo.nest(bar);
        assertEquals("Foo{short, Bar{String, Double}}", foo.toString());
        
        //from this array: 200s, "bar1", 12.3d)
        Object[] values = {(short)200, "bar1", 12.3d};
        Class[]  types  = {short.class, String.class, Double.class};
        String[]  aliases  = {"foo-short", "foo-bar-string", "foo-bar-Double"};
        Foo result = foo.pack(values, types, aliases);
        assertEquals(200, result.shrt);
        assertEquals("bar1", result.b.string);
        assertEquals(12.3, result.b.Dbl);
    }
    public void testFill2() {
        //Fill this shape: Object[]{Foo, Object, Foo{short, Bar{String, Double}}, Bar{double}};
        ResultShape<Object[]> root = new ResultShape<Object[]>(Object[].class);
        ResultShape<Bar> bar1 = new ResultShape<Bar>(Bar.class, CONSTRUCTOR, false);
        bar1.add(String.class, Double.class);
        ResultShape<Foo> fooBarConstructor = new ResultShape<Foo>(Foo.class, CONSTRUCTOR, false);
        fooBarConstructor.add(short.class);
        fooBarConstructor.nest(bar1);
        ResultShape<Bar> bar2 = new ResultShape<Bar>(Bar.class, CONSTRUCTOR, false);
        bar2.add(double.class);
        root.add(Foo.class, Object.class);
        root.nest(fooBarConstructor);
        root.nest(bar2);
        assertEquals("Object[]{Foo, Object, Foo{short, Bar{String, Double}}, Bar{double}}", root.toString());
        
        //from this array: new Foo(), new Object(), 200s, "bar1", 12.3d, 45.6d)
        Object[] values = {new Foo(), new Object(), 200, "bar1", 12.3d, 45.6d};
        Class[]  types  = {Foo.class, Object.class, short.class, String.class, Double.class, double.class};
        String[]  aliases  = {"Foo", "Object", "foo-short", "foo-bar-string", "foo-bar-Double", "bar-double"};
        Object[] result = root.pack(values, types, aliases);
        
        assertEquals(4, result.length);
        assertEquals(Foo.class, result[0].getClass());
        assertEquals(Object.class, result[1].getClass());
        assertEquals(Foo.class, result[2].getClass());
        assertEquals(Bar.class, result[3].getClass());
        assertEquals(200, ((Foo)result[2]).shrt);
        assertEquals("bar1", ((Foo)result[2]).b.string);
        assertEquals(12.3, ((Foo)result[2]).b.Dbl);
        assertEquals(45.6, ((Bar)result[3]).dbl);
    }

    void assertCategory(ResultShape<?> s, boolean primitive, boolean compound, boolean nesting) {
        if (primitive)
            assertTrue(s + " is not primitive", s.isPrimitive());
        else
            assertFalse(s + " is primitive", s.isPrimitive());
        if (compound)
            assertTrue(s + " is not compound", s.isCompound());
        else 
            assertFalse(s + " is compound", s.isCompound());
        if (nesting)
            assertTrue(s + " is not nesting", s.isNesting());
        else 
            assertFalse(s + " is nesting", s.isNesting());
    }
    
    void arrayEquals(Object[] a, Object[] b) {
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(i+"-th element not equal", a[i], b[i]);
        }
    }
    
    <T> Constructor<T> constructor(Class<T> t, Class<?>...args) {
        try {
            return t.getConstructor(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    <T> Method method(Class<T> t, String name, Class<?>...args) {
        try {
            return t.getMethod(name, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class Foo {
        private String string;
        private int i;
        private short shrt;
        private Bar b;
        public Foo() {}
        public Foo(String s, int i) {this.string = s; this.i = i;}
        public Foo(short s, Bar b){this.shrt = s; this.b = b;}
        public String toString() {
            return "Foo(string='"+string+"' i="+i+" short="+shrt+" bar="+b+"";}
    }
    
    public static class Bar {
        private String string;
        private Double Dbl;
        private double dbl;
        public Bar() {}
        public Bar(double d) {this.dbl = d;}
        public Bar(String s, Double i) {this.string = s; this.Dbl = i;}
        public String toString() {return "Bar(string='"+string+"' Dbl="+Dbl+" dbl="+dbl+"";}
    }
}
