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

import org.eclipse.core.runtime.QualifiedName;

/**
 * Enumerates persistent properties of the project.
 *  
 * @author Pinaki Poddar
 *
 */
public class PluginProperty {
    /**
     * Is the project using the plugin's captive version of OpenJPA runtime libraries? 
     * 
     * Allowed values: "true" or "false"
     */
    public static final QualifiedName USING_CAPTIVE_LIBS   = qname("openjpa.usingCaptiveLibs");
    
    /**
     * Does the project requires plugin's captive version of OpenJPA runtime libraries to be added? 
     * 
     * Allowed values: "true" or "false"
     */
    public static final QualifiedName REQUIRES_CAPTIVE_LIBS   = qname("openjpa.requiresCaptiveLibs");
    
    /**
     * Does enhancer add a no-argument constructor for a persistent entity?
     * 
     * Allowed values: "true" or "false"
     */
    public static final QualifiedName ADD_CONSTRUCTOR = qname("enhancer.addConstructor");
    /**
     * Does enhancer enforce property based access restrictions?
     * 
     * Allowed values: "true" or "false"
     */
    public static final QualifiedName ENFORCE_PROP = qname("enhancer.enforceProperty");
    
    /**
     * The output directory for enhanced classes.
     * 
     * Allowed values: a directory
     */
    public static final QualifiedName ENHANCER_OUTPUT  = qname("enhancer.output.dir");
    
    private static QualifiedName qname(String s) {
        return new QualifiedName(Activator.PLUGIN_ID, s);
    }
    
}