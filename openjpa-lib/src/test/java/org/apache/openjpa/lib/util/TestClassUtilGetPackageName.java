package org.apache.openjpa.lib.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestClassUtilGetPackageName {
    private Class<?> classObj;

    private Object result;

    @Parameterized.Parameters
    public static Collection<Object[]> configure() {
        return Arrays.asList(new Object[][]{
                { TestClassUtilToClass.class, "org.apache.openjpa.lib.util"},
                { TestClass1.class, "org.apache.openjpa.lib.util"},
                { TestClass1[].class, "org.apache.openjpa.lib.util"},
                { long.class, ""},
                { long[].class, ""},
                { null, null}
        });
    }

    public TestClassUtilGetPackageName(Class classObj, Object result ){
        this.classObj = classObj;
        this.result = result;
    }

    @Test
    public void getPackageNameTest() {
        String pakageName = ClassUtil.getPackageName(classObj);
        try {
            Assert.assertEquals(pakageName, result);
        }
        catch (Exception e){
            Assert.assertEquals(e.toString() ,result.toString());
        }
    }
}
