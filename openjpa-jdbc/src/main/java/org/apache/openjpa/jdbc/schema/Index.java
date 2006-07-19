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
package org.apache.openjpa.jdbc.schema;

import org.apache.commons.lang.StringUtils;

/**
 * Represents a database index. Can also represent a partial index,
 * aligning with {@link java.sql.DatabaseMetaData}.
 *
 * @author Abe White
 * @author Stephen Kim
 */
public class Index
    extends LocalConstraint {

    private boolean _unique = false;

    /**
     * Default constructor.
     */
    public Index() {
    }

    /**
     * Constructor.
     *
     * @param name the name of the index
     * @param table the table of the index
     */
    public Index(String name, Table table) {
        super(name, table);
    }

    /**
     * Return true if this is a UNIQUE index.
     */
    public boolean isUnique() {
        return _unique;
    }

    /**
     * Set whether this is a UNIQUE index.
     */
    public void setUnique(boolean unique) {
        _unique = unique;
    }

    public boolean isLogical() {
        return false;
    }

    /**
     * Indexes are equal if they have the same name, the same columns, and
     * are both unique/not unique.
     */
    public boolean equalsIndex(Index idx) {
        if (idx == this)
            return true;
        if (idx == null)
            return false;

        if (isUnique() != idx.isUnique())
            return false;
        if (!StringUtils.equalsIgnoreCase(getFullName(), idx.getFullName()))
            return false;
        return equalsLocalConstraint(idx);
    }
}
