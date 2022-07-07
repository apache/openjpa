package org.apache.openjpa.kernel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestResultShapeNestParent {
    private final boolean primitiveParent;
    private final boolean primitiveChild;
    private final Object result;
    private final Class classParent;
    private final Class classChild;
    private ResultShape<Object> resultShapeParent;
    private ResultShape<Object> resultShapeChild;

    public TestResultShapeNestParent(Class classParent, boolean primitiveParent, Class classChild, boolean primitiveChild, Object result) {
        this.primitiveParent = primitiveParent;
        this.primitiveChild = primitiveChild;
        this.result = result;
        this.classParent = classParent;
        this.classChild = classChild;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters() {
        return Arrays.asList(new Object[][]{
                {Object[].class, false, Object.class, false,
                        new IllegalArgumentException("Object[] can not nest recursive Object{Object[]}")},
                {TestClass2.class, false, Object.class, false,
                        new IllegalArgumentException("TestClass2 can not nest recursive Object{TestClass2}")},
                {int.class, true, TestClass2[].class, false,
                        new UnsupportedOperationException("Can not add/nest shape to primitive shape int")},
        });
    }

    @Before
    public void configureTest() {
        resultShapeParent = new ResultShape<>(classParent, primitiveParent);
        resultShapeChild = new ResultShape<>(classChild, primitiveChild);
    }

    @Test
    public void nestParentTest() {
        try {
            resultShapeChild.nest(resultShapeParent);
            resultShapeParent.nest(resultShapeChild);
        }
        catch(Exception e) {
            Assert.assertEquals(e.toString(), result.toString());
        }
    }
}
