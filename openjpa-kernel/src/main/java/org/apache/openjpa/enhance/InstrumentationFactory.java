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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.JavaVersions;
import org.apache.openjpa.lib.util.Localizer;


/**
 * Factory for obtaining an {@link Instrumentation} instance.
 *
 * @author Marc Prud'hommeaux
 * @since 1.0.0
 */
public class InstrumentationFactory {
    private static Instrumentation _inst;
    private static boolean _dynamicallyInstall = true;
    private static final String _name = InstrumentationFactory.class.getName();
    private static final Localizer _loc = Localizer.forPackage(
        InstrumentationFactory.class);
    
    /**
     * This method is not synchronized because when the agent is loaded from
     * getInstrumentation() that method will cause agentmain(..) to be called.
     * Synchronizing this method would cause a deadlock.
     * 
     * @param inst The instrumentation instance to be used by this factory.
     */
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

    /**
     * @param log OpenJPA log.
     * @return null if Instrumentation can not be obtained, or if any 
     * Exceptions are encountered.
     */
    public static synchronized Instrumentation 
        getInstrumentation(final Log log) {
        if (_inst != null || !_dynamicallyInstall)
            return _inst;

        // dynamic loading of the agent is only available in JDK 1.6+
        if (JavaVersions.VERSION < 6)
            return null;

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                // If we can't find the tools.jar, we can't load the agent.
                File toolsJar = findToolsJar(log);
                if (toolsJar == null) {
                    return null;
                }

                Class<?> vmClass = loadVMClass(toolsJar, log);
                if (vmClass == null) {
                    return null;
                }
                String agentPath = getAgentJar(log);
                if (agentPath == null) {
                    return null;
                }
                loadAgent(log, agentPath, vmClass);
                return null;
            }// end run()
        });
        // If the load(...) agent call was successful, this variable will no 
        // longer be null.
        return _inst;
    }//end getInstrumentation()

    /**
     *  The method that is called when a jar is added as an agent at runtime.
     *  All this method does is store the {@link Instrumentation} for
     *  later use.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        InstrumentationFactory.setInstrumentation(inst);
    }

    /**
     * Create a new jar file for the sole purpose of specifying an Agent-Class
     * to load into the JVM.
     * 
     * @return absolute path to the new jar file.
     */
    private static String createAgentJar() throws IOException {
        File file =
            File.createTempFile(InstrumentationFactory.class.getName(), ".jar");
        file.deleteOnExit();

        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(file));
        zout.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(zout));

        writer
            .println("Agent-Class: " + InstrumentationFactory.class.getName());
        writer.println("Can-Redefine-Classes: true");
        writer.println("Can-Retransform-Classes: true");

        writer.close();

        return file.getAbsolutePath();
    }

    /**
     * This private worker method attempts to find [java_home]/lib/tools.jar.
     * Note: The tools.jar is a part of the SDK, it is not present in the JRE.
     * 
     * @return If tools.jar can be found, a File representing tools.jar. <BR>
     *         If tools.jar cannot be found, null.
     */
    private static File findToolsJar(Log log) {
        String javaHome = System.getProperty("java.home");
        File javaHomeFile = new File(javaHome);

        // IBM JDK hack -- for some reason when running on the IBM JDK, the JVM
        // appends /jre to the java.home SystemProperty. Remove the addition to
        // be consistent with Sun. Note: Not sure if this is something dependent
        // on my machine. Not really that big of a deal since this isn't
        // supported on the IBM JDK at this point.
        File toolsJarFile =
            new File(javaHomeFile, "lib" + File.separator + "tools.jar");
        if (toolsJarFile.exists() == false) {
            // If tools jar file isn't found, we may be on an IBM JDK. If the
            // java.home property ends in /jre, try removing it to look for the
            // tools.jar.
            String absPath = javaHomeFile.getAbsolutePath();
            if (absPath.endsWith(File.separator + "jre") == true) {
                javaHomeFile = javaHomeFile.getParentFile();
                toolsJarFile =
                    new File(javaHomeFile, "lib" + File.separator +
                        "tools.jar");
            }
        }

        if (toolsJarFile.exists() == false) {
            String toolsJarPath = toolsJarFile.getAbsolutePath();
            if (log.isTraceEnabled() == true) {
                log.trace(_name + ".findToolsJar() -- couldn't find "
                    + toolsJarPath);
            }
            return null;
        }
        return toolsJarFile;
    }

    /**
     * This private worker method will return a fully qualified path to a jar
     * that has this class defined as an Agent-Class in it's
     * META-INF/manifest.mf file. Under normal circumstances the path should
     * point to the OpenJPA jar. If running in a development environment a
     * temporary jar file will be created.
     * 
     * @return absolute path to the agent jar.
     * @throws Exception
     *             if this method is unable to detect where this class was
     *             loaded from. It is unknown if this is actually possible.
     */
    private static String getAgentJar(Log log) {
        // Find the name of the jar that this class was loaded from. That
        // jar *should* be the same location as our agent.
        File agentJarFile =
            new File(InstrumentationFactory.class.getProtectionDomain()
                .getCodeSource().getLocation().getFile());
        // We're deadmeat if we can't find a file that this class
        // was loaded from. Just return if this file doesn't exist.
        // Note: I'm not sure if this can really happen.
        if (agentJarFile.exists() == false) {
            if (log.isTraceEnabled() == true) {
                log.trace(_name + ".getAgentJar() -- Couldn't find where this "
                    + "class was loaded from!");
            }
        }
        String agentJar;
        if (agentJarFile.isDirectory() == true) {
            // This will happen when running in eclipse as an OpenJPA
            // developer. No one else should ever go down this path. We
            // should log a warning here because this will create a jar
            // in your temp directory that doesn't always get cleaned up.
            try {
                agentJar = createAgentJar();
                if (log.isInfoEnabled() == true) {
                    log.info(_loc.get("temp-file-creation", agentJar));
                }
            } catch (IOException ioe) {
                if (log.isTraceEnabled() == true) {
                    log.trace(_name + ".getAgentJar() caught unexpected "
                        + "exception.", ioe);
                }
                agentJar = null;
            }
        } else {
            agentJar = agentJarFile.getAbsolutePath();
        }

        return agentJar;
    }

    /**
     * Attach and load an agent class. 
     * 
     * @param log Log used if the agent cannot be loaded.
     * @param agentJar absolute path to the agent jar.
     * @param vmClass VirtualMachine.class from tools.jar.
     */
    private static void loadAgent(Log log, String agentJar, Class<?> vmClass) {
        try {
            // first obtain the PID of the currently-running process
            // ### this relies on the undocumented convention of the
            // RuntimeMXBean's
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
            Object vm =
                vmClass.getMethod("attach", new Class<?>[] { String.class })
                    .invoke(null, new String[] { pid });

            // now deploy the actual agent, which will wind up calling
            // agentmain()
            vmClass.getMethod("loadAgent", new Class[] { String.class })
                .invoke(vm, new Object[] { agentJar });
            vmClass.getMethod("detach", new Class[] {}).invoke(vm,
                new Object[] {});
        } catch (Throwable t) {
            if (log.isTraceEnabled() == true) {
                // Log the message from the exception. Don't log the entire
                // stack as this is expected when running on a JDK that doesn't
                // support the Attach API.
                log.trace(_name + ".loadAgent() caught an exception. Message: "
                    + t.getMessage());
            }
        }
    }

    /**
     * This private method will create a new classloader and attempt to load the
     * com.sun.tools.attach.VirtualMachine class from the provided toolsJar
     * file.
     * 
     * @return com.sun.tools.attach.VirtualMachine class <br>
     *         or null if something unexpected happened.
     */
    private static Class<?> loadVMClass(File toolsJar, Log log) {
        try {
            URLClassLoader loader =
                new URLClassLoader(new URL[] { toolsJar.toURI().toURL() },
                    Thread.currentThread().getContextClassLoader());
            return loader.loadClass("com.sun.tools.attach.VirtualMachine");
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace(_name
                    + ".loadVMClass() failed to load the VirtualMachine class");
            }
        }
        return null;
    }
}
