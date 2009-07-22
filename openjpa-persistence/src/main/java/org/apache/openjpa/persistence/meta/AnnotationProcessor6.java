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
package org.apache.openjpa.persistence.meta;

import static javax.lang.model.SourceVersion.RELEASE_6;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.persistence.metamodel.StaticMetamodel;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.meta.MetaDataFactory;
import org.apache.openjpa.persistence.PersistenceMetaDataFactory;
import org.apache.openjpa.persistence.util.SourceCode;

/**
 * Annotation processing tool generates source code for a meta-model class given 
 * the annotated source code of persistent entity.
 * <p>
 * This tool is invoked during compilation for JDK6 compiler if OpenJPA and JPA 
 * libraries are specified in the compiler <code>-processorpath</code> option.
 * <br>
 * <B>Usage</B><br>
 * <code>$ javac -processorpath path/to/openjpa-all.jar mypackage/MyEntity.java</code><br>
 * will generate source code for canonical meta-model class <code>mypackage.MyEntity_.java</code>.
 * <p>
 * The Annotation Processor recognizes the following options (none of them are mandatory):
 * <LI><code>-Alog=TRACE|INFO|WARN|ERROR</code><br>
 * The logging level. Default is <code>WARN</code>.
 * <LI>-Asource=&lt;n&gt;<br>
 * where &lt;n&gt; denotes the integral number for Java source version of the generated code. 
 * Default is <code>6</code>.
 * <LI>-Anaming=class name <br>
 * fully-qualified name of a class implementing <code>org.apache.openjpa.meta.MetaDataFactory</code> that determines
 * the name of a meta-class given the name of the original persistent Java entity class. Defaults to
 * <code>org.apache.openjpa.persistence.PersistenceMetaDataFactory</code> which appends a underscore character
 * (<code>_</code>) to the original Java class name. 
 * <LI>-Aheader=&lt;url&gt;<br>
 * A url whose content will appear as comment header to the generated file(s). Recognizes special value
 * <code>ASL</code> for Apache Source License header as comment. By default adds a OpenJPA proprietary   
 * text.
 * <LI>-Aout=dir<br>
 * A directory in the local file system. The generated files will be written <em>relative</em> to this directory
 * according to the package structure i.e. if <code>dir</code> is specified as <code>/myproject/generated-src</code>
 * then the generated source code will be written to <code>/myproject/generated-src/mypackage/MyEntity_.java</code>.
 * If this option is not specified, then an attempt will be made to write the generated source file in the same
 * directory of the source code of original class <code>mypackage.MyEntity</code>. The source code location for 
 * <code>mypackage.MyEntity</code> can only be determined for Sun JDK6 and <code>tools.jar</code> being available 
 * to the compiler classpath. If the source code location for the original class can not be determined, and the 
 * option is not specified, then the generated source code is written relative to the current directory according 
 * to the package structure.  
 * <br>
 * @author Pinaki Poddar
 * 
 * @since 2.0.0
 * 
 */
@SupportedAnnotationTypes({ 
    "javax.persistence.Entity",
    "javax.persistence.Embeddable", 
    "javax.persistence.MappedSuperclass" })
@SupportedOptions( { "log", "out", "source", "naming", "header" })
@SupportedSourceVersion(RELEASE_6)

public class AnnotationProcessor6 extends AbstractProcessor {
    private SourceAnnotationHandler handler;
    private StandardJavaFileManager fileManager;
    private boolean isUserSpecifiedOutputLocation = false;
    private MetaDataFactory factory;
    private int generatedSourceVersion = 6;
    private CompileTimeLogger logger;
    private String header;
    private static Localizer _loc =  Localizer.forPackage(AnnotationProcessor6.class);

    /**
     * Category of members as per JPA 2.0 type system.
     * 
     */
    private static enum TypeCategory {
        ATTRIBUTE("javax.persistence.metamodel.SingularAttribute"), 
        COLLECTION("javax.persistence.metamodel.CollectionAttribute"), 
        SET("javax.persistence.metamodel.SetAttribute"), 
        LIST("javax.persistence.metamodel.ListAttribute"), 
        MAP("javax.persistence.metamodel.MapAttribute");

        private String type;

