/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.util;

import org.apache.commons.lang.exception.*;


/**
 *  <p>Exception type for parse errors.</p>
 *
 *  @author Abe White
 *  @since 4.0
 *  @nojavadoc */
public class ParseException extends NestableRuntimeException {
    public ParseException() {
    }

    public ParseException(String msg) {
        super(msg);
    }

    public ParseException(Throwable cause) {
        super(cause);
    }

    public ParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
