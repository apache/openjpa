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
package org.apache.openjpa.datacache;

import java.util.BitSet;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.AbstractPCData;
import org.apache.openjpa.kernel.PCData;
import org.apache.openjpa.kernel.PCDataImpl;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueMetaData;

/**
 * Specialized {@link PCData} implementation for data caching. This
 * implementation is properly synchronized.
 *
 * @author Patrick Linskey
 */
public class DataCachePCDataImpl
    extends PCDataImpl
    implements DataCachePCData {

    private final long _exp;

    /**
     * Constructor.
     */
    public DataCachePCDataImpl(Object oid, ClassMetaData meta) {
        super(oid, meta);

        int timeout = meta.getDataCacheTimeout();
        if (timeout > 0)
            _exp = System.currentTimeMillis() + timeout;
        else
            _exp = -1;
    }

    public boolean isTimedOut() {
        return _exp != -1 && _exp < System.currentTimeMillis();
    }

    public synchronized Object getData(int index) {
        return super.getData(index);
    }

    public synchronized void setData(int index, Object val) {
        super.setData(index, val);
    }

    public synchronized void clearData(int index) {
        super.clearData(index);
    }

    public synchronized Object getImplData() {
        return super.getImplData();
    }

    public synchronized void setImplData(Object val) {
        super.setImplData(val);
    }

    public synchronized Object getImplData(int index) {
        return super.getImplData(index);
    }

    public synchronized void setImplData(int index, Object val) {
        super.setImplData(index, val);
    }

    public synchronized Object getIntermediate(int index) {
        return super.getIntermediate(index);
    }

    public synchronized void setIntermediate(int index, Object val) {
        super.setIntermediate(index, val);
    }

    public synchronized boolean isLoaded(int index) {
        return super.isLoaded(index);
    }

    public synchronized void setLoaded(int index, boolean loaded) {
        super.setLoaded(index, loaded);
    }

    public synchronized Object getVersion() {
        return super.getVersion();
    }

    public synchronized void setVersion(Object version) {
        super.setVersion(version);
    }

    public synchronized void store(OpenJPAStateManager sm) {
        super.store(sm);
    }

    public synchronized void store(OpenJPAStateManager sm, BitSet fields) {
        super.store(sm, fields);
    }

    protected Object toData(FieldMetaData fmd, Object val, StoreContext ctx) {
        // avoid caching large result set fields
        if (fmd.isLRS())
            return NULL;
        return super.toData(fmd, val, ctx);
    }

    protected Object toNestedData(ValueMetaData vmd, Object val,
        StoreContext ctx) {
        if (val == null)
            return null;

        // don't try to cache nested containers
        switch (vmd.getDeclaredTypeCode()) {
            case JavaTypes.COLLECTION:
            case JavaTypes.MAP:
            case JavaTypes.ARRAY:
                return NULL;
            default:
                return super.toNestedData(vmd, val, ctx);
        }
    }

    public AbstractPCData newEmbeddedPCData(OpenJPAStateManager sm) {
        return new DataCachePCDataImpl(sm.getId(), sm.getMetaData());
    }
}
