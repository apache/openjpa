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
package org.apache.openjpa.kernel.exps;

import org.apache.openjpa.meta.ClassMetaData;

/**
 * <p>Interface for any non-operator in a query filter, including
 * constants, variables, and object fields.</p>
 *
 * @author Abe White
 */
public interface Value {

    /**
     * Return the expected type for this value, or <code>Object</code> if
     * the type is unknown.
     */
    public Class getType();

    /**
     * Set the implicit type of the value, based on how it is used in the
     * filter.  This method is only called on values who return
     * <code>Object</code> from {@link #getType}.
     */
    public void setImplicitType(Class type);

    /**
     * Return true if this value is a variable.
     */
    public boolean isVariable();

    /**
     * Return any associated persistent type.
     */
    public ClassMetaData getMetaData();

    /**
     *	Associate a persistent type with this value.
     */
    public void setMetaData(ClassMetaData meta);
}
