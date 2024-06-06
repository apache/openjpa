package org.apache.openjpa.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import static org.mockito.Mockito.spy;

@RunWith(Parameterized.class)
public class CacheMapPutTest {
    private Object key;
    private Object value;
    private final boolean existingKey;
    private CacheMap cacheMap;
    private final Object output;
    private static final Integer dummyValueOld = 5;
    private Integer dummyValueNew = 6;
    public String keyType;
    public String valueType;

    private static final String NULL = "null";
    private static final String VALID = "valid";
    private static final String INVALID = "invalid";

    public CacheMapPutTest(String keyType, String valueType, boolean existingKey, Object output) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.existingKey = existingKey;
        this.output = output;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {NULL, NULL, false, NullPointerException.class},
                {VALID, VALID, true, dummyValueOld},
                {VALID, VALID, false, null},
                {INVALID, VALID, true, dummyValueOld},
                {INVALID, VALID, false, null},
                {VALID, NULL, true, null},
                {VALID, NULL, false, null},
                {INVALID, NULL, true, null},
                {INVALID, NULL, false, null},
                {VALID, INVALID, true, null},   // invalid value cannot be associated to a valid key -> key associated with null
                {VALID, INVALID, false, null},
                {INVALID, INVALID, true, null},
                {INVALID, INVALID, false, null},
        });
    }

    @Before
    public void setUp() {
        cacheMap = spy(new CacheMap());

        if (!Objects.equals(keyType, NULL) && !Objects.equals(valueType, NULL)) {
            setParam(keyType);
            setParam(valueType);
        }

        if (existingKey)
            cacheMap.put(key, value);
    }

    @Test
    public void test() {
        if (!Objects.equals(valueType, NULL) && !Objects.equals(keyType, NULL))
            if (Objects.equals(valueType, VALID))
                value = dummyValueNew;
            else
                value = new InvalidKeyValue();

        try {
            Object res = cacheMap.put(key, value);  // res is the output that we want to check
            Object checkGet = cacheMap.get(key);

            if (output != null && Objects.equals(keyType, VALID)) {
                if (valueType.equals(VALID)) {
                    Assert.assertEquals(output, res);   // check put return
                    Assert.assertEquals(dummyValueNew, checkGet);   // check get return
                }
            } else if (output != null && Objects.equals(keyType, INVALID)) {
                Assert.assertEquals(output, res);   // check put return
                Assert.assertEquals(dummyValueNew, checkGet);   // check get return
            } else {
                /* check when the output is null:
                * 1) key is invalid
                * 2) no existing key */
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
                value = null;
                break;
            case VALID:
                key = new Object();
                value = dummyValueOld;
                break;
            case INVALID:
                key = new InvalidKeyValue();
                value = new InvalidKeyValue();
                break;
        }
    }
}
