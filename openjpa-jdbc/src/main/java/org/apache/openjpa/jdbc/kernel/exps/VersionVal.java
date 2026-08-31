/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.jdbc.kernel.exps;

import java.sql.SQLException;

import org.apache.openjpa.jdbc.kernel.exps.PCPath.PathExpState;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.kernel.exps.ExpressionVisitor;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.UserException;

/**
 * Select the version value of an object; typically used in projections.
 *
 * @author Abe White
 * @author Paulo Cristovão Filho
 */
class VersionVal
    extends AbstractVal {

    
    private static final long serialVersionUID = 1L;

    private static final Localizer _loc = Localizer.forPackage(VersionVal.class);

    protected final PCPath _path;
    private ClassMetaData _meta = null;

    /**
     * Constructor. Provide the value whose version to extract.
     */
    public VersionVal(PCPath path) {
        _path = path;
    }

    /**
     * Return the version columns. Note that these are the columns of the
     * version strategy, which are not necessarily the columns of a version
     * field: some strategies map version columns without a version field.
     */
    public Column[] getColumns(ExpState state) {
        return _path.getClassMapping(state).getVersion().getColumns();
    }

    @Override
    public ClassMetaData getMetaData() {
        return _meta;
    }

    @Override
    public void setMetaData(ClassMetaData meta) {
        _meta = meta;
    }

    @Override
    public Class getType() {
        // note: getColumns()/initialize() resolve the target type from the
        // ExpState; getType() has none, so it uses the path's class.
        ClassMetaData meta = _path.getMetaData();
        FieldMetaData versionField = (meta == null) ? null : meta.getVersionField();
        if (versionField != null) {
            return versionField.getType();
        }

        // surrogate version: the version strategy maps column(s) but there is
        // no version field, so no java type is declared here. Type the value
        // loosely, exactly as the in-memory VersionVal does, rather than
        // returning null: callers dereference this (result shapes, comparison
        // type checks, Filters.convert).
        return Object.class;
    }

    @Override
    public void setImplicitType(Class type) {
    }

    @Override
    public ExpState initialize(Select sel, ExpContext ctx, int flags) {
        ExpState state = _path.initialize(sel, ctx, JOIN_REL);

        // it's difficult to get calls on non-pc fields to always return null
        // without screwing up the SQL, to just don't let users call it on
        // non-pc fields at all
        ClassMapping cls = _path.getClassMapping(state);
        if (cls == null || cls.getEmbeddingMapping() != null) {
            throw new UserException(_loc.get("bad-version-path", pathDescription()));
        }

        // types that are not versioned have no version columns to select,
        // group, order or compare by; fail with a meaningful message rather
        // than a NullPointerException further down the line
        if (cls.getVersion().getColumns().length == 0) {
            throw new UserException(_loc.get("no-version-field", cls));
        }
        return state;
    }

    /**
     * A user-recognizable description of the VERSION() argument, for error
     * messages.
     */
    private String pathDescription() {
        String desc = _path.getPCPathString();
        if (desc != null && desc.endsWith(".")) {
            desc = desc.substring(0, desc.length() - 1);
        }
        if (desc != null && desc.length() > 0) {
            return desc;
        }
        String alias = _path.getSchemaAlias();
        return (alias != null) ? alias : String.valueOf(_path.getMetaData());
    }

    @Override
    public Object toDataStoreValue(Select sel, ExpContext ctx, ExpState state, Object val) {
        ClassMapping cls = _path.getClassMapping(state);
        FieldMetaData versionField = cls.getVersionField();
        if (versionField != null) {
            return Filters.convert(val, versionField.getType());
        }

        // surrogate version: convert using the version column's java type,
        // which the version strategy stamped onto the column
        Column[] cols = cls.getVersion().getColumns();
        if (cols.length == 1) {
            return JavaTypes.convert(val, cols[0].getJavaType());
        }
        return val;
    }

    @Override
    public void select(Select sel, ExpContext ctx, ExpState state,
        boolean pks) {
        selectColumns(sel, ctx, state, true);
    }

    @Override
    public void selectColumns(Select sel, ExpContext ctx, ExpState state,
        boolean pks) {
    	sel.setSchemaAlias(_path.getSchemaAlias());
    	sel.select(getColumns(state), ((PathExpState) state).joins);
    }

    @Override
    public void groupBy(Select sel, ExpContext ctx, ExpState state) {
        sel.setSchemaAlias(_path.getSchemaAlias());
        sel.groupBy(getColumns(state), sel.outer(((PathExpState) state).joins));
    }

    @Override
    public void orderBy(Select sel, ExpContext ctx, ExpState state,
        boolean asc) {
        sel.setSchemaAlias(_path.getSchemaAlias());
        sel.orderBy(getColumns(state), asc, sel.outer(((PathExpState) state).joins), false);
    }

    @Override
    public Object load(ExpContext ctx, ExpState state, Result res)
        throws SQLException {
        return res.getObject(getColumns(state)[0], null, ((PathExpState) state).joins);
    }

    @Override
    public void calculateValue(Select sel, ExpContext ctx, ExpState state,
        Val other, ExpState otherState) {
        _path.calculateValue(sel, ctx, state, null, null);
    }

    @Override
    public int length(Select sel, ExpContext ctx, ExpState state) {
        return getColumns(state).length;
    }

    @Override
    public void appendTo(Select sel, ExpContext ctx, ExpState state,
        SQLBuffer sql, int index) {
        _path.appendTo(sel, state, sql, getColumns(state)[index]);
    }

    @Override
    public void acceptVisit(ExpressionVisitor visitor) {
        visitor.enter(this);
        _path.acceptVisit(visitor);
        visitor.exit(this);
    }
}

