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
package org.apache.openjpa.util;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.lib.util.ClassUtil;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.openjpa.util.asm.ClassWriterTracker;
import org.apache.openjpa.util.proxy.DelayedArrayListProxy;
import org.apache.openjpa.util.proxy.DelayedHashSetProxy;
import org.apache.openjpa.util.proxy.DelayedLinkedHashSetProxy;
import org.apache.openjpa.util.proxy.DelayedLinkedListProxy;
import org.apache.openjpa.util.proxy.DelayedPriorityQueueProxy;
import org.apache.openjpa.util.proxy.DelayedTreeSetProxy;
import org.apache.openjpa.util.proxy.DelayedVectorProxy;
import org.apache.openjpa.util.proxy.ProxyBean;
import org.apache.openjpa.util.proxy.ProxyCalendar;
import org.apache.openjpa.util.proxy.ProxyCollection;
import org.apache.openjpa.util.proxy.ProxyCollections;
import org.apache.openjpa.util.proxy.ProxyDate;
import org.apache.openjpa.util.proxy.ProxyMap;
import org.apache.openjpa.util.proxy.ProxyMaps;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.Label;
import org.apache.xbean.asm9.MethodVisitor;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;


/**
 * Default implementation of the {@link ProxyManager} interface.
 *
 * @author Abe White
 * @author Mark Struberg
 */
