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
package org.apache.openjpa.jdbc.meta;

import java.util.Comparator;

import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.MetaDataException;

/**
 * Use the database to order by a field in the related type.
 *
 * @author Abe White
 */
class JDBCRelatedFieldOrder
    implements JDBCOrder {

    private static final Localizer _loc = Localizer.forPackage
        (JDBCRelatedFieldOrder.class);

    private final FieldMapping _fm;
    private final boolean _asc;

    public JDBCRelatedFieldOrder(FieldMapping owner, FieldMapping rel,
        boolean asc) {
        if (!rel.isInDefaultFetchGroup())
            throw new MetaDataException(_loc.get("nondfg-field-orderable",
                owner, rel.getName()));

        _fm = rel;
        _asc = asc;
    }

    public String getName() {
        return _fm.getName();
    }

    public boolean isAscending() {
        return _asc;
    }

    public Comparator getComparator() {
        return null;
    }

    public boolean isInRelation() {
        return true;
    }

    public void order(Select sel, ClassMapping elem, Joins joins) {
        FieldMapping fm = elem.getFieldMapping(_fm.getIndex());
        sel.orderBy(fm.getColumns(), _asc, joins, false);
    }
}
