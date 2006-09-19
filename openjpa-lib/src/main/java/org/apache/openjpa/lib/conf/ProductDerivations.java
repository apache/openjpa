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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import org.apache.openjpa.lib.util.JavaVersions;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.lib.util.Services;

/**
 * Utilities for running product derivations.
 *
 * @author Abe White
 * @nojavadoc
 */
public class ProductDerivations {

    private static final ProductDerivation[] _derivations;

    static {
        Class[] pdcls = Services.getImplementorClasses(ProductDerivation.class,
          ProductDerivation.class.getClassLoader());
        List derivations = new ArrayList(pdcls.length);
        for (int i = 0; i < pdcls.length; i++) {
            try {
                derivations.add(pdcls[i].newInstance());
            } catch (Throwable t) {
                // invalid service
                t.printStackTrace();
            }
        }

        // must be at least one product derivation to define metadata factories,
        // etc. 
        if (derivations.isEmpty()) {
            Localizer loc = Localizer.forPackage(ProductDerivations.class);
            throw new MissingResourceException(loc.get("no-product-derivations",
                ProductDerivation.class.getName()).getMessage(),
                ProductDerivations.class.getName(), "derivations");
        }

        Collections.sort(derivations, new ProductDerivationComparator());
        _derivations = (ProductDerivation[]) derivations.toArray
            (new ProductDerivation[derivations.size()]);
    }

    /**
     * Apply {@link ProductDerivation#beforeConfigurationConstruct} callbacks
     * to the the given instance. Exceptions are swallowed.
     */
    public static void beforeConfigurationConstruct(ConfigurationProvider cp) {
        for (int i = 0; i < _derivations.length; i++) {
            try {
                boolean ret = _derivations[i].beforeConfigurationConstruct(cp);
            } catch (Exception e) {
                // can't log; no configuration yet
                e.printStackTrace();
            }
        }
    }

    /**
     * Apply {@link ProductDerivation#beforeConfigurationLoad} callbacks
     * to the the given instance. Exceptions are swallowed.
     */
    public static void beforeConfigurationLoad(Configuration conf) {
        for (int i = 0; i < _derivations.length; i++) {
            try {
                boolean ret = _derivations[i].beforeConfigurationLoad(conf);
            } catch (Exception e) {
                // logging not configured yet
                e.printStackTrace();
            }
        }
    }

    /**
     * Apply {@link ProductDerivation#afterSpecificationSet} callbacks
     * to the the given instance. Exceptions are swallowed.
     */
    public static void afterSpecificationSet(Configuration conf) {
        for (int i = 0; i < _derivations.length; i++) {
            try {
                boolean ret = _derivations[i].afterSpecificationSet(conf);
            } catch (Exception e) {
                // logging not configured yet
                e.printStackTrace();
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
        if (resource == null || resource.length() == 0)
            return null;
        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        for (int i = 0; i < _derivations.length; i++) {
            try {
                provider = _derivations[i].load(resource, anchor, loader);
                if (provider != null) {
                    return provider;
                }
            } catch (Throwable t) {
                errs = (errs == null) ? new StringBuffer() 
                        : errs.append("\r\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportError(errs, resource);
        return null;
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
            loader = Thread.currentThread().getContextClassLoader();
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        for (int i = 0; i < _derivations.length; i++) {
            try {
                provider = _derivations[i].load(file, anchor);
                if (provider != null) {
                    return provider;
                }
            } catch (Throwable t) {
                errs = (errs == null) ? new StringBuffer() 
                        : errs.append("\r\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportError(errs, file.getAbsolutePath());
        return null;
    }
    
    public static ConfigurationProvider load(String rsrc, String anchor, 
        Map m) {
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        for (int i = 0; i < _derivations.length; i++) {
            try {
                provider = _derivations[i].load(rsrc, anchor, m);
                if (provider != null) {
                    return provider;
                }
            } catch (Throwable t) {
                errs = (errs == null) ? new StringBuffer() 
                        : errs.append("\r\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportError(errs, rsrc);
        return null;
    }
        
    public static ConfigurationProvider load(ClassLoader loader, 
       boolean globals) {
        if (loader == null)
            loader = Thread.currentThread().getContextClassLoader();
        
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        String type = (globals) ? "globals" : "defaults";
        for (int i = 0; i < _derivations.length; i++) {
            try {
                provider = (globals) ? _derivations[i].loadGlobals(loader) 
                        : _derivations[i].loadDefaults(loader);
                if (provider != null) {
                   return provider;
                }
            } catch (Throwable t) {
                errs = (errs == null) ? new StringBuffer() 
                        : errs.append("\r\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportError(errs, type);
        return null;
    }
    
    private static void reportError(StringBuffer errs, String resource) {
        if (errs == null)
            return;
        throw new MissingResourceException(
                errs.toString(), ProductDerivations.class.getName(), resource);
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
}

