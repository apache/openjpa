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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Broker;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;

/**
 * A field traversal starting with a constant filter parameter.
 *
 * @author Abe White
 */
class ConstPath
    extends Const
    implements JDBCPath {

    private final Const _constant;
    private final LinkedList _actions = new LinkedList();
    private Object _val = null;
    private Object _sqlVal = null;
    private int _otherLen = 0;

    /**
     * Constructor. Supply constant to traverse.
     */
    public ConstPath(Const constant) {
        _constant = constant;
    }

    public Class getType() {
        if (_actions.isEmpty()) {
            ClassMetaData meta = getMetaData();
            if (meta == null)
                return Object.class;
            return meta.getDescribedType();
        }

        Object last = _actions.getLast();
        if (last instanceof Class)
            return (Class) last;
        FieldMetaData fmd = (FieldMetaData) last;
        return fmd.getDeclaredType();
    }

    public void setImplicitType(Class type) {
        _actions.add(type);
    }

    public void get(FieldMetaData field, boolean nullTraversal) {
        _actions.add(field);
    }

    public void getKey() {
    }

    public FieldMetaData last() {
        ListIterator itr = _actions.listIterator(_actions.size());
        Object prev;
        while (itr.hasPrevious()) {
            prev = itr.previous();
            if (prev instanceof FieldMetaData)
                return (FieldMetaData) prev;
        }
        return null;
    }

    public Object getValue() {
        return _val;
    }

    public Object getSQLValue() {
        return _sqlVal;
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
        super.calculateValue(sel, store, params, other, fetch);
        _constant.calculateValue(sel, store, params, null, fetch);
        _val = _constant.getValue();
        boolean failed = false;

        // copied from org.apache.openjpa.query.InMemoryPath
        Object action;
        OpenJPAStateManager sm;
        Broker tmpBroker = null;
        for (Iterator itr = _actions.iterator(); itr.hasNext();) {
            // fail on null value
            if (_val == null) {
                failed = true;
                break;
            }

            action = itr.next();
            if (action instanceof Class) {
                try {
                    _val = Filters.convert(_val, (Class) action);
                    continue;
                } catch (ClassCastException cce) {
                    failed = true;
                    break;
                }
            }

            // make sure we can access the instance; even non-pc vals might
            // be proxyable
            sm = null;
            tmpBroker = null;
            if (_val instanceof PersistenceCapable)
                sm = (OpenJPAStateManager) ((PersistenceCapable) _val).
                    pcGetStateManager();
            if (sm == null) {
                tmpBroker = store.getContext().getBroker();
                tmpBroker.transactional(_val, false, null);
                sm = tmpBroker.getStateManager(_val);
            }

            try {
                // get the specified field value and switch candidate
                _val = sm.fetchField(((FieldMetaData) action).getIndex(),
                    true);
            } finally {
                // setTransactional does not clear the state, which is
                // important since tmpVal might be also managed by
                // another broker if it's a proxied non-pc instance
                if (tmpBroker != null)
                    tmpBroker.nontransactional(sm.getManagedInstance(), null);
            }
        }

        if (failed)
            _val = null;

        if (other != null) {
            _sqlVal = other.toDataStoreValue(_val, store);
            _otherLen = other.length();
        } else
            _sqlVal = _val;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        if (_otherLen > 1)
            sql.appendValue(((Object[]) _sqlVal)[index], getColumn(index));
        else
            sql.appendValue(_sqlVal, getColumn(index));
    }

    public void clearParameters() {
        _constant.clearParameters();
        _val = null;
        _sqlVal = null;
    }
}
