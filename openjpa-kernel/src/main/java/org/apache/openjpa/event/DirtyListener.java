/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.event;

/**
 * <p>Listener for when a persistent instance becomes dirty.</p>
 *
 * @author Steve Kim
 * @author Abe White
 */
public interface DirtyListener {

    /**
     * Invoked before the first change is applied.
     */
    public void beforeDirty(LifecycleEvent event);

    /**
     * Invoked after the first change is applied.
     */
    public void afterDirty(LifecycleEvent event);

    /**
     * Invoked before the first change is applied to a flushed instance.
     */
    public void beforeDirtyFlushed(LifecycleEvent event);

    /**
     *	Invoked after the first change is applied to a flushed instance.
     */
    public void afterDirtyFlushed(LifecycleEvent event);
}
