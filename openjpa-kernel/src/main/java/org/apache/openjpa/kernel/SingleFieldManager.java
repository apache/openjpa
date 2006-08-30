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
package org.apache.openjpa.kernel;

import java.io.IOException;
import java.io.ObjectOutput;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.ChangeTracker;
import org.apache.openjpa.util.Exceptions;
import org.apache.openjpa.util.InvalidStateException;
import org.apache.openjpa.util.MapChangeTracker;
import org.apache.openjpa.util.ObjectId;
import org.apache.openjpa.util.Proxies;
import org.apache.openjpa.util.Proxy;
import org.apache.openjpa.util.ProxyManager;
import org.apache.openjpa.util.UserException;

/**
 * FieldManager type used to hold onto a single field value and then
 * dispense it via the fetch methods. The manager can also perform actions
 * on the held field.
 *
 * @author Abe White
 */
class SingleFieldManager
    extends TransferFieldManager {

    private static final Localizer _loc = Localizer.forPackage
        (SingleFieldManager.class);

    private final StateManagerImpl _sm;
    private final BrokerImpl _broker;

    public SingleFieldManager(StateManagerImpl sm, BrokerImpl broker) {
        _sm = sm;
        _broker = broker;
    }

    /**
     * Proxy the held field if needed. Return true if the field needs to
     * be replaced with the now-proxied instance.
     */
    public boolean proxy(boolean reset, boolean replaceNull) {
        FieldMetaData fmd = _sm.getMetaData().getField(field);
        Proxy proxy = null;
        boolean ret = false;
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.DATE:
                if (objval == null)
                    return false;
                proxy = checkProxy();
                if (proxy == null) {
                    proxy = (Proxy) _sm.newFieldProxy(field);
                    ((Date) proxy).setTime(((Date) objval).getTime());
                    if (proxy instanceof Timestamp &&
                        objval instanceof Timestamp)
                        ((Timestamp) proxy).setNanos(((Timestamp) objval).
                            getNanos());
                    ret = true;
                }
                break;
            case JavaTypes.CALENDAR:
                if (objval == null)
                    return false;
                proxy = checkProxy();
                if (proxy == null) {
                    proxy = (Proxy) _sm.newFieldProxy(field);
                    if (objval != null) {
                        Calendar pcal = (Calendar) proxy;
                        Calendar ocal = (Calendar) objval;
                        pcal.setTime(ocal.getTime());
                        pcal.setTimeZone(ocal.getTimeZone());
                    }
                    ret = true;
                }
                break;
            case JavaTypes.COLLECTION:
                if (objval == null && !replaceNull)
                    return false;
                proxy = checkProxy();
                if (proxy == null) {
                    proxy = (Proxy) _sm.newFieldProxy(field);
                    if (objval != null)
                        ((Collection) proxy).addAll((Collection) objval);
                    ret = true;
                }
                break;
            case JavaTypes.MAP:
                if (objval == null && !replaceNull)
                    return false;
                proxy = checkProxy();
                if (proxy == null) {
                    proxy = (Proxy) _sm.newFieldProxy(field);
                    if (objval != null)
                        ((Map) proxy).putAll((Map) objval);
                    ret = true;
                }
                break;
            case JavaTypes.OBJECT:
                if (objval == null)
                    return false;
                proxy = checkProxy();
                if (proxy == null) {
                    proxy = getProxyManager().newCustomProxy(objval);
                    ret = proxy != null;
                }
        }

        if (proxy != null) {
            proxy.setOwner(_sm, field);
            ChangeTracker tracker = proxy.getChangeTracker();
            if (reset && tracker != null) {
                if (fmd.getDeclaredTypeCode() == JavaTypes.MAP) {
                    // track values if key is derived from value, else keys
                    boolean keys = fmd.getKey().getValueMappedBy() == null;
                    ((MapChangeTracker) tracker).setTrackKeys(keys);
                }
                tracker.startTracking();
            }
            objval = proxy;
        }
        return ret;
    }

    /**
     * If the current field is a usable proxy, return it; else return null.
     */
    private Proxy checkProxy() {
        if (!(objval instanceof Proxy))
            return null;

        Proxy proxy = (Proxy) objval;
        if (proxy.getOwner() == null || Proxies.isOwner(proxy, _sm, field))
            return proxy;
        return null;
    }

    /**
     * Unproxies the current field if needed.
     */
    public void unproxy() {
        if (objval == null)
            return;

        FieldMetaData fmd = _sm.getMetaData().getField(field);
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.COLLECTION:
            case JavaTypes.MAP:
            case JavaTypes.DATE:
            case JavaTypes.OBJECT:
                if (objval instanceof Proxy) {
                    Proxy proxy = (Proxy) objval;
                    proxy.setOwner(null, -1);
                    if (proxy.getChangeTracker() != null)
                        proxy.getChangeTracker().stopTracking();
                }
        }
    }

    /**
     * Release the currently embedded field (make it transient).
     */
    public void releaseEmbedded() {
        if (objval == null)
            return;

        FieldMetaData fmd = _sm.getMetaData().getField(field);
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.PC:
                if (fmd.isEmbeddedPC())
                    releaseEmbedded(fmd, objval);
                break;
            case JavaTypes.ARRAY:
                if (fmd.getElement().isEmbeddedPC())
                    releaseEmbedded(fmd.getElement(), (Object[]) objval);
                break;
            case JavaTypes.COLLECTION:
                if (fmd.getElement().isEmbeddedPC())
                    releaseEmbedded(fmd.getElement(), (Collection) objval);
                break;
            case JavaTypes.MAP:
                if (fmd.getKey().isEmbeddedPC())
                    releaseEmbedded(fmd.getKey(), ((Map) objval).keySet());
                if (fmd.getElement().isEmbeddedPC())
                    releaseEmbedded(fmd.getElement(), ((Map) objval).values());
                break;
        }
    }

    /**
     * Release the given embedded objects.
     */
    private void releaseEmbedded(ValueMetaData vmd, Object[] objs) {
        for (int i = 0; i < objs.length; i++)
            releaseEmbedded(vmd, objs[i]);
    }

    /**
     * Release the given embedded objects.
     */
    private void releaseEmbedded(ValueMetaData vmd, Collection objs) {
        for (Iterator itr = objs.iterator(); itr.hasNext();)
            releaseEmbedded(vmd, itr.next());
    }

    /**
     * Release the given embedd object.
     */
    private void releaseEmbedded(ValueMetaData vmd, Object obj) {
        if (obj == null)
            return;

        StateManagerImpl sm = _broker.getStateManagerImpl(obj, false);
        if (sm != null && sm.getOwner() == _sm
            && sm.getOwnerMetaData() == vmd)
            sm.release(true);
    }

    /**
     * Persist the stored field safely, preventing infinite recursion using
     * the given set of already-persisted objects. This method is only called
     * for fields that we know have cascade-immediate settings.
     */
    public void persist(OpCallbacks call) {
        if (objval == null)
            return;

        FieldMetaData fmd = _sm.getMetaData().getField(field);
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.PC:
            case JavaTypes.PC_UNTYPED:
                if (!_broker.isDetachedNew() && _broker.isDetached(objval))
                    return; // allow but ignore
                _broker.persist(objval, call);
                break;
            case JavaTypes.ARRAY:
                _broker.persistAll(Arrays.asList((Object[]) objval), call);
                break;
            case JavaTypes.COLLECTION:
                _broker.persistAll((Collection) objval, call);
                break;
            case JavaTypes.MAP:
                if (fmd.getKey().getCascadePersist()
                    == ValueMetaData.CASCADE_IMMEDIATE)
                    _broker.persistAll(((Map) objval).keySet(), call);
                if (fmd.getElement().getCascadePersist()
                    == ValueMetaData.CASCADE_IMMEDIATE)
                    _broker.persistAll(((Map) objval).values(), call);
                break;
        }
    }

    /**
     * Delete and/or dereference field values.
     */
    public void delete(OpCallbacks call) {
        delete(true, call);
    }

    /**
     * Dereference field values.
     */
    public void dereferenceDependent() {
        delete(false, null);
    }

    /**
     * Delete or dereference the stored field as necessary.
     */
    private void delete(boolean immediate, OpCallbacks call) {
        if (objval == null)
            return;

        FieldMetaData fmd = _sm.getMetaData().getField(field);
        if (fmd.getCascadeDelete() != ValueMetaData.CASCADE_NONE) {
            // immediate cascade works on field value; dependent deref
            // works on external value
            if ((immediate || fmd.isEmbeddedPC())
                && fmd.getCascadeDelete() == ValueMetaData.CASCADE_IMMEDIATE)
                delete(fmd, objval, call);
            else if (fmd.getCascadeDelete() == ValueMetaData.CASCADE_AUTO)
                dereferenceDependent(fmd.getExternalValue(objval, _broker));
            return;
        }

        Object external = null;
        ValueMetaData vmd = fmd.getKey();
        if ((immediate || vmd.isEmbeddedPC())
            && vmd.getCascadeDelete() == ValueMetaData.CASCADE_IMMEDIATE)
            delete(vmd, ((Map) objval).keySet(), call);
        else if (vmd.getCascadeDelete() == ValueMetaData.CASCADE_AUTO) {
            external = fmd.getExternalValue(objval, _broker);
            if (external == null)
                return;
            dereferenceDependent(((Map) external).keySet());
        }

        vmd = fmd.getElement();
        if ((immediate || vmd.isEmbeddedPC())
            && vmd.getCascadeDelete() == ValueMetaData.CASCADE_IMMEDIATE) {
            switch (fmd.getDeclaredTypeCode()) {
                case JavaTypes.COLLECTION:
                    delete(vmd, (Collection) objval, call);
                    break;
                case JavaTypes.ARRAY:
                    delete(vmd, (Object[]) objval, call);
                    break;
                case JavaTypes.MAP:
                    delete(vmd, ((Map) objval).values(), call);
                    break;
            }
        } else if (vmd.getCascadeDelete() == ValueMetaData.CASCADE_AUTO) {
            if (external == null) {
                external = fmd.getExternalValue(objval, _broker);
                if (external == null)
                    return;
            }

            switch (fmd.getTypeCode()) {
                case JavaTypes.COLLECTION:
                    dereferenceDependent((Collection) external);
                    break;
                case JavaTypes.ARRAY:
                    dereferenceDependent((Object[]) external);
                    break;
                case JavaTypes.MAP:
                    dereferenceDependent(((Map) external).values());
                    break;
            }
        }
    }

    /**
     * Delete the objects in the given value.
     */
    private void delete(ValueMetaData vmd, Object[] objs, OpCallbacks call) {
        for (int i = 0; i < objs.length; i++)
            delete(vmd, objs[i], call);
    }

    /**
     * Delete the objects embedded in the given value.
     */
    private void delete(ValueMetaData vmd, Collection objs, OpCallbacks call) {
        for (Iterator itr = objs.iterator(); itr.hasNext();)
            delete(vmd, itr.next(), call);
    }

    /**
     * Delete an object embedded in the given value.
     */
    void delete(ValueMetaData vmd, Object obj, OpCallbacks call) {
        if (obj == null)
            return;

        // delete if unknowned or this isn't an embedded field or if owned by us
        StateManagerImpl sm = _broker.getStateManagerImpl(obj, false);
        if (sm != null && (sm.getOwner() == null || !vmd.isEmbeddedPC()
            || (sm.getOwner() == _sm && sm.getOwnerMetaData() == vmd)))
            _broker.delete(sm.getManagedInstance(), sm, call);
    }

    /**
     * Dereference all valid persistent objects in the given collection.
     */
    private void dereferenceDependent(Object[] objs) {
        for (int i = 0; i < objs.length; i++)
            dereferenceDependent(objs[i]);
    }

    /**
     * Dereference all valid persistent objects in the given collection.
     */
    private void dereferenceDependent(Collection objs) {
        for (Iterator itr = objs.iterator(); itr.hasNext();)
            dereferenceDependent(itr.next());
    }

    /**
     * Dereference the given object.
     */
    void dereferenceDependent(Object obj) {
        if (obj == null)
            return;
        StateManagerImpl sm = _broker.getStateManagerImpl(obj, false);
        if (sm != null)
            sm.setDereferencedDependent(true, true);
    }

    /**
     * Recursively invoke the broker to gather cascade-refresh objects in
     * the current field into the given set. This method is only called
     * for fields that we know have cascade-refresh settings.
     */
    public void gatherCascadeRefresh(OpCallbacks call) {
        if (objval == null)
            return;

        FieldMetaData fmd = _sm.getMetaData().getField(field);
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.PC:
            case JavaTypes.PC_UNTYPED:
                _broker.gatherCascadeRefresh(objval, call);
                break;
            case JavaTypes.ARRAY:
                gatherCascadeRefresh((Object[]) objval, call);
                break;
            case JavaTypes.COLLECTION:
                gatherCascadeRefresh((Collection) objval, call);
                break;
            case JavaTypes.MAP:
                if (fmd.getKey().getCascadeRefresh()
                    == ValueMetaData.CASCADE_IMMEDIATE)
                    gatherCascadeRefresh(((Map) objval).keySet(), call);
                if (fmd.getElement().getCascadeRefresh()
                    == ValueMetaData.CASCADE_IMMEDIATE)
                    gatherCascadeRefresh(((Map) objval).values(), call);
                break;
        }
    }

    /**
     * Gather each element.
     */
    private void gatherCascadeRefresh(Object[] arr, OpCallbacks call) {
        for (int i = 0; i < arr.length; i++)
            _broker.gatherCascadeRefresh(arr[i], call);
    }

    /**
     * Gather each element.
     */
    private void gatherCascadeRefresh(Collection coll, OpCallbacks call) {
        for (Iterator itr = coll.iterator(); itr.hasNext();)
            _broker.gatherCascadeRefresh(itr.next(), call);
    }

    /**
     * Perform pre-flush tasks on the current field. This includes checking
     * for nulls, persisting pcs, embedding embedded fields, and ref'ing
     * pc fields. Return true if the field needs to be replaced with the
     * new value.
     */
    public boolean preFlush(OpCallbacks call) {
        // only care about object fields
        FieldMetaData fmd = _sm.getMetaData().getField(field);
        if (fmd.getDeclaredTypeCode() < JavaTypes.OBJECT)
            return false;

        // perform pers-by-reach and dependent refs
        boolean ret = preFlush(fmd, call);

        // manage inverses
        InverseManager manager = _broker.getInverseManager();
        if (manager != null)
            manager.correctRelations(_sm, fmd, objval);
        return ret;
    }

    /**
     * Return true if the last-provided field has a default value.
     */
    public boolean isDefaultValue() {
        return dblval == 0 && longval == 0
            && (objval == null || "".equals(objval));
    }

    /**
     * Write the stored field or its default value to the given stream.
     */
    public void serialize(ObjectOutput out, boolean def)
        throws IOException {
        FieldMetaData fmd = _sm.getMetaData().getField(field);
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
                out.writeBoolean(!def && longval == 1);
                break;
            case JavaTypes.BYTE:
                out.writeByte((def) ? (byte) 0 : (byte) longval);
                break;
            case JavaTypes.CHAR:
                out.writeChar((def) ? (char) 0 : (char) longval);
                break;
            case JavaTypes.DOUBLE:
                out.writeDouble((def) ? 0D : dblval);
                break;
            case JavaTypes.FLOAT:
                out.writeFloat((def) ? 0F : (float) dblval);
                break;
            case JavaTypes.INT:
                out.writeInt((def) ? 0 : (int) longval);
                break;
            case JavaTypes.LONG:
                out.writeLong((def) ? 0L : longval);
                break;
            case JavaTypes.SHORT:
                out.writeShort((def) ? (short) 0 : (short) longval);
                break;
            default:
                out.writeObject((def) ? null : objval);
        }
    }

    /**
     * Helper method to perform pre flush actions on the current object.
     */
    private boolean preFlush(FieldMetaData fmd, OpCallbacks call) {
        // check for illegal nulls
        if (objval == null) {
            if (fmd.getNullValue() == FieldMetaData.NULL_EXCEPTION
                || fmd.getDeclaredTypeCode() == JavaTypes.OID)
                throw new InvalidStateException(_loc.get("null-value",
                    fmd.getName(), _sm.getManagedInstance())).
                    setFatal(true);
            return false;
        }

        // nothing else to do for non-persistent
        if (fmd.getManagement() != FieldMetaData.MANAGE_PERSISTENT)
            return false;

        // don't allow managed objectid field value
        if (fmd.getDeclaredTypeCode() == JavaTypes.OID) {
            _sm.assertNotManagedObjectId(objval);
            if (_sm.getObjectId() != null
                && !objval.equals(((ObjectId) _sm.getObjectId()).getId()))
                throw new InvalidStateException(_loc.get("changed-oid",
                    _sm.getObjectId(), objval,
                    Exceptions.toString(_sm.getManagedInstance()))).
                    setFatal(true);
        }

        // check for pcs in field value
        if (preFlush(fmd, fmd.getDeclaredTypeCode(),
            fmd.getKey().getDeclaredTypeCode(),
            fmd.getElement().getDeclaredTypeCode(), false, call))
            return true;

        // also check for pcs in externalized values
        if (fmd.isExternalized())
            preFlush(fmd, fmd.getTypeCode(), fmd.getKey().getTypeCode(),
                fmd.getElement().getTypeCode(), true, call);
        return false;
    }

    /**
     * Make new objects persistent and ref other objects so referenced
     * dependent objects won't be deleted.
     */
    private boolean preFlush(FieldMetaData fmd, int type, int keyType,
        int elemType, boolean external, OpCallbacks call) {
        Object val = objval;
        if (val == null)
            return false;

        boolean copy = false;
        switch (type) {
            case JavaTypes.PC:
                if (fmd.isEmbeddedPC()) {
                    objval = embed(fmd, val);
                    copy = true;
                } else {
                    if (external)
                        val = fmd.getExternalValue(val, _broker);
                    if (val != null)
                        preFlushPC(fmd, val, call);
                }
                break;
            case JavaTypes.PC_UNTYPED:
                if (external)
                    val = fmd.getExternalValue(val, _broker);
                if (val != null)
                    preFlushPC(fmd, val, call);
                break;
            case JavaTypes.ARRAY:
                if (fmd.getElement().isEmbeddedPC())
                    embed(fmd.getElement(), (Object[]) val);
                else if (elemType == JavaTypes.PC
                    || elemType == JavaTypes.PC_UNTYPED) {
                    if (external)
                        val = fmd.getExternalValue(val, _broker);
                    if (val != null)
                        preFlushPCs(fmd.getElement(), (Object[]) val, call);
                }
                break;
            case JavaTypes.COLLECTION:
                if (fmd.getElement().isEmbeddedPC()) {
                    objval = embed(fmd.getElement(), (Collection) val);
                    copy = true;
                } else if (elemType == JavaTypes.PC
                    || elemType == JavaTypes.PC_UNTYPED) {
                    boolean flushed = false;
                    if (external)
                        val = fmd.getExternalValue(val, _broker);
                    else if (val instanceof Proxy) {
                        // shortcut change trackers; also ensures we don't iterate
                        // lrs fields
                        ChangeTracker ct = ((Proxy) val).getChangeTracker();
                        if (ct != null && ct.isTracking()) {
                            preFlushPCs(fmd.getElement(), ct.getAdded(), call);
                            preFlushPCs(fmd.getElement(), ct.getChanged(),
                                call);
                            flushed = true;
                        }
                    }
                    if (!flushed && val != null)
                        preFlushPCs(fmd.getElement(), (Collection) val, call);
                }
                break;
            case JavaTypes.MAP:
                boolean keyEmbed = fmd.getKey().isEmbeddedPC();
                boolean valEmbed = fmd.getElement().isEmbeddedPC();
                if (keyEmbed || valEmbed) {
                    objval = embed(fmd, (Map) val, keyEmbed, valEmbed);
                    copy = keyEmbed;
                }

                if (!keyEmbed && (keyType == JavaTypes.PC
                    || keyType == JavaTypes.PC_UNTYPED)) {
                    boolean flushed = false;
                    if (external) {
                        val = fmd.getExternalValue(val, _broker);
                        external = false;
                    } else if (val instanceof Proxy) {
                        // shortcut change trackers; also ensures we don't iterate
                        // lrs fields
                        MapChangeTracker ct = (MapChangeTracker) ((Proxy) val).
                            getChangeTracker();
                        if (ct != null && ct.isTracking() && ct.getTrackKeys())
                        {
                            preFlushPCs(fmd.getKey(), ct.getAdded(), call);
                            preFlushPCs(fmd.getKey(), ct.getChanged(), call);
                            flushed = true;
                        }
                    }
                    if (!flushed && val != null)
                        preFlushPCs(fmd.getKey(), ((Map) val).keySet(), call);
                }

                if (!valEmbed && (elemType == JavaTypes.PC
                    || elemType == JavaTypes.PC_UNTYPED)) {
                    boolean flushed = false;
                    if (external)
                        val = fmd.getExternalValue(val, _broker);
                    else if (val instanceof Proxy) {
                        // shortcut change trackers; also ensures we don't iterate
                        // lrs fields
                        MapChangeTracker ct = (MapChangeTracker) ((Proxy) val).
                            getChangeTracker();
                        if (ct != null && ct.isTracking()) {
                            if (ct.getTrackKeys()) {
                                preFlushPCs(fmd.getElement(), ct.getAdded(),
                                    (Map) val, call);
                                preFlushPCs(fmd.getElement(), ct.getChanged(),
                                    (Map) val, call);
                            } else {
                                preFlushPCs(fmd.getElement(), ct.getAdded(),
                                    call);
                                preFlushPCs(fmd.getElement(), ct.getChanged(),
                                    call);
                            }
                            flushed = true;
                        }
                    }
                    if (!flushed && val != null)
                        preFlushPCs(fmd.getElement(), ((Map) val).values(),
                            call);
                }
                break;
        }
        return copy;
    }

    /**
     * Make new objects persistent and ref all valid persistent objects for
     * the given keys.
     */
    private void preFlushPCs(ValueMetaData vmd, Collection keys, Map map,
        OpCallbacks call) {
        for (Iterator itr = keys.iterator(); itr.hasNext();)
            preFlushPC(vmd, map.get(itr.next()), call);
    }

    /**
     * Make new objects persistent and ref all valid persistent objects in
     * the given array.
     */
    private void preFlushPCs(ValueMetaData vmd, Object[] objs,
        OpCallbacks call) {
        for (int i = 0; i < objs.length; i++)
            preFlushPC(vmd, objs[i], call);
    }

    /**
     * Make new objects persistent and ref all valid persistent objects in
     * the given collection.
     */
    private void preFlushPCs(ValueMetaData vmd, Collection objs,
        OpCallbacks call) {
        for (Iterator itr = objs.iterator(); itr.hasNext();)
            preFlushPC(vmd, itr.next(), call);
    }

    /**
     * Perform pre flush operations on the given object.
     */
    private void preFlushPC(ValueMetaData vmd, Object obj, OpCallbacks call) {
        if (obj == null)
            return;

        OpenJPAStateManager sm;
        if (vmd.getCascadePersist() == ValueMetaData.CASCADE_NONE) {
            if (!_broker.isDetachedNew() && _broker.isDetached(obj))
                return; // allow but ignore

            sm = _broker.getStateManager(obj);
            if (sm == null || !sm.isPersistent())
                throw new InvalidStateException
                    (_loc.get("cant-cascade-persist",
                        Exceptions.toString(obj), vmd,
                        Exceptions.toString(_sm.getManagedInstance()))).
                    setFailedObject(obj);
        } else
            sm = _broker.persist(obj, null, call);

        if (sm != null) {
            // if deleted and not managed inverse, die
            if (sm.isDeleted() && (_broker.getInverseManager() == null
                || vmd.getFieldMetaData().getInverseMetaDatas().length == 0))
                throw new UserException(_loc.get("ref-to-deleted",
                    Exceptions.toString(obj), vmd,
                    Exceptions.toString(_sm.getManagedInstance()))).
                    setFailedObject(obj);
            ((StateManagerImpl) sm).setDereferencedDependent(false, true);
        }
    }

    /**
     * Make all elements of the given array embedded.
     */
    private void embed(ValueMetaData vmd, Object[] arr) {
        for (int i = 0; i < arr.length; i++)
            arr[i] = embed(vmd, arr[i]);
    }

    /**
     * Create a copy of the given collection containing embedded elements.
     */
    private Collection embed(ValueMetaData vmd, Collection orig) {
        // we have to copy to get a collection of the right type and size,
        // though we immediately clear it
        Collection coll = getProxyManager().copyCollection(orig);
        if (coll == null)
            throw new UserException(_loc.get("not-copyable",
                vmd.getFieldMetaData()));

        coll.clear();
        for (Iterator itr = orig.iterator(); itr.hasNext();)
            coll.add(embed(vmd, itr.next()));
        return coll;
    }

    /**
     * Embed the elements of the given map.
     */
    private Map embed(FieldMetaData fmd, Map orig, boolean keyEmbed,
        boolean valEmbed) {
        Map map;
        Map.Entry entry;

        // if we have to replace keys, we need to copy the map; otherwise
        // we can mutate the values directly
        if (keyEmbed) {
            // we have to copy to get a collection of the right type and size,
            // though we immediately clear it
            map = getProxyManager().copyMap(orig);
            if (map == null)
                throw new UserException(_loc.get("not-copyable", fmd));

            map.clear();
            Object key, val;
            for (Iterator itr = orig.entrySet().iterator(); itr.hasNext();) {
                entry = (Map.Entry) itr.next();
                key = embed(fmd.getKey(), entry.getKey());
                val = entry.getValue();
                if (valEmbed)
                    val = embed(fmd.getElement(), val);
                map.put(key, val);
            }
        } else {
            map = orig;
            for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
                entry = (Map.Entry) itr.next();
                entry.setValue(embed(fmd.getElement(),
                    entry.getValue()));
            }
        }
        return map;
    }

    /**
     * Make the given object embedded.
     */
    private Object embed(ValueMetaData vmd, Object obj) {
        if (obj == null)
            return null;
        return _broker.embed(obj, null, _sm, vmd).getManagedInstance();
    }

    /**
     * Return the proxy manager.
     */
    private ProxyManager getProxyManager ()
	{
		return _broker.getConfiguration ().getProxyManagerInstance ();
	}
}
