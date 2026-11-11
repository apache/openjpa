/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.kernel;

import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.util.Collection;
import java.util.EnumSet;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.OpenJPAQuery;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;
import org.apache.openjpa.persistence.kernel.common.apps.AllFieldTypesTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * Test various special numbers, such as Double.NaN.
 *
 * @author <a href="mailto:marc@solarmetric.com">Marc Prud'hommeaux</a>
 */
class TestSpecialNumbers2 extends BaseKernelTest {
    @BeforeEach
    void setUp() {
        try {
            deleteAll(AllFieldTypesTest.class);
        }
        catch (Exception e) {
            // catch errors when deleting ... PostgreSQL has
            // a couple problems with some of the values that
            // try to get inserted.
        }
    }

    @Test
    void shortMax() {
        AllFieldTypesTest aftt = new AllFieldTypesTest();
        aftt.setTestshort(Short.MAX_VALUE);
        saveAndQuery(aftt, "testshort =", Short.MAX_VALUE);
    }

    @Test
    void shortMin() {
        AllFieldTypesTest aftt = new AllFieldTypesTest();
        aftt.setTestshort(Short.MIN_VALUE);
        try {
            saveAndQuery(aftt, "testshort =",
                    Short.MIN_VALUE);
        } catch (Throwable t) {
            bug(AbstractTestCase.Platform.EMPRESS, 889, t,
                    "Empress cannot store min values");
        }
    }

    @Test
    void longMax() {
        AllFieldTypesTest aftt = new AllFieldTypesTest();
        aftt.setTestlong(Long.MAX_VALUE);
        saveAndQuery(aftt, "testlong =", Long.MAX_VALUE);
    }

