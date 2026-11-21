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
import java.util.Iterator;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.CDIProvider;
import jakarta.enterprise.util.TypeLiteral;

public class CDIBeanManagerProvider implements CDIProvider {

	@Override
	public CDI<Object> getCDI() {
		return new CDI<Object>() {
			
			@Override
			public Object get() {
				return null;
			}
			
			@Override
			public Iterator<Object> iterator() {
				return null;
			}
			
			@Override
			public <U> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
				return null;
			}
			
			@Override
			public <U> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
				return null;
			}
			
			@Override
			public Instance<Object> select(Annotation... qualifiers) {
				return null;
			}
			
			@Override
			public boolean isUnsatisfied() {
				return false;
			}
			
			@Override
			public boolean isAmbiguous() {
				return false;
			}
			
			@Override
			public Iterable<? extends Handle<Object>> handles() {
				return null;
			}
			
			@Override
			public Handle<Object> getHandle() {
				return null;
			}
			
			@Override
			public void destroy(Object instance) {
			}
			
			@Override
			public BeanManager getBeanManager() {
				return null;
			}
		};
	}
	
	@Override
	public int getPriority() {
		// This provider should be the last to be used, as it is a noop provider
		return -1;
	}
	
}
