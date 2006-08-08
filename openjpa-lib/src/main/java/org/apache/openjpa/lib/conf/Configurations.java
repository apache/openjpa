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
package org.apache.openjpa.lib.conf;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.TreeSet;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang.exception.NestableRuntimeException;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.lib.util.ParseException;
import org.apache.openjpa.lib.util.Services;
import org.apache.openjpa.lib.util.StringDistance;
import serp.util.Strings;

/**
 * Utility methods dealing with configuration.
 *
 * @author Abe White
 * @nojavadoc
 */
public class Configurations {

    private static final Localizer _loc = Localizer.forPackage
        (Configurations.class);

    /**
     * Return the class name from the given plugin string, or null if none.
     */
    public static String getClassName(String plugin) {
        return getPluginComponent(plugin, true);
    }

    /**
     * Return the properties part of the given plugin string, or null if none.
     */
    public static String getProperties(String plugin) {
        return getPluginComponent(plugin, false);
    }

    /**
     * Return either the class name or properties string from a plugin string.
     */
    private static String getPluginComponent(String plugin, boolean clsName) {
        if (plugin != null)
            plugin = plugin.trim();
        if (plugin == null || plugin.length() == 0)
            return null;

        int openParen = -1;
        if (plugin.charAt(plugin.length() - 1) == ')')
            openParen = plugin.indexOf('(');
        if (openParen == -1) {
            int eq = plugin.indexOf('=');
            if (eq == -1)
                return (clsName) ? plugin : null;
            return (clsName) ? null : plugin;
        }

        // clsName(props) form
        if (clsName)
            return plugin.substring(0, openParen).trim();
        String prop = plugin.substring(openParen + 1,
            plugin.length() - 1).trim();
        return (prop.length() == 0) ? null : prop;
    }

    /**
     * Combine the given class name and properties into a plugin string.
     */
    public static String getPlugin(String clsName, String props) {
        if (clsName == null || clsName.length() == 0)
            return props;
        if (props == null || props.length() == 0)
            return clsName;
        return clsName + "(" + props + ")";
    }

    /**
     * Create the instance with the given class name, using the given
     * class loader. No configuration of the instance is performed by
     * this method.
     */
    public static Object newInstance(String clsName, ClassLoader loader) {
        return newInstance(clsName, null, null, loader, true);
    }

    /**
     * Create and configure an instance with the given class name and
     * properties.
     */
    public static Object newInstance(String clsName, Configuration conf,
        String props, ClassLoader loader) {
        Object obj = newInstance(clsName, null, conf, loader, true);
        configureInstance(obj, conf, props);
        return obj;
    }

    /**
     * Helper method used by members of this package to instantiate plugin
     * values.
     */
    static Object newInstance(String clsName, Value val, Configuration conf,
        ClassLoader loader, boolean fatal) {
        if (clsName == null || clsName.length() == 0)
            return null;
        if (loader == null && conf != null)
            loader = conf.getClass().getClassLoader();

        Class cls = null;
        try {
            cls = Strings.toClass(clsName, loader);
        } catch (RuntimeException re) {
            if (val != null)
                re = getCreateException(clsName, val, re);
            if (fatal)
                throw re;
            Log log = (conf == null) ? null : conf.getConfigurationLog();
            if (log != null && log.isErrorEnabled())
                log.error(_loc.get("plugin-creation-exception", val), re);
            return null;
        }

        try {
            return cls.newInstance();
        } catch (Exception e) {
            RuntimeException re = new NestableRuntimeException(_loc.get
                ("obj-create", cls).getMessage(), e);
            if (fatal)
                throw re;
            Log log = (conf == null) ? null : conf.getConfigurationLog();
            if (log != null && log.isErrorEnabled())
                log.error(_loc.get("plugin-creation-exception", val), re);
            return null;
        }
    }

