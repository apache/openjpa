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

import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

/**
 * PDE Test for ClasspathHelper.
 *
 * @author Kai Kreuzer
 */
public class ClasspathHelperTest extends TestCase {

	private static final String PROJECT_NAME = "test";
	private static final String TESTCLASS_PACKAGE = "com.odcgroup.test";
	private static final String TESTCLASS_NAME = "TestClass";

	private IProject project;

	protected void setUp() throws Exception {
		
		IWorkspace workspace = null;
		try {
			workspace = ResourcesPlugin.getWorkspace();
		}
		catch (IllegalStateException e) {
			fail("workspace is closed, you are most probably running this as a standalone JUnit Test instead of as an Eclipse PDE Plug-In Test?!");
		}

		// create project
		project = workspace.getRoot().getProject(PROJECT_NAME);
		project.create(null);
		project.open(null);

		// create source and output folders
		IFolder srcFolder = project.getFolder("src");
		srcFolder.create(true, true, null);
		IFolder binFolder = project.getFolder("bin");
		binFolder.create(true, true, null);

		// Set the Java nature on the project, so that the builder is added and initialized
		IProjectDescription desc = workspace.newProjectDescription(PROJECT_NAME);
		desc.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(desc, null);

		// Declare Java source and output folders
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.setOutputLocation(binFolder.getFullPath(), null);
		IClasspathEntry cpEntry = JavaCore.newSourceEntry(srcFolder.getFullPath());
		javaProject.setRawClasspath(new IClasspathEntry[] { cpEntry }, null);

		// create a Java package and a class
		IPackageFragmentRoot pkgFragmentRoot = javaProject.getPackageFragmentRoot(srcFolder);
		IPackageFragment pkgFragment = pkgFragmentRoot.createPackageFragment(TESTCLASS_PACKAGE, true, null);
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(
				"/com/odcgroup/classpath/demo/tests/resources/testclasscontent.txt");
		String contents = IOUtils.toString(is);
		pkgFragment.createCompilationUnit(TESTCLASS_NAME + ".java", contents, true, null);
		javaProject.save(null, true);
	}

	protected void tearDown() throws Exception {
		project.delete(true, null);
	}

	public void testCreateClassLoader() throws Exception {
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
		
		assertTrue("Class file has not been built", project.getFile("bin/com/odcgroup/test/TestClass.class").exists());
		
		ClassLoader cl = ClassLoaderFromIProjectHelper.createClassLoader(project);
		assertNotNull(cl);
		try {
			Class<?> clazz = cl.loadClass(TESTCLASS_PACKAGE + "." + TESTCLASS_NAME);
			assertEquals(TESTCLASS_NAME, clazz.getSimpleName());
		} catch (ClassNotFoundException e) {
			fail("Cannot find test class through classloader");
		}
	}

}