    @Test
    void longMin() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestlong(Long.MIN_VALUE);
            saveAndQuery(aftt, "testlong =", Long.MIN_VALUE);
        } catch (Throwable t) {
            bug(AbstractTestCase.Platform.HYPERSONIC, 474, t,
                    "Some databases cannot store Long.MIN_VALUE");
        }
    }

    @Test
    void integerMax() {
        AllFieldTypesTest aftt = new AllFieldTypesTest();
        aftt.setTestint(Integer.MAX_VALUE);
        saveAndQuery(aftt, "testint =",
                Integer.MAX_VALUE);
    }

    @Test
    void integerMin() {
        AllFieldTypesTest aftt = new AllFieldTypesTest();
        aftt.setTestint(Integer.MIN_VALUE);
        try {
            saveAndQuery(aftt, "testint =",
                    Integer.MIN_VALUE);
        } catch (Throwable t) {
            bug(AbstractTestCase.Platform.EMPRESS, 889, t,
                    "Empress cannot store min values");
        }
    }

    @Test
    void floatMax() {
        try {
        	AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestfloat(Float.MAX_VALUE);
            saveAndQuery(aftt, "testfloat =",
                    Float.MAX_VALUE);
        } catch (Exception e) {
            bug(getCurrentPlatform(), 494, e,
                    "Some datastores cannot store Float.MAX_VALUE");
        }
    }

    @Test
    void floatMin() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestfloat(Float.MIN_VALUE);
            saveAndQuery(aftt, "testfloat =",
                    Float.MIN_VALUE);
        } catch (Exception | AssertionFailedError e) {
            bug(getCurrentPlatform(), 494, e,
                    "Some databases cannot store Float.MIN_VALUE");
        }
    }

    @Test
    void floatNaN() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestfloat(Float.NaN);
            saveAndQuery(aftt, "testfloat =", Float.NaN);
        } catch (Throwable t) {
            bug(461, t, "NaN problems");
        }
    }

    @Test
    void floatNegativeInfinity() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestfloat(Float.NEGATIVE_INFINITY);
            saveAndQuery(aftt, "testfloat =",
                    Float.NEGATIVE_INFINITY);
        } catch (Exception e) {
            bug(getCurrentPlatform(), 494, e,
                    "Some databases cannot store Float.NEGATIVE_INFINITY");
        }
    }

    @Test
    void floatPostivieInfinity() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestfloat(Float.POSITIVE_INFINITY);
            saveAndQuery(aftt, "testfloat =",
                    Float.POSITIVE_INFINITY);
        } catch (Exception e) {
            bug(getCurrentPlatform(), 494, e,
                    "Some databases cannot store Float.POSITIVE_INFINITY");
        }
    }

    @Test
    void doubleMax() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestdouble(Double.MAX_VALUE);
            saveAndQuery(aftt, "testdouble =",
                    Double.MAX_VALUE);
        } catch (Exception e) {
            bug(getCurrentPlatform(), 494, e,
                    "Some databases cannot store Double.MAX_VALUE");
        }
    }

    @Test
    void doubleMin() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestdouble(Double.MIN_VALUE);
            saveAndQuery(aftt, "testdouble =",
                    Double.MIN_VALUE);
        } catch (Exception e) {
            bug(EnumSet.of(AbstractTestCase.Platform.POSTGRESQL,
                AbstractTestCase.Platform.SQLSERVER,
                AbstractTestCase.Platform.ORACLE,
                AbstractTestCase.Platform.EMPRESS,
                AbstractTestCase.Platform.DB2,
                AbstractTestCase.Platform.INFORMIX,
                AbstractTestCase.Platform.DERBY), 494, e,
                "Some databases cannot store Double.MIN_VALUE");
        } catch (AssertionFailedError e) {
            bug(EnumSet.of(AbstractTestCase.Platform.MARIADB, AbstractTestCase.Platform.MYSQL), 494, e,
                    "Some databases cannot store Double.MIN_VALUE");
        }
    }

    @Test
    void doubleNaN() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestdouble(Double.NaN);
            saveAndQuery(aftt, "testdouble =", Double.NaN);
        } catch (Throwable t) {
            bug(461, t, "NaN problems");
        }
    }

    @Test
    void doubleNegativeInfinity() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestdouble(Double.NEGATIVE_INFINITY);
            saveAndQuery(aftt, "testdouble =",
                    Double.NEGATIVE_INFINITY);
        } catch (Throwable t) {
            bug(461, t, "infinity problems");
        }
    }

    @Test
    void doublePostivieInfinity() {
        try {
            AllFieldTypesTest aftt = new AllFieldTypesTest();
            aftt.setTestdouble(Double.POSITIVE_INFINITY);
            saveAndQuery(aftt, "testdouble =",
                    Double.POSITIVE_INFINITY);
        } catch (Throwable t) {
            bug(461, t, "infinity problems");
        }
    }

    @Test
    void byteMin() {
        AllFieldTypesTest aftt = new AllFieldTypesTest();
        aftt.setTestbyte(Byte.MIN_VALUE);
        try {
            saveAndQuery(aftt, "testbyte =", Byte.MIN_VALUE);
        } catch (Throwable t) {
            //bug(AbstractTestCase.Platform.EMPRESS, 889, t,
            //    "Empress cannot store min values");
            fail(
                "Exception was thrown while storing Byte.MIN_VALUE : \n" +
                    getStackTrace(t));
        }
    }

    @Test
    void byteMax() {
        AllFieldTypesTest aftt = new AllFieldTypesTest();
        aftt.setTestbyte(Byte.MAX_VALUE);
        saveAndQuery(aftt, "testbyte =", Byte.MAX_VALUE);
    }

    @Test
    void zeroBigInteger() {
        AllFieldTypesTest aftt = new AllFieldTypesTest();
        aftt.setTestBigInteger(BigInteger.ZERO);
        saveAndQuery(aftt, "testBigInteger =", BigInteger.ZERO);
    }

    @Test
    void oneBigInteger() {
        AllFieldTypesTest aftt = new AllFieldTypesTest();
        aftt.setTestBigInteger(BigInteger.ONE);
        saveAndQuery(aftt, "testBigInteger =", BigInteger.ONE);
    }

    private void saveAndQuery(Object obj, String query, Object param) {
        OpenJPAEntityManager pm = getPM(false, false);
        startTx(pm);
        pm.persist(obj);
        endTx(pm);
        endEm(pm);

        pm = getPM(false, false);
//        Query q = pm.newQuery(obj.getClassquery);
//        q.declareParameters("Object param");
//        Collection c = (Collection) q.execute(param);
        OpenJPAQuery q = pm.createQuery(
            "SELECT a FROM AllFieldTypesTest a WHERE a." + query + " " + param);
        Collection c = (Collection) q.getResultList();
        assertSize(1, c);
        pm.close();
    }
}


