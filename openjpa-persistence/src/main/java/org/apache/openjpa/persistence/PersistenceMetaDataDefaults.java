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

import static jakarta.persistence.AccessType.FIELD;
import static jakarta.persistence.AccessType.PROPERTY;
import static org.apache.openjpa.persistence.PersistenceStrategy.BASIC;
import static org.apache.openjpa.persistence.PersistenceStrategy.ELEM_COLL;
import static org.apache.openjpa.persistence.PersistenceStrategy.EMBEDDED;
import static org.apache.openjpa.persistence.PersistenceStrategy.MANY_MANY;
import static org.apache.openjpa.persistence.PersistenceStrategy.MANY_ONE;
import static org.apache.openjpa.persistence.PersistenceStrategy.ONE_MANY;
import static org.apache.openjpa.persistence.PersistenceStrategy.ONE_ONE;
import static org.apache.openjpa.persistence.PersistenceStrategy.PERS;
import static org.apache.openjpa.persistence.PersistenceStrategy.PERS_COLL;
import static org.apache.openjpa.persistence.PersistenceStrategy.PERS_MAP;
import static org.apache.openjpa.persistence.PersistenceStrategy.TRANSIENT;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.Reflection;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.AbstractMetaDataDefaults;
import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.MetaDataException;
import org.apache.openjpa.util.UserException;

/**
 * JPA-based metadata defaults.
 *
 * @author Patrick Linskey
 * @author Abe White
 * @author Pinaki Poddar
 */
