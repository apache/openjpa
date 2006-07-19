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
 *  Represents a transient instance that is managed by a StateManager and
 * may be participating in the current	transaction, but has not yet been
 * modified.
 *
 * @author Abe White
 */
class TCleanState
    extends PCState {

    void initialize(StateManagerImpl context) {
        context.clearSavedFields();
        context.setLoaded(true);
        context.setDirty(false);

        // need to replace the second class objects with proxies that
        // listen for dirtying so we can track changes to these objects
        context.proxyFields(true, false);
    }

    PCState persist(StateManagerImpl context) {
        return (context.getBroker().isActive()) ? PNEW : PNONTRANSNEW;
    }

    PCState delete(StateManagerImpl context) {
        return error("transient", context);
    }

    PCState nontransactional(StateManagerImpl context) {
        return TRANSIENT;
    }

    PCState beforeWrite(StateManagerImpl context, int field, boolean mutate) {
        return TDIRTY;
    }

    PCState beforeOptimisticWrite(StateManagerImpl context, int field,
        boolean mutate) {
        return TDIRTY;
    }
}

