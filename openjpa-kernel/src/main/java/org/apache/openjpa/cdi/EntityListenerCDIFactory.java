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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.util.UserException;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;

public class EntityListenerCDIFactory implements EntityListenerFactory {
	
	private static final Localizer _loc = Localizer.forPackage(EntityListenerCDIFactory.class);
	
	private static final int NORMAL_BEAN_CHECK_DEPTH = 2;

	private BeanManager beanManager;
	
	private Map<Class<?>, EntityListenerWrapper<?>> cache = new ConcurrentHashMap<>();
	
	public EntityListenerCDIFactory() {
		beanManager = lookupBeanManager();
		if (beanManager == null) {
			try {
				beanManager = CDI.current().getBeanManager();
			} catch (IllegalStateException ex) {
				throw new IllegalStateException(_loc.get("cdi-not-available").getMessage());
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> EntityListenerWrapper<T> getWrapper(Class<T> listenerClass) {
		if (cache.containsKey(listenerClass)) {
			return (EntityListenerWrapper<T>) cache.get(listenerClass);
		} else {
			EntityListenerWrapper<T> wrapper = createWrapper(beanManager, listenerClass);
			if (wrapper.cacheable()) {
				cache.putIfAbsent(listenerClass, wrapper);
			}
			return wrapper;
		}
	}
	
	@Override
	public void dispose() {
		cache.forEach((c, w) -> {
			w.dispose();
		});
		cache.clear();
	}
		
	private <T> EntityListenerWrapper<T> createWrapper(BeanManager bm, Class<T> cls) {
		return bm == null ? createNonContextualWrapper(cls) : createContextualWrapper(bm, cls); 
	}
	
	private <T> EntityListenerWrapper<T> createNonContextualWrapper(Class<T> cls) {
		try {
			T instance = J2DoPrivHelper.newInstance(cls);
			return new CacheableListener<T>(instance);
		} catch (Throwable t) {
			throw new UserException(_loc.get("bean-constructor", cls.getName()), t);
		}
	}
	
	private <T> EntityListenerWrapper<T> createContextualWrapper(BeanManager bm, Class<T> cls) {
		// based on DeltaSpike BeanProvider#injectFields
		// added map to allow disposing afterwards as expected by JPA API
		final AnnotatedType<T> type = beanManager.createAnnotatedType(cls);
		final InjectionTarget<T> target = beanManager.getInjectionTargetFactory(type).createInjectionTarget(null);
		final CreationalContext<T> creationalContext = beanManager.createCreationalContext(null);
		T instance = null;
		try {
			instance = target.produce(creationalContext);
			target.inject(instance, creationalContext);
			target.postConstruct(instance);
			boolean cacheable = isCacheable(target);
			final EntityListenerWrapper<T> wrapper = new CDIEntityListenerWrapper<>(instance, target, creationalContext, cacheable);
			return wrapper;
		} catch (Throwable t) {
			try {
				if (instance != null) {
					target.dispose(instance);
				}
			} finally {
				if (creationalContext != null) {
					creationalContext.release();
				}
			}
			throw new UserException(_loc.get("cdi-bean-constructor", cls.getName(), t));
		}
	}

	private BeanManager lookupBeanManager() {
		String[] jndiNames = {"java:comp/BeanManager", "java:comp/env/BeanManager"};
		Context ctx;
		try {
			ctx = new InitialContext();
			for (String jndi: jndiNames) {
				try {
					return (BeanManager) ctx.lookup(jndi);
				} catch (NamingException ex) {
					// swallow
				}
			}
		} catch (NamingException ex) {
			// swallow
		}
		return null;
	}
	
	private <T> boolean isCacheable(InjectionTarget<T> target) {
		Set<InjectionPoint> ips = target.getInjectionPoints();
		boolean hasInjections = !ips.isEmpty();
		if (hasInjections) {
			return ips.stream().allMatch(ip -> isCacheable(ip, NORMAL_BEAN_CHECK_DEPTH));
		}
		return true;
	}
	
	private boolean isCacheable(InjectionPoint ip, int depth) {
		Set<Bean<?>> candidateBeans = beanManager.getBeans(ip.getType(), ip.getQualifiers().toArray(new Annotation[0]));
		Bean<?> bean = beanManager.resolve(candidateBeans);
		if (depth <= 0) {
			return beanManager.isNormalScope(bean.getScope());
		} else {
			if (bean.getTypes().contains(Instance.class)) {
				return true;
			}
			if (!beanManager.isNormalScope(bean.getScope())) {
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
