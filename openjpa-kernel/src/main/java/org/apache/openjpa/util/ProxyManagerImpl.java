/*
 * Copyright 2006 The Apache Software Foundation.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.util.JavaVersions;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the {@link ProxyManager} interface.
 *
 * @author Abe White
 */
public class ProxyManagerImpl implements ProxyManager, Configurable {

    private static final Localizer _loc = Localizer.forPackage
        (ProxyManagerImpl.class);
    // date proxy cache
    private static final Map _dates = new HashMap();

    static {
        _dates.put(Date.class, new ProxyUtilDate());
        _dates.put(java.sql.Date.class, new ProxySQLDate());
        _dates.put(java.sql.Timestamp.class, new ProxyTimestamp());
        _dates.put(java.sql.Time.class, new ProxyTime());
    }

    // calendar proxy cache
    private static final Map _calendars = new HashMap();

    static {
        ProxyGregorianCalendar cal = new ProxyGregorianCalendar();
        try {
            cal = (ProxyGregorianCalendar) JavaVersions.
                getVersionSpecificClass(ProxyGregorianCalendar.class).
                newInstance();
        } catch (Exception e) {
        }
        _calendars.put(Calendar.class, cal);
        _calendars.put(GregorianCalendar.class, cal);
    }

    // standard collection proxy cache
    private static final Map _stdCollections = new HashMap();

    static {
        ProxyArrayList listTempl = new ProxyArrayList();
        ProxyHashSet setTempl = new ProxyHashSet();
        ProxyTreeSet sortedSetTempl = new ProxyTreeSet();
        _stdCollections.put(Collection.class, listTempl);
        _stdCollections.put(Set.class, setTempl);
        _stdCollections.put(SortedSet.class, sortedSetTempl);
        _stdCollections.put(List.class, listTempl);
        _stdCollections.put(ArrayList.class, listTempl);
        _stdCollections.put(LinkedList.class, new ProxyLinkedList());
        _stdCollections.put(Vector.class, new ProxyVector());
        _stdCollections.put(HashSet.class, setTempl);
        _stdCollections.put(TreeSet.class, sortedSetTempl);
    }

    // standard map proxy cache
    private static final Map _stdMaps = new HashMap();

    static {
        ProxyHashMap mapTempl = new ProxyHashMap();
        ProxyTreeMap sortedMapTempl = new ProxyTreeMap();
        _stdMaps.put(Map.class, mapTempl);
        _stdMaps.put(SortedMap.class, sortedMapTempl);
        _stdMaps.put(HashMap.class, mapTempl);
        _stdMaps.put(TreeMap.class, sortedMapTempl);
        _stdMaps.put(Hashtable.class, new ProxyHashtable());
        _stdMaps.put(Properties.class, new ProxyProperties());
    }

    // allow subclasses to manipulate collection and map templates
    private final Map _collections = new ConcurrentHashMap(_stdCollections);
    private final Map _maps = new ConcurrentHashMap(_stdMaps);
    protected OpenJPAConfiguration conf = null;
    private boolean _trackChanges = true;
    private boolean _assertType = false;

    /**
     * Whether proxies produced by this factory will use {@link ChangeTracker}s
     * to try to cut down on data store operations at the cost of some extra
     * bookkeeping overhead. Defaults to true.
     */
    public boolean getTrackChanges() {
        return _trackChanges;
    }

    /**
     * Whether proxies produced by this factory will use {@link ChangeTracker}s
     * to try to cut down on data store operations at the cost of some extra
     * bookkeeping overhead. Defaults to true.
     */
    public void setTrackChanges(boolean track) {
        _trackChanges = track;
    }

    /**
     * Whether to perform runtime checks to ensure that all elements
     * added to collection and map proxies are the proper element/key/value
     * type as defined by the metadata. Defaults to false.
     */
    public boolean getAssertAllowedType() {
        return _assertType;
    }

