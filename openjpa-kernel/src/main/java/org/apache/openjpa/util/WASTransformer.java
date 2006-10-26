/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.util;

import serp.bytecode.BCClass;
import serp.bytecode.Project;

/**
 * WASTransformer uses Serp to add WebSphere proprietary interface to
 * WASManagedRuntime$WASSynchronization. The interface is added at build time.
 * The WebSphere extensions classes must be found on the classpath whenever an
 * instance of WASManagedRuntime$WASSynchronization is instantiated.
 *
 * @author Michael Dick
 *
 */
public class WASTransformer {

    /**
     * Class that will be modified
     */
    public static final String _class =
        "org.apache.openjpa.ee.WASManagedRuntime$WASSynchronization";

    /**
     * Interface which will be added
     */
    public static final String _interface =
        "com.ibm.websphere.jtaextensions.SynchronizationCallback";

    public static void main(String[] args) {

        Project project = new Project();

        BCClass bcClass = project.loadClass(_class);

        bcClass.declareInterface(_interface);
        try {
            bcClass.write();
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
