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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.meta.ClassAnnotationMetaDataFilter;
import org.apache.openjpa.lib.meta.ClassArgParser;
import org.apache.openjpa.lib.meta.MetaDataFilter;
import org.apache.openjpa.lib.meta.MetaDataParser;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.AbstractCFMetaDataFactory;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.MetaDataDefaults;
import org.apache.openjpa.meta.MetaDataFactory;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.MetaDataException;

/**
 * {@link MetaDataFactory} for JPA metadata.
 *
 * @author Steve Kim
 * @since 0.4.0
 */
public class PersistenceMetaDataFactory
    extends AbstractCFMetaDataFactory
    implements Configurable {

    private static final Localizer _loc = Localizer.forPackage
        (PersistenceMetaDataFactory.class);

    private AnnotationPersistenceMetaDataParser _annoParser = null;
    private XMLPersistenceMetaDataParser _xmlParser = null;
    private PersistenceMetaDataDefaults _def = null;
    private Map<URL, Set> _xml = null; // xml rsrc -> class names
    private Set<URL> _unparsed = null; // xml rsrc
    private boolean _fieldOverride = true;
    private int _access = ClassMetaData.ACCESS_FIELD;

    /**
     * Whether to use field-level override or class-level override.
     * Defaults to true.
     */
    public void setFieldOverride(boolean field) {
        _fieldOverride = field;
    }

    /**
     * Whether to use field-level override or class-level override.
     * Defaults to true.
     */
    public boolean getFieldOverride() {
        return _fieldOverride;
    }

    /**
     * The default access type for base classes with ACCESS_UNKNOWN
     */
    public void setDefaultAccessType(String type) {
        if (type == null)
            return;
        if ("PROPERTY".equals(type.toUpperCase()))
            _access = ClassMetaData.ACCESS_PROPERTY;
        else
            _access = ClassMetaData.ACCESS_FIELD;
    }

    /**
     * Return metadata parser, creating it if it does not already exist.
     */
    public AnnotationPersistenceMetaDataParser getAnnotationParser() {
        if (_annoParser == null) {
            _annoParser = newAnnotationParser();
            _annoParser.setRepository(repos);
        }
        return _annoParser;
    }

    /**
     * Set the metadata parser.
     */
    public void setAnnotationParser(
        AnnotationPersistenceMetaDataParser parser) {
        if (_annoParser != null)
            _annoParser.setRepository(null);
        if (parser != null)
            parser.setRepository(repos);
        _annoParser = parser;
    }

    /**
     * Create a new metadata parser.
     */
    protected AnnotationPersistenceMetaDataParser newAnnotationParser() {
        return new AnnotationPersistenceMetaDataParser
            (repos.getConfiguration());
    }

    /**
     * Return XML metadata parser, creating it if it does not already exist.
     */
    public XMLPersistenceMetaDataParser getXMLParser() {
        if (_xmlParser == null) {
            _xmlParser = newXMLParser(true);
            _xmlParser.setRepository(repos);
            if (_fieldOverride)
                _xmlParser.setAnnotationParser(getAnnotationParser());
        }
        return _xmlParser;
    }

    /**
     * Set the metadata parser.
     */
    public void setXMLParser(XMLPersistenceMetaDataParser parser) {
        if (_xmlParser != null)
            _xmlParser.setRepository(null);
        if (parser != null)
            parser.setRepository(repos);
        _xmlParser = parser;
    }

    /**
     * Create a new metadata parser.
     */
    protected XMLPersistenceMetaDataParser newXMLParser(boolean loading) {
        return new XMLPersistenceMetaDataParser(repos.getConfiguration());
    }

    /**
     * Create a new serializer
     */
    protected XMLPersistenceMetaDataSerializer newXMLSerializer() {
        return new XMLPersistenceMetaDataSerializer(repos.getConfiguration());
    }

    public void load(Class cls, int mode, ClassLoader envLoader) {
        if (mode == MODE_NONE)
            return;
        if (!strict && (mode & MODE_META) != 0)
            mode |= MODE_MAPPING;

        // getting the list of persistent types runs callbacks to
        // mapPersistentTypeNames if it hasn't been called already, which
        // caches XML resources
        getPersistentTypeNames(false, envLoader);
        URL xml = findXML(cls);

        // we have to parse metadata up-front to register persistence unit
        // defaults and system callbacks
        ClassMetaData meta;
        boolean parsedXML = false;
        if (_unparsed != null && !_unparsed.isEmpty()
            && (mode & MODE_META) != 0) {
            for (URL url : _unparsed)
                parseXML(url, cls, mode, envLoader);
            parsedXML = _unparsed.contains(xml);
            _unparsed.clear();

            // XML process check
            meta = repos.getCachedMetaData(cls);
            if (meta != null && (meta.getSourceMode() & mode) == mode) {
                validateStrategies(meta);
                return;
            }
        }

        // might have been looking for system-level query
        if (cls == null)
            return;

        // we may still need to parse XML if this is a redeploy of a class, or
        // if we're in strict query-only mode
        if (!parsedXML && xml != null) {
            parseXML(xml, cls, mode, envLoader);
            // XML process check
            meta = repos.getCachedMetaData(cls);
            if (meta != null && (meta.getSourceMode() & mode) == mode) {
                validateStrategies(meta);
                return;
            }
        }

        AnnotationPersistenceMetaDataParser parser = getAnnotationParser();
        parser.setEnvClassLoader(envLoader);
        parser.setMode(mode);
        parser.parse(cls);

        meta = repos.getCachedMetaData(cls);
        if (meta != null && (meta.getSourceMode() & mode) == mode)
            validateStrategies(meta);
    }

    /**
     * Parse the given XML resource.
     */
    private void parseXML(URL xml, Class cls, int mode, ClassLoader envLoader) {
        ClassLoader loader = repos.getConfiguration().
            getClassResolverInstance().getClassLoader(cls, envLoader);
        XMLPersistenceMetaDataParser xmlParser = getXMLParser();
        xmlParser.setClassLoader(loader);
        xmlParser.setEnvClassLoader(envLoader);
        xmlParser.setMode(mode);
        try {
            xmlParser.parse(xml);
        } catch (IOException ioe) {
            throw new GeneralException(ioe);
        }
    }

    /**
     * Locate the XML resource for the given class.
     */
    private URL findXML(Class cls) {
        if (_xml != null && cls != null)
            for (Map.Entry<URL, Set> entry : _xml.entrySet())
                if (entry.getValue().contains(cls.getName()))
                    return entry.getKey();
        return null;
    }

    @Override
    protected void mapPersistentTypeNames(Object rsrc, String[] names) {
        if (!(rsrc instanceof URL) || rsrc.toString().endsWith(".class"))
            return;

        if (_xml == null)
            _xml = new HashMap<URL, Set>();
        _xml.put((URL) rsrc, new HashSet(Arrays.asList(names)));
        if (_unparsed == null)
            _unparsed = new HashSet<URL>();
        _unparsed.add((URL) rsrc);
    }

    @Override
    public Class getQueryScope(String queryName, ClassLoader loader) {
        if (queryName == null)
            return null;
        Collection classes = repos.loadPersistentTypes(false, loader);
        for (Class cls : (Collection<Class>) classes) {
            if (cls.isAnnotationPresent(NamedQuery.class) && hasNamedQuery
                (queryName, (NamedQuery) cls.getAnnotation(NamedQuery.class)))
                return cls;
            if (cls.isAnnotationPresent(NamedQueries.class) &&
                hasNamedQuery(queryName, ((NamedQueries) cls.getAnnotation
                    (NamedQueries.class)).value()))
                return cls;
        }
        return null;
    }

    private boolean hasNamedQuery(String query, NamedQuery... queries) {
        for (NamedQuery q : queries) {
            if (query.equals(q.name()))
                return true;
        }
        return false;
    }

    @Override
    protected MetaDataFilter newMetaDataFilter() {
        return new ClassAnnotationMetaDataFilter(new Class[]{
            Entity.class, Embeddable.class, MappedSuperclass.class });
    }

    /**
     * Ensure all fields have declared a strategy.
     */
    private void validateStrategies(ClassMetaData meta) {
        StringBuffer buf = null;
        for (FieldMetaData fmd : meta.getDeclaredFields()) {
            if (!fmd.isExplicit()) {
                if (buf == null)
                    buf = new StringBuffer();
                else
                    buf.append(", ");
                buf.append(fmd);
            }
        }
        if (buf != null)
            throw new MetaDataException(_loc.get("no-pers-strat", buf));
    }

    public MetaDataDefaults getDefaults() {
        if (_def == null) {
            _def = new PersistenceMetaDataDefaults();
            _def.setDefaultAccessType(_access);
        }
        return _def;
    }

    @Override
    public ClassArgParser newClassArgParser() {
        ClassArgParser parser = new ClassArgParser();
        parser.setMetaDataStructure("package", null, new String[]{
            "entity", "embeddable", "mapped-superclass" }, "class");
        return parser;
    }

    @Override
    public void clear() {
        super.clear();
        if (_annoParser != null)
            _annoParser.clear();
        if (_xmlParser != null)
            _xmlParser.clear();
        if (_xml != null)
            _xml.clear();
    }

    protected Parser newParser(boolean loading) {
        return newXMLParser(loading);
    }

    protected Serializer newSerializer() {
        return newXMLSerializer();
    }

    @Override
    protected void parse(MetaDataParser parser, Class[] cls) {
        parse(parser, Collections.singleton(defaultXMLFile()));
    }

    protected File defaultSourceFile(ClassMetaData meta) {
        return defaultXMLFile();
    }

    protected File defaultSourceFile(QueryMetaData query, Map clsNames) {
        ClassMetaData meta = getDefiningMetaData(query, clsNames);
        File file = (meta == null) ? null : meta.getSourceFile();
        if (file != null)
            return file;
        return defaultXMLFile();
    }

    protected File defaultSourceFile(SequenceMetaData seq, Map clsNames) {
        return defaultXMLFile();
    }

    /**
     * Look for META-INF/orm.xml, and if it doesn't exist, choose a default.
     */
    private File defaultXMLFile() {
        ClassLoader loader = repos.getConfiguration().
            getClassResolverInstance().getClassLoader(getClass(), null);
        URL rsrc = loader.getResource("META-INF/orm.xml");
        if (rsrc != null) {
            File file = new File(rsrc.getFile());
            if (file.exists())
                return file;
        }
        return new File("orm.xml");
    }

    public void setConfiguration(Configuration conf) {
    }

    public void startConfiguration() {
    }

    public void endConfiguration() {
        if (rsrcs == null)
            rsrcs = Collections.singleton("META-INF/orm.xml");
        else
			rsrcs.add ("META-INF/orm.xml");
	}
}