        private TypeCategory(String type) {
            this.type = type;
        }

        public String getMetaModelType() {
            return type;
        }
    }
    
    /**
     * Enumerates available java.util.* collection classes to categorize them
     * into corresponding JPA meta-model member type.
     */
    private static List<String> CLASSNAMES_LIST = Arrays.asList(
        new String[]{
        "java.util.List", "java.util.AbstractList", 
        "java.util.AbstractSequentialList", "java.util.ArrayList", 
        "java.util.Stack", "java.util.Vector"});
    private static List<String> CLASSNAMES_SET = Arrays.asList(
        new String[]{
        "java.util.Set", "java.util.AbstractSet", "java.util.EnumSet", 
        "java.util.HashSet", "java.util.LinkedList", "java.util.LinkedHashSet", 
        "java.util.SortedSet", "java.util.TreeSet"});
    private static List<String> CLASSNAMES_MAP = Arrays.asList(
        new String[]{
        "java.util.Map", "java.util.AbstractMap", "java.util.EnumMap", 
        "java.util.HashMap",  "java.util.Hashtable", 
        "java.util.IdentityHashMap",  "java.util.LinkedHashMap", 
        "java.util.Properties", "java.util.SortedMap", 
        "java.util.TreeMap"});
    private static List<String> CLASSNAMES_COLLECTION = Arrays.asList(
        new String[]{
        "java.util.Collection", "java.util.AbstractCollection", 
        "java.util.AbstractQueue", "java.util.Queue", 
        "java.util.PriorityQueue"});
    
    /**
     * Gets the fully-qualified name of member class in JPA 2.0 type system,
     * given the fully-qualified name of a Java class.
     *  
     */
    private TypeCategory toMetaModelTypeCategory(TypeMirror mirror, 
        String name) {
        if (mirror.getKind() == TypeKind.ARRAY)
            return TypeCategory.LIST;
        if (CLASSNAMES_COLLECTION.contains(name))
            return TypeCategory.COLLECTION;
        if (CLASSNAMES_LIST.contains(name))
            return TypeCategory.LIST;
        if (CLASSNAMES_SET.contains(name))
            return TypeCategory.SET;
        if (CLASSNAMES_MAP.contains(name))
            return TypeCategory.MAP;
        return TypeCategory.ATTRIBUTE;
    }
    
