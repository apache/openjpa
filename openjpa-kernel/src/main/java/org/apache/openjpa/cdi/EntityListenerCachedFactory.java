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

import java.util.HashMap;
import java.util.Map;

import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

public class EntityListenerCachedFactory implements EntityListenerFactory {

	private static final Localizer _loc = Localizer.forPackage(EntityListenerFactoryImpl.class);
	
	private Map<Class<?>, EntityListenerWrapper<?>> cache = new HashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> EntityListenerWrapper<T> getWrapper(Class<T> listenerClass) {
		return (EntityListenerWrapper<T>) cache.computeIfAbsent(listenerClass, this::createCacheableListener);
	}
	
	private <T> EntityListenerWrapper<T> createCacheableListener(Class<T> cls) {
		try {
			T instance = J2DoPrivHelper.newInstance(cls);
			return new CacheableListener<T>(instance);
		} catch (Throwable t) {
			throw new UserException(_loc.get("bean-constructor", cls.getName()), t);
		}
	}

}
