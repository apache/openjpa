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
package org.apache.openjpa.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.MissingResourceException;

import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.lib.util.Localizer;
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
          ProductDerivations.class.getClassLoader());
        List derivations = new ArrayList(pdcls.length);
        for (int i = 0; i < pdcls.length; i++) {
            try {
                derivations.add(pdcls[i].newInstance());
            } catch (Throwable t) {
                // invalid service
            }
        }

        // there must be some product derivation to define metadata factories,
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
    public static void beforeConfigurationLoad(OpenJPAConfiguration conf) {
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
    public static void afterSpecificationSet(OpenJPAConfiguration conf) {
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

