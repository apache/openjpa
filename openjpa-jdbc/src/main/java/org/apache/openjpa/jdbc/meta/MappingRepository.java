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
package org.apache.openjpa.jdbc.meta;

import java.lang.reflect.Modifier;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.strats.BlobValueHandler;
import org.apache.openjpa.jdbc.meta.strats.ByteArrayValueHandler;
import org.apache.openjpa.jdbc.meta.strats.CharArrayStreamValueHandler;
import org.apache.openjpa.jdbc.meta.strats.CharArrayValueHandler;
import org.apache.openjpa.jdbc.meta.strats.ClassNameDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.ClobValueHandler;
import org.apache.openjpa.jdbc.meta.strats.EmbedFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.EmbeddedClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.FlatClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.FullClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.HandlerFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.ImmutableValueHandler;
import org.apache.openjpa.jdbc.meta.strats.MaxEmbeddedBlobFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.MaxEmbeddedByteArrayFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.MaxEmbeddedCharArrayFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.MaxEmbeddedClobFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.NoneClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.NoneDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.NoneFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.NoneVersionStrategy;
import org.apache.openjpa.jdbc.meta.strats.NumberVersionStrategy;
import org.apache.openjpa.jdbc.meta.strats.ObjectIdClassStrategy;
import org.apache.openjpa.jdbc.meta.strats.ObjectIdValueHandler;
import org.apache.openjpa.jdbc.meta.strats.PrimitiveFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.RelationCollectionInverseKeyFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.RelationCollectionTableFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.RelationFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.RelationMapInverseKeyFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.RelationMapTableFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.StateComparisonVersionStrategy;
import org.apache.openjpa.jdbc.meta.strats.StringFieldStrategy;
import org.apache.openjpa.jdbc.meta.strats.SubclassJoinDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.SuperclassDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.SuperclassVersionStrategy;
import org.apache.openjpa.jdbc.meta.strats.TimestampVersionStrategy;
import org.apache.openjpa.jdbc.meta.strats.UntypedPCValueHandler;
import org.apache.openjpa.jdbc.meta.strats.ValueMapDiscriminatorStrategy;
import org.apache.openjpa.jdbc.meta.strats.VerticalClassStrategy;
import org.apache.openjpa.jdbc.schema.Column;
import org.apache.openjpa.jdbc.schema.SchemaGroup;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.jdbc.sql.JoinSyntaxes;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.Order;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.meta.ValueMetaData;
import org.apache.openjpa.util.MetaDataException;

/**
 * Repository of object/relational mapping information.
 *
 * @author Abe White
 */