public class PersistenceMetaDataDefaults
    extends AbstractMetaDataDefaults {

    private static final Localizer _loc = Localizer.forPackage
        (PersistenceMetaDataDefaults.class);

    private static final Map<Class<?>, PersistenceStrategy> _strats =
        new HashMap<>();
    private static final Set<String> _ignoredAnnos = new HashSet<>();
    private static final Set<Class<?>> _accessDefiningAnnos = new HashSet<>();

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
        _strats.put(ElementCollection.class, ELEM_COLL);
        _strats.put(PersistentMap.class, PERS_MAP);

        _ignoredAnnos.add(DetachedState.class.getName());
        _ignoredAnnos.add(PostLoad.class.getName());
        _ignoredAnnos.add(PostPersist.class.getName());
        _ignoredAnnos.add(PostRemove.class.getName());
        _ignoredAnnos.add(PostUpdate.class.getName());
        _ignoredAnnos.add(PrePersist.class.getName());
        _ignoredAnnos.add(PreRemove.class.getName());
        _ignoredAnnos.add(PreUpdate.class.getName());

        _accessDefiningAnnos.add(Id.class);
        _accessDefiningAnnos.add(EmbeddedId.class);
        _accessDefiningAnnos.add(Version.class);
        _accessDefiningAnnos.addAll(_strats.keySet());
    }

	/**
     * Set of Inclusion Filters based on member type, access type or transient
     * annotations. Used to determine the persistent field/methods.
     */
    protected AccessFilter propertyAccessFilter = new AccessFilter(PROPERTY);
    protected AccessFilter fieldAccessFilter = new AccessFilter(FIELD);

    protected MemberFilter fieldFilter = new MemberFilter(Field.class);
    protected MemberFilter methodFilter = new MemberFilter(Method.class);
    protected TransientFilter nonTransientFilter = new TransientFilter(false);
    protected AnnotatedFilter annotatedFilter = new AnnotatedFilter();
    protected AccessTypeFilter accessTypeFilter = new AccessTypeFilter();
    protected GetterFilter getterFilter = new GetterFilter();
    protected SetterFilter setterFilter = new SetterFilter();
    private Boolean _isAbstractMappingUniDirectional = null;
    private Boolean _isNonDefaultMappingAllowed = null;
    private String _defaultSchema;
    private Boolean _isCascadePersistPersistenceUnitDefaultEnabled = null;

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
        return getPersistenceStrategy(fmd, member, false);
    }

    /**
     * Return the code for the strategy of the given member. Return null if
     * no strategy.
     */
    public static PersistenceStrategy getPersistenceStrategy
        (FieldMetaData fmd, Member member, boolean ignoreTransient) {
        if (member == null)
            return null;
        AnnotatedElement el = (AnnotatedElement) member;
        if (!ignoreTransient && el.isAnnotationPresent(Transient.class))
            return TRANSIENT;
        if (fmd != null
            && fmd.getManagement() != FieldMetaData.MANAGE_PERSISTENT)
            return null;

        // look for persistence strategy in annotation table
        PersistenceStrategy pstrat = null;
        for (Annotation anno : el.getDeclaredAnnotations()) {
            PersistenceStrategy newStrat = _strats.get(anno.annotationType());
            if (newStrat == null)
                continue;
            if (pstrat != null) {
                // @Basic can coexist with a more specific strategy — the specific one wins
                if (pstrat == BASIC) {
                    pstrat = newStrat;
                    continue;
                }
                if (newStrat == BASIC) {
                    continue;
                }
                throw new MetaDataException(_loc.get("already-pers", member));
            }
            pstrat = newStrat;
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
            case JavaTypes.LOCAL_DATE:
            case JavaTypes.LOCAL_TIME:
            case JavaTypes.LOCAL_DATETIME:
            case JavaTypes.OFFSET_TIME:
            case JavaTypes.OFFSET_DATETIME:
            case JavaTypes.INSTANT:
            case JavaTypes.YEAR:
                return BASIC;
            case JavaTypes.OBJECT:
                if (Enum.class.isAssignableFrom(type))
                    return BASIC;
                break;
        }

        //### EJB3: what if defined in XML?
        if (type.isAnnotationPresent(Embeddable.class))
            return EMBEDDED;
        // Also check if the type has been declared as embeddable via XML
        // mapping (orm.xml) without the @Embeddable annotation.
        if (fmd != null) {
            MetaDataRepository repos = fmd.getRepository();
            if (repos != null) {
                ClassMetaData typeMeta = repos.getCachedMetaData(type);
                if (typeMeta != null && typeMeta.isEmbeddedOnly()) {
                    return EMBEDDED;
                }
            }
        }
        if (Serializable.class.isAssignableFrom(type))
            return BASIC;
        return null;
    }

    /**
     * Auto-configuration method for the default access type of base classes
     * with ACCESS_UNKNOWN
     */
    public void setDefaultAccessType(String type) {
        if ("PROPERTY".equals(type.toUpperCase(Locale.ENGLISH)))
            setDefaultAccessType(AccessCode.PROPERTY);
        else if ("FIELD".equals(type.toUpperCase(Locale.ENGLISH)))
            setDefaultAccessType(AccessCode.FIELD);
        else
        	throw new IllegalArgumentException(_loc.get("access-invalid",
        	    type).toString());
    }

    /**
     * Populates the given class metadata. The access style determines which
     * field and/or getter method will contribute as the persistent property
     * of the given class. If the given access is unknown, then the access
     * type is to be determined at first.
     *
     * @see #determineAccessType(ClassMetaData)
     */
    @Override
    public void populate(ClassMetaData meta, int access) {
        populate(meta, access, false);
    }

    /**
     * Populates the given class metadata. The access style determines which
     * field and/or getter method will contribute as the persistent property
     * of the given class. If the given access is unknown, then the access
     * type is to be determined at first.
     *
     * @see #determineAccessType(ClassMetaData)
     */
    @Override
    public void populate(ClassMetaData meta, int access, boolean ignoreTransient) {
    	if (AccessCode.isUnknown(access)) {
    		access = determineAccessType(meta);
    	}
    	if (AccessCode.isUnknown(access)) {
    		error(meta, _loc.get("access-unknown", meta));
    	}
        super.populate(meta, access, ignoreTransient);
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
     * Turns off auto cascading of persist, refresh, attach, detach.
     */
    static void setCascadeNone(ValueMetaData vmd) {
        vmd.setCascadePersist(ValueMetaData.CASCADE_NONE);
        vmd.setCascadeRefresh(ValueMetaData.CASCADE_NONE);
        vmd.setCascadeAttach(ValueMetaData.CASCADE_NONE);
        vmd.setCascadeDetach(ValueMetaData.CASCADE_NONE);
    }

    ClassMetaData getCachedSuperclassMetaData(ClassMetaData meta) {
    	if (meta == null)
    		return null;
    	Class<?> cls = meta.getDescribedType();
    	Class<?> sup = cls.getSuperclass();
    	if (sup == null || "java.lang.Object".equals(
    	    sup.getName()))
    		return null;
    	MetaDataRepository repos = meta.getRepository();
    	ClassMetaData supMeta = repos.getCachedMetaData(sup);
    	if (supMeta == null)
    		supMeta = repos.getMetaData(sup, null, false);
    	return supMeta;
    }

    /**
     * Recursive helper to determine access type based on annotation placement
     * on members for the given class without an explicit access annotation.
     *
     * @return must return a not-unknown access code
     */
    private int determineAccessType(ClassMetaData meta) {
    	if (meta == null)
    		return AccessCode.UNKNOWN;
        if (meta.getDescribedType().isInterface()) // managed interfaces
        	return AccessCode.PROPERTY;
        // JPA 3.2: records use FIELD access - components map to fields
        if (meta.getDescribedType().isRecord())
            return AccessCode.FIELD;
    	if (!AccessCode.isUnknown(meta))
    		return meta.getAccessType();
    	int access = determineExplicitAccessType(meta.getDescribedType());
    	if (!AccessCode.isUnknown(access))
    		return access;
    	access = determineImplicitAccessType(meta.getDescribedType(),
    	            meta.getRepository().getConfiguration());

    	ClassMetaData sup = getCachedSuperclassMetaData(meta);
    	ClassMetaData tmpSup = sup;
    	while (tmpSup != null && tmpSup.isExplicitAccess()) {
            tmpSup = getCachedSuperclassMetaData(tmpSup);
            if (tmpSup != null) {
                sup = tmpSup;
            }
    	}

    	if (!AccessCode.isUnknown(access)) {
    		// If implicit access conflicts with superclass, prefer the
    		// superclass access type. Per JPA spec, a subclass without
    		// explicit @Access inherits from its persistent superclass.
    		if (sup != null && !AccessCode.isUnknown(sup)
    			&& !AccessCode.isCompatibleSuper(access, sup.getAccessType())) {
    			return sup.getAccessType();
    		}
    		return access;
    	}

    	if (sup != null && !AccessCode.isUnknown(sup))
    		return sup.getAccessType();

        trace(meta, _loc.get("access-default", meta, AccessCode.toClassString(getDefaultAccessType())));
        return getDefaultAccessType();
    }

    /**
     * Determines the access type for the given class by placement of
     * annotations on field or getter method. Does not consult the
     * super class.
     *
     * Annotation can be placed on either fields or getters but not on both.
     * If no field or getter is annotated then UNKNOWN access code is returned.
     */
    private int determineImplicitAccessType(Class<?> cls, OpenJPAConfiguration
        conf) {
    	if (cls.isInterface()) // Managed interfaces
    		return AccessCode.PROPERTY;
        Field[] allFields = cls.getDeclaredFields();
		Method[] methods = cls.getDeclaredMethods();
        List<Field> fields = filter(allFields, new TransientFilter(true));
        /*
         * OpenJPA 1.x permitted private properties to be persistent.  This is
         * contrary to the JPA 1.0 specification, which states that persistent
         * properties must be public or protected. OpenJPA 2.0+ will adhere
         * to the specification by default, but provides a compatibility
         * option to provide pre-2.0 behavior.
         */
        getterFilter.setIncludePrivate(
            conf.getCompatibilityInstance().getPrivatePersistentProperties());
        List<Method> getters = filter(methods, getterFilter);
        if (fields.isEmpty() && getters.isEmpty())
        	return AccessCode.EMPTY;

        // Use access-defining filter for access type determination.
        AccessDefiningFilter accessFilter = new AccessDefiningFilter();
        List<Field> accessFields = filter(fields, accessFilter);
        List<Method> accessGetters = filter(getters, accessFilter);

        List<Method> setters = filter(methods, setterFilter);
        accessGetters = matchGetterAndSetter(accessGetters, setters);

        boolean mixed = !accessFields.isEmpty() && !accessGetters.isEmpty();
        if (mixed) {
            // Collect getter property names for overlap detection
            Set<String> getterPropertyNames = new HashSet<>();
            for (Method getter : accessGetters) {
                String gn = getter.getName();
                if (gn.startsWith("get") && gn.length() > 3) {
                    getterPropertyNames.add(
                        Character.toLowerCase(gn.charAt(3)) + gn.substring(4));
                } else if (gn.startsWith("is") && gn.length() > 2) {
                    getterPropertyNames.add(
                        Character.toLowerCase(gn.charAt(2)) + gn.substring(3));
                }
            }

            // Remove fields whose getter also has access-defining annotations
            // (same attribute annotated on both — property access wins).
            List<Field> uniqueAccessFields = new ArrayList<>();
            for (Field f : accessFields) {
                if (!getterPropertyNames.contains(f.getName())) {
                    uniqueAccessFields.add(f);
                }
            }

            // Remove getters whose field is @Transient (implicit
            // property-access override, not mixed access).
            List<Method> nonTransientGetters = new ArrayList<>();
            for (Method getter : accessGetters) {
                String getterName = getter.getName();
                String fieldName = null;
                if (getterName.startsWith("get") && getterName.length() > 3) {
                    fieldName = Character.toLowerCase(getterName.charAt(3))
                        + getterName.substring(4);
                } else if (getterName.startsWith("is")
                    && getterName.length() > 2) {
                    fieldName = Character.toLowerCase(getterName.charAt(2))
                        + getterName.substring(3);
                }
                if (fieldName != null) {
                    boolean hasTransientField = false;
                    for (Field f : allFields) {
                        if (f.getName().equals(fieldName)
                            && f.isAnnotationPresent(Transient.class)) {
                            hasTransientField = true;
                            break;
                        }
                    }
                    if (!hasTransientField) {
                        nonTransientGetters.add(getter);
                    }
                } else {
                    nonTransientGetters.add(getter);
                }
            }

            if (!uniqueAccessFields.isEmpty()
                && !nonTransientGetters.isEmpty()) {
                throw new UserException(_loc.get("access-mixed",
                    cls, toFieldNames(uniqueAccessFields),
                    toMethodNames(nonTransientGetters)));
            }

            // Exclude getters whose field is @Transient from access type
            // determination. These getters represent property-access overrides
            // on individual fields (mixed access), not a signal to change the
            // class-level access type. They will be discovered later via
            // getTransientFieldPropertyOverrides() in getPersistentMembers().
            accessGetters = nonTransientGetters;
        }
        // After the mixed block, if all accessFields were overlapping
        // with accessGetters (dual-annotated), accessGetters wins (PROPERTY).
        if (!accessGetters.isEmpty()) {
        	return AccessCode.PROPERTY;
        }
        if (!accessFields.isEmpty()) {
        	return AccessCode.FIELD;
        }
        // Fall back to AnnotatedFilter if no access-defining annotations
        fields = filter(fields, annotatedFilter);
        getters = filter(getters, annotatedFilter);
        getters = matchGetterAndSetter(getters, setters);
        if (!fields.isEmpty() && !getters.isEmpty()) {
        	throw new UserException(_loc.get("access-mixed",
        		cls, toFieldNames(fields), toMethodNames(getters)));
        }
        if (!fields.isEmpty()) {
        	return AccessCode.FIELD;
        }
        if (!getters.isEmpty()) {
        	return AccessCode.PROPERTY;
        }
        return AccessCode.UNKNOWN;
    }

    /**
     * Checks whether the given class has JPA annotations on both fields AND
     * getters, indicating mixed annotation placement.
     */
    private boolean hasMixedAnnotations(Class<?> cls, OpenJPAConfiguration conf) {
        Field[] allFields = cls.getDeclaredFields();
        Method[] methods = cls.getDeclaredMethods();
        List<Field> fields = filter(allFields, new TransientFilter(true));
        getterFilter.setIncludePrivate(
            conf.getCompatibilityInstance().getPrivatePersistentProperties());
        List<Method> getters = filter(methods, getterFilter);
        fields = filter(fields, annotatedFilter);
        getters = filter(getters, annotatedFilter);
        List<Method> setters = filter(methods, setterFilter);
        getters = matchGetterAndSetter(getters, setters);
        return !fields.isEmpty() && !getters.isEmpty();
    }

    private boolean hasFieldStrategyAnnotations(Class<?> cls) {
        for (Field f : cls.getDeclaredFields()) {
            if (accessTypeFilter.includes(f)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGetterStrategyAnnotations(Class<?> cls) {
        for (Method m : cls.getDeclaredMethods()) {
            if (accessTypeFilter.includes(m)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Explicit access type, if any, is generally detected by the parser. This
     * is only used for metadata of an embeddable type which is encountered
     * as a field during some other owning entity.
     *
     * @see ValueMetaData#addEmbeddedMetaData()
     */
    private int determineExplicitAccessType(Class<?> cls) {
        Access access = cls.getAnnotation(Access.class);
        return access == null ? AccessCode.UNKNOWN : ((access.value() ==
            AccessType.FIELD ? AccessCode.FIELD : AccessCode.PROPERTY) |
            AccessCode.EXPLICIT);
    }

    /**
     * Matches the given getters with the given setters. Removes the getters
     * that do not have a corresponding setter.
     */
    private List<Method> matchGetterAndSetter(List<Method> getters,
    		List<Method> setters) {
        Collection<Method> unmatched =  new ArrayList<>();

        for (Method getter : getters) {
            String getterName = getter.getName();
            Class<?> getterReturnType = getter.getReturnType();
            String expectedSetterName = "set" + getterName.substring(
                (isBooleanGetter(getter) ? "is" : "get").length());
            boolean matched = false;
            for (Method setter : setters) {
                Class<?> setterArgType = setter.getParameterTypes()[0];
                String actualSetterName = setter.getName();
                matched = actualSetterName.equals(expectedSetterName)
                    && setterArgType == getterReturnType;
                if (matched)
                    break;
            }
            if (!matched) {
                unmatched.add(getter);
            }

        }
        getters.removeAll(unmatched);
        return getters;
    }

    /**
     * Gets the fields that are possible candidate for being persisted. The
     * result depends on the current access style of the given class.
     */
    List<Field> getPersistentFields(ClassMetaData meta, boolean ignoreTransient) {
    	boolean explicit = meta.isExplicitAccess();
    	boolean unknown  = AccessCode.isUnknown(meta);
    	boolean isField  = AccessCode.isField(meta);

    	if (explicit || unknown || isField) {
    		Field[] fields = meta.getDescribedType().getDeclaredFields();

        	return filter(fields, fieldFilter,
        	    ignoreTransient ? null : nonTransientFilter,
        		unknown || isField  ? null : annotatedFilter,
        	    explicit ? (isField ? null : fieldAccessFilter) : null);
    	}
    	return Collections.EMPTY_LIST;
    }

    /**
     * Gets the methods that are possible candidate for being persisted. The
     * result depends on the current access style of the given class.
     */
    List<Method> getPersistentMethods(ClassMetaData meta, boolean ignoreTransient) {
    	boolean explicit = meta.isExplicitAccess();
    	boolean unknown  = AccessCode.isUnknown(meta.getAccessType());
    	boolean isProperty  = AccessCode.isProperty(meta.getAccessType());
    	if (explicit || unknown || isProperty) {
    		Method[] publicMethods = meta.getDescribedType().getDeclaredMethods();

            /*
             * OpenJPA 1.x permitted private accessor properties to be persistent.  This is
             * contrary to the JPA 1.0 specification, which states that persistent
             * properties must be public or protected. OpenJPA 2.0+ will adhere
             * to the specification by default, but provides a compatibility
             * option to provide pre-2.0 behavior.
             */
            getterFilter.setIncludePrivate(
                meta.getRepository().getConfiguration().getCompatibilityInstance().getPrivatePersistentProperties());

            List<Method> getters = filter(publicMethods, methodFilter,
                getterFilter,
                ignoreTransient ? null : nonTransientFilter,
        		unknown || isProperty ? null : annotatedFilter,
                explicit ? (isProperty ? null : propertyAccessFilter) : null);

            List<Method> setters = filter(publicMethods, setterFilter);
            return getters = matchGetterAndSetter(getters, setters);
    	}

    	return Collections.EMPTY_LIST;
    }

    /**
     * Gets the members that are backing members for attributes being persisted.
     * Unlike {@linkplain #getPersistentFields(ClassMetaData)} and
     * {@linkplain #getPersistentMethods(ClassMetaData)} which returns
     * <em>possible</em> candidates, the result of this method is definite.
     *
     * Side-effect of this method is if the given class metadata has
     * no access type set, this method will set it.
     */
    @Override
    public List<Member> getPersistentMembers(ClassMetaData meta, boolean ignoreTransient) {
    	List<Member> members = new ArrayList<>();
    	List<Field> fields   = getPersistentFields(meta, ignoreTransient);
    	List<Method> getters = getPersistentMethods(meta, ignoreTransient);

    	boolean isMixed = !fields.isEmpty() && !getters.isEmpty();
    	boolean isEmpty = fields.isEmpty() && getters.isEmpty();

    	boolean explicit    = meta.isExplicitAccess();
    	boolean unknown     = AccessCode.isUnknown(meta.getAccessType());

    	if (isEmpty) {
    		warn(meta, _loc.get("access-empty", meta));
    		return Collections.EMPTY_LIST;
    	}
    	if (explicit) {
    		if (isMixed) {
    			assertNoDuplicate(fields, getters);
                meta.setAccessType(AccessCode.MIXED | meta.getAccessType());
    			members.addAll(fields);
    			members.addAll(getters);
    		} else {
    			members.addAll(fields.isEmpty() ? getters : fields);
    		}
    	} else {
    		if (isMixed)
                error(meta, _loc.get("access-mixed", meta, fields, getters));
    		if (fields.isEmpty()) {
    			meta.setAccessType(AccessCode.PROPERTY);
    			members.addAll(getters);
    		} else {
    			meta.setAccessType(AccessCode.FIELD);
    			members.addAll(fields);
    		}
    	}
    	if (AccessCode.isField(meta.getAccessType())
    	    && !AccessCode.isMixed(meta.getAccessType())) {
    	    List<Method> transientOverrides =
    	        getTransientFieldPropertyOverrides(meta);
    	    if (!transientOverrides.isEmpty()) {
    	        members.addAll(transientOverrides);
    	        meta.setAccessType(AccessCode.MIXED
    	            | meta.getAccessType() | AccessCode.EXPLICIT);
    	    }
    	}

    	return members;
    }

    private List<Method> getTransientFieldPropertyOverrides(
        ClassMetaData meta) {
        List<Method> result = new ArrayList<>();
        Class<?> cls = meta.getDescribedType();
        Field[] allFields = cls.getDeclaredFields();
        Method[] methods = cls.getDeclaredMethods();

        getterFilter.setIncludePrivate(
            meta.getRepository().getConfiguration()
                .getCompatibilityInstance()
                .getPrivatePersistentProperties());

        List<Method> getters = filter(methods, getterFilter);
        List<Method> setters = filter(methods, setterFilter);
        getters = matchGetterAndSetter(getters, setters);

        AccessDefiningFilter adf = new AccessDefiningFilter();
        for (Method getter : getters) {
            if (!adf.includes(getter)) {
                continue;
            }
            String getterName = getter.getName();
            String fieldName = null;
            if (getterName.startsWith("get") && getterName.length() > 3) {
                fieldName = Character.toLowerCase(getterName.charAt(3))
                    + getterName.substring(4);
            } else if (getterName.startsWith("is")
                && getterName.length() > 2) {
                fieldName = Character.toLowerCase(getterName.charAt(2))
                    + getterName.substring(3);
            }
            if (fieldName != null) {
                for (Field f : allFields) {
                    if (f.getName().equals(fieldName)
                        && f.isAnnotationPresent(Transient.class)) {
                        result.add(getter);
                        break;
                    }
                }
            }
        }
        return result;
    }

    void assertNoDuplicate(List<Field> fields, List<Method> getters) {

    }

    void error(ClassMetaData meta, Localizer.Message message) {
    	Log log = meta.getRepository().getConfiguration()
    		.getLog(OpenJPAConfiguration.LOG_RUNTIME);
    	log.error(message.toString());
    	throw new UserException(message.toString());
    }

    void warn(ClassMetaData meta, Localizer.Message message) {
    	Log log = meta.getRepository().getConfiguration()
		.getLog(OpenJPAConfiguration.LOG_RUNTIME);
    	log.warn(message.toString());
    }

    void trace(ClassMetaData meta, Localizer.Message message) {
        Log log = meta.getRepository().getConfiguration()
        .getLog(OpenJPAConfiguration.LOG_RUNTIME);
        log.trace(message.toString());
    }

    @Override
    protected List<String> getFieldAccessNames(ClassMetaData meta) {
    	return toNames(getPersistentFields(meta, false));
    }

    @Override
    protected List<String> getPropertyAccessNames(ClassMetaData meta) {
    	return toNames(getPersistentMethods(meta, false));
    }

    protected boolean isDefaultPersistent(ClassMetaData meta, Member member,
        String name) {
        return isDefaultPersistent(meta, member, name, false);
    }

    @Override
    protected boolean isDefaultPersistent(ClassMetaData meta, Member member,
        String name, boolean ignoreTransient) {
        int mods = member.getModifiers();
        if (Modifier.isTransient(mods))
            return false;
        int access = meta.getAccessType();

        if (member instanceof Field) {
            // If mixed or unknown, default property access, keep explicit
            // field members
            if (AccessCode.isProperty(access)) {
                if (!isAnnotatedAccess(member, AccessType.FIELD))
                    return false;
            }
        }
        else if (member instanceof Method) {
            // If mixed or unknown, field default access, keep explicit property
            // members
            if (AccessCode.isField(access)) {
                if (!isAnnotatedAccess(member, AccessType.PROPERTY)) {
                    // JPA spec allows implicit property-access override:
                    // @Transient on field + annotation on getter.
                    // If this getter was discovered via
                    // getTransientFieldPropertyOverrides(), treat as persistent.
                    if (!hasTransientFieldForGetter(
                            meta.getDescribedType(), (Method) member)) {
                        return false;
                    }
                }
            }
            try {
                String setterName;
                if (member.getName().startsWith("is")) {
                    setterName = "set" + member.getName().substring(2);
                } else {
                    setterName = "set" + member.getName().substring(3);
                }
                // check for setters for methods
                Method setter = meta.getDescribedType().getDeclaredMethod(setterName, ((Method) member).getReturnType());
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

        PersistenceStrategy strat = getPersistenceStrategy(null, member, ignoreTransient);
        if (strat == null) {
            warn(meta, _loc.get("no-pers-strat", meta.getDescribedTypeString() + "." + name));
            return false;
        } else return strat != PersistenceStrategy.TRANSIENT;
    }

    private boolean isAnnotatedTransient(Member member) {
        return member instanceof AnnotatedElement
            && ((AnnotatedElement) member).isAnnotationPresent(Transient.class);
    }

    /**
     * Checks whether the given getter method has a corresponding field with
     * @Transient annotation. This indicates an implicit property-access
     * override per JPA spec (field marked @Transient, getter carries the
     * persistence annotations).
     */
    private boolean hasTransientFieldForGetter(Class<?> cls, Method getter) {
        String getterName = getter.getName();
        String fieldName = null;
        if (getterName.startsWith("get") && getterName.length() > 3) {
            fieldName = Character.toLowerCase(getterName.charAt(3))
                + getterName.substring(4);
        } else if (getterName.startsWith("is") && getterName.length() > 2) {
            fieldName = Character.toLowerCase(getterName.charAt(2))
                + getterName.substring(3);
        }
        if (fieldName != null) {
            try {
                Field f = cls.getDeclaredField(fieldName);
                return f.isAnnotationPresent(Transient.class);
            } catch (NoSuchFieldException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * May be used to determine if member is annotated with the specified
     * access type.
     * @param member class member
     * @param type expected access type
     * @return true if access is specified on member and that access
     *         type matches the expected type
     */
    private boolean isAnnotatedAccess(Member member, AccessType type) {
    	if (member == null)
    		return false;
        Access anno = ((AnnotatedElement) member).getAnnotation(Access.class);
        return anno != null && anno.value() == type;
    }

    private boolean isAnnotated(Member member) {
    	return member != null && member instanceof AnnotatedElement
    	    && annotatedFilter.includes((AnnotatedElement)member);
    }

    private static final AccessDefiningFilter _accessDefiningFilter =
        new AccessDefiningFilter();

    private boolean isAccessDefiningAnnotated(Member member) {
        return member != null && member instanceof AnnotatedElement
            && _accessDefiningFilter.includes((AnnotatedElement) member);
    }

    private boolean isNotTransient(Member member) {
        return member != null && member instanceof AnnotatedElement
            && nonTransientFilter.includes((AnnotatedElement)member);
    }

    /**
     * Gets either the instance field or the getter method depending upon the
     * access style of the given meta-data.
     */
    @Override
    public Member getMemberByProperty(ClassMetaData meta, String property,
    	int access, boolean applyDefaultRule) {
    	Class<?> cls = meta.getDescribedType();
        Field field = Reflection.findField(cls, property, false);
        Method getter = Reflection.findGetter(cls, property, false);
        Method setter = Reflection.findSetter(cls, property, false);
        int accessCode = AccessCode.isUnknown(access) ? meta.getAccessType() :
        	access;
        if (field == null && getter == null)
        	error(meta, _loc.get("access-no-property", cls, property));
    	if ((isNotTransient(getter) && isAccessDefiningAnnotated(getter)) &&
    	     isNotTransient(field) && isAccessDefiningAnnotated(field)
    	     && AccessCode.isUnknown(accessCode))
    		throw new IllegalStateException(_loc.get("access-duplicate",
    			field, getter).toString());

        if (AccessCode.isField(accessCode)) {
           if (isAnnotatedAccess(getter, AccessType.PROPERTY)) {
        	   meta.setAccessType(AccessCode.MIXED | meta.getAccessType());
               return getter;
           }
           return field == null ? getter : field;
        } else if (AccessCode.isProperty(accessCode)) {
            if (isAnnotatedAccess(field, AccessType.FIELD)) {
         	   meta.setAccessType(AccessCode.MIXED | meta.getAccessType());
               return field;
            }
            return getter == null ? field : getter;
        } else if (AccessCode.isUnknown(accessCode)) {
        	if (isAnnotated(field)) {
        		meta.setAccessType(AccessCode.FIELD);
        		return field;
        	} else if (isAnnotated(getter)) {
        		meta.setAccessType(AccessCode.PROPERTY);
        		return getter;
        	} else {
        		warn(meta, _loc.get("access-none", meta, property));
        		throw new IllegalStateException(
                    _loc.get("access-none", meta, property).toString());
        	}
        } else {
        	throw new InternalException(meta + " " +
        		AccessCode.toClassString(meta.getAccessType()));
        }
    }

    // ========================================================================
    //  Selection Filters select specific elements from a collection.
    //  Used to determine the persistent members of a given class.
    // ========================================================================

    /**
     * Inclusive element filtering predicate.
     *
     */
    private interface InclusiveFilter<T extends AnnotatedElement> {
        /**
         * Return true to include the given element.
         */
        boolean includes(T e);
    }

    /**
     * Filter the given collection with the conjunction of filters. The given
     * collection itself is not modified.
     */
    <T extends AnnotatedElement> List<T> filter(T[] array,
    	InclusiveFilter... filters) {
        List<T> result = new ArrayList<>();
        for (T e : array) {
            boolean include = true;
            for (InclusiveFilter f : filters) {
                if (f != null && !f.includes(e)) {
                    include = false;
                    break;
                }
            }
            if (include)
                result.add(e);
        }
        return result;
    }

    <T extends AnnotatedElement> List<T> filter(List<T> list,
        	InclusiveFilter... filters) {
        List<T> result = new ArrayList<>();
        for (T e : list) {
            boolean include = true;
            for (InclusiveFilter f : filters) {
                if (f != null && !f.includes(e)) {
                    include = false;
                    break;
                }
            }
            if (include)
                result.add(e);
        }
        return result;
    }

    /**
     * Selects getter method. A getter method name starts with 'get', returns a
     * non-void type and has no argument. Or starts with 'is', returns a boolean
     * and has no argument.
     *
     */
    static class GetterFilter implements InclusiveFilter<Method> {

        private boolean includePrivate;

        @Override
        public boolean includes(Method method) {
            return isGetter(method, isIncludePrivate());
        }

        public void setIncludePrivate(boolean includePrivate) {
            this.includePrivate = includePrivate;
        }

        public boolean isIncludePrivate() {
            return includePrivate;
        }
    }

    /**
     * Selects setter method. A setter method name starts with 'set', returns a
     * void and has single argument.
     *
     */
    static class SetterFilter implements InclusiveFilter<Method> {
        @Override
        public boolean includes(Method method) {
            return isSetter(method);
        }
        /**
         * Affirms if the given method matches the following signature
         * <code> public void setXXX(T t) </code>
         */
        public static boolean isSetter(Method method) {
        	String methodName = method.getName();
        	return startsWith(methodName, "set")
        	    && Character.isUpperCase(methodName.charAt(3))
        	    && method.getParameterTypes().length == 1
        	    && method.getReturnType() == void.class;
        }
    }

    /**
     * Selects elements which is annotated with @Access annotation and that
     * annotation has the given AccessType value.
     *
     */
        record AccessFilter(AccessType target) implements InclusiveFilter<AnnotatedElement> {

        @Override
            public boolean includes(AnnotatedElement obj) {
            Access access = obj.getAnnotation(Access.class);
            return access != null && access.value().equals(target);
            }
        }

    /**
     * Selects elements which is annotated with @Access annotation and that
     * annotation has the given AccessType value.
     *
     */
        record MemberFilter(Class<?> target) implements InclusiveFilter<AnnotatedElement> {

        @Override
            public boolean includes(AnnotatedElement obj) {
            int mods = ((Member) obj).getModifiers();

                if (obj.getClass() != target) {
                    return false;
                }
                if (Modifier.isStatic(mods) || Modifier.isTransient(mods)
                        || Modifier.isNative(mods)) {
                    return false;
                }
                // JPA 3.2: record component fields are final but persistent
                if (Modifier.isFinal(mods)) {
                    return ((Member) obj).getDeclaringClass().isRecord();
                }
                return true;
            }
        }

    /**
         * Selects non-transient elements.  Selectively will examine only the
         * transient field modifier.
         */
        record TransientFilter(boolean modifierOnly) implements InclusiveFilter<AnnotatedElement> {

        @Override
            public boolean includes(AnnotatedElement obj) {
                if (modifierOnly) {
                    return !Modifier.isTransient(((Member) obj).getModifiers());
                }
            return !obj.isAnnotationPresent(Transient.class) &&
                    !Modifier.isTransient(((Member) obj).getModifiers());
            }
        }

    /**
     * Selects all element annotated with <code>jakarta.persistence.*</code> or
     * <code>org.apache.openjpa.*</code> annotation except the annotations
     * marked to be ignored.
     */
    static class AnnotatedFilter implements InclusiveFilter<AnnotatedElement> {
        @Override
        public boolean includes(AnnotatedElement obj) {
            Annotation[] annos = obj.getAnnotations();
        	for (Annotation anno : annos) {
        		String name = anno.annotationType().getName();
                if ((name.startsWith("jakarta.persistence.")
                  || name.startsWith("org.apache.openjpa.persistence."))
                  && !_ignoredAnnos.contains(name))
                	return true;
        	}
        	return false;
        }
    }

    static class AccessDefiningFilter
        implements InclusiveFilter<AnnotatedElement> {
        @Override
        public boolean includes(AnnotatedElement obj) {
            for (Annotation anno : obj.getAnnotations()) {
                if (_accessDefiningAnnos.contains(anno.annotationType()))
                    return true;
            }
            return false;
        }
    }

    /**
     * Filter that includes only members annotated with access-type-determining
     * annotations: persistence strategy annotations (@Id, @Basic, @ManyToOne, etc.),
     * @Version, and @EmbeddedId. Supplementary annotations like @Column,
     * @JoinColumn, @Enumerated do NOT determine the access type per JPA spec.
     */
    static class AccessTypeFilter implements InclusiveFilter<AnnotatedElement> {
        @Override
        public boolean includes(AnnotatedElement obj) {
            for (Annotation anno : obj.getAnnotations()) {
                Class<?> type = anno.annotationType();
                if (_strats.containsKey(type)
                    || type == Id.class
                    || type == Version.class) {
                    return true;
                }
            }
            return false;
        }
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

    private Log getLog(ClassMetaData meta) {
        return meta.getRepository().getConfiguration()
            .getLog(OpenJPAConfiguration.LOG_METADATA);
    }

    String toFieldNames(List<Field> fields) {
    	return fields.toString();
    }

    String toMethodNames(List<Method> methods) {
    	return methods.toString();
    }

    @Override
    public boolean isAbstractMappingUniDirectional(OpenJPAConfiguration conf) {
        if (_isAbstractMappingUniDirectional == null)
            setAbstractMappingUniDirectional(conf);
        return _isAbstractMappingUniDirectional;
    }

    public void setAbstractMappingUniDirectional(OpenJPAConfiguration conf) {
        _isAbstractMappingUniDirectional = conf.getCompatibilityInstance().isAbstractMappingUniDirectional();
    }

    @Override
    public boolean isNonDefaultMappingAllowed(OpenJPAConfiguration conf) {
        if (_isNonDefaultMappingAllowed == null)
            setNonDefaultMappingAllowed(conf);
        return _isNonDefaultMappingAllowed;
    }

    public void setNonDefaultMappingAllowed(OpenJPAConfiguration conf) {
        _isNonDefaultMappingAllowed = conf.getCompatibilityInstance().
            isNonDefaultMappingAllowed();
    }

    @Override
    public Boolean isDefaultCascadePersistEnabled() {
        return _isCascadePersistPersistenceUnitDefaultEnabled;
    }

    @Override
    public void setDefaultCascadePersistEnabled(Boolean bool) {
        _isCascadePersistPersistenceUnitDefaultEnabled = bool;
    }

    @Override
    public String getDefaultSchema() {
        return _defaultSchema;
    }

    @Override
    public void setDefaultSchema(String schema) {
        _defaultSchema=schema;
    }
}
