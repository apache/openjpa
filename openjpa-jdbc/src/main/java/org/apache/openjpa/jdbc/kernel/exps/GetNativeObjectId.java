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

import org.apache.openjpa.jdbc.meta.ClassMapping;
import org.apache.openjpa.jdbc.meta.Joinable;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.sql.Result;
import org.apache.openjpa.jdbc.sql.Select;
import org.apache.openjpa.kernel.Filters;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.Id;
import org.apache.openjpa.util.OpenJPAId;

/**
 * Select the id value of an object; typically used in projections.
 *
 * @author Abe White
 * @author Paulo Cristovão Filho
 */
class GetNativeObjectId
    extends GetObjectId {

    
    private static final long serialVersionUID = 1L;

    /**
     * Constructor. Provide the value whose id to extract.
     */
    public GetNativeObjectId(PCPath path) {
    	super(path);
    }

    @Override
    public Object toDataStoreValue(Select sel, ExpContext ctx, ExpState state,
        Object val) {
        // if datastore identity, try to convert to a long value
        ClassMapping mapping = _path.getClassMapping(state);
        if (mapping.getIdentityType() == ClassMetaData.ID_DATASTORE) {
            if (val instanceof Id)
                return ((Id) val).getId();
            return Filters.convert(val, long.class);
        }

        // if unknown identity, can't do much
        if (mapping.getIdentityType() == ClassMetaData.ID_UNKNOWN)
            return (val instanceof OpenJPAId) ? ((OpenJPAId) val).getIdObject() : val;

        // application identity; convert to pk values in the same order as
        // the mapping's primary key columns will be returned
        Object[] pks = ApplicationIds.toPKValues(val, mapping);
        if (pks.length == 1)
            return pks[0];
        if (val == null)
            return pks;
        while (!mapping.isPrimaryKeyObjectId(false))
            mapping = mapping.getJoinablePCSuperclassMapping();

        Column[] cols = mapping.getPrimaryKeyColumns();
        Object[] vals = new Object[cols.length];
        Joinable join;
        for (int i = 0; i < cols.length; i++) {
            join = mapping.assertJoinable(cols[i]);
            vals[i] = pks[mapping.getField(join.getFieldIndex()).
                getPrimaryKeyIndex()];
            vals[i] = join.getJoinValue(vals[i], cols[i], ctx.store);
        }
        return vals;
    }

    @Override
    public Object load(ExpContext ctx, ExpState state, Result res)
        throws SQLException {
    	Object ret = super.load(ctx, state, res);
    	return (ret != null && OpenJPAId.class.isAssignableFrom(ret.getClass()))
    			? ((OpenJPAId) ret).getIdObject() : ret;
    }

}

