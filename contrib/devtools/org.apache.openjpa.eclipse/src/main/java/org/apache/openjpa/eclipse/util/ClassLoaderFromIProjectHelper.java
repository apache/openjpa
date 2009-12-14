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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.openjpa.eclipse.Activator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * Helper to get a ClassLoader from an Eclipse IProject. 
 *
 * @author Kai Kreuzer
 * @author Michael Vorburger
 */
public class ClassLoaderFromIProjectHelper {
    private static final char BACKSLASH = '\\';
    private static final char FORWARDSLASH = '/';
    private static final String PROTOCOL_FILE = "file:///";
    private static final String JAR_EXTENSION = ".jar";
    private static final String DIRECTORY_MARKER = "/";
    
    /**
     * Creates a classloader for the given Eclipse Project.
     * 
     * @param project a Eclipse Java Project
     * @return a URLClassLoader with the configured classpath of the given project
     * @throws CoreException
     */
	public static ClassLoader createClassLoader(IProject project) throws CoreException {
		IJavaProject javaProject = JavaCore.create(project);
        String[] classPath = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
        URL[] urls = new URL[classPath.length];
        for(int i=0; i < classPath.length; i++) {
        	try {
        		String urlString = PROTOCOL_FILE + classPath[i].replace(BACKSLASH, FORWARDSLASH);
        		
        		// make sure that directory URLs end with a slash as they are otherwise not 
        		// treated as directories but as libraries by the URLClassLoader
        		if(!classPath[i].endsWith(JAR_EXTENSION) && !classPath[i].endsWith(DIRECTORY_MARKER)) 
        		    urlString += DIRECTORY_MARKER;
				urls[i] = new URL(urlString);
			} catch (MalformedURLException e) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						"Could not resolve classpath correctly.", e));
			}
        }
        return URLClassLoader.newInstance(urls);
	}
	
    /**
     * Get the class of the given name by loading it using the given project's classpath.
     * 
     * @return null if the given class can not be loaded.
     */
    public static Class<?> findClass(String className, IProject project) {
        try {
            return Class.forName(className, false, createClassLoader(project));
        } catch (Exception e) {
            return null;
        }
    }

}
