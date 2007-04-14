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
package org.apache.openjpa.lib.rop;

import java.util.ListIterator;

import org.apache.openjpa.lib.util.Localizer;

/**
 * Abstract read-only list iterator.
 *
 * @author Abe White
 * @nojavadoc
 */
abstract class AbstractListIterator implements ListIterator {

    private static final Localizer _loc = Localizer.forPackage
        (AbstractListIterator.class);

    public void add(Object o) {
        throw new UnsupportedOperationException(_loc.get("read-only")
            .getMessage());
    }

    public void set(Object o) {
        throw new UnsupportedOperationException(_loc.get("read-only")
            .getMessage());
    }

    public void remove() {
        throw new UnsupportedOperationException(_loc.get("read-only")
            .getMessage());
    }
}
