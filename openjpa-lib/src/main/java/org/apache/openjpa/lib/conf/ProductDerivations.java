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
import java.util.MissingResourceException;

import org.apache.commons.lang.StringUtils;
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
    private static final String[] _prefixes;
    static {
        ClassLoader l = ProductDerivation.class.getClassLoader();
        _derivationNames = Services.getImplementors(ProductDerivation.class, l);
        _derivationErrors = new Throwable[_derivationNames.length];
        List derivations = new ArrayList(_derivationNames.length);
        for (int i = 0; i < _derivationNames.length; i++) {
            try {
                ProductDerivation d = (ProductDerivation) Class.
                    forName(_derivationNames[i], true, l).newInstance();
                d.validate();
                derivations.add(d);
            } catch (Throwable t) {
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
            System.err.println(_loc.get("bad-product-derivations",
                ProductDerivations.class.getName()));
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
        _prefixes = new String[1 + prefixes.size()];
        _prefixes[0] = "openjpa";
        for (int i = 0; i < prefixes.size(); i++)
            _prefixes[i + 1] = (String) prefixes.get(i);
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
     * Apply {@link ProductDerivation#beforeConfigurationConstruct} callbacks
     * to the the given instance. Exceptions are swallowed.
     */
    public static void beforeConfigurationConstruct(ConfigurationProvider cp) {
        for (int i = 0; i < _derivations.length; i++) {
            try {
                _derivations[i].beforeConfigurationConstruct(cp);
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
                _derivations[i].beforeConfigurationLoad(conf);
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
                _derivations[i].afterSpecificationSet(conf);
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
            loader = Thread.currentThread().getContextClassLoader();
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        // most specific to least
        for (int i = _derivations.length - 1; i >= 0; i--) {
            try {
                provider = _derivations[i].load(resource, anchor, loader);
                if (provider != null)
                    return provider;
            } catch (Throwable t) {
                errs = (errs == null) ? new StringBuffer() : errs.append("\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportErrors(errs, resource);
        throw new MissingResourceException(resource, 
            ProductDerivations.class.getName(), resource);
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
        // most specific to least
        for (int i = _derivations.length - 1; i >= 0; i--) {
            try {
                provider = _derivations[i].load(file, anchor);
                if (provider != null)
                    return provider;
            } catch (Throwable t) {
                errs = (errs == null) ? new StringBuffer() : errs.append("\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportErrors(errs, file.getAbsolutePath());
        throw new MissingResourceException(file.getAbsolutePath(), 
            ProductDerivations.class.getName(), file.getAbsolutePath());
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
            loader = Thread.currentThread().getContextClassLoader();
        
        ConfigurationProvider provider = null;
        StringBuffer errs = null;
        String type = (globals) ? "globals" : "defaults";
        // most specific to least
        for (int i = _derivations.length - 1; i >= 0; i--) {
            try {
                provider = (globals) ? _derivations[i].loadGlobals(loader) 
                    : _derivations[i].loadDefaults(loader);
                if (provider != null)
                   return provider;
            } catch (Throwable t) {
                errs = (errs == null) ? new StringBuffer() : errs.append("\n");
                errs.append(_derivations[i].getClass().getName() + ":" + t);
            }
        }
        reportErrors(errs, type);
        return null;
    }
 
    /**
     * Thrown proper exception for given errors.
     */
    private static void reportErrors(StringBuffer errs, String resource) {
        if (errs == null)
            return;
        throw new MissingResourceException(errs.toString(), 
            ProductDerivations.class.getName(), resource);
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
        System.err.println(derivationErrorsToString());
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

