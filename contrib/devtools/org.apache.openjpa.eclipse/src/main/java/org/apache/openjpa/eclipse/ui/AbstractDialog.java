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

import org.apache.openjpa.eclipse.Activator;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractDialog extends TitleAreaDialog {
    protected final IProject project;
    protected final String header;
    protected final String title;
    protected final String message;
    
    public static Image logo;
    static {
        try {
            logo = Activator.getImageDescriptor("icons/openjpa-logo-small.png").createImage();
        } catch (Exception e) {
        }
    }
    
    public AbstractDialog(Shell parentShell, IProject project, String header, String title, String message) {
        super(parentShell);
        this.project = project;
        this.header = header;
        this.title = title;
        this.message = message;
        this.setBlockOnOpen(true);
    }
    
    /**
     * Creates the dialog's contents
     * 
     * @param parent the parent composite
     * @return Control
     */
    protected Control createContents(Composite parent) {
      Control contents = super.createContents(parent);

      this.setTitle(title);
      this.setMessage(message);
      this.setTitleImage(logo);
      getShell().setText(header);

      return contents;
    }
    
    /**
     * Creates the dialog's content area
     * 
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 10;
        layout.horizontalSpacing = 10;
        layout.marginLeft = 10;
        layout.marginRight = 10;
        composite.setLayout(layout);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(gridData);
        composite.setFont(parent.getFont());
        // Build the separator line
        Label titleBarSeparator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
        titleBarSeparator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        return composite;
    }
    
    /**
     * Creates the buttons for the button bar.
     * 
     * @param parent the parent composite
     */
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

}
