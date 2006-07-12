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
package org.apache.openjpa.kernel;

/**
 * <p>Constants for which fields to include in the detach graph.</p>
 *
 * @author Abe White
 * @since 4.0
 */
public interface DetachState {

    /**
     * Mode to detach all fields in the current fetch groups.
     */
    public static final int DETACH_FGS = 0;

    /**
     * Mode to detach all currently-loaded fields.
     */
    public static final int DETACH_LOADED = 1;

    /**
     *	Mode to detach all fields.
     */
    public static final int DETACH_ALL = 2;
}
