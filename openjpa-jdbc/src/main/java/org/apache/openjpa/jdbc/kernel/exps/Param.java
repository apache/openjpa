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

import java.util.Collection;
import java.util.Map;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.Parameter;
import org.apache.openjpa.util.ImplHelper;

/**
 * A parameter in a filter.
 *
 * @author Abe White
 */
class Param
    extends Const
    implements Parameter {

    private final String _name;
    private Class _type = null;
    private int _idx = -1;
    private boolean _container = false;

    /**
     * Constructor. Supply parameter name and type.
     */
    public Param(String name, Class type) {
        _name = name;
        setImplicitType(type);
    }

    public String getName() {
        return _name;
    }

    public String getParameterName() {
        return getName();
    }

    public Class getType() {
        return _type;
    }

    public void setImplicitType(Class type) {
        _type = type;
        _container = (getMetaData() == null || !ImplHelper.isManagedType(type))
            && (Collection.class.isAssignableFrom(type)
            || Map.class.isAssignableFrom(type));
    }

    public int getIndex() {
        return _idx;
    }

    public void setIndex(int idx) {
        _idx = idx;
    }

    public Object getValue(Object[] params) {
        return Filters.convert(params[_idx], getType());
    }

    public Object getSQLValue(Select sel, ExpContext ctx, ExpState state) {
        return ((ParamExpState) state).sqlValue;
    }

    public ExpState initialize(Select sel, ExpContext ctx, int flags) {
        return new ParamExpState();
    }

    /**
     * Expression state.
     */
    private static class ParamExpState
        extends ConstExpState {

        public Object sqlValue = null;
        public int otherLength = 1; 
    } 

    public void calculateValue(Select sel, ExpContext ctx, ExpState state, 
        Val other, ExpState otherState) {
        super.calculateValue(sel, ctx, state, other, otherState);
        Object val = getValue(ctx.params);
        ParamExpState pstate = (ParamExpState) state;
        if (other != null && !_container) {
            pstate.sqlValue = other.toDataStoreValue(sel, ctx, otherState, val);
            pstate.otherLength = other.length(sel, ctx, otherState);
        } else if (val instanceof PersistenceCapable) {
            ClassMapping mapping = ctx.store.getConfiguration().
                getMappingRepositoryInstance().getMapping(val.getClass(), 
                ctx.store.getContext().getClassLoader(), true);
            pstate.sqlValue = mapping.toDataStoreValue(val, 
                mapping.getPrimaryKeyColumns(), ctx.store);
            pstate.otherLength = mapping.getPrimaryKeyColumns().length;
        } else
            pstate.sqlValue = val;
    }

    public void appendTo(Select sel, ExpContext ctx, ExpState state, 
        SQLBuffer sql, int index) {
        ParamExpState pstate = (ParamExpState) state;
        if (pstate.otherLength > 1)
            sql.appendValue(((Object[]) pstate.sqlValue)[index], 
                pstate.getColumn(index));
        else
            sql.appendValue(pstate.sqlValue, pstate.getColumn(index));
    }
}
