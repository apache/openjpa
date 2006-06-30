/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import org.apache.commons.lang.exception.*;

/**
 * UUID value generator. Based on the time-based generator in the LGPL
 * project:<br /> http://www.doomdark.org/doomdark/proj/jug/<br />
 * The code has been vastly simplified and modified to replace the ethernet
 * address of the host machine with the IP, since we do not want to require
 * native libs and Java cannot access the MAC address directly.
 *  Aside from the above modification, implements the IETF UUID draft
 * specification, found here: http://www1.ics.uci.edu/~ejw/authoring/uuid-guid/
 * draft-leach-uuids-guids-01.txt
 * 
 * @author Abe White
 * @since 3.3
 * @nojavadoc
 */
public class UUIDGenerator {
    // indexes within the uuid array for certain boundaries
    private static final byte IDX_TIME_HI = 6;
    private static final byte IDX_TYPE = 6; // multiplexed
    private static final byte IDX_TIME_MID = 4;
    private static final byte IDX_TIME_LO = 0;
    private static final byte IDX_TIME_SEQ = 8;
    private static final byte IDX_VARIATION = 8; // multiplexed

    // offset to move from 1/1/1970, which is 0-time for Java, to gregorian
    // 0-time 10/15/1582, and multiplier to go from 100nsec to msec units
    private static final long GREG_OFFSET = 0x01b21dd213814000L;
    private static final long MILLI_MULT = 10000L;

    // type of UUID; is this part of the spec?
    private final static byte TYPE_TIME_BASED = 1;

    // random number generator used to reduce conflicts with other JVMs, and
    // hasher for strings.  note that secure random is very slow the first time
    // it is used; consider switching to a standard random
    private static final Random RANDOM = new SecureRandom();

    // 4-byte IP address + 2 random bytes to compensate for the fact that
    // the MAC address is usually 6 bytes
    private static final byte[] IP;

    // counter is initialized not to 0 but to a random 8-bit number, and each
    // time clock changes, lowest 8-bits of counter are preserved. the purpose
    // is to reduce chances of multi-JVM collisions without reducing perf
    // awhite: I don't really understand this precaution, but it was in the
    // original algo
    private static int _counter;

    // last used millis time, and a randomized sequence that gets reset
    // whenever the time is reset
    private static long _last = 0L;
    private static byte[] _seq = new byte[2];

    static {
        byte[] ip = null;
        try {
            ip = InetAddress.getLocalHost().getAddress();
        } catch (IOException ioe) {
            throw new NestableRuntimeException(ioe);
        }

        IP = new byte[6];
        RANDOM.nextBytes(IP);
        System.arraycopy(ip, 0, IP, 2, ip.length);

        resetTime();
    }

    /**
     * Return a unique UUID value.
     */
    public static byte[] next() {
        // set ip addr
        byte[] uuid = new byte[16];
        System.arraycopy(IP, 0, uuid, 10, IP.length);

        // set time info
        long now = System.currentTimeMillis();
        synchronized (UUIDGenerator.class) {
            // if time moves backwards somehow, spec says to reset randomization
            if (now < _last)
                resetTime();
            else if (now == _last && _counter == MILLI_MULT) {
                // if we run out of slots in this milli, increment
                now++;
                _last = now;
                _counter &= 0xFF; // rest counter?
            } else if (now > _last) {
                _last = now;
                _counter &= 0xFF; // rest counter?
            }

            // translate timestamp to 100ns slot since beginning of gregorian
            now *= MILLI_MULT;
            now += GREG_OFFSET;

            // add nano slot
            now += _counter;
            _counter++; // increment counter

            // set random info
            for (int i = 0; i < _seq.length; i++)
                uuid[IDX_TIME_SEQ + i] = _seq[i];
        }

        // have to break up time because bytes are spread through uuid
        int timeHi = (int) (now >>> 32);
        int timeLo = (int) now;

        uuid[IDX_TIME_HI] = (byte) (timeHi >>> 24);
        uuid[IDX_TIME_HI + 1] = (byte) (timeHi >>> 16);
        uuid[IDX_TIME_MID] = (byte) (timeHi >>> 8);
        uuid[IDX_TIME_MID + 1] = (byte) timeHi;

        uuid[IDX_TIME_LO] = (byte) (timeLo >>> 24);
        uuid[IDX_TIME_LO + 1] = (byte) (timeLo >>> 16);
        uuid[IDX_TIME_LO + 2] = (byte) (timeLo >>> 8);
        uuid[IDX_TIME_LO + 3] = (byte) timeLo;

        // set type info
        uuid[IDX_TYPE] &= (byte) 0x0F;
        uuid[IDX_TYPE] |= (byte) (TYPE_TIME_BASED << 4);
        uuid[IDX_VARIATION] &= 0x3F;
        uuid[IDX_VARIATION] |= 0x80;

        return uuid;
    }

    /**
     * Return the next unique uuid value as a 16-character string.
     */
    public static String nextString() {
        byte[] bytes = next();
        try {
            return new String(bytes, "ISO-8859-1");
        } catch (Exception e) {
            return new String(bytes);
        }
    }

    /**
     * Return the next unique uuid value as a 32-character hex string.
     */
    public static String nextHex() {
        return Base16Encoder.encode(next());
    }

    /**
     * Reset the random time sequence and counter. Must be called from
     * synchronized code.
     */
    private static void resetTime() {
        _last = 0L;
        RANDOM.nextBytes(_seq);

        // awhite: I don't understand this; copied from original algo
        byte[] tmp = new byte[1];
        RANDOM.nextBytes(tmp);
        _counter = tmp[0] & 0xFF;
    }
}
