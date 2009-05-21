package org.apache.openjpa.persistence.meta;

import static javax.persistence.metamodel.Type.PersistenceType.BASIC;
import static javax.persistence.metamodel.Type.PersistenceType.EMBEDDABLE;
import static javax.persistence.metamodel.Type.PersistenceType.ENTITY;
import static 
 javax.persistence.metamodel.Type.PersistenceType.MAPPED_SUPERCLASS;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.Embeddable;
import javax.persistence.metamodel.Entity;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MappedSuperclass;
import javax.persistence.metamodel.Member;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.TypesafeMetamodel;
import javax.persistence.metamodel.AbstractCollection.CollectionType;
import javax.persistence.metamodel.Type.PersistenceType;

import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.util.InternalException;

/**
 * Adapts JPA Metamodel to OpenJPA meta-data repository.
 * 
 * @author Pinaki Poddar
 * 
 */
public class MetamodelImpl implements Metamodel {
    public final MetaDataRepository repos;
    Map<Class<?>, Entity<?>> _entities = new HashMap<Class<?>, Entity<?>>();
    Map<Class<?>, Embeddable<?>> _embeddables =
        new HashMap<Class<?>, Embeddable<?>>();
    Map<Class<?>, MappedSuperclass<?>> _mappedsupers =
        new HashMap<Class<?>, MappedSuperclass<?>>();
    Map<Class<?>, Type<?>> _basics = new HashMap<Class<?>, Type<?>>();

    private static Localizer _loc = Localizer.forPackage(MetamodelImpl.class);

    public MetamodelImpl(MetaDataRepository repos) {
        this.repos = repos;
        Collection<Class<?>> classes = repos.loadPersistentTypes(true, null);
        for (Class<?> cls : classes) {
        	ClassMetaData meta = repos.getMetaData(cls, null, true);
            PersistenceType type = getPersistenceType(meta);
            switch (type) {
            case ENTITY:
                find(cls, _entities, ENTITY);
                break;
            case EMBEDDABLE:
                find(cls, _embeddables, EMBEDDABLE);
                break;
            case MAPPED_SUPERCLASS:
                find(cls, _mappedsupers, MAPPED_SUPERCLASS);
                break;
            default:
            }
        }
    }

    public <X> Embeddable<X> embeddable(Class<X> clazz) {
        return (Embeddable<X>)find(clazz, _embeddables, EMBEDDABLE);
    }

    public <X> Entity<X> entity(Class<X> clazz) {
        return (Entity<X>) find(clazz, _entities, ENTITY);
    }

    public Set<Embeddable<?>> getEmbeddables() {
        return unmodifiableSet(_embeddables.values());
    }

    public Set<Entity<?>> getEntities() {
        return unmodifiableSet(_entities.values());
    }

    public Set<ManagedType<?>> getManagedTypes() {
        Set<ManagedType<?>> result = new HashSet<ManagedType<?>>();
        result.addAll(_entities.values());
        result.addAll(_embeddables.values());
        result.addAll(_mappedsupers.values());
        return result;
    }

    public <X> ManagedType<X> type(Class<X> clazz) {
        if (_entities.containsKey(clazz))
            return (Entity<X>) _entities.get(clazz);
        if (_embeddables.containsKey(clazz))
            return (Embeddable<X>) _embeddables.get(clazz);
        if (_mappedsupers.containsKey(clazz))
            return (MappedSuperclass<X>) _mappedsupers.get(clazz);
        throw new IllegalArgumentException(_loc.get("type-not-managed", clazz)
            .getMessage());
    }

    public <X> Type<X> getType(Class<X> cls) {
        try {
            return type(cls);
        } catch (IllegalArgumentException ex) {
            if (_basics.containsKey(cls))
                return (Type<X>)_basics.get(cls);
            Type<X> basic = new Types.Basic<X>(cls);
            _basics.put(cls, basic);
            return basic;
        }
    }

    public static PersistenceType getPersistenceType(ClassMetaData meta) {
        if (meta == null)
            return BASIC;
        if (meta.isEmbeddedOnly())
            return meta.isAbstract() ? MAPPED_SUPERCLASS : EMBEDDABLE;
        return ENTITY;
    }

    private <V extends ManagedType<?>> V find(Class<?> cls, 
        Map<Class<?>,V> container,  PersistenceType expected) {
        if (container.containsKey(cls))
            return container.get(cls);
        ClassMetaData meta = repos.getMetaData(cls, null, false);
        if (meta != null) {
            instantiate(cls, container, expected);
        }
        return container.get(cls);
    }

    private <X,V extends ManagedType<?>> void instantiate(Class<X> cls, 
        Map<Class<?>,V> container, PersistenceType expected) {
        ClassMetaData meta = repos.getMetaData(cls, null, true);
        PersistenceType actual = getPersistenceType(meta);
        if (actual != expected)
            throw new IllegalArgumentException(_loc.get("type-wrong-category",
                cls, actual, expected).getMessage());

        switch (actual) {
        case EMBEDDABLE:
            Types.Embeddable<X> embedded = new Types.Embeddable<X>(meta, this);
            _embeddables.put(cls, embedded);
            populate(embedded);
            break;
        case ENTITY:
        	Types.Entity<X> entity = new Types.Entity<X>(meta, this);
            _entities.put(cls, entity);
            populate(entity);
            break;
        case MAPPED_SUPERCLASS:
            Types.MappedSuper<X> mapped = new Types.MappedSuper<X>(meta, this);
            _mappedsupers.put(cls, mapped);
            populate(mapped);
            break;
        default:
            throw new InternalException(cls.getName());
        }
    }

