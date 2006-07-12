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
 * <p>State restore constants.</p>
 *
 * @author Abe White
 * @since 4.0
 */
public interface RestoreState {

    /**
     * Do not restore any state on rollback.
     */
    public static final int RESTORE_NONE = 0;

    /**
     * Restore immutable state on rollback; clear mutable state.
     */
    public static final int RESTORE_IMMUTABLE = 1;

    /**
     *	Restore all state on rollback.
     */
    public static final int RESTORE_ALL = 2;
}
