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
 * Represents a persistent instance that has been dirtied outside of
 * transaction. This state is only reachable only if the
 * RetainNontransactional property is set.
 *
 * @author Steve Kim
 */
class PNonTransDirtyState extends PCState {
    private static final long serialVersionUID = 1L;

    @Override
    void initialize(StateManagerImpl context, PCState previous) {
        context.saveFields(false);
    }

    @Override
    PCState delete(StateManagerImpl context) {
        context.preDelete();
        return PNONTRANSDELETED;
    }

    @Override
    PCState transactional(StateManagerImpl context) {
        return PDIRTY;
    }

    @Override
    PCState release(StateManagerImpl context) {
        return error("dirty", context);
    }

    @Override
    PCState evict(StateManagerImpl context) {
        return HOLLOW;
    }

    @Override
    PCState afterNontransactionalRefresh() {
        return PNONTRANS;
    }

    @Override
    boolean isPendingTransactional() {
        return true;
    }

    @Override
    boolean isPersistent() {
        return true;
    }

    @Override
    boolean isDirty() {
        return true;
    }

    @Override
    public String toString() {
        return "Persistent-Notransactional-Dirty";
    }
}

