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
 * Represents a persistent instance that has been deleted in the current
 * transaction.
 *
 * @author Abe White
 */
class PDeletedState
    extends PCState {

    void initialize(StateManagerImpl context) {
        context.saveFields(false);
    }

    PCState flush(StateManagerImpl context) {
        return PDELETEDFLUSHED;
    }

    PCState commit(StateManagerImpl context) {
        context.clearFields();
        return TRANSIENT;
    }

    PCState commitRetain(StateManagerImpl context) {
        context.clearFields();
        return TRANSIENT;
    }

    PCState rollback(StateManagerImpl context) {
        return HOLLOW;
    }

    PCState rollbackRestore(StateManagerImpl context) {
        context.restoreFields();
        return PNONTRANS;
    }

    PCState nontransactional(StateManagerImpl context) {
        return error("deleted", context);
    }

    PCState persist(StateManagerImpl context) {
        return (context.getDirty().length() > 0) ? PDIRTY : PCLEAN;
    }

    PCState release(StateManagerImpl context) {
        return error("deleted", context);
    }

    PCState beforeWrite(StateManagerImpl context, int field, boolean mutate) {
        return error("deleted", context);
    }

    PCState beforeOptimisticWrite(StateManagerImpl context, int field,
        boolean mutate) {
        return error("deleted", context);
    }

    boolean isTransactional() {
        return true;
    }

    boolean isPersistent() {
        return true;
    }

    boolean isDeleted() {
        return true;
    }

    boolean isDirty() {
        return true;
    }
}
