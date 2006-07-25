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
package org.apache.openjpa.jdbc.kernel.exps;

import java.util.Map;

import org.apache.openjpa.jdbc.kernel.JDBCFetchState;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Discriminator;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Tests whether the given path is an instance of the given class.
 *
 * @author Abe White
 */
class InstanceofExpression
    implements Exp {

    private final PCPath _path;
    private final Class _cls;
    private Joins _joins = null;
    private Discriminator _dsc = null;
    private Class _relCls = null;
    private ClassMapping _mapping = null;

    /**
     * Constructor. Supply path and class to test for.
     */
    public InstanceofExpression(PCPath path, Class cls) {
        _path = path;
        _cls = cls;
    }

    public void initialize(Select sel, JDBCStore store,
        Object[] params, Map contains) {
        // note that we tell the path to go ahead and join to its related
        // object (if any) in order to access its class indicator
        _path.initialize(sel, store, false);
        _path.joinRelation();
        _joins = _path.getJoins();

        // does this path represent a relation?  if not, what class
        // is the field?
        ClassMapping rel = _path.getClassMapping();
        if (rel == null) {
            FieldMapping field = _path.getFieldMapping();
            switch (field.getTypeCode()) {
                case JavaTypes.MAP:
                    if (_path.isKey())
                        _relCls = field.getKey().getDeclaredType();
                    // no break
                case JavaTypes.ARRAY:
                case JavaTypes.COLLECTION:
                    _relCls = field.getElement().getDeclaredType();
                    break;
                default:
                    _relCls = field.getDeclaredType();
            }
        } else
            _relCls = rel.getDescribedType();

        // if the path represents a relation, get its class indicator and
        // make sure it's joined down to its base type
        _dsc = (rel == null || !rel.getDescribedType().isAssignableFrom(_cls))
            ? null : rel.getDiscriminator();
        if (_dsc != null) {
            // cache mapping for cast
            MappingRepository repos = store.getConfiguration().
                getMappingRepositoryInstance();
            _mapping = repos.getMapping(_cls, store.getContext().
                getClassLoader(), false);

            // if not looking for a PC, don't bother with indicator
            if (_mapping == null)
                _dsc = null;
            else {
                ClassMapping owner = _dsc.getClassMapping();
                ClassMapping from, to;
                if (rel.getDescribedType().isAssignableFrom
                    (owner.getDescribedType())) {
                    from = owner;
                    to = rel;
                } else {
                    from = rel;
                    to = owner;
                }

                for (; from != null && from != to;
                    from = from.getJoinablePCSuperclassMapping())
                    _joins = from.joinSuperclass(_joins, false);
            }
        }
    }

    public void appendTo(SQLBuffer sql, Select sel, JDBCStore store,
        Object[] params, JDBCFetchState fetchState) {
        // if no class indicator or a final class, just append true or false
        // depending on whether the cast matches the expected type
        if (_dsc == null) {
            if (_cls.isAssignableFrom(_relCls))
                sql.append("1 = 1");
            else
                sql.append("1 <> 1");
        } else {
            store.loadSubclasses(_dsc.getClassMapping());
            SQLBuffer buf = _dsc.getClassConditions(store, sel, _joins,
                _mapping, true);
            if (buf == null)
                sql.append("1 = 1");
            else
                sql.append(buf);
        }
        sel.append(sql, _joins);
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchState fetchState) {
        if (_dsc != null)
            sel.select(_dsc.getColumns(), _joins);
    }

    public Joins getJoins() {
        return _joins;
    }

    public boolean hasContainsExpression() {
        return false;
    }

    public boolean hasVariable(Variable var) {
        return _path.hasVariable(var);
    }
}