    /**
     * Helper method to throw an informative description on instantiation error.
     */
    private static RuntimeException getCreateException(String clsName,
        Value val, Exception e) {
        // re-throw the exception with some better information
        final String msg;
        final Object[] params;

        String alias = val.alias(clsName);
        String[] aliases = val.getAliases();
        String[] keys;
        if (aliases.length == 0)
            keys = aliases;
        else {
            keys = new String[aliases.length / 2];
            for (int i = 0; i < aliases.length; i += 2)
                keys[i / 2] = aliases[i];
        }

        String closest;
        if (keys.length == 0) {
            msg = "invalid-plugin";
            params = new Object[]{ val.getProperty(), alias, e.toString(), };
        } else if ((closest = StringDistance.getClosestLevenshteinDistance
            (alias, keys, 0.5f)) == null) {
            msg = "invalid-plugin-aliases";
            params = new Object[]{
                val.getProperty(), alias, e.toString(),
                new TreeSet(Arrays.asList(keys)), };
        } else {
            msg = "invalid-plugin-aliases-hint";
            params = new Object[]{
                val.getProperty(), alias, e.toString(),
                new TreeSet(Arrays.asList(keys)), closest, };
        }
        return new ParseException(_loc.get(msg, params), e);
    }

    /**
     * Configures the given object with the given properties by
     * matching the properties string to the object's setter
     * methods. The properties string should be in the form
     * "prop1=val1, prop2=val2 ...". Does not validate that setter
     * methods exist for the properties.
     *
     * @throws RuntimeException on configuration error
     */
    public static void configureInstance(Object obj, Configuration conf,
        String properties) {
        configureInstance(obj, conf, properties, null);
    }

    /**
     * Configures the given object with the given properties by
     * matching the properties string to the object's setter
     * methods. The properties string should be in the form
     * "prop1=val1, prop2=val2 ...". Validates that setter methods
     * exist for the properties.
     *
     * @throws RuntimeException on configuration error
     */
    public static void configureInstance(Object obj, Configuration conf,
        String properties, String configurationName) {
        if (obj == null)
            return;

        Properties props = null;
        if (properties != null && properties.length() > 0)
            props = parseProperties(properties);
        configureInstance(obj, conf, props, configurationName);
    }

    /**
     * Configures the given object with the given properties by
     * matching the properties string to the object's setter
     * methods. Does not validate that setter methods exist for the properties.
     *
     * @throws RuntimeException on configuration error
     */
    public static void configureInstance(Object obj, Configuration conf,
        Properties properties) {
        configureInstance(obj, conf, properties, null);
    }

    /**
     * Configures the given object with the given properties by
     * matching the properties string to the object's setter
     * methods. If <code>configurationName</code> is
     * non-<code>null</code>, validates that setter methods exist for
     * the properties.
     *
     * @throws RuntimeException on configuration error
     */
    public static void configureInstance(Object obj, Configuration conf,
        Properties properties, String configurationName) {
        if (obj == null)
            return;

        Options opts;
        if (properties instanceof Options)
            opts = (Options) properties;
        else { 
            opts = new Options();
            if (properties != null)
                opts.putAll(properties);
        }

        Configurable configurable = null;
        if (conf != null && obj instanceof Configurable)
            configurable = (Configurable) obj;

        if (configurable != null) {
            configurable.setConfiguration(conf);
            configurable.startConfiguration();
        }
        Options invalidEntries = opts.setInto(obj);
        if (obj instanceof GenericConfigurable)
            ((GenericConfigurable) obj).setInto(invalidEntries);

		if (!invalidEntries.isEmpty() && configurationName != null) {
			Localizer.Message msg = null;
			String first = (String) invalidEntries.keySet().iterator().next();
			if (invalidEntries.keySet().size() == 1 &&
				first.indexOf('.') == -1) {
				// if there's just one misspelling and this is not a
				// path traversal, check for near misses.
				Collection options = Options.findOptionsFor(obj.getClass());
				String close = StringDistance.getClosestLevenshteinDistance
					(first, options, 0.75f);
				if (close != null)
					msg = _loc.get("invalid-config-param-hint", new Object[]{
						configurationName, obj.getClass(), first, close,
						options, });
			}

            if (msg == null) {
                msg = _loc.get("invalid-config-params", new String[]{
                    configurationName, obj.getClass().getName(),
                    invalidEntries.keySet().toString(),
                    Options.findOptionsFor(obj.getClass()).toString(), });
            }
            throw new ParseException(msg);
        }
        if (configurable != null)
            configurable.endConfiguration();
    }

