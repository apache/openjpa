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
package org.apache.openjpa.jdbc.sql;

/**
 * SQL UNION.
 *
 * @author Abe White
 */
public interface Union
    extends SelectExecutor {

    /**
     * Return the selects that make up this union.
     */
    public Select[] getSelects();

    /**
     * Get the union-level ordering, if any.
     */
    public String getOrdering();

    /**
     * Whether this union will return at most a single result. Setting this
     * flag makes it more efficient to execute logical unions that are actually
     * made up from multiple selects executed in batch.
     */
    public boolean isSingleResult();

    /**
     * Whether this union will return at most a single result. Setting this
     * flag makes it more efficient to execute logical unions that are actually
     * made up from multiple selects executed in batch.
     */
    public void setSingleResult(boolean single);

    /**
     * Whether this is a true UNION, rather than a logical combination of
     * independent selects.
     */
    public boolean isUnion();

    /**
     * Force the use of a series of standard selects rather than a true UNION.
     */
    public void abortUnion();

    /**
     * Select data using the given selector.
     */
    public void select(Selector selector);

    /**
     * A callback used to create the selects in a SQL union.
     */
    public static interface Selector {

        /**
         * Populate the <code>i</code>th select in the union.
         */
        public void select(Select sel, int i);
    }
}
