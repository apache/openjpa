package org.apache.openjpa.kernel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class TestResultShapeBasic {
    private final Class cls;
    private final Boolean primitive;
    private final FillStrategy fillStrategy;
    private final String alias;
    private final Object res;
    private ResultShape resultShape;

    @Rule
    public TestName testName = new TestName();

    public TestResultShapeBasic(Class cls, Boolean primitive, FillStrategy fillStrategy, String alias, Object res) {
        this.cls = cls;
        this.primitive = primitive;
        this.fillStrategy = fillStrategy;
        this.alias = alias;
        this.res = res;
    }

    @Before
    public void configureTests(){
        try{
            if (testName.getMethodName().startsWith("testBasic1")){
                resultShape = new ResultShape(cls);
            }

            if (testName.getMethodName().startsWith("testBasic2")) {
                resultShape = new ResultShape(cls, primitive);
            }

            if (testName.getMethodName().startsWith("testBasic3")){
                resultShape = new ResultShape<>(cls, fillStrategy, primitive);
            }
        }
        catch (NullPointerException e){
            resultShape = null;
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters() {
        return Arrays.asList(new Object[][]{

                {Object.class, true, new FillStrategy.NewInstance<>(Object.class), "alias", null},
                {TestClass1.class, true, null, "", null},
                {TestClass2.class, false, new FillStrategy.NewInstance<>(TestClass2.class), null, null},
                {null, null, null, "", NullPointerException.class},

        });
    }

    @Test
    public void testBasic1() {
        try {
            Assert.assertEquals(resultShape.getType(),cls);
            Assert.assertFalse(resultShape.isPrimitive());
            if(alias!=null) {
                resultShape.setAlias(alias);
                Assert.assertEquals(resultShape.getAlias(),alias);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals(e.getClass(), res);
        }
    }

    @Test
    public void testBasic2() {
        try {
            Assert.assertEquals(resultShape.getType(),cls);
            if(primitive)
                Assert.assertTrue(resultShape.isPrimitive());
            else
                Assert.assertTrue(resultShape.isCompound());
            if(alias!=null) {
                resultShape.setAlias(alias);
                Assert.assertEquals(resultShape.getAlias(),alias);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals(e.getClass(), res);
        }
    }

    @Test
    public void testBasic3() {
        try {
            Assert.assertEquals(resultShape.getStrategy(), fillStrategy);
            Assert.assertEquals(resultShape.getType(),cls);
            if(primitive)
                Assert.assertTrue(resultShape.isPrimitive());
            else
                Assert.assertTrue(resultShape.isCompound());
            if(alias!=null) {
                resultShape.setAlias(alias);
                Assert.assertEquals(resultShape.getAlias(),alias);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals(e.getClass(), res);
        }
    }
}
