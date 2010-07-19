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

import java.security.AccessController;

/**
 * Utilities for dealing with different Java vendors.
 * 
 */
public class JavaVendors {

    static public final int VENDOR;

    static public final int OTHER = 0;
    static public final int SUN = 1;
    static public final int IBM = 2;

    static {
        String vendor = AccessController.doPrivileged(J2DoPrivHelper.getPropertyAction("java.vendor"));

        if (vendor.toUpperCase().contains("SUN MICROSYSTEMS")) {
            VENDOR = SUN;
        } else if (vendor.toUpperCase().contains("IBM")) {
            VENDOR = IBM;
        } else {
            VENDOR = OTHER;
        }
    }

    /**
     * This static worker method returns <b>true</b> if the current implementation is IBM.
     */
    public static boolean isIBM() {
        return VENDOR == IBM;
    }

    /**
     * This static worker method returns <b>true</b> if the current implementation is Sun.
     */
    public static boolean isSun() {
        return VENDOR == SUN;
    }
}
