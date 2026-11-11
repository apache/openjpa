/*
 * TestFieldRange.java
 *
 * Created on October 12, 2006, 10:14 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.common.utils.AbstractTestCase;
import org.apache.openjpa.persistence.kernel.common.apps.AllFieldTypesTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class TestFieldRange extends BaseKernelTest {

    protected static String RANDOM_STRING =
        "This is my test String with all "
            + "kinds of wierd characters: "
            + "!@@#$\\%^&\"*()-=\\|\"\"\"\"\"+_/?.>,<~`"
            + "'''''''''''\\\\\\\\\\\\\\\\\\\\\\\\\\\\"
            + "''''''''''''\\\\\\\\\\\\\\\\\\\\\\\\\\\\"
            + "!@@#$\\%^&\"*()-=\\|+_/?.>,<~`";

    @BeforeEach
    void setUp()
        throws Exception {
        deleteAll(AllFieldTypesTest.class);
    }

    @Test
    void saveState()
        throws Exception {
        allFieldSaveState(
            (int) 259645,
            (short) 50849,
            (long) 2349847982L,
            (float) 43273423.0234723F,
            (double) 34678.02384723D,
            (byte) -120,
            (boolean) true,
            (char) '%',
            (Date) new Date(),
            (Serializable) new StringBuffer(5000),
            (String) RANDOM_STRING,
            randomBigInteger(),
            randomBigDecimal());
    }

    /**
     * Test to make sure all the numeric fields can accept the maximum
     * values for their data size. Note that we subtract one from
     * each of the values because some databases (like InstantDB)
     * may consider them to be equivalent to NULL.
     * FixMe: Hangs for some mysterious reason. aokeke
     */

    //    public void testLargeNumbers()
