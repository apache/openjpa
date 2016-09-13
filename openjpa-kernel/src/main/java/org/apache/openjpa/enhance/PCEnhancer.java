/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.enhance;

import java.io.File;
import java.io.IOException;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.BytecodeWriter;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Options;


/**
 * Takes an entity class and changes the bytecode to be able to process OpenJPA handling.
 *
 * Use {@link org.apache.openjpa.conf.OpenJPAConfiguration#getPCEnhancerInstance()} to access it.
 */
public abstract class PCEnhancer {

    private static final Localizer _loc = Localizer.forPackage(PCEnhancer.class);

    public static final int ENHANCE_NONE = 0;
    public static final int ENHANCE_AWARE = 2 << 0;
    public static final int ENHANCE_INTERFACE = 2 << 1;
    public static final int ENHANCE_PC = 2 << 2;

    /**
     * Usage: java org.apache.openjpa.enhance.PCEnhancer [option]*
     * &lt;class name | .java file | .class file | .jdo file&gt;+
     *  Where the following options are recognized.
     * <ul>
     * <li><i>-properties/-p &lt;properties file&gt;</i>: The path to a OpenJPA
     * properties file containing information as outlined in
     * {@link org.apache.openjpa.lib.conf.Configuration}; optional.</li>
     * <li><i>-&lt;property name&gt; &lt;property value&gt;</i>: All bean
     * properties of the standard OpenJPA {@link OpenJPAConfiguration} can be
     * set by using their names and supplying a value; for example:
     * <li><i>-directory/-d &lt;build directory&gt;</i>: The path to the base
     * directory where enhanced classes are stored. By default, the
     * enhancer overwrites the original .class file with the enhanced
     * version. Use this option to store the generated .class file in
     * another directory. The package structure will be created beneath
     * the given directory.</li>
     * <li><i>-addDefaultConstructor/-adc [true/t | false/f]</i>: Whether to
     * add a default constructor to persistent classes missing one, as
     * opposed to throwing an exception. Defaults to true.</li>
     * <li><i>-tmpClassLoader/-tcl [true/t | false/f]</i>: Whether to
     * load the pre-enhanced classes using a temporary class loader.
     * Defaults to true. Set this to false when attempting to debug
     * class loading errors.</li>
     * <li><i>-enforcePropertyRestrictions/-epr [true/t | false/f]</i>:
     * Whether to throw an exception if a PROPERTY access entity appears
     * to be violating standard property restrictions. Defaults to false.</li>
     * </ul>
     *  Each additional argument can be either the full class name of the
     * type to enhance, the path to the .java file for the type, the path to
     * the .class file for the type, or the path to a .jdo file listing one
     * or more types to enhance.
     * If the type being enhanced has metadata, it will be enhanced as a
     * persistence capable class. If not, it will be considered a persistence
     * aware class, and all access to fields of persistence capable classes
     * will be replaced by the appropriate	get/set method. If the type
     * explicitly declares the persistence-capable interface, it will
     * not be enhanced. Thus, it is safe to invoke the enhancer on classes
     * that are already enhanced.
     */
    public static void main(String[] args) {
        Options opts = new Options();
        args = opts.setFromCmdLine(args);
        if (!run(args, opts, null)) {
            // START - ALLOW PRINT STATEMENTS
            System.err.println(_loc.get("enhance-usage"));
            // STOP - ALLOW PRINT STATEMENTS
        }
    }

    /**
     * Run the tool. Returns false if invalid options given. Runs against all
     * the persistence units defined in the resource to parse.
     * TODO: Keep as IMPORTANT
     *
     * @param args the fully qualified class file locations of the entities and other args like jdo files
     * @param opts invocation options
     * @param cl an optional ClassLoader which should be used to load the entities or {@code null} to auto-select one
     */
    public static boolean run(final String[] args, Options opts, final ClassLoader cl) {
        return Configurations.runAgainstAllAnchors(opts,
                   new Configurations.Runnable() {
                       public boolean run(Options opts) throws IOException {
                           OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
                           Flags flags = new Flags();
                           flags.directory = Files.getFile(opts.removeProperty("directory", "d",
                                                                               null), null);
                           flags.addDefaultConstructor = opts.removeBooleanProperty
                                   ("addDefaultConstructor", "adc", flags.addDefaultConstructor);
                           flags.tmpClassLoader = opts.removeBooleanProperty
                                   ("tmpClassLoader", "tcl", flags.tmpClassLoader);
                           flags.enforcePropertyRestrictions = opts.removeBooleanProperty
                                   ("enforcePropertyRestrictions", "epr",
                                    flags.enforcePropertyRestrictions);

                           Configurations.populateConfiguration(conf, opts);

                           // for unit testing
                           flags.byteCodeWriter = (BytecodeWriter) opts.get(PCEnhancer.class.getName() + "#bytecodeWriter");

                           try {
                               return conf.getPCEnhancerInstance().enhanceFiles(args, flags, cl);
                           } finally {
                               conf.close();
                           }
                       }
                   });
    }

    /**
     * @return the version of the current enhancer.
     */
    public abstract int getEnhancerVersion();


    /**
     * This method detects and logs any Entities that may have been enhanced at build time by
     * a version of the enhancer that is older than the current version.
     *
     * @param cls A non-null Class implementing org.apache.openjpa.enhance.PersistenceCapable.
     * @return {@code true} if the provided Class is down level from the current {@link #getEnhancerVersion()}. {@code false} otherwise.
     * @throws IllegalStateException if cls doesn't implement org.apache.openjpa.enhance.PersistenceCapable.
     */
    public abstract boolean checkEnhancementLevel(Class<?> cls);

    /**
     * Enhance all the given files
     *
     * @param files the entity class files, java class names or .jdo file names to enhance
     * @param flags additional switches to define the generated bytecode
     * @param cl an optional ClassLoader which should be used to load the entities or {@code null} to auto-select one
     * @return {@code true} if all went fine, {@code false} if there was some error.
     */
    public abstract boolean enhanceFiles(String[] files, Flags flags, ClassLoader cl) throws IOException;

    /**
     * Whether or not <code>className</code> is the name for a
     * dynamically-created persistence-capable subclass.
     *
     * @see #toManagedTypeName(String)
     */
    public abstract boolean isPCSubclassName(String className);

    /**
     * If <code>className</code> is a dynamically-created persistence-capable
     * subclass name, returns the name of the class that it subclasses.
     * Otherwise, returns <code>className</code>.
     *
     * @see #isPCSubclassName(String)
     */
    public abstract String toManagedTypeName(String className);

    /**
     * Run flags.
     */
    public static class Flags {

        public File directory = null;
        public boolean addDefaultConstructor = true;
        public boolean tmpClassLoader = true;
        public boolean enforcePropertyRestrictions = false;
        public BytecodeWriter byteCodeWriter;
    }
}
