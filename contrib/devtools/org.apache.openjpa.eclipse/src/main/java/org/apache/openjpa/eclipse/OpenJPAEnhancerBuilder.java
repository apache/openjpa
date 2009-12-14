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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.openjpa.eclipse.util.ClassLoaderFromIProjectHelper;
import org.apache.openjpa.eclipse.util.LogUtil;
import org.apache.openjpa.eclipse.util.PCEnhancerHelper;
import org.apache.openjpa.eclipse.util.PCEnhancerHelperImpl;
import org.apache.openjpa.eclipse.util.PathMatcherUtil;
import org.apache.openjpa.lib.util.MultiClassLoader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

/**
 * Builder for the OpenJPA PCEnhancer.
 * 
 * @see org.apache.openjpa.enhance.PCEnhancer
 * 
 * @author Eclipse PDE Example Wizard! ;-)
 * @author Michael Vorburger (MVO)
 * @author Pinaki Poddar
 */
public class OpenJPAEnhancerBuilder extends IncrementalProjectBuilder implements IElementChangedListener {

	public static final String BUILDER_ID = "org.apache.openjpa.eclipse.OpenJPAEnhancerBuilder";

	private static final String MARKER_TYPE = "org.apache.openjpa.eclipse.openJPAEnhancementProblem";
    private static final Map<IProject,PCEnhancerHelper> _enhancers = new HashMap<IProject, PCEnhancerHelper>();

    public OpenJPAEnhancerBuilder() {
        super();
        JavaCore.addElementChangedListener(this, ElementChangedEvent.POST_CHANGE);
    }
    
	private class MyIncrementalBuildResourceDeltaVisitor implements IResourceDeltaVisitor {
		private final IProgressMonitor monitor;
		private final PCEnhancerHelper enhancerHelper;
		private final BuilderOptions opts;

