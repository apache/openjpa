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
package org.apache.openjpa.util;

import java.util.Collection;
import java.util.Comparator;

import org.apache.openjpa.conf.OpenJPAConfiguration;

/**
 * <p>Interface implemented by all built-in proxies on {@link Collection}
 * types.</p>
 *
 * @author Abe White
 */
public interface ProxyCollection
    extends Proxy {

    /**
     * This method should return a new proxy of the same concrete type as the
     * implementing class.  Used by the {@link ProxyManager} factories: one
     * template instance of each type is created for the purpose of producing
     * new instances via this method.  Overcomes the performance penalties of
     * reflection.
     */
    public ProxyCollection newInstance(Class elementType, Comparator compare,
        boolean trackChanges, OpenJPAConfiguration conf);
}
