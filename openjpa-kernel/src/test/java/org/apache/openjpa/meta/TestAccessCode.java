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
package org.apache.openjpa.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * Access code is a 5-bit integer.
 *
 * @author Pinaki Poddar
 *
 */
public class TestAccessCode {
    public static final int UNKNOWN = AccessCode.UNKNOWN;
    public static final int FIELD = AccessCode.FIELD;
    public static final int PROPERTY = AccessCode.PROPERTY;
    public static final int EXPLICIT = AccessCode.EXPLICIT;
    public static final int MIXED = AccessCode.MIXED;

    // Valid class codes are 0 2 4 10 12 26 28
    @Test
    public void testValidClassCodes() {
        isValidClassCode(true,   0, UNKNOWN);

        isValidClassCode(true,   2, FIELD);
        isValidClassCode(true,   4, PROPERTY);
        isValidClassCode(false,  6, FIELD | PROPERTY);

        isValidClassCode(false,  8, EXPLICIT);
        isValidClassCode(true,  10, EXPLICIT | FIELD);
        isValidClassCode(true,  12, EXPLICIT | PROPERTY);
        isValidClassCode(false, 14, EXPLICIT | FIELD | PROPERTY);

        isValidClassCode(false, 16, MIXED);
        isValidClassCode(false, 18, MIXED | FIELD);
        isValidClassCode(false, 20, MIXED | PROPERTY);
        isValidClassCode(false, 22, MIXED | FIELD | PROPERTY);
        isValidClassCode(false, 24, MIXED | EXPLICIT | UNKNOWN);
        isValidClassCode(true,  26, MIXED | EXPLICIT | FIELD);
        isValidClassCode(true,  28, MIXED | EXPLICIT | PROPERTY);
        isValidClassCode(false, 30, MIXED | EXPLICIT | FIELD | PROPERTY);

        // All odd codes other than 1 are invalid
        for (int i = 3; i < 32; i += 2)
            assertFalse(AccessCode.isValidClassCode(i),
                "Not a valid Class Code: " +
                AccessCode.toClassString(i));
    }

    // Valid field codes are 0 2 4 10 12
    @Test
    public void testValidFieldCodes() {
        isValidClassCode(true,   0, UNKNOWN);

        isValidClassCode(true,   2, FIELD);
        isValidClassCode(true,   4, PROPERTY);
        isValidClassCode(false,  6, FIELD | PROPERTY);

        isValidClassCode(false,  8, EXPLICIT);
        isValidClassCode(true,  10, EXPLICIT | FIELD);
        isValidClassCode(true,  12, EXPLICIT | PROPERTY);
        isValidClassCode(false, 14, EXPLICIT | FIELD | PROPERTY);

        // any even code with MIXED bit set is invalid
        for (int i = MIXED; i < 32; i += 2) {
            assertFalse(AccessCode.isValidFieldCode(i),
                "Not a valid field code: " +
                AccessCode.toFieldString(i));
        }
    }

    @Test
    public void testProperty() {
        isProperty(false,  0, UNKNOWN);
        isProperty(false,  2, FIELD);
        isProperty(true,   4, PROPERTY);
        isProperty(false, 10, EXPLICIT | FIELD);
        isProperty(true,  12, EXPLICIT | PROPERTY);
        isProperty(false, 26, MIXED | EXPLICIT | FIELD);
        isProperty(true,  28, MIXED | EXPLICIT | PROPERTY);
    }

    @Test
    public void testField() {
        isField(false,  0, UNKNOWN);
        isField(true,   2, FIELD);
        isField(false,  4, PROPERTY);
        isField(true,  10, EXPLICIT | FIELD);
        isField(false, 12, EXPLICIT | PROPERTY);
        isField(true,  14, EXPLICIT | FIELD | PROPERTY);
        isField(true, 26, MIXED | EXPLICIT | FIELD);
        isField(false,  28, MIXED | EXPLICIT | PROPERTY);
    }

