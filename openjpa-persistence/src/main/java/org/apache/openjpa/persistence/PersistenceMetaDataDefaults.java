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
package org.apache.openjpa.persistence;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.meta.AbstractMetaDataDefaults;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.ValueMetaData;
import static org.apache.openjpa.persistence.PersistenceStrategy.*;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.conf.OpenJPAConfiguration;

/**
 * JPA-based metadata defaults.
 *
 * @author Patrick Linskey
 * @author Abe White
 */
public class PersistenceMetaDataDefaults
    extends AbstractMetaDataDefaults {

    private boolean _allowsMultipleMethodsForSameCallback = false;

    private static Localizer _loc = Localizer.forPackage
        (PersistenceMetaDataDefaults.class);

    private static final Map<Class, PersistenceStrategy> _strats =
        new HashMap<Class, PersistenceStrategy>();
    private static final Set<String> _ignoredAnnos = new HashSet<String>();

    static {
        _strats.put(Basic.class, BASIC);
        _strats.put(ManyToOne.class, MANY_ONE);
        _strats.put(OneToOne.class, ONE_ONE);
        _strats.put(Embedded.class, EMBEDDED);
        _strats.put(EmbeddedId.class, EMBEDDED);
        _strats.put(OneToMany.class, ONE_MANY);
        _strats.put(ManyToMany.class, MANY_MANY);
        _strats.put(Persistent.class, PERS);
        _strats.put(PersistentCollection.class, PERS_COLL);
        _strats.put(PersistentMap.class, PERS_MAP);

        _ignoredAnnos.add(DetachedState.class.getName());
        _ignoredAnnos.add(PostLoad.class.getName());
        _ignoredAnnos.add(PostPersist.class.getName());
        _ignoredAnnos.add(PostRemove.class.getName());
        _ignoredAnnos.add(PostUpdate.class.getName());
        _ignoredAnnos.add(PrePersist.class.getName());
        _ignoredAnnos.add(PreRemove.class.getName());
        _ignoredAnnos.add(PreUpdate.class.getName());
    }

    public PersistenceMetaDataDefaults() {
        setCallbackMode(CALLBACK_RETHROW | CALLBACK_ROLLBACK |
            CALLBACK_FAIL_FAST);
        setDataStoreObjectIdFieldUnwrapped(true);
    }

    /**
     * Return the code for the strategy of the given member. Return null if
     * no strategy.
     */
    public static PersistenceStrategy getPersistenceStrategy
        (FieldMetaData fmd, Member member) {
        if (member == null)
            return null;
        AnnotatedElement el = (AnnotatedElement) member;
        if (el.isAnnotationPresent(Transient.class))
            return TRANSIENT;
        if (fmd != null
            && fmd.getManagement() != FieldMetaData.MANAGE_PERSISTENT)
            return null;

        // look for persistence strategy in annotation table
        PersistenceStrategy pstrat = null;
        for (Annotation anno : el.getDeclaredAnnotations()) {
            if (pstrat != null && _strats.containsKey(anno.annotationType()))
                throw new MetaDataException(_loc.get("already-pers", member));
            if (pstrat == null)
                pstrat = _strats.get(anno.annotationType());
        }
        if (pstrat != null)
            return pstrat;

        Class type;
        int code;
        if (fmd != null) {
            type = fmd.getType();
            code = fmd.getTypeCode();
        } else if (member instanceof Field) {
            type = ((Field) member).getType();
            code = JavaTypes.getTypeCode(type);
        } else {
            type = ((Method) member).getReturnType();
            code = JavaTypes.getTypeCode(type);
        }

        switch (code) {
            case JavaTypes.ARRAY:
                if (type == byte[].class
                    || type == char[].class
                    || type == Byte[].class
                    || type == Character[].class)
                    return BASIC;
                break;
            case JavaTypes.BOOLEAN:
            case JavaTypes.BOOLEAN_OBJ:
            case JavaTypes.BYTE:
            case JavaTypes.BYTE_OBJ:
            case JavaTypes.CHAR:
            case JavaTypes.CHAR_OBJ:
            case JavaTypes.DOUBLE:
            case JavaTypes.DOUBLE_OBJ:
            case JavaTypes.FLOAT:
            case JavaTypes.FLOAT_OBJ:
            case JavaTypes.INT:
            case JavaTypes.INT_OBJ:
            case JavaTypes.LONG:
            case JavaTypes.LONG_OBJ:
            case JavaTypes.SHORT:
            case JavaTypes.SHORT_OBJ:
            case JavaTypes.STRING:
            case JavaTypes.BIGDECIMAL:
            case JavaTypes.BIGINTEGER:
            case JavaTypes.DATE:
                return BASIC;
            case JavaTypes.OBJECT:
                if (Enum.class.isAssignableFrom(type))
                    return BASIC;
                break;
        }

        //### EJB3: what if defined in XML?
        if (type.isAnnotationPresent(Embeddable.class))
            return EMBEDDED;
        if (Serializable.class.isAssignableFrom(type))
            return BASIC;
        return null;
    }
    
    /** 
     * Flags if multiple methods of the same class can handle the same 
     * callback event.
     */
    public boolean getAllowsMultipleMethodsForSameCallback() {
        return _allowsMultipleMethodsForSameCallback;
    }
    
    /** 
     * Flags if multiple methods of the same class can handle the same 
     * callback event.
     */
    public void setAllowsMultipleMethodsForSameCallback(boolean flag) {
        _allowsMultipleMethodsForSameCallback = flag;
    }

    /**
     * Auto-configuration method for the default access type of base classes 
     * with ACCESS_UNKNOWN
     */
    public void setDefaultAccessType(String type) {
        if (type == null)
            return;
        if ("PROPERTY".equals(type.toUpperCase()))
            setDefaultAccessType(ClassMetaData.ACCESS_PROPERTY);
        else
            setDefaultAccessType(ClassMetaData.ACCESS_FIELD);
    }

    @Override
    public void populate(ClassMetaData meta, int access) {
        super.populate(meta, access);
        meta.setDetachable(true);
        // do not call get*Fields as it will lock down the fields.
    }

    @Override
    protected void populate(FieldMetaData fmd) {
        setCascadeNone(fmd);
        setCascadeNone(fmd.getKey());
        setCascadeNone(fmd.getElement());
    }

    /**
     * Turns off auto cascading of persist, refresh, attach.
     */
    static void setCascadeNone(ValueMetaData vmd) {
        vmd.setCascadePersist(ValueMetaData.CASCADE_NONE);
        vmd.setCascadeRefresh(ValueMetaData.CASCADE_NONE);
        vmd.setCascadeAttach(ValueMetaData.CASCADE_NONE);
    }

    @Override
    protected int getAccessType(ClassMetaData meta) {
        return getAccessType(meta.getDescribedType());
    }

    /**
     * Recursive helper to determine access type based on annotation placement.
     */
    private int getAccessType(Class cls) {
        // traversed entire hierarchy without finding annotations
        if (cls == null || cls == Object.class)
            return ClassMetaData.ACCESS_UNKNOWN;

        int access = 0;
        if (usesAccess((Field[]) AccessController.doPrivileged(
            J2DoPrivHelper.getDeclaredFieldsAction(cls))))
            access |= ClassMetaData.ACCESS_FIELD;
        if (usesAccess((Method[]) AccessController.doPrivileged(
            J2DoPrivHelper.getDeclaredMethodsAction(cls))))
            access |= ClassMetaData.ACCESS_PROPERTY;
        return (access == 0) ? getAccessType(cls.getSuperclass()) : access;
    }

    /**
     * Return whether the given members have persistence annotations.
     */
    private static boolean usesAccess(AnnotatedElement[] members) {
        Annotation[] annos;
        String name;
        for (int i = 0; i < members.length; i++) {
            annos = members[i].getAnnotations();
            for (int j = 0; j < annos.length; j++) {
                name = annos[j].annotationType().getName();
                if ((name.startsWith("javax.persistence.")
                    || name.startsWith("org.apache.openjpa.persistence."))
                    && !_ignoredAnnos.contains(name))
                    return true;
            }
        }
        return false;
    }

    protected boolean isDefaultPersistent(ClassMetaData meta, Member member,
        String name) {
        int mods = member.getModifiers();
        if (Modifier.isTransient(mods))
            return false;

        if (member instanceof Method) {
            try {
                // check for setters for methods
                Method setter = (Method) AccessController.doPrivileged(
                    J2DoPrivHelper.getDeclaredMethodAction(
                        meta.getDescribedType(), "set" +
                        StringUtils.capitalize(name), new Class[] { 
                            ((Method) member).getReturnType() }));
                if (setter == null && !isAnnotatedTransient(member)) {
                    logNoSetter(meta, name, null);
                    return false;
                }
            } catch (Exception e) {
                // e.g., NoSuchMethodException
                if (!isAnnotatedTransient(member))
                    logNoSetter(meta, name, e);
                return false;
            }
        }

        PersistenceStrategy strat = getPersistenceStrategy(null, member);
        if (strat == null || strat == PersistenceStrategy.TRANSIENT)
            return false;
        return true;
	}

    private boolean isAnnotatedTransient(Member member) {
        return member instanceof AnnotatedElement
            && ((AnnotatedElement) member).isAnnotationPresent(Transient.class);
    }

    private void logNoSetter(ClassMetaData meta, String name, Exception e) {
        Log log = meta.getRepository().getConfiguration()
            .getLog(OpenJPAConfiguration.LOG_METADATA);
        if (log.isWarnEnabled())
            log.warn(_loc.get("no-setter-for-getter", name,
                meta.getDescribedType().getName()));
        else if (log.isTraceEnabled())
            // log the exception, if any, if we're in trace-level debugging
            log.warn(_loc.get("no-setter-for-getter", name,
                meta.getDescribedType().getName()), e);
    }
}
