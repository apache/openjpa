package org.apache.openjpa.kernel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestResultShapeAdd {
    private final Class<?>[] classes;
    private final boolean primitive;
    private final Object response;
    private ResultShape<Object> resultShape;


    public TestResultShapeAdd(Class<?>[] classes, boolean primitive, Object response) {
        this.classes = classes;
        this.primitive = primitive;
        this.response = response;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters() {
        return Arrays.asList(new Object[][]{

                {new Class[]{TestClass2.class, short.class}, false, null},
                {new Class[]{}, true,
                        new UnsupportedOperationException("Can not add/nest shape to primitive shape Object")},
                {null, false, new NullPointerException()}
        });
    }

    @Before
    public void configureTest() {
        resultShape = new ResultShape<>(Object.class, primitive);
    }

    @Test
    public void addTest() {
        try {
            resultShape.add(classes);
        }catch(Exception e) {
            Assert.assertEquals(e.toString() ,response.toString());
        }
    }

}
