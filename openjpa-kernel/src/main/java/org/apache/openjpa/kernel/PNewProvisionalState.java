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
 * Represents an instance that was made persistent via reachability within the
 * current	transaction.
 *
 * @author Steve Kim
 * @author Abe White
 */
class PNewProvisionalState extends PCState {
    private static final long serialVersionUID = 1L;

    @Override
    void initialize(StateManagerImpl context, PCState previous) {
        context.setLoaded(true);
        context.setDirty(true);
        context.saveFields(false);
    }

    @Override
    PCState persist(StateManagerImpl context) {
        return PNEW;
    }

    @Override
    PCState nonprovisional(StateManagerImpl context, boolean logical,
        OpCallbacks call) {
        context.preFlush(logical, call);
        return PNEW;
    }

    @Override
    PCState commit(StateManagerImpl context) {
        return TRANSIENT;
    }

    @Override
    PCState commitRetain(StateManagerImpl context) {
        return TRANSIENT;
    }

    @Override
    PCState rollback(StateManagerImpl context) {
        return TRANSIENT;
    }

    @Override
    PCState rollbackRestore(StateManagerImpl context) {
        context.restoreFields();
        return TRANSIENT;
    }

    @Override
    PCState delete(StateManagerImpl context) {
        context.preDelete();
        return TRANSIENT;
    }

    @Override
    PCState release(StateManagerImpl context) {
        return TRANSIENT;
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
    boolean isNew() {
        return true;
    }

    @Override
    boolean isDirty() {
        return true;
    }

    @Override
    boolean isProvisional() {
        return true;
    }

    @Override
    public String toString() {
        return "Persistent-New-Provisional";
    }
}
