package org.apache.openjpa.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ProxyManagerImplCopyTest {
    private ProxyManagerImpl proxyManager;
    private final Object obj;
    private final Object output;

    public ProxyManagerImplCopyTest(Object obj, Object output) {
        this.obj = obj;
        this.output = output;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, null},
                {new Istance(), new Istance()},
                {new NonProxyableIstance("Apple", "iPhone12"), null}
        });
    }

    @Before
    public void setUp() {
        proxyManager = new ProxyManagerImpl();
        proxyManager.setUnproxyable(NonProxyableIstance.class.getName());
    }


    @Test
    public void test() {
        Object ret = proxyManager.copyCustom(obj);

        if (output != null)
            Assert.assertEquals(output.getClass(), ret.getClass());
        else
            Assert.assertNull(ret);
    }

    @After
    public void tearDown() {
        proxyManager = null;
    }
}

