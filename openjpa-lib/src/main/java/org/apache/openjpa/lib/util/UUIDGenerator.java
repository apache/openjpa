/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Random;

import org.apache.commons.lang.exception.NestableRuntimeException;

/**
 * UUID value generator.  Based on the time-based generator in the Apache
 * Commons Id project:  http://jakarta.apache.org/commons/sandbox/id/uuid.html
 *
 * The code has been vastly simplified and modified to replace the ethernet
 * address of the host machine with the IP, since we do not want to require
 * native libs and Java cannot access the MAC address directly.
 *
 * In spirit, implements the IETF UUID draft specification, found here:<br />
 * http://www1.ics.uci.edu/~ejw/authoring/uuid-guid/draft-leach-uuids-guids-01
 * .txt
 *
 * @author Abe White, Kevin Sutter
 * @nojavadoc
 * @since 0.3.3
 */
public class UUIDGenerator {

    // indexes within the uuid array for certain boundaries
    private static final byte IDX_TIME_HI = 6;
    private static final byte IDX_TYPE = 6; // multiplexed
    private static final byte IDX_TIME_MID = 4;
    private static final byte IDX_TIME_LO = 0;
    private static final byte IDX_TIME_SEQ = 8;
    private static final byte IDX_VARIATION = 8; // multiplexed

    // indexes and lengths within the timestamp for certain boundaries
    private static final byte TS_TIME_LO_IDX = 4;
    private static final byte TS_TIME_LO_LEN = 4;
    private static final byte TS_TIME_MID_IDX = 2;
    private static final byte TS_TIME_MID_LEN = 2;
    private static final byte TS_TIME_HI_IDX = 0;
    private static final byte TS_TIME_HI_LEN = 2;

    // offset to move from 1/1/1970, which is 0-time for Java, to gregorian
    // 0-time 10/15/1582, and multiplier to go from 100nsec to msec units
    private static final long GREG_OFFSET = 0x01b21dd213814000L;
    private static final long MILLI_MULT = 10000L;

    // type of UUID -- time based
    private final static byte TYPE_TIME_BASED = 0x10;

    // random number generator used to reduce conflicts with other JVMs, and
    // hasher for strings.  note that secure random is very slow the first time
    // it is used; consider switching to a standard random
    private static final Random RANDOM = new SecureRandom();

    // 4-byte IP address + 2 random bytes to compensate for the fact that
    // the MAC address is usually 6 bytes
    private static final byte[] IP;

    // counter is initialized to 0 and is incremented for each uuid request
    // within the same timestamp window.
    private static int _counter;

    // current timestamp (used to detect multiple uuid requests within same
    // timestamp)
    private static long _currentMillis;

    // last used millis time, and a semi-random sequence that gets reset
    // when it overflows
    private static long _lastMillis = 0L;
    private static final int MAX_14BIT = 0x3FFF;
    private static short _seq = (short)RANDOM.nextInt(MAX_14BIT);

    /*
     * Static initializer to get the IP address of the host machine.
     */
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
    }

    /**
     * Return a unique UUID value.
     */
    public static byte[] next() {
        // set ip addr
        byte[] uuid = new byte[16];
        System.arraycopy(IP, 0, uuid, 10, IP.length);

        // Set time info.  Have to do this processing within a synchronized
        // block because of the statics...
        long now = 0;
        synchronized (UUIDGenerator.class) {
            // Get the time to use for this uuid.  This method has the side
            // effect of modifying the clock sequence, as well.
            now = getTime();

            // Insert the resulting clock sequence into the uuid
            uuid[IDX_TIME_SEQ] = (byte) ((_seq & 0x3F00) >>> 8);
            uuid[IDX_VARIATION] |= 0x80;
            uuid[IDX_TIME_SEQ+1] = (byte) (_seq & 0xFF);

        }

        // have to break up time because bytes are spread through uuid
        byte[] timeBytes = Bytes.toBytes(now);

        // Copy time low
        System.arraycopy(timeBytes, TS_TIME_LO_IDX, uuid, IDX_TIME_LO,
                TS_TIME_LO_LEN);
        // Copy time mid
        System.arraycopy(timeBytes, TS_TIME_MID_IDX, uuid, IDX_TIME_MID,
                TS_TIME_MID_LEN);
        // Copy time hi
        System.arraycopy(timeBytes, TS_TIME_HI_IDX, uuid, IDX_TIME_HI,
                TS_TIME_HI_LEN);
        //Set version (time-based)
        uuid[IDX_TYPE] |= TYPE_TIME_BASED; // 0001 0000

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
     * Get the timestamp to be used for this uuid.  Must be called from
     * a synchronized block.
     *
     * @return long timestamp
     */
    private static long getTime() {
        long newTime = getUUIDTime();
        if (newTime <= _lastMillis) {
            incrementSequence();
        }
        _lastMillis = newTime;
        return newTime;
    }

    /**
     * Gets the appropriately modified timestamep for the UUID.  Must be called
     * from a synchronized block.
     *
     * @return long timestamp in 100ns intervals since the Gregorian change
     * offset
     */
    private static long getUUIDTime() {
        if (_currentMillis != System.currentTimeMillis()) {
            _currentMillis = System.currentTimeMillis();
            _counter = 0;  // reset counter
        }

        // check to see if we have created too many uuid's for this timestamp
        if (_counter + 1 >= MILLI_MULT) {
            // Original algorithm threw exception.  Seemed like overkill.
            // Let's just increment the timestamp instead and start over...
            _currentMillis++;
            _counter = 0;
        }

        // calculate time as current millis plus offset times 100 ns ticks
        long currentTime = (_currentMillis + GREG_OFFSET) * MILLI_MULT;

        // return the uuid time plus the artificial tick counter incremented
        return currentTime + _counter++;
    }

    /**
     * Increments the clock sequence for this uuid.  Must be called from a
     * synchronized block.
     */
    private static void incrementSequence() {
        // increment, but if it's greater than its 14-bits, reset it
        if (++_seq > MAX_14BIT) {
            _seq = (short)RANDOM.nextInt(MAX_14BIT);  // semi-random
        }
    }


}
