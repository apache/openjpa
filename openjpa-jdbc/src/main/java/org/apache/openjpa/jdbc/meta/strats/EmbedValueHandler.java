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
package org.apache.openjpa.jdbc.meta.strats;

import java.sql.SQLException;
import java.util.List;

import org.apache.openjpa.jdbc.kernel.JDBCFetchConfiguration;
import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.meta.Embeddable;
import org.apache.openjpa.jdbc.meta.FieldMapping;
import org.apache.openjpa.jdbc.meta.ValueMapping;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.ColumnIO;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.MetaDataException;

/**
 * Base class for embedded value handlers.
 *
 * @author Abe White
 * @since 4.0
 */
public abstract class EmbedValueHandler
    extends AbstractValueHandler {

    private static final Localizer _loc = Localizer.forPackage
        (EmbedValueHandler.class);

    /**
     * Maps embedded value and gathers columns and arguments into given lists.
     */
    protected void map(ValueMapping vm, String name, ColumnIO io,
        boolean adapt, List cols, List args) {
        // have to resolve embedded value to collect its columns
        vm.getEmbeddedMapping().resolve(vm.MODE_META | vm.MODE_MAPPING);

        // gather columns and result arguments
        FieldMapping[] fms = vm.getEmbeddedMapping().getFieldMappings();
        Column[] curCols;
        Object[] curArgs;
        ColumnIO curIO;
        for (int i = 0; i < fms.length; i++) {
            if (fms[i].getManagement() != FieldMapping.MANAGE_PERSISTENT)
                continue;
            if (!(fms[i].getStrategy()instanceof Embeddable))
                throw new MetaDataException(_loc.get("not-embeddable",
                    vm, fms[i]));

            curCols = ((Embeddable) fms[i].getStrategy()).getColumns();
            curIO = ((Embeddable) fms[i].getStrategy()).getColumnIO();
            for (int j = 0; j < curCols.length; j++) {
                io.setInsertable(cols.size(), curIO.isInsertable(j, false));
                io.setNullInsertable(cols.size(),
                    curIO.isInsertable(j, true));
                io.setUpdatable(cols.size(), curIO.isUpdatable(j, false));
                io.setNullUpdatable(cols.size(), curIO.isUpdatable(j, true));
                cols.add(curCols[j]);
            }

            curArgs = ((Embeddable) fms[i].getStrategy()).getResultArguments();
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
        // set rest of columns from fields
        FieldMapping[] fms = vm.getEmbeddedMapping().getFieldMappings();
        Object cval;
        Column[] ecols;
        Embeddable embed;
        for (int i = 0; i < fms.length; i++) {
            if (fms[i].getManagement() != FieldMapping.MANAGE_PERSISTENT)
                continue;
            embed = (Embeddable) fms[i].getStrategy();
            ecols = embed.getColumns();
            if (ecols.length == 0)
                continue;

            cval = (em == null) ? null : em.fetch(i);
            cval = embed.toEmbeddedDataStoreValue(cval, store);
            if (cols.length == 1)
                rval = cval;
            else if (ecols.length == 1)
                ((Object[]) rval)[idx++] = cval;
            else {
                System.arraycopy(cval, 0, rval, idx, ecols.length);
                idx += ecols.length;
            }
        }
        return rval;
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
        FieldMapping[] fms = vm.getEmbeddedMapping().getFieldMappings();
        Embeddable embed;
        Object cval;
        Column[] ecols;
        for (int i = 0; i < fms.length; i++) {
            if (fms[i].getManagement() != FieldMapping.MANAGE_PERSISTENT)
                continue;

            embed = (Embeddable) fms[i].getStrategy();
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

            if (store != null)
                embed.loadEmbedded(em, store, fetch, cval);
            else {
                cval = embed.toEmbeddedObjectValue(cval);
                em.store(fms[i].getIndex(), cval);
            }
        }
    }
}
