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
import java.util.Map.Entry;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;

public class CDIInjectionManager<T> implements InjectionManager<T>{
	
	private BeanManager beanManager;
	
	private CreationalContext<T> creationalContext;
	
	final Map<T, InjectionTarget<T>> targets = Collections.synchronizedMap(new HashMap<>());
	
	public CDIInjectionManager() throws NamingException {
		// Try to retrieve BeanManager from JNDI context
		Context ctx = new InitialContext();
		try {
			beanManager = (BeanManager) ctx.lookup("java:comp/BeanManager");
		} catch (NamingException ex) {
			beanManager = (BeanManager) ctx.lookup("java:comp/env/BeanManager");
		}
	}

	@Override
	public T createInstance(final Class<T> cls) {
		// based on DeltaSpike BeanProvider#injectFields
		// added map to allow disposing afterwards as expected by JPA API
		final AnnotatedType<T> type = beanManager.createAnnotatedType(cls);
		final InjectionTarget<T> target = beanManager.getInjectionTargetFactory(type).createInjectionTarget(null);
		creationalContext = beanManager.createCreationalContext(null);
		final T instance = target.produce(creationalContext);
		targets.put(instance, target);
		target.inject(instance, creationalContext);
		target.postConstruct(instance);
		return instance;
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

}
