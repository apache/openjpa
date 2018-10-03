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
 * Represents a hollow instance that exists in the data store, but whose
 * state has not been loaded and who is not active in the current transaction
 * (if any). This may be the state of instances returned on ID lookups or by
 * traversing relations, and the state of persistent instances outside of
 * transactions (unless the retainValues flag is set in the Broker).
 *
 * @author Abe White
 */
class HollowState extends PCState {
    private static final long serialVersionUID = 1L;

    @Override
    void initialize(StateManagerImpl context, PCState previous) {
        context.clearFields();
        context.clearSavedFields();
        context.setDirty(false);
    }

    @Override
    PCState delete(StateManagerImpl context) {
        context.preDelete();
        return PDELETED;
    }

    @Override
    PCState transactional(StateManagerImpl context) {
        return PCLEAN;
    }

    @Override
    PCState release(StateManagerImpl context) {
        return TRANSIENT;
    }

    @Override
    PCState beforeRead(StateManagerImpl context, int field) {
        return PCLEAN;
    }

    @Override
    PCState beforeOptimisticRead(StateManagerImpl context, int field) {
        return PNONTRANS;
    }

    @Override
    PCState beforeNontransactionalRead(StateManagerImpl context, int field) {
        return PNONTRANS;
    }

    @Override
    PCState beforeWrite(StateManagerImpl context, int field, boolean mutate) {
        return PDIRTY;
    }

    @Override
    PCState beforeOptimisticWrite(StateManagerImpl context, int field,
        boolean mutate) {
        return PDIRTY;
    }

    @Override
    PCState beforeNontransactionalWrite(StateManagerImpl context, int field,
        boolean mutate) {
        return PNONTRANS;
    }

    @Override
    boolean isPersistent() {
        return true;
    }

    @Override
    public String toString() {
        return "Hollow";
    }
}

