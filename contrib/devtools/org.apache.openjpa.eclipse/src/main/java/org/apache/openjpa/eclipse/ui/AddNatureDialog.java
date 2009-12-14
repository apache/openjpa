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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.openjpa.eclipse.PluginProperty;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A dialog to inform that bundle runtime libraries will be added to the classpath of
 * a project.
 * 
 * @author Pinaki Poddar
 *
 */
public class AddNatureDialog extends AbstractDialog {
    
    private final List<String> requiredLibs;
    private Button addLibrary;
    
    public static void main(String[] args) throws Exception {
        Display d = Display.getDefault();//PlatformUI.getWorkbench().getDisplay();
        Shell shell = new Shell(d);
        AddNatureDialog dialog = new AddNatureDialog(shell, null, "Test Header", "Test Title", "Test Message", 
                new ArrayList<String>());
        dialog.open();
    }
    
    public AddNatureDialog(Shell parentShell, IProject project, String header, String title, String message,
            List<String> libariesToBeAdded) {
        super(parentShell, project, header, title, message);
        if (libariesToBeAdded == null) {
            requiredLibs =  Collections.emptyList();
        } else {
            requiredLibs = libariesToBeAdded;
        }
        try {
            project.setPersistentProperty(PluginProperty.REQUIRES_CAPTIVE_LIBS, ""+!requiredLibs.isEmpty());
        } catch (CoreException e) {
        }
    }
    
    /**
     * Creates the dialog's content area.
     * 
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite)super.createDialogArea(parent);
      
        boolean requiresCaptiveLibs = !requiredLibs.isEmpty();
        String message = requiresCaptiveLibs 
            ? "Following libraries are missing from the project's classpath. \r\n" +
              "The plugin's captive version of these libraries will be added to the project's classpath.\r\n" +
              "If you want to add the libraries manually later, please uncheck the box."
            : "Required libraries are already available to the project's classpath";
        
        addLibrary = createCheckBox(composite, message, PluginProperty.REQUIRES_CAPTIVE_LIBS);
        addLibrary.setSelection(requiresCaptiveLibs);
        addLibrary.setEnabled(requiresCaptiveLibs);
        if (requiresCaptiveLibs) {
            org.eclipse.swt.widgets.List libList = new 
                org.eclipse.swt.widgets.List(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
            for (String lib : requiredLibs)
                libList.add(lib);
            libList.setEnabled(false);
        }
        final Group enhanceOptions = new Group(composite, SWT.NONE);
        enhanceOptions.setText("Bytecode Enhancement Options");
        GridLayout layout = new GridLayout();
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        layout.marginTop = 10;
        layout.marginBottom = 10;
        enhanceOptions.setLayout(layout);
        enhanceOptions.setLayoutData(gridData);
        
        Button overwrite = createCheckBox(enhanceOptions, "Overwrite *.class files");
        overwrite.setSelection(true); 
        overwrite.setGrayed(true); 
        overwrite.setEnabled(false);
        
        Button output = createCheckBox(enhanceOptions, "Write enhanced classes", null);
        output.setSelection(true); 
        output.setGrayed(true); 
        output.setEnabled(false);
        
        createCheckBox(enhanceOptions, "Add no-arg constructor to persistent entity", PluginProperty.ADD_CONSTRUCTOR);
        createCheckBox(enhanceOptions, "Enforce Property Restriction", PluginProperty.ENFORCE_PROP);
        
        new Label(parent, SWT.NONE); // empty space
        Label endBar = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
        endBar.setLayoutData(new GridData(GridData.GRAB_VERTICAL|GridData.FILL_HORIZONTAL));

        return composite;
    }
    
    public boolean getAddLibrary() {
        return addLibrary.getSelection();
    }
    
    Button createCheckBox(Composite parent, String text) {
        return createCheckBox(parent, text, null);
    }
    
    Button createCheckBox(Composite parent, String text, QualifiedName prop) {
        Button b = new Button(parent, SWT.CHECK);
        b.setText(text);
        GridData gridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
        b.setLayoutData(gridData);
        if (prop != null) {
            b.addSelectionListener(new BooleanPropertyRegister(b, prop));
            try {
                boolean selected = "true".equals(project.getPersistentProperty(prop));
                b.setSelection(selected);
            } catch (CoreException ex) {
                
            }
        }
        return b;
    }
    
    /**
     * Tracks the given boolean property of a project by selection state of the given button. 
     * 
     * @author Pinaki Poddar
     *
     */
    private class BooleanPropertyRegister implements SelectionListener {
        private Button button;
        private QualifiedName property;
        
        /**
         * Sets the state of the given button according to the boolean value of the given property.
         * @param b the button to attach to the given property.
         * @param p the property to track
         */
        public BooleanPropertyRegister(Button b, QualifiedName p) {
            button = b;
            property = p;
            if (property != null) {
            }
        }

        public void widgetDefaultSelected(SelectionEvent e) {
        }

        public void widgetSelected(SelectionEvent e) {
            if (property != null) {
                try {
                    project.setPersistentProperty(property, ""+button.getSelection());
                } catch (CoreException ex) {
                    
                }
            }
        }
    }
}