    public <T> Set<T> unmodifiableSet(Collection<T> coll) {
        HashSet<T> result = new HashSet<T>();
        for (T t : coll)
            result.add(t);
        return result;
    }

    public static CollectionType getCollectionType(Class<?> cls) {
        if (Set.class.isAssignableFrom(cls))
            return CollectionType.SET;
        if (List.class.isAssignableFrom(cls))
            return CollectionType.LIST;
        if (Collection.class.isAssignableFrom(cls))
            return CollectionType.COLLECTION;
        if (Map.class.isAssignableFrom(cls))
            return CollectionType.MAP;
        if (cls.isArray())
            return CollectionType.LIST;
        return null;
    }
    
    /**
     * Populate the static fields of the canonical type.
     */
    public <X> void populate(Types.Managed<X> type) {
		Class<X> cls = type.getJavaType();
		Class<?> mcls = repos.getMetaModel(cls, true);
		if (mcls == null)
		    return;
        TypesafeMetamodel anno = mcls.getAnnotation(TypesafeMetamodel.class);
		if (anno == null)
            throw new IllegalArgumentException(_loc.get("meta-class-no-anno", 
               mcls.getName(), cls.getName(), TypesafeMetamodel.class.getName())
		       .getMessage());
		
        if (cls != anno.value()) {
            throw new IllegalStateException(_loc.get("meta-class-mismatch",
            mcls.getName(), cls.getName(), anno.value()).getMessage());
        }
        
    	Field[] mfields = mcls.getFields();
    	for (Field mf : mfields) {
            try {
                ParameterizedType mfType = getParameterziedType(mf);
    	        Member<? super X, ?> f = type.getMember(mf.getName());
    	        Class<?> fClass = f.getMemberJavaType();
    	       java.lang.reflect.Type[] args = mfType.getActualTypeArguments();
    	       if (args.length < 2)
    	           throw new IllegalStateException(
    	               _loc.get("meta-field-no-para", mf).getMessage());
    	       java.lang.reflect.Type ftype = args[1];
    	       if (fClass.isPrimitive() 
    	        || Collection.class.isAssignableFrom(fClass) 
    	        || Map.class.isAssignableFrom(fClass)) {
    	        ;
    	    } else if (ftype != args[1]) {
    	        throw new RuntimeException(_loc.get("meta-field-mismatch", 
    	            new Object[]{mf.getName(), mcls.getName(), 
    	                toTypeName(mfType), toTypeName(ftype)}).getMessage());
    	    }
            mf.set(null, f);
	} catch (Exception e) {
	    e.printStackTrace();
		throw new RuntimeException(mf.toString());
	}
        }
    }
    
    /**
     * Gets the parameterized type of the given field after validating. 
     */
    ParameterizedType getParameterziedType(Field mf) {
        java.lang.reflect.Type t = mf.getGenericType();
        if (t instanceof ParameterizedType == false) {
            throw new IllegalStateException(_loc.get("meta-field-not-param", 
            mf.getDeclaringClass(), mf.getName(), toTypeName(t)).getMessage());
        }
        ParameterizedType mfType = (ParameterizedType)t;
        java.lang.reflect.Type[] args = mfType.getActualTypeArguments();
        if (args.length < 2) {
            throw new IllegalStateException(_loc.get("meta-field-less-param", 
            mf.getDeclaringClass(), mf.getName(), toTypeName(t)).getMessage());
        }
        
        return mfType;
    }
    
    /**
     * Pretty prints a Type. 
     */
    String toTypeName(java.lang.reflect.Type type) {
        if (type instanceof GenericArrayType) {
            return toTypeName(((GenericArrayType)type).
                getGenericComponentType())+"[]";
        }
        if (type instanceof ParameterizedType == false) {
            Class<?> cls = (Class<?>)type;
            return cls.getName();
        }
        ParameterizedType pType = (ParameterizedType)type;
        java.lang.reflect.Type[] args = pType.getActualTypeArguments();
        StringBuffer tmp = new StringBuffer(pType.getRawType().toString());
        for (int i = 0; i < args.length; i++) {
            tmp.append((i == 0) ? "<" : ",");
            tmp.append(toTypeName(args[i]));
            tmp.append((i == args.length-1) ? ">" : "");
        }
        return tmp.toString();
    }
    
    /**
     * Validates the given field of the meta class matches the given 
     * FieldMetaData and 
     * @param <X>
     * @param <Y>
     * @param mField
     * @param member
     */
    void validate(Field metaField, FieldMetaData fmd) {
        
    }
    
    <X,Y> void validate(Field mField, Member<X, Y> member) {
        Class<?> fType = member.getMemberJavaType();
        if (!ParameterizedType.class.isInstance(mField.getGenericType())) {
            throw new IllegalArgumentException(_loc.get("meta-bad-field", 
                mField).getMessage());
        }
        ParameterizedType mfType = (ParameterizedType)mField.getGenericType();
        java.lang.reflect.Type[] args = mfType.getActualTypeArguments();
        java.lang.reflect.Type owner = args[0];
        if (member.getDeclaringType().getJavaType() != owner)
            throw new IllegalArgumentException(_loc.get("meta-bad-field-owner", 
                    mField, owner).getMessage());
        java.lang.reflect.Type elementType = args[1];
        if (fType.isPrimitive())
            return;
    }    
}
