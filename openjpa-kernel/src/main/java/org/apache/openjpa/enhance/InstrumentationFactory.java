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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.util.JavaVendors;
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
    private static boolean _attachAttempted = false;
    private static final String _name = InstrumentationFactory.class.getName();
    private static final Localizer _loc = Localizer.forPackage(
        InstrumentationFactory.class);
    private static final String ATTACH_MODULE = "jdk.attach";
    private static final String VIRTUAL_MACHINE_CLASS = "com.sun.tools.attach.VirtualMachine";
    private static final String ALLOW_ATTACH_SELF = "jdk.attach.allowAttachSelf";

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
    public static synchronized Instrumentation getInstrumentation(final Log log) {
        if (log.isTraceEnabled()) {
            log.trace(_name + ".getInstrumentation() _inst:" + _inst
                + " _dynamicallyInstall:" + _dynamicallyInstall);
        }
        if ( _inst != null || !_dynamicallyInstall)
            return _inst;

        // Dynamic agent enhancement should only occur when the OpenJPA library is
        // loaded using the system class loader.  Otherwise, the OpenJPA
        // library may get loaded by separate, disjunct loaders, leading to linkage issues.
        if (!InstrumentationFactory.class.getClassLoader().equals(ClassLoader.getSystemClassLoader())) {
        	return null;
        }
        // The attach is tried once. If it fails, the reason (no jdk.attach
        // module, self-attach not allowed) does not change for this JVM.
        if (_attachAttempted) {
            return null;
        }
        _attachAttempted = true;

        Class<?> vmClass = loadVMClass(log);
        if (vmClass == null) {
            return null;
        }
        if (!isAttachSelfAllowed()) {
            if (log.isInfoEnabled()) {
                log.info(_loc.get("dynamic-agent-no-self-attach", ALLOW_ATTACH_SELF));
            }
            return null;
        }
        String agentPath = getAgentJar(log);
        if (agentPath == null) {
            return null;
        }
        loadAgent(log, agentPath, vmClass);

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
            Files.createTempFile(InstrumentationFactory.class.getName(), ".jar").toFile();
        file.deleteOnExit();

        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(file));
        zout.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(zout));

        writer
            .println("Agent-Class: " + InstrumentationFactory.class.getName());
        writer.println("Can-Redefine-Classes: true");
        // IBM doesn't support retransform
        writer.println("Can-Retransform-Classes: " + (!JavaVendors.getCurrentVendor().isIBM()));

        writer.close();

        return file.getAbsolutePath();
    }

    /**
     * This private worker method will return a fully qualified path to a jar
     * that has this class defined as an Agent-Class in it's
     * META-INF/manifest.mf file. Under normal circumstances the path should
     * point to the OpenJPA jar. If running in a development environment a
     * temporary jar file will be created.
     *
     * @return absolute path to the agent jar or null if anything unexpected
     * happens.
     */
    private static String getAgentJar(Log log) {
        File agentJarFile = null;
        // Find the name of the File that this class was loaded from. That
        // jar *should* be the same location as our agent.
        CodeSource cs =
            InstrumentationFactory.class.getProtectionDomain().getCodeSource();
        if (cs != null) {
            URL loc = cs.getLocation();
            if(loc!=null){
                agentJarFile = new File(loc.getFile());
            }
        }

        // Determine whether the File that this class was loaded from has this
        // class defined as the Agent-Class.
        boolean createJar = false;
        if (cs == null || agentJarFile == null
            || agentJarFile.isDirectory()) {
            createJar = true;
        }else if(!validateAgentJarManifest(agentJarFile, log, _name)){
            // We have an agentJarFile, but this class isn't the Agent-Class.
            createJar=true;
        }

        String agentJar;
        if (createJar) {
            // This can happen when running in eclipse as an OpenJPA
            // developer or for some reason the CodeSource is null. We
            // should log a warning here because this will create a jar
            // in your temp directory that doesn't always get cleaned up.
            try {
                agentJar = createAgentJar();
                if (log.isInfoEnabled()) {
                    log.info(_loc.get("temp-file-creation", agentJar));
                }
            } catch (IOException ioe) {
                if (log.isTraceEnabled()) {
                    log.trace(_name + ".getAgentJar() caught unexpected "
                        + "exception.", ioe);
                }
                agentJar = null;
            }
        } else {
            agentJar = agentJarFile.getAbsolutePath();
        }

        return agentJar;
    }//end getAgentJar

    /**
     * Attach and load an agent class.
     *
     * @param log Log used if the agent cannot be loaded.
     * @param agentJar absolute path to the agent jar.
     * @param vmClass VirtualMachine class from the jdk.attach module.
     */
    private static void loadAgent(Log log, String agentJar, Class<?> vmClass) {
        try {
            String pid = String.valueOf(ProcessHandle.current().pid());
            // attach to the current VM so we can deploy a new agent
            Object vm = vmClass.getMethod("attach", String.class).invoke(null, pid);
            try {
                // now deploy the actual agent, which will wind up calling
                // agentmain()
                vmClass.getMethod("loadAgent", String.class).invoke(vm, agentJar);
            } finally {
                vmClass.getMethod("detach").invoke(vm);
            }
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            // Log the message from the exception, the stack is not of
            // interest.
            if (log.isInfoEnabled()) {
                log.info(_loc.get("dynamic-agent-attach-failed", t.toString()));
            }
        }
    }

    /**
     * Load the com.sun.tools.attach.VirtualMachine class from the jdk.attach
     * module. The module is not in the boot layer when the runtime image was
     * created without it, or when the application runs as a named module and
     * the JVM is started without --add-modules jdk.attach.
     *
     * @return The Attach API VirtualMachine class <br>
     *         or null if the jdk.attach module is not available.
     */
    static Class<?> loadVMClass(Log log) {
        if (ModuleLayer.boot().findModule(ATTACH_MODULE).isEmpty()) {
            if (log.isInfoEnabled()) {
                log.info(_loc.get("dynamic-agent-no-attach-module", ATTACH_MODULE));
            }
            return null;
        }
        try {
            return Class.forName(VIRTUAL_MACHINE_CLASS, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            if (log.isInfoEnabled()) {
                log.info(_loc.get("dynamic-agent-no-attach-module", ATTACH_MODULE));
            }
            return null;
        }
    }

    /**
     * The Attach API refuses to attach to the current VM unless the JVM was
     * started with -Djdk.attach.allowAttachSelf=true (or an empty value).
     *
     * @return true if the JVM allows attaching to itself.
     */
    static boolean isAttachSelfAllowed() {
        String allow = System.getProperty(ALLOW_ATTACH_SELF);
        return allow != null && (allow.isEmpty() || Boolean.parseBoolean(allow));
    }

    /**
     * This private worker method will validate that the provided agentClassName
     * is defined as the Agent-Class in the manifest file from the provided jar.
     *
     * @param agentJarFile
     *            non-null agent jar file.
     * @param log
     *            non-null logger.
     * @param agentClassName
     *            the non-null agent class name.
     * @return True if the provided agentClassName is defined as the Agent-Class
     *         in the manifest from the provided agentJarFile. False otherwise.
     */
    private static boolean validateAgentJarManifest(File agentJarFile, Log log, String agentClassName) {
        try (JarFile jar = new JarFile(agentJarFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest == null) {
                return false;
            }
            Attributes attributes = manifest.getMainAttributes();
            String ac = attributes.getValue("Agent-Class");
            if (ac != null && ac.equals(agentClassName)) {
                return true;
            }
        } catch (Exception e) {
            if (log.isTraceEnabled()) {
                log.trace(_name
                    + ".validateAgentJarManifest() caught unexpected "
                    + "exception " + e.getMessage());
            }
        }
        return false;
    }// end validateAgentJarManifest
}
