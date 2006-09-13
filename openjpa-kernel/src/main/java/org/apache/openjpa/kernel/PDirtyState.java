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
 * Represents a persistent instance that is participating in the current
 * transaction, and has been modified.
 *
 * @author Abe White
 */
class PDirtyState
    extends PCState {

    void initialize(StateManagerImpl context) {
        context.saveFields(false);
    }

    void beforeFlush(StateManagerImpl context, boolean logical,
        OpCallbacks call) {
        context.preFlush(logical, call);
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
        context.restoreFields();
        return PNONTRANS;
    }

    PCState delete(StateManagerImpl context) {
        context.preDelete();
        return PDELETED;
    }

    PCState nontransactional(StateManagerImpl context) {
        return error("dirty", context);
    }

    PCState release(StateManagerImpl context) {
        return error("dirty", context);
    }

    PCState afterRefresh() {
        return PCLEAN;
    }

    PCState afterOptimisticRefresh() {
        return PNONTRANS;
    }

    boolean isTransactional() {
        return true;
    }

    boolean isPersistent() {
        return true;
    }

    boolean isDirty() {
        return true;
    }
}

