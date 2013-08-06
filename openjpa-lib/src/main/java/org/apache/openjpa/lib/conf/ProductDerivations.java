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
package org.apache.openjpa.lib.conf;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.JavaVersions;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Services;

/**
 * Utilities for running product derivations.
 *
 * @author Abe White
 * @nojavadoc
 */
public class ProductDerivations {

    private static final Localizer _loc = Localizer.forPackage
        (ProductDerivations.class);

    private static final ProductDerivation[] _derivations;
    private static final String[] _derivationNames;
    private static final Throwable[] _derivationErrors;
    private static String[] _prefixes;
    static {
        ClassLoader l = (ClassLoader) AccessController.doPrivileged(
            J2DoPrivHelper.getClassLoaderAction(ProductDerivation.class)); 
        _derivationNames = Services.getImplementors(ProductDerivation.class, l);
        _derivationErrors = new Throwable[_derivationNames.length];
        List derivations = new ArrayList(_derivationNames.length);
        for (int i = 0; i < _derivationNames.length; i++) {
            try {
                ProductDerivation d = (ProductDerivation)
                    AccessController.doPrivileged(
                        J2DoPrivHelper.newInstanceAction(
                            Class.forName(_derivationNames[i], true, l)));
                d.validate();
                derivations.add(d);
            } catch (Throwable t) {
                if (t instanceof PrivilegedActionException)
                    t = ((PrivilegedActionException) t).getException();
                _derivationErrors[i] = t;
            }
        }

        // must be at least one product derivation to define metadata factories,
        // etc. 
        if (derivations.isEmpty()) {
            throw new MissingResourceException(_loc.get
                ("no-product-derivations", ProductDerivation.class.getName(),
                derivationErrorsToString()).getMessage(), 
                ProductDerivations.class.getName(),"derivations");
        }

        // if some derivations weren't instantiable, warn
        for (int i = 0; i < _derivationErrors.length; i++) {
            if (_derivationErrors[i] == null)
                continue;
            // START - ALLOW PRINT STATEMENTS
            System.err.println(_loc.get("bad-product-derivations",
                ProductDerivations.class.getName()));
            // STOP - ALLOW PRINT STATEMENTS
            break;
        }

        Collections.sort(derivations, new ProductDerivationComparator());
        _derivations = (ProductDerivation[]) derivations.toArray
            (new ProductDerivation[derivations.size()]);

        List prefixes = new ArrayList(2);
        for (int i = 0; i < _derivations.length; i++) {
            if (_derivations[i].getConfigurationPrefix() != null
                && !"openjpa".equals(_derivations[i].getConfigurationPrefix()))
                prefixes.add(_derivations[i].getConfigurationPrefix());
        }
        String[] prefixArray = new String[1 + prefixes.size()];
        prefixArray[0] = "openjpa";
        for (int i = 0; i < prefixes.size(); i++)
            prefixArray[i + 1] = (String) prefixes.get(i);
        setConfigurationPrefixes(prefixArray);
    }

    /**
     * Return all the product derivations registered in the current classloader
     */
    public static ProductDerivation[] getProductDerivations() {
        return _derivations;
    }

    /**
     * Return the recognized prefixes for configuration properties.
     */
    public static String[] getConfigurationPrefixes() {
        return _prefixes;
    }

    /**
     * Set the configuration prefix array. This is package-visible for 
     * testing purposes.
     * 
     * @since 0.9.7
     */
    static void setConfigurationPrefixes(String[] prefixes) {
        _prefixes = prefixes;
    }
    
    /**
     * Determine the full key name for <code>key</code>, given the registered
     * prefixes and the entries in <code>map</code>. This method
     * computes the appropriate configuration prefix to use by looking 
     * through <code>map</code> for a key starting with any of the known
     * configuration prefixes and ending with <code>key</code> and, if a
     * value is found, using the prefix of that key. Otherwise, it uses
     * the first registered prefix. 
     * 
     * @since 0.9.7
     */
    public static String getConfigurationKey(String partialKey, Map map) {
        String firstKey = null;
        for (int i = 0; map != null && i < _prefixes.length; i++) {
            String fullKey = _prefixes[i] + "." + partialKey;
            if (map.containsKey(fullKey)) {
                if (firstKey == null) 
                    firstKey = fullKey;
                else {
                    // if we've already found a property with a previous 
                    // prefix, then this is a collision.
                    throw new IllegalStateException(_loc.get(
                        "dup-with-different-prefixes", firstKey, fullKey)
                        .getMessage());
                }
            }
        }
        
        if (firstKey == null)
            return _prefixes[0] + "." + partialKey;
        else
            return firstKey;
    }

