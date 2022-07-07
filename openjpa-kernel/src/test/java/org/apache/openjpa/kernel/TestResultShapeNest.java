package org.apache.openjpa.kernel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestResultShapeNest {

    private final boolean primitiveParent;
    private final boolean primitiveChild;
    private final Object response;
    private final Class classParent;
    private final Class classChild;

    private ResultShape<Object> resultShapeParent;

    private ResultShape<Object> resultShapeChild;

    public TestResultShapeNest(Class classParent, boolean primitiveParent, Class classChild, boolean primitiveChild, Object response) {
        this.primitiveParent = primitiveParent;
        this.primitiveChild = primitiveChild;
        this.response = response;
        this.classParent = classParent;
        this.classChild = classChild;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters() {
        return Arrays.asList(new Object[][]{
                {Object[].class, false, Object.class, false, null},
                {TestClass1[].class, false, TestClass1.class, false, null},
                {TestClass2.class, false, Object.class, false, null},
                {int.class, true, TestClass2[].class, false, new UnsupportedOperationException("Can not add/nest shape to primitive shape int")},
        });
    }

    @Before
    public void configureTest() {
        resultShapeParent = new ResultShape<>(classParent, primitiveParent);
        resultShapeChild = new ResultShape<>(classChild, primitiveChild);
    }

    @Test
    public void nestTest() {
        try {
            resultShapeParent.nest(resultShapeChild);
            Assert.assertTrue(resultShapeChild.isNested());
            Assert.assertTrue(resultShapeParent.isNesting());
            Assert.assertTrue(resultShapeParent.isCompound());
            Assert.assertFalse(resultShapeParent.getChildren().isEmpty());
        }
        catch(Exception e) {
            Assert.assertEquals(e.toString() ,response.toString());
        }
    }
}