    /**
     * Turn a set of properties into a comma-separated string.
     */
    public static String serializeProperties(Map map) {
        if (map == null || map.isEmpty())
            return null;

        StringBuffer buf = new StringBuffer();
        Map.Entry entry;
        String val;
        for (Iterator itr = map.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();
            if (buf.length() > 0)
                buf.append(", ");
            buf.append(entry.getKey()).append('=');
            val = String.valueOf(entry.getValue());
            if (val.indexOf(',') != -1)
                buf.append('"').append(val).append('"');
            else
                buf.append(val);
        }
        return buf.toString();
    }

    /**
     * Parse a set of properties from a comma-separated string.
     */
    public static Options parseProperties(String properties) {
        Options opts = new Options();
        if (properties == null)
            return opts;
        properties = properties.trim();
        if (properties.length() == 0)
            return opts;

        try {
            String[] props = Strings.split(properties, ",", 0);
            int idx;
            char quote;
            String prop;
            String val;
            for (int i = 0; i < props.length; i++) {
                idx = props[i].indexOf('=');
                if (idx == -1) {
                    // if the key is not assigned to any value, set the
                    // value to the same thing as the key, and continue.
                    // This permits GenericConfigurable instances to
                    // behave meaningfully. We might consider setting the
                    // value to some well-known "value was not set, but
                    // key is present" string so that instances getting
                    // values injected can differentiate between a mentioned
                    // property and one set to a particular value.
                    prop = props[i];
                    val = prop;
                } else {
                    prop = props[i].substring(0, idx).trim();
                    val = props[i].substring(idx + 1).trim();
                }

                // if the value is quoted, read until the end quote
                if (((val.startsWith("\"") && val.endsWith("\""))
                    || (val.startsWith("'") && val.endsWith("'")))
                    && val.length() > 1)
                    val = val.substring(1, val.length() - 1);
                else if (val.startsWith("\"") || val.startsWith("'")) {
                    quote = val.charAt(0);
                    StringBuffer buf = new StringBuffer(val.substring(1));
                    int quotIdx;
                    while (++i < props.length) {
                        buf.append(",");

                        quotIdx = props[i].indexOf(quote);
                        if (quotIdx != -1) {
                            buf.append(props[i].substring(0, quotIdx));
                            if (quotIdx + 1 < props[i].length())
                                buf.append(props[i].substring(quotIdx + 1));
                            break;
                        } else
                            buf.append(props[i]);
                    }

                    val = buf.toString();
                }

                opts.put(prop, val);
            }
            return opts;
        } catch (RuntimeException re) {
            throw new ParseException(_loc.get("prop-parse", properties), re);
        }
    }

    /**
     * Set the given {@link Configuration} instance from the command line
     * options provided. All property names of the given configuration are
     * recognized; additionally, if a <code>properties</code> or
     * <code>p</code> argument exists, the resource it
     * points to will be loaded and set into the given configuration instance.
     * It can point to either a file or a resource name.
     */
    public static void populateConfiguration(Configuration conf, Options opts) {
        String props = opts.removeProperty("properties", "p", null);
        if (props != null && props.length() > 0) {
            File file = new File(props);
            ConfigurationProvider provider;
            if (file.isFile())
                provider = load(file, null);
            else {
                file = new File("META-INF" + File.separatorChar + props);
                if (file.isFile())
                    provider = load(file, null);
                else
                    provider = load(props, null);
            }
            provider.setInto(conf);
        }
        opts.setInto(conf);
    }