public class MappingRepository
    extends MetaDataRepository {

    private static final Localizer _loc = Localizer.forPackage
        (MappingRepository.class);
    private static final Map _handlers = new HashMap();

    static {
        // register default value handlers
        _handlers.put("java.lang.Enum",
            "org.apache.openjpa.jdbc.meta.strats.EnumValueHandler");
    }

    private DBDictionary _dict = null;
    private MappingDefaults _defaults = null;
    private Map _results = new HashMap(); // object->queryresultmapping
    private SchemaGroup _schema = null;
    private StrategyInstaller _installer = null;

    /**
     * Default constructor.  Configure via 
     * {@link org.apache.openjpa.lib.conf.Configurable}.
     */
    public MappingRepository() {
        setValidate(VALIDATE_MAPPING, true);
    }

    /**
     * Convenient access to dictionary for mappings.
     */
    public DBDictionary getDBDictionary() {
        return _dict;
    }

    /**
     * Mapping defaults.
     */
    public MappingDefaults getMappingDefaults() {
        return _defaults;
    }

    /**
     * Mapping default.
     */
    public void setMappingDefaults(MappingDefaults defaults) {
        _defaults = defaults;
    }

    /**
     * Representation of the database schema.
     */
    public synchronized SchemaGroup getSchemaGroup() {
        if (_schema == null)
            _schema = ((JDBCConfiguration) getConfiguration()).
                getSchemaFactoryInstance().readSchema();
        return _schema;
    }

    /**
     * Representation of the database schema.
     */
    public synchronized void setSchemaGroup(SchemaGroup schema) {
        _schema = schema;
    }

    /**
     * Installs mapping strategies on components.
     */
    public synchronized StrategyInstaller getStrategyInstaller() {
        if (_installer == null)
            _installer = new RuntimeStrategyInstaller(this);
        return _installer;
    }

    /**
     * Installs mapping strategies on components.
     */
    public synchronized void setStrategyInstaller(StrategyInstaller installer) {
        _installer = installer;
    }

    /**
     * Return the query result mapping for the given name.
     */
    public synchronized QueryResultMapping getQueryResultMapping(Class cls,
        String name, ClassLoader envLoader, boolean mustExist) {
        QueryResultMapping res = getQueryResultMappingInternal(cls, name,
            envLoader);
        if (res == null && mustExist)
            throw new MetaDataException(_loc.get("no-query-res", cls, name));
        return res;
    }

    /**
     * Returned the query result mapping with the given name.
     */
    private QueryResultMapping getQueryResultMappingInternal(Class cls,
        String name, ClassLoader envLoader) {
        if (name == null)
            return null;

        // check cache
        Object key = getQueryResultKey(cls, name);
        QueryResultMapping res = (QueryResultMapping) _results.get(key);
        if (res != null)
            return res;

        // get metadata for class, which will find results in metadata file
        if (cls != null && getMetaData(cls, envLoader, false) != null) {
            res = (QueryResultMapping) _results.get(key);
            if (res != null)
                return res;
        }
        if ((getSourceMode() & MODE_QUERY) == 0)
            return null;

        // not in cache; load
        getMetaDataFactory().load(cls, MODE_QUERY, envLoader);
        return (QueryResultMapping) _results.get(key);
    }

    /**
     * Return all cached query result mappings.
     */
    public synchronized QueryResultMapping[] getQueryResultMappings() {
        Collection values = _results.values();
        return (QueryResultMapping[]) values.toArray
            (new QueryResultMapping[values.size()]);
    }

    /**
     * Return the cached query result mapping with the given name, or null if
     * none.
     */
    public synchronized QueryResultMapping getCachedQueryResultMapping
        (Class cls, String name) {
        return (QueryResultMapping) _results.get(getQueryResultKey(cls, name));
    }

    /**
     * Add a query result mapping.
     */
    public synchronized QueryResultMapping addQueryResultMapping(Class cls,
        String name) {
        QueryResultMapping res = new QueryResultMapping(name, this);
        res.setDefiningType(cls);
        _results.put(getQueryResultKey(res), res);
        return res;
    }

    /**
     * Remove a query result mapping.
     */
    public synchronized boolean removeQueryResultMapping
        (QueryResultMapping res) {
        return _results.remove(getQueryResultKey(res)) != null;
    }

    /**
     * Remove a query result mapping.
     */
    public synchronized boolean removeQueryResultMapping(Class cls,
        String name) {
        if (name == null)
            return false;
        return _results.remove(getQueryResultKey(cls, name)) != null;
    }

    /**
     * Return a unique key for the given mapping.
     */
    private static Object getQueryResultKey(QueryResultMapping res) {
        if (res == null)
            return null;
        return getQueryResultKey(res.getDefiningType(), res.getName());
    }

    /**
     * Return a unique key for the given class / name. The class argument
     * can be null.
     */
    private static Object getQueryResultKey(Class cls, String name) {
        return getQueryKey(cls, name);
    }

    public ClassMapping getMapping(Class cls, ClassLoader envLoader,
        boolean mustExist) {
        return (ClassMapping) super.getMetaData(cls, envLoader, mustExist);
    }

    public ClassMapping[] getMappings() {
        return (ClassMapping[]) super.getMetaDatas();
    }

    public ClassMapping getMapping(Object oid, ClassLoader envLoader,
        boolean mustExist) {
        return (ClassMapping) super.getMetaData(oid, envLoader, mustExist);
    }

    public ClassMapping[] getImplementorMappings(Class cls,
        ClassLoader envLoader, boolean mustExist) {
        return (ClassMapping[]) super.getImplementorMetaDatas(cls, envLoader,
            mustExist);
    }

    public synchronized void clear() {
        super.clear();
        _schema = null;
        _results.clear();
    }

    protected void prepareMapping(ClassMetaData meta) {
        // make sure superclass resolved first; resolving superclass may have
        // resolved this mapping
        ClassMapping mapping = (ClassMapping) meta;
        ClassMapping sup = mapping.getPCSuperclassMapping();
        if (sup != null && (mapping.getResolve() & MODE_MAPPING) != 0)
            return;

        // define superclass fields after mapping class, so we can tell whether
        // the class is mapped and needs to redefine abstract superclass fields
        getStrategyInstaller().installStrategy(mapping);
        mapping.defineSuperclassFields(mapping.getJoinablePCSuperclassMapping()
            == null);

        // resolve everything that doesn't involve relations to allow relation
        // mappings to use the others as joinables
        mapping.resolveNonRelationMappings();
    }

    protected ClassMetaData newClassMetaData(Class type) {
        return new ClassMapping(type, this);
    }

    protected ClassMetaData[] newClassMetaDataArray(int length) {
        return new ClassMapping[length];
    }

    protected FieldMetaData newFieldMetaData(String name, Class type,
        ClassMetaData owner) {
        return new FieldMapping(name, type, (ClassMapping) owner);
    }

    protected FieldMetaData[] newFieldMetaDataArray(int length) {
        return new FieldMapping[length];
    }

    protected ClassMetaData newEmbeddedClassMetaData(ValueMetaData owner) {
        return new ClassMapping(owner);
    }

    protected ValueMetaData newValueMetaData(FieldMetaData owner) {
        return new ValueMappingImpl((FieldMapping) owner);
    }

    protected SequenceMetaData newSequenceMetaData(String name) {
        return new SequenceMapping(name, this);
    }

    protected Order newValueOrder(FieldMetaData owner, boolean asc) {
        return new JDBCValueOrder((FieldMapping) owner, asc);
    }

    protected Order newRelatedFieldOrder(FieldMetaData owner,
        FieldMetaData rel, boolean asc) {
        return new JDBCRelatedFieldOrder((FieldMapping) owner,
            (FieldMapping) rel, asc);
    }

    protected Order[] newOrderArray(int size) {
        return new JDBCOrder[size];
    }

    /**
     * Create version metadata for the given class.
     */
    protected Version newVersion(ClassMapping cls) {
        return new Version(cls);
    }

    /**
     * Create discriminator metadata for the given class.
     */
    protected Discriminator newDiscriminator(ClassMapping cls) {
        return new Discriminator(cls);
    }

    /**
     * Create raw mapping info for the given instance.
     */
    protected ClassMappingInfo newMappingInfo(ClassMapping cls) {
        ClassMappingInfo info = new ClassMappingInfo();
        info.setClassName(cls.getDescribedType().getName());
        return info;
    }

    /**
     * Create raw mapping info for the given instance.
     */
    protected FieldMappingInfo newMappingInfo(FieldMapping fm) {
        return new FieldMappingInfo();
    }

    /**
     * Create raw mapping info for the given instance.
     */
    protected ValueMappingInfo newMappingInfo(ValueMapping vm) {
        return new ValueMappingInfo();
    }

    /**
     * Create raw mapping info for the given instance.
     */
    protected VersionMappingInfo newMappingInfo(Version version) {
        return new VersionMappingInfo();
    }

    /**
     * Create raw mapping info for the given instance.
     */
    protected DiscriminatorMappingInfo newMappingInfo(Discriminator disc) {
        return new DiscriminatorMappingInfo();
    }

    /**
     * Instantiate the given class' named strategy, or return null if no
     * named strategy.
     */
    protected ClassStrategy namedStrategy(ClassMapping cls) {
        String name = cls.getMappingInfo().getStrategy();
        if (name == null)
            return null;
        return instantiateClassStrategy(name, cls);
    }

    /**
     * Return the strategy for the given name.
     */
    protected ClassStrategy instantiateClassStrategy(String name,
        ClassMapping cls) {
        if (NoneClassStrategy.ALIAS.equals(name))
            return NoneClassStrategy.getInstance();

        String props = Configurations.getProperties(name);
        name = Configurations.getClassName(name);
        Class strat = null;

        // base and vertical strategies use same alias; differentiate on join
        if (FullClassStrategy.ALIAS.equals(name))
            strat = FullClassStrategy.class;
        else if (FlatClassStrategy.ALIAS.equals(name))
            strat = FlatClassStrategy.class;
        else if (VerticalClassStrategy.ALIAS.equals(name))
            strat = VerticalClassStrategy.class;
        try {
            if (strat == null)
                strat = JavaTypes.classForName(name, cls,
                    ClassStrategy.class.getClassLoader());
            ClassStrategy strategy = (ClassStrategy) strat.newInstance();
            Configurations.configureInstance(strategy, getConfiguration(),
                props);
            return strategy;
        } catch (Exception e) {
            throw new MetaDataException(_loc.get("bad-cls-strategy",
                cls, name), e);
        }
    }

    /**
     * Instantiate the given field's named strategy, or return null if no
     * named strategy.
     */
    protected FieldStrategy namedStrategy(FieldMapping field,
        boolean installHandlers) {
        String name = field.getMappingInfo().getStrategy();
        if (name == null)
            return null;

        if (NoneFieldStrategy.ALIAS.equals(name))
            return NoneFieldStrategy.getInstance();

        String props = Configurations.getProperties(name);
        name = Configurations.getClassName(name);
        try {
            Class c = JavaTypes.classForName(name, field,
                FieldStrategy.class.getClassLoader());
            if (FieldStrategy.class.isAssignableFrom(c)) {
                FieldStrategy strat = (FieldStrategy) c.newInstance();
                Configurations.configureInstance(strat, getConfiguration(),
                    props);
                return strat;
            }

            // must be named handler
            if (installHandlers) {
                ValueHandler vh = (ValueHandler) c.newInstance();
                Configurations.configureInstance(vh, getConfiguration(),
                    props);
                field.setHandler(vh);
            }
            return new HandlerFieldStrategy();
        } catch (Exception e) {
            throw new MetaDataException(_loc.get("bad-field-strategy",
                field, name), e);
        }
    }

    /**
     * Instantiate the given discriminator's named strategy, or return null
     * if no named strategy.
     */
    protected DiscriminatorStrategy namedStrategy(Discriminator discrim) {
        String name = discrim.getMappingInfo().getStrategy();
        if (name == null)
            return null;

        // if there is a named strategy present, discard it if it matches
        // the base strategy, so that we won't create an independent instance
        ClassMapping cls = discrim.getClassMapping();
        while (cls.getJoinablePCSuperclassMapping() != null)
            cls = cls.getJoinablePCSuperclassMapping();
        Discriminator base = cls.getDiscriminator();
        if (base != discrim && base.getStrategy() != null
            && name.equals(base.getStrategy().getAlias()))
            return null;

        return instantiateDiscriminatorStrategy(name, discrim);
    }

    /**
     * Instantiate the given discriminator strategy.
     */
    protected DiscriminatorStrategy instantiateDiscriminatorStrategy
        (String name, Discriminator discrim) {
        if (NoneDiscriminatorStrategy.ALIAS.equals(name))
            return NoneDiscriminatorStrategy.getInstance();

        String props = Configurations.getProperties(name);
        name = Configurations.getClassName(name);
        Class strat = null;

        if (ClassNameDiscriminatorStrategy.ALIAS.equals(name))
            strat = ClassNameDiscriminatorStrategy.class;
        else if (ValueMapDiscriminatorStrategy.ALIAS.equals(name))
            strat = ValueMapDiscriminatorStrategy.class;
        else if (SubclassJoinDiscriminatorStrategy.ALIAS.equals(name))
            strat = SubclassJoinDiscriminatorStrategy.class;

        try {
            if (strat == null)
                strat = JavaTypes.classForName(name,
                    discrim.getClassMapping(),
                    DiscriminatorStrategy.class.getClassLoader());
            DiscriminatorStrategy strategy = (DiscriminatorStrategy)
                strat.newInstance();
            Configurations.configureInstance(strategy, getConfiguration(),
                props);
            return strategy;
        } catch (Exception e) {
            throw new MetaDataException(_loc.get("bad-discrim-strategy",
                discrim.getClassMapping(), name), e);
        }
    }

    /**
     * Instantiate the given version's named strategy, or return null
     * if no named strategy.
     */
    protected VersionStrategy namedStrategy(Version version) {
        String name = version.getMappingInfo().getStrategy();
        if (name == null)
            return null;

        // if there is a named strategy present, discard it if it matches
        // the base strategy, so that we won't create an independent instance
        ClassMapping cls = version.getClassMapping();
        while (cls.getJoinablePCSuperclassMapping() != null)
            cls = cls.getJoinablePCSuperclassMapping();
        Version base = cls.getVersion();
        if (base != version && base.getStrategy() != null
            && name.equals(base.getStrategy().getAlias()))
            return null;

        return instantiateVersionStrategy(name, version);
    }

    /**
     * Instantiate the given version strategy.
     */
    protected VersionStrategy instantiateVersionStrategy(String name,
        Version version) {
        if (NoneVersionStrategy.ALIAS.equals(name))
            return NoneVersionStrategy.getInstance();

        String props = Configurations.getProperties(name);
        name = Configurations.getClassName(name);
        Class strat = null;

        if (NumberVersionStrategy.ALIAS.equals(name))
            strat = NumberVersionStrategy.class;
        else if (TimestampVersionStrategy.ALIAS.equals(name))
            strat = TimestampVersionStrategy.class;
        else if (StateComparisonVersionStrategy.ALIAS.equals(name))
            strat = StateComparisonVersionStrategy.class;

        try {
            if (strat == null)
                strat = JavaTypes.classForName(name,
                    version.getClassMapping(),
                    VersionStrategy.class.getClassLoader());
        } catch (Exception e) {
            throw new MetaDataException(_loc.get("bad-version-strategy",
                version.getClassMapping(), name), e);
        }

        return instantiateVersionStrategy(strat, version, props);
    }

    /**
     * Instantiate the given version strategy.
     */
    protected VersionStrategy instantiateVersionStrategy(Class strat,
        Version version, String props) {
        try {
            VersionStrategy strategy = (VersionStrategy) strat.newInstance();
            Configurations.configureInstance(strategy, getConfiguration(),
                props);
            return strategy;
        } catch (Exception e) {
            throw new MetaDataException(_loc.get("bad-version-strategy",
                version.getClassMapping(), strat + ""), e);
        }
    }

    /**
     * Determine the default strategy to use for the given class. Does
     * not take into account the current strategy, if any.
     */
    protected ClassStrategy defaultStrategy(ClassMapping cls) {
        return defaultStrategy(cls, getStrategyInstaller().isAdapting());
    }

    /**
     * Determine the default strategy to use for the given class. Does
     * not take into account the current strategy, if any.
     */
    protected ClassStrategy defaultStrategy(ClassMapping cls,
        boolean adapting) {
        ValueMapping embed = cls.getEmbeddingMapping();
        if (embed != null) {
            // superclass of embedded class isn't mapped
            if (embed.getType() != cls.getDescribedType()
                || embed.getFieldMapping().getStrategy()
                == NoneFieldStrategy.getInstance())
                return NoneClassStrategy.getInstance();
            if (embed.getTypeCode() == JavaTypes.OID)
                return new ObjectIdClassStrategy();
            return new EmbeddedClassStrategy();
        }
        if (cls.isEmbeddedOnly())
            return NoneClassStrategy.getInstance();

        Object strat = _defaults.getStrategy(cls, adapting);
        if (strat instanceof String)
            return instantiateClassStrategy((String) strat, cls);
        if (strat != null)
            return (ClassStrategy) strat;

        ClassMapping sup = cls.getMappedPCSuperclassMapping();
        if (sup == null)
            return new FullClassStrategy();

        while (sup.getMappedPCSuperclassMapping() != null)
            sup = sup.getMappedPCSuperclassMapping();
        String subStrat = sup.getMappingInfo().getHierarchyStrategy();
        if (subStrat != null)
            return instantiateClassStrategy(subStrat, cls);

        return new FlatClassStrategy();
    }

    /**
     * Determine the default strategy to use for the given field. Does
     * not take into account the named or current strategy, if any. If a
     * non-null strategy is returned, this method may as a side effect install
     * value handlers on the field's value mappings.
     */
    protected FieldStrategy defaultStrategy(FieldMapping field,
        boolean installHandlers) {
        return defaultStrategy(field, installHandlers,
            getStrategyInstaller().isAdapting());
    }

    /**
     * Determine the default strategy to use for the given field. Does
     * not take into account the named or current strategy, if any. If a
     * non-null strategy is returned, this method may as a side effect install
     * value handlers on the field's value mappings.
     */
    protected FieldStrategy defaultStrategy(FieldMapping field,
        boolean installHandlers, boolean adapting) {
        // not persistent?
        if (field.getManagement() != field.MANAGE_PERSISTENT
            || field.isVersion())
            return NoneFieldStrategy.getInstance();
        if (field.getDefiningMapping().getStrategy() ==
            NoneClassStrategy.getInstance())
            return NoneFieldStrategy.getInstance();

        // check for named handler first
        ValueHandler handler = namedHandler(field);
        if (handler != null) {
            if (installHandlers)
                field.setHandler(handler);
            return new HandlerFieldStrategy();
        }

        if (field.isSerialized()) {
            if (_dict.maxEmbeddedBlobSize != -1)
                return new MaxEmbeddedBlobFieldStrategy();
        } else {
            // check for mapped strategy
            Object strat = mappedStrategy(field, field.getType(), adapting);
            if (strat instanceof FieldStrategy)
                return (FieldStrategy) strat;
            if (strat != null) {
                if (installHandlers)
                    field.setHandler((ValueHandler) strat);
                return new HandlerFieldStrategy();
            }
        }

        // check for known field strategies
        if (!field.isSerialized() && (field.getType() == byte[].class
            || field.getType() == Byte[].class)) {
            if (_dict.maxEmbeddedBlobSize != -1)
                return new MaxEmbeddedByteArrayFieldStrategy();
        } else if (!field.isSerialized()
            && (field.getType() == char[].class
            || field.getType() == Character[].class)) {
            if (_dict.maxEmbeddedClobSize != -1 && isClob(field, false))
                return new MaxEmbeddedCharArrayFieldStrategy();
        } else if (!field.isSerialized()) {
            FieldStrategy strat = defaultTypeStrategy(field, installHandlers,
                adapting);
            if (strat != null)
                return strat;
        }

        // check for default handler
        handler = defaultHandler(field, adapting);
        if (handler != null) {
            if (installHandlers)
                field.setHandler(handler);
            return new HandlerFieldStrategy();
        }

        // default to blob
        if (installHandlers) {
            if (getLog().isWarnEnabled())
                getLog().warn(_loc.get("no-field-strategy", field));
            field.setSerialized(true);
        }
        if (_dict.maxEmbeddedBlobSize == -1) {
            if (installHandlers)
                field.setHandler(BlobValueHandler.getInstance());
            return new HandlerFieldStrategy();
        }
        return new MaxEmbeddedBlobFieldStrategy();
    }

    /**
     * Return the built-in strategy for the field's type, or null if none.
     */
    protected FieldStrategy defaultTypeStrategy(FieldMapping field,
        boolean installHandlers, boolean adapting) {
        switch (field.getTypeCode()) {
            case JavaTypes.BOOLEAN:
            case JavaTypes.BYTE:
            case JavaTypes.CHAR:
            case JavaTypes.DOUBLE:
            case JavaTypes.FLOAT:
            case JavaTypes.INT:
            case JavaTypes.LONG:
            case JavaTypes.SHORT:
                return new PrimitiveFieldStrategy();
            case JavaTypes.STRING:
                if (!isClob(field, false))
                    return new StringFieldStrategy();
                if (_dict.maxEmbeddedClobSize != -1)
                    return new MaxEmbeddedClobFieldStrategy();
                break;
            case JavaTypes.PC:
                if (field.isEmbeddedPC())
                    return new EmbedFieldStrategy();
                if (field.getTypeMapping().isMapped()
                    || !useUntypedPCHandler(field))
                    return new RelationFieldStrategy();
                break;
            case JavaTypes.ARRAY:
            case JavaTypes.COLLECTION:
                ValueMapping elem = field.getElementMapping();
                ValueHandler ehandler = namedHandler(elem);
                if (ehandler == null)
                    ehandler = defaultHandler(elem);
                if (ehandler != null)
                    return handlerCollectionStrategy(field, ehandler, 
                        installHandlers);
                if (elem.getTypeCode() == JavaTypes.PC
                    && !elem.isSerialized() && !elem.isEmbeddedPC()) {
                    if (useInverseKeyMapping(field))
                        return new RelationCollectionInverseKeyFieldStrategy();
                    return new RelationCollectionTableFieldStrategy();
                }
                break;
            case JavaTypes.MAP:
                ValueMapping key = field.getKeyMapping();
                ValueHandler khandler = namedHandler(key);
                if (khandler == null)
                    khandler = defaultHandler(key);
                ValueMapping val = field.getElementMapping();
                ValueHandler vhandler = namedHandler(val);
                if (vhandler == null)
                    vhandler = defaultHandler(val);
                boolean krel = khandler == null 
                    && key.getTypeCode() == JavaTypes.PC
                    && !key.isSerialized() && !key.isEmbeddedPC();
                boolean vrel = vhandler == null 
                    && val.getTypeCode() == JavaTypes.PC
                    && !val.isSerialized() && !val.isEmbeddedPC();
                if (!krel && vrel && key.getValueMappedBy() != null) {
                    if (useInverseKeyMapping(field))
                        return new RelationMapInverseKeyFieldStrategy();
                    return new RelationMapTableFieldStrategy();
                }
                if (!krel && khandler == null)
                    break;
                if (!vrel && vhandler == null)
                    break;
                return handlerMapStrategy(field, khandler, vhandler, krel,
                    vrel, installHandlers);
        }
        return null;
    }

    /**
     * Return the collection strategy for the given element handler, or null
     * if none.
     */
    protected FieldStrategy handlerCollectionStrategy(FieldMapping field, 
        ValueHandler ehandler, boolean installHandlers) {
        return null;
    }

    /**
     * Return the map strategy for the given key and value handlers / relations,
     * or null if none.
     */
    protected FieldStrategy handlerMapStrategy(FieldMapping field, 
        ValueHandler khandler, ValueHandler vhandler, boolean krel, 
        boolean vrel,  boolean installHandlers) {
        return null;
    }

    /**
     * Use hints in mapping data to figure out whether the given relation
     * field should use an inverse foreign key or an association table mapping.
     */
    private boolean useInverseKeyMapping(FieldMapping field) {
        FieldMapping mapped = field.getMappedByMapping();
        if (mapped != null) {
            if (mapped.getTypeCode() == JavaTypes.PC)
                return true;
            if (mapped.getElement().getTypeCode() == JavaTypes.PC)
                return false;
            throw new MetaDataException(_loc.get("bad-mapped-by", field,
                mapped));
        }

        // without a mapped-by, we have to look for clues as to the mapping.
        // we assume that anything with element foreign key columns but no join
        // columns or table uses an inverse foreign key, and anything else uses
        // an association table
        FieldMappingInfo info = field.getMappingInfo();
        ValueMapping elem = field.getElementMapping();
        return info.getTableName() == null && info.getColumns().isEmpty()
            && !elem.getValueInfo().getColumns().isEmpty();
    }

    /**
     * Check the given value against mapped strategies.
     */
    private Object mappedStrategy(ValueMapping val, Class type,
        boolean adapting) {
        if (type == null || type == Object.class)
            return null;

        Object strat = _defaults.getStrategy(val, type, adapting);
        if (strat == null)
            strat = _handlers.get(type.getName());

        // recurse on superclass so that, for example, a registered handler
        // for java.lang.Enum will work on all enums
        if (strat == null)
            return mappedStrategy(val, type.getSuperclass(), adapting);
        if (!(strat instanceof String))
            return strat;

        String name = (String) strat;
        if (NoneFieldStrategy.ALIAS.equals(name))
            return NoneFieldStrategy.getInstance();

        String props = Configurations.getProperties(name);
        name = Configurations.getClassName(name);
        try {
            Object o = JavaTypes.classForName(name, val,
                FieldStrategy.class.getClassLoader()).newInstance();
            Configurations.configureInstance(o, getConfiguration(), props);
            return o;
        } catch (Exception e) {
            throw new MetaDataException(_loc.get("bad-mapped-strategy",
                val, name), e);
        }
    }

    /**
     * Instantiate the given value's named handler, or return null if no
     * named handler.
     */
    protected ValueHandler namedHandler(ValueMapping val) {
        String name = val.getValueInfo().getStrategy();
        if (name == null)
            return null;

        String props = Configurations.getProperties(name);
        name = Configurations.getClassName(name);
        try {
            Class c = JavaTypes.classForName(name, val,
                ValueHandler.class.getClassLoader());
            if (ValueHandler.class.isAssignableFrom(c)) {
                ValueHandler vh = (ValueHandler) c.newInstance();
                Configurations.configureInstance(vh, getConfiguration(),
                    props);
                return vh;
            }
            return null; // named field strategy
        } catch (Exception e) {
            throw new MetaDataException(_loc.get("bad-value-handler",
                val, name), e);
        }
    }

    /**
     * Determine the default handler to use for the given value. Does
     * not take into account the named handler, if any.
     */
    protected ValueHandler defaultHandler(ValueMapping val) {
        return defaultHandler(val, getStrategyInstaller().isAdapting());
    }

    /**
     * Determine the default handler to use for the given value. Does
     * not take into account the named handler, if any.
     */
    protected ValueHandler defaultHandler(ValueMapping val, boolean adapting) {
        if (val.isSerialized()) {
            if (_dict.maxEmbeddedBlobSize != -1)
                warnMaxEmbedded(val, _dict.maxEmbeddedBlobSize);
            return BlobValueHandler.getInstance();
        }

        Object handler = mappedStrategy(val, val.getType(), adapting);
        if (handler instanceof ValueHandler)
            return (ValueHandler) handler;

        if (val.getType() == byte[].class) {
            if (_dict.maxEmbeddedBlobSize != -1)
                warnMaxEmbedded(val, _dict.maxEmbeddedBlobSize);
            return ByteArrayValueHandler.getInstance();
        }
        if (val.getType() == char[].class
            || val.getType() == Character[].class) {
            if (isClob(val, true))
                return CharArrayStreamValueHandler.getInstance();
            return CharArrayValueHandler.getInstance();
        }

        switch (val.getTypeCode()) {
            case JavaTypes.BOOLEAN:
            case JavaTypes.BYTE:
            case JavaTypes.CHAR:
            case JavaTypes.DOUBLE:
            case JavaTypes.FLOAT:
            case JavaTypes.INT:
            case JavaTypes.LONG:
            case JavaTypes.SHORT:
            case JavaTypes.BOOLEAN_OBJ:
            case JavaTypes.BYTE_OBJ:
            case JavaTypes.CHAR_OBJ:
            case JavaTypes.DOUBLE_OBJ:
            case JavaTypes.FLOAT_OBJ:
            case JavaTypes.INT_OBJ:
            case JavaTypes.LONG_OBJ:
            case JavaTypes.SHORT_OBJ:
            case JavaTypes.BIGINTEGER:
            case JavaTypes.BIGDECIMAL:
            case JavaTypes.NUMBER:
            case JavaTypes.DATE:
            case JavaTypes.CALENDAR:
            case JavaTypes.LOCALE:
                return ImmutableValueHandler.getInstance();
            case JavaTypes.STRING:
                if (isClob(val, true))
                    return ClobValueHandler.getInstance();
                return ImmutableValueHandler.getInstance();
            case JavaTypes.PC:
                if (!val.getTypeMapping().isMapped()
                    && useUntypedPCHandler(val)) 
                    return UntypedPCValueHandler.getInstance();
                break;
            case JavaTypes.PC_UNTYPED:
                return UntypedPCValueHandler.getInstance();
            case JavaTypes.OID:
                return new ObjectIdValueHandler();
        }
        return null;
    }

    /**
     * Return true if we should use the generic untyped PC handler for the
     * given unmapped relation.
     */
    private boolean useUntypedPCHandler(ValueMapping val) {
        ClassMapping rel = val.getTypeMapping();
        return rel.getIdentityType() == ClassMapping.ID_UNKNOWN
            || (rel.getIdentityType() == ClassMapping.ID_APPLICATION
            && (rel.getPrimaryKeyFields().length == 0
            || (!rel.isOpenJPAIdentity() && Modifier.isAbstract
            (rel.getObjectIdType().getModifiers()))));
    }

    /**
     * Checks for hints as to whether the given column is a CLOB.
     */
    private boolean isClob(ValueMapping val, boolean warn) {
        List cols = val.getValueInfo().getColumns();
        if (cols.size() != 1)
            return false;

        Column col = (Column) cols.get(0);
        if (col.getSize() != -1 && col.getType() != Types.CLOB)
            return false;

        if (_dict.getPreferredType(Types.CLOB) != Types.CLOB)
            return false;

        if (warn && _dict.maxEmbeddedClobSize != -1)
            warnMaxEmbedded(val, _dict.maxEmbeddedClobSize);
        return true;
    }

    /**
     * Warn that the given value is being mapped to a handler that will not
     * be able to store large lobs.
     */
    private void warnMaxEmbedded(ValueMapping val, int size) {
        if (getLog().isWarnEnabled())
            getLog().warn(_loc.get("max-embed-lob", val,
                String.valueOf(size)));
    }

    /**
     * Determine the default strategy to use for the given discriminator.
     * Does not take into account the current strategy, if any.
     */
    protected DiscriminatorStrategy defaultStrategy(Discriminator discrim) {
        return defaultStrategy(discrim, getStrategyInstaller().isAdapting());
    }

    /**
     * Determine the default strategy to use for the given discriminator.
     * Does not take into account the current strategy, if any.
     */
    protected DiscriminatorStrategy defaultStrategy(Discriminator discrim,
        boolean adapting) {
        ClassMapping cls = discrim.getClassMapping();
        if (cls.getEmbeddingMetaData() != null)
            return NoneDiscriminatorStrategy.getInstance();
        if (cls.getJoinablePCSuperclassMapping() == null
            && (cls.getStrategy() == NoneClassStrategy.getInstance()
            || Modifier.isFinal(discrim.getClassMapping().getDescribedType().
            getModifiers())))
            return NoneDiscriminatorStrategy.getInstance();

        Object strat = _defaults.getStrategy(discrim, adapting);
        if (strat instanceof String)
            return instantiateDiscriminatorStrategy((String) strat, discrim);
        if (strat != null)
            return (DiscriminatorStrategy) strat;

        if (cls.getJoinablePCSuperclassMapping() != null)
            return new SuperclassDiscriminatorStrategy();
        if (discrim.getMappingInfo().getValue() != null)
            return new ValueMapDiscriminatorStrategy();
        if (cls.getMappedPCSuperclassMapping() != null)
            return NoneDiscriminatorStrategy.getInstance();
        if (adapting || _defaults.defaultMissingInfo())
            return new ClassNameDiscriminatorStrategy();
        DBDictionary dict = ((JDBCConfiguration) getConfiguration()).
            getDBDictionaryInstance();
        if (dict.joinSyntax == JoinSyntaxes.SYNTAX_TRADITIONAL)
            return NoneDiscriminatorStrategy.getInstance();
        return new SubclassJoinDiscriminatorStrategy();
    }

    /**
     * Determine the default strategy to use for the given version.
     * Does not take into account the current strategy, if any.
     */
    protected VersionStrategy defaultStrategy(Version version) {
        return defaultStrategy(version, getStrategyInstaller().isAdapting());
    }

    /**
     * Determine the default strategy to use for the given version.
     * Does not take into account the current strategy, if any.
     */
    protected VersionStrategy defaultStrategy(Version version,
        boolean adapting) {
        ClassMapping cls = version.getClassMapping();
        if (cls.getEmbeddingMetaData() != null)
            return NoneVersionStrategy.getInstance();
        if (cls.getJoinablePCSuperclassMapping() == null
            && cls.getStrategy() == NoneClassStrategy.getInstance())
            return NoneVersionStrategy.getInstance();

        Object strat = _defaults.getStrategy(version, adapting);
        if (strat instanceof String)
            return instantiateVersionStrategy((String) strat, version);
        if (strat != null)
            return (VersionStrategy) strat;

        if (cls.getJoinablePCSuperclassMapping() != null)
            return new SuperclassVersionStrategy();

        FieldMapping vfield = version.getClassMapping().
            getVersionFieldMapping();
        if (vfield != null)
            return defaultStrategy(version, vfield);
        if (adapting || _defaults.defaultMissingInfo())
            return new NumberVersionStrategy();
        return NoneVersionStrategy.getInstance();
    }

    /**
     * Return the default version strategy, given a version field.
     */
    protected VersionStrategy defaultStrategy(Version vers,
        FieldMapping vfield) {
        switch (vfield.getTypeCode()) {
            case JavaTypes.DATE:
            case JavaTypes.CALENDAR:
                return new TimestampVersionStrategy();
            case JavaTypes.BYTE:
            case JavaTypes.INT:
            case JavaTypes.LONG:
            case JavaTypes.SHORT:
            case JavaTypes.BYTE_OBJ:
            case JavaTypes.INT_OBJ:
            case JavaTypes.LONG_OBJ:
            case JavaTypes.SHORT_OBJ:
            case JavaTypes.NUMBER:
                return new NumberVersionStrategy();
            default:
                return NoneVersionStrategy.getInstance();
        }
    }

    public void endConfiguration()
    {
        super.endConfiguration();

        JDBCConfiguration conf = (JDBCConfiguration) getConfiguration();
        _dict = conf.getDBDictionaryInstance();
        if (_defaults == null)
            _defaults = conf.getMappingDefaultsInstance();
    }
}
