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

/**
 * Interface implemented by all generated custom types, which use JavaBean
 * conventions for copying state.
 *
 * @author Abe White
 */
public interface ProxyBean 
    extends Proxy {

    /**
     * Create a new instance of this proxy type with the same state as the
     * given instance.
     */
    public ProxyBean newInstance(Object orig);
}
