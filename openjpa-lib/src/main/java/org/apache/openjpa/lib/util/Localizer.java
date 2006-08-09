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
package org.apache.openjpa.lib.util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.openjpa.lib.util.concurrent.ConcurrentHashMap;
import org.apache.openjpa.lib.util.concurrent.CopyOnWriteArraySet;

/**
 * The Localizer provides convenient access to localized
 * strings. It inlcudes built-in support for parameter substitution through
 * the use of the {@link MessageFormat} utility.
 * Strings are stored in per-package {@link Properties} files.
 * The property file for the default locale must be named
 * <code>localizer.properties</code>. Additional locales can be supported
 * through additional property files using the naming conventions specified
 * in the {@link ResourceBundle} class. For example, the german locale
 * could be supported through a <code>localizer_de_DE.properties</code> file.
 *
 * @author Abe White
 */
public class Localizer {

    // static cache of package+loc name to localizer mappings
    private static final Map _localizers = new ConcurrentHashMap();

    // list of resource providers to delegate to when locating resources
    private static final Collection _providers = new CopyOnWriteArraySet
        (Arrays.asList(new Object[]{
            new SimpleResourceBundleProvider(),
            new StreamResourceBundleProvider(),
            new ZipResourceBundleProvider(), }));

    // the local file name and class' classloader
    private ResourceBundle _bundle = null;

    // the package that this localizer was created for.
    private Package _package;

    /**
     * Return a Localizer instance that will access the properties file
     * in the package of the given class using the system default locale.
     *
     * @see #forPackage(Class,Locale)
     */
    public static Localizer forPackage(Class cls) {
        return forPackage(cls, null);
    }

    /**
     * Return a Localizer instance that will access the properties file
     * in the package of the given class using the given locale.
     *
     * @param cls the class whose package to check for the localized
     * properties file; if null, the system will check for
     * a top-level properties file
     * @param locale the locale to which strings should be localized; if
     * null, the system default will be assumed
     */
    public static Localizer forPackage(Class cls, Locale locale) {
        if (locale == null)
            locale = Locale.getDefault();

        int dot = (cls == null) ? -1 : cls.getName().lastIndexOf('.');
        String file;
        if (dot == -1)
            file = "localizer";
        else
            file = cls.getName().substring(0, dot + 1) + "localizer";
        String key = file + locale.toString();

        // no locking; ok if bundle created multiple times

        // check for cached version
        Localizer loc = (Localizer) _localizers.get(key);
        if (loc != null)
            return loc;

        // find resource bundle
        ResourceBundle bundle = null;
        ClassLoader loader = (cls == null) ? null : cls.getClassLoader();
        for (Iterator itr = _providers.iterator(); itr.hasNext();) {
            bundle = ((ResourceBundleProvider) itr.next()).findResource
                (file, locale, loader);
            if (bundle != null)
                break;
        }

        // cache the localizer
        loc = new Localizer(cls == null ? null : cls.getPackage());
        loc._bundle = bundle;
        _localizers.put(key, loc);
        return loc;
    }

    /**
     * Register a resource provider.
     */
    public static void addProvider(ResourceBundleProvider provider) {
        _providers.add(provider);
    }

    /**
     * Remove a resource provider.
     */
    public static boolean removeProvider(ResourceBundleProvider provider) {
        return _providers.remove(provider);
    }

    private Localizer(Package p) {
        _package = p;
    }

    /**
     * Return the localized string matching the given key.
     */
    public Message get(String key) {
        return get(key, null);
    }

    /**
     * Return the localized string matching the given key.
     */
    public Message getFatal(String key) {
        return getFatal(key, null);
    }

    /**
     * Return the localized string matching the given key. The given
     * <code>sub</code> object will be packed into an array and substituted
     * into the found string according to the rules of the
     * {@link MessageFormat} class.
     *
     * @see #get(String)
     */
    public Message get(String key, Object sub) {
        return get(key, new Object[]{ sub });
    }

    /**
     * Return the localized string matching the given key. The given
     * <code>sub</code> object will be packed into an array and substituted
     * into the found string according to the rules of the
     * {@link MessageFormat} class.
     *
     * @see #getFatal(String)
     */
    public Message getFatal(String key, Object sub) {
        return getFatal(key, new Object[]{ sub });
    }

    /**
     * Return the localized string for the given key.
     *
     * @see #get(String,Object)
     */
    public Message get(String key, Object sub1, Object sub2) {
        return get(key, new Object[]{ sub1, sub2 });
    }

    /**
     * Return the localized string for the given key.
     *
     * @see #getFatal(String,Object)
     */
    public Message getFatal(String key, Object sub1, Object sub2) {
        return getFatal(key, new Object[]{ sub1, sub2 });
    }

    /**
     * Return the localized string for the given key.
     *
     * @see #get(String,Object)
     */
    public Message get(String key, Object sub1, Object sub2, Object sub3) {
        return get(key, new Object[]{ sub1, sub2, sub3 });
    }

    /**
     * Return the localized string matching the given key. The given
     * <code>subs</code> objects will be substituted
     * into the found string according to the rules of the
     * {@link MessageFormat} class.
     *
     * @see #get(String)
     */
    public Message get(String key, Object[] subs) {
        return new Message(_package, _bundle, key, subs, false);
    }

    /**
     * Return the localized string matching the given key. The given
     * <code>subs</code> objects will be substituted
     * into the found string according to the rules of the
     * {@link MessageFormat} class.
     *
     * @see #getFatal(String)
     */
    public Message getFatal(String key, Object[] subs) {
        return new Message(_package, _bundle, key, subs, true);
    }

    /**
     * A <code>Message</code> can provide a localized message via the 
     * {@link #getMessage} method call, and can also provide the original key,
     * package, and substitution array that were used to assemble the message.
     */
    public static class Message {

        private final Package _package;
        private final String _key;
        private final Object[] _subs;
        private final String _localizedMessage;
        private final boolean _messageFound;

        private Message(Package pkg, ResourceBundle bundle, String key, 
            Object[] subs, boolean fatal) {
            if (bundle == null && fatal)
                throw new MissingResourceException(key, key, key);

            _package = pkg;
            _key = key;
            _subs = subs;
            if (bundle == null) {
                _localizedMessage = key;
                _messageFound = false;
            } else {
                String localized = null;
                try {
                    localized = bundle.getString(key);
                } catch (MissingResourceException mre) {
                    if (fatal)
                        throw mre;
                }
                _localizedMessage = (localized == null) ? key : localized;
                _messageFound = localized != null;
            }
        }

        /**
         * The localized message.
         */
        public String getMessage() {
            return MessageFormat.format(_localizedMessage, _subs);
        }

        /**
         * The unique key for the localized message.
         */
        public String getKey() {
            return _key;
        }

        /**
         * Substitutions inserted into the message.
         */
        public Object[] getSubstitutions() {
            return _subs;
        }

        /**
         * Package containing localized resource for this message.
         */
        public Package getPackage() {
            return _package;
        }
        
        public String toString() {
            return getMessage();
        }
    }
}