		public MyIncrementalBuildResourceDeltaVisitor(IProgressMonitor monitor, PCEnhancerHelper enhancerHelper, 
		        BuilderOptions opts) {
			this.monitor = monitor;
			this.enhancerHelper = enhancerHelper;
			this.opts = opts;
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			// better do NOT use monitor.worked() & monitor.subTask() here, as this is fast enough  
		    // and any UI will only slow it down
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
			// If Added or Changed, handle changed resource:
			case IResourceDelta.ADDED:
			case IResourceDelta.CHANGED:
				if (needsEnhancement(resource, opts)) {
					if (enhance(resource, this.enhancerHelper, opts)) {
						delta.getResource().refreshLocal(IResource.DEPTH_ZERO, monitor);
					}
				}
				break;
			// If Removed, nothing to do:
			case IResourceDelta.REMOVED:
				break;
			}
			// return true to continue visiting children
			return true;
		}
	}

	private class MyFullBuildResourceVisitor implements IResourceVisitor {
		private final IProgressMonitor monitor;
		private final List<IResource> list = new LinkedList<IResource>();
		private final BuilderOptions opts;

		public MyFullBuildResourceVisitor(IProgressMonitor monitor, BuilderOptions opts) {
			this.monitor = monitor;
			this.opts = opts;
		}

		public boolean visit(IResource resource) throws CoreException {
			// NO monitor.worked(1);

			if (needsEnhancement(resource, opts))
				list.add(resource);
				
			checkCancel(monitor);
			
			// return true to continue visiting children
			return true;
		}
		
		List<IResource> getResourcesPotentiallyNeedingEnhancement() {
			return list;
		}
	}

	
	@Override
	@SuppressWarnings("unchecked")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		BuilderOptions opts = new BuilderOptions();
		opts.pathMatcher = new PathMatcherUtil(args);
		opts.isVerboseLoggingEnabled = isFullLoggingEnabled(args);
		
		if (kind == FULL_BUILD) {
			fullBuild(monitor, opts);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor, opts);
			} else {
				incrementalBuild(delta, monitor, opts);
			}
		}
		return null;
	}

	private void fullBuild(final IProgressMonitor monitor, BuilderOptions opts) throws CoreException {
		long startTime = System.currentTimeMillis();
		monitor.subTask("OpenJPA Enhancement... (Full Build, collecting resources)");
		MyFullBuildResourceVisitor visitor = new MyFullBuildResourceVisitor(monitor, opts);
		getProject().accept(visitor);
		long duration = System.currentTimeMillis() - startTime;
		if (opts.isVerboseLoggingEnabled)
			LogUtil.logOK("OpenJPA Enhancement (Full Build, collecting resources) took "
				+ duration + "ms, found " + visitor.getResourcesPotentiallyNeedingEnhancement().size()
				+ " classes potentially needing enhancement");
	
		List<IResource> resources = visitor.getResourcesPotentiallyNeedingEnhancement();
		if (!resources.isEmpty()) {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 
			    "OpenJPA Enhancement... (Full Build, actual bytecode work)", resources.size());
			try {
				int actuallyEnhanced = 0;
				startTime = System.currentTimeMillis();
				ClassLoader classLoader = ClassLoaderFromIProjectHelper.createClassLoader(getProject());

				PCEnhancerHelper enhancerHelper = new PCEnhancerHelperImpl(classLoader);
				for (IResource resource : resources) {
					subMonitor.subTask("OpenJPA Enhancement... (Full Build, enhancing " + resource.getName() + ")");
					checkCancel(subMonitor);
					if (enhance(resource, enhancerHelper, opts)) {
						++actuallyEnhanced;
					}
					subMonitor.worked(1);
				}
				
				duration = System.currentTimeMillis() - startTime;
				if (opts.isVerboseLoggingEnabled)
					LogUtil.logOK("OpenJPA Enhancement (Full Build) took " + duration + "ms, for " + resources.size()
							+ " potential classes, of which " + actuallyEnhanced + " were actually enhanced");
			} finally {
				// LogUtil.logInfo("OpenJPA Enhancement (Full Build) will now cause a full project refresh");
				getProject().refreshLocal(IResource.DEPTH_INFINITE, subMonitor.newChild(1));
				// LogUtil.logOK("OpenJPA Enhancement (Full Build) completely done (after refresh), good bye.");
				monitor.done();
			}
		}
	}

	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor, BuilderOptions opts) 
	    throws CoreException {
		monitor.subTask("OpenJPA Enhancement... (Incremental Build)");
		try {
			PCEnhancerHelper enhancerHelper = getEnhancer(getProject());
			delta.accept(new MyIncrementalBuildResourceDeltaVisitor(monitor, enhancerHelper, opts));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Check if a resource needs enhancement.
	 * Needs to be very fast, should NOT e.g. actually analyze any class files (do that later only).
	 * @param opts 
	 */
	private boolean needsEnhancement(IResource resource, BuilderOptions opts) throws CoreException {
		if (resource instanceof IFile && resource.getName().endsWith(".class")) {
			IFile iFile = (IFile) resource;
			String fileNameForLog = iFile.getFullPath().toString();
			deleteMarkers(iFile);
			
			if (!opts.pathMatcher.match(iFile.getLocation().toString())) {
				if (opts.isVerboseLoggingEnabled) {
					LogUtil.logInfo("OpenJPA Enhancer skipped class because it did not match pattern " 
					    + fileNameForLog );
				}
				return false;
			}
			
			return true;
		} else {
			return false;
		}
	}
	
    /**
     * Gets the enhancer for the given user project. Creates if one does not exist for the given project.
     */
    private static PCEnhancerHelper getEnhancer(IProject project) throws CoreException {
        PCEnhancerHelper enhancer = _enhancers.get(project);
        if (enhancer == null) {
            Activator.log("Creating enhancer for project " + project.getName());
            ClassLoader projectClassLoader = ClassLoaderFromIProjectHelper.createClassLoader(project);
            if (Activator.isUsingOpenJPA(project)) {
                Activator.log("Project " + project.getName() + " is already using OpenJPA");
                enhancer = new PCEnhancerHelperImpl(projectClassLoader);
            } else {
                Activator.log("Project " + project.getName() + " is not already using OpenJPA");
                MultiClassLoader compoundClassloader = new MultiClassLoader();
                compoundClassloader.addClassLoader(projectClassLoader);
                compoundClassloader.addClassLoader(Activator.class.getClassLoader());
                enhancer = new PCEnhancerHelperImpl(projectClassLoader);
            }
        }
        return enhancer;
    }

	
	private boolean enhance(IResource resource, PCEnhancerHelper enhancerHelper, BuilderOptions opts) 
	    throws CoreException {
		IFile iFile = (IFile) resource;
		String fileNameForLog = iFile.getFullPath().toString();
		try {
			File file = iFile.getLocation().toFile();
			boolean hasEnhanced = enhancerHelper.enhance(file);
			if (opts.isVerboseLoggingEnabled) {
				if (hasEnhanced)
					LogUtil.logInfo("OpenJPA Enhancer ran on and actually bytecode enhanced " + fileNameForLog);
				else
					LogUtil.logInfo("OpenJPA Enhancer ran on but did not have to bytecode enhance " + fileNameForLog);
			}
			
			return hasEnhanced;
		} catch (Throwable e) {
			String msg = "OpenJPA Enhancement Builder failed with message '" + e.toString() + "' for class: " 
			    + iFile.getLocation();
			addMarkerAndThrowNewCoreException(iFile, msg, e);
			return false;
		}
	}
	
    /**
     * Callback notification on Java Model change determines if the user project's classpath has been changed.
     * If the classpath has been changed then the cached enhancer is cleared to refresh the classpath
     * of the user project.
     */
    public void elementChanged(ElementChangedEvent event) {
        IResourceDelta[] rsrcs = event.getDelta().getResourceDeltas();
        for (int i = 0; rsrcs != null && i < rsrcs.length; i++) {
            if (isClasspath(rsrcs[i])) {
                IProject project = rsrcs[i].getResource().getProject();
                _enhancers.remove(project);
            }
        }
    }
    
    /**
     * Affirms if the given resource represents a classpath.
     */
    private boolean isClasspath(IResourceDelta resource) {
        IPackageFragmentRoot path = (IPackageFragmentRoot)resource.getAdapter(IPackageFragmentRoot.class);
        return path != null;
    }


	/**
	 * Note that if full/verbose logging is enabled, which writes to that Error
	 * Log view, then something in Eclipse (3.4 at least) goes wrong with the
	 * Progress view and the Monitor stuff - it doesn't update correctly, and
	 * keeps running!
	 */
	private boolean isFullLoggingEnabled(Map<String, String> args) {
		if (args.containsKey("debugLogs")) {
			return "true".equalsIgnoreCase((String) args.get("debugLogs"));
		}
		return false;
	}

	private void addMarkerAndThrowNewCoreException(IFile iFile, String msg, Throwable e) throws CoreException {
		addMarker(iFile, msg, 0, IMarker.SEVERITY_ERROR);
		logAndThrowNewCoreException(msg, e);
	}

	private void logAndThrowNewCoreException(String msg, Throwable e) throws CoreException {
		LogUtil.logError(msg, e);
		IStatus status;
		if (e != null) {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e);
		} else {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg);
		}
		throw new CoreException(status);
	}

	private void addMarker(IFile file, String message, int lineNumber, int severity) {
		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
		} catch (CoreException e) {
		}
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	private void checkCancel(IProgressMonitor monitor) {
		// @see http://www.eclipse.org/articles/Article-Builders/builders.html
		if (monitor.isCanceled()) {
			// No monitor.done(); in example 
			forgetLastBuiltState(); // not sure if this is really necessary for us, but probably doesn't hurt? 
			throw new OperationCanceledException();
		}
	}
}
