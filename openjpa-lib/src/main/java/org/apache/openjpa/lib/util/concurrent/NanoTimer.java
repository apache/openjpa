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
/*
 * Written by Dawid Kurzyniec and released to the public domain, as explained
 * at http://creativecommons.org/licenses/publicdomain
 */
package org.apache.openjpa.lib.util.concurrent;

/**
 * Interface to specify custom implementation of precise timer.
 *
 * @author Dawid Kurzyniec
 * @version 1.0
 */
public interface NanoTimer {

    /**
     * Returns the current value of the most precise available system timer,
     * in nanoseconds. This method can only be used to measure elapsed time and
     * is not related to any other notion of system or wall-clock time. The
     * value returned represents nanoseconds since some fixed but arbitrary
     * time(perhaps in the future, so values may be negative). This method
     * provides nanosecond precision, but not necessarily nanosecond accuracy.
     * No guarantees are made about how frequently values change. Differences
     * in successive calls that span greater than approximately 292 years
     * (263 nanoseconds) will not accurately compute elapsed time due to
     * numerical overflow.
     *
     * @return The current value of the system timer, in nanoseconds.
     */
    long nanoTime();
}
