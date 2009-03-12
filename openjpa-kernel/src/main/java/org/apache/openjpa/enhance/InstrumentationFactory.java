/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.enhance;

import java.io.*;
import java.lang.instrument.*;
import java.lang.management.*;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.*;
import org.apache.openjpa.lib.util.*;


/**
 * Factory for obtaining an {@link Instrumentation} instance.
 *
 * @author Marc Prud'hommeaux
 * @since 1.0.0
 */
public class InstrumentationFactory {
    private static Instrumentation _inst;
    private static boolean _dynamicallyInstall = true;

    public static void setInstrumentation(Instrumentation inst) {
        _inst = inst;
    }

    /**
     * Configures whether or not this instance should attempt to dynamically
     * install an agent in the VM. Defaults to <code>true</code>.
     */
    public static synchronized void setDynamicallyInstallAgent(boolean val) {
        _dynamicallyInstall = val;
    }

    public static synchronized Instrumentation getInstrumentation()
        throws IOException, NoSuchMethodException, IllegalAccessException,
        InvocationTargetException, ClassNotFoundException {
        if (_inst != null || !_dynamicallyInstall)
            return _inst;

        // dynamic loading of the agent is only available in JDK 1.6+
        if (JavaVersions.VERSION < 6)
            return null;

        String agentPath = getAgentJar();

        // first obtain the PID of the currently-running process
        // ### this relies on the undocumented convention of the RuntimeMXBean's
        // ### name starting with the PID, but there appears to be no other
        // ### way to obtain the current process' id, which we need for
        // ### the attach process
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String pid = runtime.getName();
        if (pid.indexOf("@") != -1)
            pid = pid.substring(0, pid.indexOf("@"));

        // JDK1.6: now attach to the current VM so we can deploy a new agent
        // ### this is a Sun JVM specific feature; other JVMs may offer
        // ### this feature, but in an implementation-dependent way
        Class vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
        Object vm = vmClass.getMethod("attach", new Class[] { String.class }).
            invoke(null, new String[] { pid });

        // now deploy the actual agent, which will wind up calling agentmain()
        vm.getClass().getMethod("loadAgent", new Class[] { String.class }).
            invoke(vm, new Object[] { agentPath });

        if (_inst != null)
            return _inst;

        return null;
    }

    /** 
     *  Create a new jar file for the sole purpose of specifying an
     *  Agent-Class to load into the JVM.
     */
    private static String getAgentJar() throws IOException {
        File file = File.createTempFile(
            InstrumentationFactory.class.getName(), ".jar");
        file.deleteOnExit();

        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(file));
        zout.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));

        PrintWriter writer = new PrintWriter
            (new OutputStreamWriter(zout));

        writer.println("Agent-Class: "
            + InstrumentationFactory.class.getName());
        writer.println("Can-Redefine-Classes: true");
        writer.println("Can-Retransform-Classes: true");

        writer.close();

        return file.getAbsolutePath();
    }

    /**
     *  The method that is called when a jar is added as an agent at runtime.
     *  All this method does is store the {@link Instrumentation} for
     *  later use.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        InstrumentationFactory.setInstrumentation(inst);
    }
}