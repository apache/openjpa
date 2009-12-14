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
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.openjpa.enhance.PersistenceCapable;

/**
 * Test for PCEnhancerHelper implementations.
 *
 * @author Michael Vorburger
 */
public class PCEnhancerHelperTest extends TestCase {
	
	private final static File srcDir = new File("src/test/notonclasspath/");
	private final static File targetDir = new File("target/PCEnhancerHelperTest");
	private final static String classPackage = "org.apache.openjpa.eclipse."; 
	
	public void todotestEnhanceFile() throws Exception {
		String className = "TestEntity";
		
		URL[] urls = new URL[] { targetDir.toURI().toURL() };
		ClassLoader classLoader = new URLClassLoader(urls);
		PCEnhancerHelper eh = new PCEnhancerHelperImpl(classLoader);
		boolean r = checkEnhance(eh, className);
		Assert.assertTrue(r); // was enhanced..

		// Reset/re-initialize classLoader, to freshly load and check the enhanced class
		classLoader = new URLClassLoader(urls);
		Class<?> testClass = classLoader.loadClass(classPackage + className);
		Assert.assertNotNull(testClass);
		Assert.assertEquals(1, testClass.getInterfaces().length);
		Assert.assertTrue(testClass.getInterfaces()[0].equals(PersistenceCapable.class));
	}
	
	public void testEnhancingAClassThatIsNotAnEntity() throws Exception {
	    if (true)
	        return;
		String className = "NotToEnhance";
		
		ClassLoader classLoader = new URLClassLoader(new URL[] { targetDir.toURI().toURL() });
		PCEnhancerHelper eh = new PCEnhancerHelperImpl(classLoader);
		boolean r = checkEnhance(eh, className);
		Assert.assertFalse(r); // NOT enhanced!
	}
	
	private boolean checkEnhance(PCEnhancerHelper eh, String classNameToCheck) throws Exception {
		String classFileName = classPackage.replace('.', '/') + classNameToCheck + ".class";
		
		FileUtils.forceMkdir(targetDir);
		FileUtils.cleanDirectory(targetDir);
		FileUtils.copyFileToDirectory(new File(srcDir, classFileName), 
		        new File(targetDir, classPackage.replace('.', '/')));
		File classFile = new File(targetDir, classFileName);
		assertTrue(classFile.exists());
		
		return eh.enhance(classFile);
	}
}
