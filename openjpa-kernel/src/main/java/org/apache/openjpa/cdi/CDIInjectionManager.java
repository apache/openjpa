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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanContainer;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;

public class CDIInjectionManager<T> implements InjectionManager<T>{
	
    private static final Localizer _loc = Localizer.forPackage(CDIUnawareInjectionManager.class);

	private static final int NORMAL_BEAN_CHECK_DEPTH = 2;
	
	private BeanManager beanManager;
	
	private CreationalContext<T> creationalContext;
	
	final Map<T, InjectionTarget<T>> targets = Collections.synchronizedMap(new HashMap<>());
	
	private transient T cachedInstance;
	
	private Boolean cacheable;
	
	private Boolean hasInjections = true;
	
	private Class<T> listenerClass;
	
	public CDIInjectionManager(BeanManager beanManager, Class<T> clazz) {
		this.beanManager = beanManager;
		listenerClass = clazz;
	}
	
	@Override
	public T createInstance(final Class<T> cls) {
		if (cachedInstance == null) {
			// based on DeltaSpike BeanProvider#injectFields
			// added map to allow disposing afterwards as expected by JPA API
			final AnnotatedType<T> type = beanManager.createAnnotatedType(cls);
			final InjectionTarget<T> target = beanManager.getInjectionTargetFactory(type).createInjectionTarget(null);
			creationalContext = beanManager.createCreationalContext(null);
			final T instance = target.produce(creationalContext);
			targets.put(instance, target);
			target.inject(instance, creationalContext);
			target.postConstruct(instance);
			if (cacheable == null) {
				checkInjections(target);
				cachedInstance = cacheable ? instance : null;
			}
			return instance;
		} else {
			return cachedInstance;
		}
	}
	
	@Override
	public boolean isCacheable() {
		return cacheable;
	}

	@Override
	public void dispose() {
		try {
			for (Entry<T, InjectionTarget<T>> entry: targets.entrySet()) {
				T instance = entry.getKey();
				InjectionTarget<T> target = entry.getValue();
				target.preDestroy(instance);
				target.dispose(instance);
			}
		} finally {
			if (creationalContext != null) {
				creationalContext.release();
			}
		}
	}
	
	private void checkInjections(InjectionTarget<T> target) {
		Set<InjectionPoint> ips = target.getInjectionPoints();
		hasInjections = !ips.isEmpty();
		if (hasInjections) {
			cacheable = ips.stream().allMatch(ip -> isCacheable(ip, NORMAL_BEAN_CHECK_DEPTH));
		} else {
			cacheable = true;
	        try {
	            cachedInstance = J2DoPrivHelper.newInstance(listenerClass);
	        } catch (Throwable t) {
	            throw new UserException(_loc.get("bean-constructor", listenerClass.getName()), t);
	        }
		}
	}
	
	private boolean isCacheable(InjectionPoint ip, int depth) {
		BeanContainer container = CDI.current().getBeanContainer();
		Annotation[] qualifiers = ip.getQualifiers().toArray(new Annotation[0]);
		Bean<?> bean = container.resolve(container.getBeans(ip.getType(), qualifiers));
		if (depth <= 0) {
			return container.isNormalScope(bean.getScope());
		} else {
			if (!container.isNormalScope(bean.getScope())) {
				return false;
			}
			for (InjectionPoint iip: bean.getInjectionPoints()) {
				if (!isCacheable(iip, depth - 1)) {
					return false;
				}
			}
			return true;
		}
	}
	
}
