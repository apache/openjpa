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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.kernel.common.apps.AllFieldTypesTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class TestFieldRange2 extends BaseKernelTest {

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
        //pm.persist(test);
        //Object testID = pm.getObjectId(test);

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

        pm.persist(test);
        Object testID = pm.getObjectId(test);

        try {
            endTx(pm);
        } catch (Exception e) {
            if (e instanceof Exception &&
                ((Exception) e).getMessage().indexOf
                    ("Maximum length is 8000") != -1) {
                bug(5, e, "SQLServer cannot deal"
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

        //System.out.println("i1 : " + i1 + "\ni2 : " + i2);
        assertEquals((int) (testDate.getTime() / 1000),
            (int) (retrievedObject.getTestDate().getTime() / 1000),
            "Field type Date: "
            + testDate.getTime() + "!="
            + retrievedObject.getTestDate().getTime()
            + "[" + new Date(testDate.getTime()) + " != "
            + new Date(retrievedObject.getTestDate().getTime()) + "]");
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
