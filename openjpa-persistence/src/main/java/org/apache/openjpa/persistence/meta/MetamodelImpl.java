package org.apache.openjpa.persistence.meta;

import static javax.persistence.metamodel.Type.PersistenceType.BASIC;
import static javax.persistence.metamodel.Type.PersistenceType.EMBEDDABLE;
import static javax.persistence.metamodel.Type.PersistenceType.ENTITY;
import static 
 javax.persistence.metamodel.Type.PersistenceType.MAPPED_SUPERCLASS;

import java.lang.reflect.Field;
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
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.Type;
import javax.persistence.metamodel.AbstractCollection.CollectionType;
import javax.persistence.metamodel.Type.PersistenceType;

import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
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
        ClassMetaData[] metas = repos.getMetaDatas();
        for (ClassMetaData meta : metas) {
            PersistenceType type = getPersistenceType(meta);
            Class<?> cls = meta.getDescribedType();
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
        return null;
    }
    
    /**
     * Populate the static fields of the canonical type.
     */
    public <X> void populate(Types.Managed<X> type) {
    	try {
    		Class<X> cls = type.getJavaType();
            Class<?> mcls = J2DoPrivHelper.getForNameAction(cls.getName()+"_", 
	    	    true, cls.getClassLoader()).run();
	    	Field[] fields = mcls.getFields();
	    	for (Field f : fields) {
	    		f.set(null, type.getMember(f.getName()));
	    	}
    	} catch (Exception e) {
    		
    	}
    }
}
