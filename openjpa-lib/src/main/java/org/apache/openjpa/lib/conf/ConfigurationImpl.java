/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.conf;

import org.apache.commons.lang.*;

import org.apache.openjpa.lib.log.*;
import org.apache.openjpa.lib.util.*;
import org.apache.openjpa.lib.util.Closeable;

import serp.util.*;

import java.awt.*;

import java.beans.*;

import java.io.*;

import java.lang.reflect.*;

import java.util.*;
import java.util.List;


/**
 *  <p>Default implementation of the {@link Configuration} interface.
 *  Subclasses can choose to obtain configuration
 *  information from JNDI, Properties, a Bean-builder, etc.  This class
 *  provides base configuration functionality, including serialization,
 *  the <code>equals</code> and <code>hashCode</code> contracts, and default
 *  property loading.</p>
 *
 *  <p>Property descriptors for {@link Value} instances are constructed from
 *  the {@link Localizer} for the package of the configuration class. The
 *  following localized strings will be used for describing a value, where
 *  <em>name</em> is the last token of the value's property string:
 *  <ul>
 *  <li><em>name</em>-name: The name that will be displayed for the
*          option in a user interface; required.</li>
 *  <li><em>name</em>-desc: A brief description of the option; required.</li>
 *  <li><em>name</em>-type: The type or category name for this option;
 *          required.</li>
 *  <li><em>name</em>-expert: True if this is an expert option, false
 *          otherwise; defaults to false.</li>
 *  <li><em>name</em>-values: Set of expected or common values, excluding
 *          alias keys; optional.</li>
 *  <li><em>name</em>-interface: The class name of an interface whose
 *          discoverable implementations should be included in the set of expected
 *          or common values; optional.</li>
 *  <li><em>name</em>-cat: The hierarchical category for the property
 *          name, separated by ".".
 *  <li><em>name</em>-displayorder: The order in which the property should
 *          be displayer.</li>
 *  </ul></p>
 *
 *  @author Abe White
 */
