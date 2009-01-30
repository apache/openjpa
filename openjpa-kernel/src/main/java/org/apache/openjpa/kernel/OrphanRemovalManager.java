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
package org.apache.openjpa.kernel;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;

/**
 * Class which manages orphanRemoval before flushing
 * to the datastore. 
 *
 * @author Fay Wang
 */
public class OrphanRemovalManager  {

    /**
     * Correct relations from the given dirty field to inverse instances.
     * Field <code>fmd</code> of the instance managed by <code>sm</code> has
     * value <code>value</code>. Ensure that all inverses relations from
     * <code>value</code> are consistent with this.
     */
    public static void correctRelations(OpenJPAStateManager sm, FieldMetaData fmd,
        Object value) {
        if (fmd.getDeclaredTypeCode() != JavaTypes.PC &&
            ((fmd.getDeclaredTypeCode() != JavaTypes.COLLECTION  &&
              fmd.getDeclaredTypeCode() != JavaTypes.MAP) ||
                fmd.getElement().getDeclaredTypeCode() != JavaTypes.PC))
            return;

        FieldMetaData[] orphanRemoves = fmd.getOrphanRemovalMetaDatas();
        if (orphanRemoves.length == 0)
            return;

        // clear any restorable relations
        clearOrphanRemovalRelations(sm, fmd, orphanRemoves, value);
    }


    /**
     * Remove all relations between the initial value of <code>fmd</code> for
     * the instance managed by <code>sm</code> and its inverses. Relations
     * shared with <code>newValue</code> can be left intact.
     */
    protected static void clearOrphanRemovalRelations(OpenJPAStateManager sm,
        FieldMetaData fmd, FieldMetaData[] orphanRemoves, Object newValue) {
        // don't bother clearing unflushed new instances
        if (sm.isNew() && !sm.getFlushed().get(fmd.getIndex()))
            return;
        if (fmd.getDeclaredTypeCode() == JavaTypes.PC) {
            Object initial = sm.fetchInitialField(fmd.getIndex());
            clearInverseRelations(sm, initial, fmd, orphanRemoves);
        } else {
            Object obj = sm.fetchInitialField(fmd.getIndex());
            Collection initial = null;
            if (obj instanceof Collection)
                initial = (Collection) obj;
            else if (obj instanceof Map)
                initial = ((Map)obj).values();
            
            if (initial == null)
                return;

            // clear all relations not also in the new value
            Collection coll = null;
            if (newValue instanceof Collection)
                coll = (Collection) newValue;
            else if (newValue instanceof Map)
                coll = ((Map)newValue).values();
            Object elem;
            for (Iterator itr = initial.iterator(); itr.hasNext();) {
                elem = itr.next();
                if (coll == null || !coll.contains(elem))
                    clearInverseRelations(sm, elem, fmd, orphanRemoves);
            }
        }
    }

    /**
     * Clear all inverse the relations from <code>val</code> to the instance
     * managed by <code>sm</code>.
     */
    protected static void clearInverseRelations(OpenJPAStateManager sm, Object val,
        FieldMetaData fmd, FieldMetaData[] orphanRemoves) {
        if (val == null)
            return;
        OpenJPAStateManager other = sm.getContext().getStateManager(val);
        if (other == null || other.isDeleted())
            return;

        for (int i = 0; i < orphanRemoves.length; i++) {

            // if this is the owned side of the relation and has not yet been
            // loaded, no point in setting it now, cause it'll have the correct
            // value the next time it is loaded after the flush
            switch (orphanRemoves[i].getDeclaredTypeCode()) {
                case JavaTypes.PC:
                    if (fmd.getOrphanRemoval() || fmd.getElement().getOrphanRemoval())
                        ((StateManagerImpl)other).delete();
                    break;
                case JavaTypes.COLLECTION:
                    removeFromCollection(other, orphanRemoves[i],
                        sm.getManagedInstance());
                    break;
            }
        }
    }

    /**
     * Remove the given instance from the collection.
     */
    protected static void removeFromCollection(OpenJPAStateManager sm,
        FieldMetaData fmd, Object val) {
        Collection coll = (Collection) sm.fetchObjectField(fmd.getIndex());
        if (coll != null) 
            coll.remove(val);
    }


}
