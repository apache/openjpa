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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.openjpa.eclipse.ui.AddNatureDialog;
import org.apache.openjpa.eclipse.ui.ProjectDecorator;
import org.apache.openjpa.eclipse.ui.RemoveNatureDialog;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Action to Add/Remove OpenJPA Project Nature.
 * 
 * @author Eclipse PDE Example Wizard! ;-)
 * @author Michael Vorburger (MVO)
 * @author Pinaki Poddar
*/
public class ToggleNatureAction implements IObjectActionDelegate {

	private ISelection selection;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@SuppressWarnings("unchecked")
	public void run(IAction action) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				}
				if (project != null) {
					toggleNature(project);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * Toggles sample nature on a project
	 * 
	 * @param project
	 *            to have sample nature added or removed
	 */
    /**
     * Toggles the nature of the given project.
     * 
     */
    private void toggleNature(IProject project) {
        try {
            int natureIndex = getNatureIndex(project, OpenJPANature.NATURE_ID);
            if (natureIndex != -1) {
                removeNature(project, natureIndex);
            } else {
                addNature(project, OpenJPANature.NATURE_ID);
            }
        } catch (Exception e) {
            Activator.log(e);
        } finally {
        }
    }
	
	/**
	 * Adds given nature to the project.
	 * Adding a nature also involves finding out which require runtime libraries, if any, are missing
	 * from the given project and then copying those libraries from the bundle to the project.
	 * @param project
	 * @param natureId
	 * @return
	 * @throws CoreException
	 */
    private boolean addNature(IProject project, String natureId) throws CoreException {
        Activator.log("Adding nature " + natureId + " to project " + project.getName());
        PluginLibrary bundle = new PluginLibrary();
        List<String> missingLibraries = bundle.findMissingLibrary(project);
        Shell shell = Activator.getShell();
        AddNatureDialog dialog = new AddNatureDialog(shell, project,
            "Enable OpenJPA",
            "OpenJPA Plugin",
            "Enhances bytecode of persistent entities as you compile",
            missingLibraries);
        dialog.open();
        if (dialog.getReturnCode() != Window.OK) {
            return false;
        }
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = OpenJPANature.NATURE_ID;
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
        
        if ("true".equals(project.getPersistentProperty(PluginProperty.REQUIRES_CAPTIVE_LIBS))) {
            IClasspathEntry[] librariesToAdd = bundle.getLibraryClasspaths(project, missingLibraries, true);
            addClasspath(project, librariesToAdd);
        } else if (!missingLibraries.isEmpty()) {
            MessageDialog.openWarning(Activator.getShell(), "Missing Libraries", 
               "This project does not have the required runtime libraries. You must add them manually");
        }
        fireLabelEvent(project);
        Activator.log("Adding nature " + natureId + " to project " + project.getName() + " done...");
        return true;
    }
    
    /**
     * Add the captive runtime libraries of the bundle to the classpath of the given project.
     */
    private void addClasspath(IProject project, IClasspathEntry[] libs) throws CoreException {
        if (libs.length == 0)
            return;
        IJavaProject javaProject = JavaCore.create(project);
        IClasspathEntry[] projectClasspaths = javaProject.getRawClasspath();

        IClasspathEntry[] newClasspaths = new IClasspathEntry[projectClasspaths.length + libs.length];
        System.arraycopy(libs, 0, newClasspaths, 0, libs.length);
        System.arraycopy(projectClasspaths, 0, newClasspaths, libs.length, projectClasspaths.length);
        javaProject.setRawClasspath(newClasspaths, null);
        
        project.setPersistentProperty(PluginProperty.USING_CAPTIVE_LIBS, ""+true);
    }

    /**
     * Removes the nature from the project. Removes captive OpenJPA libraries from the project's classpath,
     * if it has been added.
     */
    private boolean removeNature(IProject project, int natureIndex) throws CoreException {
        Shell shell = Activator.getShell();
        RemoveNatureDialog dialog = new RemoveNatureDialog(shell, project,
             "Disable OpenJPA",
             "OpenJPA Plugin",
             "Enhances bytecode of persistent entities as you compile");
        dialog.open();
        if (dialog.getReturnCode() != Window.OK) {
            return false;
        }
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        Activator.log(this + ".removeNature(" + OpenJPANature.NATURE_ID + ")");
        String[] newNatures = new String[natures.length - 1];
        System.arraycopy(natures, 0, newNatures, 0, natureIndex);
        System.arraycopy(natures, natureIndex + 1, newNatures, natureIndex, natures.length - natureIndex - 1);
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
        
        removeClasspath(project);
        fireLabelEvent(project);
        
        Activator.log(this + ".removeNature()...done");
        return true;
    }
    
    /**
     * Gets the index of the given nature in the given project.
     * @param project
     * @param natureId
     * @return -1 if the nature is not present.
     * @throws CoreException
     */
    private int getNatureIndex(IProject project, String natureId) throws CoreException {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        for (int i = 0; i < natures.length; ++i) {
            if (OpenJPANature.NATURE_ID.equals(natures[i])) {
                return i;
            }
        }
        return -1;
    }

    // remove classpath entries
    private void removeClasspath(IProject project) throws CoreException {
        if ("false".equalsIgnoreCase(project.getPersistentProperty(PluginProperty.USING_CAPTIVE_LIBS))) {
            return;
        }
        IJavaProject javaProject = JavaCore.create(project);
        IClasspathEntry[] projectClasspaths = javaProject.getRawClasspath();

        PluginLibrary cpc = new PluginLibrary();
        IClasspathEntry[] cpsOpenJPA = cpc.getLibraryClasspaths(project, null, false);
        List<IClasspathEntry> cpsModified = new ArrayList<IClasspathEntry>();
        cpsModified.addAll(Arrays.asList(projectClasspaths));
        cpsModified.removeAll(Arrays.asList(cpsOpenJPA));
        javaProject.setRawClasspath(cpsModified.toArray(new IClasspathEntry[cpsModified.size()]), null);
        
        project.setPersistentProperty(PluginProperty.USING_CAPTIVE_LIBS, ""+false);
    }

    boolean contains(IClasspathEntry[] list, IClasspathEntry key) {
        for (IClasspathEntry cp : list) {
            if (cp.equals(key))
                return true;
        }
        return false;
    }

    /**
     * Fire an event to redraw the label for the given project element.
     */
    private void fireLabelEvent(final IProject project) {
        Activator.getDisplay().asyncExec(new Runnable() {
            public void run() {
                ProjectDecorator labeler = Activator.getLabelProvider();
                if (labeler == null)
                    return;
                LabelProviderChangedEvent event = new LabelProviderChangedEvent(labeler, project);
                labeler.fireLabelProviderChanged(event);
            }
        });
    }
}
