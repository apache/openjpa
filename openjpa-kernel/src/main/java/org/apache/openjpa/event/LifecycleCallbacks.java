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
 * A lifecycle listener that responds to callbacks rather than events.
 *
 * @author Steve Kim
 */
public interface LifecycleCallbacks {

    /**
     * Return whether the given instance has a callback for the given
     * event type.
     */
    public boolean hasCallback(Object obj, int eventType);

    /**
     * Invoke the callback for the given event type on the given instance.
     */
    public void makeCallback(Object obj, Object related, int eventType)
        throws Exception;
}
