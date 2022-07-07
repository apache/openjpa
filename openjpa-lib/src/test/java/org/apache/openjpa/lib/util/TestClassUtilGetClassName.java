package org.apache.openjpa.lib.util;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestClassUtilGetClassName {
    private final Class<?> classObj;
    private final Object result;

    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters() {
        return Arrays.asList(new Object[][]{
                { TestClass1.class, "TestClass1" },
                { int.class, "int" },
                { null, null },
                { "test".getClass(), "String" },
        });
    }

    public TestClassUtilGetClassName(Class<?> classObj, Object result) {
        this.classObj = classObj;
        this.result = result;
    }

    @Test
    public void getClassNameTest() {
        try {
            String className = ClassUtil.getClassName(classObj);
            Assert.assertEquals(result, className);
        }
        catch(Exception e) {
            Assert.assertEquals(e.toString() ,result.toString());
        }
    }
}
