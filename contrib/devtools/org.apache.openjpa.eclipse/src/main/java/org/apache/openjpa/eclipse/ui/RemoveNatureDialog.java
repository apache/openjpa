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
package org.apache.openjpa.eclipse.ui;

import org.apache.openjpa.eclipse.PluginProperty;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A dialog to confirm removing the nature from user project.
 * 
 * @author Pinaki Poddar
 *
 */
public class RemoveNatureDialog extends AbstractDialog {
    
    public RemoveNatureDialog(Shell parentShell, IProject project, String header, String title, String message) {
        super(parentShell, project, header, title, message);
    }
    
    /**
     * Creates the dialog's content area.
     * 
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);
        boolean warn = false;
        try {
            warn = "true".equalsIgnoreCase(project.getPersistentProperty(PluginProperty.USING_CAPTIVE_LIBS));
        } catch (CoreException e) {
        }
        String message = warn 
           ? "Disabling OpenJPA will remove runtime libraries added to " + project.getName() + ".\r\n" +
            "This project may not build after removing these libraries.\r\n" +
            "Are you sure you want to remove OpenJPA nature from " + project.getName() + "?"
           : "Remove OpenJPA nature from" + project.getName() + "?";     
        new Label(composite, SWT.NONE).setText(message);
        
        Label endBar = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
        endBar.setLayoutData(new GridData(GridData.GRAB_VERTICAL|GridData.FILL_HORIZONTAL));
        
        return composite;
    }
    
    /**
     * Test
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Display d = Display.getDefault();//PlatformUI.getWorkbench().getDisplay();
        Shell shell = new Shell(d);
        RemoveNatureDialog dialog = new RemoveNatureDialog(shell, null, "Test Header", "Test Title", "Test Message");
        dialog.open();
    }
    

}

