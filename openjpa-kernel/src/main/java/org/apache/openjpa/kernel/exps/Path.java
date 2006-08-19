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
package org.apache.openjpa.kernel.exps;

import org.apache.openjpa.meta.FieldMetaData;

/**
 * A path represents a traversal into fields of a candidate object.
 * Equivalent paths should compare equal.
 *
 * @author Abe White
 */
public interface Path
    extends Value {

    /**
     * Traverse into the given field of the current object, and update
     * the current object to that field value.
     *
     * @param nullTraversal if true, allow traversal through a null field
     */
    public void get(FieldMetaData field, boolean nullTraversal);

    /**
     * Return the last field in the path, or null if the path does not
     * not contain a final field.
     */
    public FieldMetaData last();
}
