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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.apache.openjpa.eclipse.ui.ProjectDecorator;
import org.apache.openjpa.eclipse.util.ClassLoaderFromIProjectHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 * 
 * @author Eclipse PDE Example Wizard! ;-)
 * @author Michael Vorburger (MVO)
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.apache.openjpa.eclipse";

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
    
    /**
     * Is the project has independently using OpenJPA classes? 
     */
    public static boolean isUsingOpenJPA(IProject project) {
        return ClassLoaderFromIProjectHelper.findClass("org.apache.openjpa.conf.OpenJPAVersion", project) != null;
    }
    
    public static Display getDisplay() {
        return PlatformUI.getWorkbench().getDisplay();
    }

    public static org.eclipse.swt.widgets.Shell getShell() {
        Shell parent = getDisplay().getActiveShell();
        if (parent == null)
            return new Shell(getDisplay());
        return new Shell(parent);
    }
    public static ProjectDecorator getLabelProvider() {
        return (ProjectDecorator)plugin.getWorkbench().getDecoratorManager()
                   .getBaseLabelProvider(ProjectDecorator.DECORATOR_ID);
    }

    
    public static void log(String s) {
        System.err.println(s);
        Activator.getDefault().getLog().log(new Status(Status.OK, Activator.PLUGIN_ID, s));
    }

    public static void log(Throwable t) {
        System.err.println(t.getMessage());
        t.printStackTrace();
        Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, t.getMessage(), t));
    }

}
