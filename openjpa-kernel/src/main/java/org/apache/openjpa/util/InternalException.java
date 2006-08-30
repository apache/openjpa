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

import org.apache.openjpa.lib.util.Localizer.Message;

/**
 * Exception type for internal errors.
 *
 * @author Abe White
 * @since 0.3.2
 */
public class InternalException
    extends OpenJPAException {

    public InternalException() {
        setFatal(true);
    }

    public InternalException(String msg) {
        super(msg);
        setFatal(true);
    }

    public InternalException(Message msg) {
        super(msg);
        setFatal(true);
    }

    public InternalException(Throwable cause) {
        super(cause);
        setFatal(true);
    }

    public InternalException(String msg, Throwable cause) {
        super(msg, cause);
        setFatal(true);
    }

    public InternalException(Message msg, Throwable cause) {
        super(msg, cause);
        setFatal(true);
    }

    public int getType() {
        return INTERNAL;
    }
}
