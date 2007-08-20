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
package org.apache.openjpa.persistence;

import java.util.EnumSet;

import org.apache.openjpa.event.CallbackModes;

/**
 * The possible settings for the callback behavior of an
 * {@link OpenJPAEntityManager}.
 *
 * @since 1.0.0
 * @published
 */
public enum CallbackType {
    FAIL_FAST(CallbackModes.CALLBACK_FAIL_FAST),
    IGNORE(CallbackModes.CALLBACK_IGNORE),
    LOG(CallbackModes.CALLBACK_LOG),
    RETHROW(CallbackModes.CALLBACK_RETHROW),
    ROLLBACK(CallbackModes.CALLBACK_ROLLBACK);

    private final int callbackMode;

    private CallbackType(int value) {
        callbackMode = value;
    }

    public static EnumSet<CallbackType> toEnumSet(int callback) {
        EnumSet<CallbackType> types = EnumSet.noneOf(CallbackType.class);
        if ((callback & CallbackModes.CALLBACK_FAIL_FAST) != 0)
            types.add(FAIL_FAST);
        if ((callback & CallbackModes.CALLBACK_IGNORE) != 0)
            types.add(IGNORE);
        if ((callback & CallbackModes.CALLBACK_LOG) != 0)
            types.add(LOG);
        if ((callback & CallbackModes.CALLBACK_RETHROW) != 0)
            types.add(RETHROW);
        if ((callback & CallbackModes.CALLBACK_ROLLBACK) != 0)
            types.add(ROLLBACK);
        return types;
    }

    public static int fromEnumSet(EnumSet<CallbackType> types) {
        int callback = 0;
        for (CallbackType type : types)
            callback |= type.callbackMode;
        return callback;
    }
}
