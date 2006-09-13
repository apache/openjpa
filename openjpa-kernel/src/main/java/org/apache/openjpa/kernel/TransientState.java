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
 * Represents a transient instance; this state lasts only through
 * the {@link #initialize} method, which immediately removes the
 * persistence capable instance from management by the OpenJPA runtime.
 *
 * @author Abe White
 */
class TransientState
    extends PCState {

    void initialize(StateManagerImpl context) {
        // mark r/w ok, remove from management
        context.unproxyFields();
        context.getPersistenceCapable().pcReplaceStateManager(null);
        context.getBroker().setStateManager(context.getId(),
            context, BrokerImpl.STATUS_TRANSIENT);
    }
}