public class ProxyManagerImpl
    implements ProxyManager {

    private static final String PROXY_SUFFIX = "$proxy";

    private static final Localizer _loc = Localizer.forPackage
        (ProxyManagerImpl.class);
    public static final Type TYPE_OBJECT = Type.getType(Object.class);

    private static long _proxyId = 0L;
    private static final Map _stdCollections = new HashMap();
    private static final Map _stdMaps = new HashMap();
    static {
        _stdCollections.put(Collection.class, ArrayList.class);
        _stdCollections.put(Set.class, HashSet.class);
        _stdCollections.put(SortedSet.class, TreeSet.class);
        _stdCollections.put(List.class, ArrayList.class);
        _stdCollections.put(Queue.class, LinkedList.class);
        _stdMaps.put(Map.class, HashMap.class);
        _stdMaps.put(SortedMap.class, TreeMap.class);
    }

    private final Set<String> _unproxyable = new HashSet<>();
    private final Map<Class<?>, Proxy> _proxies = new ConcurrentHashMap<>();
    private boolean _trackChanges = true;
    private boolean _assertType = false;
    private boolean _delayedCollectionLoading = false;

    public ProxyManagerImpl() {
        _unproxyable.add(TimeZone.class.getName());
    }

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

    /**
     * Whether loading of collections should be delayed until an operation
     * is performed that requires them to be loaded.  This property only
     * applies to proxies that implement java.util.Collection (ie. not arrays
     * or maps).  Defaults to false.
     */
    @Override
    public boolean getDelayCollectionLoading() {
        return _delayedCollectionLoading;
    }

    /**
     * Whether loading of collections should be delayed until an operation
     * is performed that requires them to be loaded.  Defaults to false.
     */
    public void setDelayCollectionLoading(boolean delay) {
        _delayedCollectionLoading = delay;
    }

    /**
     * Return a mutable view of class names we know cannot be proxied
     * correctly by this manager.
     */
    public Collection getUnproxyable() {
        return _unproxyable;
    }

    /**
     * Provided for auto-configuration.  Add the given semicolon-separated
     * class names to the set of class names we know cannot be proxied correctly
     * by this manager.
     */
    public void setUnproxyable(String clsNames) {
        if (clsNames != null)
            _unproxyable.addAll(Arrays.asList(StringUtil.split(clsNames, ";", 0)));
    }

    @Override
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

    @Override
    public Collection copyCollection(Collection orig) {
        if (orig == null)
            return null;
        if (orig instanceof Proxy)
            return (Collection) ((Proxy) orig).copy(orig);

        ProxyCollection proxy = getFactoryProxyCollection(orig.getClass());
        return (Collection) proxy.copy(orig);
    }

    @Override
    public Proxy newCollectionProxy(Class type, Class elementType,
        Comparator compare, boolean autoOff) {
        type = toProxyableCollectionType(type);
        ProxyCollection proxy = getFactoryProxyCollection(type);
        return proxy.newInstance((_assertType) ? elementType : null, compare,
            _trackChanges, autoOff);
    }

    @Override
    public Map copyMap(Map orig) {
        if (orig == null)
            return null;
        if (orig instanceof Proxy)
            return (Map) ((Proxy) orig).copy(orig);

        ProxyMap proxy = getFactoryProxyMap(orig.getClass());
        return (Map) proxy.copy(orig);
    }

    @Override
    public Proxy newMapProxy(Class type, Class keyType,
        Class elementType, Comparator compare,boolean autoOff) {
        type = toProxyableMapType(type);
        ProxyMap proxy = getFactoryProxyMap(type);
        return proxy.newInstance((_assertType) ? keyType : null,
            (_assertType) ? elementType : null, compare, _trackChanges, autoOff);
    }

    @Override
    public Date copyDate(Date orig) {
        if (orig == null)
            return null;
        if (orig instanceof Proxy)
            return (Date) ((Proxy) orig).copy(orig);

        ProxyDate proxy = getFactoryProxyDate(orig.getClass());
        return (Date) proxy.copy(orig);
    }

    @Override
    public Proxy newDateProxy(Class type) {
        ProxyDate proxy = getFactoryProxyDate(type);
        return proxy.newInstance();
    }

    @Override
    public Calendar copyCalendar(Calendar orig) {
        if (orig == null)
            return null;
        if (orig instanceof Proxy)
            return (Calendar) ((Proxy) orig).copy(orig);

        ProxyCalendar proxy = getFactoryProxyCalendar(orig.getClass());
        return (Calendar) proxy.copy(orig);
    }

    @Override
    public Proxy newCalendarProxy(Class type, TimeZone zone) {
        if (type == Calendar.class)
            type = GregorianCalendar.class;
        ProxyCalendar proxy = getFactoryProxyCalendar(type);
        ProxyCalendar cal = proxy.newInstance();
        if (zone != null)
            ((Calendar) cal).setTimeZone(zone);
        return cal;
    }

    @Override
    public Object copyCustom(Object orig) {
        if (orig == null)
            return null;
        if (orig instanceof Proxy)
            return ((Proxy) orig).copy(orig);
        if (ImplHelper.isManageable(orig))
            return null;
        if (orig instanceof Collection)
            return copyCollection((Collection) orig);
        if (orig instanceof Map)
            return copyMap((Map) orig);
        if (orig instanceof Date)
            return copyDate((Date) orig);
        if (orig instanceof Calendar)
            return copyCalendar((Calendar) orig);
        ProxyBean proxy = getFactoryProxyBean(orig);
        return (proxy == null) ? null : proxy.copy(orig);
    }

    @Override
    public Proxy newCustomProxy(Object orig, boolean autoOff) {
        if (orig == null)
            return null;
        if (orig instanceof Proxy)
            return (Proxy) orig;
        if (ImplHelper.isManageable(orig))
            return null;
        if (!isProxyable(orig.getClass()))
            return null;

        if (orig instanceof Collection) {
            Comparator comp = (orig instanceof SortedSet)
                ? ((SortedSet) orig).comparator() : null;
            Collection c = (Collection) newCollectionProxy(orig.getClass(),
                null, comp, autoOff);
            c.addAll((Collection) orig);
            return (Proxy) c;
        }
        if (orig instanceof Map) {
            Comparator comp = (orig instanceof SortedMap)
                ? ((SortedMap) orig).comparator() : null;
            Map m = (Map) newMapProxy(orig.getClass(), null, null, comp, autoOff);
            m.putAll((Map) orig);
            return (Proxy) m;
        }
        if (orig instanceof Date) {
            Date d = (Date) newDateProxy(orig.getClass());
            d.setTime(((Date) orig).getTime());
            if (orig instanceof Timestamp)
                ((Timestamp) d).setNanos(((Timestamp) orig).getNanos());
            return (Proxy) d;
        }
        if (orig instanceof Calendar) {
            Calendar c = (Calendar) newCalendarProxy(orig.getClass(),
                ((Calendar) orig).getTimeZone());
            c.setTimeInMillis(((Calendar) orig).getTimeInMillis());
            return (Proxy) c;
        }

        ProxyBean proxy = getFactoryProxyBean(orig);
        return (proxy == null) ? null : proxy.newInstance(orig);
    }

    /**
     * Return the concrete type for proxying.
     */
    protected Class toProxyableCollectionType(Class type) {
        if (type.getName().endsWith(PROXY_SUFFIX))
            type = type.getSuperclass();
        else if (type.isInterface()) {
            type = toConcreteType(type, _stdCollections);
            if (type == null)
                throw new UnsupportedException(_loc.get("no-proxy-intf", type));
        } else if (Modifier.isAbstract(type.getModifiers()))
            throw new UnsupportedException(_loc.get("no-proxy-abstract", type));
        return type;
    }

    /**
     * Return the concrete type for proxying.
     */
    protected Class toProxyableMapType(Class type) {
        if (type.getName().endsWith(PROXY_SUFFIX))
            type = type.getSuperclass();
        else if (type.isInterface()) {
            type = toConcreteType(type, _stdMaps);
            if (type == null)
                throw new UnsupportedException(_loc.get("no-proxy-intf", type));
        } else if (Modifier.isAbstract(type.getModifiers()))
            throw new UnsupportedException(_loc.get("no-proxy-abstract", type));
        return type;
    }

    /**
     * Locate a concrete type to proxy for the given collection interface.
     */
    private static Class toConcreteType(Class intf, Map concretes) {
        Class concrete = (Class) concretes.get(intf);
        if (concrete != null)
            return concrete;
        Class[] intfs = intf.getInterfaces();
        for (Class aClass : intfs) {
            concrete = toConcreteType(aClass, concretes);
            if (concrete != null)
                return concrete;
        }
        return null;
    }

    /**
     * Return the cached factory proxy for the given map type.
     */
    private ProxyMap getFactoryProxyMap(Class type) {
        // we don't lock here; ok if two proxies get generated for same type
        ProxyMap proxy = (ProxyMap) _proxies.get(type);
        if (proxy == null) {
            ClassLoader l = GeneratedClasses.getMostDerivedLoader(type,
                ProxyMap.class);
            Class pcls = loadBuildTimeProxy(type, l);
            if (pcls == null)
                pcls = generateAndLoadProxyMap(type, true, l);
            proxy = (ProxyMap) instantiateProxy(pcls, null, null);
            _proxies.put(type, proxy);
        }
        return proxy;
    }

    /**
     * Return the cached factory proxy for the given date type.
     */
    private ProxyDate getFactoryProxyDate(Class type) {
        // we don't lock here; ok if two proxies get generated for same type
        ProxyDate proxy = (ProxyDate) _proxies.get(type);
        if (proxy == null) {
            ClassLoader l = GeneratedClasses.getMostDerivedLoader(type,
                ProxyDate.class);
            Class pcls = loadBuildTimeProxy(type, l);
            if (pcls == null)
                pcls = generateAndLoadProxyDate(type, true, l);
            proxy = (ProxyDate) instantiateProxy(pcls, null, null);
            _proxies.put(type, proxy);
        }
        return proxy;
    }

    /**
     * Return the cached factory proxy for the given calendar type.
     */
    private ProxyCalendar getFactoryProxyCalendar(Class type) {
        // we don't lock here; ok if two proxies get generated for same type
        ProxyCalendar proxy = (ProxyCalendar) _proxies.get(type);
        if (proxy == null) {
            ClassLoader l = GeneratedClasses.getMostDerivedLoader(type,
                ProxyCalendar.class);
            Class pcls = loadBuildTimeProxy(type, l);
            if (pcls == null)
                pcls = generateAndLoadProxyCalendar(type, true, l);
            proxy = (ProxyCalendar) instantiateProxy(pcls, null, null);
            _proxies.put(type, proxy);
        }
        return proxy;
    }

    /**
     * Return the cached factory proxy for the given collection type.
     */
    private ProxyCollection getFactoryProxyCollection(Class type) {
        // we don't lock here; ok if two proxies get generated for same type
        ProxyCollection proxy = (ProxyCollection) _proxies.get(type);
        if (proxy == null) {
            ClassLoader l = GeneratedClasses.getMostDerivedLoader(type,
                    ProxyCollection.class);
            Class pcls = loadBuildTimeProxy(type, l);
            if (pcls == null)
                pcls = generateAndLoadProxyCollection(type, true, l);
            proxy = (ProxyCollection) instantiateProxy(pcls, null, null);
            _proxies.put(type, proxy);
        }
        return proxy;
    }


    /**
     * Return the cached factory proxy for the given bean type.
     */
    private ProxyBean getFactoryProxyBean(Object orig) {
        final Class<?> type = orig.getClass();
        if (isUnproxyable(type))
            return null;

        // we don't lock here; ok if two proxies get generated for same type
        ProxyBean proxy = (ProxyBean) _proxies.get(type);
        if (proxy == null) {
            ClassLoader l = GeneratedClasses.getMostDerivedLoader(type, ProxyBean.class);
            Class<?> pcls = loadBuildTimeProxy(type, l);
            if (pcls == null) {
                pcls = generateAndLoadProxyBean(type, true, l);
            }
            if (pcls != null)
                proxy = (ProxyBean) instantiateProxy(pcls, findCopyConstructor(type), new Object[] { orig });
            if (proxy == null) {
                _unproxyable.add(type.getName());
            } else {
                _proxies.put(type, proxy);
            }
        }
        return proxy;
    }

    /**
     * Return whether the given type is known to be unproxyable.
     */
    protected boolean isUnproxyable(Class type) {
        for (; type != null && type != Object.class;
            type = type.getSuperclass()) {
            if (_unproxyable.contains(type.getName()))
                return true;
        }
        return false;
    }

    /**
     * Load the proxy class generated at build time for the given type,
     * returning null if none exists.
     */
    protected Class loadBuildTimeProxy(Class type, ClassLoader loader) {
        try {
            Class<?> proxyClass = null;
            if (_delayedCollectionLoading) {
                proxyClass = loadDelayedProxy(type);
                if (proxyClass != null) {
                    return proxyClass;
                }
            }
            return Class.forName(getProxyClassName(type, false), true, loader);
        } catch (Throwable t) {
            return null;
        }
    }

    protected Class<?> loadDelayedProxy(Class<?> type) {
        if (type.equals(java.util.ArrayList.class)) {
            return DelayedArrayListProxy.class;
        }
        if (type.equals(java.util.HashSet.class)) {
            return DelayedHashSetProxy.class;
        }
        if (type.equals(java.util.LinkedList.class)) {
            return DelayedLinkedListProxy.class;
        }
        if (type.equals(java.util.Vector.class)) {
            return DelayedVectorProxy.class;
        }
        if (type.equals(java.util.LinkedHashSet.class)) {
            return DelayedLinkedHashSetProxy.class;
        }
        if (type.equals(java.util.SortedSet.class) || type.equals(java.util.TreeSet.class)) {
            return DelayedTreeSetProxy.class;
        }
        if (type.equals(java.util.PriorityQueue.class)) {
            return DelayedPriorityQueueProxy.class;
        }
        return null;
    }

    /**
     * Instantiate the given proxy class.
     */
    private Proxy instantiateProxy(Class cls, Constructor cons, Object[] args) {
        try {
            if (cons != null)
                return (Proxy) cls.getConstructor(cons.getParameterTypes()).
                    newInstance(args);
            return (Proxy) J2DoPrivHelper.newInstance(cls);
        } catch (InstantiationException ie) {
            throw new UnsupportedException(_loc.get("cant-newinstance",
                cls.getSuperclass().getName()));
        } catch (Throwable t) {
            throw new GeneralException(cls.getName()).setCause(t);
        }
    }

    /**
     * Return the name of the proxy class to generate for the given type.
     */
    protected static String getProxyClassName(Class type, boolean runtime) {
        String id = (runtime) ? "$" + nextProxyId() : "";
        return ClassUtil.getPackageName(ProxyManagerImpl.class) + "."
            + type.getName().replace('.', '$') + id + PROXY_SUFFIX;
    }

    /**
     * Throw appropriate exception if the given type is final.
     */
    private static void assertNotFinal(Class type) {
        if (Modifier.isFinal(type.getModifiers()))
            throw new UnsupportedException(_loc.get("no-proxy-final", type));
    }

    private static boolean isProxyable(Class<?> cls){
        int mod = cls.getModifiers();
        if(Modifier.isFinal(mod)) {
            return false;
        }

        if(Modifier.isProtected(mod) || Modifier.isPublic(mod)) {
            return true;
        }

        // Default scoped class, we can only extend if it is in the same package as the generated proxy. Ideally
        // we'd fix the code gen portion and place proxies in the same pacakge as the types being proxied.
        if(cls.getPackage().getName().equals("org.apache.openjpa.util")) {
            return true;
        }

        return false;

    }


    private Class generateAndLoadProxyDate(Class type, boolean runtime, ClassLoader l) {
        final String proxyClassName = getProxyClassName(type, runtime);
        final byte[] classBytes = generateProxyDateBytecode(type, runtime, proxyClassName);
        if (classBytes == null) {
            return null;
        }

        return GeneratedClasses.loadAsmClass(proxyClassName, classBytes, ProxyDate.class, l);
    }

    private Class generateAndLoadProxyCalendar(Class type, boolean runtime, ClassLoader l) {
        final String proxyClassName = getProxyClassName(type, runtime);
        final byte[] classBytes = generateProxyCalendarBytecode(type, runtime, proxyClassName);
        if (classBytes == null) {
            return null;
        }

        return GeneratedClasses.loadAsmClass(proxyClassName, classBytes, ProxyDate.class, l);
    }

    private Class generateAndLoadProxyCollection(Class type, boolean runtime, ClassLoader l) {
        final String proxyClassName = getProxyClassName(type, runtime);
        final byte[] classBytes = generateProxyCollectionBytecode(type, runtime, proxyClassName);
        if (classBytes == null) {
            return null;
        }

        return GeneratedClasses.loadAsmClass(proxyClassName, classBytes, ProxyCollection.class, l);
    }

    private Class generateAndLoadProxyMap(Class type, boolean runtime, ClassLoader l) {
        final String proxyClassName = getProxyClassName(type, runtime);
        final byte[] classBytes = generateProxyMapBytecode(type, runtime, proxyClassName);
        if (classBytes == null) {
            return null;
        }

        return GeneratedClasses.loadAsmClass(proxyClassName, classBytes, ProxyMap.class, l);
    }

    private Class generateAndLoadProxyBean(Class type, boolean runtime, ClassLoader l) {
        final String proxyClassName = getProxyClassName(type, runtime);
        final byte[] classBytes = generateProxyBeanBytecode(type, runtime, proxyClassName);
        if (classBytes == null) {
            return null;
        }

        return GeneratedClasses.loadAsmClass(proxyClassName, classBytes, ProxyBean.class, l);
    }

    /**
     * Generate the bytecode for a date proxy for the given type.
     */
    protected byte[] generateProxyDateBytecode(Class type, boolean runtime, String proxyClassName) {
        assertNotFinal(type);
        String proxyClassDef = proxyClassName.replace('.', '/');
        String superClassFileNname = Type.getInternalName(type);
        String[] interfaceNames = new String[]{Type.getInternalName(ProxyDate.class)};

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, proxyClassDef,
                null, superClassFileNname, interfaceNames);

        ClassWriterTracker ct = new ClassWriterTracker(cw);
        String classFileName = runtime ? type.getName() : proxyClassDef;
        cw.visitSource(classFileName + ".java", null);

        delegateConstructors(ct, type, superClassFileNname);
        addInstanceVariables(ct);
        addProxyMethods(ct, true, proxyClassDef, type);
        addProxyDateMethods(ct, proxyClassDef, type);
        proxySetters(ct, proxyClassDef, type);
        addWriteReplaceMethod(ct, proxyClassDef, runtime);

        return cw.toByteArray();
    }

    /**
     * Generate the bytecode for a calendar proxy for the given type.
     */
    protected byte[] generateProxyCalendarBytecode(Class type, boolean runtime, String proxyClassName) {
        assertNotFinal(type);
        String proxyClassDef = proxyClassName.replace('.', '/');
        String superClassFileNname = Type.getInternalName(type);
        String[] interfaceNames = new String[]{Type.getInternalName(ProxyCalendar.class)};

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, proxyClassDef,
                null, superClassFileNname, interfaceNames);

        ClassWriterTracker ct = new ClassWriterTracker(cw);
        String classFileName = runtime ? type.getName() : proxyClassDef;
        cw.visitSource(classFileName + ".java", null);

        delegateConstructors(ct, type, superClassFileNname);
        addInstanceVariables(ct);
        addProxyMethods(ct, true, proxyClassDef, type);
        addProxyCalendarMethods(ct, proxyClassDef, type);
        proxySetters(ct, proxyClassDef, type);
        addWriteReplaceMethod(ct, proxyClassDef, runtime);

        return cw.toByteArray();
    }

    /**
     * Generate the bytecode for a collection proxy for the given type.
     */
    protected byte[] generateProxyCollectionBytecode(Class type, boolean runtime, String proxyClassName) {
        assertNotFinal(type);
        String proxyClassDef = proxyClassName.replace('.', '/');
        String superClassFileNname = Type.getInternalName(type);
        String[] interfaceNames = new String[]{Type.getInternalName(ProxyCollection.class)};

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, proxyClassDef,
                null, superClassFileNname, interfaceNames);

        ClassWriterTracker ct = new ClassWriterTracker(cw);
        String classFileName = runtime ? type.getName() : proxyClassDef;
        cw.visitSource(classFileName + ".java", null);

        delegateConstructors(ct, type, superClassFileNname);
        addInstanceVariables(ct);
        addProxyMethods(ct, false, proxyClassDef, type);
        addProxyCollectionMethods(ct, proxyClassDef, type);
        proxyRecognizedMethods(ct, proxyClassDef, type, ProxyCollections.class, ProxyCollection.class);
        proxySetters(ct, proxyClassDef, type);
        addWriteReplaceMethod(ct, proxyClassDef, runtime);

        return cw.toByteArray();
    }

    /**
     * Generate the bytecode for a map proxy for the given type.
     */
    protected byte[] generateProxyMapBytecode(Class type, boolean runtime, String proxyClassName) {
        assertNotFinal(type);
        String proxyClassDef = proxyClassName.replace('.', '/');
        String superClassFileNname = Type.getInternalName(type);
        String[] interfaceNames = new String[]{Type.getInternalName(ProxyMap.class)};

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, proxyClassDef,
                null, superClassFileNname, interfaceNames);

        ClassWriterTracker ct = new ClassWriterTracker(cw);
        String classFileName = runtime ? type.getName() : proxyClassDef;
        cw.visitSource(classFileName + ".java", null);

        delegateConstructors(ct, type, superClassFileNname);
        addInstanceVariables(ct);
        addProxyMethods(ct, false, proxyClassDef, type);
        addProxyMapMethods(ct, proxyClassDef, type);
        proxyRecognizedMethods(ct, proxyClassDef, type, ProxyMaps.class, ProxyMap.class);
        proxySetters(ct, proxyClassDef, type);
        addWriteReplaceMethod(ct, proxyClassDef, runtime);

        return cw.toByteArray();
    }

    /**
     * Generate the bytecode for a bean proxy for the given type.
     */
    protected byte[] generateProxyBeanBytecode(Class type, boolean runtime, String proxyClassName) {
        if (Modifier.isFinal(type.getModifiers())) {
            return null;
        }
        if (ImplHelper.isManagedType(null, type)) {
            return null;
        }

        // we can only generate a valid proxy if there is a copy constructor
        // or a default constructor
        Constructor cons = findCopyConstructor(type);
        if (cons == null) {
            Constructor[] cs = type.getConstructors();
            for (int i = 0; cons == null && i < cs.length; i++) {
                if (cs[i].getParameterTypes().length == 0) {
                    cons = cs[i];
                }
            }
            if (cons == null)
                return null;
        }

        String proxyClassDef = proxyClassName.replace('.', '/');
        String superClassFileNname = Type.getInternalName(type);
        String[] interfaceNames = new String[]{Type.getInternalName(ProxyBean.class)};

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, proxyClassDef,
                null, superClassFileNname, interfaceNames);

        ClassWriterTracker ct = new ClassWriterTracker(cw);
        String classFileName = runtime ? type.getName() : proxyClassDef;
        cw.visitSource(classFileName + ".java", null);

        delegateConstructors(ct, type, superClassFileNname);
        addInstanceVariables(ct);
        addProxyMethods(ct, true, proxyClassDef, type);
        addProxyBeanMethods(ct, proxyClassDef, type, cons);
        if (!proxySetters(ct, proxyClassDef, type)) {
            return null;
        }
        addWriteReplaceMethod(ct, proxyClassDef, runtime);

        return cw.toByteArray();
    }

    private void addProxyBeanMethods(ClassWriterTracker ct, String proxyClassDef, Class type, Constructor cons) {
        // bean copy
        {
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "copy",
                    Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT)
                    , null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(type));
            mv.visitInsn(Opcodes.DUP);

            Class[] params = cons.getParameterTypes();
            if (params.length == 1) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(params[0]));
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.getParamTypes(params)), false);
            int beanVarPos = params.length+2; // params+DUP

            if (params.length == 0) {
                mv.visitVarInsn(Opcodes.ASTORE, beanVarPos);
                copyBeanProperties(mv, type, beanVarPos);
                mv.visitVarInsn(Opcodes.ALOAD, beanVarPos);
            }

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // new instance factory
        {
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "newInstance",
                    Type.getMethodDescriptor(Type.getType(ProxyBean.class), Type.getType(Object.class))
                    , null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, proxyClassDef);
            mv.visitInsn(Opcodes.DUP);


            Class[] params = cons.getParameterTypes();
            if (params.length == 1) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(params[0]));
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, proxyClassDef, "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.getParamTypes(params)), false);
            int beanVarPos = params.length+2; // params+DUP

            if (params.length == 0) {
                mv.visitVarInsn(Opcodes.ASTORE, beanVarPos);
                copyBeanProperties(mv, type, beanVarPos);
                mv.visitVarInsn(Opcodes.ALOAD, beanVarPos);
            }

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();

        }
    }

    private void copyBeanProperties(MethodVisitor mv, Class type, int copyVarPos) {
        Method[] meths = type.getMethods();
        Method getter;
        int mods;
        for (Method meth : meths) {
            mods = meth.getModifiers();
            if (!Modifier.isPublic(mods) || Modifier.isStatic(mods)) {
                continue;
            }

            if (!startsWith(meth.getName(), "set") || meth.getParameterTypes().length != 1) {
                continue;
            }

            getter = findGetter(type, meth);
            if (getter == null) {
                continue;
            }

            // copy.setXXX(orig.getXXX());
            mv.visitVarInsn(Opcodes.ALOAD, copyVarPos);
            mv.visitVarInsn(Opcodes.ALOAD, copyVarPos-1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), getter.getName(),
                    Type.getMethodDescriptor(getter), false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), meth.getName(),
                    Type.getMethodDescriptor(meth), false);
        }
    }

    private void addProxyCollectionMethods(ClassWriterTracker ct, String proxyClassDef, Class type) {
        // change tracker
        {
            ct.getCw().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                    "changeTracker", Type.getDescriptor(CollectionChangeTracker.class), null, null).visitEnd();
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "getChangeTracker",
                    Type.getMethodDescriptor(Type.getType(ChangeTracker.class))
                    , null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassDef, "changeTracker", Type.getDescriptor(CollectionChangeTracker.class));

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // collection copy
        {
            Constructor cons = findCopyConstructor(type);
            if (cons == null && SortedSet.class.isAssignableFrom(type)) {
                cons = findComparatorConstructor(type);
            }
            Class[] params = (cons == null) ? new Class[0]
                    : cons.getParameterTypes();

            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "copy",
                    Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT)
                    , null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(type));
            mv.visitInsn(Opcodes.DUP);

            if (params.length == 1) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                if (params[0] == Comparator.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(SortedSet.class));
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SortedSet.class), "comparator",
                            Type.getMethodDescriptor(Type.getType(Comparator.class)), true);
                }
                else {
                    // otherwise just pass the parameter
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(params[0]));
                }
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.getParamTypes(params)), false);

            if (params.length == 0 || params[0] == Comparator.class) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Collection.class));
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), "addAll",
                        Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Collection.class)), false);
                mv.visitInsn(Opcodes.POP);
            }

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // element type
        {
            ct.getCw().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                    "elementType", Type.getDescriptor(Class.class), null, null).visitEnd();

            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "getElementType",
                    Type.getMethodDescriptor(Type.getType(Class.class))
                    , null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassDef, "elementType", Type.getDescriptor(Class.class));

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();

        }

        // new instance factory
        {
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "newInstance",
                    Type.getMethodDescriptor(Type.getType(ProxyCollection.class),
                            Type.getType(Class.class), Type.getType(Comparator.class), Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE)
                    , null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, proxyClassDef);
            mv.visitInsn(Opcodes.DUP);

            Constructor cons = findComparatorConstructor(type);
            Class[] params = (cons == null) ? new Class[0] : cons.getParameterTypes();
            if (params.length == 1) {
                mv.visitVarInsn(Opcodes.ALOAD, 2);
            }

            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, proxyClassDef, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.getParamTypes(params)), false);

            mv.visitVarInsn(Opcodes.ASTORE, 5);
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassDef, "elementType", Type.getDescriptor(Class.class));

            mv.visitVarInsn(Opcodes.ILOAD, 3);
            Label lNotTrack = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, lNotTrack);
            mv.visitVarInsn(Opcodes.ALOAD, 5);
            mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(CollectionChangeTrackerImpl.class));

            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 5);

            mv.visitInsn(allowsDuplicates(type) ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
            mv.visitInsn(isOrdered(type) ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
            mv.visitVarInsn(Opcodes.ILOAD, 4);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(CollectionChangeTrackerImpl.class), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Collection.class),
                                             Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE),
                    false);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassDef, "changeTracker", Type.getDescriptor(CollectionChangeTracker.class));

            mv.visitLabel(lNotTrack);
            mv.visitVarInsn(Opcodes.ALOAD, 5);

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
    }


    private void addProxyMapMethods(ClassWriterTracker ct, String proxyClassDef, Class type) {
        // change tracker
        {
            ct.getCw().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                    "changeTracker", Type.getDescriptor(MapChangeTracker.class), null, null).visitEnd();
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "getChangeTracker",
                    Type.getMethodDescriptor(Type.getType(ChangeTracker.class))
                    , null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassDef, "changeTracker", Type.getDescriptor(MapChangeTracker.class));

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // Map copy
        {
            Constructor cons = findCopyConstructor(type);
            if (cons == null && SortedMap.class.isAssignableFrom(type)) {
                cons = findComparatorConstructor(type);
            }
            Class[] params = (cons == null) ? new Class[0]
                    : cons.getParameterTypes();

            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "copy",
                    Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT)
                    , null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(type));
            mv.visitInsn(Opcodes.DUP);

            if (params.length == 1) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                if (params[0] == Comparator.class) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(SortedMap.class));
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SortedMap.class), "comparator",
                            Type.getMethodDescriptor(Type.getType(Comparator.class)), true);
                }
                else {
                    // otherwise just pass the parameter
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(params[0]));
                }
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.getParamTypes(params)), false);

            if (params.length == 0 || params[0] == Comparator.class) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Map.class));
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), "putAll",
                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Map.class)), false);
            }

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // key type
        {
            ct.getCw().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                    "keyType", Type.getDescriptor(Class.class), null, null).visitEnd();

            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "getKeyType",
                    Type.getMethodDescriptor(Type.getType(Class.class))
                    , null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassDef, "keyType", Type.getDescriptor(Class.class));

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // value type
        {
            ct.getCw().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                    "valueType", Type.getDescriptor(Class.class), null, null).visitEnd();

            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "getValueType",
                    Type.getMethodDescriptor(Type.getType(Class.class))
                    , null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassDef, "valueType", Type.getDescriptor(Class.class));

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // new instance factory
        {
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "newInstance",
                    Type.getMethodDescriptor(Type.getType(ProxyMap.class),
                            Type.getType(Class.class), Type.getType(Class.class), Type.getType(Comparator.class),
                            Type.BOOLEAN_TYPE, Type.BOOLEAN_TYPE)
                    , null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, proxyClassDef);
            mv.visitInsn(Opcodes.DUP);

            Constructor cons = findComparatorConstructor(type);
            Class[] params = (cons == null) ? new Class[0] : cons.getParameterTypes();
            if (params.length == 1) {
                mv.visitVarInsn(Opcodes.ALOAD, 3);
            }

            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, proxyClassDef, "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.getParamTypes(params)), false);

            mv.visitVarInsn(Opcodes.ASTORE, 6);
            mv.visitVarInsn(Opcodes.ALOAD, 6);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassDef, "keyType", Type.getDescriptor(Class.class));

            mv.visitVarInsn(Opcodes.ALOAD, 6);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassDef, "valueType", Type.getDescriptor(Class.class));

            mv.visitVarInsn(Opcodes.ILOAD, 4);
            Label lNotTrack = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, lNotTrack);
            mv.visitVarInsn(Opcodes.ALOAD, 6);
            mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(MapChangeTrackerImpl.class));

            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 6);

            mv.visitVarInsn(Opcodes.ILOAD, 5);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(MapChangeTrackerImpl.class), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Map.class), Type.BOOLEAN_TYPE),
                    false);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassDef, "changeTracker", Type.getDescriptor(MapChangeTracker.class));

            mv.visitLabel(lNotTrack);
            mv.visitVarInsn(Opcodes.ALOAD, 6);

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
    }


    private void proxyRecognizedMethods(ClassWriterTracker ct, String proxyClassDef, Class<?> type,
                                        Class<?> helper, Class<?> proxyType) {
        Method[] meths = type.getMethods();

        for (Method meth : meths) {
            // Java 8 methods with a return type of KeySetView do not need to be proxied
            if (meth.getReturnType().getName().contains("KeySetView")) {
                continue;
            }

            Class[] helperParams = toHelperParameters(meth.getParameterTypes(), proxyType);

            // first check for overriding method
            try {
                Method match;
                match = helper.getMethod(meth.getName(), helperParams);
                proxyOverrideMethod(ct, meth, match, helperParams);
                continue;
            }
            catch (NoSuchMethodException nsme) {
                // all fine
            }
            catch (Exception e) {
                throw new GeneralException(e);
            }

            // check for before and after methods, either of which may not
            // exist
            Method before = null;
            try {
                before = helper.getMethod("before" + StringUtil.capitalize(meth.getName()), helperParams);
            }
            catch (NoSuchMethodException nsme) {
                // all fine
            }
            catch (Exception e) {
                throw new GeneralException(e);
            }
            Method after = null;
            Class[] afterParams = null;

            try {
                afterParams = toHelperAfterParameters(helperParams,
                        meth.getReturnType(), (before == null)
                                ? void.class : before.getReturnType());
                after = helper.getMethod("after"
                        + StringUtil.capitalize(meth.getName()), afterParams);
            }
            catch (NoSuchMethodException nsme) {
            }
            catch (Exception e) {
                throw new GeneralException(e);
            }
            if (before != null || after != null)
                proxyBeforeAfterMethod(ct, type, meth, helperParams, before, after, afterParams);
        }
    }

    /**
     * Proxy the given method with one that overrides it by calling into the
     * given helper.
     */
    private void proxyOverrideMethod(ClassWriterTracker ct, Method meth, Method helper, Class[] helperParams) {
        MethodVisitor mv = ct.visitMethod(meth.getModifiers() & ~Modifier.SYNCHRONIZED, meth.getName(),
                Type.getMethodDescriptor(meth), null, null);
        mv.visitCode();

        // push all the method params to the stack
        // we only start at param[1] as param[0] of the helper method is the instance itself
        // and will get loaded with ALOAD_0 (this)
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        for (int i = 1; i < helperParams.length; i++)
        {
            mv.visitVarInsn(AsmHelper.getLoadInsn(helperParams[i]), i);
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(helper.getDeclaringClass()), helper.getName(),
                Type.getMethodDescriptor(helper), false);

        mv.visitInsn(AsmHelper.getReturnInsn(meth.getReturnType()));
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    /**
     * Proxy the given method with one that overrides it by calling into the
     * given helper.
     */
    private void proxyBeforeAfterMethod(ClassWriterTracker ct, Class type, Method meth, Class[] helperParams,
                                        Method before, Method after, Class[] afterParams) {

        MethodVisitor mv = ct.visitMethod(meth.getModifiers() & ~Modifier.SYNCHRONIZED, meth.getName(),
                Type.getMethodDescriptor(meth), null, null);
        mv.visitCode();

        int beforeRetPos = -1;
        int variableNr = helperParams.length;;

        // invoke before
        if (before != null) {
            // push all the method params to the stack
            // we only start at param[1] as param[0] of the helper method is the instance itself
            // and will get loaded with ALOAD_0 (this)
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            for (int i = 1; i < helperParams.length; i++)
            {
                mv.visitVarInsn(AsmHelper.getLoadInsn(helperParams[i]), i);
            }

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(before.getDeclaringClass()), before.getName(),
                    Type.getMethodDescriptor(before), false);

            if (after != null && before.getReturnType() != void.class) {
                // this is always a boolean and 1 after the
                beforeRetPos = variableNr++;
                mv.visitVarInsn(AsmHelper.getStoreInsn(before.getReturnType()), beforeRetPos);
            }
        }

        // invoke super
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        for (int i = 1; i < helperParams.length; i++)
        {
            mv.visitVarInsn(AsmHelper.getLoadInsn(helperParams[i]), i);
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), meth.getName(),
                Type.getMethodDescriptor(meth), false);

        // invoke after
        if (after != null) {
            int retPos = -1;
            if (meth.getReturnType() != void.class) {
                retPos = variableNr++;
                mv.visitVarInsn(AsmHelper.getStoreInsn(meth.getReturnType()), retPos);
            }

            // push all the method params to the stack
            // we only start at param[1] as param[0] of the helper method is the instance itself
            // and will get loaded with ALOAD_0 (this)
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            for (int i = 1; i < helperParams.length; i++)
            {
                mv.visitVarInsn(AsmHelper.getLoadInsn(helperParams[i]), i);
            }

            if (retPos != -1) {
                mv.visitVarInsn(AsmHelper.getLoadInsn(meth.getReturnType()),retPos);
            }
            if (beforeRetPos != -1) {
                mv.visitVarInsn(AsmHelper.getLoadInsn(before.getReturnType()),beforeRetPos);
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(after.getDeclaringClass()), after.getName(),
                    Type.getMethodDescriptor(after), false);
        }

        mv.visitInsn(AsmHelper.getReturnInsn(meth.getReturnType()));
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }


    /**
     * add the instance variables to the class to be generated
     */
    private void addInstanceVariables(ClassWriterTracker ct) {
        // variable #1, the state manager
        ct.getCw().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                "sm", Type.getDescriptor(OpenJPAStateManager.class), null, null).visitEnd();

        // variable #2, the state manager
        ct.getCw().visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                "field", Type.getDescriptor(int.class), null, null).visitEnd();
    }


    /**
     * Create pass-through constructors to base type.
     */
    private void delegateConstructors(ClassWriterTracker ct, Class type, String superClassFileNname) {
        Constructor[] constructors = type.getConstructors();

        for (Constructor constructor : constructors) {
            Class[] params = constructor.getParameterTypes();
            String[] exceptionTypes = AsmHelper.getInternalNames(constructor.getExceptionTypes());
            String descriptor = Type.getConstructorDescriptor(constructor);
            MethodVisitor mv = ct.visitMethod(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, exceptionTypes);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            int stackPos = 1;
            for (Class param : params) {
                mv.visitVarInsn(AsmHelper.getLoadInsn(param), stackPos);
                stackPos += Type.getType(param).getSize();
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassFileNname, "<init>", descriptor, false);

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
    }

    /**
     * Implement the methods in the {@link Proxy} interface, with the exception
     * of {@link Proxy#copy}.
     *
     * @param defaultChangeTracker whether to implement a null change tracker; if false
     * the change tracker method is left unimplemented
     * @param proxyClassDef
     */
    private void addProxyMethods(ClassWriterTracker ct, boolean defaultChangeTracker, String proxyClassDef, Class<?> parentClass) {

        {
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "setOwner",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class), Type.INT_TYPE)
                    , null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassDef, "sm", Type.getDescriptor(OpenJPAStateManager.class));

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassDef, "field", Type.getDescriptor(Integer.TYPE));

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "getOwner",
                    Type.getMethodDescriptor(Type.getType(OpenJPAStateManager.class))
                    , null, null);
            mv.visitCode();

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassDef, "sm", Type.getDescriptor(OpenJPAStateManager.class));

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        {
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "getOwnerField",
                    Type.getMethodDescriptor(Type.INT_TYPE)
                    , null, null);
            mv.visitCode();

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassDef, "field", Type.INT_TYPE.getDescriptor());

            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        {
            /*
             * clone (return detached proxy object)
             * Note:  This method is only being provided to satisfy a quirk with
             * the IBM JDK -- while comparing Calendar objects, the clone() method
             * was invoked.  So, we are now overriding the clone() method so as to
             * provide a detached proxy object (null out the StateManager).
             */
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "clone",
                    Type.getMethodDescriptor(TYPE_OBJECT)
                    , null, null);
            mv.visitCode();

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(parentClass), "clone",
                    Type.getMethodDescriptor(TYPE_OBJECT), false);
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Proxy.class));
            mv.visitVarInsn(Opcodes.ASTORE, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 1);

            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Proxy.class), "setOwner",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OpenJPAStateManager.class), Type.INT_TYPE), true);

            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        if (defaultChangeTracker) {
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "getChangeTracker",
                    Type.getMethodDescriptor(Type.getType(ChangeTracker.class))
                    , null, null);
            mv.visitCode();
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
    }

    /**
     * Implement the methods in the {@link ProxyDate} interface.
     */
    private void addProxyDateMethods(ClassWriterTracker ct, String proxyClassDef, Class type) {

        final boolean hasDefaultCons = hasConstructor(type);
        final boolean hasMillisCons = hasConstructor(type, long.class);

        if (!hasDefaultCons && !hasMillisCons) {
            throw new UnsupportedException(_loc.get("no-date-cons", type));
        }

        // add a default constructor that delegates to the millis constructor
        if (!hasDefaultCons) {
            MethodVisitor mv = ct.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE), null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(System.class), "currentTimeMillis",
                    Type.getMethodDescriptor(Type.LONG_TYPE), false);

            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE), false);

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        {
            // date copy
            Constructor cons = findCopyConstructor(type);
            Class[] params;
            if (cons != null) {
                params = cons.getParameterTypes();
            }
            else if (hasMillisCons) {
                params = new Class[]{long.class};
            }
            else {
                params = new Class[0];
            }

            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "copy",
                    Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT)
                    , null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(type));
            mv.visitInsn(Opcodes.DUP);

            if (params.length == 1) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                if (params[0] == long.class) {
                    // call getTime on the given Date if the current type has a long constructor
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(java.util.Date.class));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(java.util.Date.class), "getTime",
                            Type.getMethodDescriptor(Type.LONG_TYPE), false);
                }
                else {
                    // otherwise just pass the parameter
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(params[0]));
                }
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.getParamTypes(params)), false);

            if (params.length == 0) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(java.util.Date.class));
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(java.util.Date.class), "getTime",
                        Type.getMethodDescriptor(Type.LONG_TYPE), false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), "setTime",
                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE), false);
            }
            if ((params.length == 0 || params[0] == long.class)
                    && Timestamp.class.isAssignableFrom(type)) {
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Timestamp.class));
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Timestamp.class), "getNanos",
                        Type.getMethodDescriptor(Type.INT_TYPE), false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), "setNanos",
                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);

            }

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        {
            // new instance factory
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "newInstance",
                    Type.getMethodDescriptor(Type.getType(ProxyDate.class))
                    , null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, proxyClassDef);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, proxyClassDef, "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE), false);

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
    }

    private void addProxyCalendarMethods(ClassWriterTracker ct, String proxyClassDef, Class type) {
        // calendar copy
        {
            Constructor cons = findCopyConstructor(type);
            Class[] params = (cons == null) ? new Class[0] : cons.getParameterTypes();

            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "copy",
                    Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT)
                    , null, null);
            mv.visitCode();

            mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(type));
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, AsmHelper.getParamTypes(params)), false);

            // timeInMillis
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Calendar.class));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Calendar.class), "getTimeInMillis",
                    Type.getMethodDescriptor(Type.LONG_TYPE), false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), "setTimeInMillis",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE), false);

            // lenient
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Calendar.class));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Calendar.class), "isLenient",
                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE), false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), "setLenient",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.BOOLEAN_TYPE), false);

            // firstDayOfWeek
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Calendar.class));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Calendar.class), "getFirstDayOfWeek",
                    Type.getMethodDescriptor(Type.INT_TYPE), false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), "setFirstDayOfWeek",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);

            // minimalDaysInFirstWeek
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Calendar.class));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Calendar.class), "getMinimalDaysInFirstWeek",
                    Type.getMethodDescriptor(Type.INT_TYPE), false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), "setMinimalDaysInFirstWeek",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE), false);

            // timeZone
            mv.visitInsn(Opcodes.DUP);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Calendar.class));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Calendar.class), "getTimeZone",
                    Type.getMethodDescriptor(Type.getType(TimeZone.class)), false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(type), "setTimeZone",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(TimeZone.class)), false);

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // newInstance factory
        {
            MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, "newInstance",
                    Type.getMethodDescriptor(Type.getType(ProxyCalendar.class))
                    , null, null);
            mv.visitCode();
            mv.visitTypeInsn(Opcodes.NEW, proxyClassDef);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, proxyClassDef, "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE), false);

            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }

        // proxy the protected computeFields method b/c it is called on
        // mutate, and some setters are final and therefore not proxyable
        {
            MethodVisitor mv = ct.visitMethod(Modifier.PROTECTED, "computeFields",
                    Type.getMethodDescriptor(Type.VOID_TYPE)
                    , null, null);
            mv.visitCode();

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Proxies.class), "dirty",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Proxy.class), Type.BOOLEAN_TYPE), false);

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "computeFields",
                    Type.getMethodDescriptor(Type.VOID_TYPE), false);

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
    }

    /**
     * Proxy setter methods of the given type.
     *
     * @return true if we generated any setters, false otherwise
     */
    private boolean proxySetters(ClassWriterTracker ct, String proxyClassDef, Class type) {
        Method[] meths = type.getMethods();

        int setters = 0;
        for (Method meth : meths) {
            if (isSetter(meth) && !Modifier.isFinal(meth.getModifiers())) {
                setters++;
                proxySetter(ct, type, meth);
            }
        }
        return setters > 0;
    }

    private void proxySetter(ClassWriterTracker ct, Class type, Method meth) {
        Class[] params = meth.getParameterTypes();
        Class ret = meth.getReturnType();

        final String methodDescriptor = Type.getMethodDescriptor(Type.getType(ret), AsmHelper.getParamTypes(params));
        if (ct.hasMethod(meth.getName(), methodDescriptor)) {
            // this method already got created
            return;
        }

        MethodVisitor mv = ct.visitMethod(Modifier.PUBLIC, meth.getName(), methodDescriptor, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Proxies.class), "dirty",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Proxy.class), Type.BOOLEAN_TYPE), false);

        mv.visitVarInsn(Opcodes.ALOAD, 0);

        // push all the method params to the stack
        int stackPos = 1;
        for (int i = 1; i <= params.length; i++) {
            Class param = params[i-1];
            mv.visitVarInsn(AsmHelper.getLoadInsn(param), stackPos);
            stackPos += Type.getType(param).getSize();
        }

        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), meth.getName(),
                methodDescriptor, false);

        mv.visitInsn(AsmHelper.getReturnInsn(ret));
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }


    /**
     * Add a writeReplace implementation that serializes to a non-proxy type
     * unless detached and this is a build-time generated class.
     */
    private void addWriteReplaceMethod(ClassWriterTracker ct, String proxyClassDef, boolean runtime) {
        MethodVisitor mv = ct.visitMethod(Modifier.PROTECTED, "writeReplace",
                Type.getMethodDescriptor(TYPE_OBJECT)
                , null, new String[]{Type.getInternalName(ObjectStreamException.class)});
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(runtime ? Opcodes.ICONST_0 : Opcodes.ICONST_1); // !runtime
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Proxies.class), "writeReplace",
                Type.getMethodDescriptor(TYPE_OBJECT, Type.getType(Proxy.class), Type.BOOLEAN_TYPE), false);

        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private boolean hasConstructor(Class type, Class<?>... paramTypes) {
        try {
            return type.getDeclaredConstructor(paramTypes) != null;
        }
        catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Return whether the given collection type allows duplicates.
     */
    protected boolean allowsDuplicates(Class type) {
        return !Set.class.isAssignableFrom(type);
    }

    /**
     * Return whether the given collection type maintains an artificial
     * ordering.
     */
    protected boolean isOrdered(Class type) {
        return List.class.isAssignableFrom(type)
            || "java.util.LinkedHashSet".equals(type.getName());
    }

    /**
     * Return the parameter types to the corresponding helper class method.
     */
    private static Class[] toHelperParameters(Class[] cls, Class helper) {
        Class[] params = new Class[cls.length + 1];
        params[0] = helper;
        System.arraycopy(cls, 0, params, 1, cls.length);
        return params;
    }

    /**
     * Return the parameter types to the corresponding helper class "after"
     * method.
     */
    private static Class[] toHelperAfterParameters(Class[] cls, Class ret,
        Class beforeRet) {
        if (ret == void.class && beforeRet == void.class)
            return cls;
        int len = cls.length;
        if (ret != void.class)
            len++;
        if (beforeRet != void.class)
            len++;
        Class[] params = new Class[len];
        System.arraycopy(cls, 0, params, 0, cls.length);
        int pos = cls.length;
        if (ret != void.class)
            params[pos++] = ret;
        if (beforeRet != void.class)
            params[pos++] = beforeRet;
        return params;
    }

    /**
     * Return whether the given method is a setter.
     */
    protected boolean isSetter(Method meth) {
        return startsWith(meth.getName(), "set")
            || startsWith(meth.getName(), "add")
            || startsWith(meth.getName(), "remove")
            || startsWith(meth.getName(), "insert")
            || startsWith(meth.getName(), "clear")
            || startsWith(meth.getName(), "roll"); // used by Calendar
    }

    /**
     * Return the getter corresponding to the given setter, or null.
     */
    protected Method findGetter(Class type, Method setter) {
        String name = setter.getName().substring(3);
        Class param = setter.getParameterTypes()[0];
        Method getter;
        try {
            getter = type.getMethod("get" + name, (Class[]) null);
            if (getter.getReturnType().isAssignableFrom(param)
                || param.isAssignableFrom(getter.getReturnType()))
                return getter;
        } catch (NoSuchMethodException nsme) {
        } catch (Exception e) {
            throw new GeneralException(e);
        }

        if (param == boolean.class || param == Boolean.class) {
            try {
                getter = type.getMethod("is" + name, (Class[]) null);
                if (getter.getReturnType().isAssignableFrom(param)
                    || param.isAssignableFrom(getter.getReturnType()))
                    return getter;
            } catch (NoSuchMethodException nsme) {
            } catch (Exception e) {
                throw new GeneralException(e);
            }
        }
        return null;
    }

    /**
     * Return whether the target string stars with the given token.
     */
    private static boolean startsWith(String str, String token) {
        return str.startsWith(token)
            && (str.length() == token.length()
            || Character.isUpperCase(str.charAt(token.length())));
    }

    /**
     * Create a unique id to avoid proxy class name conflicts.
     */
    private static synchronized long nextProxyId() {
        return _proxyId++;
    }

    /**
     * Find an appropriate copy constructor for the given type, or return null
     * if none.
     */
    protected Constructor findCopyConstructor(Class cls) {
        Constructor[] cons = cls.getConstructors();
        Constructor match = null;
        Class matchParam = null;
        Class[] params;
        for (Constructor con : cons) {
            params = con.getParameterTypes();
            if (params.length != 1)
                continue;

            // quit immediately on exact match
            if (params[0] == cls)
                return con;

            if (params[0].isAssignableFrom(cls) && (matchParam == null
                    || matchParam.isAssignableFrom(params[0]))) {
                // track most derived collection constructor
                match = con;
                matchParam = params[0];
            }
        }
        return match;
    }

    /**
     * Return the constructor that takes a comparator for the given type, or
     * null if none.
     */
    private static Constructor findComparatorConstructor(Class cls) {
        try {
            return cls.getConstructor(new Class[] { Comparator.class });
        } catch (NoSuchMethodException nsme) {
            return null;
        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Usage: java org.apache.openjpa.util.proxy.ProxyManagerImpl [option]*
     * &lt;class name&gt;+<br />
     * Where the following options are recognized:
     * <ul>
     * <li><i>-utils/-u &lt;number&gt;</i>: Generate proxies for the standard
     * java.util collection, map, date, and calendar classes of the given Java
     * version.  Use 4 for Java 1.4, 5 for Java 5, etc.</li>
     * </ul>
     *
     * The main method generates .class files for the proxies to the classes
     * given on the command line.  It writes the generated classes to beside the
     * ProxyManagerImpl.class file if possible; otherwise it writes to the
     * current directory.  The proxy manager looks for these classes
     * before generating its own proxies at runtime.
     */
    public static void main(String[] args)
        throws ClassNotFoundException, IOException {
        File dir = Files.getClassFile(ProxyManagerImpl.class);
        dir = (dir == null) ? new File(System.getProperty("user.dir")) : dir.getParentFile();

        Options opts = new Options();
        args = opts.setFromCmdLine(args);

        List types = new ArrayList(Arrays.asList(args));
        int utils = opts.removeIntProperty("utils", "u", 0);
        if (utils >= 4) {
            types.addAll(Arrays.asList(new String[] {
                java.sql.Date.class.getName(),
                java.sql.Time.class.getName(),
                java.sql.Timestamp.class.getName(),
                java.util.ArrayList.class.getName(),
                java.util.Date.class.getName(),
                java.util.GregorianCalendar.class.getName(),
                java.util.HashMap.class.getName(),
                java.util.HashSet.class.getName(),
                java.util.Hashtable.class.getName(),
                java.util.LinkedList.class.getName(),
                java.util.Properties.class.getName(),
                java.util.TreeMap.class.getName(),
                java.util.TreeSet.class.getName(),
                java.util.Vector.class.getName(),
            }));
        }
        if (utils >= 5) {
            types.addAll(Arrays.asList(new String[] {
                "java.util.EnumMap",
                "java.util.IdentityHashMap",
                "java.util.LinkedHashMap",
                "java.util.LinkedHashSet",
                "java.util.PriorityQueue",
            }));
        }

        final ProxyManagerImpl mgr = new ProxyManagerImpl();
        Class cls;
        for (Object type : types) {
            cls = Class.forName((String) type);
            try {
                if (Class.forName(getProxyClassName(cls, false), true,
                        GeneratedClasses.getMostDerivedLoader(cls, Proxy.class))
                        != null)
                    continue;
            }
            catch (Throwable t) {
                // expected if the class hasn't been generated
            }


            final String proxyClassName = getProxyClassName(cls, false);

            byte[] bytes = null;

            if (Date.class.isAssignableFrom(cls)) {
                bytes = mgr.generateProxyDateBytecode(cls, false, proxyClassName);
            }
            else if (Calendar.class.isAssignableFrom(cls)) {
                bytes = mgr.generateProxyCalendarBytecode(cls, false, proxyClassName);
            }
            else if (Collection.class.isAssignableFrom(cls)) {
                bytes = mgr.generateProxyCollectionBytecode(cls, false, proxyClassName);
            }
            else if (Map.class.isAssignableFrom(cls)) {
                bytes = mgr.generateProxyMapBytecode(cls, false, proxyClassName);
            }
            else {
                bytes = mgr.generateProxyBeanBytecode(cls, false, proxyClassName);
            }

            if (bytes != null) {
                final String fileName = cls.getName().replace('.', '$') + PROXY_SUFFIX + ".class";
                java.nio.file.Files.write(new File(dir, fileName).toPath(), bytes);
            }
        }
    }
}
