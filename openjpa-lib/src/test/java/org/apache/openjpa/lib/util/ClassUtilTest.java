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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ClassUtilTest {
	
	private static final Logger logger = Logger.getLogger(ClassUtilTest.class.getCanonicalName());

    @Test
    void testToClass() {
        assertEquals(this.getClass(), toClass("org.apache.openjpa.lib.util.ClassUtilTest"));

        assertEquals(new ClassUtilTest[0].getClass(), toClass("org.apache.openjpa.lib.util.ClassUtilTest[]"));

        assertEquals(Integer.class, toClass("java.lang.Integer"));

        assertEquals(byte.class, toClass("byte"));
        assertEquals(char.class, toClass("char"));
        assertEquals(double.class, toClass("double"));
        assertEquals(float.class, toClass("float"));
        assertEquals(int.class, toClass("int"));
        assertEquals(long.class, toClass("long"));
        assertEquals(short.class, toClass("short"));
        assertEquals(boolean.class, toClass("boolean"));
        assertEquals(void.class, toClass("void"));

        assertEquals(new float[0].getClass(), toClass("float[]"));
        assertEquals(new float[0][0].getClass(), toClass("float[][]"));
        assertEquals(new long[0][0][0].getClass(), toClass("long[][][]"));
    }

    @Test
    void nonExistingClass() {
        assertThrows(IllegalArgumentException.class, () ->
            toClass("does.not.Exist"));
    }

    private Class<?> toClass(String clazz) {
        return ClassUtil.toClass(clazz, false, this.getClass().getClassLoader());
    }

    @Test
    void getClassName() {
        assertEquals("ClassUtilTest", ClassUtil.getClassName(ClassUtilTest.class));
        assertEquals("ClassUtilTest$MyInnerClass", ClassUtil.getClassName(MyInnerClass.class));

        // anonymous class
        assertEquals("ClassUtilTest$1", ClassUtil.getClassName(INSTANCE.getClass()));

        // primitives
        assertEquals("byte", ClassUtil.getClassName(byte.class));
        assertEquals("char", ClassUtil.getClassName(char.class));
        assertEquals("double", ClassUtil.getClassName(double.class));
        assertEquals("float", ClassUtil.getClassName(float.class));
        assertEquals("int", ClassUtil.getClassName(int.class));
        assertEquals("long", ClassUtil.getClassName(long.class));
        assertEquals("short", ClassUtil.getClassName(short.class));
        assertEquals("boolean", ClassUtil.getClassName(boolean.class));
        assertEquals("void", ClassUtil.getClassName(void.class));

        // arrays
        assertEquals("long[]", ClassUtil.getClassName(long[].class));
        assertEquals("long[][]", ClassUtil.getClassName(long[][].class));
        assertEquals("float[][][]", ClassUtil.getClassName(float[][][].class));

        assertEquals("ClassUtilTest[]", ClassUtil.getClassName(ClassUtilTest[].class));
        assertEquals("ClassUtilTest$MyInnerClass[]", ClassUtil.getClassName(MyInnerClass[].class));
        assertEquals("ClassUtilTest$MyInnerClass[][]", ClassUtil.getClassName(MyInnerClass[][].class));
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

    @Test
    void getPackageName() {
        assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(ClassUtilTest.class));
        assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(MyInnerClass.class));
        assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(MyInnerClass[].class));
        assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(INSTANCE.getClass()));
        assertEquals("", ClassUtil.getPackageName(long.class));
        assertEquals("", ClassUtil.getPackageName(long[].class));
    }

    private static abstract class MyInnerClass {
        // not needed
    }

    private static final MyInnerClass INSTANCE = new MyInnerClass() {
    };

}
