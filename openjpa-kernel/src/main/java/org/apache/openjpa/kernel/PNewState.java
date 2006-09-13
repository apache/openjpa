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
 * Represents an instance that was made persistent within the
 * current	transaction.
 *
 * @author Abe White
 */
class PNewState
    extends PCState {

    void initialize(StateManagerImpl context) {
        context.setLoaded(true);
        context.setDirty(true);
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
        return TRANSIENT;
    }

    PCState rollbackRestore(StateManagerImpl context) {
        context.restoreFields();
        return TRANSIENT;
    }

    PCState delete(StateManagerImpl context) {
        context.preDelete();
        if (context.isFlushed())
            return PNEWFLUSHEDDELETED;
        return PNEWDELETED;
    }

    PCState nontransactional(StateManagerImpl context) {
        return error("new", context);
    }

    PCState release(StateManagerImpl context) {
        return error("new", context);
    }

    boolean isTransactional() {
        return true;
    }

    boolean isPersistent() {
        return true;
    }

    boolean isNew() {
        return true;
    }

    boolean isDirty() {
        return true;
    }
}