    /**
     * Return a {@link ConfigurationProvider} that has parsed system defaults.
     */
    public static ConfigurationProvider loadDefaults(ClassLoader loader) {
        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();
        Class[] impls = Services.getImplementorClasses
            (ConfigurationProvider.class, loader);
        ConfigurationProvider provider = null;
        int providerCount = 0;
        StringBuffer errs = null;
        for (int i = 0; i < impls.length; i++) {
            provider = newProvider(impls[i]);
            if (provider == null)
                continue;

            providerCount++;
            try {
                if (provider.loadDefaults(loader))
                    return provider;
            } catch (MissingResourceException mre) {
                throw mre;
            } catch (Exception e) {
                if (errs == null)
                    errs = new StringBuffer();
                else
                    errs.append(", ");
                errs.append(e.toString());
            }
        }
        if (errs != null)
            throw new MissingResourceException(errs.toString(),
                Configurations.class.getName(), "defaults");
        if (providerCount == 0)
            throw new MissingResourceException(_loc.get ("no-providers", 
                ConfigurationProvider.class.getName()).getMessage(),
                Configurations.class.getName(), "defaults"); 
        return null;
    }

    /**
     * Return a new new configuration provider instance of the given class,
     * or null if the class cannot be instantiated.
     */
    private static ConfigurationProvider newProvider(Class cls) {
        try {
            return (ConfigurationProvider) cls.newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Return a {@link ConfigurationProvider} that has parsed the given
     * resource. Throws {@link MissingResourceException} if resource does
     * not exist.
     */
    public static ConfigurationProvider load(String resource,
        ClassLoader loader) {
        if (resource == null || resource.length() == 0)
            return null;

        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();
        Class[] impls = Services.getImplementorClasses
            (ConfigurationProvider.class, loader);
        ConfigurationProvider provider = null;
        int providerCount = 0;
        StringBuffer errs = null;
        for (int i = 0; i < impls.length; i++) {
            provider = newProvider(impls[i]);
            if (provider == null)
                continue;

            providerCount++;
            try {
                if (provider.load(resource, loader))
                    return provider;
            } catch (MissingResourceException mre) {
                throw mre;
            } catch (Exception e) {
                if (errs == null)
                    errs = new StringBuffer();
                else
                    errs.append(", ");
                errs.append(e.toString());
            }
        }
        String msg;
        if (errs != null)
            msg = errs.toString();
        else if (providerCount == 0)
            msg = _loc.get("no-providers", 
                ConfigurationProvider.class.getName()).getMessage();
        else
            msg = _loc.get("no-provider", resource).getMessage();
        
        throw new MissingResourceException(msg,
            Configurations.class.getName(), resource);
    }

    /**
     * Return a {@link ConfigurationProvider} that has parsed the given
     * file. Throws {@link MissingResourceException} if file does not exist.
     */
    public static ConfigurationProvider load(File file, ClassLoader loader) {
        if (file == null)
            return null;

        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();
        Class[] impls = Services.getImplementorClasses
            (ConfigurationProvider.class, loader);
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        for (int i = 0; i < impls.length; i++) {
            provider = newProvider(impls[i]);
            try {
                if (provider != null && provider.load(file))
                    return provider;
            } catch (MissingResourceException mre) {
                throw mre;
            } catch (Exception e) {
                if (errs == null)
                    errs = new StringBuffer();
                else
                    errs.append(", ");
                errs.append(e.toString());
            }
        }
        String msg = (errs == null) ? file.toString() : errs.toString();
        throw new MissingResourceException(msg,
            Configurations.class.getName(), file.toString());
    }

    /**
     * Looks up the given name in JNDI. If the name is null, null is returned.
     */
    public static Object lookup(String name) {
        if (name == null || name.length() == 0)
            return null;

        Context ctx = null;
        try {
            ctx = new InitialContext();
            return ctx.lookup(name);
        } catch (NamingException ne) {
            throw new NestableRuntimeException(
                _loc.get("naming-err", name).getMessage(), ne);
        } finally {
            if (ctx != null)
                try {
                    ctx.close();
                } catch (Exception e) {
                }
        }
    }
}
