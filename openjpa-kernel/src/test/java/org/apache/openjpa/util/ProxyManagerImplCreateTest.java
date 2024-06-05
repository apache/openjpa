package org.apache.openjpa.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ProxyManagerImplCreateTest {
    private ProxyManagerImpl proxyManager;
    private final Object obj;
    private final Class<?> output;
    private final boolean autoOff;

    public ProxyManagerImplCreateTest(Class<?> output, Object obj, boolean autoOff) {
        this.output = output;
        this.obj = obj;
        this.autoOff = autoOff;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, null, true},
                {Proxy.class, new Istance(), true},
                {Proxy.class, new Istance(), false},
                {null, new NonProxyableIstance("Apple", "iPhone12"), false}
        });
    }

    @Before
    public void setUp() {
        proxyManager = new ProxyManagerImpl();
    }

    @Test
    public void test() {
        Object ret = proxyManager.newCustomProxy(obj, autoOff);

        if (output != null)
            assertThat(ret, instanceOf(output));
        else
            Assert.assertNull(ret);
    }

    @After
    public void tearDown() {
        proxyManager = null;
    }
}