public class ConfigurationImpl implements Configuration, Externalizable,
    ValueListener {
    private static final String SEP = System.getProperty("line.separator");
    private static final Localizer _loc = Localizer.forPackage(ConfigurationImpl.class);
    public ObjectValue logFactoryPlugin;
    private boolean _readOnly = false;
    private Map _props = null;
    private boolean _defaults = false;
    private final List _vals = new ArrayList();

    // property listener helper
    private PropertyChangeSupport _changeSupport = null;

    // cache descriptors
    private PropertyDescriptor[] _pds = null;
    private MethodDescriptor[] _mds = null;

    /**
     *  Default constructor.  Attempts to load default properties through
     *  system's configured {@link ConfigurationProvider}s.
     */
    public ConfigurationImpl() {
        this(true);
    }

    /**
     *  Constructor.
      *
     *  @param loadDefaults        whether to attempt to load the default
     *                                                  properties
     */
    public ConfigurationImpl(boolean loadDefaults) {
        logFactoryPlugin = addPlugin("org.apache.openjpa.lib.Log", true);

        String[] aliases = new String[] {
                "true", "org.apache.openjpa.lib.log.LogFactoryImpl", "commons",
                "org.apache.openjpa.lib.log.CommonsLogFactory", "log4j",
                "org.apache.openjpa.lib.log.Log4JLogFactory", "none",
                "org.apache.openjpa.lib.log.NoneLogFactory", "false",
                "org.apache.openjpa.lib.log.NoneLogFactory",
            };
        logFactoryPlugin.setAliases(aliases);
        logFactoryPlugin.setDefault(aliases[0]);
        logFactoryPlugin.setString(aliases[0]);
        logFactoryPlugin.setInstantiatingGetter("getLogFactory");

        if (loadDefaults) {
            loadDefaults();
        }
    }

    /**
     *  Automatically load default values from the system's
     *  {@link ConfigurationProvider}s, and from System properties.
     */
    public boolean loadDefaults() {
        ConfigurationProvider provider = Configurations.loadDefaults(getClass()
                                                                         .getClassLoader());

        if (provider != null) {
            provider.setInto(this);
        }

        // let system properties override other defaults
        try {
            fromProperties(new HashMap(System.getProperties()));
        } catch (SecurityException se) {
            // security manager might disallow
        }

        _defaults = true;

        if (provider == null) {
            Log log = getConfigurationLog();

            if (log.isTraceEnabled()) {
                log.trace(_loc.get("no-providers"));
            }

            return false;
        }

        return true;
    }

    public String getProductName() {
        return "solarmetric";
    }

    public LogFactory getLogFactory() {
        if (logFactoryPlugin.get() == null) {
            logFactoryPlugin.instantiate(LogFactory.class, this);
        }

        return (LogFactory) logFactoryPlugin.get();
    }

    public void setLogFactory(LogFactory logFactory) {
        assertNotReadOnly();
        logFactoryPlugin.set(logFactory);
    }

    public String getLog() {
        return logFactoryPlugin.getString();
    }

    public void setLog(String log) {
        assertNotReadOnly();
        logFactoryPlugin.setString(log);
    }

    public Log getLog(String category) {
        return getLogFactory().getLog(category);
    }

    /**
     *  Returns the logging channel <code>org.apache.openjpa.lib.Runtime</code> by
     *  default.
     */
    public Log getConfigurationLog() {
        return getLog("org.apache.openjpa.lib.Runtime");
    }

    /**
     *  Returns the logging channel <code>org.apache.openjpa.lib.Manage</code> by
     *  default.
     */
    public Log getManagementLog() {
        return getLog("org.apache.openjpa.lib.Manage");
    }

    /**
     *  Returns the logging channel <code>org.apache.openjpa.lib.Profile</code> by
     *  default.
     */
    public Log getProfilingLog() {
        return getLog("org.apache.openjpa.lib.Profile");
    }

    public Value[] getValues() {
        return (Value[]) _vals.toArray(new Value[_vals.size()]);
    }

    public Value getValue(String property) {
        if (property == null) {
            return null;
        }

        // search backwards so that custom values added after construction
        // are found quickly, since this will be the std way of accessing them
        Value val;

        for (int i = _vals.size() - 1; i >= 0; i--) {
            val = (Value) _vals.get(i);

            if (val.getProperty().equals(property)) {
                return val;
            }
        }

        return null;
    }

    public void setReadOnly(boolean readOnly) {
        _readOnly = readOnly;
    }

    public void instantiateAll() {
        StringWriter errs = null;
        PrintWriter stack = null;
        Value val;
        String getterName;
        Method getter;
        Object getterTarget;

        for (int i = 0; i < _vals.size(); i++) {
            val = (Value) _vals.get(i);
            getterName = val.getInstantiatingGetter();

            if (getterName == null) {
                continue;
            }

            getterTarget = this;

            if (getterName.startsWith("this.")) {
                getterName = getterName.substring("this.".length());
                getterTarget = val;
            }

            try {
                getter = getterTarget.getClass()
                                     .getMethod(getterName, (Class[]) null);
                getter.invoke(getterTarget, (Object[]) null);
            } catch (Throwable t) {
                if (t instanceof InvocationTargetException) {
                    t = ((InvocationTargetException) t).getTargetException();
                }

                if (errs == null) {
                    errs = new StringWriter();
                    stack = new PrintWriter(errs);
                } else {
                    errs.write(SEP);
                }

                t.printStackTrace(stack);
                stack.flush();
            }
        }

        if (errs != null) {
            throw new RuntimeException(_loc.get("get-prop-errs", errs.toString()));
        }
    }

    public boolean isReadOnly() {
        return _readOnly;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (_changeSupport == null) {
            _changeSupport = new PropertyChangeSupport(this);
        }

        _changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (_changeSupport != null) {
            _changeSupport.removePropertyChangeListener(listener);
        }
    }

    public void valueChanged(Value val) {
        if ((_changeSupport == null) && (_props == null)) {
            return;
        }

        String newString = val.getString();

        if (_changeSupport != null) {
            _changeSupport.firePropertyChange(val.getProperty(), null, newString);
        }

        // keep cached props up to date
        if (_props != null) {
            if (newString == null) {
                _props.remove(val.getProperty());
            } else if (_props.containsKey(val.getProperty()) ||
                    (val.getDefault() == null) ||
                    !val.getDefault().equals(newString)) {
                _props.put(val.getProperty(), newString);
            }
        }
    }

    /**
     *  Closes all closeable plugins.
     */
    public void close() {
        ObjectValue val;

        for (int i = 0; i < _vals.size(); i++) {
            if (!(_vals.get(i) instanceof ObjectValue)) {
                continue;
            }

            val = (ObjectValue) _vals.get(i);

            if (val.get() instanceof Closeable) {
                try {
                    ((Closeable) val.get()).close();
                } catch (Exception e) {
                }
            }
        }
    }

    ///////////////////////////
    // BeanInfo implementation
    ///////////////////////////
    public BeanInfo[] getAdditionalBeanInfo() {
        return new BeanInfo[0];
    }

    public BeanDescriptor getBeanDescriptor() {
        return new BeanDescriptor(getClass());
    }

    public int getDefaultEventIndex() {
        return 0;
    }

    public int getDefaultPropertyIndex() {
        return 0;
    }

    public EventSetDescriptor[] getEventSetDescriptors() {
        return new EventSetDescriptor[0];
    }

    public Image getIcon(int kind) {
        return null;
    }

    public synchronized MethodDescriptor[] getMethodDescriptors() {
        if (_mds != null) {
            return _mds;
        }

        PropertyDescriptor[] pds = getPropertyDescriptors();
        _mds = new MethodDescriptor[pds.length * 2];

        for (int i = 0; i < pds.length; i++) {
            _mds[i * 2] = new MethodDescriptor(pds[i].getWriteMethod());
            _mds[(i * 2) + 1] = new MethodDescriptor(pds[i].getReadMethod());
        }

        return _mds;
    }

    public synchronized PropertyDescriptor[] getPropertyDescriptors() {
        if (_pds != null) {
            return _pds;
        }

        _pds = new PropertyDescriptor[_vals.size()];

        List failures = null;
        Value val;

        for (int i = 0; i < _vals.size(); i++) {
            val = (Value) _vals.get(i);

            try {
                _pds[i] = getPropertyDescriptor(val);
            } catch (MissingResourceException mre) {
                if (failures == null) {
                    failures = new ArrayList();
                }

                failures.add(val.getProperty());
            } catch (IntrospectionException ie) {
                if (failures == null) {
                    failures = new ArrayList();
                }

                failures.add(val.getProperty());
            }
        }

        if (failures != null) {
            throw new ParseException(_loc.get("invalid-property-descriptors",
                    failures));
        }

        return _pds;
    }

    /**
     *  Create a property descriptor for the given value.
     */
    private PropertyDescriptor getPropertyDescriptor(Value val)
        throws IntrospectionException {
        String prop = val.getProperty();
        prop = prop.substring(prop.lastIndexOf('.') + 1);

        // set up property descriptor
        PropertyDescriptor pd;

        try {
            pd = new PropertyDescriptor(Introspector.decapitalize(prop),
                    getClass());
        } catch (IntrospectionException ie) {
            // if there aren't any methods for this value (i.e., if it's a 
            // dynamically-added value), then an IntrospectionException will
            // be thrown. Try to create a PD with no read or write methods.
            pd = new PropertyDescriptor(Introspector.decapitalize(prop),
                    (Method) null, (Method) null);
        }

        pd.setDisplayName(findLocalized(prop + "-name", true, val.getScope()));
        pd.setShortDescription(findLocalized(prop + "-desc", true,
                val.getScope()));
        pd.setExpert("true".equals(findLocalized(prop + "-expert", false,
                    val.getScope())));

        try {
            pd.setReadMethod(getClass()
                                 .getMethod("get" +
                    StringUtils.capitalize(prop), (Class[]) null));
            pd.setWriteMethod(getClass()
                                  .getMethod("set" +
                    StringUtils.capitalize(prop),
                    new Class[] { pd.getReadMethod().getReturnType() }));
        } catch (Throwable t) {
            // if an error occurs, it might be because the value is a 
            // dynamic property.
        }

        String type = findLocalized(prop + "-type", true, val.getScope());

        if (type != null) {
            pd.setValue(ATTRIBUTE_TYPE, type);
        }

        String cat = findLocalized(prop + "-cat", false, val.getScope());

        if (cat != null) {
            pd.setValue(ATTRIBUTE_CATEGORY, cat);
        }

        String order = findLocalized(prop + "-displayorder", false,
                val.getScope());

        if (order != null) {
            pd.setValue(ATTRIBUTE_ORDER, order);
        }

        // collect allowed values from alias keys, listed values, and
        // interface implementors
        Collection allowed = new TreeSet();
        List aliases = Collections.EMPTY_LIST;

        if (val.getAliases() != null) {
            aliases = Arrays.asList(val.getAliases());

            for (int i = 0; i < aliases.size(); i += 2)
                allowed.add(aliases.get(i));
        }

        String[] vals = Strings.split(findLocalized(prop + "-values", false,
                    val.getScope()), ",", 0);

        for (int i = 0; i < vals.length; i++)
            if (!aliases.contains(vals[i])) {
                allowed.add(vals[i]);
            }

        try {
            Class intf = Class.forName(findLocalized(prop + "-interface", true,
                        val.getScope()), false, getClass().getClassLoader());
            pd.setValue(ATTRIBUTE_INTERFACE, intf.getName());

            String[] impls = Services.getImplementors(intf);

            for (int i = 0; i < impls.length; i++)
                if (!aliases.contains(impls[i])) {
                    allowed.add(impls[i]);
                }
        } catch (Throwable t) {
        }

        if (!allowed.isEmpty()) {
            pd.setValue(ATTRIBUTE_ALLOWED_VALUES,
                (String[]) allowed.toArray(new String[allowed.size()]));
        }

        return pd;
    }

    /**
     *  Find the given localized string, or return null if not found.
     */
    private String findLocalized(String key, boolean fatal, Class scope) {
        // find the localizer package that contains this key
        Localizer loc = null;

        // check the package that the value claims to be defined in, if 
        // available, before we start guessing.
        if (scope != null) {
            loc = Localizer.forPackage(scope);

            try {
                return loc.getFatal(key);
            } catch (MissingResourceException mse) {
            }
        }

        for (Class cls = getClass(); cls != Object.class;
                cls = cls.getSuperclass()) {
            loc = Localizer.forPackage(cls);

            try {
                return loc.getFatal(key);
            } catch (MissingResourceException mse) {
            }
        }

        if (fatal) {
            throw new MissingResourceException(key, getClass().getName(), key);
        }

        return null;
    }

    ////////////////
    // To/from maps
    ////////////////
    public Map toProperties(boolean storeDefaults) {
        // clone properties before making any modifications; we need to keep
        // the internal properties instance consistent to maintain equals and
        // hashcode contracts
        Map clone;

        if (_props == null) {
            clone = new HashMap();
        } else if (_props instanceof Properties) {
            clone = (Map) ((Properties) _props).clone();
        } else {
            clone = new HashMap(_props);
        }

        // if no existing properties or the properties should contain entries
        // with default values, add values to properties
        if ((_props == null) || storeDefaults) {
            Value val;
            String str;

            for (int i = 0; i < _vals.size(); i++) {
                // if key in existing properties, we already know value is up 
                // to date
                val = (Value) _vals.get(i);

                if ((_props != null) && _props.containsKey(val.getProperty())) {
                    continue;
                }

                str = val.getString();

                if ((str != null) &&
                        (storeDefaults || !str.equals(val.getDefault()))) {
                    clone.put(val.getProperty(), str);
                }
            }

            if (_props == null) {
                _props = new HashMap(clone);
            }
        }

        return clone;
    }

    public void fromProperties(Map map) {
        if ((map == null) || map.isEmpty()) {
            return;
        }

        assertNotReadOnly();

        // if the only previous call was to load defaults, forget them.
        // this way we preserve the original formatting of the user's props
        // instead of the defaults.  this is important for caching on
        // configuration objects
        if (_defaults) {
            _props = null;
            _defaults = false;
        }

        Map remaining = new HashMap(map);
        boolean ser = true;
        Value val;
        Object set;

        for (int i = 0; i < _vals.size(); i++) {
            val = (Value) _vals.get(i);
            set = map.get(val.getProperty());

            if (set == null) {
                continue;
            }

            if (set instanceof String) {
                if (!StringUtils.equals((String) set, val.getString())) {
                    val.setString((String) set);
                }
            } else {
                ser = ser && set instanceof Serializable;
                val.setObject(set);
            }

            remaining.remove(val.getProperty());
        }

        // convention is to point product at a resource with the 
        // <product>.properties System property; remove that property so we
        // we don't warn about it
        remaining.remove(getProductName() + ".properties");

        // now warn if there are any remaining properties that there
        // is an unhandled prop
        Map.Entry entry;

        for (Iterator itr = remaining.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();

            if (entry.getKey() != null) {
                warnInvalidProperty((String) entry.getKey());
            }

            ser = ser && entry.getValue() instanceof Serializable;
        }

        // cache properties
        if ((_props == null) && ser) {
            _props = map;
        }
    }

    /**
     *  Issue a warning that the specified property is not valid.
     */
    private void warnInvalidProperty(String propName) {
        if (!isInvalidProperty(propName)) {
            return;
        }

        Log log = getConfigurationLog();

        if ((log == null) || !log.isWarnEnabled()) {
            return;
        }

        // try to find the closest string to the invalid property
        // so that we can provide a hint in case of a misspelling
        String closest = StringDistance.getClosestLevenshteinDistance(propName,
                new PropertyList(), 15);

        if (closest == null) {
            log.warn(_loc.get("invalid-property", propName));
        } else {
            log.warn(_loc.get("invalid-property-hint", propName, closest));
        }
    }

    /**
     *  Returns true if the specified property name should raise a warning
     *  if it is not found in the list of known properties.
     */
    protected boolean isInvalidProperty(String propName) {
        // by default, we don't warn on any properties, since we don't
        // know what property pattern will be used for the base config
        return false;
    }

    /**
     *  This method loads the named resource as a properties file.  It is
     *  useful for auto-configuration tools so users can specify a
     *  <code>properties</code> value with the name of a resource.
     */
    public void setProperties(String resourceName) throws IOException {
        Configurations.load(resourceName, getClass().getClassLoader())
                      .setInto(this);
    }

    /**
     *  This method loads the named file as a properties file.  It is
     *  useful for auto-configuration tools so users can specify a
     *  <code>propertiesFile</code> value with the name of a file.
     */
    public void setPropertiesFile(File file) throws IOException {
        Configurations.load(file, getClass().getClassLoader()).setInto(this);
    }

    /////////////
    // Utilities
    /////////////

    /**
     *  Checks if the configuration is read only and if so throws an
     *  exception, otherwise returns silently.
     *  Implementations        should call this method before setting any state.
     */
    public void assertNotReadOnly() {
        if (isReadOnly()) {
            throw new IllegalStateException(_loc.get("read-only"));
        }
    }

    /**
     *  Performs an equality check based on the properties returned from
     *  {@link #toProperties}.
     */
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other == null) {
            return false;
        }

        if (!getClass().equals(other.getClass())) {
            return false;
        }

        // compare properties
        ConfigurationImpl conf = (ConfigurationImpl) other;
        Map p1 = (_props == null) ? toProperties(false) : _props;
        Map p2 = (conf._props == null) ? conf.toProperties(false) : conf._props;

        return p1.equals(p2);
    }

    /**
     *  Computes hash code based on the properties returned from
     *  {@link #toProperties}.
     */
    public int hashCode() {
        if (_props != null) {
            return _props.hashCode();
        }

        return toProperties(false).hashCode();
    }

    /**
     *  Implementation of the {@link Externalizable} interface to read from
     *  the properties written by {@link #writeExternal}.
     */
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {
        fromProperties((Map) in.readObject());
    }

    /**
     *  Implementation of the {@link Externalizable} interface to write
     *  the properties returned by {@link #toProperties}.
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        if (_props != null) {
            out.writeObject(_props);
        } else {
            out.writeObject(toProperties(false));
        }
    }

    /**
     *  Uses {@link #toProperties} and {@link #fromProperties} to clone
     *  configuration.
     */
    public Object clone() {
        try {
            Constructor cons = getClass()
                                   .getConstructor(new Class[] { boolean.class });
            Configuration clone = (Configuration) cons.newInstance(new Object[] {
                        Boolean.FALSE
                    });
            clone.fromProperties(toProperties(true));

            return clone;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new ParseException(e);
        }
    }

    public boolean removeValue(Value val) {
        if (!_vals.remove(val)) {
            return false;
        }

        val.setListener(null);

        return true;
    }

    public Value addValue(Value val) {
        _vals.add(val);
        val.setListener(this);

        return val;
    }

    /**
     *  Add the given value to the set of configuration properties.
     */
    public StringValue addString(String property) {
        StringValue val = new StringValue(property);
        addValue(val);

        return val;
    }

    /**
     *  Add the given value to the set of configuration properties.
     */
    public FileValue addFile(String property) {
        FileValue val = new FileValue(property);
        addValue(val);

        return val;
    }

    /**
     *  Add the given value to the set of configuration properties.
     */
    public IntValue addInt(String property) {
        IntValue val = new IntValue(property);
        addValue(val);

        return val;
    }

    /**
     *  Add the given value to the set of configuration properties.
     */
    public DoubleValue addDouble(String property) {
        DoubleValue val = new DoubleValue(property);
        addValue(val);

        return val;
    }

    /**
     *  Add the given value to the set of configuration properties.
     */
    public BooleanValue addBoolean(String property) {
        BooleanValue val = new BooleanValue(property);
        addValue(val);

        return val;
    }

    /**
     *  Add the given value to the set of configuration properties.
     */
    public StringListValue addStringList(String property) {
        StringListValue val = new StringListValue(property);
        addValue(val);

        return val;
    }

    /**
     *  Add the given value to the set of configuration properties.
     */
    public ObjectValue addObject(String property) {
        ObjectValue val = new ObjectValue(property);
        addValue(val);

        return val;
    }

    /**
     *  Add the given value to the set of configuration properties.
     */
    public PluginValue addPlugin(String property, boolean singleton) {
        PluginValue val = new PluginValue(property, singleton);
        addValue(val);

        return val;
    }

    /**
     *  Add the given value to the set of configuration properties.
     */
    public PluginListValue addPluginList(String property) {
        PluginListValue val = new PluginListValue(property);
        addValue(val);

        return val;
    }

    /**
     *  Exposes our values list as a list of property names.
     */
    private class PropertyList extends AbstractList {
        public Object get(int i) {
            return ((Value) _vals.get(i)).getProperty();
        }

        public int size() {
            return _vals.size();
        }
    }
}
