package org.apache.openjpa.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.spy;

@RunWith(Parameterized.class)
public class CacheMapGetTest {
    private Object key;
    private String keyType;
    private boolean existingKey;
    private CacheMap cacheMap;
    private Object output;
    private static Integer dummyValue = 5;

    private static final String NULL = "null";
    private static final String VALID = "valid";
    private static final String INVALID = "invalid";

    public CacheMapGetTest(String keyType, boolean existingKey, Object output) {
        this.keyType = keyType;
        this.existingKey = existingKey;
        this.output = output;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {NULL, false, NullPointerException.class},
                {VALID, true, dummyValue},
                {VALID, false, null},
                {INVALID, true, null},
                {INVALID, false, null}
        });
    }

    @Before
    public void setUp() {
        cacheMap = spy(new CacheMap());

        setParam(keyType);

        if (existingKey)
            cacheMap.put(key, dummyValue);
    }

    @Test
    public void test() {
        try {
            Object res = cacheMap.get(key);

            if (output != null && keyType.equals(VALID)) {
                Assert.assertEquals(output, res);
            } else {
                Assert.assertNull(res);
            }
        } catch (Exception e) {
            assert output != null;
            Assert.assertEquals(output.getClass(), e.getClass());
        }
    }

    @After
    public void tearDown() {
        cacheMap.clear();
    }

    private void setParam(String param) {
        switch (param) {
            case NULL:
                key = null;
                break;
            case VALID:
                key = new Object();
                break;
            case INVALID:
                key = new InvalidKeyValue();
                break;
        }
    }
}