    /**
     * Whether to perform runtime checks to ensure that all elements
     * added to collection and map proxies are the proper element/key/value
     * type as defined by the metadata. Defaults to false.
     */
    public void setAssertAllowedType(boolean assertType) {
        _assertType = assertType;
    }

    public Date copyDate(Date orig) {
        return (Date) copy(orig, _dates);
    }

    public Calendar copyCalendar(Calendar orig) {
        return (Calendar) copy(orig, _calendars);
    }

    public Collection copyCollection(Collection orig) {
        return (Collection) copy(orig, _collections);
    }

    public Map copyMap(Map orig) {
        return (Map) copy(orig, _maps);
    }

    /**
     * Internal helper to copy value.
     */
    private static Object copy(Object orig, Map proxies) {
        if (orig == null)
            return null;
        if (orig instanceof Proxy)
            return ((Proxy) orig).copy(orig);
        Class type = orig.getClass();
        Proxy proxy = (Proxy) proxies.get(type);
        if (proxy == null)
            throw new UnsupportedException(_loc.get("bad-proxy", type));
        return proxy.copy(orig);
    }

    public Object copyArray(Object orig) {
        if (orig == null)
            return null;
        try {
            int length = Array.getLength(orig);
            Object array = Array.newInstance(orig.getClass().
                getComponentType(), length);
            System.arraycopy(orig, 0, array, 0, length);
            return array;
        } catch (Exception e) {
            throw new UnsupportedException(_loc.get("bad-array",
                e.getMessage()), e);
        }
    }

    public Object copyCustom(Object orig) {
        if (!(orig instanceof Proxy))
            return null;
        return ((Proxy) orig).copy(orig);
    }

    public Proxy newDateProxy(Class type) {
        ProxyDate pd = (ProxyDate) findProxy(type, ProxyDate.class, _dates);
        return pd.newInstance();
    }

    public Proxy newCalendarProxy(Class type, TimeZone timeZone) {
        ProxyCalendar pc = (ProxyCalendar) findProxy(type,
            ProxyCalendar.class, _calendars);
        return pc.newInstance(timeZone);
    }

    public Proxy newCollectionProxy(Class type, Class elementType,
        Comparator compare) {
        ProxyCollection pc = (ProxyCollection) findProxy(type,
            ProxyCollection.class, _collections);
        if (!_assertType)
            elementType = null;
        return pc.newInstance(elementType, compare, _trackChanges, conf);
    }

    public Proxy newMapProxy(Class type, Class keyType, Class valueType,
        Comparator compare) {
        ProxyMap pm = (ProxyMap) findProxy(type, ProxyMap.class, _maps);
        if (!_assertType) {
            keyType = null;
            valueType = null;
        }
        return pm.newInstance(keyType, valueType, compare, _trackChanges, conf);
    }

    /**
     * Helper method to find an existing proxy for the given type.
     */
    private static Proxy findProxy(Class type, Class proxyType, Map proxies) {
        Proxy p = (Proxy) proxies.get(type);
        if (p == null) {
            // check for custom proxy
            if (proxyType.isAssignableFrom(type)) {
                try {
                    p = (Proxy) type.newInstance();
                    proxies.put(type, p);
                } catch (Exception e) {
                    throw new UnsupportedException(_loc.get("no-proxy-cons",
                        type), e);
                }
            } else throw new UnsupportedException(_loc.get("bad-proxy", type));
        }
        return p;
    }

    public Proxy newCustomProxy(Object obj) {
        return (obj instanceof Proxy) ? (Proxy) obj : null;
    }

    /**
     * Add a supported proxy collection type.
     */
    protected void setProxyTemplate(Class collType, ProxyCollection proxy) {
        _collections.put(collType, proxy);
    }

    /**
     * Add a supported proxy map type.
     */
    protected void setProxyTemplate(Class mapType, ProxyMap proxy) {
        _maps.put(mapType, proxy);
    }

    public void setConfiguration(Configuration conf) {
        this.conf = (OpenJPAConfiguration) conf;
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
    }
}
