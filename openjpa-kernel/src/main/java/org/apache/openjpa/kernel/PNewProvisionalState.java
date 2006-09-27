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
package org.apache.openjpa.kernel;

/**
 * Lifecycle state.
 * Represents an instance that was made persistent via reachability within the
 * current	transaction.
 *
 * @author Steve Kim
 * @author: Abe White
 */
class PNewProvisionalState
    extends PNewState {

    PCState persist(StateManagerImpl context) {
        return PNEW;
    }

    PCState nonprovisional() {
        return PNEW;
    }

    PCState commit(StateManagerImpl context) {
        return TRANSIENT;
    }

    PCState commitRetain(StateManagerImpl context) {
        return TRANSIENT;
    }

    boolean isProvisional() {
        return true;
    }
}
