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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.apache.openjpa.eclipse.util.ClassLoaderFromIProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;


/**
 * Locates the required runtime class libraries for OpenJPA by looking up the Bundle manifest.
 * These libraries are embedded in the plugin jar and hence can not directly be used as 
 * classpath entry for a user project. Hence these runtime libaries are read from the
 * plugin jar and copied into the user project.
 *  
 * @author Pinaki Poddar
 *
 */
public class PluginLibrary {
	/**
	 * This identifier must match the <code>Bundle.Symbolic-Name</code> of the root manifest.
	 */
	public static final String BUNDLE_ID = "org.apache.openjpa"; 
	
	/**
	 * Map of library key to marker class name. Used to determine if a specific library
	 * is visible to the user's project's classloader. 
	 */
	private static final Map<String,String> probes = new HashMap<String, String>();
	static {
        probes.put("commons-collections", "org.apache.commons.collections.ArrayStack");
        probes.put("commons-lang",        "org.apache.commons.lang.ObjectUtils");
        probes.put("geronimo-jms",        "javax.jms.Connection");
        probes.put("geronimo-jpa",        "javax.persistence.Entity");
        probes.put("geronimo-jta",        "javax.transaction.Transaction");
        probes.put("openjpa",             "org.apache.openjpa.conf.OpenJPAVersion");
        probes.put("serp",                "serp.bytecode.BCClass");
	}
	
	public String getDescription() {
        Bundle bundle = Platform.getBundle(BUNDLE_ID);
        Object desc = bundle.getHeaders().get(Constants.BUNDLE_DESCRIPTION);
        return desc == null ? "OpenJPA Eclipse Plugin Bundle" : desc.toString();
	}
	
    /**
     * Reads the given bundle manifest for the names of libraries required for 
     * OpenJPA runtime.
     */
    private List<String> getRuntimeLibraries(Bundle bundle) {
        List<String> result = new ArrayList<String>();
        try {
            String cpEntries = (String) bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
            if (cpEntries == null)
                cpEntries = ".";
            ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, cpEntries);
            for (int i = 0; i < elements.length; ++i) {
                ManifestElement element = elements[i];
                String value = element.getValue();
                result.add(value);
            }
        } catch (BundleException e) {
            e.printStackTrace();
        }
        return result;
    }

	/**
	 * Gets the runtime libraries required for this bundle to the given project.
	 * 
	 * @param list of patterns that matches an actual library. null implies all runtime libraries.
	 * @param copy if true then the libraries are copied to the given project directory.
	 */
	public IClasspathEntry[] getLibraryClasspaths(IProject project, List<String> libs, boolean copy) throws CoreException {
        if (libs != null && libs.isEmpty())
            return new IClasspathEntry[0];
        Bundle bundle = Platform.getBundle(BUNDLE_ID);
        List<String> libraries = getRuntimeLibraries(bundle);
        List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
        ProgressMonitorDialog progress = null;
        for (String lib : libraries) {
            try {
                if (".".equals(lib))
                    continue;
                URL url = bundle.getEntry(lib);
                url = FileLocator.resolve(url);
                String urlString = url.getFile();
                if (!urlString.endsWith(".jar") || !matchesPattern(urlString, libs))
                    continue;
                String libName = urlString.substring(urlString.indexOf('!')+1);
                IFile iFile = project.getFile(libName);
                if (iFile == null) {
                    continue;
                }
                IPath outPath = iFile.getRawLocation();
                File outFile = outPath.toFile();
                if (!outFile.getParentFile().exists() && copy) {
                    outFile.getParentFile().mkdirs();
                }
                if (!outFile.exists() && copy) {
                    outFile.createNewFile();
                }
                if (copy) {
                    boolean firstTask = progress == null;
                    if (progress == null) {
                        progress = new ProgressMonitorDialog(Activator.getShell());
                    }
                    if (firstTask) {
                        int nTask = libs == null ? libraries.size() : libs.size();
                        progress.run(true, false, new JarCopier(url.openStream(),outFile, true, nTask));
                    } else {
                        progress.run(true, false, new JarCopier(url.openStream(),outFile));
                    }
                }
                IClasspathEntry classpath = JavaCore.newLibraryEntry(outPath, null, null);
                entries.add(classpath);
            } catch (Exception e) {
                Activator.log(e);
            } finally {
                if (progress != null) {
                    progress.getProgressMonitor().done();
                }
            }
        }
        return entries.toArray(new IClasspathEntry[entries.size()]);
	}
	
	void copyJar(JarInputStream jar, JarOutputStream out) throws IOException {
	    if (jar == null || out == null)
	        return;
	    
	    try {
	        JarEntry entry = null;
	        while ((entry = jar.getNextJarEntry()) != null) {
	            out.putNextEntry(entry);
	            int b = -1;
	            while ((b = jar.read()) != -1) {
	                out.write(b);
	            }
	        }
	        out.closeEntry();
	    } finally {
	        out.finish();
            out.flush();
	        out.close();
	        jar.close();
	    }
	}
	
	/**
	 * Finds the libraries that are required but missing from the given project's classpath.
	 * @param project
	 * @return empty list if no required libraries are missing.
	 */
	public List<String> findMissingLibrary(IProject project) throws CoreException {
	    List<String> missing = new ArrayList<String>();
	    ClassLoader projectClassLoader = ClassLoaderFromIProjectHelper.createClassLoader(project);
	    for (Map.Entry<String, String> e : probes.entrySet()) {
	        try {
	            Class.forName(e.getValue(), false, projectClassLoader);
	        } catch (Exception cnf) {
	            missing.add(e.getKey());
	        }
	    }
	    return missing;
	}
	
	/**
	 * Affirms if any of the given pattern is present in the given full name. 
	 * @return
	 */
	private boolean matchesPattern(String fullName, List<String> patterns) {
	    if (patterns == null)
	        return true;
	    for (String pattern : patterns) {
	        if (fullName.indexOf(pattern) != -1)
	            return true;
	    }
	    return false;
	}

	class JarCopier implements IRunnableWithProgress {
	    final JarInputStream in;
	    final JarOutputStream out;
	    final boolean beginTask;
	    final int size;
	    final String message;
	    public JarCopier(InputStream jar, File outFile)  throws IOException {
	        this(jar, outFile, false, 0);
	    }
	    
        public JarCopier(InputStream jar, File outFile, boolean begin, int size) throws IOException {
            super();
            this.in = new JarInputStream(jar);
            this.out = new JarOutputStream(new FileOutputStream(outFile));
            this.beginTask = begin;
            this.size = size;
            this.message = outFile.getAbsolutePath();
        }
        
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            if (in == null || out == null)
                return;
            if (beginTask)
                monitor.beginTask("Copying OpenJPA runtime libraries to user projects", size);
            monitor.subTask(message);
            try {
                try {
                    JarEntry entry = null;
                    while ((entry = in.getNextJarEntry()) != null) {
                        out.putNextEntry(entry);
                        int b = -1;
                        while ((b = in.read()) != -1) {
                            out.write(b);
                        }
                    }
                    out.closeEntry();
                } finally {
                    out.finish();
                    out.flush();
                    out.close();
                    in.close();
                    monitor.worked(1);
                }
        } catch (IOException ex) {
        } 
	}
	}
}
