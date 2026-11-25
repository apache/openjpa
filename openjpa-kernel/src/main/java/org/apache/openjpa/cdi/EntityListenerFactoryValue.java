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

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.lib.conf.Configurable;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.PluginValue;
import org.apache.openjpa.lib.conf.Value;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.Localizer;

public class EntityListenerFactoryValue extends PluginValue implements Configurable {
	
	private static final Localizer _loc = Localizer.forPackage(EntityListenerFactoryValue.class);
	
	private static final String KEY = "EntityListenerFactory";
	
	private static final String STATELESS_ALIAS = "stateless";
	
	private static final String CDI_ALIAS = "cdi";
	
	private static final String CDI_FACTORY_CLASSNAME = "org.apache.openjpa.cdi.EntityListenerCDIFactory";

	private Configuration _conf;
	
	private Log _log;

	public EntityListenerFactoryValue() {
		super(KEY, true);
		String[] aliases = new String[] {
				STATELESS_ALIAS, EntityListenerCachedFactory.class.getName(),
				CDI_ALIAS, CDI_FACTORY_CLASSNAME
		};
		setAliases(aliases);
		setDefault(CDI_ALIAS);
	}
	
	@Override
	public Object instantiate(Class<?> type, Configuration c, boolean fatal) {
		Object instance = null;
		try {
			instance = super.instantiate(type, c, true);
		} catch (Throwable t) {
			if (_log.isTraceEnabled()) {
				_log.trace(_loc.get("cdi-not-available"));
			}
			setClassName(EntityListenerCachedFactory.class.getName());
			instance = super.instantiate(type, c, true);
		}
		return instance;
	}

	@Override
	public void setConfiguration(Configuration conf) {
		if (conf instanceof OpenJPAConfiguration c) {
			_conf = c;
			Value value = _conf.getValue(KEY);
			if (value != null) {
				setString(value.getString());
			}
			_log = c.getLog(OpenJPAConfiguration.LOG_RUNTIME);
		}
	}

	@Override
	public void startConfiguration() {
	}

	@Override
	public void endConfiguration() {
	}
	
}
