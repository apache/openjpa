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
package org.apache.openjpa.persistence.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * Aids to run a single test under different combination of configuration
 * parameters.
 *
 * Each configurable property can be registered to this receiver with all its
 * possible values. This class generates all combinations of all the possible
 * property values as configuration and invokes the same test with each
 * configuration combination.
 * The properties can be designated as <em>runtime</code> to be included in
 * combination for execution but excluded from configuration.
 *
 * For example,
 *
 * @see TestParentChild
 *
 * @author Pinaki Poddar
 *
 */
public class CombinatorialTestHelper {
    private CombinationGenerator generator;
    private List<String> propertyKeys;
    private BitSet runtimeKeys = new BitSet();

    public CombinatorialTestHelper() {
        generator = new CombinationGenerator();
        propertyKeys = new ArrayList<>();
        runtimeKeys = new BitSet();
    }

    public Stream<CombinatorialOption> provideArguments() {
        return Stream.of(generator.generate()).map(CombinatorialOption::new);
    }

    /**
     * Adds options for the given configuration property.
     */
    public CombinatorialTestHelper withOption(String property, Object[] options) {
        return withOption(property, options, false);
    }

    /**
     * Adds options for the given configuration property.
     */
    public CombinatorialTestHelper withOption(String property, List options) {
        return withOption(property, options, false);
    }

    /**
     * Adds options for the given property.
     * If runtime is true then this property is not added to configuration.
     */
    public CombinatorialTestHelper withOption(String property, Object[] options, boolean runtime) {
        return withOption(property, Arrays.asList(options), runtime);
    }

    /**
     * Adds options for the given property.
     * If runtime is true then this property is not added to configuration.
     */
    public CombinatorialTestHelper withOption(String property, List options, boolean runtime) {
        if (generator == null) {
            generator = new CombinationGenerator();
        }
        if (propertyKeys == null) {
            propertyKeys = new ArrayList<>();
        }
        if (!propertyKeys.contains(property)) {
            generator.addDimension(options);
            propertyKeys.add(property);
            if (runtime) {
                runtimeKeys.set(propertyKeys.size() - 1);
            }
        }
        return this;
    }

    public class CombinatorialOption {
        private final List<?> current;

        private CombinatorialOption(List<?> current) {
            this.current = current;
        }

        public List<?> get() {
            return current;
        }

        /**
         * Gets the key and values of the current options as printable string.
         */
        public String getOptionsAsString() {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < propertyKeys.size(); i++) {
                String key = propertyKeys.get(i);
                if (!runtimeKeys.get(i)) {
                    buf.append(key + " : " + getOption(key)).append("\r\n");
                }
            }
            for (int i = 0; i < propertyKeys.size(); i++) {
                String key = propertyKeys.get(i);
                if (runtimeKeys.get(i)) {
                    buf.append("* " + key + " : " + getOption(key)).append("\r\n");
                }
            }
            return buf.toString();
        }

        /**
         * Gets the string value of current option for the given key.
         * Raises exception if the given key is not an option.
         */
        public String getOptionAsString(String key) {
            return getOption(key).toString();
        }

        /**
         * Gets the value of current option for the given key.
         * Raises exception if the given key is not an option.
         */
        public Object getOption(String key) {
            int index = propertyKeys.indexOf(key);
            if (index == -1) {
                throw new IllegalArgumentException("Unknown option " + key);
            }
            return current.get(index);
        }

        public Object[] getProps(Object... props) {
            // Each non-runtime property contributes a key-value pair
            Object[] options = new Object[2 * (propertyKeys.size() - runtimeKeys.cardinality())];
            int k = 0;
            for (int i = 0; i < propertyKeys.size(); i++) {
                if (runtimeKeys.get(i)) {
                    continue;
                }
                options[k++] = propertyKeys.get(i);
                options[k++] = current.get(i);
            }
            if (props == null || props.length == 0) {
                return options;
            }

            Object[] newProps = new Object[props.length + options.length];
            System.arraycopy(props, 0, newProps, 0, props.length);
            System.arraycopy(options, 0, newProps, props.length, options.length);
            return newProps;
        }
    }
}