    /**
     * Initialization.
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        logger = new CompileTimeLogger(processingEnv);
        logger.info(_loc.get("mmg-tool-banner"));
        setSourceVersion();
        setFileManager();
        setNamingPolicy();
        setHeader();
        handler = new SourceAnnotationHandler(processingEnv, logger);
    }
    
    /**
     * The entry point for java compiler.
     */
    @Override
    public boolean process(Set<? extends TypeElement> annos,
        RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            Set<? extends Element> elements = roundEnv.getRootElements();
            for (Element e : elements) {
                process((TypeElement) e);
            }
        }
        return true;
    }

    /**
     * Generate meta-model source code for the given type.
     * 
     * @return true if code is generated for the given element. false otherwise.
     */
    private boolean process(TypeElement e) {
    	if (!handler.isAnnotatedAsEntity(e)) {
            return false;
        }

        Elements eUtils = processingEnv.getElementUtils();
        String originalClass = eUtils.getBinaryName((TypeElement) e).toString();
        String originalSimpleClass = e.getSimpleName().toString();
        String metaClass = factory.getMetaModelClassName(originalClass);

        SourceCode source = new SourceCode(metaClass);
        comment(source);
        annotate(source, originalClass);
        TypeElement supCls = handler.getPersistentSupertype(e);
        if (supCls != null) {
            String superName = factory.getMetaModelClassName(
                    supCls.toString());
            source.getTopLevelClass().setSuper(superName);
        }
        try {
            PrintWriter writer = createSourceFile(originalClass, metaClass, e);
            SourceCode.Class modelClass = source.getTopLevelClass();
            Set<? extends Element> members = handler.getPersistentMembers(e);
            
            for (Element m : members) {
                TypeMirror decl = handler.getDeclaredType(m);
                decl.getKind();
                String fieldName = handler.getPersistentMemberName(m);
                String fieldType = handler.getDeclaredTypeName(decl);
                TypeCategory typeCategory = toMetaModelTypeCategory(decl, 
                        fieldType);
                String metaModelType = typeCategory.getMetaModelType();
                SourceCode.Field modelField = null;
                switch (typeCategory) {
                case ATTRIBUTE:
                    modelField = modelClass.addField(fieldName, metaModelType);
                    modelField.addParameter(originalSimpleClass)
                              .addParameter(fieldType);
                    break;
                case COLLECTION:
                case LIST:
                case SET:
                    TypeMirror param = handler.getTypeParameter(decl, 0);
                    String elementType = handler.getDeclaredTypeName(param);
                    modelField = modelClass.addField(fieldName, metaModelType);
                    modelField.addParameter(originalSimpleClass)
                              .addParameter(elementType);
                    break;
                case MAP:
                    TypeMirror key = handler.getTypeParameter(decl, 0);
                    TypeMirror value = handler.getTypeParameter(decl, 1);
                    String keyType = handler.getDeclaredTypeName(key);
                    String valueType = handler.getDeclaredTypeName(value);
                    modelField = modelClass.addField(fieldName, metaModelType);
                    modelField.addParameter(originalSimpleClass)
                              .addParameter(keyType)
                              .addParameter(valueType);
                    break;
                }
                modelField.makePublic().makeStatic().makeVolatile();
            }
            source.write(writer);
            writer.flush();
            writer.close();
            return true;
        } catch (Exception e1) {
            logger.error(_loc.get("mmg-process-error", e.getQualifiedName()), e1);
            return false;
        } finally {

        }
    }
    
    private void annotate(SourceCode source, String originalClass) {
        SourceCode.Class cls = source.getTopLevelClass();
        cls.addAnnotation(StaticMetamodel.class.getName())
            .addArgument("value", originalClass + ".class", false);
        if (generatedSourceVersion >= 6) {
            cls.addAnnotation(Generated.class.getName())
            .addArgument("value", this.getClass().getName())
            .addArgument("date", new Date().toString());
        }
    }
    
    private void comment(SourceCode source) {
        if (header != null)
            source.addComment(false, header);
        String defaultHeader = _loc.get("mmg-tool-sign").getMessage();
        source.addComment(false, defaultHeader);
    }
    
    /**
     * Parse annotation processor option <code>-Asource=n</code> to detect
     * the source version for the generated classes. 
     * n must be a integer. Default or wrong specification returns 6.
     */
    private void setSourceVersion() {
        String version = processingEnv.getOptions().get("source");
        if (version != null) {
            try {
                generatedSourceVersion = Integer.parseInt(version);
            } catch (NumberFormatException e) {
                logger.warn(_loc.get("mmg-bad-source", version, 6));
                generatedSourceVersion = 6;
            }
        } else {
            generatedSourceVersion = 6;
        }
    }
    
    private void setNamingPolicy() {
        String policy = processingEnv.getOptions().get("naming");
        if (policy != null) {
            try {
                factory = (MetaDataFactory)Class.forName(policy).newInstance();
            } catch (Throwable e) {
                logger.warn(_loc.get("mmg-bad-naming", policy, e));
                factory = new PersistenceMetaDataFactory();
            }
        } else {
            factory = new PersistenceMetaDataFactory();
        }
    }
    
    private void setHeader() {
        String headerOption = processingEnv.getOptions().get("header");
        if (headerOption == null) {
            return;
        }
        if ("ASL".equalsIgnoreCase(headerOption)) {
            header = _loc.get("mmg-asl-header").getMessage();
        } else {
            try {
                URL url = new URL(headerOption);
                header = url.getContent().toString();
            } catch (Throwable t) {
                
            }
        }
    }
    
    private void setFileManager() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        fileManager = compiler.getStandardFileManager(null, null, null);
        String outDir = processingEnv.getOptions().get("out");
        if (outDir != null)
           isUserSpecifiedOutputLocation = setSourceOutputDirectory(new File(outDir));
    }

    /**
     * Creates a file where source code of the given metaClass will be written.
     * 
     */
    private PrintWriter createSourceFile(String originalClass, String metaClass, TypeElement e) 
        throws IOException {
        if (!isUserSpecifiedOutputLocation) {
            setSourceOutputDirectory(OutputPath.getAbsoluteDirectory(processingEnv, e));
        }
        JavaFileObject javaFile = fileManager.getJavaFileForOutput(StandardLocation.SOURCE_OUTPUT, 
            metaClass, JavaFileObject.Kind.SOURCE, 
            null); // do not use sibling hint because of indeterminable behavior across JDK 
        logger.info(_loc.get("mmg-process", javaFile.toUri()));
        OutputStream out = javaFile.openOutputStream();
        PrintWriter writer = new PrintWriter(out);
        return writer;
    }
    
    /**
     * Sets the output directory for generated source files.
     * Tries to create the directory structure if does not exist.
     * 
     * @return true if the output has been set successfully.
     */
    boolean setSourceOutputDirectory(File outDir) {
        if (outDir == null)
            return false;
        if (!outDir.exists()) {
            if (!outDir.mkdirs()) {
                logger.warn(_loc.get("mmg-bad-out", outDir, StandardLocation.SOURCE_OUTPUT));
                return false;
            }
        }
        try {
            fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, Collections.singleton(outDir));
            return true;
        } catch (IOException e) {
            logger.warn(_loc.get("mmg-bad-out", outDir, StandardLocation.SOURCE_OUTPUT));
            return false;
        }
    }
    
    /**
     * An utility class to determine the source file corresponding to a {@link TypeElement}.
     * The utility uses Sun JDK internal API (com.sun.tools.*) and hence works reflectively
     * to avoid compile-time dependency.
     *   
     * @author Pinaki Poddar
     *
     */
    public static class OutputPath {
        private static Class<?> trees = null;
        static {
            try {
                trees = Class.forName("com.sun.source.util.Trees");
            } catch (Throwable t) {
                
            }
        }
        
        /**
         * Gets the directory relative to the Java source file corresponding to the TypeElement.
         * 
         * @return null if the com.sun.source.util.* package is not available or the given TypeElement
         * does not correspond to a compilation unit associated to a source file.
         */
        public static File getAbsoluteDirectory(ProcessingEnvironment env, TypeElement e) {
            if (trees == null)
                return null;
            try {
                // Trees root = Trees.instance(env);
                Object root = trees.getMethod("instance", new Class[]{ProcessingEnvironment.class})
                    .invoke(null, env);
                
                // TreePath path = root.getPath(e);
                Object path = root.getClass().getMethod("getPath", new Class[]{Element.class})
                    .invoke(root, e);
                
                // CompilationUnitTree unit = path.getCompilationUnit();
                Object unit = path.getClass().getMethod("getCompilationUnit", (Class[])null)
                    .invoke(path, (Object[])null);
                
                // JavaFileObject f = unit.getSourceFile();
                JavaFileObject f = (JavaFileObject)unit.getClass().getMethod("getSourceFile", (Class[])null)
                    .invoke(unit, (Object[])null);
                
                URI uri = f.toUri();
                File dir = getParentFile(new File(uri.toURL().getPath()), 
                        packageDepth(e.getQualifiedName().toString()));
                return dir;
            } catch (Throwable t) {
                return null;
            }
        }
        
        /**
         * Gets the parent of the given file recursively traversing to given number of levels.
         */
        public static File getParentFile(File f, int n) {
            if (n < 0)
                return f;
            if (n == 0)
                return f.getParentFile();
            return getParentFile(f.getParentFile(), n-1);
        }
        
        public static int packageDepth(String s) {
            String pkg = getPackageName(s);
            if (pkg == null)
                return 0;
            int depth = 1;
            int i = 0;
            while ((i = pkg.indexOf('.')) != -1) {
                depth++;
                pkg = pkg.substring(i+1);
            }
            return depth;
        }
        
        public static String getPackageName(String s) {
            if (s == null)
                return null;
            int i = s.lastIndexOf('.');
            return (i == -1) ? null : s.substring(0, i);
        }
        
        public static String getSimpleName(String s) {
            if (s == null)
                return null;
            int i = s.lastIndexOf('.');
            return (i == -1) ? s : s.substring(i+1);
        }
    }

}
