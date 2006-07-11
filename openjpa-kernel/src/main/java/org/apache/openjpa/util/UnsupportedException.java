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
package org.apache.openjpa.util;

/**
 * Exception type for attempts to perform unsupported operations.
 *
 * @author Marc Prud'hommeaux
 * @since 2.5
 */
public class UnsupportedException extends OpenJPAException {

    public UnsupportedException() {
        setFatal(true);
    }

    public UnsupportedException(String msg) {
        super(msg);
        setFatal(true);
    }

    public UnsupportedException(Throwable cause) {
        super(cause);
        setFatal(true);
    }

    public UnsupportedException(String msg, Throwable cause) {
        super(msg, cause);
        setFatal(true);
    }

    public int getType() {
        return UNSUPPORTED;
    }
}
