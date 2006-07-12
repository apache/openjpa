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
 * <p>Lifecycle state.</p>
 * <p/>
 * <p>Represents an embedded instance that has been deleted in the current
 * transaction.</p>
 *
 * @author Abe White
 */
class EDeletedState
    extends PCState {

    PCState commit(StateManagerImpl context) {
        context.clearFields();
        return TRANSIENT;
    }

    PCState commitRetain(StateManagerImpl context) {
        context.clearFields();
        return TRANSIENT;
    }

    PCState rollback(StateManagerImpl context) {
        return TRANSIENT;
    }

    PCState rollbackRestore(StateManagerImpl context) {
        context.restoreFields();
        return ENONTRANS;
    }

    PCState nontransactional(StateManagerImpl context) {
        return error("dirty", context);
    }

    PCState release(StateManagerImpl context) {
        return TRANSIENT;
    }

    boolean isTransactional() {
        return true;
    }

    public boolean isPersistent() {
        return true;
    }

    boolean isDeleted() {
        return true;
    }

    boolean isDirty() {
        return true;
    }
}

