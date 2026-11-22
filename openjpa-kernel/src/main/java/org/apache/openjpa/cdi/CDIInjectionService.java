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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;

public class CDIInjectionService {
	
	private boolean cdiCtxAvailable = true;
	
	private Map<Class<?>, InjectionManager<?>> injectors = Collections.synchronizedMap(new HashMap<>());
	
	public CDIInjectionService() {
	}

	@SuppressWarnings("unchecked")
	public <T> InjectionManager<T> getInjectionManager(Class<T> cls) {
		return (InjectionManager<T>) injectors.computeIfAbsent(cls, (clazz) -> buildInjectionManager(clazz));
	}
	
	private <T> InjectionManager<T> buildInjectionManager(Class<T> cls) {
		BeanManager beanManager = null;
		if (cdiCtxAvailable) {
			try {
				beanManager = CDI.current().getBeanManager();
			} catch (IllegalStateException ise) {
				cdiCtxAvailable = false;
			}
		}
		if (beanManager != null) {
			return new CDIInjectionManager<T>(beanManager, cls);
		} else {
			return new CDIUnawareInjectionManager<T>(cls);
		}
	}
	
	public void dispose() {
		for (InjectionManager<?> injector: injectors.values()) {
			injector.dispose();
		}
		injectors.clear();
	}

}
