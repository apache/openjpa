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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.kernel.FetchState;
import org.apache.openjpa.kernel.LockManager;
import org.apache.openjpa.kernel.OpenJPAStateManager;
import org.apache.openjpa.kernel.PCState;
import org.apache.openjpa.kernel.StoreContext;
import org.apache.openjpa.kernel.StoreManager;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Closeable;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.UUIDGenerator;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.meta.ValueStrategies;
import serp.util.Strings;

/**
 * Helper for OpenJPA back-ends.
 *
 * @since 3.0
 * @author Abe White
 * @nojavadoc
 */
public class ImplHelper {

    private static final Localizer _loc = Localizer.forPackage
        (ImplHelper.class);

    /**
     * Return the getter method matching the given property name.
     */
    public static Method getGetter(Class cls, String prop) {
        prop = StringUtils.capitalize(prop);
        try {
            return cls.getMethod("get" + prop, (Class[]) null);
        }
        catch (Exception e) {
            try {
                return cls.getMethod("is" + prop, (Class[]) null);
            }
            catch (Exception e2) {
                throw new UserException(_loc.get("bad-getter", cls,
                    prop)).setCause(e);
            }
        }
    }

    /**
     * Return the setter method matching the given property name.
     */
    public static Method getSetter(Class cls, String prop) {
        Method getter = getGetter(cls, prop);
        prop = StringUtils.capitalize(prop);
        try {
            return cls.getMethod("set" + prop,
                new Class[]{ getter.getReturnType() });
        }
        catch (Exception e) {
            throw new UserException(_loc.get("bad-setter", cls, prop)).
                setCause(e);
        }
    }

    /**
     * Helper for store manager implementations. This method simply delegates
     * to the proper singular method for each state manager.
     *
     * @see StoreManager#loadAll
     * @since 4.0
     */
    public static Collection loadAll(Collection sms, StoreManager store,
        PCState state, int load, FetchState fetchState, Object context) {
        Collection failed = null;
        OpenJPAStateManager sm;
        LockManager lm;
        for (Iterator itr = sms.iterator(); itr.hasNext();) {
            sm = (OpenJPAStateManager) itr.next();
            if (sm.getManagedInstance() == null) {
                if (!store.initialize(sm, state, fetchState, context))
                    failed = addFailedId(sm, failed);
            } else if (load != StoreManager.FORCE_LOAD_NONE
                || sm.getPCState() == PCState.HOLLOW) {

                lm = sm.getContext().getLockManager();
                if (!store.load(sm, sm.getUnloaded(fetchState),
                    fetchState, lm.getLockLevel(sm), context))
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
        return generateValue(ctx, null, fmd, fmd.getDeclaredTypeCode());
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
     * Return the store-specific facade class for the given broker
     * component class. This method is used by facade implementations to
     * wrap store-specific components without knowing about all possible
     * back-ends.
     *
     * @param conf configuration for runtime
     * @param openjpaCls class of OpenJPA component (e.g.
     * JDBCFetchConfiguration.class)
     * @param openjpaSuff suffix of OpenJPA component (e.g. "FetchConfiguration")
     * @param facadePkg the unqualified facade package name (e.g. "jdo")
     * @param facadeCls the generic facade interface's class (e.g.
     * FetchPlan.class)
     * @param facadeSuff the suffix to append to the store prefix to get
     * the implementation class name (e.g. "FetchPlanImpl")
     * or null to use the unqualified name of
     * <code>facadeCls</code>
     * @return the class formed by taking the top-most org.apache.openjpa.aaa package and
     * BBBStoreManager name prefix from <code>storeCls</code> and
     * combining them with the facade package ccc and suffix DDD to
     * get: org.apache.openjpa.ccc.aaa.BBBDDD
     */
    public static Class getStoreFacadeType(OpenJPAConfiguration conf,
        Class openjpaCls, String openjpaSuff, String facadePkg, Class facadeCls,
        String facadeSuff) {
        String clsName = openjpaCls.getName();
        int dotIdx = clsName.lastIndexOf('.');
        int suffixIdx = clsName.indexOf(openjpaSuff, dotIdx + 1);
        if (!clsName.startsWith("org.apache.openjpa.") || suffixIdx == -1)
            return null;

        // extract 'xxx.' from org.apache.openjpa.xxx.yyy..., and XXX from XXXStoreManager
        String pkg = clsName.substring(5, clsName.indexOf('.', 5) + 1);
        String prefix = clsName.substring(dotIdx + 1, suffixIdx);

        // suffix of impl class name
        if (facadeSuff == null)
            facadeSuff = Strings.getClassName(facadeCls);

        clsName =
            "org.apache.openjpa." + facadePkg + "." + pkg + prefix + facadeSuff;
        try {
            return Class.forName(clsName, true, facadeCls.getClassLoader());
        }
        catch (ClassNotFoundException ncfe) {
            Log log = conf.getLog(OpenJPAConfiguration.LOG_RUNTIME);
            if (log.isTraceEnabled())
                log.trace(_loc.get("no-store-exts", clsName));
            return null;
        }
        catch (Exception e) {
            throw new InternalException(e);
        }
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
        }
        catch (RuntimeException re) {
            throw re;
        }
        catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Returns true if the specified class is a type that can be managed by
     * OpenJPA.
     *
     * @param type the class to test
     * @return true if the class is manageable.
     * @param conf the configuration that defines the current context
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
