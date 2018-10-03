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
package org.apache.openjpa.util;

/**
 * Interface supplying additional exception information. All OpenJPA
 * exceptions implement this interface.
 *
 * @author Abe White
 * @since 0.4.0
 */
public interface ExceptionInfo {

    int GENERAL = 0;
    int INTERNAL = 1;
    int STORE = 2;
    int UNSUPPORTED = 3;
    int USER = 4;
    int WRAPPED = 5;
    int UNAVAILABLE = 6;


    /**
     * Exception message.
     *
     * @see Throwable#getMessage
     */
    String getMessage();

    /**
     * Returns the first {@link Throwable} from {@link #getNestedThrowables}
     * in order to conform to {@link Throwable#getCause} in Java 1.4+.
     *
     * @see Throwable#getCause
     */
    Throwable getCause();

    /**
     * Stack.
     *
     * @see Throwable#printStackTrace
     */
    void printStackTrace();

    /**
     * Exception type.
     */
    int getType();

    /**
     * Exception subtype.
     */
    int getSubtype();

    /**
     * Whether this error is fatal.
     */
    boolean isFatal();

    /**
     * The nested throwables.
     */
    Throwable[] getNestedThrowables();

    /**
     * The failed object.
     */
    Object getFailedObject();
}

