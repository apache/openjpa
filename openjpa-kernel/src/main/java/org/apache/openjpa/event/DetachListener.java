/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.event;

/**
 * Listener for when a persistent instance is detached.
 *
 * @author Steve Kim
 */
public interface DetachListener {

    /**
     * Invoked before the instance is detached.
     */
    public void beforeDetach(LifecycleEvent event);

    /**
     * Invoked after the instance has been detached.
     */
    public void afterDetach(LifecycleEvent event);
}
