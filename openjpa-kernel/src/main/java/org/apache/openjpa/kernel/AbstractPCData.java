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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.openjpa.event.OrphanedKeyAction;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.ChangeTracker;
import org.apache.openjpa.util.Proxy;

/**
 * Abstract base class which implements core PCData behavior.
 *
 * @author Patrick Linskey
 * @author Abe White
 * @nojavadoc
 */
public abstract class AbstractPCData
    implements PCData {

    public static final Object NULL = new Object();

    /**
     * Return the loaded field mask.
     */
    public abstract BitSet getLoaded();

    /**
     * Create a new pcdata for holding the state of an embedded instance.
     */
    public abstract AbstractPCData newEmbeddedPCData(OpenJPAStateManager sm);

    public boolean isLoaded(int field) {
        return getLoaded().get(field);
    }

    /**
     * Transform the given data value into its field value.
     */
    protected Object toField(OpenJPAStateManager sm, FieldMetaData fmd,
        Object data, FetchState fetchState, Object context) {
        if (data == null)
            return null;

        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.COLLECTION:
                ProxyDataList c = (ProxyDataList) data;
                Collection c2 = (Collection) sm.newFieldProxy(fmd.getIndex());
                for (int i = 0; i < c.size(); i++)
                    c2.add(toNestedField(sm, fmd.getElement(), c.get(i),
                        fetchState, context));
                if (c2 instanceof Proxy) {
                    ChangeTracker ct = ((Proxy) c2).getChangeTracker();
                    if (ct != null)
                        ct.setNextSequence(c.nextSequence);
                }
                return c2;
            case JavaTypes.MAP:
                Map m = (Map) data;
                Map m2 = (Map) sm.newFieldProxy(fmd.getIndex());
                Map.Entry e;
                Object key;
                Object value;
                for (Iterator mi = m.entrySet().iterator(); mi.hasNext();) {
                    e = (Map.Entry) mi.next();
                    key = toNestedField(sm, fmd.getKey(), e.getKey(),
                        fetchState, context);
                    value = toNestedField(sm, fmd.getElement(), e.getValue(),
                        fetchState, context);
                    m2.put(key, value);
                }
                return m2;
            case JavaTypes.ARRAY:
                List l = (List) data;
                Object a = Array.newInstance(fmd.getElement().getDeclaredType(),
                    l.size());
                for (int i = 0; i < l.size(); i++) {
                    Array.set(a, i, toNestedField(sm, fmd.getElement(),
                        l.get(i), fetchState, context));
                }
                return a;
            default:
                return toNestedField(sm, fmd, data, fetchState, context);
        }
    }

    /**
     * Transform the given data value to its field value. The data value
     * may be a key, value, or element of a map or collection.
     */
    protected Object toNestedField(OpenJPAStateManager sm, ValueMetaData vmd,
        Object data, FetchState fetchState, Object context) {
        if (data == null)
            return null;

        switch (vmd.getDeclaredTypeCode()) {
            case JavaTypes.DATE:
                return ((Date) data).clone();
            case JavaTypes.LOCALE:
                return (Locale) data;
            case JavaTypes.PC:
                if (vmd.isEmbedded())
                    return toEmbeddedField(sm, vmd, data, fetchState, context);
                // no break
            case JavaTypes.PC_UNTYPED:
                Object ret =
                    toRelationField(sm, vmd, data, fetchState, context);
                if (ret != null)
                    return ret;
                OrphanedKeyAction action = sm.getContext().getConfiguration().
                    getOrphanedKeyActionInstance();
                return action.orphan(data, sm, vmd);
            default:
                return data;
        }
    }

    /**
     * Transform the given data into a relation field value. Default
     * implementation assumes the data is an oid.
     */
    protected Object toRelationField(OpenJPAStateManager sm, ValueMetaData vmd,
        Object data, FetchState fetchState, Object context) {
        return sm.getContext().find(data, fetchState, null, null, 0);
    }

    /**
     * Transform the given data into an embedded PC field value. Default
     * implementation assumes the data is an {@link AbstractPCData}.
     */
    protected Object toEmbeddedField(OpenJPAStateManager sm, ValueMetaData vmd,
        Object data, FetchState fetchState, Object context) {
        AbstractPCData pcdata = (AbstractPCData) data;
        OpenJPAStateManager embedded = sm.getContext().embed(null,
            pcdata.getId(), sm, vmd);
        pcdata.load(embedded, (BitSet) pcdata.getLoaded().clone(),
            fetchState, context);
        return embedded.getManagedInstance();
    }

    /**
     * Transform the given field value to a data value for caching. Return
     * {@link #NULL} if unable to cache.
     */
    protected Object toData(FieldMetaData fmd, Object val, StoreContext ctx) {
        if (val == null)
            return null;

        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.COLLECTION:
                Collection c = (Collection) val;
                if (c.isEmpty())
                    return ProxyDataList.EMPTY_LIST;
                ProxyDataList c2 = null;
                int size;
                for (Iterator ci = c.iterator(); ci.hasNext();) {
                    val = toNestedData(fmd.getElement(), ci.next(), ctx);
                    if (val == NULL)
                        return NULL;
                    if (c2 == null) {
                        size = c.size();
                        c2 = new ProxyDataList(size);
                        if (c instanceof Proxy) {
                            ChangeTracker ct = ((Proxy) c).getChangeTracker();
                            if (ct != null)
                                c2.nextSequence = ct.getNextSequence();
                        } else
                            c2.nextSequence = size;
                    }
                    c2.add(val);
                }
                return c2;
            case JavaTypes.MAP:
                Map m = (Map) val;
                if (m.isEmpty())
                    return Collections.EMPTY_MAP;
                Map m2 = null;
                Map.Entry e;
                Object val2;
                for (Iterator mi = m.entrySet().iterator(); mi.hasNext();) {
                    e = (Map.Entry) mi.next();
                    val = toNestedData(fmd.getKey(), e.getKey(), ctx);
                    if (val == NULL)
                        return NULL;
                    val2 = toNestedData(fmd.getElement(), e.getValue(), ctx);
                    if (val2 == NULL)
                        return NULL;
                    if (m2 == null)
                        m2 = new HashMap(m.size());
                    m2.put(val, val2);
                }
                return m2;
            case JavaTypes.ARRAY:
                Object a = val;
                int length = Array.getLength(a);
                if (length == 0)
                    return Collections.EMPTY_LIST;
                List l = null;
                for (int i = 0; i < length; i++) {
                    val = toNestedData(fmd.getElement(), Array.get(a, i), ctx);
                    if (val == NULL)
                        return NULL;
                    if (l == null)
                        l = new ArrayList(length);
                    l.add(val);
                }
                return l;
            default:
                return toNestedData(fmd, val, ctx);
        }
    }

    /**
     * Transform the given nested value to a cachable value. Return
     * {@link #NULL} if the value cannot be cached.
     */
    protected Object toNestedData(ValueMetaData vmd, Object val,
        StoreContext ctx) {
        if (val == null)
            return null;

        switch (vmd.getDeclaredTypeCode()) {
            case JavaTypes.PC:
                if (vmd.isEmbedded())
                    return toEmbeddedData(val, ctx);
                // no break
            case JavaTypes.PC_UNTYPED:
                return toRelationData(val, ctx);
            case JavaTypes.DATE:
                if (val instanceof Proxy)
                    return ((Proxy) val).copy(val);
                else
                    return ((Date) val).clone();
            case JavaTypes.LOCALE:
                return (Locale) val;
            case JavaTypes.OBJECT:
                if (val instanceof Proxy)
                    return ((Proxy) val).copy(val);
                else
                    return val;
            default:
                return val;
        }
    }

    /**
     * Return the value to cache for the given object. Caches its oid by
     * default.
     */
    protected Object toRelationData(Object val, StoreContext ctx) {
        return ctx.getObjectId(val);
    }

    /**
     * Return the value to cache for the given embedded PC. Caches a
     * {@link PCData} from {@link #newEmbeddedPCData} by default.
     */
    protected Object toEmbeddedData(Object val, StoreContext ctx) {
        if (ctx == null)
            return NULL;

        OpenJPAStateManager sm = ctx.getStateManager(val);
        if (sm == null)
            return NULL;

        // have to cache all data, so make sure it's all loaded
        // ### prevent loading of things that aren't cached (lobs, lrs, etc)
        ctx.retrieve(val, false, null);

        PCData pcdata = newEmbeddedPCData(sm);
        pcdata.store(sm);
        return pcdata;
    }

    /**
     * Tracks proxy data along with list elements.
     */
    private static class ProxyDataList
        extends ArrayList {

        public static final ProxyDataList EMPTY_LIST = new ProxyDataList(0);

        public int nextSequence = 0;

        public ProxyDataList(int size) {
			super (size);
		}
	}
}
