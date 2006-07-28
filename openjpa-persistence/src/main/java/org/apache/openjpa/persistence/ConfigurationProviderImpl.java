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
package org.apache.openjpa.persistence;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.MapConfigurationProvider;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.meta.XMLMetaDataParser;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.GeneralException;

/**
 * Configuration provider capable of loading a {@link Configuration} from
 * the current environment's JPA-style XML configuration data.
 * 
 * For defaults, looks in <code>openjpa.properties</code> system property for
 * the location of a file to parse. If no system property is defined, the
 * default resource location of <code>openjpa.xml</code> is used.
 * If it exists, the resource is parsed as an XML file.
 *
 * @nojavadoc
 * @since 4.0.0
 */
public class ConfigurationProviderImpl
    extends MapConfigurationProvider {

    private static final String RSRC_DEFAULT = "openjpa.xml";
    private static final String RSRC_SPEC = "META-INF/persistence.xml";

    private static final Localizer _loc = Localizer.forPackage
        (ConfigurationProviderImpl.class);

    private ClassLoader _loader = null;
    private String _source = null;

    public ConfigurationProviderImpl() {
        super(null);
    }

    public ClassLoader getClassLoader() {
        return _loader;
    }

    /**
     * Load configuration from the given persistence unit with the specified
     * user properties.
     */
    public boolean load(PersistenceUnitInfo pinfo)
        throws IOException {
        return load(pinfo, null);
    }

    /**
     * Load configuration from the given persistence unit with the specified
     * user properties.
     */
    public boolean load(PersistenceUnitInfo pinfo, Map m)
        throws IOException {
        if (pinfo == null)
            return false;
        String providerName = pinfo.getPersistenceProviderClassName();
        if (providerName != null && providerName.length() != 0
            && !PersistenceProviderImpl.class.getName().equals(providerName))
            return false;

        addProperties(PersistenceUnitInfoImpl.toOpenJPAProperties(pinfo));
        if (m != null)
            addProperties(m);

        _loader = pinfo.getClassLoader();
        if (pinfo instanceof PersistenceUnitInfoImpl) {
            PersistenceUnitInfoImpl impl = (PersistenceUnitInfoImpl) pinfo;
            if (impl.getPersistenceXmlFileUrl() != null)
                _source = impl.getPersistenceXmlFileUrl().toString();
        }

        return true;
    }

    /**
     * Load configuration from the given resource, with the given map of
     * overrides. If the resource is null, tries to load from persistence.xml,
     * but still returns true if persistence.xml does not exist.
     */
    public boolean load(String name, String rsrc, Map m)
        throws IOException {
        boolean explicit = rsrc != null && rsrc.length() > 0;
        if (!explicit)
            rsrc = RSRC_SPEC;
        Boolean ret = load(name, rsrc, m, null, explicit);
        if (ret != null)
            return ret.booleanValue();
        if (explicit)
            return false;

        // persistence.xml does not exist; just load map
        PersistenceUnitInfoImpl punit = new PersistenceUnitInfoImpl();
        punit.fromUserProperties(m);
        return load(punit);
    }

    @Override
    public boolean loadDefaults(ClassLoader loader)
        throws IOException {
        String rsrc = System.getProperty("openjpa.properties");
        String name = null;
        boolean explicit = false;
        if (rsrc == null)
            rsrc = RSRC_DEFAULT;
        else {
            // separate name from <resrouce>:<name> string
            explicit = true;
            int idx = rsrc.lastIndexOf(':');
            if (idx != -1) {
                name = rsrc.substring(idx + 1);
                rsrc = rsrc.substring(0, idx);
            }
            if (!rsrc.endsWith(".xml"))
                return false;
        }
        return load(name, rsrc, null, loader, explicit) == Boolean.TRUE;
    }

    /**
     * Looks through the resources at <code>rsrc</code> for a configuration
     * file that matches <code>name</code> (or the first one if
     * <code>name</code> is <code>null</code>), and loads the XML in the
     * resource into a new {@link PersistenceUnitInfo}. Then, applies the
     * overrides in <code>m</code>.
     *
     * @return {@link Boolean#TRUE} if the resource was loaded, null if it
     * does not exist, or {@link Boolean#FALSE} if it is not for OpenJPA
     */
    private Boolean load(String name, String rsrc, Map m, ClassLoader loader,
        boolean explicit)
        throws IOException {
        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();

        Enumeration<URL> urls = loader.getResources(rsrc);
        if (!urls.hasMoreElements()) {
            if (!rsrc.startsWith("META-INF"))
                urls = loader.getResources("META-INF/" + rsrc);
            if (!urls.hasMoreElements())
                return null;
        }

        ConfigurationParser parser = new ConfigurationParser(m);
        PersistenceUnitInfo pinfo = parseResources(parser, urls, name, loader);
        if (pinfo == null || !load(pinfo)) {
            if (!explicit)
                return false;
            String msg = (pinfo == null) ? "missing-xml-config"
                : "cantload-xml-config";
            throw new MissingResourceException(_loc.get(msg, rsrc,
                String.valueOf(name)), getClass().getName(), rsrc);
        }
        return true;
    }

    /**
     * Parse resources at the given location. Searches for a
     * PersistenceUnitInfo with the requested name, or the first
     * one if no name is passed in.
     */
    private PersistenceUnitInfo parseResources(ConfigurationParser parser,
        Enumeration<URL> urls, String name, ClassLoader loader)
        throws IOException {
        for (URL url : Collections.list(urls)) {
            parser.parse(url);
            List pinfos = parser.getResults();
            for (PersistenceUnitInfo pinfo : (List<PersistenceUnitInfo>) pinfos)
            {
                if (name == null
                    || name.equals(pinfo.getPersistenceUnitName()))
                    return pinfo;
            }
        }
        return null;
    }

    @Override
    public boolean load(String rsrc, ClassLoader loader)
        throws IOException {
        String name = null;
        int idx = rsrc.lastIndexOf(':');
        if (idx != -1) {
            name = rsrc.substring(idx + 1);
            rsrc = rsrc.substring(0, idx);
        }
        if (!rsrc.endsWith(".xml"))
            return false;
        return load(name, rsrc, null, loader, true) == Boolean.TRUE;
    }

    @Override
    public boolean load(File file) {
        if (!file.getName().endsWith(".xml"))
            return false;

        try {
            ConfigurationParser parser = new ConfigurationParser(null);
            parser.parse(file);
            List pinfos = parser.getResults();
            for (PersistenceUnitInfo pinfo : (List<PersistenceUnitInfo>) pinfos)
                if (load(pinfo))
                    return true;
            return false;
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        }
    }

    @Override
    public void setInto(Configuration conf) {
        if (conf instanceof OpenJPAConfiguration)
            ((OpenJPAConfiguration) conf).setSpecification
                (PersistenceProductDerivation.SPEC_JPA);
        super.setInto(conf, null);

        Log log = conf.getConfigurationLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("conf-load", _source, getProperties()));
    }

    /**
     * SAX handler capable of parsing an JPA persistence.xml file.
     * Package-protected for testing.
     */
    static class ConfigurationParser
        extends XMLMetaDataParser {

        private final Map _map;

        private PersistenceUnitInfoImpl _info = null;
        private URL _source = null;

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
            super.parse(url);
        }

        @Override
        public void parse(File file)
            throws IOException {
            _source = file.toURL();
            super.parse(file);
        }

        @Override
        protected Object getSchemaSource() {
            return getClass().getResourceAsStream("persistence-xsd.rsrc");
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
                case 'c': // class
                    _info.addManagedClassName(currentText());
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
            }
        }

        /**
         * Parse persistence-unit element.
         */
        private void startPersistenceUnit(Attributes attrs)
            throws SAXException {
            _info = new PersistenceUnitInfoImpl();
            _info.setPersistenceUnitName(attrs.getValue("name"));

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
}
