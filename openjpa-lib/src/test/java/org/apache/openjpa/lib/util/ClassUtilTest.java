/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ClassUtilTest {

	private static final Logger logger = Logger.getLogger(ClassUtilTest.class.getCanonicalName());

    @ParameterizedTest(name = "#{index} toClass(\"{0}\") = {1}")
    @MethodSource("toClassArguments")
    void testToClass(String name, Class<?> expected) {
        assertEquals(expected, toClass(name));
    }

    private static Stream<Arguments> toClassArguments() {
        return Stream.of(
            Arguments.of("org.apache.openjpa.lib.util.ClassUtilTest", ClassUtilTest.class),
            Arguments.of("org.apache.openjpa.lib.util.ClassUtilTest[]", ClassUtilTest[].class),
            Arguments.of("java.lang.Integer", Integer.class),
            Arguments.of("byte", byte.class),
            Arguments.of("char", char.class),
            Arguments.of("double", double.class),
            Arguments.of("float", float.class),
            Arguments.of("int", int.class),
            Arguments.of("long", long.class),
            Arguments.of("short", short.class),
            Arguments.of("boolean", boolean.class),
            Arguments.of("void", void.class),
            Arguments.of("float[]", float[].class),
            Arguments.of("float[][]", float[][].class),
            Arguments.of("long[][][]", long[][][].class)
        );
    }

    @Test
    void nonExistingClass() {
        assertThrows(IllegalArgumentException.class, () -> toClass("does.not.Exist"));
    }

    private Class<?> toClass(String clazz) {
        return ClassUtil.toClass(clazz, false, this.getClass().getClassLoader());
    }

    @ParameterizedTest(name = "#{index} getClassName({0}) = \"{1}\"")
    @MethodSource("getClassNameArguments")
    void getClassName(Class<?> clazz, String expected) {
        assertEquals(expected, ClassUtil.getClassName(clazz));
    }

    private static Stream<Arguments> getClassNameArguments() {
        return Stream.of(
            // this test class and its inner classes
            Arguments.of(ClassUtilTest.class, "ClassUtilTest"),
            Arguments.of(MyInnerClass.class, "ClassUtilTest$MyInnerClass"),
            // anonymous class
            Arguments.of(INSTANCE.getClass(), "ClassUtilTest$1"),
            // primitives
            Arguments.of(byte.class, "byte"),
            Arguments.of(char.class, "char"),
            Arguments.of(double.class, "double"),
            Arguments.of(float.class, "float"),
            Arguments.of(int.class, "int"),
            Arguments.of(long.class, "long"),
            Arguments.of(short.class, "short"),
            Arguments.of(boolean.class, "boolean"),
            Arguments.of(void.class, "void"),
            // arrays
            Arguments.of(long[].class, "long[]"),
            Arguments.of(long[][].class, "long[][]"),
            Arguments.of(float[][][].class, "float[][][]"),
            Arguments.of(ClassUtilTest[].class, "ClassUtilTest[]"),
            Arguments.of(MyInnerClass[].class, "ClassUtilTest$MyInnerClass[]"),
            Arguments.of(MyInnerClass[][].class, "ClassUtilTest$MyInnerClass[][]")
        );
    }

    @Test
    @Disabled("only needed for manual performance tests")
    void getClassNamePerformance() {

        long start = System.nanoTime();
        for (int i = 1; i < 10000000; i++) {
            //X String className = Strings.getClassName(MyInnerClass.class);
            ClassUtil.getClassName(MyInnerClass.class);
        }

        long stop = System.nanoTime();
        logger.fine("took: " + TimeUnit.NANOSECONDS.toMillis(stop - start));
    }

    @ParameterizedTest(name = "#{index} getPackageName({0}) = \"{1}\"")
    @MethodSource("getPackageNameArguments")
    void getPackageName(Class<?> clazz, String expected) {
        assertEquals(expected, ClassUtil.getPackageName(clazz));
    }

    private static Stream<Arguments> getPackageNameArguments() {
        return Stream.of(
            Arguments.of(ClassUtilTest.class, "org.apache.openjpa.lib.util"),
            Arguments.of(MyInnerClass.class, "org.apache.openjpa.lib.util"),
            Arguments.of(MyInnerClass[].class, "org.apache.openjpa.lib.util"),
            Arguments.of(INSTANCE.getClass(), "org.apache.openjpa.lib.util"),
            Arguments.of(long.class, ""),
            Arguments.of(long[].class, "")
        );
    }

    private static abstract class MyInnerClass {
        // not needed
    }

    private static final MyInnerClass INSTANCE = new MyInnerClass() {
    };

}