//    throws Exception {
//        try {
//            allFieldSaveState(
//                    (int) (Integer.MAX_VALUE - 1),
//                    (short) (Short.MAX_VALUE - 1),
//                    (long) (Long.MAX_VALUE - 1l),
//                    (float) (Float.MAX_VALUE - 1.0f),
//                    (double) (Double.MAX_VALUE - 1.0d),
//                    (byte) (Byte.MAX_VALUE),
//                    (boolean) true,
//                    (char) 'q',
//                    (Date) new Date(),
//                    (Serializable) new StringBuffer(5000),
//                    (String) RANDOM_STRING,
//                    randomBigInteger(),
//                    randomBigDecimal());
//        } catch (Throwable e) {
//            bug(3, e, "doubles and floats");
//        }
//    }

    /**
     * Test to make sure all the numeric fields can be set to
     * very low values. We add one to the minimim value because
     * some databases (such as InstantDB) consider the MIN_VALUE
     * to be equivalent to null. This is arguably a bug, but
     * not a killer one.
     */
    @Test
    void largeNumbersNegative()
        throws Exception {
        allFieldSaveState(
            (int) (Integer.MIN_VALUE + 1),
            (short) (Short.MIN_VALUE + 1),
            (long) (Long.MIN_VALUE + 1L),
            (float) (Float.MIN_VALUE + 1.0f),
            (double) (Double.MIN_VALUE + 1.0d),
            (byte) (Byte.MIN_VALUE + 1),
            (boolean) true,
            (char) 'q',
            (Date) new Date(),
            (Serializable) new StringBuffer(5000),
            (String) RANDOM_STRING,
            randomBigInteger(),
            randomBigDecimal());
    }

    @Test
    void doubleAndFloatPrecision()
        throws Exception {
        allFieldSaveState(
            (int) (0),
            (short) (0),
            (long) (0L),
            (float) (10.0f / 3.0f),
            (double) (100.0d / 3.0d),
            (byte) (0),
            (boolean) true,
            (char) 'q',
            (Date) new Date(),
            (Serializable) new StringBuffer(5000),
            (String) RANDOM_STRING,
            randomBigInteger(),
            randomBigDecimal());
    }

    @Test
    void zeroNumbers()
        throws Exception {
        allFieldSaveState(
            (int) (0),
            (short) (0),
            (long) (0L),
            (float) (0.0f),
            (double) (0.0d),
            (byte) (0),
            (boolean) true,
            (char) 'q',
            (Date) new Date(),
            (Serializable) new StringBuffer(5000),
            (String) RANDOM_STRING,
            new BigInteger("0"),
            new BigDecimal("0.0"));
    }

    @Test
    void lowDate()
        throws Exception {
        dateTest(0);
    }

    @Test
    void curDate()
        throws Exception {
        dateTest(System.currentTimeMillis());
    }

    @Test
    void highDate()
        throws Exception {
        try {
            // postgres will sometimes store the String "invalid" if the
            // date is too high, which will prevent us from even reading
            // the records that contain this corrupt value (thus breaking
            // any subsequent attempts to read instances of AllFieldTypesTest).
            // An Example of a date like this is:
            //   (Timestamp) 2038-02-08 22:20:07.65
            if (getCurrentPlatform() ==
                AbstractTestCase.Platform.POSTGRESQL)
                fail("Postgres can't even try to store a high date");

            dateTest(System.currentTimeMillis() * 2);
        } catch (AssertionFailedError e) {
            bug(6, e, "Some data stores cannot deal "
                + "with very high dates");
        }
    }

    /**
     * Some date instances that have been known to have problems.
     */
    @Test
    void problematicDates()
        throws Exception {
        dateTest(1047744639);        // pointbase had probs with this
    }

    private void dateTest(long l)
        throws Exception {
        Date d = new Date(l);

        allFieldSaveState(
            (int) 10,
            (short) 10,
            (long) 10,
            (float) 0,
            (double) 0,
            (byte) 10,
            (boolean) true,
            (char) 'x',
            (Date) d,
            (Serializable) new StringBuffer(10),
            (String) RANDOM_STRING,
            new BigInteger("0"),
            new BigDecimal("0"));
    }

    public void allFieldSaveState(int testint, short testshort, long testlong,
        float testfloat, double testdouble, byte testbyte,
        boolean testboolean, char testchar, Date testDate,
        Serializable testObject, String testString,
        BigInteger testBigInteger, BigDecimal testBigDecimal)
        throws Exception {
        try {
            allFieldSaveStateInternal(testint, testshort, testlong,
                testfloat, testdouble, testbyte,
                testboolean, testchar, testDate,
                testObject, testString, testBigInteger, testBigDecimal);
        } finally {
            try {
                // make *sure* we do not leave a transaction open
                rollbackTx(getPM(true, false));
            } catch (Throwable t) {

            }
        }
    }

    public void allFieldSaveStateInternal(
        int testint, short testshort, long testlong,
        float testfloat, double testdouble, byte testbyte,
        boolean testboolean, char testchar, Date testDate,
        Serializable testObject, String testString,
        BigInteger testBigInteger, BigDecimal testBigDecimal)
        throws Exception {
        OpenJPAEntityManager pm = getPM(true, false);
        startTx(pm);

        AllFieldTypesTest test = new AllFieldTypesTest();
        pm.persist(test);
        Object testID = pm.getObjectId(test);

        test.setTestint(testint);
        test.setTestlong(testlong);
        test.setTestdouble(testdouble);
        test.setTestshort(testshort);
        test.setTestfloat(testfloat);
        test.setTestbyte(testbyte);
        test.setTestboolean(testboolean);
        test.setTestchar(testchar);
        test.setTestString(testString);
        test.setTestDate(testDate);
        test.setTestObject(testObject);
        test.setTestBigInteger(testBigInteger);
        test.setTestBigDecimal(testBigDecimal);

        try {
            endTx(pm);
        } catch (Exception e) {
            if (e instanceof Exception &&
                ((Exception) e).getMessage().indexOf
                    ("Maximum length is 8000") != -1) {
                bug(AbstractTestCase.Platform.SQLSERVER, 5, e,
                    "SQLServer cannot deal"
                        + " with numbers with more than 8000 digits");
            } else {
                throw e;
            }
        }

        endEm(pm);

        //assertPersistent (test, true, false, false, false);

        pm = getPM(true, false);
        startTx(pm);

        AllFieldTypesTest retrievedObject =
            (AllFieldTypesTest) pm.find(AllFieldTypesTest.class, testID);

        assertEquals(testint, retrievedObject.getTestint(),
            "Field type int");
        assertEquals(testshort, retrievedObject.getTestshort(),
            "Field type short");
        assertEquals(testboolean, retrievedObject.getTestboolean(),
            "Field type boolean");
        assertEquals(testchar, retrievedObject.getTestchar(),
            "Field type char");
        assertEquals(testlong, retrievedObject.getTestlong(),
            "Field type long");

        assertEquals(testbyte, retrievedObject.getTestbyte(),
            "Field type byte");
        assertEquals(testString, retrievedObject.getTestString(),
            "Field type String");

        int i1 = (int) (testDate.getTime() / 1000);
        int i2 = (int) (retrievedObject.getTestDate().getTime() / 1000);

        int testDateDay = testDate.getDay();
        int testDateMonth = testDate.getMonth();
        int testDateYear = testDate.getYear();

        int retrievedObjectDay = retrievedObject.getTestDate().getDay();
        int retrievedObjectMonth = retrievedObject.getTestDate().getMonth();
        int retrievedObjectYear = retrievedObject.getTestDate().getYear();

        // System.out.println("i1 : " + i1 + "\ni2 : " + i2);

        // CR346162. In this CR, it was stated that @Temporal(DATE) fields will
        // be equal for year, month, day but not for hours,
        // minutes, seconds. So, we removed the time check and checked only for
        // the equality of day, month and year

        /* assertEquals("Field type Date: "
     + testDate.getTime() + "!="
     + retrievedObject.getTestDate().getTime()
     + "[" + new Date(testDate.getTime()) + " != "
     + new Date(retrievedObject.getTestDate().getTime()) + "]",
     (int) (testDate.getTime() / 1000),
     (int) (retrievedObject.getTestDate().getTime() / 1000));*/

        if ((testDateDay != retrievedObjectDay) ||
            (testDateMonth != retrievedObjectMonth) ||
            (testDateYear != retrievedObjectYear)) {
            fail("Field type Date not stored properly. One or more of "
                + "the components of the date (day, month or year) "
                + "do not match. \n" + " Value that should be stored : "
                + testDate.toGMTString() + ". \nValue that is actually"
                + "stored : " + retrievedObject.getTestDate().toGMTString());
        }
        //assertEquals ("Field type Object", testObject,
        //retrievedObject.getTestObject ());
        assertEquals(testBigInteger, retrievedObject.getTestBigInteger(),
            "Field type BigInteger");

        try {
            assertEquals(testBigDecimal.toBigInteger(),
                retrievedObject.getTestBigDecimal().toBigInteger(),
                "Field type BigDecimal (BigInteger part)");

            assertEquals(testBigDecimal,
                retrievedObject.getTestBigDecimal(),
                "Field type BigDecimal");

            assertEquals(testfloat, retrievedObject.getTestfloat(),
                0.01f, "Field type float");
            assertEquals(testdouble, retrievedObject.getTestdouble(),
                0.01d, "Field type double");
        } catch (AssertionFailedError afe) {
            bug(3, afe,
                "Doubles and Floats lose precision in some data stores");
        }

        rollbackTx(pm);
    }
}
