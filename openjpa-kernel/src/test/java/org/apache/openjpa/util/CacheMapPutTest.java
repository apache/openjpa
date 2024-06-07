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

import static org.mockito.Mockito.*;

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
                // Test cases added after PIT results
                {INVALID, VALID, true, true, false, null},
                {VALID, VALID, true, false, true, 5},
                {INVALID, INVALID, true, true, true, null},
        });
    }

    @Before
    public void setUp() {
        cacheMap = spy(new CacheMap());
        pinnedSize = cacheMap.pinnedMap.size();

        setParam(keyType);
        setParam(valueType);

        if (maxSize)
            cacheMap.cacheMap.setMaxSize(0);

        if (existingKey)
            cacheMap.put(key, value);

        if (pinnedMap)
            cacheMap.put(cacheMap.pinnedMap, key, value);

    }

    @Test
    public void test() {
        if (!Objects.equals(valueType, NULL))
            if (Objects.equals(valueType, VALID))
                value = dummyValueNew;
            else
                value = new InvalidKeyValue();

        if (pinnedMap) {
            /* Clear cacheMap to ensure get(key) from pinnedMap */
            cacheMap.cacheMap.clear();
        }

        Object res = cacheMap.put(key, value);  // res is the output that we want to check
        Object checkGet = cacheMap.get(key);

        if (output != null) {
            if (valueType.equals(VALID)) {
                Assert.assertEquals(output, res);   // check put return
                Assert.assertEquals(dummyValueNew, checkGet);   // check get return
            }
        } else {
            /* check when the output is null:
             * 1) key is invalid
             * 2) no existing key */
            Assert.assertNull(res);
        }

        /* Killed some mutations */
        if (!existingKey && !pinnedMap && !maxSize)
            verify(cacheMap).put(key, value);

        verify(cacheMap).get(key);

        if (pinnedMap && valueType.equals(VALID) && existingKey) {
            verify(cacheMap).put(cacheMap.pinnedMap, key, dummyValueOld);
            verify(cacheMap).put(cacheMap.pinnedMap, key, dummyValueNew);

            verify(cacheMap, times(2)).writeLock();
            verify(cacheMap).entryAdded(key, dummyValueOld);
            verify(cacheMap).entryRemoved(key, dummyValueOld, false);
            verify(cacheMap).entryAdded(key, dummyValueNew);
            verify(cacheMap, times(2)).writeUnlock();
        } else if (!pinnedMap && !maxSize && existingKey && valueType.equals(VALID)) {
            verify(cacheMap).put(key, dummyValueOld);
            verify(cacheMap).put(key, dummyValueNew);

            verify(cacheMap, times(2)).writeLock();
            verify(cacheMap).entryRemoved(key, dummyValueOld, false);
            verify(cacheMap).entryAdded(key, dummyValueNew);
            verify(cacheMap, times(2)).writeUnlock();
        } else if (pinnedMap && valueType.equals(NULL)) {
            verify(cacheMap, times(2)).put(cacheMap.pinnedMap, key, value);
            verify(cacheMap).put(key, value);

            verify(cacheMap).writeLock();
            verify(cacheMap).entryAdded(key, value);
            verify(cacheMap).writeUnlock();
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
