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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.Compatibility;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.conf.OpenJPAProductDerivation;
import org.apache.openjpa.conf.Specification;
import org.apache.openjpa.datacache.DataCacheMode;
import org.apache.openjpa.kernel.QueryHints;
import org.apache.openjpa.lib.conf.AbstractProductDerivation;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.conf.MapConfigurationProvider;
import org.apache.openjpa.lib.conf.ProductDerivations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.meta.XMLMetaDataParser;
import org.apache.openjpa.lib.meta.XMLVersionParser;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.MultiClassLoader;
import org.apache.openjpa.persistence.osgi.BundleUtils;
import org.apache.openjpa.validation.Validator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import static org.apache.openjpa.kernel.MixedLockLevels.*;

/**
 * Sets JPA specification defaults and parses JPA specification XML files.
 * 
 * For globals, looks in <code>openjpa.properties</code> system property for
 * the location of a file to parse. If no system property is defined, the
 * default resource location of <code>META-INF/openjpa.xml</code> is used.
 *
 * For defaults, looks for <code>META-INF/persistence.xml</code>.
 * Within <code>persistence.xml</code>, look for the named persistence unit, or
 * if no name given, an OpenJPA unit (preferring an unnamed OpenJPA unit to 
 * a named one).
 *
 * @author Abe White
 * @author Pinaki Poddar
 * @nojavadoc
 */
