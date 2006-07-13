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
 *  Represents a persistent instance that is participating in the current
 * transaction, but has not yet been modified.
 *
 * @author Abe White
 */
class PCleanState
    extends PCState {

    void initialize(StateManagerImpl context) {
        context.setDirty(false);
    }

    PCState commit(StateManagerImpl context) {
        return HOLLOW;
    }

    PCState commitRetain(StateManagerImpl context) {
        return PNONTRANS;
    }

    PCState rollback(StateManagerImpl context) {
        return HOLLOW;
    }

    PCState rollbackRestore(StateManagerImpl context) {
        return PNONTRANS;
    }

    PCState delete(StateManagerImpl context) {
        context.preDelete();
        return PDELETED;
    }

    PCState nontransactional(StateManagerImpl context) {
        return PNONTRANS;
    }

    PCState release(StateManagerImpl context) {
        return TRANSIENT;
    }

    PCState evict(StateManagerImpl context) {
        return HOLLOW;
    }

    PCState beforeWrite(StateManagerImpl context, int field, boolean mutate) {
        return PDIRTY;
    }

    PCState beforeOptimisticWrite(StateManagerImpl context, int field,
        boolean mutate) {
        return PDIRTY;
    }

    boolean isTransactional() {
        return true;
    }

    boolean isPersistent() {
        return true;
    }
}

