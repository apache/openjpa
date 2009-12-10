/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openjpa.eclipse.util;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;

/**
 * PCEnhancerHelper implementation wrapping around the original
 * PCEnhancerHelperImpl, but delegating lookup of the actual OpenJPA classes to
 * a passed-in classloader (the one of the project).
 * 
 * The idea is to use the OpenJPA JARs from the projects under enhancement, and
 * in the future no longer bundle them in the plug-in. This way the plug-in is
 * independent of the OpenJPA version. This is important as the enhancer
 * implementation can change between OpenJPA versions, and the enhancer of the
 * same OpenJPA version than the project has on it's classpath should naturally
 * be used, not some fixed arbitrary version that happens to come with the
 * plug-in.
 * 
 * @author Michael Vorburger
 */
public class PCEnhancerHelperClassLoadingWrapper implements PCEnhancerHelper {

	/*
	 * TODO PCEnhancerHelperClassLoadingWrapper does not yet work! FIXME!!!  
	 * Thread.currentThread().setContextClassLoader seems to have no effect, how the h%#§ ??? ;-)
	 */
	
	private final ClassLoader classLoader;
	private final PCEnhancerHelper delegateHelper;
	
	public PCEnhancerHelperClassLoadingWrapper(ClassLoader classLoader) {
		this.classLoader = new FunkyClassLoader(classLoader, Thread.currentThread().getContextClassLoader());

		ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(this.classLoader);
		try {
			delegateHelper = new PCEnhancerHelperImpl(classLoader);
		} finally {
			Thread.currentThread().setContextClassLoader(currentCL);
		}
	}
	
	public boolean enhance(File resource) throws CoreException, IOException, ClassNotFoundException {
		ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(this.classLoader);
		try {
			return delegateHelper.enhance(resource);
		} finally {
			Thread.currentThread().setContextClassLoader(currentCL);
		}
	}
	
	private static class FunkyClassLoader extends ClassLoader {
		final private ClassLoader delegate;
		final private ClassLoader cl;

		public FunkyClassLoader(ClassLoader cl, ClassLoader delegate) {
			this.cl = cl;
			this.delegate = delegate;
		}
		
		// TODO Is it loadClass() or rather findClass() that needs overriding? 
		
		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			try {
				return cl.loadClass(name);
			} catch(ClassNotFoundException e) {
				return delegate.loadClass(name);
			}
		}
	}
}
