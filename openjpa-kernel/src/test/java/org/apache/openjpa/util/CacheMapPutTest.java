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
    private Integer dummyValueOld = 5;
    private Integer dummyValueNew = 6;
    public String keyType;
    public String valueType;
    private boolean maxSize;
    private boolean pinnedMap;

    private static final String NULL = "null";
    private static final String VALID = "valid";
    private static final String INVALID = "invalid";

    public CacheMapPutTest(String keyType, String valueType, boolean existingKey, boolean maxSize, boolean pinnedMap, Object output) {
        this.keyType = keyType;
        this.valueType = valueType;
        this.existingKey = existingKey;
        this.output = output;
        this.maxSize = maxSize;
        this.pinnedMap = pinnedMap;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {NULL, NULL, false, false, false, null},
                {NULL, NULL, true, false, false, null},
                {NULL, VALID, true, false, false, 5},
                {NULL, VALID, false, false, false, null},
                {NULL, INVALID, false, false, false, null},
                {NULL, INVALID, true, false, false, null},
                {VALID, INVALID, true, false, false, 5},
                {VALID, VALID, true, false, false, 5},
                {VALID, VALID, false, false, false, null},
                {INVALID, VALID, true, false, false, 5},
                {INVALID, VALID, false, false, false, null},
                {VALID, NULL, true, false, false, null},
                {VALID, NULL, false, false, false, null},
                {INVALID, NULL, true, false, false, null},
                {INVALID, NULL, false, false, false, null},
                {VALID, INVALID, true, false, false, null},   // invalid value cannot be associated to a valid key -> key associated with null
                {VALID, INVALID, false, false, false, null},
                {INVALID, INVALID, true, false, false, null},
                {INVALID, INVALID, false, false, false, null},
                // Test cases added after JaCoCo results
                {VALID, INVALID, true, true, false, null},
                {VALID, VALID, true, true, false, null},
                {VALID, INVALID, false, false, true, null},
                {VALID, VALID, false, false, true, 5},
                {VALID, NULL, false, false, true, null},
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

        if (maxSize)
            cacheMap.cacheMap.setMaxSize(0);

        if(pinnedMap)
            cacheMap.put(cacheMap.pinnedMap, key, value);

    }

    @Test
    public void test() {
        if (!Objects.equals(valueType, NULL) && !Objects.equals(keyType, NULL))
            if (Objects.equals(valueType, VALID))
                value = dummyValueNew;
            else
                value = new InvalidKeyValue();

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
