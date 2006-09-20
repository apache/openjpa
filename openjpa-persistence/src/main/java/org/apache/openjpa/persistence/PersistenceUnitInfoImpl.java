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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.meta.SourceTracker;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.MultiClassLoader;
import org.apache.openjpa.lib.util.TemporaryClassLoader;
import org.apache.openjpa.util.ClassResolver;

/**
 * Implementation of the {@link PersistenceUnitInfo} interface used by OpenJPA 
 * when parsing persistence configuration information.
 *
 * @nojavadoc
 */
public class PersistenceUnitInfoImpl
    implements PersistenceUnitInfo, SourceTracker {

    private static final Localizer s_loc = Localizer.forPackage
        (PersistenceUnitInfoImpl.class);

    private String _name;
    private final Properties _props = new Properties();
    private PersistenceUnitTransactionType _transType =
        PersistenceUnitTransactionType.RESOURCE_LOCAL;

    private String _providerClassName;
    private List<String> _mappingFileNames;
    private List<String> _entityClassNames;
    private List<URL> _jarFiles;
    private String _jtaDataSourceName;
    private DataSource _jtaDataSource;
    private String _nonJtaDataSourceName;
    private DataSource _nonJtaDataSource;
    private boolean _excludeUnlisted;
    private URL _persistenceXmlFile;

    // A persistence unit is defined by a persistence.xml file. The jar
    // file or directory whose META-INF directory contains the
    // persistence.xml file is termed the root of the persistence unit.
    //
    // In Java EE, the root of a persistence unit may be one of the following:
    // - an EJB-JAR file
    // - the WEB-INF/classes directory of a WAR file[38]
    // - a jar file in the WEB-INF/lib directory of a WAR file
    // - a jar file in the root of the EAR
    // - a jar file in the EAR library directory
    // - an application client jar file
    private URL _root;

    public ClassLoader getClassLoader() {
        return null;
    }

    public ClassLoader getNewTempClassLoader() {
        return new TemporaryClassLoader(Thread.currentThread().
            getContextClassLoader());
    }

    public String getPersistenceUnitName() {
        return _name;
    }

    public void setPersistenceUnitName(String emName) {
        _name = emName;
    }

    public String getPersistenceProviderClassName() {
        return _providerClassName;
    }

    public void setPersistenceProviderClassName(String providerClassName) {
        _providerClassName = providerClassName;
    }

    public PersistenceUnitTransactionType getTransactionType() {
        return _transType;
    }

    public void setTransactionType(PersistenceUnitTransactionType transType) {
        _transType = transType;
    }

    public String getJtaDataSourceName() {
        return _jtaDataSourceName;
    }

    public void setJtaDataSourceName(String jta) {
        _jtaDataSourceName = jta;
        if (jta != null)
            _jtaDataSource = null;
    }

    public DataSource getJtaDataSource() {
        return _jtaDataSource;
    }

    public void setJtaDataSource(DataSource ds) {
        _jtaDataSource = ds;
        if (ds != null)
            _jtaDataSourceName = null;
    }

    public String getNonJtaDataSourceName() {
        return _nonJtaDataSourceName;
    }

    public void setNonJtaDataSourceName(String nonJta) {
        _nonJtaDataSourceName = nonJta;
        if (nonJta != null)
            _nonJtaDataSource = null;
    }

    public DataSource getNonJtaDataSource() {
        return _nonJtaDataSource;
    }

    public void setNonJtaDataSource(DataSource ds) {
        _nonJtaDataSource = ds;
        if (ds != null)
            _nonJtaDataSourceName = null;
    }

    public URL getPersistenceUnitRootUrl() {
        return _root;
    }

    public void setPersistenceUnitRootUrl(URL root) {
        _root = root;
    }

    public boolean excludeUnlistedClasses() {
        return _excludeUnlisted;
    }

    public void setExcludeUnlistedClasses(boolean excludeUnlisted) {
        _excludeUnlisted = excludeUnlisted;
    }

    public List<String> getMappingFileNames() {
        return (_mappingFileNames == null)
            ? (List<String>) Collections.EMPTY_LIST : _mappingFileNames;
    }

    public void addMappingFileName(String name) {
        if (_mappingFileNames == null)
            _mappingFileNames = new ArrayList<String>();
        _mappingFileNames.add(name);
    }

    public List<URL> getJarFileUrls() {
        return (_jarFiles == null)
            ? (List<URL>) Collections.EMPTY_LIST : _jarFiles;
    }

    public void addJarFile(URL jar) {
        if (_jarFiles == null)
            _jarFiles = new ArrayList<URL>();
        _jarFiles.add(jar);
    }

    public void addJarFileName(String name) {
        MultiClassLoader loader = new MultiClassLoader();
        loader.addClassLoader(getClass().getClassLoader());
        loader.addClassLoader(MultiClassLoader.THREAD_LOADER);
        URL url = loader.getResource(name);
        if (url != null) {
            addJarFile(url);
            return;
        }

        // jar file is not a resource; check classpath
        String[] cp = System.getProperty("java.class.path").
            split(System.getProperty("path.separator"));
        for (int i = 0; i < cp.length; i++) {
            if (cp[i].equals(name)
                || cp[i].endsWith(File.separatorChar + name)) {
                try {
                    addJarFile(new File(cp[i]).toURL());
                    return;
                } catch (MalformedURLException mue) {
                    break;
                }
            }
        }
        throw new IllegalArgumentException(s_loc.get("bad-jar-name", name)
            .getMessage());
    }

    public List<String> getManagedClassNames() {
        return (_entityClassNames == null)
            ? (List<String>) Collections.EMPTY_LIST : _entityClassNames;
    }

    public void addManagedClassName(String name) {
        if (_entityClassNames == null)
            _entityClassNames = new ArrayList<String>();
        _entityClassNames.add(name);
    }

    public Properties getProperties() {
        return _props;
    }

    public void setProperty(String key, String value) {
        _props.setProperty(key, value);
    }

    public void addTransformer(ClassTransformer transformer) {
        throw new UnsupportedOperationException();
    }

    /**
     * The location of the persistence.xml resource. May be null.
     */
    public URL getPersistenceXmlFileUrl() {
        return _persistenceXmlFile;
    }

    /**
     * The location of the persistence.xml resource. May be null.
     */
    public void setPersistenceXmlFileUrl(URL url) {
        _persistenceXmlFile = url;
    }

    /**
     * Load the given user-supplied map of properties into this persistence
     * unit.
     */
    public void fromUserProperties(Map map) {
        if (map == null)
            return;

        Object key;
        Object val;
        for (Object o : map.entrySet()) {
            key = ((Map.Entry) o).getKey();
            val = ((Map.Entry) o).getValue();
            if ("javax.persistence.provider".equals(key))
                setPersistenceProviderClassName((String) val);
            else if ("javax.persistence.transactionType".equals(key)) {
                PersistenceUnitTransactionType ttype;
                if (val instanceof String)
                    ttype = Enum.valueOf
                        (PersistenceUnitTransactionType.class, (String) val);
                else
                    ttype = (PersistenceUnitTransactionType) val;
                setTransactionType(ttype);
            } else if ("javax.persistence.jtaDataSource".equals(key)) {
                if (val instanceof String)
                    setJtaDataSourceName((String) val);
                else
                    setJtaDataSource((DataSource) val);
            } else if ("javax.persistence.nonJtaDataSource".equals(key)) {
                if (val instanceof String)
                    setNonJtaDataSourceName((String) val);
                else
                    setNonJtaDataSource((DataSource) val);
            } else if (key instanceof String && val instanceof String)
                setProperty((String) key, (String) val);
        }
    }

    /**
     * Return a {@link Map} containing the properties necessary to create
     * a {@link Configuration} that reflects the information in this
     * persistence unit info.
     */
    public Map toOpenJPAProperties() {
        return toOpenJPAProperties(this);
    }

    /**
     * Return a {@link Map} containing the properties necessary to create
     * a {@link Configuration} that reflects the information in the given
     * persistence unit info.
     */
    public static Map toOpenJPAProperties(PersistenceUnitInfo info) {
        Map map = new HashMap();
        if (info.getTransactionType() == PersistenceUnitTransactionType.JTA)
            map.put("openjpa.TransactionMode", "managed");

        boolean hasJta = false;
        DataSource ds = info.getJtaDataSource();
        if (ds != null) {
            map.put("openjpa.ConnectionFactory", ds);
            map.put("openjpa.ConnectionFactoryMode", "managed");
            hasJta = true;
        } else if (info instanceof PersistenceUnitInfoImpl
            && ((PersistenceUnitInfoImpl) info).getJtaDataSourceName() != null)
        {
            map.put("openjpa.ConnectionFactoryName",
                ((PersistenceUnitInfoImpl)
                    info).getJtaDataSourceName());
            map.put("openjpa.ConnectionFactoryMode", "managed");
            hasJta = true;
        }

        ds = info.getNonJtaDataSource();
        if (ds != null) {
            if (!hasJta)
                map.put("openjpa.ConnectionFactory", ds);
            else
                map.put("openjpa.ConnectionFactory2", ds);
        } else if (info instanceof PersistenceUnitInfoImpl
            && ((PersistenceUnitInfoImpl) info).getNonJtaDataSourceName()
            != null) {
            String nonJtaName = ((PersistenceUnitInfoImpl) info).
                getNonJtaDataSourceName();
            if (!hasJta)
                map.put("openjpa.ConnectionFactoryName", nonJtaName);
            else
                map.put("openjpa.ConnectionFactory2Name",
                    nonJtaName);
        }

        if (info.getClassLoader() != null)
            map.put("openjpa.ClassResolver", new ClassResolverImpl
                (info.getClassLoader()));

        Properties props = info.getProperties();
        if (props != null) {
            map.putAll(props);
            // this isn't a real config property; remove it.
            map.remove(PersistenceProviderImpl.CLASS_TRANSFORMER_OPTIONS);
        }

        Properties metaFactoryProps = new Properties();
        if (info.getManagedClassNames() != null &&
            !info.getManagedClassNames().isEmpty()) {
            StringBuffer types = new StringBuffer();
            for (String type : info.getManagedClassNames()) {
                if (types.length() > 0)
                    types.append(';');
                types.append(type);
            }
            metaFactoryProps.put("Types", types.toString());
        }
        if (info.getJarFileUrls() != null && !info.getJarFileUrls().isEmpty()
            || (!info.excludeUnlistedClasses()
            && info.getPersistenceUnitRootUrl() != null)) {
            StringBuffer jars = new StringBuffer();
            String file = null;
            if (!info.excludeUnlistedClasses()
                && info.getPersistenceUnitRootUrl() != null) {
                URL url = info.getPersistenceUnitRootUrl();
                if ("file".equals(url.getProtocol())) // exploded jar?
                    file = URLDecoder.decode(url.getPath());
                else
                    jars.append(url);
            }
            for (URL jar : info.getJarFileUrls()) {
                if (jars.length() > 0)
                    jars.append(';');
                jars.append(jar);
            }
            if (file != null)
                metaFactoryProps.put("Files", file);
            if (jars.length() != 0)
                metaFactoryProps.put("URLs", jars.toString());
        }
        if (info.getMappingFileNames() != null
            && !info.getMappingFileNames().isEmpty()) {
            StringBuffer rsrcs = new StringBuffer();
            for (String rsrc : info.getMappingFileNames()) {
                if (rsrcs.length() > 0)
                    rsrcs.append(';');
                rsrcs.append(rsrc);
            }
            metaFactoryProps.put("Resources", rsrcs.toString());
        }
        if (!metaFactoryProps.isEmpty()) {
            // set persistent class locations as properties of metadata factory
            String factory =
                (String) map.get("openjpa.MetaDataFactory");
            if (factory == null)
                factory = Configurations.serializeProperties(metaFactoryProps);
            else {
                String clsName = Configurations.getClassName(factory);
                metaFactoryProps.putAll(Configurations.parseProperties
                    (Configurations.getProperties(factory)));
                factory = Configurations.getPlugin(clsName,
                    Configurations.serializeProperties(metaFactoryProps));
            }
            map.put("openjpa.MetaDataFactory", factory);
        }
        return map;
    }

    // --------------------

    public File getSourceFile() {
        if (_persistenceXmlFile == null)
            return null;

        try {
            return new File(_persistenceXmlFile.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public Object getSourceScope() {
        return null;
    }

    public int getSourceType() {
        return SRC_XML;
    }

    public String getResourceName() {
        return "PersistenceUnitInfo:" + _name;
    }

    /**
     * Simple class resolver built around the persistence unit loader.
     */
    private static class ClassResolverImpl
        implements ClassResolver {

        private final ClassLoader _loader;

        public ClassResolverImpl(ClassLoader loader) {
            _loader = loader;
        }

        public ClassLoader getClassLoader(Class ctx, ClassLoader env) {
            return _loader;
        }
	}
}
