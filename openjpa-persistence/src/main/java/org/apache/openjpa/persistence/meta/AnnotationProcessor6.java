package org.apache.openjpa.persistence.meta;

import static javax.lang.model.SourceVersion.RELEASE_6;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.persistence.metamodel.TypesafeMetamodel;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.persistence.util.SourceCode;

/**
 * Annotation processing tool generates souce code for a meta-model class given 
 * the annotated source code of persistent entity.
 * <p>
 * This tool is invoked during compilation for JDK6 compiler if OpenJPA and JPA 
 * libraries are specified in the compiler <code>-processorpath</code> option.
 * <br>
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
@SupportedOptions( { "log" })
@SupportedSourceVersion(RELEASE_6)

public class AnnotationProcessor6 extends AbstractProcessor {
    private SourceAnnotationHandler handler;

    private static Localizer _loc =
        Localizer.forPackage(AnnotationProcessor6.class);
    private static final String UNDERSCORE = "_";

    /**
     * Category of members as per JPA 2.0 type system.
     * 
     */
    private static enum TypeCategory {
        ATTRIBUTE("javax.persistence.metamodel.Attribute"), 
        COLLECTION("javax.persistence.metamodel.Collection"), 
        SET("javax.persistence.metamodel.Set"), 
        LIST("javax.persistence.metamodel.List"), 
        MAP("javax.persistence.metamodel.Map");

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
    private TypeCategory toMetaModelTypeCategory(String name) {
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
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, 
            _loc.get("mmg-tool-banner").getMessage());
        handler = new SourceAnnotationHandler(processingEnv);
    }
    
    /**
     * The entry point for javac compiler.
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
        String metaClass = originalClass + UNDERSCORE;

        SourceCode source = new SourceCode(metaClass);
        comment(source);
        annotate(source, originalClass);
        TypeElement supCls = handler.getPersistentSupertype(e);
        if (supCls != null)
            source.getTopLevelClass().setSuper(supCls.toString() + UNDERSCORE);
        try {
            PrintWriter writer = createSourceFile(metaClass, e);
            SourceCode.Class modelClass = source.getTopLevelClass();
            Set<? extends Element> members = handler.getPersistentMembers(e);
            
            for (Element m : members) {
                TypeMirror decl = handler.getDeclaredType(m);
                String fieldName = handler.getPersistentMemberName(m);
                String fieldType = handler.getDeclaredTypeName(decl);
                TypeCategory typeCategory = toMetaModelTypeCategory(fieldType);
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
        cls.addAnnotation(TypesafeMetamodel.class.getName())
            .addArgument("value", originalClass + ".class", false);
        cls.addAnnotation(Generated.class.getName())
           .addArgument("value", this.getClass().getName())
           .addArgument("date", new Date().toString());
    }
    
    private void comment(SourceCode source) {
        source.addComment(false, _loc.get("mmg-tool-sign").getMessage());
    }
    
    private PrintWriter createSourceFile(String metaClass, TypeElement e) 
        throws IOException {
        Filer filer = processingEnv.getFiler();
        JavaFileObject javaFile = filer.createSourceFile(metaClass, e);
        log(_loc.get("mmg-process", metaClass, javaFile.toUri()).getMessage());
        OutputStream out = javaFile.openOutputStream();
        PrintWriter writer = new PrintWriter(out);
        return writer;
    }

    private void log(String msg) {
        if (!processingEnv.getOptions().containsKey("log"))
            return;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }
}
