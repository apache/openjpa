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
package org.apache.openjpa.persistence.jdbc.common.apps;

import java.io.Serializable;

/**
 * Auto-generated by:
 * org.apache.openjpa.enhance.ApplicationIdTool
 */
public class ByteArrayPKPCId implements Serializable {

    
    private static final long serialVersionUID = 1L;
    private static final char[] HEX = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    static {
        // register persistent class in JVM
        Class c = ByteArrayPKPC.class;
    }

    public byte[] pk;

    public ByteArrayPKPCId() {
    }

    public ByteArrayPKPCId(String fromString) {
        pk = toBytes(fromString);
    }

    @Override
    public String toString() {
        return toString(pk);
    }

    @Override
    public int hashCode() {
        return hashCode(pk);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ByteArrayPKPCId))
            return false;

        ByteArrayPKPCId other = (ByteArrayPKPCId) obj;
        return (equals(pk, other.pk));
    }

    private static byte[] toBytes(String s) {
        if ("null".equals(s))
            return null;

        int len = s.length();
        byte[] r = new byte[len / 2];
        for (int i = 0; i < r.length; i++) {
            int digit1 = s.charAt(i * 2), digit2 = s.charAt(i * 2 + 1);
            if (digit1 >= '0' && digit1 <= '9')
                digit1 -= '0';
            else if (digit1 >= 'A' && digit1 <= 'F')
                digit1 -= 'A' - 10;
            if (digit2 >= '0' && digit2 <= '9')
                digit2 -= '0';
            else if (digit2 >= 'A' && digit2 <= 'F')
                digit2 -= 'A' - 10;

            r[i] = (byte) ((digit1 << 4) + digit2);
        }
        return r;
    }

    private static String toString(byte[] b) {
        if (b == null)
            return "null";

        StringBuilder r = new StringBuilder(b.length * 2);
        for (byte value : b)
            for (int j = 1; j >= 0; j--)
                r.append(HEX[(value >> (j * 4)) & 0xF]);
        return r.toString();
    }

    private static boolean equals(byte[] b1, byte[] b2) {
        if (b1 == null && b2 == null)
            return true;
        if (b1 == null || b2 == null)
            return false;
        if (b1.length != b2.length)
            return false;
        for (int i = 0; i < b1.length; i++)
            if (b1[i] != b2[i])
                return false;
        return true;
    }

    private static int hashCode(byte[] b) {
        if (b == null)
            return 0;
        int sum = 0;
        for (byte value : b) sum += value;
        return sum;
    }
}
