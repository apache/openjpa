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

import junit.framework.TestCase;

import org.apache.openjpa.util.WASTransformer;

/**
 * Test class for WASTransformer.
 *
 */
public class TestWASTransformer extends TestCase {

    /**
     * This test will verify that the WASManagedRuntime$WASSynchronization
     * class was properly modified by the maven build process (reference
     * the top level pom.xml).  This testcase will not execute properly
     * within Eclipse since the Eclipse target directory (probably) hasn't
     * been modified via the maven build.
     *
     * @throws ClassNotFoundException
     * @author Michael Dick
     */
    public void testInterfaceAdded()throws ClassNotFoundException {

        boolean caughtExpectedException = false;

        Class syncClass = null;

        try {
             syncClass = Class.forName(WASTransformer._class);
        } catch (NoClassDefFoundError e) {
            if (e.getMessage().contains(WASTransformer._interface)) {
                caughtExpectedException = true;
            }
        }
        assertNull(syncClass);
        assertTrue(caughtExpectedException);
    }
}
