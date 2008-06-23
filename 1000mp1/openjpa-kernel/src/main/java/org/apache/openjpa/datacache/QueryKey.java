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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.kernel.Query;
import org.apache.openjpa.kernel.QueryContext;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.ImplHelper;

/**
 * This class stores information about a particular invocation of
 * a query. It contains a reference to the external properties of the
 * query that was executed, as well as any parameters used to execute
 * that query, with one exception: first-class objects used as
 * parameter values are converted to OIDs.
 *
 * @author Patrick Linskey
 */
public class QueryKey
    implements Externalizable {

    // initialize the set of unmodifiable classes. This allows us
    // to avoid cloning collections that are not modifiable,
    // provided that they do not contain mutable objects.
    private static Collection s_unmod = new HashSet();

    static {
        // handle the set types; jdk uses different classes for collection,
        // set, and sorted set
        TreeSet s = new TreeSet();
        s_unmod.add(Collections.unmodifiableCollection(s).getClass());
        s_unmod.add(Collections.unmodifiableSet(s).getClass());
        s_unmod.add(Collections.unmodifiableSortedSet(s).getClass());

        // handle the list types; jdk uses different classes for standard
        // and random access lists
        List l = new LinkedList();
        s_unmod.add(Collections.unmodifiableList(l).getClass());
        l = new ArrayList(0);
        s_unmod.add(Collections.unmodifiableList(l).getClass());

        // handle the constant types
        s_unmod.add(Collections.EMPTY_SET.getClass());
        s_unmod.add(Collections.EMPTY_LIST.getClass());
    }

    // caching state; no need to include parameter and variable declarations
    // because they are implicit in the filter
    private String _candidateClassName;
    private boolean _subclasses;
    private Set _accessPathClassNames;
    private String _query;
    private boolean _ignoreChanges;
    private Map _params;
    private long _rangeStart;
    private long _rangeEnd;

    // ### pcl: 2 May 2003: should this timeout take into account the
    // ### timeouts for classes in the access path of the query?
    // ### Currently, it only considers the candidate class and its
    // ### subclasses. Note that this value is used to decide whether
    // ### or not OIDs should be registered for expiration callbacks
    private int _timeout = -1;

    /**
     * Return a key for the given query, or null if it is not cacheable.
     */
    public static QueryKey newInstance(Query q) {
        return newInstance(q, (Object[]) null);
    }

    /**
     * Return a key for the given query, or null if it is not cacheable.
     */
    public static QueryKey newInstance(Query q, Object[] args) {
        // compile to make sure info encoded in query string is available
        // via API calls (candidate class, result class, etc)
        q.compile();
        return newInstance(q, false, args, q.getCandidateType(),
            q.hasSubclasses(), q.getStartRange(), q.getEndRange());
    }

    /**
     * Return a key for the given query, or null if it is not cacheable.
     */
    public static QueryKey newInstance(Query q, Map args) {
        // compile to make sure info encoded in query string is available
        // via API calls (candidate class, result class, etc)
        q.compile();
        return newInstance(q, false, args, q.getCandidateType(),
            q.hasSubclasses(), q.getStartRange(), q.getEndRange());
    }

    /**
     * Return a key for the given query, or null if it is not cacheable.
     */
    static QueryKey newInstance(QueryContext q, boolean packed, Object[] args,
        Class candidate, boolean subs, long startIdx, long endIdx) {
        QueryKey key = createKey(q, packed, candidate, subs, startIdx, endIdx);
        if (key != null && setParams(key, q, args))
            return key;
        return null;
    }

    /**
     * Return a key for the given query, or null if it is not cacheable.
     */
    static QueryKey newInstance(QueryContext q, boolean packed, Map args,
        Class candidate, boolean subs, long startIdx, long endIdx) {
        QueryKey key = createKey(q, packed, candidate, subs, startIdx, endIdx);
        if (key != null && (args == null || args.isEmpty() ||
            setParams(key, q.getStoreContext(), new HashMap(args))))
            return key;
        return null;
    }

    /**
     * Extract the relevant identifying information from
     * <code>q</code>. This includes information such as candidate
     * class, query filter, etc.
     */
    private static QueryKey createKey(QueryContext q, boolean packed,
        Class candidateClass, boolean subclasses, long startIdx, long endIdx) {
        if (candidateClass == null)
            return null;

        // can only cache datastore queries
        if (q.getCandidateCollection() != null)
            return null;

        // no support already-packed results
        if (q.getResultType() != null && packed)
            return null;

        // can't cache non-serializable non-managed complex types
        Class[] types = q.getProjectionTypes();
        for (int i = 0; i < types.length; i++) {
            switch (JavaTypes.getTypeCode(types[i])) {
                case JavaTypes.ARRAY:
                    return null;
                case JavaTypes.COLLECTION:
                case JavaTypes.MAP:
                case JavaTypes.OBJECT:
                    if (!ImplHelper.isManagedType(types[i]))
                        return null;
                    break;
            }
        }

        // we can't cache the query if we don't know which classes are in the
        // access path
        ClassMetaData[] metas = q.getAccessPathMetaDatas();
        if (metas.length == 0)
            return null;

        Set accessPathClassNames = new HashSet((int) (metas.length * 1.33 + 1));
        ClassMetaData meta;
        for (int i = 0; i < metas.length; i++) {
            // since the class change framework deals with least-derived types,
            // record the least-derived access path types
            meta = metas[i];
            while (meta.getPCSuperclass() != null)
                meta = meta.getPCSuperclassMetaData();

            // ensure that this metadata is cacheable
            if (meta.getDataCache() == null)
                return null;
            accessPathClassNames.add(meta.getDescribedType().getName());
        }

        // if any of the types are currently dirty, we can't cache this query
        StoreContext ctx = q.getStoreContext();
        if (intersects(accessPathClassNames, ctx.getPersistedTypes())
            || intersects(accessPathClassNames, ctx.getUpdatedTypes())
            || intersects(accessPathClassNames, ctx.getDeletedTypes()))
            return null;

        // calculate the timeout for the key
        MetaDataRepository repos = ctx.getConfiguration().
            getMetaDataRepositoryInstance();

        // won't find metadata for interfaces.
        if (candidateClass.isInterface())
            return null;
        meta = repos.getMetaData(candidateClass, ctx.getClassLoader(), true);
        int timeout = meta.getDataCacheTimeout();
        if (subclasses) {
            metas = meta.getPCSubclassMetaDatas();
            int subTimeout;
            for (int i = 0; i < metas.length; i++) {
                if (metas[i].getDataCache() == null)
                    return null;

                subTimeout = metas[i].getDataCacheTimeout();
                if (subTimeout != -1 && subTimeout < timeout)
                    timeout = subTimeout;
            }
        }

        // tests all passed; cacheable
        QueryKey key = new QueryKey();
        key._candidateClassName = candidateClass.getName();
        key._subclasses = subclasses;
        key._accessPathClassNames = accessPathClassNames;
        key._timeout = timeout;
        key._query = q.getQueryString();
        key._ignoreChanges = q.getIgnoreChanges();
        key._rangeStart = startIdx;
        key._rangeEnd = endIdx;
        return key;
    }

    /**
     * Convert an array of arguments into the corresponding parameter
     * map, and do any PC to OID conversion necessary.
     */
    private static boolean setParams(QueryKey key, QueryContext q,
        Object[] args) {
        if (args == null || args.length == 0)
            return true;

        // Create a map for the given parameters, and convert the
        // parameter list into a map, using the query's parameter
        // declaration to determine ordering etc.
        Map types = q.getParameterTypes();
        Map map = new HashMap((int) (types.size() * 1.33 + 1));
        int idx = 0;
        for (Iterator iter = types.keySet().iterator(); iter.hasNext(); idx++)
            map.put(iter.next(), args[idx]);
        return setParams(key, q.getStoreContext(), map);
    }

    /**
     * Convert parameters to a form that is cacheable. Mutable params
     * will be cloned.
     */
    private static boolean setParams(QueryKey key, StoreContext ctx,
        Map params) {
        if (params == null || params.isEmpty())
            return true;

        Map.Entry e;
        Object v;
        for (Iterator iter = params.entrySet().iterator(); iter.hasNext();) {
            e = (Map.Entry) iter.next();
            v = e.getValue();
            if (ImplHelper.isManageable(v)) {
                if (!ctx.isPersistent(v) || ctx.isNew(v) || ctx.isDeleted(v))
                    return false;
                e.setValue(ctx.getObjectId(v));
            }

            if (v instanceof Collection) {
                Collection c = (Collection) v;
                boolean contentsAreDates = false;
                if (c.iterator().hasNext()) {
                    // this assumes that the collection is homogeneous
                    Object o = c.iterator().next();
                    if (ImplHelper.isManageable(o))
                        return false;

                    // pcl: 27 Jun 2004: if we grow this logic to
                    // handle other mutable types that are not
                    // known to be cloneable, we will have to add
                    // logic to handle them. This is because we
                    // can't just cast to Cloneable and invoke
                    // clone(), as clone() is a protected method
                    // in Object.
                    if (o instanceof Date)
                        contentsAreDates = true;

                    // if the collection is not a known immutable
                    // type, or if it contains mutable instances,
                    // clone it for good measure.
                    if (contentsAreDates || !s_unmod.contains(c.getClass())) {
                        // copy the collection
                        Collection copy;
                        if (c instanceof SortedSet)
                            copy = new TreeSet();
                        else if (c instanceof Set)
                            copy = new HashSet();
                        else
                            copy = new ArrayList(c.size());

                        if (contentsAreDates) {
                            // must go through by hand and do the
                            // copy, since Date is mutable.
                            for (Iterator itr2 = c.iterator(); itr2.hasNext();)
                                copy.add(((Date) itr2.next()).clone());
                        } else
                            copy.addAll(c);

                        e.setValue(copy);
                    }
                }
            } else if (v instanceof Date)
                e.setValue(((Date) v).clone());
        }

        key._params = params;
        return true;
    }

    /**
     * Public constructor for externalization only.
     */
    public QueryKey() {
    }

    /**
     * Returns the candidate class name for this query.
     */
    public String getCandidateTypeName() {
        return _candidateClassName;
    }

    /**
     * Return the amount of time this key is good for.
     */
    public int getTimeout() {
        return _timeout;
    }

    /**
     * Returns <code>true</code> if modifications to any of the
     * classes in <code>changed</code> results in a possible
     * invalidation of this query; otherwise returns
     * <code>false</code>. Invalidation is possible if one or more of
     * the classes in this query key's access path has been changed.
     */
    public boolean changeInvalidatesQuery(Collection changed) {
        return intersects(_accessPathClassNames, changed);
    }

    /**
     * Whether the given set of least-derived class names intersects with
     * the given set of changed classes.
     */
    private static boolean intersects(Collection names, Collection changed) {
        Class cls;
        Class sup;
        for (Iterator iter = changed.iterator(); iter.hasNext();) {
            cls = (Class) iter.next();
            while ((sup = PCRegistry.getPersistentSuperclass(cls)) != null)
                cls = sup;
            if (names.contains(cls.getName()))
                return true;
        }
        return false;
    }

    /**
     * Determine equality based on identifying information. Keys
     * created for queries that specify a candidate collection are
     * always not equal.
     */
    public boolean equals(Object ob) {
        if (this == ob)
            return true;
        if (ob == null || getClass() != ob.getClass())
            return false;

        QueryKey other = (QueryKey) ob;
        return StringUtils.equals(_candidateClassName,
            other._candidateClassName)
            && _subclasses == other._subclasses
            && _ignoreChanges == other._ignoreChanges
            && _rangeStart == other._rangeStart
            && _rangeEnd == other._rangeEnd
            && StringUtils.equals(_query, other._query)
            && ObjectUtils.equals(_params, other._params);
    }

    /**
     * Define a hashing algorithm corresponding to the {@link #equals}
     * method defined above.
     */
    public int hashCode() {
        int code = 37 * 17 + _candidateClassName.hashCode();
        if (_query != null)
            code = 37 * code + _query.hashCode();
        if (_params != null)
            code = 37 * code + _params.hashCode();
        return code;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(255);
        buf.append(super.toString()).
            append("[query:[").append(_query).append("]").
            append(",access path:").append(_accessPathClassNames).
            append(",subs:").append(_subclasses).
            append(",ignoreChanges:").append(_ignoreChanges).
            append(",startRange:").append(_rangeStart).
            append(",endRange:").append(_rangeEnd).
            append(",timeout:").append(_timeout).
            append("]");
        return buf.toString();
    }

    // ---------- Externalizable implementation ----------

    public void writeExternal(ObjectOutput out)
        throws IOException {
        out.writeObject(_candidateClassName);
        out.writeBoolean(_subclasses);
        out.writeObject(_accessPathClassNames);
        out.writeObject(_query);
        out.writeBoolean(_ignoreChanges);
        out.writeObject(_params);
        out.writeLong(_rangeStart);
        out.writeLong(_rangeEnd);
        out.writeInt(_timeout);
    }

    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {
        _candidateClassName = (String) in.readObject();
        _subclasses = in.readBoolean();
        _accessPathClassNames = (Set) in.readObject();
        _query = (String) in.readObject();
        _ignoreChanges = in.readBoolean();
        _params = (Map) in.readObject();
        _rangeStart = in.readLong();
        _rangeEnd = in.readLong ();
		_timeout = in.readInt ();
	}
}
