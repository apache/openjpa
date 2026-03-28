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
import java.util.ArrayList;
import java.util.List;

import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.jdbc.identifier.DBIdentifier;
import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Embeddable;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.FieldStrategy;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.meta.ValueMappingImpl;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.kernel.ObjectIdStateManager;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.StateManagerImpl;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.util.MetaDataException;

/**
 * Base class for embedded value handlers.
 *
 * @author Abe White
 * @since 0.4.0
 */
public abstract class EmbedValueHandler
    extends AbstractValueHandler {

    
    private static final long serialVersionUID = 1L;
    private static final Localizer _loc = Localizer.forPackage
        (EmbedValueHandler.class);

    /**
     * Maps embedded value and gathers columns and arguments into given lists.
     * @deprecated
     */
    @Deprecated
    protected void map(ValueMapping vm, String name, ColumnIO io,
        boolean adapt, List cols, List args) {
        DBDictionary dict = vm.getMappingRepository().getDBDictionary();
        DBIdentifier colName = DBIdentifier.newColumn(name, dict != null ? dict.delimitAll() : false);
        map(vm, colName, io, adapt, cols, args);
    }

    /**
     * Maps embedded value and gathers columns and arguments into given lists.
     */
    protected void map(ValueMapping vm, DBIdentifier name, ColumnIO io,
        boolean adapt, List cols, List args) {
        // have to resolve embedded value to collect its columns
        vm.getEmbeddedMapping().resolve(MetaDataModes.MODE_META | MetaDataModes.MODE_MAPPING);

        // gather columns and result arguments
        FieldMapping[] fms = vm.getEmbeddedMapping().getFieldMappings();
        Column[] curCols;
        Object[] curArgs;
        ColumnIO curIO;
        for (FieldMapping fm : fms) {
            if (fm.getManagement() != FieldMetaData.MANAGE_PERSISTENT)
                continue;
            FieldStrategy strat = fm.getStrategy();

            if (!(strat instanceof Embeddable)) {
                // JPA 2.4.1.3 ex2b: non-@Embeddable @IdClass field inside
                // @EmbeddedId. Include @MapsId FK columns so the identity
                // covers all PK columns. Columns are read-only (the
                // @ManyToOne FK relationship handles insert/update).
                List<Column> mapsIdCols = fm.getValueInfo()
                    .getMapsIdColumns();
                if (mapsIdCols != null && mapsIdCols.size() > 0) {
                    // Get target entity PK columns for proper types
                    Column[] tgtCols = findMapsIdTargetCols(vm);
                    for (int j = 0; j < mapsIdCols.size(); j++) {
                        Column col = new Column();
                        col.setIdentifier(mapsIdCols.get(j).getIdentifier());
                        if (tgtCols != null && j < tgtCols.length) {
                            col.setType(tgtCols[j].getType());
                            col.setSize(tgtCols[j].getSize());
                        } else {
                            col.setType(java.sql.Types.VARCHAR);
                        }
                        col.setNotNull(true);
                        io.setInsertable(cols.size(), false);
                        io.setNullInsertable(cols.size(), false);
                        io.setUpdatable(cols.size(), false);
                        io.setNullUpdatable(cols.size(), false);
                        cols.add(col);
                        args.add(null);
                    }
                    continue;
                }
                throw new MetaDataException(_loc.get("not-embeddable",
                        vm, fm));
            }

            ValueMapping val = fm.getValueMapping();
            if (val.getEmbeddedMapping() != null)
                map(val, name, io, adapt, cols, args);

            curCols = ((Embeddable) strat).getColumns();
            curIO = ((Embeddable) strat).getColumnIO();
            for (int j = 0; j < curCols.length; j++) {
                io.setInsertable(cols.size(), curIO.isInsertable(j, false));
                io.setNullInsertable(cols.size(),
                        curIO.isInsertable(j, true));
                io.setUpdatable(cols.size(), curIO.isUpdatable(j, false));
                io.setNullUpdatable(cols.size(), curIO.isUpdatable(j, true));
                cols.add(curCols[j]);
            }

            curArgs = ((Embeddable) fm.getStrategy()).getResultArguments();
            if (curCols.length == 1)
                args.add(curArgs);
            else if (curCols.length > 1)
                for (int j = 0; j < curCols.length; j++)
                    args.add((curArgs == null) ? null
                            : ((Object[]) curArgs)[j]);
        }
    }

    /**
     * Helper to convert an object value to its datastore equivalent.
     *
     * @param em state manager for embedded object
     * @param vm owning value
     * @param store store manager
     * @param cols embedded columns
     * @param rval return array if multiple columns
     * @param idx index in columns array to start
     */
    protected Object toDataStoreValue(OpenJPAStateManager em, ValueMapping vm,
            JDBCStore store, Column[] cols, Object rval, int idx) {

        // This is a placeholder to hold the value generated in
        // toDataStoreValue1. When this method is called from
        // ElementEmbedValueHandler or ObjectIdValueHandler,
        // if the dimension of cols > 1, rval is an array of the
        // same dimension. If the dimension of cols is 1, rval is null.
        // If rval is not null, it is an array of objects and this array
        // will be populated in toDatastoreValue1. If rval is null,
        // a new value will be added to rvals in toDataStoreValue1
        // and return to the caller.
        List rvals = new ArrayList();
        if (rval != null)
            rvals.add(rval);

        toDataStoreValue1(em, vm, store, cols, rvals, idx);
        return rvals.get(0);
    }

    protected int toDataStoreValue1(OpenJPAStateManager em, ValueMapping vm,
        JDBCStore store, Column[] cols, List rvals, int idx) {
        // set rest of columns from fields
        FieldMapping[] fms = vm.getEmbeddedMapping().getFieldMappings();
        Object cval;
        Column[] ecols;
        Embeddable embed;
        for (int i = 0; i < fms.length; i++) {
            if (fms[i].getManagement() != FieldMetaData.MANAGE_PERSISTENT)
                continue;

            // This recursive code is mainly to deal with situations
            // where an entity contains a collection of embeddableA.
            // The embeddableA element in the collection contains an
            // embeddableB. The parameter vm to toDataStoreValue is
            // embeddableA. If some field in embeddableA is of type
            // embeddableB, recursive call is required to populate the
            // value for embeddableB.
            ValueMapping val = fms[i].getValueMapping();
            if (val.getEmbeddedMapping() != null) {
                cval = (em == null) ? null : em.fetch(i);
                if (cval instanceof PersistenceCapable) {
                    OpenJPAStateManager embedSm = (OpenJPAStateManager)
                        ((PersistenceCapable)cval).pcGetStateManager();
                    idx = toDataStoreValue1(embedSm, val, store, cols, rvals,
                            idx);
                } else if (cval instanceof ObjectIdStateManager) {
                    idx = toDataStoreValue1((ObjectIdStateManager)cval, val,
                            store, cols, rvals, idx);
                } else if (cval == null) {
                    idx = toDataStoreValue1(null, val, store, cols, rvals, idx);
                }
            }

            // JPA 2.4.1.3 ex2b: non-@Embeddable @IdClass field — extract
            // POJO field values to FK columns via reflection
            if (!(fms[i].getStrategy() instanceof Embeddable)) {
                List<Column> mic = fms[i].getValueInfo().getMapsIdColumns();
                if (mic != null && !mic.isEmpty()) {
                    Object idObj = (em == null) ? null : em.fetch(i);
                    if (idObj != null) {
                        try {
                            java.lang.reflect.Field[] df =
                                idObj.getClass().getDeclaredFields();
                            int ci = 0;
                            for (java.lang.reflect.Field f : df) {
                                if (java.lang.reflect.Modifier
                                        .isStatic(f.getModifiers()))
                                    continue;
                                if (ci >= mic.size()) break;
                                f.setAccessible(true);
                                Object fv = f.get(idObj);
                                if (cols.length == 1) rvals.add(fv);
                                else ((Object[]) rvals.get(0))[idx++] = fv;
                                ci++;
                            }
                        } catch (Exception ex) {
                            for (int c = 0; c < mic.size(); c++) {
                                if (cols.length == 1) rvals.add(null);
                                else ((Object[]) rvals.get(0))[idx++] = null;
                            }
                        }
                    } else {
                        for (int c = 0; c < mic.size(); c++) {
                            if (cols.length == 1) rvals.add(null);
                            else ((Object[]) rvals.get(0))[idx++] = null;
                        }
                    }
                }
                continue;
            }

            embed = (Embeddable) fms[i].getStrategy();
            ecols = embed.getColumns();
            if (ecols.length == 0)
                continue;

            cval = (em == null) ? null : getValue(embed, em, i);
            cval = embed.toEmbeddedDataStoreValue(cval, store);
            if (cols.length == 1) {
                // rvals is empty
                rvals.add(cval); // save the return value
            } else if (ecols.length == 1) {
                Object rval = rvals.get(0);
                ((Object[]) rval)[idx++] = cval;
            } else {
                Object rval = rvals.get(0);
                System.arraycopy(cval, 0, rval, idx, ecols.length);
                idx += ecols.length;
            }
        }
        return idx;
    }

    /**
     * Find the target entity's PK columns for @MapsId column types.
     */
    private Column[] findMapsIdTargetCols(ValueMapping vm) {
        if (!(vm instanceof FieldMapping)) return null;
        ClassMapping owner = ((FieldMapping) vm).getDefiningMapping();
        for (FieldMapping f : owner.getFieldMappings()) {
            if (f.getMappedByIdValue() != null
                && f.getDeclaredTypeMetaData() != null) {
                ClassMapping target =
                    (ClassMapping) f.getDeclaredTypeMetaData();
                if (target.getTable() != null)
                    return target.getPrimaryKeyColumns();
            }
        }
        return null;
    }

    private Object getValue(Embeddable embed, OpenJPAStateManager sm, int idx) {
        if (embed instanceof MaxEmbeddedLobFieldStrategy) {
            return ((MaxEmbeddedLobFieldStrategy)embed).getValue(sm);
        }
        return sm.fetch(idx);
    }

    /**
     * Helper to convert a datastore value to its object equivalent.
     *
     * @param em state manager for embedded object
     * @param vm owning value
     * @param val datastore value
     * @param store optional store manager
     * @param fetch optional fetch configuration
     * @param cols embedded columns
     * @param idx index in columns array to start
     */
    protected void toObjectValue(OpenJPAStateManager em, ValueMapping vm,
            Object val, JDBCStore store, JDBCFetchConfiguration fetch,
            Column[] cols, int idx)
            throws SQLException {
        toObjectValue1(em, vm, val, store, fetch, cols, idx);
    }

    protected int toObjectValue1(OpenJPAStateManager em, ValueMapping vm,
        Object val, JDBCStore store, JDBCFetchConfiguration fetch,
        Column[] cols, int idx)
        throws SQLException {
        FieldMapping[] fms = vm.getEmbeddedMapping().getFieldMappings();
        Embeddable embed;
        Object cval;
        Column[] ecols;
        for (FieldMapping fm : fms) {
            if (fm.getManagement() != FieldMetaData.MANAGE_PERSISTENT)
                continue;

            ValueMapping vm1 = fm.getValueMapping();
            OpenJPAStateManager em1 = null;

            // JPA 2.4.1.3 ex2b: non-@Embeddable @IdClass field —
            // reconstruct POJO from FK column values via reflection
            if (!(fm.getStrategy() instanceof Embeddable)) {
                List<Column> mic = fm.getValueInfo().getMapsIdColumns();
                if (mic != null && !mic.isEmpty()) {
                    try {
                        Object idObj = fm.getDeclaredType()
                            .getDeclaredConstructor().newInstance();
                        java.lang.reflect.Field[] df =
                            fm.getDeclaredType().getDeclaredFields();
                        int ci = 0;
                        for (java.lang.reflect.Field f : df) {
                            if (java.lang.reflect.Modifier
                                    .isStatic(f.getModifiers()))
                                continue;
                            if (ci >= mic.size()) break;
                            f.setAccessible(true);
                            Object cv;
                            if (val instanceof Object[])
                                cv = ((Object[]) val)[idx + ci];
                            else
                                cv = val;
                            f.set(idObj, cv);
                            ci++;
                        }
                        em.store(fm.getIndex(), idObj);
                    } catch (Exception ex) {
                        // field stays null
                    }
                    idx += mic.size();
                }
                continue;
            }

            embed = (Embeddable) fm.getStrategy();
            if (vm1.getEmbeddedMapping() != null) {
                if (em instanceof StateManagerImpl) {
                    em1 = store.getContext().embed(null, null, em, vm1);
                    idx = toObjectValue1(em1, vm1, val, store, fetch, cols, idx);
                }
                else if (em instanceof ObjectIdStateManager) {
                    em1 = new ObjectIdStateManager(null, null, vm1);
                    idx = toObjectValue1(em1, vm1, val, store, null,
                            getColumns(fm), idx);
                }
                if (em1 != null) {
                    cval = em1.getManagedInstance();
                }
                else {
                    cval = null;
                }
            }
            else {
                ecols = embed.getColumns();
                if (ecols.length == 0)
                    cval = null;
                else if (idx == 0 && ecols.length == cols.length)
                    cval = val;
                else if (ecols.length == 1)
                    cval = ((Object[]) val)[idx++];
                else {
                    cval = new Object[ecols.length];
                    System.arraycopy(val, idx, cval, 0, ecols.length);
                    idx += ecols.length;
                }
            }

            if (store != null && em instanceof StateManagerImpl)
                embed.loadEmbedded(em, store, fetch, cval);
            else {
                if (!(em instanceof ObjectIdStateManager))
                    cval = embed.toEmbeddedObjectValue(cval);
                if (fm.getHandler() != null)
                    cval = fm.getHandler().toObjectValue(fm, cval);

                em.store(fm.getIndex(), cval);
            }
        }
        return idx;
    }
    private Column[] getColumns(FieldMapping fm) {
        List<Column> colList = new ArrayList<>();
        getEmbeddedIdCols(fm, colList);
        Column[] cols = new Column[colList.size()];
        int i = 0;
        for (Column col : colList) {
            cols[i++] = col;
        }
        return cols;
    }

    public static void getEmbeddedIdCols(FieldMapping fmd, List cols) {
        ClassMapping embed = fmd.getEmbeddedMapping();
        if (embed == null) {
            // Non-@Embeddable @IdClass field with @MapsId columns
            if (fmd.hasMapsIdCols()) {
                List<Column> mapsIdCols = fmd.getValueInfo()
                    .getMapsIdColumns();
                for (Object col : mapsIdCols) {
                    Column newCol = new Column();
                    newCol.copy((Column) col);
                    cols.add(newCol);
                }
            }
            return;
        }
        FieldMapping[] fmds = embed.getFieldMappings();
        for (FieldMapping fieldMapping : fmds) {
            if (fieldMapping.getValue().getEmbeddedMetaData() == null) {
                getIdColumns(fieldMapping, cols);
            }
            else {
                getEmbeddedIdCols(fieldMapping, cols);
            }
        }
    }

    public static void getIdColumns(FieldMapping fmd, List cols) {
        Column[] pkCols =  ((ValueMappingImpl)fmd.getValue()).getColumns();
        for (Column pkCol : pkCols) {
            Column newCol = new Column();
            newCol.copy(pkCol);
            cols.add(newCol);
        }
    }

}
