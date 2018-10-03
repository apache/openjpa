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
 * Represents a transient instance that is participating in the current
 * transaction, and has been modified.
 *
 * @author Abe White
 */
class TDirtyState extends PCState {
    private static final long serialVersionUID = 1L;

    @Override
    void initialize(StateManagerImpl context, PCState previous) {
        context.saveFields(false);
        context.setLoaded(true);
        context.setDirty(true);
    }

    @Override
    PCState commit(StateManagerImpl context) {
        return TCLEAN;
    }

    @Override
    PCState commitRetain(StateManagerImpl context) {
        return TCLEAN;
    }

    @Override
    PCState rollback(StateManagerImpl context) {
        return TCLEAN;
    }

    @Override
    PCState rollbackRestore(StateManagerImpl context) {
        context.restoreFields();
        return TCLEAN;
    }

    @Override
    PCState persist(StateManagerImpl context) {
        return (context.getBroker().isActive()) ? PNEW : PNONTRANSNEW;
    }

    @Override
    PCState delete(StateManagerImpl context) {
        return error("transient", context);
    }

    @Override
    PCState nontransactional(StateManagerImpl context) {
        return error("dirty", context);
    }

    @Override
    boolean isTransactional() {
        return true;
    }

    @Override
    boolean isDirty() {
        return true;
    }

    @Override
    public String toString() {
        return "Transient-Dirty";
    }
}

