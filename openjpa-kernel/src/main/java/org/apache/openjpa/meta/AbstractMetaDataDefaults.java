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
package org.apache.openjpa.meta;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.enhance.PCRegistry;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.UserException;

/**
 * Abstract metadata defaults.
 *
 * @author Abe White
 */
public abstract class AbstractMetaDataDefaults
    implements MetaDataDefaults {

    private static final Localizer _loc = Localizer.forPackage
        (AbstractMetaDataDefaults.class);

    private int _access = ClassMetaData.ACCESS_FIELD;
    private boolean _ignore = true;
    private boolean _pcRegistry = true;
    private int _callback = CALLBACK_RETHROW;

    /**
     * Whether to attempt to use the information from registered classes
     * to populate metadata defaults. Defaults to true.
     */
    public boolean getUsePCRegistry() {
        return _pcRegistry;
    }

    /**
     * Whether to attempt to use the information from registered classes
     * to populate metadata defaults. Defaults to true.
     */
    public void setUsePCRegistry(boolean pcRegistry) {
        _pcRegistry = pcRegistry;
    }

    /**
     * The default access type for base classes with ACCESS_UNKNOWN.
     * Returns ACCESS_FIELD by default.
     */
    public void setDefaultAccessType(int access) {
        _access = access;
    }

    /**
     * The default access type for base classes with ACCESS_UNKNOWN.
     * Returns ACCESS_FIELD by default.
     */
    public int getDefaultAccessType() {
        return _access;
    }

    public int getCallbackMode() {
        return _callback;
    }

    public void setCallbackMode(int mode) {
        _callback = mode;
    }

    public void setCallbackMode(int mode, boolean on) {
        if (on)
            _callback |= mode;
        else
            _callback &= ~mode;
    }

    public boolean getCallbacksBeforeListeners(int type) {
        return false;
    }

    public boolean getIgnoreNonPersistent() {
        return _ignore;
    }

    public void setIgnoreNonPersistent(boolean ignore) {
        _ignore = ignore;
    }

    public void populate(ClassMetaData meta, int access) {
        if (meta.getDescribedType() == Object.class)
            return;

        if (access == ClassMetaData.ACCESS_UNKNOWN) {
            // we do not allow using both field and method access at
            // the same time
            access = getAccessType(meta);
            if ((access & ClassMetaData.ACCESS_FIELD) != 0
                && (access & ClassMetaData.ACCESS_PROPERTY) != 0)
                throw new UserException(_loc.get("access-field-and-prop",
                    meta.getDescribedType().getName()));
        }
        meta.setAccessType(access);

        Log log = meta.getRepository().getLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("gen-meta", meta));
        if (!_pcRegistry || !populateFromPCRegistry(meta)) {
            if (log.isTraceEnabled())
                log.trace(_loc.get("meta-reflect"));
            populateFromReflection(meta);
        }
    }

    /**
     * Populate initial field data. Does nothing by default.
     */
    protected void populate(FieldMetaData fmd) {
    }

    /**
     * Populate the given metadata using the {@link PCRegistry}.
     */
    private boolean populateFromPCRegistry(ClassMetaData meta) {
        Class cls = meta.getDescribedType();
        try {
            String[] fieldNames = PCRegistry.getFieldNames(cls);
            Class[] fieldTypes = PCRegistry.getFieldTypes(cls);
            Member member;
            FieldMetaData fmd;
            for (int i = 0; i < fieldNames.length; i ++) {
                if (meta.getAccessType() == ClassMetaData.ACCESS_FIELD)
                    member = cls.getDeclaredField(fieldNames[i]);
                else
                    member = getBackingMethod(meta.getDescribedType(),
                        fieldNames[i], fieldTypes[i]);
                fmd = meta.addDeclaredField(fieldNames[i], fieldTypes[i]);
                fmd.backingMember(member);
                populate(fmd);
            }
            return true;
        } catch (IllegalStateException iae) {
            // thrown by registry when no metadata available
            return false;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new UserException(e);
        }
    }

    /**
     * Generate the given metadata using reflection.
     */
    private void populateFromReflection(ClassMetaData meta) {
        Member[] members;
        if (meta.getAccessType() == ClassMetaData.ACCESS_FIELD)
            members = meta.getDescribedType().getDeclaredFields();
        else
            members = meta.getDescribedType().getDeclaredMethods();

        int mods;
        String name;
        boolean def;
        FieldMetaData fmd;
        for (int i = 0; i < members.length; i++) {
            mods = members[i].getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isFinal(mods))
                continue;

            name = getFieldName(members[i]);
            if (name == null || isReservedFieldName(name))
                continue;

            def = isDefaultPersistent(meta, members[i], name);
            if (!def && _ignore)
                continue;

            // passed the tests; persistent type -- we construct with
            // Object.class because setting backing member will set proper
            // type anyway
            fmd = meta.addDeclaredField(name, Object.class);
            fmd.backingMember(members[i]);
            if (!def) {
                fmd.setExplicit(true);
                fmd.setManagement(FieldMetaData.MANAGE_NONE);
            }
            populate(fmd);
        }
    }

    /**
     * Return the access type of the given metadata. May be a bitwise
     * combination of field and property access constants, or ACCESS_UNKNOWN.
     * Returns ACCESS_FIELD by default.
     */
    protected int getAccessType(ClassMetaData meta) {
        return ClassMetaData.ACCESS_FIELD;
    }

    /**
     * Return the field name for the given member. This will only be invoked
     * on members of the right type (field vs. method). Return null if the
     * member cannot be managed. Default behavior: For fields, returns the
     * field name. For getter methods, returns the minus "get" or "is" with
     * the next letter lower-cased. For other methods, returns null.
     */
    protected String getFieldName(Member member) {
        if (member instanceof Field)
            return member.getName();

        Method meth = (Method) member;
        if (meth.getReturnType() == void.class
            || meth.getParameterTypes().length != 0)
            return null;

        String name = meth.getName();
        if (name.startsWith("get") && name.length() > 3)
            name = name.substring(3);
        else if ((meth.getReturnType() == boolean.class
            || meth.getReturnType() == Boolean.class)
            && name.startsWith("is") && name.length() > 2)
            name = name.substring(2);
        else
            return null;

        if (name.length() == 1)
            return name.toLowerCase();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Returns true if the given field name is reserved for unmanaged fields.
     */
    protected boolean isReservedFieldName(String name) {
        // names used by enhancers
        return name.startsWith("openjpa") || name.startsWith("jdo");
    }

    /**
     * Return true if the given member is persistent by default. This will
     * only be invoked on members of the right type (field vs. method).
     * Returns false if member is static or final by default.
     *
     * @param name the field name from {@link #getFieldName}
     */
    protected abstract boolean isDefaultPersistent(ClassMetaData meta,
        Member member, String name);

    public Member getBackingMember(FieldMetaData fmd) {
        if (fmd == null)
            return null;
        try {
            //### note that we might not have access to declaring metadata yet
            //### (this could be used during parse), so we have to settle for
            //### defining.  could cause problems if maps a superclass field
            //### where the superclass uses a different access type
            if (fmd.getDefiningMetaData().getAccessType() ==
                ClassMetaData.ACCESS_FIELD)
                return fmd.getDeclaringType().getDeclaredField(fmd.getName());
            return getBackingMethod(fmd.getDeclaringType(), fmd.getName(),
                fmd.getDeclaredType());
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new InternalException(e);
        }
    }

    /**
     * Return the method backing the given field metadata. Looks for
     * "get" and "is" methods with no parameters by default. This looks
     * for elements defined in <code>cls</code> and its superclasses.
     */
    private Method getBackingMethod(Class cls, String name,
        Class methReturnType)
        throws NoSuchMethodException {
        String clsName = cls.getName();
        String capName = StringUtils.capitalize(name);
        boolean isBoolean = methReturnType == boolean.class
            || methReturnType == Boolean.class;
        do {
            try {
                return cls.getDeclaredMethod("get" + capName, (Class[]) null);
            } catch (NoSuchMethodException e) {
            }

            if (isBoolean) {
                try {
                    return cls.getDeclaredMethod("is" + capName,
                        (Class[]) null);
                } catch (NoSuchMethodException e) {
                }
            }
            // ### EJB3: recursion should be limited to manageable types,
            // ### including embeddable and embeddable superclass
            cls = cls.getSuperclass();
        }
        while (cls != null);

        if (!isBoolean)
            throw new UserException(_loc.get("pc-registry-no-method",
                name, clsName, "get" + capName));
        throw new UserException(_loc.get("pc-registry-no-boolean-method",
            new String[]{ name, clsName, "get" + capName, "is" + capName }));
    }

    /**
     * Helper method; returns true if the given class appears to be
     * user-defined.
     */
    protected static boolean isUserDefined(Class cls) {
        return cls != null && !cls.getName().startsWith("java.")
            && !cls.getName().startsWith ("javax.");
	}
}
