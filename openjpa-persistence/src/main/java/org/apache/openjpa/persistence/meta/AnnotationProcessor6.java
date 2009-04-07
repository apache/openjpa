package org.apache.openjpa.persistence.meta;

import static javax.lang.model.SourceVersion.RELEASE_6;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Transient;
import javax.persistence.metamodel.TypesafeMetamodel;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.persistence.util.SourceCode;
import org.apache.openjpa.util.UserException;

/**
 * Annotation processing tool to generate/instantiate a meta-model given the
 * annotated source code of persistent domain model.
 * <p>
 * This tool is invoked as part of compilation phase for JDK6 compiler provided
 * OpenJPA and JPA class libraries are specified in the compiler 
 * <code>-processorpath</code> option. <br>
 * For example<br>
 * <code>$ javac -processorpath path/to/openjpa.jar;/path/to/jpa.jar 
 * src/mypackage/MyClass.java</code>
 * <br> 
 * will generate source code for canonical meta-model for 
 * <code>mypackage.MyClass</code> (if it is annotated with persistence 
 * annotation <code>Entity or Embedded or MappedSuperclass</code>) to produce a 
 * file <code>mypackage/MyClass_.java</code>.
 * <p>
 * The generated source code is written relative to the source path root which
 * is, by default, the current directory or as specified by -s option to 
 * <code>javac</code> compiler. 
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
    private static final String UNDERSCORE = "_";


    /**
     * Set of Inclusion Filters based on member type, access type or transient
     * annotations. Used to determine the subset of available field/method that 
     * are persistent.   
     */
    static InclusiveFilter propertyAccessFilter =
        new AccessFilter(AccessType.PROPERTY);
    static InclusiveFilter fieldAccessFilter = 
        new AccessFilter(AccessType.FIELD);
    
    static InclusiveFilter fieldFilter = new KindFilter(ElementKind.FIELD);
    static InclusiveFilter methodFilter = new KindFilter(ElementKind.METHOD);
    
    static InclusiveFilter getterFilter = new GetterFilter();
    static InclusiveFilter setterFilter = new SetterFilter();
    
    static InclusiveFilter nonTransientFilter = new NonTransientMemberFilter();

    private static Localizer _loc =
        Localizer.forPackage(AnnotationProcessor6.class);

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
        System.err.println(processingEnv.getOptions());
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
        if (!isAnnotatedAsEntity(e)) {
            return false;
        }

        Elements eUtils = processingEnv.getElementUtils();
        String originalClass = eUtils.getBinaryName((TypeElement) e).toString();
        String originalSimpleClass = e.getSimpleName().toString();
        String metaClass = originalClass + UNDERSCORE;

        log(_loc.get("mmg-process", originalClass).getMessage());

        SourceCode source = new SourceCode(metaClass);
        comment(source);
        annotate(source, originalClass);
        TypeElement supCls = getPCSuperclass(e);
        if (supCls != null)
            source.getTopLevelClass().setSuper(supCls.toString() + UNDERSCORE);
        try {
            PrintWriter writer = createSourceFile(metaClass, e);
            SourceCode.Class modelClass = source.getTopLevelClass();
            List<? extends Element> members = getPersistentMembers(e);
            
            for (Element m : members) {
                DeclaredType decl = getDeclaredType(m);
                String fieldName = m.getSimpleName().toString();
                String fieldType = getDeclaredTypeName(decl);
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
                    TypeMirror param = getTypeParameter(decl, 0);
                    String elementType = getDeclaredTypeName(param);
                    modelField = modelClass.addField(fieldName, metaModelType);
                    modelField.addParameter(originalSimpleClass)
                              .addParameter(elementType);
                    break;
                case MAP:
                    TypeMirror key = getTypeParameter(decl, 0);
                    TypeMirror value = getTypeParameter(decl, 1);
                    String keyType = getDeclaredTypeName(key);
                    String valueType = getDeclaredTypeName(value);
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
        OutputStream out = javaFile.openOutputStream();
        PrintWriter writer = new PrintWriter(out);
        return writer;
    }
    
    /**
     * Get  access type of the given class, if specified explicitly. 
     * null otherwise.
     * 
     * @param type
     * @return FIELD or PROPERTY 
     */
    AccessType getExplicitAccessType(TypeElement type) {
        Object access = getAnnotationValue(type, Access.class);
        if (equalsByValue(AccessType.FIELD, access))
            return AccessType.FIELD;
        if (equalsByValue(AccessType.PROPERTY, access))
            return AccessType.PROPERTY;
        return null;
    }
    
    /**
     * Gets the list of persistent fields and/or methods for the given type.
     * 
     * Scans relevant @AccessType annotation and field/method as per JPA
     * specification to determine the candidate set of field/methods.
     */
    private List<Element> getPersistentMembers(TypeElement type) {
        AccessType access = getExplicitAccessType(type);
        boolean isExplicit = access != null;
        
        List<Element> result = new ArrayList<Element>();
        List<? extends Element> allMembers = type.getEnclosedElements();
        Set<VariableElement> allFields =
            (Set<VariableElement>) filter(allMembers, fieldFilter,
                nonTransientFilter);
        Set<ExecutableElement> allMethods =
            (Set<ExecutableElement>) filter(allMembers, methodFilter,
                nonTransientFilter);
        Set<VariableElement> propertyAnnotatedFields =
            filter(allFields, propertyAccessFilter);

        Set<ExecutableElement> fieldAnnotatedMethods =
            filter(allMethods, fieldAccessFilter);

        if (!propertyAnnotatedFields.isEmpty())
            throw new UserException(_loc.get("access-prop-on-field", type,
                toString(propertyAnnotatedFields)));
        if (!fieldAnnotatedMethods.isEmpty())
            throw new UserException(_loc.get("access-field-on-prop", type,
                toString(fieldAnnotatedMethods)));

        Set<ExecutableElement> getters = filter(allMethods, getterFilter);
        Set<ExecutableElement> setters = filter(allMethods, setterFilter);
        getters = matchGetterAndSetter(getters, setters);
        
        boolean isFieldAccess = isExplicit 
            ? AccessType.FIELD.equals(access) : !allFields.isEmpty();
        boolean isPropertyAccess = isExplicit 
            ? AccessType.PROPERTY.equals(access) : !getters.isEmpty();

        if (isExplicit) {
            if (isFieldAccess) {
                result.addAll(allFields);
                result.addAll(filter(getters, propertyAccessFilter));
            } else {
                result.addAll(getters);
                result.addAll(filter(allFields, fieldAccessFilter));
            }
        } else { // implicit access type
            if (isFieldAccess && isPropertyAccess)
                throw new UserException(_loc.get("access-mixed", type,
                    toString(allFields), toString(getters)));
            if (isFieldAccess) {
                result.addAll(allFields);
            } else if (isPropertyAccess) {
                result.addAll(getters);
            } else {
                warn(_loc.get("access-none", type).toString());
            }
        }
        return result;
    }

    // =========================================================================
    // Annotation processing utilities
    // =========================================================================
    
    /**
     * Affirms if the given element is annotated with any of the given 
     * annotations.
     * 
     * @param annos null checks for any annotation that starts with 
     *            'javax.persistence.' or 'openjpa.*'.
     * 
     */
    private boolean isAnnotatedWith(Element e, Set<String> annos) {
        if (e == null)
            return false;
        List<? extends AnnotationMirror> mirrors = e.getAnnotationMirrors();
        if (annos == null) {
            for (AnnotationMirror mirror : mirrors) {
                String name = mirror.getAnnotationType().toString();
                if (startsWith(name, "javax.persistence.")
                 || startsWith(name, "openjpa."))
                    return true;
            }
            return false;
        } else {
            for (AnnotationMirror mirror : mirrors) {
                String name = mirror.getAnnotationType().toString();
                if (annos.contains(name))
                    return true;
            }
            return false;
        }
    }
    
    /**
     * Affirms if the given list contains one of the registered annotation that
     * designates an entity type.
     */
    private boolean isAnnotatedAsEntity(TypeElement e) {
        return isAnnotatedWith(e, getSupportedAnnotationTypes());
    }
    
     /**
     * Get the element name of the class the given mirror represents. If the
     * mirror is primitive then returns the corresponding boxed class name.
     * If the mirror is parameterized returns only the generic type i.e.
     * if the given declared type is 
     * <code>java.util.Set&lt;java.lang.String&gt;</code> this method will 
     * return <code>java.util.Set</code>.
     */
    private String getDeclaredTypeName(TypeMirror mirror) {
        return processingEnv.getTypeUtils().asElement(box(mirror)).toString();
    }

    /**
     * Gets the declared type of the given member. For fields, returns the 
     * declared type while for method returns the return type. 
     * 
     * @param e a field or method.
     * @exception if given member is neither a field nor a method.
     */
    private DeclaredType getDeclaredType(Element e) {
        TypeMirror result = null;
        switch (e.getKind()) {
        case FIELD:
            result = e.asType();
            break;
        case METHOD:
            result = ((ExecutableElement) e).getReturnType();
            break;
        default:
            throw new IllegalArgumentException(toDetails(e));
        }
        result = box(result);
        if (result.getKind() == TypeKind.DECLARED)
            return (DeclaredType)result;
        throw new IllegalArgumentException(toDetails(e));
    }
    
    /**
     * Affirms if the given type mirrors a primitive.
     */
    private boolean isPrimitive(TypeMirror mirror) {
        TypeKind kind = mirror.getKind();
        return kind == TypeKind.BOOLEAN 
            || kind == TypeKind.BYTE
            || kind == TypeKind.CHAR
            || kind == TypeKind.DOUBLE
            || kind == TypeKind.FLOAT
            || kind == TypeKind.INT
            || kind == TypeKind.LONG
            || kind == TypeKind.SHORT;
    }
    
    TypeMirror box(TypeMirror t) {
        if (isPrimitive(t))
            return processingEnv.getTypeUtils()
            .boxedClass((PrimitiveType)t).asType();
        return t;
    }

    /**
     * Gets the parameter type argument at the given index of the given type.
     * 
     * @return if the given type represents a parameterized type, then the
     *         indexed parameter type argument. Otherwise null.
     */
    private TypeMirror getTypeParameter(DeclaredType mirror, int index) {
        List<? extends TypeMirror> params = mirror.getTypeArguments();
        return (params == null || params.size() < index+1) 
            ? null : params.get(index);
    }

    /**
     * Gets the nearest super class of the given class which is persistent.
     * 
     * @return null if no such super class exists.
     */
    private TypeElement getPCSuperclass(TypeElement cls) {
        TypeMirror sup = cls.getSuperclass();
        if (sup == null || isRootObject(sup))
            return null;
        TypeElement supe =
            (TypeElement) processingEnv.getTypeUtils().asElement(sup);
        if (isAnnotatedAsEntity(supe)) 
            return supe;
        return getPCSuperclass(supe);
    }


    /**
     * Affirms if the given declaration has the given annotation.
     */
    private static boolean hasAnnotation(Element e,
        Class<? extends Annotation> anno) {
        return e != null && e.getAnnotation(anno) != null;
    }
    
    /**
     * Gets the value of the given annotation, if present, in the given
     * declaration. Otherwise, null.
     */
    private static Object getAnnotationValue(Element decl,
        Class<? extends Annotation> anno) {
        return getAnnotationValue(decl, anno, "value");
    }

    /**
     * Gets the value of the given attribute of the given annotation, if
     * present, in the given declaration. Otherwise, null.
     */
    private static Object getAnnotationValue(Element e,
        Class<? extends Annotation> anno, String attr) {
        if (e == null || e.getAnnotation(anno) == null)
            return null;
        List<? extends AnnotationMirror> annos = e.getAnnotationMirrors();
        for (AnnotationMirror mirror : annos) {
            if (mirror.getAnnotationType().toString().equals(anno.getName())) {
                Map<? extends ExecutableElement, ? extends AnnotationValue> 
                values = mirror.getElementValues();
                for (ExecutableElement ex : values.keySet()) {
                    if (ex.getSimpleName().equals(attr))
                        return values.get(ex).getValue();
                }
            }
        }
        return null;
    }

    /**
     * Matches the given getters with the given setters. Removes the getters
     * that do not have a corresponding setter.
     */
    private Set<ExecutableElement> matchGetterAndSetter(
        Set<ExecutableElement> getters,  Set<ExecutableElement> setters) {
        Collection<ExecutableElement> unmatched =
            new ArrayList<ExecutableElement>();
        for (ExecutableElement getter : getters) {
            String name = getter.getSimpleName().toString();
            TypeMirror returns = getter.getReturnType();
            String setterName = "set" + name.substring("get".length());
            boolean matched = false;
            for (ExecutableElement setter : setters) {
                TypeMirror argType =
                    setter.getParameters().iterator().next().asType();
                matched =
                    setter.getSimpleName().equals(setterName)
                        && argType.equals(returns);
                if (matched)
                    break;
            }
            if (!matched && isBoolean(returns)) {
                setterName = "set" + name.substring("is".length());
                for (ExecutableElement setter : setters) {
                    TypeMirror argType =
                        setter.getParameters().iterator().next().asType();
                    matched =
                        setter.getSimpleName().equals(setterName)
                            && isBoolean(argType);
                    if (matched)
                        break;
                }
            }
            if (!matched) {
                warn(_loc.get("getter-unmatched", getter, 
                    getter.getEnclosingElement()).toString());
                unmatched.add(getter);
            }

        }
        getters.removeAll(unmatched);
        return getters;
    }

    // ========================================================================
    //  Selection Filters select specific elements from a collection.
    // ========================================================================
    
    /**
     * Inclusive element filtering predicate.
     *
     */
    private static interface InclusiveFilter {
        /**
         * Return true to include the given element.
         */
        boolean includes(Element e);
    }

    /**
     * Filter the given collection with the conjunction of filters. The given
     * collection itself is not modified.
     */
    private <T extends Element> Set<T> filter(Collection<T> coll, 
        InclusiveFilter... filters) {
        Set<T> result = new HashSet<T>();
        for (T e : coll) {
            boolean include = true;
            for (InclusiveFilter f : filters) {
                if (!f.includes(e)) {
                    include = false;
                    break;
                }
            }
            if (include)
                result.add(e);
        }
        return result;
    }

    /**
     * Selects getter method. A getter method name starts with 'get', returns a
     * non-void type and has no argument. Or starts with 'is', returns a boolean
     * and has no argument.
     * 
     */
    static class GetterFilter implements InclusiveFilter {
        public boolean includes(Element obj) {
            if (!isMethod(obj))
                return false;

            ExecutableElement m = (ExecutableElement) obj;
            String name = m.getSimpleName().toString();
            TypeMirror returnType = m.getReturnType();
            return m.getParameters().isEmpty()
                 && ((startsWith(name, "get") && !isVoid(returnType))
                 || (startsWith(name, "is") && isBoolean(returnType)));
        }
    }

    /**
     * Selects setter method. A setter method name starts with 'set', returns a
     * void and has single argument.
     * 
     */
    static class SetterFilter implements InclusiveFilter {
        public boolean includes(Element obj) {
            if (!isMethod(obj))
                return false;

            ExecutableElement m = (ExecutableElement) obj;
            String name = m.getSimpleName().toString();
            TypeMirror returnType = m.getReturnType();
            return startsWith(name, "set") && isVoid(returnType) 
                && m.getParameters().size() == 1;
        }
    }

    /**
     * Selects elements which is annotated with @Access annotation and that 
     * annotation has the given AccessType value.
     * 
     */
    static class AccessFilter implements InclusiveFilter {
        final AccessType target;

        public AccessFilter(AccessType target) {
            this.target = target;
        }

        public boolean includes(Element obj) {
            Object value = getAnnotationValue(obj, Access.class);
            return equalsByValue(target, value);
        }
    }

    /**
     * Selects elements of given kind.
     * 
     */
    static class KindFilter implements InclusiveFilter {
        final ElementKind target;

        public KindFilter(ElementKind target) {
            this.target = target;
        }

        public boolean includes(Element obj) {
            return obj.getKind() == target;
        }
    }

    /**
     * Selects all non-transient element.
     * 
     */
    static class NonTransientMemberFilter implements InclusiveFilter {
        public boolean includes(Element obj) {
            boolean isTransient = hasAnnotation(obj, Transient.class)
                            || obj.getModifiers().contains(Modifier.TRANSIENT);
            return !isTransient;
        }
    }
    
    
    // ========================================================================
    //  Utilities
    // ========================================================================

    /**
     * Affirms if the given mirror represents a primitive or non-primitive
     * boolean.
     */
    private static boolean isBoolean(TypeMirror type) {
        return (type != null && (type.getKind() == TypeKind.BOOLEAN 
            || "java.lang.Boolean".equals(type.toString())));
    }

    /**
     * Affirms if the given mirror represents a void.
     */
    private static boolean isVoid(TypeMirror type) {
        return (type != null && type.getKind() == TypeKind.VOID);
    }

    /**
     * Affirms if the given element represents a method.
     */
    private static boolean isMethod(Element e) {
        return e != null && ExecutableElement.class.isInstance(e)
            && ((Element) e).getKind() == ElementKind.METHOD;
    }

    /**
     * Affirms if the given mirror represents root java.lang.Object.
     */
    private static boolean isRootObject(TypeMirror type) {
        return type != null && "java.lang.Object".equals(type.toString());
    }
    
    /**
     * Affirms if the given full string starts with the given head.
     */
    private static boolean startsWith(String full, String head) {
        return full != null && full.startsWith(head) 
            && full.length() > head.length();
    }

    /**
     * Affirms if the given enum equals the given value.
     */
    private static boolean equalsByValue(Enum<?> e, Object v) {
        if (v == null)
            return false;
        return e.toString().equals(v.toString());
    }
    
    
    // =========================================================================
    // Logging
    // =========================================================================

    private void log(String msg) {
        if (!processingEnv.getOptions().containsKey("log"))
            return;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    private void warn(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg);
    }
    
    private static String toString(Collection<? extends Element> elements) {
        StringBuilder tmp = new StringBuilder();
        int i = 0;
        for (Element e : elements) {
            tmp.append(e.getSimpleName() + (++i == elements.size() ? "" : ","));
        }
        return tmp.toString();
    }
    
    String toDetails(Element e) {
        TypeMirror mirror = e.asType();
        return new StringBuffer(e.getKind().toString()).append(" ")
                           .append(e.toString())
                           .append("Mirror ")
                           .append(mirror.getKind().toString())
                           .append(mirror.toString()).toString();
    }
}