    /**
     * Apply {@link ProductDerivation#beforeConfigurationConstruct} callbacks
     * to the the given instance. Exceptions other than fatal
     * {@link BootstrapException} are swallowed.
     */
    public static void beforeConfigurationConstruct(ConfigurationProvider cp) {
        for (int i = 0; i < _derivations.length; i++) {
            try {
                _derivations[i].beforeConfigurationConstruct(cp);
            } catch (BootstrapException be) {
            	if (be.isFatal())
            		throw be;
            } catch (Exception e) {
                // can't log; no configuration yet
                e.printStackTrace();
            }
        }
    }

    /**
     * Apply {@link ProductDerivation#beforeConfigurationLoad} callbacks
     * to the the given instance. Exceptions other than fatal
     * {@link BootstrapException} are swallowed.
     */
    public static void beforeConfigurationLoad(Configuration conf) {
        for (int i = 0; i < _derivations.length; i++) {
            try {
                _derivations[i].beforeConfigurationLoad(conf);
            } catch (BootstrapException be) {
            	if (be.isFatal())
            		throw be;
            } catch (Exception e) {
                // logging not configured yet
                e.printStackTrace();
            }
        }
    }

    /**
     * Apply {@link ProductDerivation#afterSpecificationSet} callbacks
     * to the the given instance. Exceptions other than fatal
     * {@link BootstrapException} are swallowed.
     */
    public static void afterSpecificationSet(Configuration conf) {
        for (int i = 0; i < _derivations.length; i++) {
            try {
                _derivations[i].afterSpecificationSet(conf);
            } catch (BootstrapException be) {
            	if (be.isFatal())
            		throw be;
            } catch (Exception e) {
                // logging not configured yet
                e.printStackTrace();
            }
        }
    }

    /**
     * Called as the first step of a Configuration's close() method. 
     * Exceptions are swallowed.
     * 
     * @since 0.9.7
     */
    public static void beforeClose(Configuration conf) {
        for (int i = 0; i < _derivations.length; i++) {
            try {
                _derivations[i].beforeConfigurationClose(conf);
            } catch (Exception e) {
                conf.getConfigurationLog().warn(_loc.get("before-close-ex"), e);
            }
        }
    }

