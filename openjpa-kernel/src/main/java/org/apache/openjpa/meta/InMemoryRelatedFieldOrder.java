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
package org.apache.openjpa.meta;

import java.util.Comparator;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.OpenJPAStateManager;

/**
 * Order by a field in the related type in memory.
 *
 * @author Abe White
 */
class InMemoryRelatedFieldOrder
    implements Order, Comparator {

    private final FieldMetaData _rel;
    private final boolean _asc;

    public InMemoryRelatedFieldOrder(FieldMetaData rel, boolean asc) {
        _rel = rel;
        _asc = asc;
    }

    public String getName() {
        return _rel.getName();
    }

    public boolean isAscending() {
        return _asc;
    }

    public Comparator getComparator() {
        return this;
    }

    public int compare(Object o1, Object o2) {
        if (o1 == o2)
            return 0;
        if (!(o1 instanceof PersistenceCapable)
            || !(o2 instanceof PersistenceCapable))
            return 0;

        PersistenceCapable pc1 = (PersistenceCapable) o1;
        PersistenceCapable pc2 = (PersistenceCapable) o2;
        OpenJPAStateManager sm1 = (OpenJPAStateManager) pc1.pcGetStateManager();
        OpenJPAStateManager sm2 = (OpenJPAStateManager) pc2.pcGetStateManager();
        if (sm1 == null || sm2 == null)
            return 0;

        Object v1 = sm1.fetchField(_rel.getIndex(), false);
        Object v2 = sm2.fetchField(_rel.getIndex(), false);
        if (v1 == v2)
            return 0;
        if (v1 == null)
            return (_asc) ? -1 : 1;
        if (v2 == null)
            return (_asc) ? 1 : -1;
        int cmp = ((Comparable) v1).compareTo(v2);
        return (_asc) ? cmp : -cmp;
    }
}
