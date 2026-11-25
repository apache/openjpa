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
package org.apache.openjpa.cdi;

import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.PluginValue;

public class EntityListenerFactoryValue extends PluginValue {
	
	private static final String KEY = "EntityListenerFactory";
	
	private static final String DEFAULT_ALIAS = "default";

	public EntityListenerFactoryValue() {
		super(KEY, false);
		String[] aliases = new String[] {
				DEFAULT_ALIAS, EntityListenerCachedFactory.class.getName()
		};
		setAliases(aliases);
		setDefault(aliases[0]);
	}
	
	@Override
	public Object instantiate(Class<?> type, Configuration c, boolean fatal) {
		if (isBeanManagerAPIAvailable()) {
			setClassName("org.apache.openjpa.cdi.EntityListenerCDIFactory");
		} else {
			setClassName(EntityListenerCachedFactory.class.getName());
		}
		return super.instantiate(type, c, fatal);
	}

	private boolean isBeanManagerAPIAvailable() {
		try {
			// false avoids running static initializers just to check existence
			Class.forName("jakarta.enterprise.inject.spi.BeanManager", false, EntityListenerCDIFactory.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}
}
