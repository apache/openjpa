package org.apache.openjpa.lib.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestClassUtilToClass {
    private final String classStr;

    private final Object result;

    private final ClassLoader loader;

    private final Boolean resolve;

    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters() {
        return Arrays.asList(new Object[][]{
                { "int", false, int.class.getClassLoader(), int.class },
                { "int[]", true, int.class.getClassLoader(), int.class },
                { "NotExistingClass", true, TestClass1.class.getClassLoader(), new IllegalArgumentException("NotExistingClass")},
                { "org.apache.openjpa.lib.util.TestClass1", false, TestClassUtilToClass.class.getClassLoader(), TestClass1.class },
                { null, false, null, new NullPointerException("str == null") },
        });
    }

    public TestClassUtilToClass (String classStr, Boolean resolve, ClassLoader loader, Object result){
        this.classStr = classStr;
        this.resolve = resolve;
        this.loader = loader;
        this.result = result;
    }

    @Test
    public void getClassNameTest() {
        try {
            Class<?> classObj = ClassUtil.toClass(classStr, resolve, loader);
            Assert.assertEquals(result, classObj);
        }
        catch(Exception e) {
            Assert.assertEquals(e.toString() ,result.toString());
        }
    }
}