    /**
     * Load the given given resource, or return false if it is not a resource
     * this provider understands. The given class loader may be null.
     *
     * @param anchor optional named anchor within a multiple-configuration
     * resource
     */
    public static ConfigurationProvider load(String resource, String anchor, 
        ClassLoader loader) {
        if (StringUtils.isEmpty(resource))
            return null;
        if (loader == null)
            loader = (ClassLoader) AccessController.doPrivileged(
                J2DoPrivHelper.getContextClassLoaderAction());
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        // most specific to least
        Throwable err = null;
        for (int i = _derivations.length - 1; i >= 0; i--) {
            try {
                provider = _derivations[i].load(resource, anchor, loader);
                if (provider != null)
                    return provider;
            } catch (Throwable t) {
                err = t;
                errs = (errs == null) ? new StringBuffer() : errs.append("\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportErrors(errs, resource, err);
        String rsrc = resource + "#" + anchor;
        throw (MissingResourceException) JavaVersions.initCause
            (new MissingResourceException(rsrc,
                ProductDerivations.class.getName(), rsrc), err);
    }

    /**
     * Load given file, or return false if it is not a file this provider
     * understands.
     *
     * @param anchor optional named anchor within a multiple-configuration file
     */
    public static ConfigurationProvider load(File file, String anchor, 
        ClassLoader loader) {
        if (file == null)
            return null;
        if (loader == null)
            loader = (ClassLoader) AccessController.doPrivileged(
                J2DoPrivHelper.getContextClassLoaderAction());
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        Throwable err = null;
        // most specific to least
        for (int i = _derivations.length - 1; i >= 0; i--) {
            try {
                provider = _derivations[i].load(file, anchor);
                if (provider != null)
                    return provider;
            } catch (Throwable t) {
                err = t;
                errs = (errs == null) ? new StringBuffer() : errs.append("\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        String aPath = (String) AccessController.doPrivileged(
            J2DoPrivHelper.getAbsolutePathAction(file));
        reportErrors(errs, aPath, err);
        String rsrc = aPath + "#" + anchor;
        throw (MissingResourceException) JavaVersions.initCause
            (new MissingResourceException(rsrc,
                ProductDerivations.class.getName(), rsrc), err);
    }
   
    /**
     * Return a {@link ConfigurationProvider} that has parsed system defaults.
     */
    public static ConfigurationProvider loadDefaults(ClassLoader loader) {
        return load(loader, false);
    }

    /**
     * Return a {@link ConfigurationProvider} that has parsed system globals.
     */
    public static ConfigurationProvider loadGlobals(ClassLoader loader) {
        return load(loader, true);
    }
            
    /**
     * Load a built-in resource location.
     */
    private static ConfigurationProvider load(ClassLoader loader, 
       boolean globals) {
        if (loader == null)
            loader = (ClassLoader) AccessController.doPrivileged(
                J2DoPrivHelper.getContextClassLoaderAction());
        
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        String type = (globals) ? "globals" : "defaults";
        Throwable err = null;
        // most specific to least
        for (int i = _derivations.length - 1; i >= 0; i--) {
            try {
                provider = (globals) ? _derivations[i].loadGlobals(loader) 
                    : _derivations[i].loadDefaults(loader);
                if (provider != null)
                   return provider;
            } catch (Throwable t) {
                err = t;
                errs = (errs == null) ? new StringBuffer() : errs.append("\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportErrors(errs, type, err);
        return null;
    }
 
    /**
     * Thrown proper exception for given errors.
     */
    private static void reportErrors(StringBuffer errs, String resource,
        Throwable nested) {
        if (errs == null)
            return;
        throw (MissingResourceException) JavaVersions.initCause
            (new MissingResourceException(errs.toString(),
                ProductDerivations.class.getName(), resource), nested);
    }

    /**
     * Return a List<String> of all the fully-qualified anchors specified in
     * <code>propertiesLocation</code>. The return values must be used in
     * conjunction with <code>propertiesLocation</code>. If there are no
     * product derivations or if no product derivations could find anchors,
     * this returns an empty list.
     *
     * @since 1.1.0
     */
    public static List getFullyQualifiedAnchorsInPropertiesLocation(
        final String propertiesLocation) {
        List fqAnchors = new ArrayList();
        StringBuffer errs = null;
        Throwable err = null;
        for (int i = _derivations.length - 1; i >= 0; i--) {
            try {
                if (propertiesLocation == null) {
                    String loc = _derivations[i].getDefaultResourceLocation();
                    addAll(fqAnchors, loc,
                        _derivations[i].getAnchorsInResource(loc));
                    continue;
                }

                File f = new File(propertiesLocation);
                if (((Boolean) J2DoPrivHelper.isFileAction(f).run())
                    .booleanValue()) {
                    addAll(fqAnchors, propertiesLocation,
                        _derivations[i].getAnchorsInFile(f));
                } else {
                    f = new File("META-INF" + File.separatorChar
                        + propertiesLocation);
                    if (((Boolean) J2DoPrivHelper.isFileAction(f).run())
                        .booleanValue()) {
                        addAll(fqAnchors, propertiesLocation,
                            _derivations[i].getAnchorsInFile(f));
                    } else {
                        addAll(fqAnchors, propertiesLocation,
                            _derivations[i].getAnchorsInResource(
                                propertiesLocation));
                    }
                }
            } catch (Throwable t) {
                err = t;
                errs = (errs == null) ? new StringBuffer() : errs.append("\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportErrors(errs, propertiesLocation, err);
        return fqAnchors;
    }

    private static void addAll(Collection collection, String base,
        Collection newMembers) {
        if (newMembers == null || collection == null)
            return;
        for (Iterator iter = newMembers.iterator(); iter.hasNext(); ) {
            String fqLoc = base + "#" + iter.next();
            if (!collection.contains(fqLoc))
                collection.add(fqLoc);
        }
    }

    /**
     * Compare {@link ProductDerivation}s.
     */
    private static class ProductDerivationComparator
        implements Comparator {

        public int compare(Object o1, Object o2) {
            int type1 = ((ProductDerivation) o1).getType();
            int type2 = ((ProductDerivation) o2).getType();
            if (type1 != type2)
                return type1 - type2;

            // arbitrary but consistent order
            return o1.getClass().getName().compareTo(o2.getClass().
                getName());
		}
	}

    /**
     * Prints product derivation information.
     */
    public static void main(String[] args) {
        // START - ALLOW PRINT STATEMENTS
        System.err.println(derivationErrorsToString());
        // STOP - ALLOW PRINT STATEMENTS
    }

    /**
     * Return a message about the status of each product derivation.
     */
    private static String derivationErrorsToString() {
        StringBuffer buf = new StringBuffer();
        buf.append("ProductDerivations: ").append(_derivationNames.length);
        for (int i = 0; i < _derivationNames.length; i++) {
            buf.append("\n").append(i + 1).append(". ").
                append(_derivationNames[i]).append(": ");
            if (_derivationErrors[i] == null)
                buf.append("OK");
            else
                buf.append(_derivationErrors[i].toString());
        }
        return buf.toString();
    }
}

