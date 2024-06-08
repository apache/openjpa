package org.apache.openjpa.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.mockito.Mockito.spy;

@RunWith(Parameterized.class)
public class ProxyManagerImplCopyTest {
    private ProxyManagerImpl proxyManager;
    private Object obj;
    private Object output;
    private final String instance;
    private final String outputInstance;

    private static final String NULL = "null";
    private static final String PROXYABLE = "instance";
    private static final String NON_PROXYABLE = "non-proxyable";

    public ProxyManagerImplCopyTest(String instance, String outputInstance) {
        this.instance = instance;
        this.outputInstance = outputInstance;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {NULL, NULL},
                {PROXYABLE, PROXYABLE},
                {NON_PROXYABLE, NULL},
        });
    }

    @Before
    public void setUp() {
        proxyManager = spy(new ProxyManagerImpl());
        proxyManager.setUnproxyable(NonProxyableIstance.class.getName());

        setParam(instance);
        setParam(outputInstance);
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

    private void setParam(String param) {
        switch (param) {
            case NULL:
                obj = null;
                output = null;
                break;
            case PROXYABLE:
                obj = new Istance();
                output = new Istance();
                break;
            case NON_PROXYABLE:
                obj = new NonProxyableIstance("Apple", "iPhone12");
                output = new NonProxyableIstance("Apple", "iPhone12");
                break;
        }
    }
}

