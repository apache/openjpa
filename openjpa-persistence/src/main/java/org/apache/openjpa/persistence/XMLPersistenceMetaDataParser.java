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

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.persistence.CascadeType;
import javax.persistence.GenerationType;
import static javax.persistence.CascadeType.*;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.apache.openjpa.event.BeanLifecycleCallbacks;
import org.apache.openjpa.event.LifecycleCallbacks;
import org.apache.openjpa.event.LifecycleEvent;
import org.apache.openjpa.event.MethodLifecycleCallbacks;
import org.apache.openjpa.kernel.QueryLanguages;
import org.apache.openjpa.kernel.jpql.JPQLParser;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.meta.CFMetaDataParser;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.DelegatingMetaDataFactory;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.LifecycleMetaData;
import org.apache.openjpa.meta.MetaDataDefaults;
import org.apache.openjpa.meta.MetaDataFactory;
import static org.apache.openjpa.meta.MetaDataModes.*;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.Order;
import org.apache.openjpa.meta.QueryMetaData;
import org.apache.openjpa.meta.SequenceMetaData;
import org.apache.openjpa.meta.ValueMetaData;
import static org.apache.openjpa.persistence.MetaDataTag.*;
import static org.apache.openjpa.persistence.PersistenceStrategy.*;
import org.apache.openjpa.util.ImplHelper;

import serp.util.Numbers;

/**
 * Custom SAX parser used by the system to quickly parse persistence i
 * metadata files.
 *
 * @author Steve Kim
 * @nojavadoc
 */
