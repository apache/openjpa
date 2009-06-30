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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
 * Annotation processing tool generates souce code for a meta-model class given 
 * the annotated source code of persistent entity.
 * <p>
 * This tool is invoked during compilation for JDK6 compiler if OpenJPA and JPA 
 * libraries are specified in the compiler <code>-processorpath</code> option.
 * <br>
 * Supported options
 * <LI>
 * For example,<br>
 * <center><code>$ javac -processorpath path/to/openjpa;/path/to/jpa 
 * -s src -Alog mypackage/MyClass.java</code></center>
 * <p> 
 * will generate source code for canonical meta-model class  at
 * <code>src/mypackage/MyClass_.java</code>.
 * <p>
 * The generated source code is written relative to the source path root which
 * is, by default, the current directory or as specified by -s option to 
 * <code>javac</code> compiler. 
 * <p>
 * Currently the only recognized option is <code>-Alog</code> specified as shown
 * in the <code>javac</code> command above.
 * 
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
    private MetaDataFactory factory;
    private int generatedSourceVersion = 6;
    private CompileTimeLogger logger;
    private boolean addHeader = false;
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
        addHeader = "true".equalsIgnoreCase(processingEnv.getOptions().get("header"));
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
            PrintWriter writer = createSourceFile(metaClass, e);
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
        } catch (IOException e1) {
            throw new RuntimeException(e1);
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
        if (addHeader) {
           source.addComment(false, _loc.get("mmg-asl-header").getMessage());
        } else {
            source.addComment(false, _loc.get("mmg-tool-sign").getMessage());
        }
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
    
    private void setFileManager() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        fileManager = compiler.getStandardFileManager(null, 
            null, null);
        String srcOutput = processingEnv.getOptions().get("out");
        if (srcOutput != null) {
            try {
                fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, 
                    Collections.singletonList(new File(srcOutput)));
            } catch (Throwable t) {
                logger.warn(_loc.get("mmg-bad-out", srcOutput, 
                    StandardLocation.SOURCE_OUTPUT));
            }
        }
    }

    private PrintWriter createSourceFile(String metaClass, TypeElement e) 
        throws IOException {
        
        JavaFileObject javaFile = fileManager.getJavaFileForOutput(
            StandardLocation.SOURCE_OUTPUT, 
            metaClass, JavaFileObject.Kind.SOURCE, null);
        logger.info(_loc.get("mmg-process", javaFile.toUri()));
        OutputStream out = javaFile.openOutputStream();
        PrintWriter writer = new PrintWriter(out);
        return writer;
    }
}