    @Test
    public void testExplicit() {
        isExplicit(false,  0, UNKNOWN);
        isExplicit(false,  2, FIELD);
        isExplicit(false,  4, PROPERTY);
        isExplicit(true, 10, EXPLICIT | FIELD);
        isExplicit(true, 12, EXPLICIT | PROPERTY);
        isExplicit(true,  14, EXPLICIT | FIELD | PROPERTY);
        isExplicit(true, 26, MIXED | EXPLICIT | FIELD);
        isExplicit(true,  28, MIXED | EXPLICIT | PROPERTY);
    }

    @Test
    public void testMixed() {
        isMixed(false,  0, UNKNOWN);
        isMixed(false,  2, FIELD);
        isMixed(false,  4, PROPERTY);
        isMixed(false, 10, EXPLICIT | FIELD);
        isMixed(false, 12, EXPLICIT | PROPERTY);
        isMixed(false, 14, EXPLICIT | FIELD | PROPERTY);
        isMixed(true,  26, MIXED | EXPLICIT | FIELD);
        isMixed(true,  28, MIXED | EXPLICIT | PROPERTY);
    }

    @Test
    public void testCompatibleField() {
        assertCompatible(EXPLICIT|FIELD, PROPERTY, MIXED|EXPLICIT|FIELD);
        assertCompatible(EXPLICIT|FIELD, FIELD, EXPLICIT|FIELD);
        assertCompatible(EXPLICIT|PROPERTY, PROPERTY, EXPLICIT|PROPERTY);
        assertCompatible(EXPLICIT|PROPERTY, FIELD, MIXED|EXPLICIT|PROPERTY);

        assertNotCompatible(FIELD, PROPERTY);
        assertCompatible(FIELD, FIELD, FIELD);
        assertCompatible(PROPERTY, PROPERTY, PROPERTY);
        assertNotCompatible(PROPERTY, FIELD);
    }

    void assertCompatible(int cCode, int fCode) {
        assertCompatibility(true, cCode, fCode, cCode);
    }

    void assertNotCompatible(int cCode, int fCode) {
        assertCompatibility(false, cCode, fCode, cCode);
    }

    void assertCompatible(int cCode, int fCode, int tCode) {
        assertCompatibility(true, cCode, fCode, tCode);
    }

    void assertCompatibility(boolean flag, int cCode, int fCode, int tCode) {
        if (flag) {
            assertEquals(tCode, AccessCode.mergeFieldCode(cCode, fCode));
        } else {
            try {
                AccessCode.mergeFieldCode(cCode, fCode);
                fail();
            } catch (IllegalStateException e) {

            }
        }
    }

    @Test
    public void testToString() {
        assertEquals("explicit property access", AccessCode.toClassString(12));
    }

    void isValidClassCode(boolean flag, int v, int valid) {
        assertEquals(v, valid);
        if (flag)
            assertTrue(AccessCode.isValidClassCode(valid),
            "Invalid Class Code: " +
            AccessCode.toClassString(valid));
        else
            assertFalse(AccessCode.isValidClassCode(valid),
            "Wrong Valid Class Code: " +
            AccessCode.toClassString(valid));
    }

    void isValidFieldCode(boolean flag, int v, int valid) {
        assertEquals(v, valid);
        if (flag)
            assertTrue(AccessCode.isValidFieldCode(valid),
            "Invalid Field Code: " +
            AccessCode.toFieldString(valid));
        else
            assertFalse(AccessCode.isValidFieldCode(valid),
            "Wrong Field Class Code: " +
            AccessCode.toFieldString(valid));
    }

    void isProperty(boolean flag, int v, int valid) {
        assertEquals(v, valid);
        if (flag)
            assertTrue(AccessCode.isProperty(valid));
        else
            assertFalse(AccessCode.isProperty(valid));
    }

    void isField(boolean flag, int v, int valid) {
        assertEquals(v, valid);
        if (flag)
            assertTrue(AccessCode.isField(valid));
        else
            assertFalse(AccessCode.isField(valid));
    }

    void isExplicit(boolean flag, int v, int valid) {
        assertEquals(v, valid);
        if (flag)
            assertTrue(AccessCode.isExplicit(valid));
        else
            assertFalse(AccessCode.isExplicit(valid));
    }

    void isMixed(boolean flag, int v, int valid) {
        assertEquals(v, valid);
        if (flag)
            assertTrue(AccessCode.isMixed(valid));
        else
            assertFalse(AccessCode.isMixed(valid));
    }
}
