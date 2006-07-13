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
package org.apache.openjpa.util;

/**
 * Base exception for data store errors.
 *
 * @author Marc Prud'hommeaux
 * @since 2.5
 */
public class StoreException
    extends OpenJPAException {

    public static final int LOCK = 1;
    public static final int OBJECT_NOT_FOUND = 2;
    public static final int OPTIMISTIC = 3;
    public static final int REFERENTIAL_INTEGRITY = 4;
    public static final int OBJECT_EXISTS = 5;

    public StoreException() {
    }

    public StoreException(String msg) {
        super(msg);
    }

    public StoreException(Throwable cause) {
        super(cause);
    }

    public StoreException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public int getType() {
        return STORE;
    }
}