public class XMLPersistenceMetaDataParser
    extends CFMetaDataParser
    implements PersistenceMetaDataFactory.Parser {

    // parse constants
    protected static final String ELEM_PKG = "package";
    protected static final String ELEM_ACCESS = "access";
    protected static final String ELEM_ATTRS = "attributes";
    protected static final String ELEM_LISTENER = "entity-listener";
    protected static final String ELEM_CASCADE = "cascade";
    protected static final String ELEM_CASCADE_ALL = "cascade-all";
    protected static final String ELEM_CASCADE_PER = "cascade-persist";
    protected static final String ELEM_CASCADE_MER = "cascade-merge";
    protected static final String ELEM_CASCADE_REM = "cascade-remove";
    protected static final String ELEM_CASCADE_REF = "cascade-refresh";
    protected static final String ELEM_PU_META = "persistence-unit-metadata";
    protected static final String ELEM_PU_DEF = "persistence-unit-defaults";
    protected static final String ELEM_XML_MAP_META_COMPLETE =
        "xml-mapping-metadata-complete";

    private static final Map<String, Object> _elems =
        new HashMap<String, Object>();

    static {
        _elems.put(ELEM_PKG, ELEM_PKG);
        _elems.put(ELEM_ACCESS, ELEM_ACCESS);
        _elems.put(ELEM_ATTRS, ELEM_ATTRS);
        _elems.put(ELEM_LISTENER, ELEM_LISTENER);
        _elems.put(ELEM_CASCADE, ELEM_CASCADE);
        _elems.put(ELEM_CASCADE_ALL, ELEM_CASCADE_ALL);
        _elems.put(ELEM_CASCADE_PER, ELEM_CASCADE_PER);
        _elems.put(ELEM_CASCADE_REM, ELEM_CASCADE_REM);
        _elems.put(ELEM_CASCADE_MER, ELEM_CASCADE_MER);
        _elems.put(ELEM_CASCADE_REF, ELEM_CASCADE_REF);
        _elems.put(ELEM_PU_META, ELEM_PU_META);
        _elems.put(ELEM_PU_DEF, ELEM_PU_DEF);
        _elems.put(ELEM_XML_MAP_META_COMPLETE, ELEM_XML_MAP_META_COMPLETE);

        _elems.put("entity-listeners", ENTITY_LISTENERS);
        _elems.put("pre-persist", PRE_PERSIST);
        _elems.put("post-persist", POST_PERSIST);
        _elems.put("pre-remove", PRE_REMOVE);
        _elems.put("post-remove", POST_REMOVE);
        _elems.put("pre-update", PRE_UPDATE);
        _elems.put("post-update", POST_UPDATE);
        _elems.put("post-load", POST_LOAD);
        _elems.put("exclude-default-listeners", EXCLUDE_DEFAULT_LISTENERS);
        _elems.put("exclude-superclass-listeners",
            EXCLUDE_SUPERCLASS_LISTENERS);

        _elems.put("named-query", QUERY);
        _elems.put("named-native-query", NATIVE_QUERY);
        _elems.put("query-hint", QUERY_HINT);
        _elems.put("query", QUERY_STRING);

        _elems.put("flush-mode", FLUSH_MODE);
        _elems.put("sequence-generator", SEQ_GENERATOR);

        _elems.put("id", ID);
        _elems.put("id-class", ID_CLASS);
        _elems.put("embedded-id", EMBEDDED_ID);
        _elems.put("version", VERSION);
        _elems.put("generated-value", GENERATED_VALUE);
        _elems.put("map-key", MAP_KEY);
        _elems.put("order-by", ORDER_BY);
        _elems.put("lob", LOB);

        _elems.put("basic", BASIC);
        _elems.put("many-to-one", MANY_ONE);
        _elems.put("one-to-one", ONE_ONE);
        _elems.put("embedded", EMBEDDED);
        _elems.put("one-to-many", ONE_MANY);
        _elems.put("many-to-many", MANY_MANY);
        _elems.put("transient", TRANSIENT);
    }

    private static final Localizer _loc = Localizer.forPackage
        (XMLPersistenceMetaDataParser.class);

    private final OpenJPAConfiguration _conf;
    private MetaDataRepository _repos = null;
    private AnnotationPersistenceMetaDataParser _parser = null;
    private ClassLoader _envLoader = null;
    private int _mode = MODE_NONE;
    private boolean _override = false;

    private final Stack _elements = new Stack();
    private final Stack _parents = new Stack();

    private Class _cls = null;
    private int _fieldPos = 0;
    private int _clsPos = 0;
    private int _access = ClassMetaData.ACCESS_UNKNOWN;
    private PersistenceStrategy _strategy = null;
    private Set<CascadeType> _cascades = null;
    private Set<CascadeType> _pkgCascades = null;

    private Class _listener = null;
    private Collection<LifecycleCallbacks>[] _callbacks = null;
    private int[] _highs = null;

    /**
     * Constructor; supply configuration.
     */
    public XMLPersistenceMetaDataParser(OpenJPAConfiguration conf) {
        _conf = conf;
        setValidating(true);
        setLog(conf.getLog(OpenJPAConfiguration.LOG_METADATA));
        setParseComments(true);
        setMode(MODE_META | MODE_MAPPING | MODE_QUERY);
        setParseText(true);
    }

    /**
     * Configuration supplied on construction.
     */
    public OpenJPAConfiguration getConfiguration() {
        return _conf;
    }

    /**
     * The annotation parser. When class is discovered in an XML file,
     * we first parse any annotations present, then override with the XML.
     */
    public AnnotationPersistenceMetaDataParser getAnnotationParser() {
        return _parser;
    }

    /**
     * The annotation parser. When class is discovered in an XML file,
     * we first parse any annotations present, then override with the XML.
     */
    public void setAnnotationParser(AnnotationPersistenceMetaDataParser parser){
        _parser = parser;
    }

    /**
     * Returns the repository for this parser. If none has been set, creates
     * a new repository and sets it.
     */
    public MetaDataRepository getRepository() {
        if (_repos == null) {
            MetaDataRepository repos = _conf.newMetaDataRepositoryInstance();
            MetaDataFactory mdf = repos.getMetaDataFactory();
            if (mdf instanceof DelegatingMetaDataFactory)
                mdf = ((DelegatingMetaDataFactory) mdf).getInnermostDelegate();
            if (mdf instanceof PersistenceMetaDataFactory)
                ((PersistenceMetaDataFactory) mdf).setXMLParser(this);
            _repos = repos;
        }
        return _repos;
    }

    /**
     * Set the metadata repository for this parser.
     */
    public void setRepository(MetaDataRepository repos) {
        _repos = repos;
        if (repos != null
            && (repos.getValidate() & repos.VALIDATE_RUNTIME) != 0)
            setParseComments(false);
    }

    /**
     * Return the environmental class loader to pass on to parsed
     * metadata instances.
     */
    public ClassLoader getEnvClassLoader() {
        return _envLoader;
    }

    /**
     * Set the environmental class loader to pass on to parsed
     * metadata instances.
     */
    public void setEnvClassLoader(ClassLoader loader) {
        _envLoader = loader;
    }

    /**
     * Whether to allow later parses of mapping information to override
     * earlier information for the same class. Defaults to false. Useful
     * when a tool is mapping a class, so that .jdo file partial mapping
     * information can be used even when mappings are stored in .orm files
     * or other locations.
     */
    public boolean getMappingOverride() {
        return _override;
    }

    /**
     * Whether to allow later parses of mapping information to override
     * earlier information for the same class. Defaults to false. Useful
     * when a tool is mapping a class, so that .jdo file partial mapping
     * information can be used even when mappings are stored in .orm files
     * or other locations.
     */
    public void setMappingOverride(boolean override) {
        _override = override;
    }

    /**
     * The parse mode according to the expected document type. The
     * mode constants act as bit flags, and therefore can be combined.
     */
    public int getMode() {
        return _mode;
    }

    /**
     * The parse mode according to the expected document type.
     */
    public void setMode(int mode, boolean on) {
        if (mode == MODE_NONE)
            setMode(MODE_NONE);
        else if (on)
            setMode(_mode | mode);
        else
            setMode(_mode & ~mode);
    }

    /**
     * The parse mode according to the expected document type.
     */
    public void setMode(int mode) {
        _mode = mode;
        if (_parser != null)
            _parser.setMode(mode);
    }

    /**
     * Convenience method for interpreting {@link #getMode}.
     */
    protected boolean isMetaDataMode() {
        return (_mode & MODE_META) != 0;
    }

    /**
     * Convenience method for interpreting {@link #getMode}.
     */
    protected boolean isQueryMode() {
        return (_mode & MODE_QUERY) != 0;
    }

    /**
     * Convenience method for interpreting {@link #getMode}.
     */
    protected boolean isMappingMode() {
        return (_mode & MODE_MAPPING) != 0;
    }

    /**
     * Returns true if we're in mapping mode or in metadata mode with
     * mapping override enabled.
     */
    protected boolean isMappingOverrideMode() {
        return isMappingMode() || (_override && isMetaDataMode());
    }

    ///////////////
    // XML parsing
    ///////////////

    /**
     * Push a parse element onto the stack.
     */
    protected void pushElement(Object elem) {
        _elements.push(elem);
    }

    /**
     * Pop a parse element from the stack.
     */
    protected Object popElement() {
        return _elements.pop();
    }

    /**
     * Return the current element being parsed. May be a class metadata,
     * field metadata, query metadata, etc.
     */
    protected Object currentElement() {
        if (_elements.isEmpty())
            return null;
        return _elements.peek();
    }

    /**
     * Return the current {@link PersistenceStrategy} if any.
     */
    protected PersistenceStrategy currentStrategy() {
        return _strategy;
    }

    /**
     * Return the tag of the current parent element.
     */
    protected Object currentParent() {
        if (_parents.isEmpty())
            return null;
        return _parents.peek();
    }

    /**
     * Return whether we're running the parser at runtime.
     */
    protected boolean isRuntime() {
        return (getRepository().getValidate()
            & MetaDataRepository.VALIDATE_RUNTIME) != 0;
    }

    @Override
    protected Object getSchemaSource() {
        return XMLPersistenceMetaDataParser.class.getResourceAsStream
            ("orm-xsd.rsrc");
    }

    @Override
    protected String getPackageAttributeName() {
        return null;
    }

    @Override
    protected String getClassAttributeName() {
        return "class";
    }

    @Override
    protected int getClassElementDepth() {
        return 1;
    }

    @Override
    protected boolean isClassElementName(String name) {
        return "entity".equals(name)
            || "embeddable".equals(name)
            || "mapped-superclass".equals(name);
    }

    @Override
    protected void reset() {
        super.reset();
        _elements.clear();
        _parents.clear();
        _cls = null;
        _fieldPos = 0;
        _clsPos = 0;

        _access = ClassMetaData.ACCESS_UNKNOWN;
        _strategy = null;
        _listener = null;
        _callbacks = null;
        _highs = null;
        _cascades = null;
        _pkgCascades = null;
    }

    @Override
    protected boolean startSystemElement(String name, Attributes attrs)
        throws SAXException {
        Object tag = (Object) _elems.get(name);
        boolean ret = false;
        if (tag == null) {
            if (isMappingOverrideMode())
                tag = startSystemMappingElement(name, attrs);
            ret = tag != null;
        } else if (tag instanceof MetaDataTag) {
            switch ((MetaDataTag) tag) {
                case QUERY:
                    ret = startNamedQuery(attrs);
                    break;
                case QUERY_HINT:
                    ret = startQueryHint(attrs);
                    break;
                case NATIVE_QUERY:
                    ret = startNamedNativeQuery(attrs);
                    break;
                case QUERY_STRING:
                    ret = startQueryString(attrs);
                    break;
                case SEQ_GENERATOR:
                    ret = startSequenceGenerator(attrs);
                    break;
                case FLUSH_MODE:
                    ret = startFlushMode(attrs);
                    break;
                case ENTITY_LISTENERS:
                    ret = startEntityListeners(attrs);
                    break;
                case PRE_PERSIST:
                case POST_PERSIST:
                case PRE_REMOVE:
                case POST_REMOVE:
                case PRE_UPDATE:
                case POST_UPDATE:
                case POST_LOAD:
                    ret = startCallback((MetaDataTag) tag, attrs);
                    break;
                default:
                    warnUnsupportedTag(name);
            }
        } else if (tag == ELEM_PU_META || tag == ELEM_PU_DEF)
            ret = isMetaDataMode();
        else if (tag == ELEM_XML_MAP_META_COMPLETE)
            setAnnotationParser(null);
        else if (tag == ELEM_ACCESS)
            ret = _mode != MODE_QUERY;
        else if (tag == ELEM_LISTENER)
            ret = startEntityListener(attrs);
        else if (tag == ELEM_CASCADE)
            ret = isMetaDataMode();
        else if (tag == ELEM_CASCADE_ALL || tag == ELEM_CASCADE_PER
            || tag == ELEM_CASCADE_MER || tag == ELEM_CASCADE_REM
            || tag == ELEM_CASCADE_REF)
            ret = startCascade(tag, attrs);

        if (ret)
            _parents.push(tag);
        return ret;
    }

    @Override
    protected void endSystemElement(String name)
        throws SAXException {
        Object tag = _elems.get(name);
        if (tag == null && isMappingOverrideMode())
            endSystemMappingElement(name);
        else if (tag instanceof MetaDataTag) {
            switch ((MetaDataTag) tag) {
                case QUERY:
                    endNamedQuery();
                    break;
                case QUERY_HINT:
                    endQueryHint();
                    break;
                case NATIVE_QUERY:
                    endNamedNativeQuery();
                    break;
                case QUERY_STRING:
                    endQueryString();
                    break;
                case SEQ_GENERATOR:
                    endSequenceGenerator();
                    break;
            }
        } else if (tag == ELEM_ACCESS)
            endAccess();
        else if (tag == ELEM_LISTENER)
            endEntityListener();

        _parents.pop();
    }

    /**
     * Implement to parse a mapping element outside of any class.
     *
     * @return the tag for the given element, or null to skip the element
     */
    protected Object startSystemMappingElement(String name, Attributes attrs)
        throws SAXException {
        return null;
    }

    /**
     * Implement to parse a mapping element outside of any class.
     */
    protected void endSystemMappingElement(String name)
        throws SAXException {
    }

    @Override
    protected boolean startClassElement(String name, Attributes attrs)
        throws SAXException {
        Object tag = (Object) _elems.get(name);
        boolean ret = false;
        if (tag == null) {
            if (isMappingOverrideMode())
                tag = startClassMappingElement(name, attrs);
            ret = tag != null;
        } else if (tag instanceof MetaDataTag) {
            switch ((MetaDataTag) tag) {
                case GENERATED_VALUE:
                    ret = startGeneratedValue(attrs);
                    break;
                case ID:
                    ret = startId(attrs);
                    break;
                case EMBEDDED_ID:
                    ret = startEmbeddedId(attrs);
                    break;
                case ID_CLASS:
                    ret = startIdClass(attrs);
                    break;
                case LOB:
                    ret = startLob(attrs);
                    break;
                case QUERY:
                    ret = startNamedQuery(attrs);
                    break;
                case QUERY_HINT:
                    ret = startQueryHint(attrs);
                    break;
                case NATIVE_QUERY:
                    ret = startNamedNativeQuery(attrs);
                    break;
                case QUERY_STRING:
                    ret = startQueryString(attrs);
                    break;
                case SEQ_GENERATOR:
                    ret = startSequenceGenerator(attrs);
                    break;
                case VERSION:
                    ret = startVersion(attrs);
                    break;
                case MAP_KEY:
                    ret = startMapKey(attrs);
                    break;
                case FLUSH_MODE:
                    ret = startFlushMode(attrs);
                    break;
                case ORDER_BY:
                case ENTITY_LISTENERS:
                    ret = isMetaDataMode();
                    break;
                case EXCLUDE_DEFAULT_LISTENERS:
                    ret = startExcludeDefaultListeners(attrs);
                    break;
                case EXCLUDE_SUPERCLASS_LISTENERS:
                    ret = startExcludeSuperclassListeners(attrs);
                    break;
                case PRE_PERSIST:
                case POST_PERSIST:
                case PRE_REMOVE:
                case POST_REMOVE:
                case PRE_UPDATE:
                case POST_UPDATE:
                case POST_LOAD:
                    ret = startCallback((MetaDataTag) tag, attrs);
                    break;
                default:
                    warnUnsupportedTag(name);
            }
        } else if (tag instanceof PersistenceStrategy) {
            PersistenceStrategy ps = (PersistenceStrategy) tag;
            ret = startStrategy(ps, attrs);
            if (ret)
                _strategy = ps;
        } else if (tag == ELEM_LISTENER)
            ret = startEntityListener(attrs);
        else if (tag == ELEM_ATTRS)
            ret = _mode != MODE_QUERY;
        else if (tag == ELEM_CASCADE)
            ret = isMetaDataMode();
        else if (tag == ELEM_CASCADE_ALL || tag == ELEM_CASCADE_PER
            || tag == ELEM_CASCADE_MER || tag == ELEM_CASCADE_REM
            || tag == ELEM_CASCADE_REF)
            ret = startCascade(tag, attrs);

        if (ret)
            _parents.push(tag);
        return ret;
    }

    @Override
    protected void endClassElement(String name)
        throws SAXException {
        Object tag = _elems.get(name);
        if (tag == null && isMappingOverrideMode())
            endClassMappingElement(name);
        else if (tag instanceof MetaDataTag) {
            switch ((MetaDataTag) tag) {
                case GENERATED_VALUE:
                    endGeneratedValue();
                    break;
                case ID:
                    endId();
                    break;
                case EMBEDDED_ID:
                    endEmbeddedId();
                    break;
                case ID_CLASS:
                    endIdClass();
                    break;
                case LOB:
                    endLob();
                    break;
                case QUERY:
                    endNamedQuery();
                    break;
                case QUERY_HINT:
                    endQueryHint();
                    break;
                case NATIVE_QUERY:
                    endNamedNativeQuery();
                    break;
                case QUERY_STRING:
                    endQueryString();
                    break;
                case SEQ_GENERATOR:
                    endSequenceGenerator();
                    break;
                case VERSION:
                    endVersion();
                    break;
                case ORDER_BY:
                    endOrderBy();
                    break;
            }
        } else if (tag instanceof PersistenceStrategy)
            endStrategy((PersistenceStrategy) tag);
        else if (tag == ELEM_ACCESS)
            endAccess();
        else if (tag == ELEM_LISTENER)
            endEntityListener();

        _parents.pop();
    }

    /**
     * Log warning about an unsupported tag.
     */
    private void warnUnsupportedTag(String name) {
        Log log = getLog();
        if (log.isInfoEnabled())
            log.trace(_loc.get("unsupported-tag", name));
    }

    /**
     * Implement to parse a mapping element within a class.
     *
     * @return the tag for the given element, or null to skip element
     */
    protected Object startClassMappingElement(String name, Attributes attrs)
        throws SAXException {
        return null;
    }

    /**
     * Implement to parse a mapping element within a class.
     */
    protected void endClassMappingElement(String name)
        throws SAXException {
    }

    @Override
    protected boolean startClass(String elem, Attributes attrs)
        throws SAXException {
        super.startClass(elem, attrs);

        // query mode only?
        _cls = classForName(currentClassName());
        if (_mode == MODE_QUERY) {
            if (_parser != null &&
                !"true".equals(attrs.getValue("metadata-complete")))
                _parser.parse(_cls);
            return true;
        }

        Log log = getLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("parse-class", _cls.getName()));

        MetaDataRepository repos = getRepository();
        ClassMetaData meta = repos.getCachedMetaData(_cls);
        if (meta != null
            && ((isMetaDataMode() && (meta.getSourceMode() & MODE_META) != 0)
            || (isMappingMode() && (meta.getSourceMode() & MODE_MAPPING) != 0)))
        {
            if (log.isWarnEnabled())
                log.warn(_loc.get("dup-metadata", _cls, getSourceName()));
            _cls = null;
            return false;
        }

        // if we don't know the access type, check to see if a superclass
        // has already defined the access type
        int defaultAccess = _access;
        if (defaultAccess == ClassMetaData.ACCESS_UNKNOWN) {
            ClassMetaData sup = repos.getCachedMetaData(_cls.getSuperclass());
            if (sup != null)
                defaultAccess = sup.getAccessType();
        }

        if (meta == null) {
            // add metadata for this type
            int access = toAccessType(attrs.getValue("access"), defaultAccess);
            meta = repos.addMetaData(_cls, access);
            meta.setEnvClassLoader(_envLoader);
            meta.setSourceMode(MODE_NONE);

            // parse annotations first so XML overrides them
            if (_parser != null &&
                !"true".equals(attrs.getValue("metadata-complete")))
                _parser.parse(_cls);
        }

        boolean mappedSuper = "mapped-superclass".equals(elem);
        if (isMetaDataMode()) {
            meta.setSource(getSourceFile(), meta.SRC_XML);
            meta.setSourceMode(MODE_META, true);
            Locator locator = getLocation().getLocator();
            if (locator != null) {
                meta.setLineNumber(Numbers.valueOf(locator.getLineNumber()));
                meta.setColNumber(Numbers.valueOf(locator.getColumnNumber()));
            }
            meta.setListingIndex(_clsPos);
            String name = attrs.getValue("name");
            if (!StringUtils.isEmpty(name))
                meta.setTypeAlias(name);
            meta.setAbstract(mappedSuper);
            meta.setEmbeddedOnly(mappedSuper || "embeddable".equals(elem));
            if (mappedSuper)
                meta.setIdentityType(meta.ID_UNKNOWN);
        }
        if (isMappingMode())
            meta.setSourceMode(MODE_MAPPING, true);
        if (isMappingOverrideMode())
            startClassMapping(meta, mappedSuper, attrs);
        if (isQueryMode())
            meta.setSourceMode(MODE_QUERY, true);

        _clsPos++;
        _fieldPos = 0;
        addComments(meta);
        pushElement(meta);
        return true;
    }

    @Override
    protected void endClass(String elem)
        throws SAXException {
        if (_mode != MODE_QUERY) {
            ClassMetaData meta = (ClassMetaData) popElement();
            storeCallbacks(meta);
            if (isMappingOverrideMode())
                endClassMapping(meta);
        }
        _cls = null;
        super.endClass(elem);
    }

    /**
     * Implement to add mapping attributes to class.
     */
    protected void startClassMapping(ClassMetaData mapping,
        boolean mappedSuper, Attributes attrs)
        throws SAXException {
    }

    /**
     * Implement to finalize class mapping.
     */
    protected void endClassMapping(ClassMetaData mapping)
        throws SAXException {
    }

    /**
     * Default access element.
     */
    private void endAccess() {
        _access = toAccessType(currentText(), ClassMetaData.ACCESS_UNKNOWN);
    }

    /**
     * Parse the given string as an entity access type, defaulting to given
     * default if string is empty.
     */
    private int toAccessType(String str, int def) {
        if (StringUtils.isEmpty(str))
            return def;
        if ("PROPERTY".equals(str))
            return ClassMetaData.ACCESS_PROPERTY;
        return ClassMetaData.ACCESS_FIELD;
    }

    /**
     * Parse flush-mode element.
     */
    private boolean startFlushMode(Attributes attrs)
        throws SAXException {
        Log log = getLog();
        if (log.isWarnEnabled())
            log.warn(_loc.get("unsupported", "flush-mode", getSourceName()));
        return false;
    }

    /**
     * Parse sequence-generator.
     */
    protected boolean startSequenceGenerator(Attributes attrs) {
        if (!isMappingOverrideMode())
            return false;

        String name = attrs.getValue("name");
        Log log = getLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("parse-sequence", name));

        SequenceMetaData meta = getRepository().getCachedSequenceMetaData(name);
        if (meta != null && log.isWarnEnabled())
            log.warn(_loc.get("override-sequence", name));

        meta = getRepository().addSequenceMetaData(name);
        String seq = attrs.getValue("sequence-name");
        String val = attrs.getValue("initial-value");
        int initial = val == null ? 1 : Integer.parseInt(val);
        val = attrs.getValue("allocation-size");
        int allocate = val == null ? 50 : Integer.parseInt(val);

        String clsName, props;
        if (seq == null || seq.indexOf('(') == -1) {
            clsName = SequenceMetaData.IMPL_NATIVE;
            props = null;
        } else { // plugin
            clsName = Configurations.getClassName(seq);
            props = Configurations.getProperties(seq);
            seq = null;
        }

        meta.setSequencePlugin(Configurations.getPlugin(clsName, props));
        meta.setSequence(seq);
        meta.setInitialValue(initial);
        meta.setAllocate(allocate);

        Object cur = currentElement();
        Object scope = (cur instanceof ClassMetaData)
            ? ((ClassMetaData) cur).getDescribedType() : null;
        meta.setSource(getSourceFile(), scope, meta.SRC_XML);
        Locator locator = getLocation().getLocator();
        if (locator != null) {
            meta.setLineNumber(Numbers.valueOf(locator.getLineNumber()));
            meta.setColNumber(Numbers.valueOf(locator.getColumnNumber()));
        }
        return true;
    }

    protected void endSequenceGenerator() {
    }

    /**
     * Parse id.
     */
    protected boolean startId(Attributes attrs)
        throws SAXException {
        FieldMetaData fmd = parseField(attrs);
        fmd.setExplicit(true);
        fmd.setPrimaryKey(true);
        return true;
    }

    protected void endId()
        throws SAXException {
        finishField();
    }

    /**
     * Parse embedded-id.
     */
    protected boolean startEmbeddedId(Attributes attrs)
        throws SAXException {
        FieldMetaData fmd = parseField(attrs);
        fmd.setExplicit(true);
        fmd.setPrimaryKey(true);
        fmd.setEmbedded(true);
        if (fmd.getEmbeddedMetaData() == null)
            fmd.addEmbeddedMetaData();
        return true;
    }

    protected void endEmbeddedId()
        throws SAXException {
        finishField();
    }

    /**
     * Parse id-class.
     */
    protected boolean startIdClass(Attributes attrs)
        throws SAXException {
        if (!isMetaDataMode())
            return false;

        ClassMetaData meta = (ClassMetaData) currentElement();
        String cls = attrs.getValue("class");
        Class idCls = null;
        try {
            idCls = classForName(cls);
        } catch (Throwable t) {
            throw getException(_loc.get("invalid-id-class", meta, cls), t);
        }
        meta.setObjectIdType(idCls, true);
        return true;
    }

    protected void endIdClass()
        throws SAXException {
    }

    /**
     * Parse lob.
     */
    protected boolean startLob(Attributes attrs)
        throws SAXException {
        FieldMetaData fmd = (FieldMetaData) currentElement();
        if (fmd.getDeclaredTypeCode() != JavaTypes.STRING
            && fmd.getDeclaredType() != char[].class
            && fmd.getDeclaredType() != Character[].class
            && fmd.getDeclaredType() != byte[].class
            && fmd.getDeclaredType() != Byte[].class)
            fmd.setSerialized(true);
        return true;
    }

    protected void endLob()
        throws SAXException {
    }

    /**
     * Parse generated-value.
     */
    protected boolean startGeneratedValue(Attributes attrs)
        throws SAXException {
        if (!isMappingOverrideMode())
            return false;

        String strategy = attrs.getValue("strategy");
        String generator = attrs.getValue("generator");
        GenerationType type = StringUtils.isEmpty(strategy)
            ? GenerationType.AUTO : GenerationType.valueOf(strategy);

        FieldMetaData fmd = (FieldMetaData) currentElement();
        AnnotationPersistenceMetaDataParser.parseGeneratedValue(fmd, type,
            generator);
        return true;
    }

    protected void endGeneratedValue()
        throws SAXException {
    }

    /**
     * Lazily parse cascades.
     */
    protected boolean startCascade(Object tag, Attributes attrs)
        throws SAXException {
        if (!isMetaDataMode())
            return false;

        Set<CascadeType> cascades = null;
        if (currentElement() instanceof FieldMetaData) {
            if (_cascades == null)
                _cascades = EnumSet.noneOf(CascadeType.class);
            cascades = _cascades;
        } else {
            if (_pkgCascades == null)
                _pkgCascades = EnumSet.noneOf(CascadeType.class);
            cascades = _pkgCascades;
        }
        boolean all = ELEM_CASCADE_ALL == tag;
        if (all || ELEM_CASCADE_PER == tag)
            cascades.add(PERSIST);
        if (all || ELEM_CASCADE_REM == tag)
            cascades.add(REMOVE);
        if (all || ELEM_CASCADE_MER == tag)
            cascades.add(MERGE);
        if (all || ELEM_CASCADE_REF == tag)
            cascades.add(REFRESH);
        return true;
    }

    /**
     * Set the cached cascades into the field.
     */
    protected void setCascades(FieldMetaData fmd) {
        Set<CascadeType> cascades = _cascades;
        if (_cascades == null)
            cascades = _pkgCascades;
        if (cascades == null)
            return;

        ValueMetaData vmd = fmd;
        switch (_strategy) {
            case ONE_MANY:
            case MANY_MANY:
                vmd = fmd.getElement();
        }
        for (CascadeType cascade : cascades) {
            switch (cascade) {
                case PERSIST:
                    vmd.setCascadePersist(ValueMetaData.CASCADE_IMMEDIATE);
                    break;
                case MERGE:
                    vmd.setCascadeAttach(ValueMetaData.CASCADE_IMMEDIATE);
                    break;
                case REMOVE:
                    vmd.setCascadeDelete(ValueMetaData.CASCADE_IMMEDIATE);
                    break;
                case REFRESH:
                    vmd.setCascadeRefresh(ValueMetaData.CASCADE_IMMEDIATE);
                    break;
            }
        }
        _cascades = null;
    }

    /**
     * Parse common field attributes.
     */
    private FieldMetaData parseField(Attributes attrs)
        throws SAXException {
        ClassMetaData meta = (ClassMetaData) currentElement();
        String name = attrs.getValue("name");
        FieldMetaData field = meta.getDeclaredField(name);
        if ((field == null || field.getDeclaredType() == Object.class)
            && meta.getDescribedType() != Object.class) {
            Member member = null;
            Class type = null;
            int def = _repos.getMetaDataFactory().getDefaults().
                getDefaultAccessType();
            try {
                if (meta.getAccessType() == ClassMetaData.ACCESS_PROPERTY
                    || (meta.getAccessType() == ClassMetaData.ACCESS_UNKNOWN
                    && def == ClassMetaData.ACCESS_PROPERTY)) {
                    String cap = StringUtils.capitalize(name);
                    type = meta.getDescribedType();
                    try {
                        member = (Method) AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                type, "get" + cap,
                                (Class[]) null));// varargs disambiguate
                    } catch (Exception excep) {
                        try {
                            member = (Method) AccessController.doPrivileged(
                                J2DoPrivHelper.getDeclaredMethodAction(
                                    type, "is" + cap, (Class[]) null));
                        } catch (Exception excep2) {
                            throw excep;
                        }
                    }
                    type = ((Method) member).getReturnType();
                } else {
                    member = (Field) AccessController.doPrivileged(
                        J2DoPrivHelper.getDeclaredFieldAction(
                            meta.getDescribedType(), name));
                    type = ((Field) member).getType();
                }
            } catch (Exception e) {
                if (e instanceof PrivilegedActionException)
                    e = ((PrivilegedActionException) e).getException();
                throw getException(_loc.get("invalid-attr", name, meta), e);
            }

            if (field == null) {
                field = meta.addDeclaredField(name, type);
                PersistenceMetaDataDefaults.setCascadeNone(field);
                PersistenceMetaDataDefaults.setCascadeNone(field.getKey());
                PersistenceMetaDataDefaults.setCascadeNone(field.getElement());
            }
            field.backingMember(member);
        } else if (field == null) {
            field = meta.addDeclaredField(name, Object.class);
            PersistenceMetaDataDefaults.setCascadeNone(field);
            PersistenceMetaDataDefaults.setCascadeNone(field.getKey());
            PersistenceMetaDataDefaults.setCascadeNone(field.getElement());
        }

        if (isMetaDataMode())
            field.setListingIndex(_fieldPos);

        _fieldPos++;
        pushElement(field);
        addComments(field);

        if (isMappingOverrideMode())
            startFieldMapping(field, attrs);
        return field;
    }

    /**
     * Pops field element.
     */
    private void finishField()
        throws SAXException {
        FieldMetaData field = (FieldMetaData) popElement();
        setCascades(field);
        if (isMappingOverrideMode())
            endFieldMapping(field);
        _strategy = null;
    }

    /**
     * Implement to add field mapping data. Does nothing by default.
     */
    protected void startFieldMapping(FieldMetaData field, Attributes attrs)
        throws SAXException {
    }

    /**
     * Implement to finalize field mapping. Does nothing by default.
     */
    protected void endFieldMapping(FieldMetaData field)
        throws SAXException {
    }

    /**
     * Parse version.
     */
    protected boolean startVersion(Attributes attrs)
        throws SAXException {
        FieldMetaData fmd = parseField(attrs);
        fmd.setExplicit(true);
        fmd.setVersion(true);
        return true;
    }

    protected void endVersion()
        throws SAXException {
        finishField();
    }

    /**
     * Parse strategy element.
     */
    private boolean startStrategy(PersistenceStrategy strategy,
        Attributes attrs)
        throws SAXException {
        FieldMetaData fmd = parseField(attrs);
        fmd.setExplicit(true);
        fmd.setManagement(FieldMetaData.MANAGE_PERSISTENT);

        String val = attrs.getValue("fetch");
        if (val != null)
            fmd.setInDefaultFetchGroup("EAGER".equals(val));
        val = attrs.getValue("optional");
        if ("false".equals(val))
            fmd.setNullValue(FieldMetaData.NULL_EXCEPTION);
        else if ("true".equals(val)
                && fmd.getNullValue() == FieldMetaData.NULL_EXCEPTION) {
            // Reset value if the field was annotated with optional=false. 
            // Otherwise leave it alone.
            fmd.setNullValue(FieldMetaData.NULL_UNSET);
        }
        if (isMappingOverrideMode()) {
            val = attrs.getValue("mapped-by");
            if (val != null)
                fmd.setMappedBy(val);
        }
        parseStrategy(fmd, strategy, attrs);
        return true;
    }

    private void endStrategy(PersistenceStrategy strategy)
        throws SAXException {
        finishField();
    }

    /**
     * Parse strategy specific attributes.
     */
    private void parseStrategy(FieldMetaData fmd,
        PersistenceStrategy strategy, Attributes attrs)
        throws SAXException {
        switch (strategy) {
            case BASIC:
                parseBasic(fmd, attrs);
                break;
            case EMBEDDED:
                parseEmbedded(fmd, attrs);
                break;
            case ONE_ONE:
                parseOneToOne(fmd, attrs);
                break;
            case MANY_ONE:
                parseManyToOne(fmd, attrs);
                break;
            case MANY_MANY:
                parseManyToMany(fmd, attrs);
                break;
            case ONE_MANY:
                parseOneToMany(fmd, attrs);
                break;
            case TRANSIENT:
                fmd.setManagement(FieldMetaData.MANAGE_NONE);
                break;
        }
    }

    /**
     * Parse basic.
     */
    protected void parseBasic(FieldMetaData fmd, Attributes attrs)
        throws SAXException {
    }

    /**
     * Parse embedded.
     */
    protected void parseEmbedded(FieldMetaData fmd, Attributes attrs)
        throws SAXException {
        assertPC(fmd, "Embedded");
        fmd.setEmbedded(true);
        fmd.setSerialized(false); // override any Lob annotation
        if (fmd.getEmbeddedMetaData() == null)
            fmd.addEmbeddedMetaData();
    }

    /**
     * Throw proper exception if given value is not possibly persistence
     * capable.
     */
    private void assertPC(FieldMetaData fmd, String attr)
        throws SAXException {
        if (!JavaTypes.maybePC(fmd))
            throw getException(_loc.get("bad-meta-anno", fmd, attr));
    }

    /**
     * Parse one-to-one.
     */
    protected void parseOneToOne(FieldMetaData fmd, Attributes attrs)
        throws SAXException {
        String val = attrs.getValue("target-entity");
        if (val != null)
            fmd.setTypeOverride(classForName(val));
        assertPC(fmd, "OneToOne");
        fmd.setSerialized(false); // override any Lob annotation
        if (!fmd.isDefaultFetchGroupExplicit())
            fmd.setInDefaultFetchGroup(true);
    }

    /**
     * Parse many-to-one.
     */
    protected void parseManyToOne(FieldMetaData fmd, Attributes attrs)
        throws SAXException {
        String val = attrs.getValue("target-entity");
        if (val != null)
            fmd.setTypeOverride(classForName(val));
        assertPC(fmd, "ManyToOne");
        fmd.setSerialized(false); // override any Lob annotation
        if (!fmd.isDefaultFetchGroupExplicit())
            fmd.setInDefaultFetchGroup(true);
    }

    /**
     * Parse many-to-many.
     */
    protected void parseManyToMany(FieldMetaData fmd, Attributes attrs)
        throws SAXException {
        String val = attrs.getValue("target-entity");
        if (val != null)
            fmd.getElement().setDeclaredType(classForName(val));
        assertPCCollection(fmd, "ManyToMany");
        fmd.setSerialized(false); // override Lob in annotation
    }

    /**
     * Throw exception if given field not a collection of possible persistence
     * capables.
     */
    private void assertPCCollection(FieldMetaData fmd, String attr)
        throws SAXException {
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.ARRAY:
            case JavaTypes.COLLECTION:
            case JavaTypes.MAP:
                if (JavaTypes.maybePC(fmd.getElement()))
                    break;
                // no break
            default:
                throw getException(_loc.get("bad-meta-anno", fmd, attr));
        }
    }

    /**
     * Parse one-to-many.
     */
    protected void parseOneToMany(FieldMetaData fmd, Attributes attrs)
        throws SAXException {
        String val = attrs.getValue("target-entity");
        if (val != null)
            fmd.getElement().setDeclaredType(classForName(val));
        assertPCCollection(fmd, "OneToMany");
        fmd.setSerialized(false); // override any Lob annotation
    }

    /**
     * Parse map-key.
     */
    private boolean startMapKey(Attributes attrs)
        throws SAXException {
        if (!isMappingOverrideMode())
            return false;

        FieldMetaData fmd = (FieldMetaData) currentElement();
        String mapKey = attrs.getValue("name");
        if (mapKey == null)
            fmd.getKey().setValueMappedBy(ValueMetaData.MAPPED_BY_PK);
        else
            fmd.getKey().setValueMappedBy(mapKey);
        return true;
    }

    /**
     * Parse order-by.
     */
    private void endOrderBy()
        throws SAXException {
        FieldMetaData fmd = (FieldMetaData) currentElement();
        String dec = currentText();
        if (StringUtils.isEmpty(dec))
            dec = Order.ELEMENT + " asc";
        fmd.setOrderDeclaration(dec);
    }

    /**
     * Parse named-query.
     */
    protected boolean startNamedQuery(Attributes attrs)
        throws SAXException {
        if (!isQueryMode())
            return false;

        String name = attrs.getValue("name");
        Log log = getLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("parse-query", name));

        QueryMetaData meta = getRepository().getCachedQueryMetaData(null, name);
        if (meta != null && log.isWarnEnabled())
            log.warn(_loc.get("override-query", name, currentLocation()));

        meta = getRepository().addQueryMetaData(null, name);
        meta.setDefiningType(_cls);
        meta.setQueryString(attrs.getValue("query"));
        meta.setLanguage(JPQLParser.LANG_JPQL);
        Locator locator = getLocation().getLocator();
        if (locator != null) {
            meta.setLineNumber(Numbers.valueOf(locator.getLineNumber()));
            meta.setColNumber(Numbers.valueOf(locator.getColumnNumber()));
        }
        Object cur = currentElement();
        Object scope = (cur instanceof ClassMetaData)
            ? ((ClassMetaData) cur).getDescribedType() : null;
        meta.setSource(getSourceFile(), scope, meta.SRC_XML);
        if (isMetaDataMode())
            meta.setSourceMode(MODE_META);
        else if (isMappingMode())
            meta.setSourceMode(MODE_MAPPING);
        else
            meta.setSourceMode(MODE_QUERY);
        pushElement(meta);
        return true;
    }

    protected void endNamedQuery()
        throws SAXException {
        popElement();
    }

    protected boolean startQueryString(Attributes attrs)
        throws SAXException {
        return true;
    }

    protected void endQueryString()
        throws SAXException {
        QueryMetaData meta = (QueryMetaData) currentElement();
        meta.setQueryString(currentText());
    }

    /**
     * Parse query-hint.
     */
    protected boolean startQueryHint(Attributes attrs)
        throws SAXException {
        QueryMetaData meta = (QueryMetaData) currentElement();
        meta.addHint(attrs.getValue("name"), attrs.getValue("value"));
        return true;
    }

    protected void endQueryHint()
        throws SAXException {
    }

    /**
     * Parse native-named-query.
     */
    protected boolean startNamedNativeQuery(Attributes attrs)
        throws SAXException {
        if (!isQueryMode())
            return false;

        String name = attrs.getValue("name");
        Log log = getLog();
        if (log.isTraceEnabled())
            log.trace(_loc.get("parse-native-query", name));

        QueryMetaData meta = getRepository().getCachedQueryMetaData(null, name);
        if (meta != null && log.isWarnEnabled())
            log.warn(_loc.get("override-query", name, currentLocation()));

        meta = getRepository().addQueryMetaData(null, name);
        meta.setDefiningType(_cls);
        meta.setQueryString(attrs.getValue("query"));
        meta.setLanguage(QueryLanguages.LANG_SQL);
        String val = attrs.getValue("result-class");
        if (val != null) {
            Class type = classForName(val);
            if (ImplHelper.isManagedType(getConfiguration(), type))
                meta.setCandidateType(type);
            else
                meta.setResultType(type);
        }

        val = attrs.getValue("result-set-mapping");
        if (val != null)
            meta.setResultSetMappingName(val);

        Object cur = currentElement();
        Object scope = (cur instanceof ClassMetaData)
            ? ((ClassMetaData) cur).getDescribedType() : null;
        meta.setSource(getSourceFile(), scope, meta.SRC_XML);
        Locator locator = getLocation().getLocator();
        if (locator != null) {
            meta.setLineNumber(Numbers.valueOf(locator.getLineNumber()));
            meta.setColNumber(Numbers.valueOf(locator.getColumnNumber()));
        }
        if (isMetaDataMode())
            meta.setSourceMode(MODE_META);
        else if (isMappingMode())
            meta.setSourceMode(MODE_MAPPING);
        else
            meta.setSourceMode(MODE_QUERY);
        pushElement(meta);
        return true;
    }

    protected void endNamedNativeQuery()
        throws SAXException {
        popElement();
    }

    /**
     * Start entity-listeners
     */
    private boolean startEntityListeners(Attributes attrs)
        throws SAXException {
        if (!isMetaDataMode())
            return false;
        if (currentElement() == null)
            return true;

        // reset listeners declared in annotations.
        LifecycleMetaData meta = ((ClassMetaData) currentElement()).
            getLifecycleMetaData();
        for (int i = 0; i < LifecycleEvent.ALL_EVENTS.length; i++)
            meta.setDeclaredCallbacks(i, null, 0);
        return true;
    }

    /**
     * Parse exclude-default-listeners.
     */
    private boolean startExcludeDefaultListeners(Attributes attrs)
        throws SAXException {
        if (!isMetaDataMode())
            return false;
        ClassMetaData meta = (ClassMetaData) currentElement();
        meta.getLifecycleMetaData().setIgnoreSystemListeners(true);
        return true;
    }

    /**
     * Parse exclude-superclass-listeners.
     */
    private boolean startExcludeSuperclassListeners(Attributes attrs)
        throws SAXException {
        if (!isMetaDataMode())
            return false;
        ClassMetaData meta = (ClassMetaData) currentElement();
        meta.getLifecycleMetaData().setIgnoreSuperclassCallbacks
            (LifecycleMetaData.IGNORE_HIGH);
        return true;
    }

    /**
     * Parse entity-listener.
     */
    private boolean startEntityListener(Attributes attrs)
        throws SAXException {
        _listener = classForName(attrs.getValue("class"));
        boolean system = currentElement() == null;
        Collection<LifecycleCallbacks>[] parsed =
            AnnotationPersistenceMetaDataParser.parseCallbackMethods(_listener,
                null, true, true, _repos);
        if (parsed == null)
            return true;

        if (_callbacks == null) {
            _callbacks = (Collection<LifecycleCallbacks>[])
                new Collection[LifecycleEvent.ALL_EVENTS.length];
            if (!system)
                _highs = new int[LifecycleEvent.ALL_EVENTS.length];
        }
        for (int i = 0; i < parsed.length; i++) {
            if (parsed[i] == null)
                continue;
            if (_callbacks[i] == null)
                _callbacks[i] = parsed[i];
            else
                _callbacks[i].addAll(parsed[i]);
            if (!system)
                _highs[i] += parsed[i].size();
        }
        return true;
    }

    private void endEntityListener()
        throws SAXException {
        // should be in endEntityListeners I think to merge callbacks
        // into a single listener.  But then the user cannot remove.
        if (currentElement() == null && _callbacks != null) {
            _repos.addSystemListener(new PersistenceListenerAdapter
                (_callbacks));
            _callbacks = null;
        }
        _listener = null;
    }

    private boolean startCallback(MetaDataTag callback, Attributes attrs)
        throws SAXException {
        if (!isMetaDataMode())
            return false;
        int[] events = MetaDataParsers.getEventTypes(callback);
        if (events == null)
            return false;

        boolean system = currentElement() == null;
        Class type = currentElement() == null ? null :
            ((ClassMetaData) currentElement()).getDescribedType();
        if (type == null)
            type = Object.class;

        if (_callbacks == null) {
            _callbacks = (Collection<LifecycleCallbacks>[])
                new Collection[LifecycleEvent.ALL_EVENTS.length];
            if (!system)
                _highs = new int[LifecycleEvent.ALL_EVENTS.length];
        }

        MetaDataDefaults def = _repos.getMetaDataFactory().getDefaults();
        LifecycleCallbacks adapter;
        if (_listener != null)
            adapter = new BeanLifecycleCallbacks(_listener,
                attrs.getValue("method-name"), false, type);
        else
            adapter = new MethodLifecycleCallbacks(_cls,
                attrs.getValue("method-name"), false);

        for (int i = 0; i < events.length; i++) {
            int event = events[i];
            if (_listener != null) {
                MetaDataParsers.validateMethodsForSameCallback(_listener,
                    _callbacks[event], ((BeanLifecycleCallbacks) adapter).
                    getCallbackMethod(), callback, def, getLog());
            } else {
                MetaDataParsers.validateMethodsForSameCallback(_cls,
                    _callbacks[event], ((MethodLifecycleCallbacks) adapter).
                    getCallbackMethod(), callback, def, getLog());

            }
            if (_callbacks[event] == null)
                _callbacks[event] = new ArrayList<LifecycleCallbacks>(3);
            _callbacks[event].add(adapter);
            if (!system && _listener != null)
                _highs[event]++;
        }
        return true;
    }

    /**
     * Store lifecycle metadata.
     */
    private void storeCallbacks(ClassMetaData cls) {
        LifecycleMetaData meta = cls.getLifecycleMetaData();
        Class supCls = cls.getDescribedType().getSuperclass();
        Collection<LifecycleCallbacks>[] supCalls = null;
        if (!Object.class.equals(supCls)) {
            supCalls = AnnotationPersistenceMetaDataParser.parseCallbackMethods
                (supCls, null, true, false, _repos);
        }
        if (supCalls != null) {
            for (int event : LifecycleEvent.ALL_EVENTS) {
                if (supCalls[event] == null)
                    continue;
                meta.setNonPCSuperclassCallbacks(event, supCalls[event].toArray
                    (new LifecycleCallbacks[supCalls[event].size()]), 0);
            }
        }
        if (_callbacks == null)
            return;

        for (int event : LifecycleEvent.ALL_EVENTS) {
            if (_callbacks[event] == null)
                continue;
            meta.setDeclaredCallbacks(event, (LifecycleCallbacks[])
                _callbacks[event].toArray
                    (new LifecycleCallbacks[_callbacks[event].size()]),
                _highs[event]);
        }
        _callbacks = null;
        _highs = null;
    }

    /**
     * Instantiate the given class, taking into account the default package.
	 */
	protected Class classForName(String name)
		throws SAXException {
		if ("Entity".equals(name))
			return PersistenceCapable.class;
		return super.classForName(name, isRuntime());
	}
}
