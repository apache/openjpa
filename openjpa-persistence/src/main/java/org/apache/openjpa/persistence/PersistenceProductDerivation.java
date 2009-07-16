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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.Compatibility;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.conf.OpenJPAProductDerivation;
import org.apache.openjpa.conf.Specification;
import org.apache.openjpa.event.LifecycleEventManager;
import org.apache.openjpa.kernel.MixedLockLevels;
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
import org.apache.openjpa.persistence.validation.ValidatorImpl;
import org.apache.openjpa.validation.Validator;
import org.apache.openjpa.validation.ValidationException;
import org.apache.openjpa.validation.ValidatingLifecycleEventManager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

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
 * @nojavadoc
 */
public class PersistenceProductDerivation 
    extends AbstractProductDerivation
    implements OpenJPAProductDerivation {

    public static final Specification SPEC_JPA = new Specification("jpa 2");
    public static final Specification ALIAS_EJB = new Specification("ejb 3");
    public static final String RSRC_GLOBAL = "META-INF/openjpa.xml";
    public static final String RSRC_DEFAULT = "META-INF/persistence.xml";

    private static final Localizer _loc = Localizer.forPackage
        (PersistenceProductDerivation.class);

    private HashMap<String, PUNameCollision> _puNameCollisions
        = new HashMap<String,PUNameCollision>();
    public void putBrokerFactoryAliases(Map m) {
    }

    public int getType() {
        return TYPE_SPEC;
    }

    @Override
    public void validate()
        throws Exception {
        // make sure JPA is available
        AccessController.doPrivileged(J2DoPrivHelper.getClassLoaderAction(
            javax.persistence.EntityManagerFactory.class));
    }
    
    @Override
    public boolean beforeConfigurationLoad(Configuration c) {
        if (!(c instanceof OpenJPAConfigurationImpl))
            return false;
        
        OpenJPAConfigurationImpl conf = (OpenJPAConfigurationImpl) c;
        conf.metaFactoryPlugin.setAlias(ALIAS_EJB.getName(),
            PersistenceMetaDataFactory.class.getName());
        conf.metaFactoryPlugin.setAlias(SPEC_JPA.getName(),
            PersistenceMetaDataFactory.class.getName());
        
        conf.addValue(new EntityManagerFactoryValue());
        
        conf.readLockLevel.setAlias("optimistic", String
            .valueOf(MixedLockLevels.LOCK_OPTIMISTIC));
        conf.readLockLevel.setAlias("optimistic-force-increment", String
            .valueOf(MixedLockLevels.LOCK_OPTIMISTIC_FORCE_INCREMENT));
        conf.readLockLevel.setAlias("pessimistic-read", String
            .valueOf(MixedLockLevels.LOCK_PESSIMISTIC_READ));
        conf.readLockLevel.setAlias("pessimistic-write", String
            .valueOf(MixedLockLevels.LOCK_PESSIMISTIC_WRITE));
        conf.readLockLevel.setAlias("pessimistic-force-increment", String
            .valueOf(MixedLockLevels.LOCK_PESSIMISTIC_FORCE_INCREMENT));

        conf.writeLockLevel.setAlias("optimistic", String
            .valueOf(MixedLockLevels.LOCK_OPTIMISTIC));
        conf.writeLockLevel.setAlias("optimistic-force-increment", String
            .valueOf(MixedLockLevels.LOCK_OPTIMISTIC_FORCE_INCREMENT));
        conf.writeLockLevel.setAlias("pessimistic-read", String
            .valueOf(MixedLockLevels.LOCK_PESSIMISTIC_READ));
        conf.writeLockLevel.setAlias("pessimistic-write", String
            .valueOf(MixedLockLevels.LOCK_PESSIMISTIC_WRITE));
        conf.writeLockLevel.setAlias("pessimistic-force-increment", String
            .valueOf(MixedLockLevels.LOCK_PESSIMISTIC_FORCE_INCREMENT));

        conf.lockManagerPlugin.setAlias("mixed",
            "org.apache.openjpa.jdbc.kernel.MixedLockManager");

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

        return true;
    }

    @Override
    public boolean afterSpecificationSet(Configuration c) {
        if (!OpenJPAConfigurationImpl.class.isInstance(c)
         && !SPEC_JPA.isSame(((OpenJPAConfiguration) c).getSpecification()))
            return false;
 
        OpenJPAConfigurationImpl conf = (OpenJPAConfigurationImpl) c;
        conf.metaFactoryPlugin.setDefault(SPEC_JPA.getName());
        conf.metaFactoryPlugin.setString(SPEC_JPA.getName());
        conf.lockManagerPlugin.setDefault("mixed");
        conf.lockManagerPlugin.setString("mixed");
        conf.nontransactionalWrite.setDefault("true");
        conf.nontransactionalWrite.set(true);
        int specVersion = ((OpenJPAConfiguration) c).getSpecificationInstance()
            .getVersion();
        if (specVersion < 2) {
            Compatibility compatibility = conf.getCompatibilityInstance();
            compatibility.setFlushBeforeDetach(true);
            compatibility.setCopyOnDetach(true);
            compatibility.setPrivatePersistentProperties(true);
        } 
        return true;
    }

    /**
     * Load configuration from the given persistence unit with the specified
     * user properties.
     */
    public ConfigurationProvider load(PersistenceUnitInfo pinfo, Map m)
        throws IOException {
        if (pinfo == null)
            return null;
        if (!isOpenJPAPersistenceProvider(pinfo, null)) {
            warnUnknownProvider(pinfo);
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
        Boolean ret = load(cp, rsrc, name, m, null, explicit);
        if (ret != null)
            return (ret.booleanValue()) ? cp : null;
        if (explicit)
            return null;

        // persistence.xml does not exist; just load map
        PersistenceUnitInfoImpl pinfo = new PersistenceUnitInfoImpl();
        pinfo.fromUserProperties(m);
        if (!isOpenJPAPersistenceProvider(pinfo, null)) {
            warnUnknownProvider(pinfo);
            return null;
        }
        cp.addProperties(pinfo.toOpenJPAProperties());
        return cp;
    }

    @Override
    public ConfigurationProvider load(String rsrc, String anchor, 
        ClassLoader loader)
        throws IOException {
        if (rsrc != null && !rsrc.endsWith(".xml"))
            return null;
        ConfigurationProviderImpl cp = new ConfigurationProviderImpl();
        if (load(cp, rsrc, anchor, null, loader, true) == Boolean.TRUE)
            return cp;
        return null;
    }

    @Override
    public ConfigurationProvider load(File file, String anchor) 
        throws IOException {
        if (!file.getName().endsWith(".xml"))
            return null;

        ConfigurationParser parser = new ConfigurationParser(null);
        parser.parse(file);
        return load(findUnit((List<PersistenceUnitInfoImpl>) 
            parser.getResults(), anchor, null), null);
    }

    @Override
    public String getDefaultResourceLocation() {
        return RSRC_DEFAULT;
    }

    @Override
    public List getAnchorsInFile(File file) throws IOException {
        ConfigurationParser parser = new ConfigurationParser(null);
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
        for (PersistenceUnitInfoImpl unit : units)
            names.add(unit.getPersistenceUnitName());
        return names;
    }

    @Override
    public List getAnchorsInResource(String resource) throws Exception {
        ConfigurationParser parser = new ConfigurationParser(null);
        try {
        	List results = new ArrayList();
            ClassLoader loader = AccessController.doPrivileged(
                J2DoPrivHelper.getContextClassLoaderAction());
            List<URL> urls = getResourceURLs(resource, loader);
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
    public ConfigurationProvider loadGlobals(ClassLoader loader)
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
        if (load(cp, rsrc, anchor, null, loader, explicit) == Boolean.TRUE)
            return cp;
        return null;
    }

    @Override
    public ConfigurationProvider loadDefaults(ClassLoader loader)
        throws IOException {
        ConfigurationProviderImpl cp = new ConfigurationProviderImpl();
        if (load(cp, RSRC_DEFAULT, null, null, loader, false) == Boolean.TRUE)
            return cp;
        return null;
    }

      /**
      * This method checks to see if the provided <code>puName</code> was
      * detected in multiple resources. If a collision is detected, a warning
      * will be logged and this method will return <code>true</code>.
      * <p>
      */
     public boolean checkPuNameCollisions(Log logger,String puName){
         PUNameCollision p = _puNameCollisions.get(puName);
         if(p!=null){
             p.logCollision(logger);
             return true;
         }
         return false;
     }

    private static List<URL> getResourceURLs(String rsrc, ClassLoader loader)
        throws IOException {
        Enumeration<URL> urls = null;
        try {
            urls = AccessController.doPrivileged(
                J2DoPrivHelper.getResourcesAction(loader, rsrc)); 
            if (!urls.hasMoreElements()) {
                if (!rsrc.startsWith("META-INF"))
                    urls = AccessController.doPrivileged(
                        J2DoPrivHelper.getResourcesAction(
                            loader, "META-INF/" + rsrc)); 
                if (!urls.hasMoreElements())
                    return null;
            }
        } catch (PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }

        return Collections.list(urls);
    }

    /**
     * Looks through the resources at <code>rsrc</code> for a configuration
     * file that matches <code>name</code> (or an unnamed one if
     * <code>name</code> is <code>null</code>), and loads the XML in the
     * resource into a new {@link PersistenceUnitInfo}. Then, applies the
     * overrides in <code>m</code>.
     *
     * @return {@link Boolean#TRUE} if the resource was loaded, null if it
     * does not exist, or {@link Boolean#FALSE} if it is not for OpenJPA
     */
    private Boolean load(ConfigurationProviderImpl cp, String rsrc, 
        String name, Map m, ClassLoader loader, boolean explicit)
        throws IOException {
        if (loader == null)
            loader = AccessController.doPrivileged(
                J2DoPrivHelper.getContextClassLoaderAction());

        List<URL> urls = getResourceURLs(rsrc, loader);
        if (urls == null || urls.size() == 0)
            return null;

        ConfigurationParser parser = new ConfigurationParser(m);
        PersistenceUnitInfoImpl pinfo = parseResources(parser, urls, name, 
            loader);
        if (pinfo == null) {
            if (!explicit)
                return Boolean.FALSE;
            throw new MissingResourceException(_loc.get("missing-xml-config", 
                rsrc, String.valueOf(name)).getMessage(), getClass().getName(), 
                rsrc);
        } else if (!isOpenJPAPersistenceProvider(pinfo, loader)) {
            if (!explicit) {
                warnUnknownProvider(pinfo);
                return Boolean.FALSE;
            }
            throw new MissingResourceException(_loc.get("unknown-provider", 
                rsrc, name, pinfo.getPersistenceProviderClassName()).
                getMessage(), getClass().getName(), rsrc);
        }
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
        List<URL> urls, String name, ClassLoader loader)
        throws IOException {
        List<PersistenceUnitInfoImpl> pinfos = 
            new ArrayList<PersistenceUnitInfoImpl>();
        for (URL url : urls) {
            parser.parse(url);
            pinfos.addAll((List<PersistenceUnitInfoImpl>) parser.getResults());
        }
        return findUnit(pinfos, name, loader);
    }

    /**
     * Find the unit with the given name, or an OpenJPA unit if no name is
     * given (preferring an unnamed OpenJPA unit to a named one).
     */
    private PersistenceUnitInfoImpl findUnit(List<PersistenceUnitInfoImpl> 
        pinfos, String name, ClassLoader loader) {
        PersistenceUnitInfoImpl ojpa = null;
        PersistenceUnitInfoImpl result = null;
        for (PersistenceUnitInfoImpl pinfo : pinfos) {
            // found named unit?
            if (name != null) {
                if (name.equals(pinfo.getPersistenceUnitName())){

                    if(result!=null){
                        this.addPuNameCollision(name,
                            result.getPersistenceXmlFileUrl().toString(),
                                pinfo.getPersistenceXmlFileUrl().toString());

                    }else{
                        // Grab a ref to the pinfo that matches the name we're
                        // looking for. Keep going to look for duplicate pu
                        // names.
                        result = pinfo;
                    }
                }
                continue;
            }

            if (isOpenJPAPersistenceProvider(pinfo, loader)) {
                // if no name given and found unnamed unit, return it.  
                // otherwise record as default unit unless we find a 
                // better match later
                if (StringUtils.isEmpty(pinfo.getPersistenceUnitName()))
                    return pinfo;
                if (ojpa == null)
                    ojpa = pinfo;
            }
        }
        if(result!=null){
            return result;
        }
        return ojpa;
    }

    /**
     * Return whether the given persistence unit uses an OpenJPA provider.
     */
    private static boolean isOpenJPAPersistenceProvider
        (PersistenceUnitInfo pinfo, ClassLoader loader) {
        String provider = pinfo.getPersistenceProviderClassName();
        if (StringUtils.isEmpty(provider) 
            || PersistenceProviderImpl.class.getName().equals(provider))
            return true;

        if (loader == null)
            loader = AccessController.doPrivileged(
                J2DoPrivHelper.getContextClassLoaderAction());
        try {
            if (PersistenceProviderImpl.class.isAssignableFrom
                (Class.forName(provider, false, loader)))
                return true;
        } catch (Throwable t) {
            log(_loc.get("unloadable-provider", provider, t).getMessage());
            return false;
        }
        return false;
    }

    /**
     * Warn the user that we could only find an unrecognized persistence 
     * provider.
     */
    private static void warnUnknownProvider(PersistenceUnitInfo pinfo) {
        log(_loc.get("unrecognized-provider", 
            pinfo.getPersistenceProviderClassName()).getMessage());
    }
    
    /**
     * Log a message.   
     */
    private static void log(String msg) {
        // at this point logging isn't configured yet
        System.err.println(msg);
    }

    private void addPuNameCollision(String puName, String file1, String file2){
        PUNameCollision pun = _puNameCollisions.get(puName);
        if(pun!=null){
            pun.addCollision(file1, file2);
        }else{
            _puNameCollisions.put(puName,
            	new PUNameCollision(puName, file1, file2));
        }

    }
    
    /**
     * Custom configuration provider.   
     */
    public static class ConfigurationProviderImpl
        extends MapConfigurationProvider {

        private String _source;

        public ConfigurationProviderImpl() {
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

        @Override
        public void setInto(Configuration conf) {
            if (conf instanceof OpenJPAConfiguration) {
                OpenJPAConfiguration oconf = (OpenJPAConfiguration) conf;
                oconf.setSpecification(SPEC_JPA);

                // we merge several persistence.xml elements into the 
                // MetaDataFactory property implicitly.  if the user has a
                // global openjpa.xml with this property set, its value will
                // get overwritten by our implicit setting.  so instead, combine
                // the global value with our settings
                String orig = oconf.getMetaDataFactory();
                if (!StringUtils.isEmpty(orig)) {
                    String key = ProductDerivations.getConfigurationKey
                        ("MetaDataFactory", getProperties());
                    Object override = getProperties().get(key);
                    if (override instanceof String)
                        addProperty(key, Configurations.combinePlugins(orig, 
                            (String) override));
                }
            }

            super.setInto(conf, null);
            Log log = conf.getConfigurationLog();
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

        private static final Localizer _loc = Localizer.forPackage
            (ConfigurationParser.class);

        private final Map _map;
        private PersistenceUnitInfoImpl _info = null;
        private URL _source = null;
        private String _persistenceVersion;
        private String _schemaLocation;

        public ConfigurationParser(Map map) {
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
                    log(_loc.get("version-check-error", 
                        _source.toString()).toString());
            }            
            super.parse(url);
        }

        @Override
        public void parse(File file)
            throws IOException {
            try {
                _source = AccessController.doPrivileged(J2DoPrivHelper
                    .toURLAction(file));
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
                    log(_loc.get("version-check-error", 
                        _source.toString()).toString());
            }            
            super.parse(file);
        }

        @Override
        protected Object getSchemaSource() {
            // use the version 1 schema by default.  non-versioned docs will 
            // continue to parse with the old xml if they do not contain a 
            // persistence-unit.  that is currently the only signficant change
            // to the schema.  if more significant changes are made in the 
            // future, the 2.0 schema may be preferable.
            String persistencexsd = "persistence-xsd.rsrc";
            // if the version and/or schema location is for 1.0, use the 1.0 
            // schema
            if (_persistenceVersion != null &&
                _persistenceVersion.equals(XMLVersionParser.VERSION_2_0) ||
                (_schemaLocation != null && 
                _schemaLocation.indexOf(PERSISTENCE_XSD_2_0) != -1)) {
                persistencexsd = "persistence_2_0-xsd.rsrc";
            }
            return getClass().getResourceAsStream(persistencexsd);
        }

        @Override
        protected void reset() {
            super.reset();
            _info = null;
            _source = null;
        }

        protected boolean startElement(String name, Attributes attrs)
            throws SAXException {
            if (currentDepth() == 1)
                startPersistenceUnit(attrs);
            else if (currentDepth() == 3 && "property".equals(name))
                _info.setProperty(attrs.getValue("name"),
                    attrs.getValue("value"));
            return true;
        }

        protected void endElement(String name)
            throws SAXException {
            if (currentDepth() == 1) {
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
                    if ("class".equals(name))
                        _info.addManagedClassName(currentText());
                    else // FIXME - caching
                        throw new javax.persistence.PersistenceException(
                            "Not implemented yet");
                    break;
                case 'e': // exclude-unlisted-classes
                    _info.setExcludeUnlistedClasses("true".equalsIgnoreCase
                        (currentText()));
                    break;
                case 'j':
                    if ("jta-data-source".equals(name))
                        _info.setJtaDataSourceName(currentText());
                    else // jar-file
                    {
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
                case 'v': // validation-mode
                    _info.setValidationMode(Enum.valueOf(ValidationMode.class,
                        currentText()));
                    break;
            }
        }

        /**
         * Parse persistence-unit element.
         */
        private void startPersistenceUnit(Attributes attrs)
            throws SAXException {
            _info = new PersistenceUnitInfoImpl();
            _info.setPersistenceUnitName(attrs.getValue("name"));
            _info.setPersistenceXMLSchemaVersion(_persistenceVersion);
            
            // we only parse this ourselves outside a container, so default
            // transaction type to local
            String val = attrs.getValue("transaction-type");
            if (val == null)
                _info.setTransactionType
                    (PersistenceUnitTransactionType.RESOURCE_LOCAL);
            else
                _info.setTransactionType(Enum.valueOf
                    (PersistenceUnitTransactionType.class, val));

            if (_source != null)
                _info.setPersistenceXmlFileUrl(_source);
		}
	}
    /**
     * This private class is used to hold onto information regarding
     * PersistentUnit name collisions.
     */
    private static class PUNameCollision{
        private String _puName;
        private Set<String> _resources;

        PUNameCollision(String puName, String file1, String file2) {
            _resources = new LinkedHashSet<String>();
            _resources.add(file1);
            _resources.add(file2);

            _puName=puName;
        }
        void logCollision(Log logger){
            if(logger.isWarnEnabled()){
                logger.warn(_loc.getFatal("dup-pu",
                    new Object[]{_puName,_resources.toString(),
                    	_resources.iterator().next()}));
            }
        }
        void addCollision(String file1, String file2){
            _resources.add(file1);
            _resources.add(file2);
        }

    }
}
