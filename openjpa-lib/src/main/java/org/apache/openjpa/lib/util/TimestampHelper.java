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
package org.apache.openjpa.lib.util;

import java.sql.Timestamp;

/**
 * Helper base class attempts to return java.sql.Timestamp object with
 * nanosecond precision. This base class is created to allow JDK 1.4 maven build
 * and only implements millisecond precision.
 * 
 * @author Albert Lee
 */
public class TimestampHelper {

    // number of millisecond, mircoseconds and nanoseconds in one second.
    protected static final long MilliMuliplier = 1000L;
    protected static final long MicroMuliplier = MilliMuliplier * 1000L;
    protected static final long NanoMuliplier = MicroMuliplier * 1000L;

    private static TimestampHelper instance = null;
    
    static {
        if (JavaVersions.VERSION >= 5) {
            try {
                Class timestamp5HelperClass = Class
                    .forName("org.apache.openjpa.lib.util.Timestamp5Helper");
                instance = (TimestampHelper) timestamp5HelperClass
                    .newInstance();
            } catch (Throwable e) {
                instance = new TimestampHelper();
            }
        } else {
            instance = new TimestampHelper();
        }
    }

    /*
     * Return a java.sql.Timestamp object of current time.
     */
    public static Timestamp getNanoPrecisionTimestamp() {
        return instance.getTimestamp();
    }
    
    /*
     * This class implements a millisecond precision Timestamp.
     */
    protected Timestamp getTimestamp() { 
        return new Timestamp(System.currentTimeMillis());
    }
}
