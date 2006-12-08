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
package org.apache.openjpa.util;

import java.util.Collection;
import java.util.Comparator;

/**
 * Interface implemented by all proxy collection types.
 *
 * @author Abe White
 */
public interface ProxyCollection 
    extends Proxy, Collection {

    /**
     * The collection element type.
     */
    public Class getElementType();

    /**
     * Create a new instance of this proxy type.
     */
    public ProxyCollection newInstance(Class elementType, Comparator comp,
        boolean trackChanges);
}
