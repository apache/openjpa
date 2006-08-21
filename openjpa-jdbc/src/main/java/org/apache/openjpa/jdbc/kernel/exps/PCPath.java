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

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.commons.lang.ObjectUtils;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.Schemas;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.UserException;

/**
 * A path represents a traversal into fields of a candidate object.
 *
 * @author Abe White
 */
class PCPath
    extends AbstractVal
    implements JDBCPath {

    private static final int PATH = 0;
    private static final int BOUND_VAR = 1;
    private static final int UNBOUND_VAR = 2;
    private static final int UNACCESSED_VAR = 3;

    private static final Localizer _loc = Localizer.forPackage(PCPath.class);

    private final ClassMapping _candidate;
    private LinkedList _actions = null;
    private Joins _joins = null;
    private boolean _forceOuter = false;
    private ClassMapping _class = null;
    private FieldMapping _field = null;
    private boolean _key = false;
    private boolean _joinedRel = false;
    private int _type = PATH;
    private String _varName = null;
    private Column[] _cols = null;
    private Class _cast = null;

    /**
     * Return a path starting with the 'this' ptr.
     */
    public PCPath(ClassMapping type) {
        _candidate = type;
    }

    /**
     * Return a path starting from the given variable.
     */
    public PCPath(ClassMapping candidate, Variable var) {
        _candidate = candidate;
        _actions = new LinkedList();

        PCPath other = var.getPCPath();
        Action action = new Action();
        if (other == null) {
            _type = UNBOUND_VAR;
            action.op = Action.UNBOUND_VAR;
            action.data = var;
        } else {
            // bound variable; copy path
            _type = UNACCESSED_VAR;
            _actions.addAll(other._actions);

            action.op = Action.VAR;
            action.data = var.getName();
        }
        _actions.add(action);
        _cast = var.getType(); // initial type is var type
    }

    /**
     * Return a path starting from the given subquery.
     */
    public PCPath(SubQ sub) {
        _candidate = sub.getCandidate();
        _actions = new LinkedList();

        Action action = new Action();
        action.op = Action.SUBQUERY;
        action.data = sub.getCandidateAlias();
        _actions.add(action);
        _cast = sub.getType(); // initial type is subquery type
        _varName = sub.getCandidateAlias();
    }

    /**
     * Set the path as a binding of the given variable.
     */
    public void addVariableAction(Variable var) {
        _varName = var.getName();
    }

    /**
     * Return true if this is a bound variable that has not been accessed
     * after binding. Useful for filters like
     * "coll.contains (var) &amp;&amp; var == null", which should really
     * just act like "coll.contains (null)".
     */
    public boolean isUnaccessedVariable() {
        return _type == UNACCESSED_VAR;
    }

    /**
     * If this path is part of a contains clause, then alias it to the
     * proper contains id before initialization.
     */
    public void setContainsId(String id) {
        // treat it just like a unique variable
        Action action = new Action();
        action.op = Action.VAR;
        action.data = id;
        if (_actions == null)
            _actions = new LinkedList();
        _actions.add(action);
    }

    public ClassMetaData getMetaData() {
        return _class;
    }

    public void setMetaData(ClassMetaData meta) {
        _class = (ClassMapping) meta;
    }

    public boolean isVariable() {
        return false;
    }

    public ClassMapping getClassMapping() {
        if (_field == null)
            return _class;
        if (_key) {
            if (_field.getKey().getTypeCode() == JavaTypes.PC)
                return _field.getKeyMapping().getTypeMapping();
            return null;
        }
        if (_field.getElement().getTypeCode() == JavaTypes.PC)
            return _field.getElementMapping().getTypeMapping();
        if (_field.getTypeCode() == JavaTypes.PC)
            return _field.getTypeMapping();
        return null;
    }

    public FieldMapping getFieldMapping() {
        return _field;
    }

    public boolean isKey() {
        return _key;
    }

    public String getPath() {
        if (_actions == null)
            return (_varName == null) ? "" : _varName + ".";

        StringBuffer path = new StringBuffer();
        Action action;
        for (Iterator itr = _actions.iterator(); itr.hasNext();) {
            action = (Action) itr.next();
            if (action.op == Action.VAR || action.op == Action.SUBQUERY)
                path.append(action.data);
            else if (action.op == Action.UNBOUND_VAR)
                path.append(((Variable) action.data).getName());
            else
                path.append(((FieldMapping) action.data).getName());
            path.append('.');
        }
        if (_varName != null)
            path.append(_varName).append('.');
        return path.toString();
    }

    public Column[] getColumns() {
        if (_cols == null)
            _cols = calculateColumns();
        return _cols;
    }

    /**
     * The columns used by this path.
     */
    private Column[] calculateColumns() {
        if (_key) {
            if (!_joinedRel && _field.getKey().getValueMappedBy() != null)
                joinRelation();
            else if (_joinedRel
                && _field.getKey().getTypeCode() == JavaTypes.PC)
                return _field.getKeyMapping().getTypeMapping().
                    getPrimaryKeyColumns();
            return _field.getKeyMapping().getColumns();
        }
        if (_field != null) {
            switch (_field.getTypeCode()) {
                case JavaTypes.MAP:
                case JavaTypes.ARRAY:
                case JavaTypes.COLLECTION:
                    ValueMapping elem = _field.getElementMapping();
                    if (_joinedRel && elem.getTypeCode() == JavaTypes.PC)
                        return elem.getTypeMapping().getPrimaryKeyColumns();
                    if (elem.getColumns().length > 0)
                        return elem.getColumns();
                    return _field.getColumns();
                case JavaTypes.PC:
                    if (_joinedRel)
                        return _field.getTypeMapping().getPrimaryKeyColumns();
                    return _field.getColumns();
                default:
                    return _field.getColumns();
            }
        }
        return (_class == null) ? Schemas.EMPTY_COLUMNS
            : _class.getPrimaryKeyColumns();
    }

    public void get(FieldMetaData field, boolean nullTraversal) {
        if (_actions == null)
            _actions = new LinkedList();
        Action action = new Action();
        action.op = (nullTraversal) ? Action.GET_OUTER : Action.GET;
        action.data = field;
        _actions.add(action);
        if (_type == UNACCESSED_VAR)
            _type = BOUND_VAR;
        _cast = null;
    }

    public void getKey() {
        // change the last action to a get key
        Action action = (Action) _actions.getLast();
        action.op = Action.GET_KEY;
        _cast = null;
    }

    public FieldMetaData last() {
        Action act = lastFieldAction();
        return (act == null) ? null : (FieldMetaData) act.data;
    }

    /**
     * Return the last action that gets a field.
     */
    private Action lastFieldAction() {
        if (_actions == null)
            return null;

        ListIterator itr = _actions.listIterator(_actions.size());
        Action prev;
        while (itr.hasPrevious()) {
            prev = (Action) itr.previous();
            if (prev.op == Action.GET || prev.op == Action.GET_OUTER
                || prev.op == Action.GET_KEY)
                return prev;

            // break if we're getting to path portions that we copied from
            // our variable
            if (prev.op == Action.VAR || prev.op == Action.UNBOUND_VAR
                || prev.op == Action.SUBQUERY)
                break;
        }
        return null;
    }

    public Class getType() {
        if (_cast != null)
            return _cast;
        FieldMetaData fld;
        boolean key;
        if (_field != null) {
            fld = _field;
            key = _key;
        } else {
            Action act = lastFieldAction();
            fld = (act == null) ? null : (FieldMetaData) act.data;
            key = act != null && act.op == Action.GET_KEY;
        }

        if (fld != null) {
            switch (fld.getDeclaredTypeCode()) {
                case JavaTypes.ARRAY:
                    if (fld.getDeclaredType() == byte[].class
                        || fld.getDeclaredType() == Byte[].class
                        || fld.getDeclaredType() == char[].class
                        || fld.getDeclaredType() == Character[].class)
                        return fld.getDeclaredType();
                    return fld.getElement().getDeclaredType();
                case JavaTypes.MAP:
                    if (key)
                        return fld.getKey().getDeclaredType();
                    return fld.getElement().getDeclaredType();
                case JavaTypes.COLLECTION:
                    return fld.getElement().getDeclaredType();
                default:
                    return fld.getDeclaredType();
            }
        }
        if (_class != null)
            return _class.getDescribedType();
        return Object.class;
    }

    public void setImplicitType(Class type) {
        _cast = type;
    }

    public void initialize(Select sel, JDBCStore store, boolean nullTest) {
        // initialize can be called more than once, so reset
        _field = null;
        _key = false;
        _forceOuter = false;
        _joinedRel = false;
        _joins = sel.newJoins();

        // iterate to the final field
        ClassMapping rel = _candidate;
        ClassMapping owner;
        ClassMapping from, to;
        Action action;
        Variable var;
        Iterator itr = (_actions == null) ? null : _actions.iterator();
        while (itr != null && itr.hasNext()) {
            action = (Action) itr.next();

            // treat subqueries like variables for alias generation purposes
            if (action.op == Action.VAR)
                _joins = _joins.setVariable((String) action.data);
            else if (action.op == Action.SUBQUERY)
                _joins = _joins.setSubselect((String) action.data);
            else if (action.op == Action.UNBOUND_VAR) {
                // unbound vars are cross-joined to the candidate table
                var = (Variable) action.data;
                rel = (ClassMapping) var.getMetaData();
                _joins = _joins.setVariable(var.getName());
                _joins = _joins.crossJoin(_candidate.getTable(),
                    rel.getTable());
            } else {
                // move past the previous field, if any
                if (_field != null)
                    rel = traverseField(false);

                // mark if the next traversal should go through
                // the key rather than value
                _key = action.op == Action.GET_KEY;
                _forceOuter |= action.op == Action.GET_OUTER;

                // get mapping for the current field
                _field = (FieldMapping) action.data;
                owner = _field.getDefiningMapping();
                if (_field.getManagement() != FieldMapping.MANAGE_PERSISTENT)
                    throw new UserException(_loc.get("non-pers-field",
                        _field));

                // find the most-derived type between the declared relation
                // type and the field's owner, and join from that type to
                // the lesser derived type
                if (rel != owner && rel != null) {
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
        if (_varName != null)
            _joins = _joins.setVariable(_varName);

        // if we're not comparing to null or doing an isEmpty, then
        // join into the data on the final field; obviously we can't do these
        // joins when comparing to null b/c the whole purpose is to see
        // whether the joins even exist
        if (!nullTest)
            traverseField(true);

        // note that we haven't yet joined to the relation of the last field yet
        _joinedRel = false;
    }

    /**
     * Traverse into the previous field of a relation path.
     *
     * @param last whether this is the last field in the path
     * @return the mapping of the related type, or null
     */
    private ClassMapping traverseField(boolean last) {
        if (_field == null)
            return null;

        // traverse into field value
        if (_key)
            _joins = _field.joinKey(_joins, _forceOuter);
        else
            _joins = _field.join(_joins, _forceOuter);

        // if this isn't the last field, traverse into the relation
        if (!last)
            joinRelation(true);

        // return the maping of the related type, if any
        if (_key)
            return _field.getKeyMapping().getTypeMapping();
        if (_field.getElement().getTypeCode() == JavaTypes.PC)
            return _field.getElementMapping().getTypeMapping();
        return _field.getTypeMapping();
    }

    /**
     * Join into the relation represented by the current field, if any.
     */
    void joinRelation() {
        joinRelation(false);
    }

    private void joinRelation(boolean traverse) {
        if (_field == null)
            return;
        if (_key)
            _joins = _field.joinKeyRelation(_joins, _forceOuter, traverse);
        else
            _joins = _field.joinRelation(_joins, _forceOuter, traverse);
        _joinedRel = true;
    }

    public Joins getJoins() {
        return _joins;
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        if (_field != null) {
            if (_key)
                return _field.toKeyDataStoreValue(val, store);
            if (_field.getElement().getDeclaredTypeCode() != JavaTypes.OBJECT)
                return _field.toDataStoreValue(val, store);

            val = _field.getExternalValue(val, store.getContext());
            return _field.toDataStoreValue(val, store);
        }
        return _class.toDataStoreValue(val, _class.getPrimaryKeyColumns(),
            store);
    }

    public void select(Select sel, JDBCStore store, Object[] params,
        boolean pks, JDBCFetchConfiguration fetch) {
        selectColumns(sel, store, params, pks, fetch);
    }

    public void selectColumns(Select sel, JDBCStore store,
        Object[] params, boolean pks, JDBCFetchConfiguration fetch) {
        ClassMapping mapping = getClassMapping();
        if (mapping == null || !_joinedRel)
            sel.select(getColumns(), _joins);
        else if (pks)
            sel.select(mapping.getPrimaryKeyColumns(), _joins);
        else {
            // select the mapping; allow any subs because we know this must
            // be either a relation, in which case it will already be
            // constrained by the joins, or 'this', in which case the
            // JDBCExpressionFactory takes care of adding class conditions for
            // the candidate class on the select
            int subs = (_type == UNBOUND_VAR) ? sel.SUBS_JOINABLE
                : sel.SUBS_ANY_JOINABLE;
            sel.select(mapping, subs, store, fetch,
                JDBCFetchConfiguration.EAGER_NONE, sel.outer(_joins));
        }
    }

    public void groupBy(Select sel, JDBCStore store, Object[] params,
        JDBCFetchConfiguration fetch) {
        ClassMapping mapping = getClassMapping();
        if (mapping == null || !_joinedRel)
            sel.groupBy(getColumns(), sel.outer(_joins));
        else {
            int subs = (_type == UNBOUND_VAR) ? sel.SUBS_JOINABLE
                : sel.SUBS_ANY_JOINABLE;
            sel.groupBy(mapping, subs, store, fetch, sel.outer(_joins));
        }
    }

    public void orderBy(Select sel, JDBCStore store, Object[] params,
        boolean asc, JDBCFetchConfiguration fetch) {
        sel.orderBy(getColumns(), asc, sel.outer(_joins), false);
    }

    public Object load(Result res, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        return load(res, store, false, fetch);
    }

    Object load(Result res, JDBCStore store, boolean pks,
        JDBCFetchConfiguration fetch)
        throws SQLException {
        ClassMapping mapping = getClassMapping();
        if (mapping != null && (_field == null || !_field.isEmbedded())) {
            if (pks)
                return mapping.getObjectId(store, res, null, true, _joins);
            return res.load(mapping, store, fetch, _joins);
        }

        Object ret;
        if (_key)
            ret = _field.loadKeyProjection(store, fetch, res, _joins);
        else
            ret = _field.loadProjection(store, fetch, res, _joins);
        if (_cast != null)
            ret = Filters.convert(ret, _cast);
        return ret;
    }

    /**
     * Whether the given variable appears in this path.
     */
    public boolean hasVariable(Variable var) {
        if (_actions == null)
            return false;

        Action action;
        for (Iterator itr = _actions.iterator(); itr.hasNext();) {
            action = (Action) itr.next();
            if (action.op == Action.VAR && action.data.equals(var.getName()))
                return true;
        }
        return false;
    }

    public void calculateValue(Select sel, JDBCStore store,
        Object[] params, Val other, JDBCFetchConfiguration fetch) {
        // we don't create the SQL b/c it forces the Select to cache aliases
        // for the tables we use, and these aliases might not ever be used if
        // we eventually call appendIsEmpty or appendIsNull rather than appendTo
    }

    public void clearParameters() {
    }

    public int length() {
        return getColumns().length;
    }

    public void appendTo(SQLBuffer sql, int index, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        Column col = getColumns()[index];

        // if select is null, it means we are not aliasing columns
        // (e.g., during a bulk update)
        if (sel == null)
            sql.append(col.getName());
        else
            sql.append(sel.getColumnAlias(col, _joins));
    }

    public void appendIsEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        if (_field == null)
            sql.append(FALSE);
        else
            _field.appendIsEmpty(sql, sel, _joins);
    }

    public void appendIsNotEmpty(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        if (_field == null)
            sql.append(FALSE);
        else
            _field.appendIsNotEmpty(sql, sel, _joins);
    }

    public void appendSize(SQLBuffer sql, Select sel, JDBCStore store,
        Object[] params, JDBCFetchConfiguration fetch) {
        if (_field == null)
            sql.append("1");
        else
            _field.appendSize(sql, sel, _joins);
    }

    public void appendIsNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        if (_field == null)
            sql.append(FALSE);
        else
            _field.appendIsNull(sql, sel, _joins);
    }

    public void appendIsNotNull(SQLBuffer sql, Select sel,
        JDBCStore store, Object[] params, JDBCFetchConfiguration fetch) {
        if (_field == null)
            sql.append(TRUE);
        else
            _field.appendIsNotNull(sql, sel, _joins);
    }

    public int hashCode() {
        if (_actions == null)
            return _candidate.hashCode();
        return _candidate.hashCode() ^ _actions.hashCode();
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof PCPath))
            return false;
        PCPath path = (PCPath) other;
        return ObjectUtils.equals(_candidate, path._candidate)
            && ObjectUtils.equals(_actions, path._actions);
    }

    /**
     * Helper class representing an action.
     */
    private static class Action {

        public static final int GET = 0;
        public static final int GET_OUTER = 1;
        public static final int GET_KEY = 2;
        public static final int VAR = 3;
        public static final int SUBQUERY = 4;
        public static final int UNBOUND_VAR = 5;
        public static final int CAST = 6;

        public int op = -1;
        public Object data = null;

        public String toString() {
            return op + "|" + data;
        }

        public int hashCode() {
            if (data == null)
                return op;
            return op ^ data.hashCode();
        }

        public boolean equals(Object other) {
            if (other == this)
                return true;
            Action a = (Action) other;
            return op == a.op
                && ObjectUtils.equals(data, a.data);
        }
    }
}
