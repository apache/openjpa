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
 * Represents an embedded instance that is managed by a StateManager and
 * may be participating in the current	transaction, but has not yet been
 * modified.
 *
 * @author Abe White
 */
class ECleanState extends PCState {
    private static final long serialVersionUID = 1L;

    @Override
    void initialize(StateManagerImpl context, PCState previous) {
        context.setDirty(false);
    }

    @Override
    PCState commit(StateManagerImpl context) {
        return TRANSIENT;
    }

    @Override
    PCState commitRetain(StateManagerImpl context) {
        return ENONTRANS;
    }

    @Override
    PCState rollback(StateManagerImpl context) {
        return TRANSIENT;
    }

    @Override
    PCState rollbackRestore(StateManagerImpl context) {
        return ENONTRANS;
    }

    @Override
    PCState delete(StateManagerImpl context) {
        context.preDelete();
        return EDELETED;
    }

    @Override
    PCState nontransactional(StateManagerImpl context) {
        return ENONTRANS;
    }

    @Override
    PCState release(StateManagerImpl context) {
        return TRANSIENT;
    }

    @Override
    PCState evict(StateManagerImpl context) {
        return TRANSIENT;
    }

    @Override
    PCState beforeWrite(StateManagerImpl context, int field, boolean mutate) {
        return EDIRTY;
    }

    @Override
    PCState beforeOptimisticWrite(StateManagerImpl context, int field,
        boolean mutate) {
        return EDIRTY;
    }

    @Override
    boolean isTransactional() {
        return true;
    }

    @Override
    boolean isPersistent() {
        return true;
    }

    @Override
    public String toString() {
        return "Embedded-Clean";
    }
}

