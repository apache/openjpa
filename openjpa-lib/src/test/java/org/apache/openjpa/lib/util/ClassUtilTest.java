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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ClassUtilTest {
	
	private static final Logger logger = Logger.getLogger(ClassUtilTest.class.getCanonicalName());

    @Test
    public void testToClass() {
        Assertions.assertEquals(this.getClass(), toClass("org.apache.openjpa.lib.util.ClassUtilTest"));

        Assertions.assertEquals(new ClassUtilTest[0].getClass(), toClass("org.apache.openjpa.lib.util.ClassUtilTest[]"));

        Assertions.assertEquals(Integer.class, toClass("java.lang.Integer"));

        Assertions.assertEquals(byte.class, toClass("byte"));
        Assertions.assertEquals(char.class, toClass("char"));
        Assertions.assertEquals(double.class, toClass("double"));
        Assertions.assertEquals(float.class, toClass("float"));
        Assertions.assertEquals(int.class, toClass("int"));
        Assertions.assertEquals(long.class, toClass("long"));
        Assertions.assertEquals(short.class, toClass("short"));
        Assertions.assertEquals(boolean.class, toClass("boolean"));
        Assertions.assertEquals(void.class, toClass("void"));

        Assertions.assertEquals(new float[0].getClass(), toClass("float[]"));
        Assertions.assertEquals(new float[0][0].getClass(), toClass("float[][]"));
        Assertions.assertEquals(new long[0][0][0].getClass(), toClass("long[][][]"));
    }

    @Test
    public void nonExistingClass() {
        assertThrows(IllegalArgumentException.class, () ->
            toClass("does.not.Exist"));
    }

    private Class<?> toClass(String clazz) {
        return ClassUtil.toClass(clazz, false, this.getClass().getClassLoader());
    }

    @Test
    public void testGetClassName() {
        Assertions.assertEquals("ClassUtilTest", ClassUtil.getClassName(ClassUtilTest.class));
        Assertions.assertEquals("ClassUtilTest$MyInnerClass", ClassUtil.getClassName(MyInnerClass.class));

        // anonymous class
        Assertions.assertEquals("ClassUtilTest$1", ClassUtil.getClassName(INSTANCE.getClass()));

        // primitives
        Assertions.assertEquals("byte", ClassUtil.getClassName(byte.class));
        Assertions.assertEquals("char", ClassUtil.getClassName(char.class));
        Assertions.assertEquals("double", ClassUtil.getClassName(double.class));
        Assertions.assertEquals("float", ClassUtil.getClassName(float.class));
        Assertions.assertEquals("int", ClassUtil.getClassName(int.class));
        Assertions.assertEquals("long", ClassUtil.getClassName(long.class));
        Assertions.assertEquals("short", ClassUtil.getClassName(short.class));
        Assertions.assertEquals("boolean", ClassUtil.getClassName(boolean.class));
        Assertions.assertEquals("void", ClassUtil.getClassName(void.class));

        // arrays
        Assertions.assertEquals("long[]", ClassUtil.getClassName(long[].class));
        Assertions.assertEquals("long[][]", ClassUtil.getClassName(long[][].class));
        Assertions.assertEquals("float[][][]", ClassUtil.getClassName(float[][][].class));

        Assertions.assertEquals("ClassUtilTest[]", ClassUtil.getClassName(ClassUtilTest[].class));
        Assertions.assertEquals("ClassUtilTest$MyInnerClass[]", ClassUtil.getClassName(MyInnerClass[].class));
        Assertions.assertEquals("ClassUtilTest$MyInnerClass[][]", ClassUtil.getClassName(MyInnerClass[][].class));
    }

    @Test
    @Disabled("only needed for manual performance tests")
    public void testGetClassNamePerformance() {

        long start = System.nanoTime();
        for (int i = 1; i < 10000000; i++) {
            //X String className = Strings.getClassName(MyInnerClass.class);
            ClassUtil.getClassName(MyInnerClass.class);
        }

        long stop = System.nanoTime();
        logger.fine("took: " + TimeUnit.NANOSECONDS.toMillis(stop - start));
    }

    @Test
    public void testGetPackageName() {
        Assertions.assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(ClassUtilTest.class));
        Assertions.assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(MyInnerClass.class));
        Assertions.assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(MyInnerClass[].class));
        Assertions.assertEquals("org.apache.openjpa.lib.util", ClassUtil.getPackageName(INSTANCE.getClass()));
        Assertions.assertEquals("", ClassUtil.getPackageName(long.class));
        Assertions.assertEquals("", ClassUtil.getPackageName(long[].class));
    }

    private static abstract class MyInnerClass {
        // not needed
    }

    private static final MyInnerClass INSTANCE = new MyInnerClass() {
    };

}
