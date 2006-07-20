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

import java.beans.BeanInfo;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.log.LogFactory;
import org.apache.openjpa.lib.util.Closeable;

/**
 * Interface for generic configuration objects. Includes the ability
 * to write configuration to and from {@link Properties} instances. Instances
 * are threadsafe for reads, but not for writes.
 *
 * @author Marc Prud'hommeaux
 * @author Abe White
 */
public interface Configuration
    extends BeanInfo, Serializable, Closeable, Cloneable {

    /**
     * Attribute of returned {@link Value} property descriptors listing
     * recognized values for the property.
     */
    public static final String ATTRIBUTE_ALLOWED_VALUES = "allowedValues";

    /**
     * Attribute of the returned {@link Value} property descriptors naming
     * the property's type or category.
     */
    public static final String ATTRIBUTE_TYPE = "propertyType";

    /**
     * Attribute of the returned {@link Value} property descriptors naming
     * the property' hierarchical category.
     */
    public static final String ATTRIBUTE_CATEGORY = "propertyCategory";

    /**
     * Attribute of the returned {@link Value} property descriptors naming
     * the property's ordering in its category.
     */
    public static final String ATTRIBUTE_ORDER = "propertyCategoryOrder";

    /**
     * Attribute of the returned {@link Value} property descriptors naming
     * the interface that plugin values for this property must implement.
     */
    public static final String ATTRIBUTE_INTERFACE = "propertyInterface";

    /**
     * Attribute of the returned {@link Value} property descriptors naming
     * the property's name in XML format (i.e. two-words instead of TwoWords).
     */
    public static final String ATTRIBUTE_XML = "xmlName";

    /**
     * Return the product name. Defaults to <code>solarmetric</code>.
     */
    public String getProductName();

    /**
     * The log factory. If no log factory has been set explicitly,
     * this method will create one.
     */
    public LogFactory getLogFactory();

    /**
     * The log factory.
     */
    public void setLogFactory(LogFactory factory);

    /**
     * Log plugin setting.
     */
    public String getLog();

    /**
     * Log plugin setting.
     */
    public void setLog(String log);

    /**
     * Return the log for the given category.
     *
     * @see #getLogFactory
     */
    public Log getLog(String category);

    /**
     * Return the log to use for configuration messages.
     */
    public Log getConfigurationLog();

    /**
     * Return the {@link Value} for the given property, or null if none.
     */
    public Value getValue(String property);

    /**
     * Return the set of all {@link Value}s.
     */
    public Value[] getValues();

    /**
     * Add the given value to the set of configuration properties. This
     * method replaces any existing value under the same property.
     */
    public Value addValue(Value val);

    /**
     * Remove the given value from the set of configuration properties.
     */
    public boolean removeValue(Value val);

    /**
     * A properties representation of this Configuration.
     * Note that changes made to this properties object will
     * not be automatically reflected in this Configuration object.
     *
     * @param storeDefaults if true, then properties will be written
     * out even if they match the default value for a property
     */
    public Map toProperties(boolean storeDefaults);

    /**
     * Set this Configuration via the given map. Any keys missing from
     * the given map will not be set. Note that changes made to this map
     * will not be automatically reflected in this Configuration object.
     * IMPORTANT: If the map contains instantiated objects(rather than
     * string values), only the string representation of those objects
     * are considered in this configuration's <code>equals</code> and
     * <code>hashCode</code> methods. If the object's property has no
     * string form(such as an {@link ObjectValue}), the object is not
     * part of the equality and hashing calculations.
     */
    public void fromProperties(Map map);

    /**
     * Adds a listener for any property changes. The property events fired
     * will <b>not</b> include the old value.
     *
     * @param listener the listener to receive notification of property changes
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes a listener for any property changes.
     *
     * @param listener the listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Lock down the configuration's state. Attempting to set state on a
     * read-only configuration results in an exception.
     */
    public void setReadOnly(boolean readOnly);

    /**
     * Return true if this configuration is immutable.
     */
    public boolean isReadOnly();

    /**
     * Call the instantiating get methods for all values. Up-front
     * instantiation allows one to avoid the synchronization necessary with
     * lazy instantiation.
     */
    public void instantiateAll();

    /**
     * Free the resources used by this object.
     */
    public void close();

    /**
     * Return a copy of this configuration.
     */
    public Object clone();

    /**
     * Add <code>prefix</code> to the list of prefixes to use
     * to identify valid configuration properties.
     */
    public void addPropertyPrefix(String prefix);
}
