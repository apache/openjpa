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

package org.apache.openjpa.eclipse;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.openjpa.eclipse.util.ClassLoaderFromIProjectHelper;
import org.apache.openjpa.enhance.PersistenceCapable;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * PDE Test for OpenJPAEnhancerBuilder.
 * 
 * Inspired by ClasspathHelperTest, thanks!
 * Uses same classes and approach as PCEnhancerHelperTest, have a look at that one before this one to better understand.
 *  
 * @author Michael Vorburger
 */
public class EnhancerBuilderTest extends TestCase {

	/*
	 * TODO EnhancerBuilderTest does not yet work! FIXME!!!  
	 * Somehow our Builder doesn't run after all with this test (it works tested standalone of course), why??
	 */

	private static final String PROJECT_NAME = "EnhancerBuilderTest";
	private static final String TESTCLASS_PACKAGE = "org.apache.openjpa.eclipse";
	
	private final static File SRC_PACKAGE_DIR = new File("src/test/notonclasspath/", TESTCLASS_PACKAGE.replace('.', '/'));
	
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

		IProjectDescription desc = project.getDescription();

		// Set the Java and OpenJPA natures on the project, 
		// so that the builders is added and initialized
		desc.setNatureIds(new String[] { JavaCore.NATURE_ID, OpenJPANature.NATURE_ID });
		project.setDescription(desc, null);

		// Declare Java source and output folders
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.setOutputLocation(binFolder.getFullPath(), null);
		IClasspathEntry cpEntry = JavaCore.newSourceEntry(srcFolder.getFullPath());
		javaProject.setRawClasspath(new IClasspathEntry[] { cpEntry }, null);

		// create a Java package and a class
		IPackageFragmentRoot pkgFragmentRoot = javaProject.getPackageFragmentRoot(srcFolder);
		IPackageFragment pkgFragment = pkgFragmentRoot.createPackageFragment(TESTCLASS_PACKAGE, true, null);
		
		// and copy the same classes already used in the PCEnhancerHelperTest
		copyTestClassToPackage(pkgFragment, "TestEntity.java");
		// copyTestClassToPackage(pkgFragment, "NotToEnhance.java");
		javaProject.save(null, true);
	}

	private void copyTestClassToPackage(IPackageFragment pkgFragment, String classFileName) throws IOException, JavaModelException {
		File testEntityFile = new File(SRC_PACKAGE_DIR, classFileName);
		String contents = FileUtils.readFileToString(testEntityFile);
		pkgFragment.createCompilationUnit(classFileName, contents, true, null);
	}

	protected void tearDown() throws Exception {
		project.delete(true, null);
	}

	public void testNoop() {}
	
	// NOTE: This currently fails, see TODO at the beginning of the class...
	public void todotestBuilder() throws Exception {
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		// TODO INCREMENTAL and FULL_BUILD
		
		IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
		if (markers.length != 0) {
			for (IMarker marker : markers) {
				System.err.print(marker.getAttribute(IMarker.LOCATION) + " : ");
				System.err.println(marker.getAttribute(IMarker.MESSAGE));
			}
			fail("Project Build unexpectedly lead to Markers (printed to stderr for debugging)");
		}

		String classFileName = "bin/" + TESTCLASS_PACKAGE.replace('.', '/') + "/TestEntity.class";
		assertTrue(classFileName + " does not exist?!", project.getFile(classFileName).exists());
		// assertTrue(project.getFile("bin/" + TESTCLASS_PACKAGE.replace('.', '/') + "NotToEnhance.class").exists());
		
		ClassLoader cl = ClassLoaderFromIProjectHelper.createClassLoader(project);
		assertNotNull(cl);
		
		Class<?> testClass = cl.loadClass(TESTCLASS_PACKAGE + "." + "TestEntity");
		Assert.assertNotNull(testClass);
		assertEquals("TestEntity", testClass.getSimpleName());
		Assert.assertEquals(1, testClass.getInterfaces().length);
		Assert.assertTrue(testClass.getInterfaces()[0].equals(PersistenceCapable.class));
	}

}
