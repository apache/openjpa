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
package org.apache.openjpa.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.FetchConfiguration;
import org.apache.openjpa.kernel.LockManager;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.UUIDGenerator;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.meta.ValueStrategies;

/**
 * Helper for OpenJPA back-ends.
 *
 * @since 0.3.0
 * @author Abe White
 * @nojavadoc
 */
public class ImplHelper {

    /**
     * Helper for store manager implementations. This method simply delegates
     * to the proper singular method for each state manager.
     *
     * @see StoreManager#loadAll
     * @since 0.4.0
     */
    public static Collection loadAll(Collection sms, StoreManager store,
        PCState state, int load, FetchConfiguration fetch, Object context) {
        Collection failed = null;
        OpenJPAStateManager sm;
        LockManager lm;
        for (Iterator itr = sms.iterator(); itr.hasNext();) {
            sm = (OpenJPAStateManager) itr.next();
            if (sm.getManagedInstance() == null) {
                if (!store.initialize(sm, state, fetch, context))
                    failed = addFailedId(sm, failed);
            } else if (load != StoreManager.FORCE_LOAD_NONE
                || sm.getPCState() == PCState.HOLLOW) {
                lm = sm.getContext().getLockManager();
                if (!store.load(sm, sm.getUnloaded(fetch), fetch, 
                    lm.getLockLevel(sm), context))
                    failed = addFailedId(sm, failed);
            } else if (!store.exists(sm, context))
                failed = addFailedId(sm, failed);
        }
        return (failed == null) ? Collections.EMPTY_LIST : failed;
    }

    /**
     * Add identity of given instance to collection.
     */
    private static Collection addFailedId(OpenJPAStateManager sm,
        Collection failed) {
        if (failed == null)
            failed = new ArrayList();
        failed.add(sm.getId());
        return failed;
    }

    /**
     * Generate a value for the given metadata, or return null. Generates
     * values for hte following strategies: {@link ValueStrategies#SEQUENCE},
     * {@link ValueStrategies#UUID_STRING}, {@link ValueStrategies#UUID_HEX}
     */
    public static Object generateIdentityValue(StoreContext ctx,
        ClassMetaData meta, int typeCode) {
        return generateValue(ctx, meta, null, typeCode);
    }

    /**
     * Generate a value for the given metadata, or return null. Generates
     * values for hte following strategies: {@link ValueStrategies#SEQUENCE},
     * {@link ValueStrategies#UUID_STRING}, {@link ValueStrategies#UUID_HEX}
     */
    public static Object generateFieldValue(StoreContext ctx,
        FieldMetaData fmd) {
        return generateValue(ctx, fmd.getDefiningMetaData(), fmd, 
            fmd.getDeclaredTypeCode());
    }

    /**
     * Generate a value for the given metadaa.
     */
    private static Object generateValue(StoreContext ctx,
        ClassMetaData meta, FieldMetaData fmd, int typeCode) {
        int strategy = (fmd == null) ? meta.getIdentityStrategy()
            : fmd.getValueStrategy();
        switch (strategy) {
            case ValueStrategies.SEQUENCE:
                SequenceMetaData smd = (fmd == null)
                    ? meta.getIdentitySequenceMetaData()
                    : fmd.getValueSequenceMetaData();
                return JavaTypes.convert(smd.getInstance(ctx.getClassLoader()).
                    next(ctx, meta), typeCode);
            case ValueStrategies.UUID_STRING:
                return UUIDGenerator.nextString();
            case ValueStrategies.UUID_HEX:
                return UUIDGenerator.nextHex();
            default:
                return null;
        }
    }

    /** 
     * Returns the fields of the state that require an update. 
     *  
     * @param  sm  the state to check
     * @return the BitSet of fields that need update, or null if none
     */
    public static BitSet getUpdateFields(OpenJPAStateManager sm) {
        if ((sm.getPCState() == PCState.PDIRTY
            && (!sm.isFlushed() || sm.isFlushedDirty()))
            || (sm.getPCState() == PCState.PNEW && sm.isFlushedDirty())) {
            BitSet dirty = sm.getDirty();
            if (sm.isFlushed()) {
                dirty = (BitSet) dirty.clone();
                dirty.andNot(sm.getFlushed());
            }
            if (dirty.length() > 0)
                return dirty;
        }
        return null;
    }

    /**
     * Close the given resource. The resource can be an extent iterator,
     * query result, large result set relation, or any closeable OpenJPA
     * component.
     */
    public static void close(Object o) {
        try {
            if (o instanceof Closeable)
                ((Closeable) o).close();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Returns true if the specified class is a type that can be managed by
     * OpenJPA.
     *
     * @param type the class to test
     * @return true if the class is manageable.
     */
    public static boolean isManagedType(Class type) {
        return PersistenceCapable.class.isAssignableFrom(type);
    }

    /**
     * Returns true if the specified instance is manageable.
     *
     * @param instance the object to check
     * @return true if the instance is a persistent type, false otherwise
     */
    public static boolean isManageable(Object instance) {
        return instance instanceof PersistenceCapable;
    }
}
