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
import org.apache.openjpa.eclipse.OpenJPANature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

/**
 * Decorates the project root node with an image if OpenJPA nature is enabled for the project.
 * 
 * @author Pinaki Poddar
 *
 */
public class ProjectDecorator extends LabelProvider implements ILightweightLabelDecorator, ILabelProviderListener {
    public static final String DECORATOR_ID   = "org.apache.openjpa.eclipse.Decorator";
    public static final ImageDescriptor decor = Activator.getImageDescriptor("icons/apache-feather-small.jpg");
    
    public ProjectDecorator() {
        addListener(this);
    }
    
    /** 
     * Decorate the project root if it has the OpenJPA nature.
     */
    public void decorate(Object element, IDecoration decoration) {
        if (!(element instanceof IProject)) {
            return;
        }
        try {
            if (((IProject)element).hasNature(OpenJPANature.NATURE_ID)) {
                decoration.addOverlay(decor);
            } else {
                decoration.addOverlay(null);
            }
        } catch (CoreException e) {
        }
    }


    public void dispose() {
        removeListener(this);
    }

    /**
     * Returns whether the label will be affected by the change in the given property of the given element.
     * Always returns false.
     */
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    public void fireLabelProviderChanged(LabelProviderChangedEvent e) {
        super.fireLabelProviderChanged(e);
    }
    
    public void labelProviderChanged(LabelProviderChangedEvent event) {
    }
}
