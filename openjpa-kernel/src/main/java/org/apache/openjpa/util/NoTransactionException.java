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
 * Exception type thrown when attempting a transactional operation without
 * an active transaction.
 *
 * @since 4.0
 * @author Abe White
 */
public class NoTransactionException
    extends InvalidStateException {

    public NoTransactionException() {
    }

    public NoTransactionException(String msg) {
        super(msg);
    }

    public NoTransactionException(String msg, Object failed) {
        super(msg);
        setFailedObject(failed);
    }

    public int getSubtype() {
        return NO_TRANSACTION;
    }
}