public class PersistenceProductDerivation 
    extends AbstractProductDerivation
    implements OpenJPAProductDerivation {

    public static final Specification SPEC_JPA = new Specification("jpa 2");
    public static final Specification ALIAS_EJB = new Specification("ejb 3");
    public static final String RSRC_GLOBAL = "META-INF/openjpa.xml";
    public static final String RSRC_DEFAULT = "META-INF/persistence.xml";
    public static final BigDecimal VERSION_1_0 = BigDecimal.valueOf(1.0);

    private static final Localizer _loc = Localizer.forPackage(PersistenceProductDerivation.class);

    
    public static final String PREFIX = "javax.persistence"; 
    
    // These are properties that are invalid to be configured at the provider level. 
    private static final String[] _invalidPersistenceProperties =
        new String[] { PREFIX + ".cache.storeMode", PREFIX + ".cache.retrieveMode" };
    
    private static Set<String> _hints = new HashSet<String>();
    
    /**
     * The loader used to load resources and classes by name and eventually set into
     * the configuration.
     */
    private final MultiClassLoader _loader;

    static {
        _hints.add("javax.persistence.lock.timeout");
        _hints.add("javax.persistence.query.timeout");
        
        _hints.add("openjpa.FetchPlan.ExtendedPathLookup");
        _hints.add("openjpa.FetchBatchSize"); 
        _hints.add("openjpa.FetchPlan.FetchBatchSize");
        _hints.add("openjpa.MaxFetchDepth"); 
        _hints.add("openjpa.FetchPlan.MaxFetchDepth");
        _hints.add("openjpa.LockTimeout");
        _hints.add("openjpa.FetchPlan.LockTimeout");
        _hints.add("openjpa.QueryTimeout");
        _hints.add("openjpa.FetchPlan.QueryTimeout");
        _hints.add("openjpa.FlushBeforeQueries");
        _hints.add("openjpa.FetchPlan.FlushBeforeQueries");
        _hints.add("openjpa.ReadLockLevel");
        _hints.add("openjpa.FetchPlan.ReadLockLevel");
        _hints.add("openjpa.WriteLockLevel");
        _hints.add("openjpa.FetchPlan.WriteLockLevel");
        _hints.add("openjpa.FetchPlan.FetchBatchSize");
        _hints.add("openjpa.FetchPlan.LockScope");
        _hints.add("openjpa.FetchPlan.LockTimeout");
        _hints.add("openjpa.FetchPlan.MaxFetchDepth");
        _hints.add("openjpa.FetchPlan.QueryTimeout");
        _hints.add("openjpa.FetchPlan.ReadLockMode");
        _hints.add("openjpa.FetchPlan.WriteLockMode");
        _hints.add(QueryHints.HINT_AGGREGATE_LISTENER);
        _hints.add(QueryHints.HINT_AGGREGATE_LISTENERS);
        _hints.add(QueryHints.HINT_FILTER_LISTENER);
        _hints.add(QueryHints.HINT_FILTER_LISTENERS);
        _hints.add(QueryHints.HINT_IGNORE_FINDER);
        _hints.add(QueryHints.HINT_IGNORE_PREPARED_QUERY);
        _hints.add(QueryHints.HINT_INVALIDATE_FINDER);
        _hints.add(QueryHints.HINT_INVALIDATE_PREPARED_QUERY);
        _hints.add(QueryHints.HINT_PARAM_MARKER_IN_QUERY);
        _hints.add(QueryHints.HINT_RECACHE_FINDER);
        _hints.add(QueryHints.HINT_RESULT_COUNT);
        _hints.add(QueryHints.HINT_SUBCLASSES);

        _hints = Collections.unmodifiableSet(_hints);
    }
    
    public PersistenceProductDerivation() {
    	this(false);
    }
    
    /**
     * Constructs and configures a class loader to be used for loading
     * resources and classes by their names.
     */
	public PersistenceProductDerivation(boolean osgi) {
		_loader = AccessController.doPrivileged(J2DoPrivHelper.newMultiClassLoaderAction());
		if (osgi) 
			_loader.addClassLoader(BundleUtils.getBundleClassLoader());
		_loader.addClassLoader(MultiClassLoader.THREAD_LOADER);
		_loader.addClassLoader(this.getClass().getClassLoader());
		_loader.addClassLoader(MultiClassLoader.SYSTEM_LOADER);
	}
    
    public void putBrokerFactoryAliases(Map<String, String> m) {
    }

    public int getType() {
        return TYPE_SPEC;
    }
    
    @Override
    public String getConfigurationPrefix() {
        return PREFIX;
    }
    
    @Override
    public Set<String> getSupportedQueryHints() {
        return _hints;
    }

    @Override
    public void validate() throws Exception {
        // make sure JPA is available
        AccessController.doPrivileged(J2DoPrivHelper.getClassLoaderAction(
            javax.persistence.EntityManagerFactory.class));
    }
    
    @Override
    public boolean beforeConfigurationLoad(Configuration c) {
        if (!(c instanceof OpenJPAConfigurationImpl))
            return false;
        
        OpenJPAConfigurationImpl conf = (OpenJPAConfigurationImpl) c;
        conf.metaFactoryPlugin.setAlias(ALIAS_EJB.getName(), PersistenceMetaDataFactory.class.getName());
        conf.metaFactoryPlugin.setAlias(SPEC_JPA.getName(),  PersistenceMetaDataFactory.class.getName());
        
        conf.addValue(new EntityManagerFactoryValue());
        
        conf.readLockLevel.setAlias("optimistic",                  String.valueOf(LOCK_OPTIMISTIC));
        conf.readLockLevel.setAlias("optimistic-force-increment",  String.valueOf(LOCK_OPTIMISTIC_FORCE_INCREMENT));
        conf.readLockLevel.setAlias("pessimistic-read",            String.valueOf(LOCK_PESSIMISTIC_READ));
        conf.readLockLevel.setAlias("pessimistic-write",           String.valueOf(LOCK_PESSIMISTIC_WRITE));
        conf.readLockLevel.setAlias("pessimistic-force-increment", String.valueOf(LOCK_PESSIMISTIC_FORCE_INCREMENT));

        conf.writeLockLevel.setAlias("optimistic",                  String.valueOf(LOCK_OPTIMISTIC));
        conf.writeLockLevel.setAlias("optimistic-force-increment",  String.valueOf(LOCK_OPTIMISTIC_FORCE_INCREMENT));
        conf.writeLockLevel.setAlias("pessimistic-read",            String.valueOf(LOCK_PESSIMISTIC_READ));
        conf.writeLockLevel.setAlias("pessimistic-write",           String.valueOf(LOCK_PESSIMISTIC_WRITE));
        conf.writeLockLevel.setAlias("pessimistic-force-increment", String.valueOf(LOCK_PESSIMISTIC_FORCE_INCREMENT));


        configureBeanValidation(conf);
        
        conf.dataCacheMode = conf.addString(JPAProperties.CACHE_MODE);
        conf.dataCacheMode.setDefault(DataCacheMode.UNSPECIFIED.toString());
        conf.dataCacheMode.set(DataCacheMode.UNSPECIFIED.toString());

        return true;
    }
    
    /**
     * Bean Validation configuration is unusual because its usage of enums and keys that
     * do not have counterparts in kernel.
     * Hence the plugins are defined in product derivation instead of the kernel's
     * core configuration.
     * 
     * @param conf
     */
    private void configureBeanValidation(OpenJPAConfigurationImpl conf) {
        // Validation defines/adds the following plugins to OpenJPA Configuration
        conf.validationFactory         = conf.addObject(Object.class, JPAProperties.VALIDATE_FACTORY); 
        conf.validator                 = conf.addObject(Validator.class, "Validator");
        conf.validationMode            = conf.addString(JPAProperties.VALIDATE_MODE);
        conf.validationGroupPrePersist = conf.addString(JPAProperties.VALIDATE_PRE_PERSIST);
        conf.validationGroupPreUpdate  = conf.addString(JPAProperties.VALIDATE_PRE_UPDATE);
        conf.validationGroupPreRemove  = conf.addString(JPAProperties.VALIDATE_PRE_REMOVE);
        
        conf.validationMode.setDynamic(true);
        String[] aliases = new String[] {
                String.valueOf(ValidationMode.AUTO),
                String.valueOf(ValidationMode.AUTO).toLowerCase(),
                String.valueOf(ValidationMode.CALLBACK),
                String.valueOf(ValidationMode.CALLBACK).toLowerCase(),
                String.valueOf(ValidationMode.NONE),
                String.valueOf(ValidationMode.NONE).toLowerCase()
        };
        conf.validationMode.setAliases(aliases);
        conf.validationMode.setAliasListComprehensive(true);
        conf.validationMode.setDefault(aliases[0]);

        conf.validationGroupPrePersist.setString(JPAProperties.VALIDATE_GROUP_DEFAULT);
        conf.validationGroupPrePersist.setDefault("");
        conf.validationGroupPrePersist.setDynamic(true);

        conf.validationGroupPreUpdate.setString(JPAProperties.VALIDATE_GROUP_DEFAULT);
        conf.validationGroupPreUpdate.setDefault("");
        conf.validationGroupPreUpdate.setDynamic(true);

        conf.validationGroupPreRemove.setDefault("");
        conf.validationGroupPreRemove.setDynamic(true);

        conf.validationFactory.setInstantiatingGetter("getValidationFactoryInstance");
        conf.validationFactory.setDynamic(true);

        conf.validator.setInstantiatingGetter("getValidatorInstance");
        conf.validator.setDynamic(true);
        conf.validator.makePrivate();
    }

    @Override
    public boolean afterSpecificationSet(Configuration c) {
        if (!OpenJPAConfigurationImpl.class.isInstance(c)
         && !SPEC_JPA.isSame(((OpenJPAConfiguration) c).getSpecification()))
            return false;
 
        OpenJPAConfigurationImpl conf = (OpenJPAConfigurationImpl) c;
        conf.metaFactoryPlugin.setDefault(SPEC_JPA.getName());
        conf.metaFactoryPlugin.setString(SPEC_JPA.getName());
        conf.nontransactionalWrite.setDefault("true");
        conf.nontransactionalWrite.set(true);
        Specification spec = ((OpenJPAConfiguration) c).getSpecificationInstance();
        int specVersion = spec.getVersion();
        Compatibility compatibility = conf.getCompatibilityInstance();
        spec.setCompatibility(compatibility);
        if (specVersion < 2) {
            compatibility.setFlushBeforeDetach(true);
            compatibility.setCopyOnDetach(true);
            compatibility.setPrivatePersistentProperties(true);
            compatibility.setIgnoreDetachedStateFieldForProxySerialization(true);
            // Disable bean validation for spec level < 2 configurations
            conf.validationMode.set(String.valueOf(ValidationMode.NONE));
        } else {
            compatibility.setAbstractMappingUniDirectional(true);
            compatibility.setNonDefaultMappingAllowed(true);
        }
        return true;
    }

    /**
     * Load configuration from the given persistence unit with the specified
     * user properties.
     */
    public ConfigurationProvider load(PersistenceUnitInfo pinfo, Map m) throws IOException {
        if (pinfo == null)
            return null;
        if (!isOpenJPAPersistenceProvider(pinfo)) {
            return null;
        }

        ConfigurationProviderImpl cp = new ConfigurationProviderImpl();
        cp.addProperties(PersistenceUnitInfoImpl.toOpenJPAProperties(pinfo));
        cp.addProperties(m);
        if (pinfo instanceof PersistenceUnitInfoImpl) {
            PersistenceUnitInfoImpl impl = (PersistenceUnitInfoImpl) pinfo;
            if (impl.getPersistenceXmlFileUrl() != null)
                cp.setSource(impl.getPersistenceXmlFileUrl().toString());
        }
        return cp;
    }

    /**
     * Load configuration from the given resource and unit names, which may
     * be null.
     */
    public ConfigurationProvider load(String rsrc, String name, Map m)
        throws IOException {
        boolean explicit = !StringUtils.isEmpty(rsrc);
        if (!explicit)
            rsrc = RSRC_DEFAULT;
        
        ConfigurationProviderImpl cp = new ConfigurationProviderImpl();
        Boolean ret = load(cp, rsrc, name, m, explicit);
        if (ret != null)
            return (ret.booleanValue()) ? cp : null;
        if (explicit)
            return null;

        // persistence.xml does not exist; just load map
        PersistenceUnitInfoImpl pinfo = new PersistenceUnitInfoImpl();
        pinfo.fromUserProperties(m);
        if (!isOpenJPAPersistenceProvider(pinfo)) {
            return null;
        }
        cp.addProperties(pinfo.toOpenJPAProperties());
        return cp;
    }

    @Override
    public ConfigurationProvider load(String rsrc, String anchor)  throws IOException {
        if (rsrc != null && !rsrc.endsWith(".xml"))
            return null;
        ConfigurationProviderImpl cp = new ConfigurationProviderImpl();
        if (load(cp, rsrc, anchor, null, true) == Boolean.TRUE)
            return cp;
        return null;
    }

    @Override
    public ConfigurationProvider load(File file, String anchor) throws IOException {
        if (!file.getName().endsWith(".xml"))
            return null;

        ConfigurationParser parser = new ConfigurationParser(_loader, null);
        parser.parse(file);
        List<PersistenceUnitInfoImpl> units = (List<PersistenceUnitInfoImpl>) parser.getResults();
        PersistenceUnitInfoImpl unit = findUnit(units, anchor);
        return load(unit, null);
    }

    @Override
    public String getDefaultResourceLocation() {
        return RSRC_DEFAULT;
    }

    @Override
    public List<String> getAnchorsInFile(File file) throws IOException {
        ConfigurationParser parser = new ConfigurationParser(_loader, null);
        try {
            parser.parse(file);
            return getUnitNames(parser);
        } catch (IOException e) {
            // not all configuration files are XML; return null if unparsable
            return null;
        }
    }

    private List<String> getUnitNames(ConfigurationParser parser) {
        List<PersistenceUnitInfoImpl> units = parser.getResults();
        List<String> names = new ArrayList<String>();
        for (PersistenceUnitInfoImpl unit : units){
        	String provider = unit.getPersistenceProviderClassName();
			// Only add the PU name if the provider it is ours or not specified.
			if (provider == null || provider.indexOf("openjpa") != -1) {
				names.add(unit.getPersistenceUnitName());
			} else {
				// Should trace something, but logging isn't configured yet.
				// Swallow.
			}
        }
        return names;
    }

    @Override
    public List<String> getAnchorsInResource(String resource) throws Exception {
        ConfigurationParser parser = new ConfigurationParser(_loader, null);
        try {
        	List<String> results = new ArrayList<String>();
            List<URL> urls = getResourceURLs(resource);
            if (urls != null) {
                for (URL url : urls) {
                    parser.parse(url);
                    results.addAll(getUnitNames(parser));
                }
            }
            return results;
        } catch (IOException e) {
            // not all configuration files are XML; return null if unparsable
            return null;
        }
    }

    @Override
    public ConfigurationProvider loadGlobals()
        throws IOException {
        String[] prefixes = ProductDerivations.getConfigurationPrefixes();
        String rsrc = null;
        for (int i = 0; i < prefixes.length && StringUtils.isEmpty(rsrc); i++)
           rsrc = AccessController.doPrivileged(J2DoPrivHelper
                .getPropertyAction(prefixes[i] + ".properties")); 
        boolean explicit = !StringUtils.isEmpty(rsrc);
        String anchor = null;
        int idx = (!explicit) ? -1 : rsrc.lastIndexOf('#');
        if (idx != -1) {
            // separate name from <resrouce>#<name> string
            if (idx < rsrc.length() - 1)
                anchor = rsrc.substring(idx + 1);
            rsrc = rsrc.substring(0, idx);
        }
        if (StringUtils.isEmpty(rsrc))
            rsrc = RSRC_GLOBAL;
        else if (!rsrc.endsWith(".xml"))
            return null;

        ConfigurationProviderImpl cp = new ConfigurationProviderImpl();
        if (load(cp, rsrc, anchor, null, explicit) == Boolean.TRUE)
            return cp;
        return null;
    }

    @Override
    public ConfigurationProvider loadDefaults()
        throws IOException {
        ConfigurationProviderImpl cp = new ConfigurationProviderImpl();
        if (load(cp, RSRC_DEFAULT, null, null, false) == Boolean.TRUE)
            return cp;
        return null;
    }

     private void checkPuNameCollisions(){
     }

    private List<URL> getResourceURLs(String rsrc)
        throws IOException {
        Enumeration<URL> urls = null;
        try {
            urls = AccessController.doPrivileged(J2DoPrivHelper.getResourcesAction(_loader, rsrc)); 
            if (!urls.hasMoreElements()) {
                if (!rsrc.startsWith("META-INF"))
                  urls = AccessController.doPrivileged(J2DoPrivHelper.getResourcesAction(_loader, "META-INF/" + rsrc));
                if (!urls.hasMoreElements())
                    return null;
            }
        } catch (PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }
        List<URL> result = new ArrayList<URL>();
        for (;urls.hasMoreElements();) {
        	URL url = urls.nextElement();
        	if (!result.contains(url)) {
        		result.add(url);
        	}
        }
        return result;
    }

    /**
     * The core load method to load the configuration resource.
     * The class loader that loads the configuration resource is remembered by the provider
     * and later {@link ConfigurationProvider#setInto(Configuration) set into} {@link Configuration}.
     * <br>
     * Looks through the resources at <code>rsrc</code> for a configuration
     * file that matches <code>name</code> (or an unnamed one if
     * <code>name</code> is <code>null</code>), and loads the XML in the
     * resource into a new {@link PersistenceUnitInfo}. Then, applies the
     * overrides in <code>m</code>.
     *
     * @return {@link Boolean#TRUE} if the resource was loaded, null if it
     * does not exist, or {@link Boolean#FALSE} if it is not for OpenJPA
     */
    private Boolean load(ConfigurationProviderImpl cp, String rsrc, String name, Map m, boolean explicit)
        throws IOException {
        List<URL> urls = getResourceURLs(rsrc);
        if (urls == null || urls.size() == 0)
            return null;

        ConfigurationParser parser = new ConfigurationParser(_loader, m);
        PersistenceUnitInfoImpl pinfo = parseResources(parser, urls, name);
        if (pinfo == null) {
            if (!explicit)
                return Boolean.FALSE;
            throw new MissingResourceException(_loc.get("missing-xml-config", 
                rsrc, String.valueOf(name)).getMessage(), getClass().getName(), rsrc);
        } else if (!isOpenJPAPersistenceProvider(pinfo)) {
            if (!explicit) {
                return Boolean.FALSE;
            }
            throw new MissingResourceException(_loc.get("unknown-provider", 
                rsrc, name, pinfo.getPersistenceProviderClassName()).
                getMessage(), getClass().getName(), rsrc);
        }

        // Process jar-file references after confirming OpenJPA is the desired JPA provider.
        pinfo.processJarFileNames();

        cp.addProperties(pinfo.toOpenJPAProperties());
        cp.setSource(pinfo.getPersistenceXmlFileUrl().toString());
        
        return Boolean.TRUE;
    }

    /**
     * Parse resources at the given location. Searches for a
     * PersistenceUnitInfo with the requested name, or an OpenJPA unit if
     * no name given (preferring an unnamed OpenJPA unit to a named one).
     */
    private PersistenceUnitInfoImpl parseResources(ConfigurationParser parser,
        List<URL> urls, String name)
        throws IOException {
        List<PersistenceUnitInfoImpl> pinfos = new ArrayList<PersistenceUnitInfoImpl>();
        for (URL url : urls) {
            parser.parse(url);
            pinfos.addAll((List<PersistenceUnitInfoImpl>) parser.getResults());
            PersistenceUnitInfoImpl pu = null;
         }
        return findUnit(pinfos, name);
    }

    /**
     * Finds the unit with the given name in the given list of persistence unit infos.
     * Ignores any persistence unit info that does not use OpenJPA as persistence provider.
     * If the same unit name is found in multiple units, returns the first unit but
     * records the rest as duplicates.
     */
    private PersistenceUnitInfoImpl findUnit(List<PersistenceUnitInfoImpl> pinfos, String name) {
        PUNameCollision _puNameCollisions = null;
        PersistenceUnitInfoImpl result = null;
        for (PersistenceUnitInfoImpl pinfo : pinfos) {
        	if (!isOpenJPAPersistenceProvider(pinfo)) {
        		continue;
        	}
        	if (name == null || name.equals(pinfo.getPersistenceUnitName())) {
        		if (result == null) {
        			result = pinfo;
        		} else {
        			if (_puNameCollisions == null) {
        				_puNameCollisions = new PUNameCollision(name, result, pinfo);
        			} else {
        				_puNameCollisions.addDuplicate(pinfo);
        			}
        		}
        	} 
        }
        if (_puNameCollisions != null) {
        	_puNameCollisions.logCollision();
        }
        return result;
    }

    /**
     * Affirms if the given provider is recognized as an OpenJPA provider.
     * An empty string or a name containing <tt>"openjpa"</tt> returns true.
     */
    private boolean isOpenJPAPersistenceProvider(PersistenceUnitInfo pinfo) {
    	String provider = pinfo.getPersistenceProviderClassName();
        if (StringUtils.isEmpty(provider) || "void".equals(provider) || provider.indexOf("openjpa") != -1)
            return true;

        log(_loc.get("unrecognized-provider", provider).getMessage());
        return false;
    }

    
    /**
     * Log a message on the console because no logging mechanism has been configured yet.   
     */
    private static void log(String msg) {
        // at this point logging isn't configured yet
        System.err.println("WARN: " + msg);
    }

    
    /**
     * Custom configuration provider.   
     */
    public static class ConfigurationProviderImpl
        extends MapConfigurationProvider {

        private String _source;

        public ConfigurationProviderImpl() {
        	super();
        }

        public ConfigurationProviderImpl(Map props) {
            super(props);
        }

        /**
         * Set the source of information in this provider.
         */
        public void setSource(String source) {
            _source = source;
        }

        /**
         * Sets the configuration.
         */
        @Override
        public void setInto(Configuration conf) {
            if (conf instanceof OpenJPAConfiguration) {
                OpenJPAConfiguration oconf = (OpenJPAConfiguration) conf;
                Object persistenceVersion = getProperties().get(PersistenceUnitInfoImpl.PERSISTENCE_VERSION);
                if (persistenceVersion == null) {
                    oconf.setSpecification(SPEC_JPA);
                } else {
                    // Set the spec level based on the persistence version
                    oconf.setSpecification("jpa " + persistenceVersion.toString());
                }
                    

                // we merge several persistence.xml elements into the 
                // MetaDataFactory property implicitly.  if the user has a
                // global openjpa.xml with this property set, its value will
                // get overwritten by our implicit setting.  so instead, combine
                // the global value with our settings
                String orig = oconf.getMetaDataFactory();
                if (!StringUtils.isEmpty(orig)) {
                    String key = ProductDerivations.getConfigurationKey("MetaDataFactory", getProperties());
                    Object override = getProperties().get(key);
                    if (override instanceof String)
                        addProperty(key, Configurations.combinePlugins(orig, (String) override));
                }
            }
            super.setInto(conf, null);
            
            // At this point user properties have been loaded into the configuration. 
            // Apply any modifications based off those.
            if (conf instanceof OpenJPAConfiguration) {
                OpenJPAConfiguration oconf = (OpenJPAConfiguration) conf;
                // If the data cache is enabled, make sure we have a RemoteCommitProvider
                String dc = oconf.getDataCache();
                String rcp = oconf.getRemoteCommitProvider();
                // If the data cache is set and is something other than false
                if (!"false".equals(dc)) {
                    // If RCP is null or empty, set it to sjvm.
                    if (StringUtils.isEmpty(rcp)) {
                        oconf.setRemoteCommitProvider("sjvm");
                    }
                }
            }
            
            Log log = conf.getConfigurationLog();
            if (log.isWarnEnabled()) {
                Map<?, ?> props = getProperties();
                for (String propKey : _invalidPersistenceProperties) {
                    Object propValue = props.get(propKey);
                    if (propValue != null) {
                        log.warn(_loc.get("invalid-persistence-property", new Object[] { propKey, propValue }));
                    }
                }
            }
            if (log.isTraceEnabled()) {
                String src = (_source == null) ? "?" : _source;
                log.trace(_loc.get("conf-load", src, getProperties()));
            }
        }
    }

    /**
     * SAX handler capable of parsing an JPA persistence.xml file.
     * Package-protected for testing.
     */
    public static class ConfigurationParser
        extends XMLMetaDataParser {

        private static final String PERSISTENCE_XSD_1_0 = "persistence_1_0.xsd";
        private static final String PERSISTENCE_XSD_2_0 = "persistence_2_0.xsd";


        private final Map _map;
        private PersistenceUnitInfoImpl _info = null;
        private URL _source = null;
        private String _persistenceVersion;
        private String _schemaLocation;
        private boolean _excludeUnlistedSet = false;

        /**
         * Parse configuration information.
         * 
         * @param loader Class Loader to load resources. Must not be null.
         * @param 
         * @param map
         */
        public ConfigurationParser(ClassLoader loader, Map map) {
        	super(loader);
            _map = map;
            setCaching(false);
            setValidating(true);
            setParseText(true);
        }

        @Override
        public void parse(URL url)
            throws IOException {
            _source = url;

            // peek at the doc to determine the version
            XMLVersionParser vp = new XMLVersionParser("persistence");
            try {
                vp.parse(url);
                _persistenceVersion = vp.getVersion();
                _schemaLocation = vp.getSchemaLocation();
            } catch (Throwable t) {
                    log(_loc.get("version-check-error", _source.toString()).toString());
            }            
            super.parse(url);
        }

        @Override
        public void parse(File file)
            throws IOException {
            try {
                _source = AccessController.doPrivileged(J2DoPrivHelper.toURLAction(file));
            } catch (PrivilegedActionException pae) {
                throw (MalformedURLException) pae.getException();
            }
            // peek at the doc to determine the version
            XMLVersionParser vp = new XMLVersionParser("persistence");
            try {
                vp.parse(file);
                _persistenceVersion = vp.getVersion();
                _schemaLocation = vp.getSchemaLocation();                
            } catch (Throwable t) {
                    log(_loc.get("version-check-error", _source.toString()).toString());
            }            
            super.parse(file);
        }
        
        boolean peek(String name, URL fileOrURL) {
            XMLVersionParser vp = new XMLVersionParser("persistence");
            try {
                vp.parse(fileOrURL);
                _persistenceVersion = vp.getVersion();
                _schemaLocation = vp.getSchemaLocation();                
            } catch (Throwable t) {
                    log(_loc.get("version-check-error", _source.toString()).toString());
            }            
            return vp.unitNames().contains(name);
        }

        @Override
        protected Object getSchemaSource() {
            // use the version 1 schema by default.  non-versioned docs will 
            // continue to parse with the old xml if they do not contain a 
            // persistence-unit.  that is currently the only significant change
            // to the schema.  if more significant changes are made in the 
            // future, the 2.0 schema may be preferable.
            String persistencexsd = "persistence-xsd.rsrc";
            // if the version and/or schema location is for 1.0, use the 1.0 
            // schema
            if (_persistenceVersion != null && _persistenceVersion.equals(XMLVersionParser.VERSION_2_0) 
            || (_schemaLocation != null && _schemaLocation.indexOf(PERSISTENCE_XSD_2_0) != -1)) {
                persistencexsd = "persistence_2_0-xsd.rsrc";
            }
            return getClass().getResourceAsStream(persistencexsd);
        }

        @Override
        protected void reset() {
            super.reset();
            _info = null;
            _source = null;
            _excludeUnlistedSet = false;
        }

        protected boolean startElement(String name, Attributes attrs)
            throws SAXException {
            if (currentDepth() == 1)
                startPersistenceUnit(attrs);
            else if (currentDepth() == 3 && "property".equals(name))
                _info.setProperty(attrs.getValue("name"), attrs.getValue("value"));
            return true;
        }

        protected void endElement(String name)
            throws SAXException {
            if (currentDepth() == 1) {
                endPersistenceUnit();
                _info.fromUserProperties(_map);
                addResult(_info);
            }
            if (currentDepth() != 2)
                return;

            switch (name.charAt(0)) {
                // cases 'name' and 'transaction-type' are handled in
                //      startPersistenceUnit()
                // case 'property' for 'properties' is handled in startElement()
                case 'c': // class
                    _info.addManagedClassName(currentText());
                    break;
                case 'e': // exclude-unlisted-classes
                    setExcludeUnlistedClasses(currentText());
                    break;
                case 'j':
                    if ("jta-data-source".equals(name))
                        _info.setJtaDataSourceName(currentText());
                    else { // jar-file 
                        try {
                            _info.addJarFileName(currentText());
                        } catch (IllegalArgumentException iae) {
                            throw getException(iae.getMessage());
                        }
                    }
                    break;
                case 'm': // mapping-file
                    _info.addMappingFileName(currentText());
                    break;
                case 'n': // non-jta-data-source
                    _info.setNonJtaDataSourceName(currentText());
                    break;
                case 'p':
                    if ("provider".equals(name))
                        _info.setPersistenceProviderClassName(currentText());
                    break;
                case 's' : // shared-cache-mode
                    _info.setSharedCacheMode(JPAProperties.getEnumValue(SharedCacheMode.class, currentText()));
                    break;
                case 'v': // validation-mode
                    _info.setValidationMode(JPAProperties.getEnumValue(ValidationMode.class, currentText()));
                    break;
            }
        }

        // The default value for exclude-unlisted-classes was 
        // modified in JPA 2.0 from false to true.  Set the default
        // based upon the persistence version to preserve behavior 
        // of pre-JPA 2.0 applications.
        private void setExcludeUnlistedClasses(String value) {
            if (!_excludeUnlistedSet) {
                BigDecimal version = getPersistenceVersion();
                boolean excludeUnlisted;
                if (version.compareTo(VERSION_1_0) > 0) {
                    excludeUnlisted = !("false".equalsIgnoreCase(value));
                } else {
                    excludeUnlisted = "true".equalsIgnoreCase(value);
                }                    
                _info.setExcludeUnlistedClasses(excludeUnlisted);
                _excludeUnlistedSet = true;            
            }
        }

        /**
         * Parse persistence-unit element.
         */
        private void startPersistenceUnit(Attributes attrs)
            throws SAXException {
            _excludeUnlistedSet = false;            
            _info = new PersistenceUnitInfoImpl();
            _info.setPersistenceUnitName(attrs.getValue("name"));
            _info.setPersistenceXMLSchemaVersion(_persistenceVersion);
            
            // we only parse this ourselves outside a container, so default
            // transaction type to local
            String val = attrs.getValue("transaction-type");
            if (val == null)
                _info.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
            else
                _info.setTransactionType(Enum.valueOf(PersistenceUnitTransactionType.class, val));

            if (_source != null)
                _info.setPersistenceXmlFileUrl(_source);
		}
        
        private void endPersistenceUnit() {
            if (!_excludeUnlistedSet) {
                setExcludeUnlistedClasses(null);
            }
        }

        private BigDecimal getPersistenceVersion() {
            if (_info.getPersistenceXMLSchemaVersion() != null) {
                try {
                    return new BigDecimal(_info.getPersistenceXMLSchemaVersion());
                }
                catch (Throwable t) {
                    log(_loc.get("invalid-version-attribute", _info.getPersistenceXMLSchemaVersion(),
                        VERSION_1_0).toString());
                }
            }
            // OpenJPA supports persistence files without a version attribute.
            // A persistence file without a version attribute will be considered
            // a version 1.0 persistence file by default to maintain backward 
            // compatibility.
            return VERSION_1_0;
        }
    }
    
    
    /**
     * Holds the URLs of persistence configuration that contain the same unit name.
     */
    private static class PUNameCollision {
    	private final String _puName;
        private final PersistenceUnitInfoImpl _original;
        private final Set<PersistenceUnitInfoImpl> _duplicates;

        PUNameCollision(String pu, PersistenceUnitInfoImpl original, PersistenceUnitInfoImpl duplicate) {
        	_puName = pu;
        	_original = original;
        	_duplicates = new LinkedHashSet<PersistenceUnitInfoImpl>();
        	_duplicates.add(duplicate);

        }
        
        void logCollision(){
        	String duplicates = listDuplicates();
        	if (_puName == null) {
        		log(_loc.get("dup-pu-unnamed", _original, duplicates).toString());
        	} else {
        		log(_loc.get("dup-pu", _puName, _original, duplicates).toString());
        	}
        }
        
        void addDuplicate(PersistenceUnitInfoImpl dup){
        	_duplicates.add(dup);
        }
        
        String listDuplicates() {
        	StringBuilder buf = new StringBuilder("\r\n");
        	for (PersistenceUnitInfoImpl pu : _duplicates) {
        		buf.append(pu).append("\r\n");
        	}
        	return buf.toString();
        }
    }
}
