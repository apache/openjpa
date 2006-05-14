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


import java.beans.*;
import java.io.*;
import java.util.*;

import org.apache.openjpa.lib.log.*;
import org.apache.openjpa.lib.util.Closeable; 


/**
 *	<p>Interface for generic configuration objects.  Includes the ability
 *	to write configuration to and from {@link Properties} instances.</p>
 *
 *	@author Marc Prud'hommeaux
 *	@author Abe White
 */
public interface Configuration
	extends BeanInfo, Serializable, Closeable, Cloneable
{
	/**
	 *	Attribute of returned {@link Value} property descriptors listing 
	 *	recognized values for the property.
	 */
	public static final String ATTRIBUTE_ALLOWED_VALUES = "allowedValues";

	/**
	 *	Attribute of the returned {@link Value} property descriptors naming 
	 *	the property's type or category.
	 */	
	public static final String ATTRIBUTE_TYPE = "propertyType";

	/**
	 *	Attribute of the returned {@link Value} property descriptors naming 
	 *	the property' hierarchical category.
	 */	
	public static final String ATTRIBUTE_CATEGORY = "propertyCategory";

	/**
	 *	Attribute of the returned {@link Value} property descriptors naming 
	 *	the property's ordering in its category.
	 */	
	public static final String ATTRIBUTE_ORDER = "propertyCategoryOrder";


	/**
	 *	Return the product name.  Defaults to <code>solarmetric</code>.
	 */
	public String getProductName ();


	/**
	 *	The log factory. If no log factory has been set explicitly,
 	 *	this method will create one.
	 */
	public LogFactory getLogFactory ();


	/**
	 * 	 The log factory.
	 */
	public void setLogFactory (LogFactory factory);


	/**
	 *	Log plugin setting.
	 */
	public String getLog ();


	/**
	 *	Log plugin setting.
	 */
	public void setLog (String log);


	/**
	 *	Return the log for the given category.
	 *
	 *	@see	#getLogFactory
	 */
	public Log getLog (String category);


	/**
	 *	Return the log to use for configuration messages.
	 */
	public Log getConfigurationLog ();


	/**
	 *	Return the log to use for management messages.
	 */
	public Log getManagementLog ();


	/**
	 *	Return the log to use for profiling messages.
	 */
	public Log getProfilingLog ();


	/**
	 *	Return the {@link Value} for the given property, or null if none.
	 */
	public Value getValue (String property);


	/**
	 *	Return the set of all {@link Value}s.
	 */
	public Value[] getValues ();


	/**
	 *	A properties representation of this Configuration.
	 *	Note that changes made to this properties object will
	 *	not be automatically reflected in this Configuration object.
	 *
	 *	@param storeDefaults if true, then properties will be written
	 * 						out even if they match the default value
	 * 						for a property
	 */
	public Properties toProperties (boolean storeDefaults);


	/**
	 *	Set this Configuration via the given map.  Any keys missing from
	 *	the given map will not be set. Note that changes made to this map 
	 *	will not be automatically reflected in this Configuration object.
	 *
	 *	IMPORTANT: If the map contains instantiated objects (rather than 
	 *	string values), only the string representation of those objects
	 *	are considered in this configuration's <code>equals</code> and 
	 *	<code>hashCode</code> methods.   If the object's property has no
	 *	string form (such as an {@link ObjectValue}), the object is not
	 *	part of the equality and hashing calculations.
	 */
	public void fromProperties (Map map);


	/** 
	 *  Adds a listener for any property changes.  The property events fired
	 * 	will <b>not</b> include the old value.
	 *  
	 *  @param  listener  	the listener to receive notification
	 *  					of property changes
	 */
	public void addPropertyChangeListener (PropertyChangeListener listener);


	/** 
	 *  Removes a listener for any property changes.
	 *  
	 *  @param  listener  the listener to remove
	 */
	public void removePropertyChangeListener (PropertyChangeListener listener);


	/**
	 *	Lock down the configuration's state.  Attempting to set state on a
	 *	read-only configuration results in an exception.
	 */
	public void setReadOnly (boolean readOnly);


	/**
	 *	Return true if this configuration is immutable.
	 */
	public boolean isReadOnly ();


	/**
	 *	Free the resources used by this object.
	 */
	public void close ();


	/**
	 *	Return a copy of this configuration.
	 */
	public Object clone ();
}
