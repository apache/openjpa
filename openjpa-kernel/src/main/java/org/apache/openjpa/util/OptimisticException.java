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

import java.util.Collection;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Exception type for optimistic concurrency violations.
 *
 * @author Marc Prud'hommeaux
 * @nojavadoc
 * @since 2.5
 */
public class OptimisticException extends StoreException {

    private static final transient Localizer _loc = Localizer.forPackage
        (OptimisticException.class);

    public OptimisticException() {
    }

    public OptimisticException(String msg) {
        super(msg);
    }

    public OptimisticException(Object failed) {
        this(_loc.get("opt-lock", Exceptions.toString(failed)));
        setFailedObject(failed);
    }

    public OptimisticException(Throwable[] nested) {
        this(_loc.get("opt-lock-nested"));
        setNestedThrowables(nested);
    }

    public OptimisticException(Collection failed, Throwable[] nested) {
        this(_loc.get("opt-lock-multi", Exceptions.toString(failed)));
        setNestedThrowables(nested);
    }

    public int getSubtype() {
        return OPTIMISTIC;
    }
}
