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
package org.apache.openjpa.event;

/**
 * Listener for when state is loaded into a persistent instnace.
 *
 * @author Steve Kim
 * @author Abe White
 */
public interface LoadListener {

    /**
     * Invoked after state has been loaded into the instance.
     */
    public void afterLoad(LifecycleEvent event);

    /**
     * Invoked after state has been refreshed.
     */
    public void afterRefresh(LifecycleEvent event);
}
