/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.kernel;

/**
 * Lifecycle state.
 * Represents an embedded instance that is participating in the current
 * transaction, and has been modified.
 *
 * @author Abe White
 */
class EDirtyState
    extends PCState {

    void initialize(StateManagerImpl context) {
        context.saveFields(false);
    }

    void beforeFlush(StateManagerImpl context, boolean logical,
        OpCallbacks call) {
        context.preFlush(logical, call);
    }

    PCState commit(StateManagerImpl context) {
        return TRANSIENT;
    }

    PCState commitRetain(StateManagerImpl context) {
        return ENONTRANS;
    }

    PCState rollback(StateManagerImpl context) {
        return TRANSIENT;
    }

    PCState rollbackRestore(StateManagerImpl context) {
        context.restoreFields();
        return ENONTRANS;
    }

    PCState delete(StateManagerImpl context) {
        context.preDelete();
        return EDELETED;
    }

    PCState nontransactional(StateManagerImpl context) {
        return error("dirty", context);
    }

    PCState release(StateManagerImpl context) {
        return TRANSIENT;
    }

    PCState afterRefresh() {
        return ECLEAN;
    }

    PCState afterOptimisticRefresh() {
        return ENONTRANS;
    }

    boolean isTransactional() {
        return true;
    }

    public boolean isPersistent() {
        return true;
    }

    boolean isDirty() {
        return true;
    }
}

