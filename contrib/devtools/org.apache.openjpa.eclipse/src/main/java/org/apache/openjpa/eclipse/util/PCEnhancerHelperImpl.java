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
import java.util.Properties;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.enhance.PCEnhancer;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.persistence.ManagedInterface;
import org.eclipse.core.runtime.CoreException;

import serp.bytecode.Annotations;
import serp.bytecode.BCClass;
import serp.bytecode.Project;

/**
 * OpenJPA Enhancer helper, for efficient invocation from an Eclipse builder.
 * 
 * @author Pinaki Poddar
 * @author Michael Vorburger (refactoring and extensions)
 */
public class PCEnhancerHelperImpl implements PCEnhancerHelper {

	private final OpenJPAConfiguration conf;
	private final MetaDataRepository repos;
	private final ClassLoader loader;
	// TODO, needed? private final ClassArgParser cap;

	public PCEnhancerHelperImpl(ClassLoader classLoader) {
		this.loader = classLoader;
		conf = new OpenJPAConfigurationImpl();
		Properties prop = new Properties();
		prop.setProperty("openjpa.MetaDataFactory", "jpa");
		prop.setProperty("openjpa.Log", EclipseLogFactory.class.getName());
		conf.fromProperties(prop);
		
		repos = conf.getMetaDataRepositoryInstance();
		repos.setSourceMode(MetaDataRepository.MODE_META);
		
		// TODO, needed? cap = conf.getMetaDataRepositoryInstance().getMetaDataFactory().newClassArgParser();
        // cap.setClassLoader(loader);
	}

	/* (non-Javadoc)
	 * @see org.apache.openjpa.eclipse.util.PCEnhancerHelper#enhance(java.io.File)
	 */
	public boolean enhance(File resource) throws CoreException, IOException, ClassNotFoundException {
		// No try/catch here - let Exception propagate upwards
		// Eclipse will catch them and abandon Build and show cause in the Error Log
		// Build should stop; if caught here, big projects with some set-up problem will appear to "hang". 
		
		// TODO, needed? cap.parseTypes(resource.getAbsolutePath().toString())
		
		Project serp = new Project();
		BCClass bcls = serp.loadClass(resource, loader);
		
		if (!needsEnhance(bcls))
			return false;
		
		// Intentionally setting initializeClass to false;
		// we do NOT want static class initialization to be executed during the builder!
		Class<?> cls = Class.forName(bcls.getName(), false, loader);
		
		repos.removeMetaData(cls);
		ClassMetaData meta = repos.addMetaData(cls);

		PCEnhancer delegate = new PCEnhancer(repos, bcls, meta);
		int result = delegate.run();
		if ((result != PCEnhancer.ENHANCE_NONE)
		 && (result != PCEnhancer.ENHANCE_INTERFACE)) 
		{
			delegate.record();
			return true;
		} else {
			return false;
		}
	}

	private boolean needsEnhance(BCClass bcls) {
		Annotations annos = bcls.getDeclaredRuntimeAnnotations(false);
		
		if (annos == null) 
			return false;
		
		return (annos.getAnnotation(Entity.class) != null 
			 || annos.getAnnotation(MappedSuperclass.class) != null
			 || annos.getAnnotation(Embeddable.class) != null
			 || annos.getAnnotation(ManagedInterface.class) != null);
		
	}
}
