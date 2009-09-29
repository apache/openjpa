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
package org.apache.openjpa.jdbc.meta.strats;

import java.sql.SQLException;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.FieldMappingInfo;
import org.apache.openjpa.jdbc.meta.FieldStrategy;
import org.apache.openjpa.jdbc.schema.ForeignKey;
import org.apache.openjpa.jdbc.sql.Joins;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.RowManager;
import org.apache.openjpa.jdbc.sql.SQLBuffer;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.jdbc.sql.SelectExecutor;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.util.MetaDataException;

/**
 * No-op strategy for easy extension.
 *
 * @author Abe White
 */
public abstract class AbstractFieldStrategy
    extends AbstractStrategy
    implements FieldStrategy {

    private static final Localizer _loc = Localizer.forPackage
        (AbstractFieldStrategy.class);

    private Boolean _isNonDefaultMappingAllowed = null;
    private Boolean _isBi1ToMJT = null;
    private Boolean _isUni1ToMFK = null;
    private Integer _bi1ToMJT = null; //index of the field
    private ForeignKey _bi_1ToM_JoinFK = null;
    private ForeignKey _bi_1ToM_ElemFK = null;
    
    /**
     * The owning field mapping.
     */
    protected FieldMapping field = null;

    /**
     * Throws an informative exception if the field declares a mapped-by value.
     */
    protected void assertNotMappedBy() {
        if (field != null && field.getMappedBy() != null)
            throw new MetaDataException(_loc.get("cant-mapped-by", field,
                getAlias()));
    }

    public void setFieldMapping(FieldMapping owner) {
        field = owner;
    }

    public int supportsSelect(Select sel, int type, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch) {
        return 0;
    }

    public void selectEagerJoin(Select sel, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch, int eagerMode) {
    }

    public void selectEagerParallel(SelectExecutor sel, OpenJPAStateManager sm,
        JDBCStore store, JDBCFetchConfiguration fetch, int eagerMode) {
    }

    public boolean isEagerSelectToMany() {
        return false;
    }

    public int select(Select sel, OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, int eagerMode) {
        return -1;
    }

    public Object loadEagerParallel(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Object res)
        throws SQLException {
        return res;
    }

    public void loadEagerJoin(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res)
        throws SQLException {
    }

    public void load(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch, Result res)
        throws SQLException {
    }

    public void load(OpenJPAStateManager sm, JDBCStore store,
        JDBCFetchConfiguration fetch)
        throws SQLException {
    }

    public Object toDataStoreValue(Object val, JDBCStore store) {
        return val;
    }

    public Object toKeyDataStoreValue(Object val, JDBCStore store) {
        return val;
    }

    public void appendIsEmpty(SQLBuffer sql, Select sel, Joins joins) {
        sql.append("1 <> 1");
    }

    public void appendIsNotEmpty(SQLBuffer sql, Select sel, Joins joins) {
        sql.append("1 = 1");
    }

    public void appendIsNull(SQLBuffer sql, Select sel, Joins joins) {
        sql.append("1 <> 1");
    }

    public void appendIsNotNull(SQLBuffer sql, Select sel, Joins joins) {
        sql.append("1 <> 1");
    }

    public void appendSize(SQLBuffer sql, Select sel, Joins joins) {
        sql.append("1");
    }

    public void appendIndex(SQLBuffer sql, Select sel, Joins joins) {
        sql.append("1");
    }

    public void appendType(SQLBuffer sql, Select sel, Joins joins) {
        sql.append("1");
    }

    public Joins join(Joins joins, boolean forceOuter) {
        return joins;
    }

    public Joins joinKey(Joins joins, boolean forceOuter) {
        return joins;
    }

    public Joins joinRelation(Joins joins, boolean forceOuter,
        boolean traverse) {
        return joins;
    }

    public Joins joinKeyRelation(Joins joins, boolean forceOuter,
        boolean traverse) {
        return joins;
    }

    public Object loadProjection(JDBCStore store, JDBCFetchConfiguration fetch,
        Result res, Joins joins)
        throws SQLException {
        return null;
    }

    public Object loadKeyProjection(JDBCStore store,
        JDBCFetchConfiguration fetch, Result res, Joins joins)
        throws SQLException {
        return null;
    }

    public boolean isVersionable() {
        return false;
    }

    public void where(OpenJPAStateManager sm, JDBCStore store, RowManager rm,
        Object prevValue)
        throws SQLException {
    }
    
    private void isNonDefaultMapping() {
        FieldMapping mapped = field.getMappedByMapping();
        _isBi1ToMJT = false;
        _isUni1ToMFK = false;
        if (isNonDefaultMappingAllowed()) {
            if (field.getAssociationType() == FieldMetaData.ONE_TO_MANY ) {
                if (mapped == null) {
                    if (hasJoinTable())
                        return;
                    else if (hasJoinColumn()) {
                        _isUni1ToMFK = true;
                        return;
                    }
                } else {
                    if (hasJoinTable()) {
                        _isBi1ToMJT = true;
                        return;
                    } else if (hasJoinColumn()){
                        return;
                    }
                }
            }
        }
    }
    
    private boolean hasJoinColumn() {
        boolean hasJoinColumn = (field.getValueInfo().getColumns().size() > 0 ? true : false);
        return hasJoinColumn;
    }
    
    private boolean hasJoinTable() {
        boolean hasJoinTable = (field.getMappingInfo().getTableName() != null ? true : false);
        return hasJoinTable;
    }

    public boolean isBi1ToMJT() {
        if (_isBi1ToMJT == null)
            isNonDefaultMapping();
        return _isBi1ToMJT;
    }
    
    public boolean isUni1ToMFK() {
        if (_isUni1ToMFK == null)
            isNonDefaultMapping();
        return _isUni1ToMFK;
    }

    protected boolean isNonDefaultMappingAllowed() {
        if (_isNonDefaultMappingAllowed == null) {
            OpenJPAConfiguration conf = field.getRepository().getConfiguration();
            _isNonDefaultMappingAllowed = field.getRepository().
                getMetaDataFactory().getDefaults().isNonDefaultMappingAllowed(conf);
        }
        return _isNonDefaultMappingAllowed;
    }

    protected void getBiOneToManyInfo() {
        _bi1ToMJT = -1;
        if (!isNonDefaultMappingAllowed())
            return;
        ClassMapping inverse = field.getValueMapping().getTypeMapping();
        FieldMapping[] fmds = inverse.getFieldMappings();
        for (int i = 0; i < fmds.length; i++) {
            if (field == fmds[i].getMappedByMapping()) {
                int typeCode = fmds[i].getDeclaredTypeCode(); 
                if (typeCode == JavaTypes.ARRAY ||
                        typeCode == JavaTypes.COLLECTION ||
                        typeCode == JavaTypes.MAP) {
                    // this is a bi-directional oneToMany relation with
                    // @JoinTable annotation ==> join table strategy
                    // ==> should not mapped in the owner's table
                    FieldMappingInfo info = fmds[i].getMappingInfo();
                    if (info.getTableName() != null)
                        _bi1ToMJT = i;
                    _bi_1ToM_ElemFK = fmds[i].getElementMapping().getForeignKey();
                    _bi_1ToM_JoinFK = fmds[i].getJoinForeignKey();
                }
                break;
            } 
        }
    }

    protected int getFieldIndexBi1ToMJT() {
        if (_bi1ToMJT == null) {
            getBiOneToManyInfo();
        }
        return _bi1ToMJT;
    }
    
    protected ForeignKey getBi1ToMElemFK() {
        if (_bi1ToMJT == null) {
            getBiOneToManyInfo();
        }
        return _bi_1ToM_ElemFK;
    }
    
    protected ForeignKey getBi1ToMJoinFK() {
        if (_bi1ToMJT == null) {
            getBiOneToManyInfo();
        }
        return _bi_1ToM_JoinFK;
    }
}
