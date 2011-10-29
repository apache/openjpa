/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIESOR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.osgi;

import org.apache.openjpa.lib.conf.ConfigurationProvider;
import org.apache.openjpa.persistence.PersistenceProductDerivation;

/**
 * Derives for OSGi environment. Adds bundle class loader in class loading scheme.
 * 
 * @author Pinaki Poddar
 *
 */
public class OSGiDerivation extends PersistenceProductDerivation {

	@Override
	public void validate() throws Exception {
		if (!BundleUtils.runningUnderOSGi()) {
			throw new RuntimeException("Not running in OSGi environment");
		}
	}

	@Override
	public boolean beforeConfigurationConstruct(ConfigurationProvider cp) {
		cp.getClassLoader().addClassLoader(BundleUtils.getBundleClassLoader());
		super.beforeConfigurationConstruct(cp);
		return true;
	}
}
