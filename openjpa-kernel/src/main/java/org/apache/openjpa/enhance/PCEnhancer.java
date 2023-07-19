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

import java.io.Externalizable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.meta.ClassArgParser;
import org.apache.openjpa.util.asm.BytecodeWriter;
import org.apache.openjpa.lib.util.ClassUtil;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.lib.util.J2DoPrivHelper;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Localizer.Message;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.lib.util.Services;
import org.apache.openjpa.lib.util.StringUtil;
import org.apache.openjpa.lib.util.git.GitUtils;
import org.apache.openjpa.meta.AccessCode;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataModes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.ValueStrategies;
import org.apache.openjpa.util.ApplicationIds;
import org.apache.openjpa.util.BigDecimalId;
import org.apache.openjpa.util.BigIntegerId;
import org.apache.openjpa.util.ByteId;
import org.apache.openjpa.util.CharId;
import org.apache.openjpa.util.DateId;
import org.apache.openjpa.util.DoubleId;
import org.apache.openjpa.util.FloatId;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.Id;
import org.apache.openjpa.util.ImplHelper;
import org.apache.openjpa.util.IntId;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.LongId;
import org.apache.openjpa.util.ObjectId;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.ShortId;
import org.apache.openjpa.util.StringId;
import org.apache.openjpa.util.UserException;
import org.apache.openjpa.util.asm.AsmHelper;
import org.apache.openjpa.util.asm.ClassNodeTracker;
import org.apache.openjpa.util.asm.EnhancementProject;
import org.apache.openjpa.util.asm.RedefinedAttribute;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.tree.*;


/**
 * Bytecode enhancer used to enhance persistent classes from metadata. The
 * enhancer must be invoked on all persistence-capable and persistence aware
 * classes.
 *
 * @author Abe White
 * @author Mark Struberg
 */
public class PCEnhancer {
    // Designates a version for maintaining compatbility when PCEnhancer
    // modifies enhancement that can break serialization or other contracts
    // Each enhanced class will return the value of this field via
    // public int getEnhancementContractVersion()
    public static final int ENHANCER_VERSION;
    public static final Type TYPE_OBJECT = Type.getType(Object.class);

    public static final int ENHANCE_NONE = 0;
    public static final int ENHANCE_AWARE = 2 << 0;
    public static final int ENHANCE_INTERFACE = 2 << 1;
    public static final int ENHANCE_PC = 2 << 2;

    public static final String PRE = "pc";
    public static final String ISDETACHEDSTATEDEFINITIVE = PRE + "isDetachedStateDefinitive";

    private static final Class<?> PCTYPE = PersistenceCapable.class;
    private static final Type TYPE_PCTYPE = Type.getType(PersistenceCapable.class);
    private static final String SM = PRE + "StateManager";
    private static final Class<?> SMTYPE = StateManager.class;
    private static final String INHERIT = PRE + "InheritedFieldCount";
    private static final String CONTEXTNAME = "GenericContext";
    private static final Class<?> USEREXCEP = UserException.class;
    private static final Class<?> INTERNEXCEP = InternalException.class;
    private static final Class<?> HELPERTYPE = PCRegistry.class;
    private static final String SUPER = PRE + "PCSuperclass";
    private static final Class OIDFSTYPE = FieldSupplier.class;
    private static final Class<?> OIDFCTYPE = FieldConsumer.class;

    private static final String VERSION_INIT_STR = PRE + "VersionInit";

    private static final Localizer _loc = Localizer.forPackage(PCEnhancer.class);

    private static final AuxiliaryEnhancer[] _auxEnhancers;

    static {
        Class[] classes = Services.getImplementorClasses(
                AuxiliaryEnhancer.class,
                AccessController.doPrivileged(
                        J2DoPrivHelper.getClassLoaderAction(AuxiliaryEnhancer.class)));
        List auxEnhancers = new ArrayList(classes.length);
        for (Class aClass : classes) {
            try {
                auxEnhancers.add(AccessController.doPrivileged(
                        J2DoPrivHelper.newInstanceAction(aClass)));
            }
            catch (Throwable t) {
                // aux enhancer may rely on non-existant spec classes, etc
            }
        }
        _auxEnhancers = (AuxiliaryEnhancer[]) auxEnhancers.toArray
                (new AuxiliaryEnhancer[0]);

        int rev = 0;
        Properties revisionProps = new Properties();
        try {
            InputStream in = PCEnhancer.class.getResourceAsStream("/META-INF/org.apache.openjpa.revision.properties");
            if (in != null) {
                try (in) {
                    revisionProps.load(in);
                }
            }
            rev = GitUtils.convertGitInfoToPCEnhancerVersion(revisionProps.getProperty("openjpa.enhancer.revision"));
        }
        catch (Exception e) {
        }
        if (rev > 0) {
            ENHANCER_VERSION = rev;
        }
        else {
            // Something bad happened and we couldn't load from the properties file. We need to default to using the
            // value of 2 because that is the value that was the value as of rev.511998.
            ENHANCER_VERSION = 2;
        }
    }

    private final MetaDataRepository _repos;
    private final ClassMetaData _meta;
    private final Log _log;

    boolean _addVersionInitFlag = true;


    private final EnhancementProject project;

    /**
     * represents the managed type.
     */
    private final ClassNodeTracker managedType;

    /**
     * represents the persistent class.
     * This might be the same as {@link #managedType}
     * but also a subclass.
     */
    private ClassNodeTracker pc;

    private boolean _defCons = true;
    private boolean _redefine = false;
    private boolean _subclass = false;
    private boolean _fail = false;
    private Set _violations = null;
    private File _dir = null;
    private BytecodeWriter _writer = null;
    private Map<String, String> _backingFields = null; // map of set / get names => field names
    private Map<String, String> _attrsToFields = null; // map of attr names => field names
    private Map<String, String> _fieldsToAttrs = null; // map of field names => attr names
    private boolean _isAlreadyRedefined = false;
    private boolean _isAlreadySubclassed = false;
    private boolean _bcsConfigured = false;

    private boolean _optimizeIdCopy = false; // whether to attempt optimizing id copy

    /**
     * Constructor. Supply configuration and type to enhance. This will look
     * up the metadata for <code>type</code> from <code>conf</code>'s
     * repository.
     */
    public PCEnhancer(OpenJPAConfiguration conf, Class<?> type) {
        this(conf, new EnhancementProject().loadClass(type), (MetaDataRepository) null);
    }

    /**
     * Constructor. Supply configuration and type to enhance. This will look
     * up the metadata for <code>meta</code> by converting back to a class
     * and then loading from <code>conf</code>'s repository.
     */
    public PCEnhancer(OpenJPAConfiguration conf, ClassMetaData meta) {
        this(conf, new EnhancementProject().loadClass(meta.getDescribedType()), meta.getRepository());
    }

    /**
     * Constructor. Supply configuration.
     *
     * @param type  the bytecode representation fo the type to
     *              enhance; this can be created from any stream or file
     * @param repos a metadata repository to use for metadata access,
     *              or null to create a new reporitory; the repository
     *              from the given configuration isn't used by default
     *              because the configuration might be an
     *              implementation-specific subclass whose metadata
     *              required more than just base metadata files
     * @deprecated use {@link #PCEnhancer(OpenJPAConfiguration, ClassNodeTracker,
     * MetaDataRepository, ClassLoader)} instead.
     */
    @Deprecated
    public PCEnhancer(OpenJPAConfiguration conf, ClassNodeTracker type, MetaDataRepository repos) {
        this(conf, type, repos, null);
    }

    /**
     * Constructor. Supply configuration.
     *
     * @param type   the bytecode representation fo the type to
     *               enhance; this can be created from any stream or file
     * @param repos  a metadata repository to use for metadata access,
     *               or null to create a new reporitory; the repository
     *               from the given configuration isn't used by default
     *               because the configuration might be an
     *               implementation-specific subclass whose metadata
     *               required more than just base metadata files
     * @param loader the environment classloader to use for loading
     *               classes and resources.
     */
    public PCEnhancer(OpenJPAConfiguration conf, ClassNodeTracker type, MetaDataRepository repos, ClassLoader loader) {

        // we assume that the original class and the enhanced class is the same
        project = type.getProject();
        managedType = type;
        pc = managedType;

        _log = conf.getLog(OpenJPAConfiguration.LOG_ENHANCE);

        if (repos == null) {
            _repos = conf.newMetaDataRepositoryInstance();
            _repos.setSourceMode(MetaDataModes.MODE_META);
        }
        else {
            _repos = repos;
        }

        _meta = _repos.getMetaData(type.getType(), loader, false);

        configureOptimizeIdCopy();
    }

    /**
     * Constructor. Supply repository. The repository's configuration will
     * be used, and the metadata passed in will be used as-is without doing
     * any additional lookups. This is useful when running the enhancer
     * during metadata load.
     *
     * @param repos a metadata repository to use for metadata access,
     *              or null to create a new reporitory; the repository
     *              from the given configuration isn't used by default
     *              because the configuration might be an
     *              implementation-specific subclass whose metadata
     *              required more than just base metadata files
     * @param type  the bytecode representation fo the type to
     *              enhance; this can be created from any stream or file
     * @param meta  the metadata to use for processing this type.
     * @since 1.1.0
     */
    public PCEnhancer(MetaDataRepository repos, ClassNodeTracker type, ClassMetaData meta) {
        // we assume that the original class and the enhanced class is the same
        project = type.getProject();
        managedType = type;
        pc = managedType;

        _log = repos.getConfiguration()
                .getLog(OpenJPAConfiguration.LOG_ENHANCE);

        _repos = repos;
        _meta = meta;
    }

    static String toPCSubclassName(ClassNodeTracker cnt) {
        return ClassUtil.getPackageName(PCEnhancer.class) + "."
                + cnt.getClassNode().name.replace('/', '$') + "$pcsubclass";
    }

    @Deprecated
    static String toPCSubclassName(Class cls) {
        return ClassUtil.getPackageName(PCEnhancer.class) + "."
                + cls.getName().replace('.', '$') + "$pcsubclass";
    }

    /**
     * Whether <code>className</code> is the name for a
     * dynamically-created persistence-capable subclass.
     *
     * @since 1.1.0
     */
    public static boolean isPCSubclassName(String className) {
        return className.startsWith(ClassUtil.getPackageName(PCEnhancer.class))
                && className.endsWith("$pcsubclass");
    }

    /**
     * If <code>className</code> is a dynamically-created persistence-capable
     * subclass name, returns the name of the class that it subclasses.
     * Otherwise, returns <code>className</code>.
     *
     * @since 1.1.0
     */
    public static String toManagedTypeName(String className) {
        if (isPCSubclassName(className)) {
            className = className.substring(
                    ClassUtil.getPackageName(PCEnhancer.class).length() + 1);
            className = className.substring(0, className.lastIndexOf("$"));
            // this is not correct for nested PCs
            className = className.replace('$', '.');
        }

        return className;
    }

    /**
     * Return the bytecode representation of the persistence-capable class
     * being manipulated.
     */
    public ClassNodeTracker getPCBytecode() {
        return pc;
    }

    /**
     * Return the bytecode representation of the managed class being
     * manipulated. This is usually the same as {@link #getPCBytecode},
     * except when running the enhancer to redefine and subclass
     * existing persistent types.
     */
    public ClassNodeTracker getManagedTypeBytecode() {
        return managedType;
    }

    /**
     * Return the metadata for the class being manipulated, or null if not
     * a persistent type.
     */
    public ClassMetaData getMetaData() {
        return _meta;
    }

    /**
     * A boolean indicating whether the enhancer should add a no-args
     * constructor if one is not already present in the class. OpenJPA
     * requires that a no-arg constructor (whether created by the compiler
     * or by the user) be present in a PC.
     */
    public boolean getAddDefaultConstructor() {
        return _defCons;
    }

    /**
     * A boolean indicating whether the enhancer should add a no-args
     * constructor if one is not already present in the class. OpenJPA
     * requires that a no-arg constructor (whether created by the compiler
     * or by the user) be present in a PC.
     */
    public void setAddDefaultConstructor(boolean addDefaultConstructor) {
        _defCons = addDefaultConstructor;
    }

    /**
     * Whether the enhancer should mutate its arguments, or just run validation
     * and optional subclassing logic on them. Usually used in conjunction with
     * <code>setCreateSubclass(true)</code>.
     *
     * @since 1.0.0
     */
    public boolean getRedefine() {
        return _redefine;
    }

    /**
     * Whether the enhancer should mutate its arguments, or just run validation
     * and optional subclassing logic on them. Usually used in conjunction with
     * <code>setCreateSubclass(true)</code>.
     *
     * @since 1.0.0
     */
    public void setRedefine(boolean redefine) {
        _redefine = redefine;
    }

    /**
     * Whether the type that this instance is enhancing has already been
     * redefined.
     *
     * @since 1.0.0
     */
    public boolean isAlreadyRedefined() {
        return _isAlreadyRedefined;
    }

    /**
     * Whether the type that this instance is enhancing has already been
     * subclassed in this instance's environment classloader.
     *
     * @since 1.0.0
     */
    public boolean isAlreadySubclassed() {
        return _isAlreadySubclassed;
    }

    /**
     * Whether the enhancer should make its arguments persistence-capable,
     * or generate a persistence-capable subclass.
     *
     * @since 1.0.0
     */
    public boolean getCreateSubclass() {
        return _subclass;
    }

    /**
     * Whether the enhancer should make its arguments persistence-capable,
     * or generate a persistence-capable subclass.
     *
     * @since 1.0.0
     */
    public void setCreateSubclass(boolean subclass) {
        _subclass = subclass;
        _addVersionInitFlag = false;
    }

    /**
     * Whether to fail if the persistent type uses property access and
     * bytecode analysis shows that it may be violating OpenJPA's property
     * access restrictions.
     */
    public boolean getEnforcePropertyRestrictions() {
        return _fail;
    }

    /**
     * Whether to fail if the persistent type uses property access and
     * bytecode analysis shows that it may be violating OpenJPA's property
     * access restrictions.
     */
    public void setEnforcePropertyRestrictions(boolean fail) {
        _fail = fail;
    }

    /**
     * The base build directory to generate code to. The proper package
     * structure will be created beneath this directory. Defaults to
     * overwriting the existing class file if null.
     */
    public File getDirectory() {
        return _dir;
    }

    /**
     * The base build directory to generate code to. The proper package
     * structure will be creaed beneath this directory. Defaults to
     * overwriting the existing class file if null.
     */
    public void setDirectory(File dir) {
        _dir = dir;
    }

    /**
     * Return the current {@link BytecodeWriter} to write to or null if none.
     */
    public BytecodeWriter getBytecodeWriter() {
        return _writer;
    }

    /**
     * Set the {@link BytecodeWriter} to write the bytecode to or null if none.
     */
    public void setBytecodeWriter(BytecodeWriter writer) {
        _writer = writer;
    }

    /**
     * Perform bytecode enhancements.
     *
     * @return <code>ENHANCE_*</code> constant
     */
    public int run() {
        try {
            // if enum, skip, no need of any meta
            if ((managedType.getClassNode().access & Opcodes.ACC_ENUM) > 0) {
                return ENHANCE_NONE;
            }

            // if managed interface, skip
            if ((managedType.getClassNode().access & Opcodes.ACC_INTERFACE) > 0) {
                return ENHANCE_INTERFACE;
            }

            // check if already enhanced
            // we cannot simply use instanceof or isAssignableFrom as we have a temp ClassLoader inbetween
            ClassLoader loader = managedType.getClassLoader();
            for (String iface : managedType.getClassNode().interfaces) {
                final String pctypeInternalName = TYPE_PCTYPE.getInternalName();
                if (iface.equals(pctypeInternalName)) {
                    if (_log.isTraceEnabled()) {
                        _log.trace(_loc.get("pc-type", managedType.getClassNode().name, loader));
                    }
                    return ENHANCE_NONE;
                }
            }

            if (_log.isTraceEnabled()) {
                _log.trace(_loc.get("enhance-start", managedType.getClassNode().name));
            }


            configureBCs();

            // validate properties before replacing field access so that
            // we build up a record of backing fields, etc
            if (isPropertyAccess(_meta)) {
                validateProperties();
                if (getCreateSubclass()) {
                    addAttributeTranslation();
                }
            }
            replaceAndValidateFieldAccess();
            processViolations();

            if (_meta != null) {
                enhanceClass(pc);
                addFields(pc);
                addStaticInitializer(pc);
                addPCMethods();
                addAccessors(pc);
                addAttachDetachCode();
                addSerializationCode();
                addCloningCode();
                runAuxiliaryEnhancers();

                return ENHANCE_PC;
            }
            return ENHANCE_AWARE;
        }
        catch (OpenJPAException ke) {
            throw ke;
        }
        catch (Exception e) {
            throw new GeneralException(_loc.get("enhance-error",
                                                managedType.getClassNode().name, e.getMessage()), e);
        }
    }

    private void configureBCs() {
        if (!_bcsConfigured) {
            if (getRedefine()) {
                final boolean isRedefined = managedType.getClassNode().attrs != null &&
                        managedType.getClassNode().attrs.stream().anyMatch(a -> a.isUnknown() && a.type.equals(RedefinedAttribute.ATTR_TYPE));

                if (!isRedefined) {
                    if (managedType.getClassNode().attrs == null) {
                        managedType.getClassNode().attrs = new ArrayList<>();
                    }
                    managedType.getClassNode().attrs.add(new RedefinedAttribute());
                }
                else {
                    _isAlreadyRedefined = true;
                }
            }

            if (getCreateSubclass()) {
                PCSubclassValidator val = new PCSubclassValidator(_meta, managedType.getClassNode(), _log, _fail);
                val.assertCanSubclass();
                pc = project.loadClass(toPCSubclassName(managedType));
                if (pc.getClassNode().superName.equals("java/lang/Object")) {
                    // set the parent class
                    pc.getClassNode().superName = managedType.getClassNode().name;
                    if ((managedType.getClassNode().access & Opcodes.ACC_ABSTRACT) > 0) {
                        pc.getClassNode().access |= Opcodes.ACC_ABSTRACT;
                    }

                    pc.declareInterface(DynamicPersistenceCapable.class);
                }
                else {
                    _isAlreadySubclassed = true;
                }
            }

            _bcsConfigured = true;
        }
    }

    /**
     * Write the generated bytecode.
     */
    public void record() throws IOException {
        if (managedType != pc && getRedefine()) {
            record(managedType);
        }

        record(pc);
    }

    /**
     * Write the given class.
     */
    private void record(ClassNodeTracker cnt)
            throws IOException {
        if (_writer != null) {
            _writer.write(cnt);
        }
        else if (_dir == null) {
            String name = cnt.getClassNode().name.replace(".", "/");
            ClassLoader cl = cnt.getClassLoader();
            if (cl == null) {
                cl = Thread.currentThread().getContextClassLoader();
            }
            final URL resource = cl.getResource(name + ".class");
            try (OutputStream out = new FileOutputStream(URLDecoder.decode(resource.getFile()))) {
                out.write(AsmHelper.toByteArray(cnt));
                out.flush();
            }
        }
        else {
            String name = cnt.getClassNode().name.replace(".", "/") + ".class";
            File targetFile = new File(_dir, name);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            java.nio.file.Files.write(targetFile.toPath(), AsmHelper.toByteArray(cnt));
        }
    }

    /**
     * Validate that the methods that use a property-access instance are
     * written correctly. This method also gathers information on each
     * property's backing field.
     */
    private void validateProperties() {
        final ClassNode classNode = managedType.getClassNode();
        FieldMetaData[] fmds;
        if (getCreateSubclass()) {
            fmds = _meta.getFields();
        }
        else {
            fmds = _meta.getDeclaredFields();
        }

        Method getter, setter;
        Field returned, assigned = null;

        for (FieldMetaData fmd : fmds) {
            if (!(fmd.getBackingMember() instanceof Method)) {
                // If not mixed access is not defined, flag the field members,
                // otherwise do not process them because they are valid
                // persistent attributes.
                if (!_meta.isMixedAccess()) {
                    addViolation("property-bad-member",
                                 new Object[]{fmd, fmd.getBackingMember()},
                                 true);
                }
                continue;
            }

            getter = (Method) fmd.getBackingMember();

            if (getter == null) {
                addViolation("property-no-getter", new Object[]{fmd},
                             true);
                continue;
            }
            returned = getReturnedField(classNode, getter);


            if (returned != null) {
                registerBackingFieldInfo(fmd, getter, returned);
            }

            setter = getMethod(getter.getDeclaringClass(), getSetterName(fmd), fmd.getDeclaredType());

            if (setter == null) {
                if (returned == null) {
                    addViolation("property-no-setter",
                                 new Object[]{fmd}, true);
                    continue;
                }
                else if (!getRedefine()) {
                    // create synthetic setter
                    MethodNode setterNode = new MethodNode(Opcodes.ACC_PRIVATE,
                                                           getSetterName(fmd),
                                                           Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(fmd.getDeclaredType())),
                                                           null, null);
                    //X TODO: pc or managedType?
                    pc.getClassNode().methods.add(setterNode);
                    InsnList instructions = setterNode.instructions;
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                    instructions.add(new VarInsnNode(AsmHelper.getLoadInsn(fmd.getDeclaredType()), 1)); // param1
                    instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,
                                                       Type.getInternalName(returned.getDeclaringClass()),
                                                       returned.getName(),
                                                       Type.getDescriptor(fmd.getDeclaredType())));
                    instructions.add(new InsnNode(Opcodes.RETURN));
                }
            }

            if (setter != null) {
                assigned = getAssignedField(classNode, getMethod(fmd.getDeclaringType(), fmd.getSetterName(), new Class[]{fmd.getDeclaredType()}));
            }

            if (assigned != null) {
                if (setter != null) {
                    registerBackingFieldInfo(fmd, setter, assigned);
                }

                if (!assigned.equals(returned)) {
                    addViolation("property-setter-getter-mismatch", new Object[]
                            {fmd, assigned.getName(), (returned == null)
                                    ? null : returned.getName()}, false);
                }
            }
        }
    }

    private void registerBackingFieldInfo(FieldMetaData fmd, Method method, Field field) {
        if (_backingFields == null) {
            _backingFields = new HashMap();
        }
        _backingFields.put(method.getName(), field.getName());

        if (_attrsToFields == null) {
            _attrsToFields = new HashMap();
        }
        _attrsToFields.put(fmd.getName(), field.getName());

        if (_fieldsToAttrs == null) {
            _fieldsToAttrs = new HashMap();
        }
        _fieldsToAttrs.put(field.getName(), fmd.getName());
    }


    private void addAttributeTranslation() {

        // Get all field metadata
        ArrayList<Integer> propFmds = new ArrayList<>();
        FieldMetaData[] fmds = _meta.getFields();

        if (_meta.isMixedAccess()) {
            // Stores indexes of property access fields to be used in
            //
            propFmds = new ArrayList<>();

            // Determine which fields have property access and save their
            // indexes
            for (int i = 0; i < fmds.length; i++) {
                if (isPropertyAccess(fmds[i])) {
                    propFmds.add(i);
                }
            }

            // if no fields have property access do not do attribute translation
            if (propFmds.size() == 0) {
                return;
            }
        }

        ClassNode classNode = pc.getClassNode();
        classNode.interfaces.add(Type.getInternalName(AttributeTranslator.class));

        MethodNode attrIdxMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                PRE + "AttributeIndexToFieldName",
                                                Type.getMethodDescriptor(Type.getType(String.class), Type.INT_TYPE),
                                                null, null);
        classNode.methods.add(attrIdxMeth);

        InsnList instructions = attrIdxMeth.instructions;

        // switch (val)
        instructions.add(new VarInsnNode(Opcodes.ILOAD, 1)); // int param of the method
        if (!_meta.isMixedAccess()) {
            // if not mixed access use a table switch on all property-based fmd.
            // a table switch is more efficient with +1 incremental operations

            LabelNode defLbl = new LabelNode();
            TableSwitchInsnNode switchNd = new TableSwitchInsnNode(0, fmds.length - 1, defLbl);
            instructions.add(switchNd);

            // case i:
            //     return <_attrsToFields.get(fmds[i].getName())>
            for (FieldMetaData fmd : fmds) {
                LabelNode caseLabel = new LabelNode();
                switchNd.labels.add(caseLabel);
                instructions.add(caseLabel);
                instructions.add(AsmHelper.getLoadConstantInsn(_attrsToFields.get(fmd.getName())));
                instructions.add(new InsnNode(Opcodes.ARETURN));
            }

            // default: throw new IllegalArgumentException ()
            instructions.add(defLbl);
            instructions.add(throwException(IllegalArgumentException.class));
        }
        else {
            // In mixed access mode, property indexes are not +1 incremental
            // a lookup switch must be used to do indexed lookup.
            LabelNode defLbl = new LabelNode();
            LookupSwitchInsnNode switchNd = new LookupSwitchInsnNode(defLbl, null, null);
            instructions.add(switchNd);
            for (Integer i : propFmds) {
                LabelNode caseLabel = new LabelNode();
                instructions.add(caseLabel);
                switchNd.labels.add(caseLabel);
                switchNd.keys.add(propFmds.get(i));
                instructions.add(AsmHelper.getLoadConstantInsn(_attrsToFields.get(fmds[i].getName())));
                instructions.add(new InsnNode(Opcodes.ARETURN));
            }
        }
    }

    /**
     * Return the name of the setter method for the given field.
     */
    private static String getSetterName(FieldMetaData fmd) {
        return fmd.getSetterName();
    }

    /**
     * Return the field returned by the given method, or null if none.
     * Package-protected and static for testing.
     */
    static Field getReturnedField(ClassNode classNode, Method meth) {
        return findField(classNode, meth, (ain) -> ain.getOpcode() == AsmHelper.getReturnInsn(meth.getReturnType()), false);
    }


    /**
     * Return the field assigned in the given method, or null if none.
     * Package-protected and static for testing.
     */
    static Field getAssignedField(ClassNode classNode, Method meth) {
        return findField(classNode, meth, (ain) -> ain.getOpcode() == Opcodes.PUTFIELD, true);
    }

    /**
     * Return the field returned / assigned by <code>meth</code>. Returns
     * null if non-fields (methods, literals, parameters, variables) are
     * returned, or if non-parameters are assigned to fields.
     */
    private static Field findField(ClassNode classNode, Method meth, Predicate<AbstractInsnNode> ain, boolean findAccessed) {
        // ignore any static methods. OpenJPA only currently supports
        // non-static setters and getters
        if (Modifier.isStatic(meth.getModifiers())) {
            return null;
        }

        if (meth.getDeclaringClass().isInterface()) {
            return null;
        }

        final MethodNode methodNode = findMethodNode(classNode, meth);

        Field field = null;
        Field cur;
        AbstractInsnNode prevInsn, earlierInsn;
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (!ain.test(insn)) {
                continue;
            }

            prevInsn = insn.getPrevious();
            if (prevInsn == null) {
                return null;
            }

            // skip a few non-functional ops like instanceof and checkcast
            if ((prevInsn.getOpcode() == Opcodes.INSTANCEOF || prevInsn.getOpcode() == Opcodes.CHECKCAST)
                    && prevInsn.getPrevious() != null) {
                prevInsn = prevInsn.getPrevious();
            }

            if (prevInsn.getPrevious() == null) {
                return null;
            }

            earlierInsn = prevInsn.getPrevious();

            // if the opcode two before the template was an aload_0, check
            // against the middle instruction based on what type of find
            // we're doing
            if (!AsmHelper.isLoadInsn(earlierInsn)
                    || !AsmHelper.isThisInsn(earlierInsn)) {
                return null;
            }

            // if the middle instruction was a getfield, then it's the
            // field that's being accessed
            if (!findAccessed && prevInsn.getOpcode() == Opcodes.GETFIELD) {
                final FieldInsnNode fieldInsn = (FieldInsnNode) prevInsn;

                cur = getField(meth.getDeclaringClass(), fieldInsn.name);

                // if the middle instruction was an xload_1, then the
                // matched instruction is the field that's being set.
            }
            else if (findAccessed && AsmHelper.isLoadInsn(prevInsn)
                    && ((VarInsnNode) prevInsn).var == 1) {
                final FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                cur = getField(meth.getDeclaringClass(), fieldInsn.name);
            }
            else {
                return null;
            }


            if (field != null && !cur.equals(field)) {
                return null;
            }
            field = cur;
        }


        return field;
    }

    private static MethodNode findMethodNode(ClassNode classNode, Method meth) {
        return AsmHelper.getMethodNode(classNode, meth).get();
    }


    private static Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        }
        catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() == Object.class) {
                throw new IllegalStateException("Cannot find field " + fieldName + " in Class " + clazz);
            }
            return getField(clazz.getSuperclass(), fieldName);
        }
    }

    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, paramTypes);
        }
        catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() == Object.class) {
                throw new IllegalStateException("Cannot find method " + methodName + " in Class " + clazz);
            }
            return getMethod(clazz.getSuperclass(), methodName);
        }
    }

    /**
     * Record a violation of the property access restrictions.
     */
    private void addViolation(String key, Object[] args, boolean fatal) {
        if (_violations == null) {
            _violations = new HashSet();
        }
        _violations.add(_loc.get(key, args));
        _fail |= fatal;
    }

    /**
     * Log / throw recorded property access violations.
     */
    private void processViolations() {
        if (_violations == null) {
            return;
        }

        String sep = J2DoPrivHelper.getLineSeparator();
        StringBuilder buf = new StringBuilder();
        for (Iterator itr = _violations.iterator(); itr.hasNext(); ) {
            buf.append(itr.next());
            if (itr.hasNext()) {
                buf.append(sep);
            }
        }
        Message msg = _loc.get("property-violations", buf);

        if (_fail) {
            throw new UserException(msg);
        }
        if (_log.isWarnEnabled()) {
            _log.warn(msg);
        }
    }

    /**
     * Replaced all direct access to managed fields with the appropriate
     * pcGet/pcSet method. Note that this includes access to fields
     * owned by PersistenceCapable classes other than this one.
     */
    private void replaceAndValidateFieldAccess() throws NoSuchMethodException, ClassNotFoundException {
        final ClassNode classNode = pc.getClassNode();
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.instructions.size() > 0 && !skipEnhance(methodNode)) {
                replaceAndValidateFieldAccess(classNode, methodNode, (a) -> a.getOpcode() == Opcodes.GETFIELD, true);
                replaceAndValidateFieldAccess(classNode, methodNode, (a) -> a.getOpcode() == Opcodes.PUTFIELD, false);
            }
        }
    }

    /**
     * Replaces all instructions matching the given template in the given
     * code block with calls to the appropriate generated getter/setter.
     *
     * @param methodNode the code block to modify; the code iterator will
     *                   be placed before the first instruction on method start,
     *                   and will be after the last instruction on method completion
     * @param insnCheck  the template instruction to search for; either a
     *                   getfield or putfield instruction
     * @param get        boolean indicating if this is a get instruction
     */
    private void replaceAndValidateFieldAccess(ClassNode classNode, MethodNode methodNode, Predicate<AbstractInsnNode> insnCheck,
                                               boolean get) throws NoSuchMethodException, ClassNotFoundException {
        AbstractInsnNode currentInsn = methodNode.instructions.getFirst();

        // skip to the next instruction we are looking for
        while ((currentInsn = searchNextInstruction(currentInsn, insnCheck)) != null) {
            FieldInsnNode fi = (FieldInsnNode) currentInsn;
            String name = fi.name;

            ClassMetaData owner = null;
            if (fi.owner != null) {
                final Class<?> declarerType = AsmHelper.getDescribedClass(managedType.getClassLoader(), fi.owner);
                owner = getPersistenceCapableOwner(name, declarerType);
            }
            FieldMetaData fmd = owner == null ? null : owner.getField(name);
            if (isPropertyAccess(fmd)) {
                // if we're directly accessing a field in another class
                // hierarchy that uses property access, something is wrong
                if (owner != _meta && owner.getDeclaredField(name) != null &&
                        _meta != null && !owner.getDescribedType()
                        .isAssignableFrom(_meta.getDescribedType())) {
                    throw new UserException(_loc.get("property-field-access",
                                                     new Object[]{_meta, owner, name, methodNode.name}));
                }

                // if we're directly accessing a property-backing field outside
                // the property in our own class, notify user
                if (isBackingFieldOfAnotherProperty(methodNode, name)) {
                    addViolation("property-field-access", new Object[]{_meta, owner, name, methodNode.name}, false);
                }
            }

            if (owner == null || owner.getDeclaredField(fromBackingFieldName(name)) == null) {
                // not a persistent field?
            }
            else if (!getRedefine() && !getCreateSubclass() && isFieldAccess(fmd)) {
                // replace the instruction with a call to the generated access method
                Type ownerType = Type.getType(getType(owner));
                MethodInsnNode pcCall;
                if (get) {
                    pcCall = new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                ownerType.getInternalName(),
                                                PRE + "Get" + name,
                                                Type.getMethodDescriptor(Type.getType(fi.desc), ownerType));
                }
                else {
                    pcCall = new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                ownerType.getInternalName(),
                                                PRE + "Set" + name,
                                                Type.getMethodDescriptor(Type.VOID_TYPE, ownerType, Type.getType(fi.desc)));
                }
                methodNode.instructions.insertBefore(currentInsn, pcCall);
                // and now delete the direct field access
                methodNode.instructions.remove(currentInsn);

                // next iteration will be started here.
                currentInsn = pcCall;
            }
            else if (getRedefine()) {
                name = fromBackingFieldName(name);
                if (get) {
                    addNotifyAccess(methodNode, currentInsn, owner.getField(name));
                }
                else {
                    // insert the set operations after the field mutation, but
                    // first load the old value for use in the
                    // StateManager.settingXXX method.

                    InsnList insns = new InsnList();
                    insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this

                    int valVarPos = methodNode.maxLocals++;
                    insns.add(new VarInsnNode(AsmHelper.getCorrespondingLoadInsn(fi.getOpcode()), valVarPos));

                    currentInsn = addNotifyMutation(classNode, methodNode, currentInsn, owner.getField(name), valVarPos, -1);
                }

            }

            currentInsn = currentInsn.getNext();
        }
    }

    /**
     * Scan the instructions until you found any which fits the predicate.
     *
     * @param currentInsn the instruction to start searching from
     * @param insnCheck   the condition which has to be met
     * @return the instruction node we did search for or {@code null} if there is no such instruction.
     */
    private AbstractInsnNode searchNextInstruction(AbstractInsnNode currentInsn, Predicate<AbstractInsnNode> insnCheck) {
        while (currentInsn != null && !insnCheck.test(currentInsn)) {
            currentInsn = currentInsn.getNext();
        }

        return currentInsn;
    }

    /**
     * Add the following code to the code:
     * <code>
     * PCHelper.accessingField(this, <absolute-index>);
     * </code>
     */
    private void addNotifyAccess(MethodNode methodNode, AbstractInsnNode currentInsn, FieldMetaData fmd) {
        InsnList insns = new InsnList();

        insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        insns.add(AsmHelper.getLoadConstantInsn(fmd.getIndex()));
        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                     Type.getInternalName(RedefinitionHelper.class),
                                     "accessingField",
                                     Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, Type.INT_TYPE)));

        if (methodNode.instructions.size() == 0) {
            methodNode.instructions.add(insns);
        }
        else {
            methodNode.instructions.insertBefore(currentInsn, insns);
        }
    }

    /**
     * This must be called after setting the value in the object.
     *
     * @param valVarPos the position in the local variable table where the
     *                  old value is stored
     * @param param     the parameter position containing the new value, or
     *                  -1 if the new value is unavailable and should therefore be looked
     *                  up.
     * @return the last inserted InsnNode
     */
    private AbstractInsnNode addNotifyMutation(ClassNode classNode, MethodNode methodNode, AbstractInsnNode currentInsn,
                                               FieldMetaData fmd, int valVarPos, int param) {
        // PCHelper.settingField(this, <absolute-index>, old, new);
        InsnList insns = new InsnList();

        insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        insns.add(AsmHelper.getLoadConstantInsn(fmd.getIndex()));

        Class type = fmd.getDeclaredType();
        // we only have special signatures for primitives and Strings
        if (!type.isPrimitive() && type != String.class) {
            type = Object.class;
        }
        insns.add(new VarInsnNode(AsmHelper.getLoadInsn(type), valVarPos));
        if (param == -1) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            addGetManagedValueCode(classNode, insns, fmd, true);
        }
        else {
            insns.add(new VarInsnNode(AsmHelper.getLoadInsn(type), param + 1));
        }
        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                     Type.getInternalName(RedefinitionHelper.class),
                                     "settingField",
                                     Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, Type.INT_TYPE, Type.getType(type), Type.getType(type))));

        methodNode.instructions.insert(currentInsn, insns);
        return insns.getLast();
    }


    /**
     * Return true if the given instruction accesses a field that is a backing
     * field of another property in this property-access class.
     */
    private boolean isBackingFieldOfAnotherProperty(MethodNode methodNode, String name) {
        String methName = methodNode.name;
        return !"<init>".equals(methName)
                && _backingFields != null
                && !name.equals(_backingFields.get(methName))
                && _backingFields.containsValue(name);
    }

    /**
     * Helper method to return the declaring PersistenceCapable class of
     * the given field.
     *
     * @param fieldName the name of the field
     * @param owner     the nominal owner of the field
     * @return the metadata for the PersistenceCapable type that
     * declares the field (and therefore has the static method), or null if none
     */
    private ClassMetaData getPersistenceCapableOwner(String fieldName,
                                                     Class owner) {
        // find the actual ancestor class that declares the field, then
        // check if the class is persistent, and if the field is managed
        Field f = Reflection.findField(owner, fieldName, false);
        if (f == null) {
            return null;
        }

        // managed interface
        if (_meta != null && _meta.getDescribedType().isInterface()) {
            return _meta;
        }

        return _repos.getMetaData(f.getDeclaringClass(), null, false);
    }

    /**
     * Adds all synthetic methods to the bytecode by delegating to
     * the various addXXXMethods () functions in this class. Includes
     * all static field access methods.
     * Note that the 'stock' methods like <code>pcIsTransactional</code>,
     * <code>pcFetchObjectId</code>, etc are defined only in the
     * least-derived PersistenceCapable type.
     */
    private void addPCMethods() throws NoSuchMethodException {
        addClearFieldsMethod(pc.getClassNode());

        addNewInstanceMethod(pc.getClassNode(), true);
        addNewInstanceMethod(pc.getClassNode(), false);

        addManagedFieldCountMethod(pc.getClassNode());
        addReplaceFieldsMethods(pc.getClassNode());
        addProvideFieldsMethods(pc.getClassNode());

        addCopyFieldsMethod(pc.getClassNode());

        if (_meta.getPCSuperclass() == null || getCreateSubclass()) {
            addStockMethods();
            addGetVersionMethod();
            addReplaceStateManagerMethod();

            if (_meta.getIdentityType() != ClassMetaData.ID_APPLICATION) {
                addNoOpApplicationIdentityMethods();
            }
        }

        // add the app id methods to each subclass rather
        // than just the superclass, since it is possible to have
        // a subclass with an app id hierarchy that matches the
        // persistent class inheritance hierarchy
        if (_meta.getIdentityType() == ClassMetaData.ID_APPLICATION
                && (_meta.getPCSuperclass() == null || getCreateSubclass() ||
                _meta.getObjectIdType() != _meta.getPCSuperclassMetaData().getObjectIdType())) {

            addCopyKeyFieldsToObjectIdMethod(true);
            addCopyKeyFieldsToObjectIdMethod(false);
            addCopyKeyFieldsFromObjectIdMethod(true);
            addCopyKeyFieldsFromObjectIdMethod(false);

            if (_meta.hasAbstractPKField()) {
                addGetIDOwningClass();
            }

            if (_meta.isEmbeddable() && _meta.getIdentityType() == ClassMetaData.ID_APPLICATION) {
                _log.warn(_loc.get("ID-field-in-embeddable-unsupported", _meta.toString()));
            }

            addNewObjectIdInstanceMethod(true);
            addNewObjectIdInstanceMethod(false);
        }
        else if (_meta.hasPKFieldsFromAbstractClass()) {
            addGetIDOwningClass();
        }
    }

    /**
     * Add a method to clear all persistent fields; we'll call this from
     * the new instance method to ensure that unloaded fields have
     * default values.
     */
    private void addClearFieldsMethod(ClassNode classNode) throws NoSuchMethodException {
        // protected void pcClearFields ()
        MethodNode clearFieldMethod = new MethodNode(Opcodes.ACC_PROTECTED,
                                                     PRE + "ClearFields",
                                                     Type.getMethodDescriptor(Type.VOID_TYPE),
                                                     null, null);

        final InsnList instructions = clearFieldMethod.instructions;
        if (_meta.getPCSuperclass() != null && !getCreateSubclass()) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(getType(_meta.getPCSuperclassMetaData())),
                                                PRE + "ClearFields",
                                                Type.getMethodDescriptor(Type.VOID_TYPE)));
        }

        FieldMetaData[] fmds = _meta.getDeclaredFields();
        for (FieldMetaData fmd : fmds) {
            if (fmd.getManagement() != FieldMetaData.MANAGE_PERSISTENT) {
                continue;
            }
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            switch (fmd.getDeclaredTypeCode()) {
                case JavaTypes.BOOLEAN:
                case JavaTypes.BYTE:
                case JavaTypes.CHAR:
                case JavaTypes.INT:
                case JavaTypes.SHORT:
                    instructions.add(getSetValueInsns(classNode, fmd, 0));
                    break;
                case JavaTypes.DOUBLE:
                    instructions.add(getSetValueInsns(classNode, fmd, 0D));
                    break;
                case JavaTypes.FLOAT:
                    instructions.add(getSetValueInsns(classNode, fmd, 0F));
                    break;
                case JavaTypes.LONG:
                    instructions.add(getSetValueInsns(classNode, fmd, 0L));
                    break;
                default:
                    instructions.add(getSetValueInsns(classNode, fmd, null));
                    break;
            }
        }
        instructions.add(new InsnNode(Opcodes.RETURN));

        classNode.methods.add(clearFieldMethod);

    }

    /**
     * Adds the <code>pcNewInstance</code> method to the bytecode.
     * These methods are used by the impl helper to create new
     * managed instances efficiently without reflection.
     *
     * @param oid set to true to mimic the method version that takes
     *            an oid value as well as a state manager
     */
    private void addNewInstanceMethod(ClassNode classNode, boolean oid) {
        // public PersistenceCapable pcNewInstance (...)
        String desc = oid
                ? Type.getMethodDescriptor(Type.getType(PCTYPE), Type.getType(SMTYPE), TYPE_OBJECT, Type.BOOLEAN_TYPE)
                : Type.getMethodDescriptor(Type.getType(PCTYPE), Type.getType(SMTYPE), Type.BOOLEAN_TYPE);
        MethodNode newInstance = new MethodNode(Opcodes.ACC_PUBLIC,
                                                PRE + "NewInstance",
                                                desc,
                                                null, null);
        classNode.methods.add(newInstance);
        final InsnList instructions = newInstance.instructions;

        if ((pc.getClassNode().access & Opcodes.ACC_ABSTRACT) > 0) {
            instructions.add(throwException(USEREXCEP));
            return;
        }

        // XXX pc = new XXX ();
        instructions.add(new TypeInsnNode(Opcodes.NEW, classNode.name));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            classNode.name,
                                            "<init>",
                                            Type.getMethodDescriptor(Type.VOID_TYPE)));

        int newPcVarPos = (oid) ? 4 : 3; // number of params +1
        instructions.add(new VarInsnNode(Opcodes.ASTORE, newPcVarPos));

        // if (clear)
        //   pc.pcClearFields ();
        instructions.add(new VarInsnNode(Opcodes.ILOAD, (oid) ? 3 : 2));
        LabelNode labelAfterClearFields = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, labelAfterClearFields));

        // inside the if
        instructions.add(new VarInsnNode(Opcodes.ALOAD, newPcVarPos));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            classNode.name,
                                            PRE + "ClearFields",
                                            Type.getMethodDescriptor(Type.VOID_TYPE)));

        instructions.add(labelAfterClearFields);

        // pc.pcStateManager = sm;
        instructions.add(new VarInsnNode(Opcodes.ALOAD, newPcVarPos));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // the 1st method param
        instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        // copy key fields from oid
        if (oid) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, newPcVarPos));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // the 2nd method param, Object
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + "CopyKeyFieldsFromObjectId",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT)));
        }

        instructions.add(new VarInsnNode(Opcodes.ALOAD, newPcVarPos));
        instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    /**
     * Adds the <code>protected static int pcGetManagedFieldCount ()</code>
     * method to the bytecode, returning the inherited field count added
     * to the number of managed fields in the current PersistenceCapable class.
     */
    private void addManagedFieldCountMethod(ClassNode classNode) {
        MethodNode getFieldCountMeth = new MethodNode(Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC,
                                                      PRE + "GetManagedFieldCount",
                                                      Type.getMethodDescriptor(Type.INT_TYPE),
                                                      null, null);
        classNode.methods.add(getFieldCountMeth);

        // return <fields> + pcInheritedFieldCount
        // awhite: the above should work, but I'm seeing a messed up situation
        // all of a sudden where when a subclass calls this method, it somehow
        // happens before <clinit> is ever invoked, and so our
        // pcInheritedFieldCount field isn't initialized!  so instead,
        // return <fields> + <superclass>.pcGetManagedFieldCount ()
        final InsnList instructions = getFieldCountMeth.instructions;
        instructions.add(AsmHelper.getLoadConstantInsn(_meta.getDeclaredFields().length));
        if (_meta.getPCSuperclass() != null) {
            Class superClass = getType(_meta.getPCSuperclassMetaData());
            String superName = getCreateSubclass() ?
                    PCEnhancer.toPCSubclassName(superClass).replace(".", "/") :
                    Type.getInternalName(superClass);
            instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                superName,
                                                PRE + "GetManagedFieldCount",
                                                Type.getMethodDescriptor(Type.INT_TYPE)));
            instructions.add(new InsnNode(Opcodes.IADD));
        }

        instructions.add(new InsnNode(Opcodes.IRETURN));
    }

    /**
     * Adds the {@link PersistenceCapable#pcProvideField} and
     * {@link PersistenceCapable#pcProvideFields} methods to the bytecode.
     */
    private void addProvideFieldsMethods(ClassNode classNode) throws NoSuchMethodException {
        MethodNode provideFieldsMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                      PRE + "ProvideField",
                                                      Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
                                                      null, null);
        classNode.methods.add(provideFieldsMeth);
        final InsnList instructions = provideFieldsMeth.instructions;

        final int relLocal = beginSwitchMethod(classNode, PRE + "ProvideField", instructions, false);

        // if no fields in this inst, just throw exception
        FieldMetaData[] fmds = getCreateSubclass() ? _meta.getFields()
                : _meta.getDeclaredFields();
        if (fmds.length == 0) {
            instructions.add(throwException(IllegalArgumentException.class));
        }
        else {
            // switch (val)
            instructions.add(new VarInsnNode(Opcodes.ILOAD, relLocal));

            LabelNode defaultCase = new LabelNode();
            TableSwitchInsnNode ts = new TableSwitchInsnNode(0, fmds.length - 1, defaultCase);
            instructions.add(ts);

            // <field> = pcStateManager.provided<type>Field(this, fieldNumber);
            for (FieldMetaData fmd : fmds) {
                // case xxx:
                LabelNode caseLabel = new LabelNode();
                instructions.add(caseLabel);
                ts.labels.add(caseLabel);

                // load pcStateManager to stack
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

                // invoke StateManager#provided
                final Method smProvidedMeth = getStateManagerMethod(fmd.getDeclaredType(), "provided", false, false);

                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new VarInsnNode(Opcodes.ILOAD, 1)); // fieldNr int

                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this for the getfield
                addGetManagedValueCode(classNode, instructions, fmd, true);

                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(SMTYPE),
                                                    smProvidedMeth.getName(),
                                                    Type.getMethodDescriptor(smProvidedMeth)));

                instructions.add(new InsnNode(Opcodes.RETURN));
            }

            instructions.add(defaultCase);
            instructions.add(throwException(IllegalArgumentException.class));
        }

        addMultipleFieldsMethodVersion(classNode, provideFieldsMeth, false);
    }

    /**
     * Adds the {@link PersistenceCapable#pcReplaceField} and
     * {@link PersistenceCapable#pcReplaceFields} methods to the bytecode.
     */
    private void addReplaceFieldsMethods(ClassNode classNode) throws NoSuchMethodException {
        MethodNode replaceFieldMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                     PRE + "ReplaceField",
                                                     Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
                                                     null, null);
        classNode.methods.add(replaceFieldMeth);
        final InsnList instructions = replaceFieldMeth.instructions;
        final int relLocal = beginSwitchMethod(classNode, PRE + "ReplaceField", instructions, false);

        // if no fields in this inst, just throw exception
        FieldMetaData[] fmds = getCreateSubclass() ? _meta.getFields()
                : _meta.getDeclaredFields();
        if (fmds.length == 0) {
            instructions.add(throwException(IllegalArgumentException.class));
        }
        else {
            // switch (val)
            instructions.add(new VarInsnNode(Opcodes.ILOAD, relLocal));

            LabelNode defaultCase = new LabelNode();
            TableSwitchInsnNode ts = new TableSwitchInsnNode(0, fmds.length - 1, defaultCase);
            instructions.add(ts);

            // <field> = pcStateManager.replace<type>Field(this, fieldNumber);
            for (FieldMetaData fmd : fmds) {
                // case xxx:
                LabelNode caseLabel = new LabelNode();
                instructions.add(caseLabel);
                ts.labels.add(caseLabel);

                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this

                // load pcStateManager to stack
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

                // invoke StateManager#replace
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new VarInsnNode(Opcodes.ILOAD, 1)); // fieldNr int
                final Method rmReplaceMeth = getStateManagerMethod(fmd.getDeclaredType(), "replace", true, false);
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(SMTYPE),
                                                    rmReplaceMeth.getName(),
                                                    Type.getMethodDescriptor(rmReplaceMeth)));
                if (!fmd.getDeclaredType().isPrimitive()) {
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(fmd.getDeclaredType())));
                }

                addSetManagedValueCode(classNode, instructions, fmd);

                if (_addVersionInitFlag && fmd.isVersion()) {
                    // If this case is setting the version field
                    // pcVersionInit = true;
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                    instructions.add(new InsnNode(Opcodes.ICONST_1));
                    putfield(classNode, instructions, getType(_meta), VERSION_INIT_STR, boolean.class);
                }

                instructions.add(new InsnNode(Opcodes.RETURN));
            }

            instructions.add(defaultCase);
            instructions.add(throwException(IllegalArgumentException.class));
        }

        addMultipleFieldsMethodVersion(classNode, replaceFieldMeth, false);
    }


    /**
     * Adds the {@link PersistenceCapable#pcCopyFields} method to the bytecode.
     */
    private void addCopyFieldsMethod(ClassNode classNode) {
        MethodNode copyFieldMeth = new MethodNode(Opcodes.ACC_PROTECTED,
                                                  PRE + "CopyField",
                                                  Type.getMethodDescriptor(Type.VOID_TYPE,
                                                                           Type.getObjectType(managedType.getClassNode().name),
                                                                           Type.INT_TYPE),
                                                  null, null);
        classNode.methods.add(copyFieldMeth);
        final InsnList instructions = copyFieldMeth.instructions;
        final int relLocal = beginSwitchMethod(classNode, PRE + "CopyField", instructions, true);

        // if no fields in this inst, just throw exception
        FieldMetaData[] fmds = getCreateSubclass() ? _meta.getFields()
                : _meta.getDeclaredFields();
        if (fmds.length == 0) {
            instructions.add(throwException(IllegalArgumentException.class));
        }
        else {
            instructions.add(new VarInsnNode(Opcodes.ILOAD, relLocal));

            LabelNode defaultCase = new LabelNode();
            TableSwitchInsnNode ts = new TableSwitchInsnNode(0, fmds.length - 1, defaultCase);
            instructions.add(ts);

            // <field> = other.<field>;
            // or set<field> (other.get<field>);
            for (FieldMetaData fmd : fmds) {
                // case xxx:
                LabelNode caseLabel = new LabelNode();
                instructions.add(caseLabel);
                ts.labels.add(caseLabel);

                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // other instance

                addGetManagedValueCode(classNode, instructions, fmd, false);
                addSetManagedValueCode(classNode, instructions, fmd);

                instructions.add(new InsnNode(Opcodes.RETURN));
            }

            instructions.add(defaultCase);
            instructions.add(throwException(IllegalArgumentException.class));
        }

        addMultipleFieldsMethodVersion(classNode, copyFieldMeth, true);
    }

    /**
     * Helper method to add the code common to the beginning of both the
     * pcReplaceField method and the pcProvideField method. This includes
     * calculating the relative field number of the desired field and calling
     * the superclass if necessary.
     *
     * @return the index in which the local variable holding the relative
     * field number is stored
     */
    private int beginSwitchMethod(ClassNode classNode, String name, InsnList instructions, boolean copy) {
        int fieldNumber = (copy) ? 2 : 1;
        int relLocal = fieldNumber + 1;
        if (getCreateSubclass()) {
            instructions.add(new VarInsnNode(Opcodes.ILOAD, fieldNumber));
            instructions.add(new VarInsnNode(Opcodes.ISTORE, relLocal));
            return relLocal;
        }

        // int rel = fieldNumber - pcInheritedFieldCount
        instructions.add(new VarInsnNode(Opcodes.ILOAD, fieldNumber));
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
        instructions.add(new InsnNode(Opcodes.ISUB));
        instructions.add(new VarInsnNode(Opcodes.ISTORE, relLocal));

        // super: if (rel < 0) super.pcReplaceField (fieldNumber); return;
        // no super: if (rel < 0) throw new IllegalArgumentException ();
        LabelNode afterRelCheck = new LabelNode();

        instructions.add(new VarInsnNode(Opcodes.ILOAD, relLocal));
        instructions.add(new JumpInsnNode(Opcodes.IFGE, afterRelCheck));
        if (_meta.getPCSuperclass() != null) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            final Class pcSuperClass = getType(_meta.getPCSuperclassMetaData());
            String mDesc = copy
                    ? Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(pcSuperClass), Type.INT_TYPE)
                    : Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE);
            if (copy) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // the instance to copy into
            }
            instructions.add(new VarInsnNode(Opcodes.ILOAD, fieldNumber));
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(pcSuperClass),
                                                name,
                                                mDesc));
            instructions.add(new InsnNode(Opcodes.RETURN));
        }
        else {
            instructions.add(throwException(IllegalArgumentException.class));
        }
        instructions.add(afterRelCheck);

        return relLocal;
    }

    /**
     * This helper method, given the pcReplaceField or pcProvideField
     * method, adds the bytecode for the corresponding 'plural' version
     * of the method -- the version that takes an int[] of fields to
     * access rather than a single field. The multiple fields version
     * simply loops through the provided indexes and delegates to the
     * singular version for each one.
     */
    private void addMultipleFieldsMethodVersion(ClassNode classNode, MethodNode single, boolean copy) {
        String desc = copy
                ? Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, Type.getType(int[].class))
                : Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(int[].class));

        MethodNode multiMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                              single.name + "s",
                                              desc,
                                              null, null);
        final InsnList instructions = multiMeth.instructions;
        classNode.methods.add(multiMeth);

        int instVarPos = 0;
        if (copy) {
            instVarPos = 3;
            if (getCreateSubclass()) {
                // get the managed instance into the local variable table

                // (EntityType)ImplHelper.getManagedInstance(other_param1) to Stack
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // other instance
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                    Type.getInternalName(ImplHelper.class),
                                                    "getManagedInstance",
                                                    Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT)));
                instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, managedType.getClassNode().name));
                instructions.add(new VarInsnNode(Opcodes.ASTORE, instVarPos));

                // there might be a difference between the classes of 'this'
                // vs 'other' in this context; use the PC methods to get the SM
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // other_param1 object

                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                    Type.getInternalName(ImplHelper.class),
                                                    "toPersistenceCapable",
                                                    Type.getMethodDescriptor(Type.getType(PersistenceCapable.class),
                                                                             TYPE_OBJECT,
                                                                             TYPE_OBJECT)));

                // now we get the StateManager from the other instance
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(PersistenceCapable.class),
                                                    "pcGetStateManager",
                                                    Type.getMethodDescriptor(Type.getType(StateManager.class))));
            }
            else {
                // XXX other = (XXX) pc;
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // other object
                instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, pc.getClassNode().name));
                instructions.add(new VarInsnNode(Opcodes.ASTORE, instVarPos));

                // access the other's sm field directly
                instructions.add(new VarInsnNode(Opcodes.ALOAD, instVarPos));
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
            }

            // if (other.pcStateManager != pcStateManager)
            //    throw new IllegalArgumentException
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
            LabelNode toEndSmCmp = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IF_ACMPEQ, toEndSmCmp));
            instructions.add(throwException(IllegalArgumentException.class));
            instructions.add(toEndSmCmp);

            // if (pcStateManager == null)
            //  throw new IllegalStateException
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
            LabelNode toEndSmNull = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, toEndSmNull));
            instructions.add(throwException(IllegalStateException.class));
            instructions.add(toEndSmNull);
        }

        // for (int i = 0;
        int iVarPos = copy ? 4 : 2;
        instructions.add(new InsnNode(Opcodes.ICONST_0));
        instructions.add(new VarInsnNode(Opcodes.ISTORE, iVarPos));
        LabelNode toI = new LabelNode();
        instructions.add(toI);

        int fieldNumbersPos = copy ? 2 : 1;

        instructions.add(new VarInsnNode(Opcodes.ILOAD, iVarPos));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, fieldNumbersPos)); // the int[]
        instructions.add(new InsnNode(Opcodes.ARRAYLENGTH)); // int[] parameter variable.length
        LabelNode toEnd = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGE, toEnd)); // if i >= int[].length

        // otherwise call the single method
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));        // this
        if (copy) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, instVarPos));  // instance
        }
        instructions.add(new VarInsnNode(Opcodes.ALOAD, fieldNumbersPos));        // the int[] param
        instructions.add(new VarInsnNode(Opcodes.ILOAD, iVarPos));  // int[ i ]
        instructions.add(new InsnNode(Opcodes.IALOAD));             // load the value at that position

        // now invoke the single method
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            classNode.name,
                                            single.name,
                                            single.desc));

        instructions.add(new IincInsnNode(iVarPos, 1));
        instructions.add(new JumpInsnNode(Opcodes.GOTO, toI));

        instructions.add(toEnd);        // end of loop

        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Adds the 'stock' methods to the bytecode; these include methods
     * like {@link PersistenceCapable#pcFetchObjectId}
     * and {@link PersistenceCapable#pcIsTransactional}.
     */
    private void addStockMethods() throws NoSuchMethodException {
        // pcGetGenericContext
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("get" + CONTEXTNAME), false);

        // pcFetchObjectId
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("fetchObjectId"), false);

        // pcIsDeleted
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("isDeleted"), false);

        // pcIsDirty
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("isDirty"), true);

        // pcIsNew
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("isNew"), false);

        // pcIsPersistent
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("isPersistent"), false);

        // pcIsTransactional
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("isTransactional"), false);

        // pcSerializing
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("serializing"), false);

        // pcDirty
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("dirty", String.class), false);

        // pcGetStateManager
        MethodNode getSmMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                              PRE + "GetStateManager",
                                              Type.getMethodDescriptor(Type.getType(SMTYPE)),
                                              null, null);
        pc.getClassNode().methods.add(getSmMeth);

        InsnList instructions = getSmMeth.instructions;

        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, pc.getClassNode().name, SM, Type.getDescriptor(SMTYPE)));
        instructions.add(new InsnNode(Opcodes.ARETURN));
    }


    /**
     * Helper method to add a stock method to the bytecode. Each
     * stock method simply delegates to a corresponding StateManager method.
     * Given the StateManager method, then, this function translates it into
     * the wrapper method that should be added to the bytecode.
     */
    private void translateFromStateManagerMethod(Method m, boolean isDirtyCheckMethod) {
        // form the name of the method by prepending 'pc' to the sm method
        String name = PRE + StringUtil.capitalize(m.getName());
        Class[] params = m.getParameterTypes();
        Type[] paramTypes = Arrays.stream(params)
                .map(Type::getType)
                .toArray(Type[]::new);
        Class returnType = m.getReturnType();

        final ClassNode classNode = pc.getClassNode();

        // add the method to the pc
        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC,
                                               name,
                                               Type.getMethodDescriptor(Type.getType(returnType), paramTypes),
                                               null, null);
        InsnList instructions = methodNode.instructions;
        classNode.methods.add(methodNode);

        // if (pcStateManager == null) return <default>;
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        LabelNode lblAfterIf = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblAfterIf));
        if (returnType.equals(boolean.class)) {
            instructions.add(new InsnNode(Opcodes.ICONST_0)); // false
        }
        else if (!returnType.equals(void.class)) {
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        instructions.add(new InsnNode(AsmHelper.getReturnInsn(returnType)));
        instructions.add(lblAfterIf);

        // load the StateManager onto the stack
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        // if this is the dirty-check method and we're subclassing but not
        // redefining, hook into PCHelper to do the dirty check
        if (isDirtyCheckMethod && !getRedefine()) {
            // RedefinitionHelper.dirtyCheck(sm);
            instructions.add(new InsnNode(Opcodes.DUP)); // duplicate the StateManager for the return statement below
            instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                Type.getInternalName(RedefinitionHelper.class),
                                                "dirtyCheck",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(SMTYPE))));
        }


        // return pcStateManager.<method> (<args>);
        // managed instance loaded above in if-else block
        for (int i = 0; i < params.length; i++) {
            instructions.add(new VarInsnNode(AsmHelper.getLoadInsn(params[i]), i + 1));
        }
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(SMTYPE),
                                            m.getName(),
                                            Type.getMethodDescriptor(m)));

        instructions.add(new InsnNode(AsmHelper.getReturnInsn(returnType)));
    }

    /**
     * Adds the {@link PersistenceCapable#pcGetVersion} method to the bytecode.
     */
    private void addGetVersionMethod() throws NoSuchMethodException {
        final ClassNode classNode = pc.getClassNode();
        MethodNode getVersionMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                   PRE + "GetVersion",
                                                   Type.getMethodDescriptor(TYPE_OBJECT),
                                                   null, null);
        classNode.methods.add(getVersionMeth);
        InsnList instructions = getVersionMeth.instructions;

        // if (pcStateManager == null)
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
        LabelNode lblAfterIf = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblAfterIf));

        FieldMetaData versionField = _meta.getVersionField();
        if (versionField == null) {
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        else {
            // return <versionField>;
            Class wrapper = toPrimitiveWrapper(versionField);
            if (wrapper != versionField.getDeclaredType()) {
                instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(wrapper)));
                instructions.add(new InsnNode(Opcodes.DUP));
            }
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            addGetManagedValueCode(classNode, instructions, versionField, true);
            if (wrapper != versionField.getDeclaredType()) {
                instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                    Type.getInternalName(wrapper),
                                                    "<init>",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(versionField.getDeclaredType()))));
            }
        }
        instructions.add(new InsnNode(Opcodes.ARETURN));
        instructions.add(lblAfterIf);

        // return pcStateManager.getVersion ();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(SMTYPE),
                                            "getVersion",
                                            Type.getMethodDescriptor(TYPE_OBJECT)));

        instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    /**
     * Return the version field type as a primitive wrapper, or null if
     * the version field is not primitive.
     */
    private Class toPrimitiveWrapper(FieldMetaData fmd) {
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
                return Boolean.class;
            case JavaTypes.BYTE:
                return Byte.class;
            case JavaTypes.CHAR:
                return Character.class;
            case JavaTypes.DOUBLE:
                return Double.class;
            case JavaTypes.FLOAT:
                return Float.class;
            case JavaTypes.INT:
                return Integer.class;
            case JavaTypes.LONG:
                return Long.class;
            case JavaTypes.SHORT:
                return Short.class;
        }
        return fmd.getDeclaredType();
    }

    /**
     * Adds the {@link PersistenceCapable#pcReplaceStateManager}
     * method to the bytecode.
     */
    private void addReplaceStateManagerMethod() {
        // public void pcReplaceStateManager (StateManager sm)
        MethodNode replaceSmMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                  PRE + "ReplaceStateManager",
                                                  Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(SMTYPE)),
                                                  null, new String[]{Type.getInternalName(SecurityException.class)});
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(replaceSmMeth);
        InsnList instructions = replaceSmMeth.instructions;

        // if (pcStateManager != null)
        //    pcStateManager = pcStateManager.replaceStateManager(sm);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        LabelNode lblEndIfNull = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblEndIfNull));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this

        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st method param, the new StateManager
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(SMTYPE),
                                            "replaceStateManager",
                                            Type.getMethodDescriptor(Type.getType(SMTYPE), Type.getType(SMTYPE))));
        instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
        instructions.add(new InsnNode(Opcodes.RETURN));

        instructions.add(lblEndIfNull);

        // pcStateManager = sm;
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st method param, the new StateManager
        instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Creates the PersistenceCapable methods dealing with application
     * identity and gives them no-op implementations.
     */
    private void addNoOpApplicationIdentityMethods() {
        ClassNode classNode = pc.getClassNode();
        {
            // public void pcCopyKeyFieldsToObjectId (ObjectIdFieldSupplier fs, Object oid)
            MethodNode copyKeyMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                    PRE + "CopyKeyFieldsToObjectId",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OIDFSTYPE), TYPE_OBJECT),
                                                    null, null);
            classNode.methods.add(copyKeyMeth);
            copyKeyMeth.instructions.add(new InsnNode(Opcodes.RETURN));
        }

        {
            // public void pcCopyKeyFieldsToObjectId (Object oid)
            MethodNode copyKeyMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                    PRE + "CopyKeyFieldsToObjectId",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT),
                                                    null, null);
            classNode.methods.add(copyKeyMeth);
            copyKeyMeth.instructions.add(new InsnNode(Opcodes.RETURN));
        }

        {
            // public void pcCopyKeyFieldsFromObjectId (ObjectIdFieldConsumer fc, Object oid)
            MethodNode copyKeyMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                    PRE + "CopyKeyFieldsFromObjectId",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OIDFCTYPE), TYPE_OBJECT),
                                                    null, null);
            classNode.methods.add(copyKeyMeth);
            copyKeyMeth.instructions.add(new InsnNode(Opcodes.RETURN));
        }

        {
            // public void pcCopyKeyFieldsFromObjectId (Object oid)
            MethodNode copyKeyMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                    PRE + "CopyKeyFieldsFromObjectId",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT),
                                                    null, null);
            classNode.methods.add(copyKeyMeth);
            copyKeyMeth.instructions.add(new InsnNode(Opcodes.RETURN));
        }

        {
            // public Object pcNewObjectIdInstance ()
            MethodNode copyKeyMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                    PRE + "NewObjectIdInstance",
                                                    Type.getMethodDescriptor(TYPE_OBJECT),
                                                    null, null);
            classNode.methods.add(copyKeyMeth);
            copyKeyMeth.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            copyKeyMeth.instructions.add(new InsnNode(Opcodes.ARETURN));
        }

        {
            // public Object pcNewObjectIdInstance (Object obj)
            MethodNode copyKeyMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                    PRE + "NewObjectIdInstance",
                                                    Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT),
                                                    null, null);
            classNode.methods.add(copyKeyMeth);
            copyKeyMeth.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            copyKeyMeth.instructions.add(new InsnNode(Opcodes.ARETURN));

        }
    }

    /**
     * Adds the <code>pcCopyKeyFieldsToObjectId</code> methods
     * to classes using application identity.
     */
    private void addCopyKeyFieldsToObjectIdMethod(boolean fieldManager) throws NoSuchMethodException {

        // public void pcCopyKeyFieldsToObjectId (ObjectIdFieldSupplier fs, Object oid)
        String mDesc = fieldManager
                ? Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OIDFSTYPE), TYPE_OBJECT)
                : Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT);
        MethodNode copyKFMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                               PRE + "CopyKeyFieldsToObjectId",
                                               mDesc,
                                               null, null);
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(copyKFMeth);
        InsnList instructions = copyKFMeth.instructions;

        // single field identity always throws exception
        if (_meta.isOpenJPAIdentity()) {
            instructions.add(throwException(INTERNEXCEP));
            return;
        }

        // call superclass method
        if (_meta.getPCSuperclass() != null && !getCreateSubclass()) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter object
            if (fieldManager) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd parameter object
            }
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(getType(_meta.getPCSuperclassMetaData())),
                                                PRE + "CopyKeyFieldsToObjectId",
                                                mDesc));
        }

        // Object id = oid;
        if (fieldManager) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd parameter object
        }
        else {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter object
        }

        if (_meta.isObjectIdTypeShared()) {
            // oid = ((ObjectId) id).getId ();
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(ObjectId.class)));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(ObjectId.class),
                                                "getId",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));
        }

        // <oid type> id = (<oid type>) oid;
        int nextFreeVarPos = (fieldManager) ? 3 : 2;
        int idVarPos = nextFreeVarPos++;

        Class oidType = _meta.getObjectIdType();
        instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(oidType)));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, idVarPos));

        // int inherited = pcInheritedFieldCount;
        int inherited = 0;
        if (fieldManager) {
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
            inherited = nextFreeVarPos++;
            instructions.add(new VarInsnNode(Opcodes.ISTORE, inherited));
        }

        // id.<field> = fs.fetch<type>Field (<index>); or...
        // id.<field> = pc.<field>;
        FieldMetaData[] fmds = getCreateSubclass() ? _meta.getFields()
                : _meta.getDeclaredFields();

        // If optimizeIdCopy is enabled and not a field manager method, try to
        // optimize the copyTo by using a public constructor instead of reflection
        if (_optimizeIdCopy) {
            ArrayList<Integer> pkfields = optimizeIdCopy(oidType, fmds);
            if (pkfields != null) {
                // search for a constructor on the IdClass that can be used
                // to construct the IdClass
                int[] parmOrder = getIdClassConstructorParmOrder(oidType, pkfields, fmds);
                if (parmOrder != null) {
                    // If using a field manager, values must be loaded into locals so they can be properly ordered
                    // as constructor parameters.
                    int[] localIndexes = new int[fmds.length];

                    if (fieldManager) {
                        for (int k = 0; k < fmds.length; k++) {
                            if (!fmds[k].isPrimaryKey()) {
                                continue;
                            }
                            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
                            instructions.add(AsmHelper.getLoadConstantInsn(k));
                            instructions.add(new VarInsnNode(Opcodes.ILOAD, inherited));
                            instructions.add(new InsnNode(Opcodes.IADD));

                            final Method fieldSupplierMethod = getFieldSupplierMethod(fmds[k].getObjectIdFieldType());
                            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                                Type.getInternalName(fieldSupplierMethod.getDeclaringClass()),
                                                                fieldSupplierMethod.getName(),
                                                                Type.getMethodDescriptor(fieldSupplierMethod)));
                            localIndexes[k] = nextFreeVarPos++;
                            instructions.add(new VarInsnNode(AsmHelper.getStoreInsn(fmds[k].getObjectIdFieldType()), localIndexes[k]));
                        }
                    }

                    // found a matching constructor.  parm array is constructor parm order
                    instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(oidType)));
                    instructions.add(new InsnNode(Opcodes.DUP));

                    // build the parm list in order
                    Class<?>[] clsArgs = new Class<?>[parmOrder.length];
                    for (int i = 0; i < clsArgs.length; i++) {
                        int parmIndex = parmOrder[i];
                        clsArgs[i] = fmds[parmIndex].getObjectIdFieldType();
                        if (!fieldManager) {
                            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                            addGetManagedValueCode(classNode, instructions, fmds[parmIndex], true);
                        }
                        else {
                            // Load constructor parameters in appropriate order
                            instructions.add(new VarInsnNode(AsmHelper.getLoadInsn(fmds[parmIndex].getObjectIdFieldType()), localIndexes[parmIndex]));

                            if (fmds[parmIndex].getObjectIdFieldTypeCode() == JavaTypes.OBJECT &&
                                    !fmds[parmIndex].getDeclaredType().isEnum()) {
                                instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(ObjectId.class)));
                                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                                    Type.getInternalName(ObjectId.class),
                                                                    "getId",
                                                                    Type.getMethodDescriptor(TYPE_OBJECT)));
                            }

                            // if the type of this field meta data is
                            // non-primitive and non-string, be sure to cast
                            // to the appropriate type.
                            if (!clsArgs[i].isPrimitive() && !clsArgs[i].getName().equals(String.class.getName())) {
                                instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(clsArgs[i])));
                            }
                        }
                    }

                    // invoke the public constructor to create a new local id
                    Type[] parms = Arrays.stream(clsArgs)
                            .map(Type::getType)
                            .toArray(Type[]::new);

                    instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                        Type.getInternalName(oidType),
                                                        "<init>",
                                                        Type.getMethodDescriptor(Type.VOID_TYPE, parms)));


                    int retVarPos = inherited + fmds.length;
                    instructions.add(new VarInsnNode(Opcodes.ASTORE, retVarPos));

                    // swap out the app id with the new one
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, fieldManager ? 2 : 1));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(ObjectId.class)));
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, retVarPos));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                        Type.getInternalName(ApplicationIds.class),
                                                        "setAppId",
                                                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectId.class), TYPE_OBJECT)));
                    instructions.add(new InsnNode(Opcodes.RETURN));
                    return;
                }
            }
        }

        Field field = null;
        Method setter = null;
        for (int i = 0; i < fmds.length; i++) {
            if (!fmds[i].isPrimaryKey()) {
                continue;
            }

            instructions.add(new VarInsnNode(Opcodes.ALOAD, idVarPos));

            String name = fmds[i].getName();
            Class<?> type = fmds[i].getObjectIdFieldType();
            boolean reflect = false;

            if (isFieldAccess(fmds[i])) {
                field = Reflection.findField(oidType, name, true);
                reflect = !Modifier.isPublic(field.getModifiers());
                if (reflect) {
                    instructions.add(AsmHelper.getLoadConstantInsn(oidType));
                    instructions.add(AsmHelper.getLoadConstantInsn(name));
                    instructions.add(AsmHelper.getLoadConstantInsn(true));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                        Type.getInternalName(Reflection.class),
                                                        "findField",
                                                        Type.getMethodDescriptor(Type.getType(Field.class), Type.getType(Class.class),
                                                                                 Type.getType(String.class), Type.BOOLEAN_TYPE)));
                }
            }
            else {
                setter = Reflection.findSetter(oidType, name, type, true);
                reflect = !Modifier.isPublic(setter.getModifiers());
                if (reflect) {
                    instructions.add(AsmHelper.getLoadConstantInsn(oidType));
                    instructions.add(AsmHelper.getLoadConstantInsn(name));
                    instructions.add(AsmHelper.getLoadConstantInsn(type));
                    instructions.add(AsmHelper.getLoadConstantInsn(true));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                        Type.getInternalName(Reflection.class),
                                                        "findSetter",
                                                        Type.getMethodDescriptor(Type.getType(Method.class),
                                                                                 Type.getType(Class.class),
                                                                                 Type.getType(String.class),
                                                                                 Type.getType(Class.class),
                                                                                 Type.BOOLEAN_TYPE)));
                }
            }

            if (fieldManager) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));  // 1st param
                instructions.add(AsmHelper.getLoadConstantInsn(i));
                instructions.add(new VarInsnNode(Opcodes.ILOAD, inherited));
                instructions.add(new InsnNode(Opcodes.IADD));

                final Method fieldSupplierMethod = getFieldSupplierMethod(type);
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(fieldSupplierMethod.getDeclaringClass()),
                                                    fieldSupplierMethod.getName(),
                                                    Type.getMethodDescriptor(fieldSupplierMethod)));


                if (fmds[i].getObjectIdFieldTypeCode() == JavaTypes.OBJECT && !fmds[i].getDeclaredType().isEnum()) {
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(ObjectId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(ObjectId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(TYPE_OBJECT)));
                }

                // if the type of this field meta data is
                // non-primitive and non-string, be sure to cast
                // to the appropriate type.
                if (!reflect && !type.isPrimitive() && !type.getName().equals(String.class.getName())) {
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(type)));
                }
            }
            else {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                addGetManagedValueCode(classNode, instructions, fmds[i], true);

                // get id/pk from pc instance
                if (fmds[i].getDeclaredTypeCode() == JavaTypes.PC) {
                    addExtractObjectIdFieldValueCode(classNode, instructions, fmds[i], nextFreeVarPos++);
                }
            }

            if (reflect && field != null) {
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                    Type.getInternalName(Reflection.class),
                                                    "set",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, Type.getType(Field.class),
                                                                             (type.isPrimitive()) ? Type.getType(type) : TYPE_OBJECT)));

            }
            else if (reflect) {
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                    Type.getInternalName(Reflection.class),
                                                    "set",
                                                    Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, Type.getType(Method.class),
                                                                             (type.isPrimitive()) ? Type.getType(type) : TYPE_OBJECT)));
            }
            else if (field != null) {
                instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,
                                                   Type.getInternalName(field.getDeclaringClass()),
                                                   field.getName(),
                                                   Type.getDescriptor(field.getType())));
            }
            else {
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                    Type.getInternalName(setter.getDeclaringClass()),
                                                    setter.getName(),
                                                    Type.getMethodDescriptor(setter)));
            }

        }
        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Add code to extract the id of the given primary key relation field for
     * setting into an objectid instance.
     */
    private void addExtractObjectIdFieldValueCode(ClassNode classNode, InsnList instructions, FieldMetaData pk, int nextFreeVarPos) {
        // if (val != null) {
        int pcVarPos = nextFreeVarPos++;
        instructions.add(new VarInsnNode(Opcodes.ASTORE, pcVarPos));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, pcVarPos));

        LabelNode lblAfterIfNull = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblAfterIfNull));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, pcVarPos));
        instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(PersistenceCapable.class)));

        //  val = ((PersistenceCapable) val).pcFetchObjectId(); or pcNewObjectIdInstance()
        if (!pk.getTypeMetaData().isOpenJPAIdentity()) {
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(PersistenceCapable.class),
                                                PRE + "FetchObjectId",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));
        }
        else {
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(PersistenceCapable.class),
                                                PRE + "NewObjectIdInstance",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));
        }

        int oidVarPos = nextFreeVarPos++;
        instructions.add(new VarInsnNode(Opcodes.ASTORE, oidVarPos));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));

        LabelNode lblAfterIfNull2 = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblAfterIfNull2));

        // for datastore / single-field identity:
        // if (val != null)
        //   val = ((OpenJPAId) val).getId();
        ClassMetaData pkmeta = pk.getDeclaredTypeMetaData();
        int pkcode = pk.getObjectIdFieldTypeCode();
        Class pktype = pk.getObjectIdFieldType();
        if (pkmeta.getIdentityType() == ClassMetaData.ID_DATASTORE && pkcode == JavaTypes.LONG) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(Id.class)));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(Id.class),
                                                "getId",
                                                Type.getMethodDescriptor(Type.LONG_TYPE)));
        }
        else if (pkmeta.getIdentityType() == ClassMetaData.ID_DATASTORE) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
        }
        else if (pkmeta.isOpenJPAIdentity()) {
            switch (pkcode) {
                case JavaTypes.BYTE_OBJ:
                    instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(Byte.class)));
                    instructions.add(new InsnNode(Opcodes.DUP));
                    // no break
                case JavaTypes.BYTE:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(ByteId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(ByteId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.BYTE_TYPE)));
                    if (pkcode == JavaTypes.BYTE_OBJ) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                            Type.getInternalName(Byte.class),
                                                            "<init>",
                                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.BYTE_TYPE)));
                    }
                    break;
                case JavaTypes.CHAR_OBJ:
                    instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(Character.class)));
                    instructions.add(new InsnNode(Opcodes.DUP));
                    // no break
                case JavaTypes.CHAR:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(CharId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(CharId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.CHAR_TYPE)));
                    if (pkcode == JavaTypes.CHAR_OBJ) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                            Type.getInternalName(Character.class),
                                                            "<init>",
                                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.CHAR_TYPE)));
                    }
                    break;
                case JavaTypes.DOUBLE_OBJ:
                    instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(Double.class)));
                    instructions.add(new InsnNode(Opcodes.DUP));
                    // no break
                case JavaTypes.DOUBLE:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(DoubleId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(DoubleId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.DOUBLE_TYPE)));
                    if (pkcode == JavaTypes.DOUBLE_OBJ) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                            Type.getInternalName(Character.class),
                                                            "<init>",
                                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.CHAR_TYPE)));
                    }
                    break;
                case JavaTypes.FLOAT_OBJ:
                    instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(Float.class)));
                    instructions.add(new InsnNode(Opcodes.DUP));
                    // no break
                case JavaTypes.FLOAT:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(FloatId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(FloatId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.FLOAT_TYPE)));
                    if (pkcode == JavaTypes.FLOAT_OBJ) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                            Type.getInternalName(Float.class),
                                                            "<init>",
                                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE)));
                    }
                    break;
                case JavaTypes.INT_OBJ:
                    instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(Integer.class)));
                    instructions.add(new InsnNode(Opcodes.DUP));
                    // no break
                case JavaTypes.INT:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(IntId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(IntId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.INT_TYPE)));
                    if (pkcode == JavaTypes.INT_OBJ) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                            Type.getInternalName(Integer.class),
                                                            "<init>",
                                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));
                    }
                    break;
                case JavaTypes.LONG_OBJ:
                    instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(Long.class)));
                    instructions.add(new InsnNode(Opcodes.DUP));
                    // no break
                case JavaTypes.LONG:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(LongId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(LongId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.LONG_TYPE)));
                    if (pkcode == JavaTypes.LONG_OBJ) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                            Type.getInternalName(Long.class),
                                                            "<init>",
                                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE)));
                    }
                    break;
                case JavaTypes.SHORT_OBJ:
                    instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(Short.class)));
                    instructions.add(new InsnNode(Opcodes.DUP));
                    // no break
                case JavaTypes.SHORT:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(ShortId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(ShortId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.SHORT_TYPE)));
                    if (pkcode == JavaTypes.SHORT_OBJ) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                            Type.getInternalName(Short.class),
                                                            "<init>",
                                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.SHORT_TYPE)));
                    }
                    break;
                case JavaTypes.DATE:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(DateId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(DateId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.getType(Date.class))));
                    if (pktype != Date.class) {
                        // java.sql.Date.class
                        instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(pktype)));
                    }
                    break;
                case JavaTypes.STRING:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(StringId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(StringId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.getType(String.class))));
                    break;
                case JavaTypes.BIGDECIMAL:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(BigDecimalId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(BigDecimalId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.getType(BigDecimal.class))));
                    break;
                case JavaTypes.BIGINTEGER:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(BigIntegerId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(BigIntegerId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.getType(BigInteger.class))));
                    break;
                default:
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
                    instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(ObjectId.class)));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(ObjectId.class),
                                                        "getId",
                                                        Type.getMethodDescriptor(Type.getType(Object.class))));
            }
        }
        else if (pkmeta.getObjectIdType() != null) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
            if (pkcode == JavaTypes.OBJECT) {
                instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(ObjectId.class)));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                    Type.getInternalName(ObjectId.class),
                                                    "getId",
                                                    Type.getMethodDescriptor(TYPE_OBJECT)));
            }
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(pktype)));
        }
        else {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, oidVarPos));
        }

        // jump from here to the end
        LabelNode lblGo2End = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.GOTO, lblGo2End));

        // elses from above to define the defaults
        instructions.add(lblAfterIfNull);
        instructions.add(lblAfterIfNull2);

        switch (pkcode) {
            case JavaTypes.BOOLEAN:
                instructions.add(AsmHelper.getLoadConstantInsn(false));
                break;
            case JavaTypes.BYTE:
                instructions.add(AsmHelper.getLoadConstantInsn(0));
                break;
            case JavaTypes.CHAR:
                instructions.add(AsmHelper.getLoadConstantInsn(0));
                break;
            case JavaTypes.DOUBLE:
                instructions.add(AsmHelper.getLoadConstantInsn(0D));
                break;
            case JavaTypes.FLOAT:
                instructions.add(AsmHelper.getLoadConstantInsn(0F));
                break;
            case JavaTypes.INT:
                instructions.add(AsmHelper.getLoadConstantInsn(0));
                break;
            case JavaTypes.LONG:
                instructions.add(AsmHelper.getLoadConstantInsn(0L));
                break;
            case JavaTypes.SHORT:
                instructions.add(AsmHelper.getLoadConstantInsn((short) 0));
                break;
            default:
                instructions.add(AsmHelper.getLoadConstantInsn(null));
        }


        instructions.add(lblGo2End);
    }

    /**
     * Adds the <code>pcCopyKeyFieldsFromObjectId</code> methods
     * to classes using application identity.
     */
    private void addCopyKeyFieldsFromObjectIdMethod(boolean fieldManager) throws NoSuchMethodException {
        // public void pcCopyKeyFieldsFromObjectId (ObjectIdFieldConsumer fc, Object oid)
        String mDesc = fieldManager
                ? Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(OIDFCTYPE), TYPE_OBJECT)
                : Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT);
        MethodNode copyKFMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                               PRE + "CopyKeyFieldsFromObjectId",
                                               mDesc,
                                               null, null);
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(copyKFMeth);
        InsnList instructions = copyKFMeth.instructions;


        // call superclass method
        if (_meta.getPCSuperclass() != null && !getCreateSubclass()) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter object
            if (fieldManager) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd parameter object
            }
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(getType(_meta.getPCSuperclassMetaData())),
                                                PRE + "CopyKeyFieldsFromObjectId",
                                                mDesc));
        }

        // Object id = oid;
        if (fieldManager) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 2)); // 2nd parameter object
        }
        else {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter object
        }

        if (!_meta.isOpenJPAIdentity() && _meta.isObjectIdTypeShared()) {
            // oid = ((ObjectId) id).getId ();
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(ObjectId.class)));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(ObjectId.class),
                                                "getId",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));
        }

        // <oid type> id = (<oid type>) oid;
        int nextFreeVarPos = (fieldManager) ? 3 : 2;
        int idVarPos = nextFreeVarPos++;

        Class oidType = _meta.getObjectIdType();
        instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(oidType)));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, idVarPos));

        // fs.store<type>Field (<index>, id.<field>); or...
        // this.<field> = id.<field>
        // or for single field identity: id.getId ()
        FieldMetaData[] fmds = getCreateSubclass() ? _meta.getFields()
                : _meta.getDeclaredFields();
        for (int i = 0; i < fmds.length; i++) {
            if (!fmds[i].isPrimaryKey()) {
                continue;
            }

            String name = fmds[i].getName();
            Class<?> type = fmds[i].getObjectIdFieldType();

            if (!fieldManager && fmds[i].getDeclaredTypeCode() == JavaTypes.PC) {
                // if (sm == null) return;
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

                LabelNode lblEndIfNotNull = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblEndIfNotNull));
                instructions.add(new InsnNode(Opcodes.RETURN));

                instructions.add(lblEndIfNotNull);

                // sm.getPCPrimaryKey(oid, i + pcInheritedFieldCount);
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new InsnNode(Opcodes.DUP)); // leave orig on stack to set value into
                instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
                instructions.add(new VarInsnNode(Opcodes.ALOAD, idVarPos));
                instructions.add(AsmHelper.getLoadConstantInsn(i));
                instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
                instructions.add(new InsnNode(Opcodes.IADD));
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(SMTYPE),
                                                    "getPCPrimaryKey",
                                                    Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT, Type.INT_TYPE)));
                instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(fmds[i].getDeclaredType())));
            }
            else {
                Class<?> unwrapped = (fmds[i].getDeclaredTypeCode() == JavaTypes.PC) ? type : unwrapSingleFieldIdentity(fmds[i]);
                if (fieldManager) {
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
                    instructions.add(AsmHelper.getLoadConstantInsn(i));
                    instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
                    instructions.add(new InsnNode(Opcodes.IADD));
                }
                else {
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                }

                if (unwrapped != type) {
                    instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(type)));
                    instructions.add(new InsnNode(Opcodes.DUP));
                }

                instructions.add(new VarInsnNode(Opcodes.ALOAD, idVarPos));
                if (_meta.isOpenJPAIdentity()) {
                    if (oidType == ObjectId.class) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                            Type.getInternalName(oidType),
                                                            "getId",
                                                            Type.getMethodDescriptor(TYPE_OBJECT)));
                        if (!fieldManager && type != Object.class) {
                            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(fmds[i].getDeclaredType())));
                        }
                    }
                    else if (oidType == DateId.class) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                            Type.getInternalName(oidType),
                                                            "getId",
                                                            Type.getMethodDescriptor(Type.getType(Date.class))));
                        if (!fieldManager && type != Date.class) {
                            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(fmds[i].getDeclaredType())));
                        }
                    }
                    else {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                            Type.getInternalName(oidType),
                                                            "getId",
                                                            Type.getMethodDescriptor(Type.getType(unwrapped))));
                        if (unwrapped != type) {
                            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                                Type.getInternalName(type),
                                                                "<init>",
                                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(unwrapped))));
                        }
                    }
                }
                else if (isFieldAccess(fmds[i])) {
                    Field field = Reflection.findField(oidType, name, true);
                    if (Modifier.isPublic(field.getModifiers())) {
                        instructions.add(new FieldInsnNode(Opcodes.GETFIELD,
                                                           Type.getInternalName(field.getDeclaringClass()),
                                                           field.getName(),
                                                           Type.getDescriptor(field.getType())));
                    }
                    else {
                        boolean usedFastOid = false;
                        if (_optimizeIdCopy) {
                            // If fastOids, ignore access type and try to use a public getter
                            Method getter = Reflection.findGetter(oidType, name, false);
                            if (getter != null && Modifier.isPublic(getter.getModifiers())) {
                                usedFastOid = true;
                                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                                    Type.getInternalName(getter.getDeclaringClass()),
                                                                    getter.getName(),
                                                                    Type.getMethodDescriptor(getter)));
                            }
                        }
                        if (!usedFastOid) {
                            // Reflection.getXXX(oid, Reflection.findField(...));
                            instructions.add(AsmHelper.getLoadConstantInsn(oidType));
                            instructions.add(AsmHelper.getLoadConstantInsn(name));
                            instructions.add(AsmHelper.getLoadConstantInsn(true));
                            instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                                Type.getInternalName(Reflection.class),
                                                                "findField",
                                                                Type.getMethodDescriptor(Type.getType(Field.class), Type.getType(Class.class),
                                                                                         Type.getType(String.class), Type.BOOLEAN_TYPE)));

                            final Method reflectionGetterMethod = getReflectionGetterMethod(type, Field.class);
                            instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                                Type.getInternalName(reflectionGetterMethod.getDeclaringClass()),
                                                                reflectionGetterMethod.getName(),
                                                                Type.getMethodDescriptor(reflectionGetterMethod)));
                            if (!type.isPrimitive() && type != Object.class) {
                                instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(type)));
                            }
                        }
                    }
                }
                else {
                    Method getter = Reflection.findGetter(oidType, name, true);
                    if (Modifier.isPublic(getter.getModifiers())) {
                        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                            Type.getInternalName(getter.getDeclaringClass()),
                                                            getter.getName(),
                                                            Type.getMethodDescriptor(getter)));
                    }
                    else {
                        // Reflection.getXXX(oid, Reflection.findGetter(...));
                        instructions.add(AsmHelper.getLoadConstantInsn(oidType));
                        instructions.add(AsmHelper.getLoadConstantInsn(name));
                        instructions.add(AsmHelper.getLoadConstantInsn(true));
                        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                            Type.getInternalName(Reflection.class),
                                                            "findGetter",
                                                            Type.getMethodDescriptor(Type.getType(Method.class), Type.getType(Class.class),
                                                                                     Type.getType(String.class), Type.BOOLEAN_TYPE)));

                        final Method reflectionGetterMethod = getReflectionGetterMethod(type, Method.class);
                        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                            Type.getInternalName(reflectionGetterMethod.getDeclaringClass()),
                                                            reflectionGetterMethod.getName(),
                                                            Type.getMethodDescriptor(reflectionGetterMethod)));
                        if (!type.isPrimitive() && type != Object.class) {
                            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(type)));
                        }
                    }
                }
            }

            if (fieldManager) {
                final Method fieldConsumerMethod = getFieldConsumerMethod(type);
                instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                    Type.getInternalName(fieldConsumerMethod.getDeclaringClass()),
                                                    fieldConsumerMethod.getName(),
                                                    Type.getMethodDescriptor(fieldConsumerMethod)));
            }
            else {
                addSetManagedValueCode(classNode, instructions, fmds[i]);
            }
        }

        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Return if the class uses the Class/String constructor
     * instead of just String.
     */
    private Boolean usesClassStringIdConstructor() {
        if (_meta.getIdentityType() != ClassMetaData.ID_APPLICATION) {
            return Boolean.FALSE;
        }

        if (_meta.isOpenJPAIdentity()) {
            if (_meta.getObjectIdType() == ObjectId.class) {
                return null;
            }
            return Boolean.TRUE;
        }

        Class oidType = _meta.getObjectIdType();
        try {
            oidType.getConstructor(new Class[]{Class.class, String.class});
            return Boolean.TRUE;
        }
        catch (Throwable t) {
        }
        try {
            oidType.getConstructor(new Class[]{String.class});
            return Boolean.FALSE;
        }
        catch (Throwable t) {
        }
        return null;
    }

    /**
     * If the given field is a wrapper-type single field identity primary key,
     * return its corresponding primitive class. Else return the field type.
     */
    private Class unwrapSingleFieldIdentity(FieldMetaData fmd) {
        if (!fmd.getDefiningMetaData().isOpenJPAIdentity()) {
            return fmd.getDeclaredType();
        }

        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.BYTE_OBJ:
                return byte.class;
            case JavaTypes.CHAR_OBJ:
                return char.class;
            case JavaTypes.DOUBLE_OBJ:
                return double.class;
            case JavaTypes.FLOAT_OBJ:
                return float.class;
            case JavaTypes.INT_OBJ:
                return int.class;
            case JavaTypes.SHORT_OBJ:
                return short.class;
            case JavaTypes.LONG_OBJ:
                return long.class;
            default:
                return fmd.getDeclaredType();
        }
    }

    /**
     * Return the proper getter method of the {@link Reflection} helper for
     * a field or getter method of the given type.
     */
    private Method getReflectionGetterMethod(Class type, Class argType)
            throws NoSuchMethodException {
        String name = "get";
        if (type.isPrimitive()) {
            name += StringUtil.capitalize(type.getName());
        }
        return Reflection.class.getMethod(name, new Class[]{Object.class,
                argType});
    }

    /**
     * Return the proper fetch method of the ObjectIdFieldSupplier for
     * a field of the given type.
     */
    private Method getFieldSupplierMethod(Class type)
            throws NoSuchMethodException {
        return getMethod(OIDFSTYPE, type, "fetch", true, false, false);
    }

    /**
     * Return the proper fetch method of the ObjectIdFieldConsumer for
     * a field of the given type.
     */
    private Method getFieldConsumerMethod(Class type)
            throws NoSuchMethodException {
        return getMethod(OIDFCTYPE, type, "store", false, false, false);
    }

    /**
     * Adds the pcNewObjectIdInstance method to classes using
     * application identity.
     */
    private void addNewObjectIdInstanceMethod(boolean obj) throws NoSuchMethodException {
        // public Object pcNewObjectIdInstance ()
        String mDesc = obj
                ? Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT)
                : Type.getMethodDescriptor(TYPE_OBJECT);

        MethodNode newOidMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                               PRE + "NewObjectIdInstance",
                                               mDesc,
                                               null, null);
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(newOidMeth);
        InsnList instructions = newOidMeth.instructions;

        Boolean usesClsString = usesClassStringIdConstructor();
        Class oidType = _meta.getObjectIdType();
        if (obj && usesClsString == null) {
            // throw new IllegalArgumentException (...);
            String msg = _loc.get("str-cons", oidType, _meta.getDescribedType()).getMessage();

            instructions.add(throwException(IllegalArgumentException.class, msg));
            return;
        }

        if (!_meta.isOpenJPAIdentity() && _meta.isObjectIdTypeShared()) {
            // new ObjectId (cls, oid)
            instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(ObjectId.class)));
            instructions.add(new InsnNode(Opcodes.DUP));

            if (_meta.isEmbeddedOnly() || _meta.hasAbstractPKField()) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                    classNode.name,
                                                    PRE + "GetIDOwningClass",
                                                    Type.getMethodDescriptor(Type.getType(Class.class))));
            }
            else {
                instructions.add(AsmHelper.getLoadConstantInsn(getType(_meta)));
            }
        }

        // new <oid class> ();
        instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(oidType)));
        instructions.add(new InsnNode(Opcodes.DUP));
        if (_meta.isOpenJPAIdentity() || (obj && usesClsString == Boolean.TRUE)) {
            if ((_meta.isEmbeddedOnly()
                    && !(_meta.isEmbeddable() && _meta.getIdentityType() == ClassMetaData.ID_APPLICATION))
                    || _meta.hasAbstractPKField()) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                    classNode.name,
                                                    PRE + "GetIDOwningClass",
                                                    Type.getMethodDescriptor(Type.getType(Class.class))));
            }
            else {
                instructions.add(AsmHelper.getLoadConstantInsn(getType(_meta)));
            }
        }

        String mDescInit = Type.getMethodDescriptor(Type.VOID_TYPE);
        if (obj) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(String.class)));

            if (usesClsString == Boolean.TRUE) {
                mDescInit = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Class.class), Type.getType(String.class));
            }
            else if (usesClsString == Boolean.FALSE) {
                mDescInit = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class));
            }
        }
        else if (_meta.isOpenJPAIdentity()) {
            // new <type>Identity (XXX.class, <pk>);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            FieldMetaData pk = _meta.getPrimaryKeyFields()[0];
            addGetManagedValueCode(classNode, instructions, pk, true);
            if (pk.getDeclaredTypeCode() == JavaTypes.PC) {
                int nextFreeVarPos = 1;
                addExtractObjectIdFieldValueCode(classNode, instructions, pk, nextFreeVarPos);
            }

            if (_meta.getObjectIdType() == ObjectId.class) {
                mDescInit = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Class.class), Type.getType(Object.class));
            }
            else if (_meta.getObjectIdType() == Date.class) {
                mDescInit = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Class.class), Type.getType(Date.class));
            }
            else {
                mDescInit = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Class.class), Type.getType(pk.getObjectIdFieldType()));
            }
        }

        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            Type.getInternalName(oidType),
                                            "<init>",
                                            mDescInit));

        if (!_meta.isOpenJPAIdentity() && _meta.isObjectIdTypeShared()) {
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(ObjectId.class),
                                                "<init>",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Class.class), Type.getType(Object.class))));

        }

        instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    /**
     * When communicating with the StateManager, many methods are used
     * depending on the class of state being passed. This method,
     * given the type of information being passed and the prefix
     * ('provided', 'replace', etc) of the method to
     * call, returns the StateManager method that should be used.
     *
     * @param type     the type of state being passed
     * @param prefix   the prefix of the method to call; all methods
     *                 end in '[state type]Field'; only the prefix varies
     * @param get      true if receiving information from the
     *                 StateManager, false if passing it to the SM
     * @param curValue true if the current state value is passed to
     *                 the StateManager as an extra argument
     */
    private Method getStateManagerMethod(Class type, String prefix,
                                         boolean get, boolean curValue)
            throws NoSuchMethodException {
        return getMethod(SMTYPE, type, prefix, get, true, curValue);
    }

    /**
     * Return the method of the given owner type matching the given criteria.
     *
     * @param type     the type of state being passed
     * @param prefix   the prefix of the method to call; all methods
     *                 end in '[state type]Field'; only the prefix varies
     * @param get      true if receiving information from the
     *                 owner, false if passing it to the owner
     * @param haspc    true if the pc is passed as an extra argument
     * @param curValue true if the current state value is passed to
     *                 the owner as an extra argument
     */
    private Method getMethod(Class owner, Class type, String prefix,
                             boolean get, boolean haspc, boolean curValue)
            throws NoSuchMethodException {
        // all methods end in [field type]Field, where the field type
        // can be any of the primitve types (but capitalized), 'String',
        // or 'Object'; figure out what type to use
        String typeName = type.getName();
        if (type.isPrimitive()) {
            typeName = typeName.substring(0, 1).toUpperCase(Locale.ENGLISH)
                    + typeName.substring(1);
        }
        else if (type.equals(String.class)) {
            typeName = "String";
        }
        else {
            typeName = "Object";
            type = Object.class;
        }

        // the field index is always passed as an arg; the pc instance and
        // the current value may be passed; if setting the new value is
        // also passed
        List plist = new ArrayList(4);
        if (haspc) {
            plist.add(PCTYPE);
        }
        plist.add(int.class);
        if (!get || curValue) {
            plist.add(type);
        }
        if (!get && curValue) {
            plist.add(type);
            plist.add(int.class);
        }

        // use reflection to return the right method
        String name = prefix + typeName + "Field";
        Class[] params = (Class[]) plist.toArray(new Class[0]);

        try {
            return AccessController.doPrivileged(
                    J2DoPrivHelper.getDeclaredMethodAction(owner, name, params));
        }
        catch (PrivilegedActionException pae) {
            throw (NoSuchMethodException) pae.getException();
        }
    }


    /**
     * Helper method to add the code necessary to throw the given
     * exception type, sans message.
     */
    private InsnList throwException(Class type) {
        return throwException(type, null);
    }

    /**
     * Helper method to add the code necessary to throw the given
     * exception type, sans message.
     */
    private InsnList throwException(Class type, String msg) {
        InsnList instructions = new InsnList();
        instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(type)));
        instructions.add(new InsnNode(Opcodes.DUP));
        if (msg != null) {
            instructions.add(AsmHelper.getLoadConstantInsn(msg));
        }
        String desc = msg != null
                ? Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class))
                : Type.getMethodDescriptor(Type.VOID_TYPE);
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            Type.getInternalName(type),
                                            "<init>",
                                            desc));
        instructions.add(new InsnNode(Opcodes.ATHROW));

        return instructions;
    }

    /**
     * Adds the PersistenceCapable interface to the class being
     * enhanced, and adds a default constructor for use by OpenJPA
     * if it is not already present.
     */
    private void enhanceClass(final ClassNodeTracker classNodeTracker) {

        // make the class implement PersistenceCapable
        final ClassNode classNode = classNodeTracker.getClassNode();
        classNode.interfaces.add(Type.getInternalName(PCTYPE));

        // add a version stamp
        addGetEnhancementContractVersionMethod(classNodeTracker);

        // find the default constructor
        final boolean hasDefaultCt = classNode.methods.stream()
                .anyMatch(m -> m.name.equals("<init>") && m.desc.equals("()V"));
        if (!hasDefaultCt) {
            if (!_defCons) {
                throw new UserException(_loc.get("enhance-defaultconst", classNode.name));
            }

            int accessMode;
            String access;
            if (_meta.isDetachable()) {
                // externalizable requires that the constructor
                // be public, so make the added constructor public
                accessMode = Opcodes.ACC_PUBLIC;
                access = "public";
            }
            else if ((pc.getClassNode().access & Opcodes.ACC_FINAL) > 0) {
                accessMode = Opcodes.ACC_PRIVATE;
                access = "private";
            }
            else {
                accessMode = Opcodes.ACC_PROTECTED;
                access = "protected";
            }

            MethodNode ctNode = new MethodNode(accessMode,
                                               "<init>",
                                               Type.getMethodDescriptor(Type.VOID_TYPE),
                                               null, null);
            ctNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            ctNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, classNode.superName,
                                                       "<init>", "()V"));
            ctNode.instructions.add(new InsnNode(Opcodes.RETURN));
            classNode.methods.add(ctNode);

            if (!(_meta.getDescribedType().isInterface() || getCreateSubclass())
                    && _log.isWarnEnabled()) {
                _log.warn(_loc.get("enhance-adddefaultconst", classNode.name, access));
            }

        }
    }

    /**
     * Adds the following fields to the PersistenceCapable instance:
     * <ul>
     * <li><code>private static int pcInheritedFieldCount</code></li>
     * <li><code>private static Class pcPCSuperclass</code>
     * </li>
     * <li><code>private static String[] pcFieldNames</code></li>
     * <li><code>private static Class[] pcFieldTypes</code></li>
     * <li><code>private static byte[] pcFieldFlags</code></li>
     * <li><code>protected transient StateManager pcStateManager</code>
     * if no PersistenceCapable superclass present)</li>
     * </ul>
     */
    private void addFields(ClassNodeTracker classNodeTracker) {
        final ClassNode classNode = classNodeTracker.getClassNode();

        classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                           INHERIT, Type.getDescriptor(int.class), null, null));
        classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                           PRE + "FieldNames", Type.getDescriptor(String[].class), null, null));
        classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                           PRE + "FieldTypes", Type.getDescriptor(Class[].class), null, null));
        classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                           PRE + "FieldFlags", Type.getDescriptor(byte[].class), null, null));
        classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                                           SUPER, Type.getDescriptor(Class.class), null, null));

        if (_addVersionInitFlag && _meta.getVersionField() != null) {
            classNode.fields.add(new FieldNode(Opcodes.ACC_PROTECTED | Opcodes.ACC_TRANSIENT,
                                               VERSION_INIT_STR, Type.getDescriptor(boolean.class), null, null));
        }
        if (_meta.getPCSuperclass() == null || getCreateSubclass()) {
            classNode.fields.add(new FieldNode(Opcodes.ACC_PROTECTED | Opcodes.ACC_TRANSIENT,
                                               SM, Type.getDescriptor(SMTYPE), null, null));
        }
    }

    /**
     * Modifies the class initialization method (creating one if necessary)
     * to initialize the static fields of the PersistenceCapable instance and
     * to register it with the impl helper.
     */
    private void addStaticInitializer(ClassNodeTracker classNodeTracker) {
        final ClassNode classNode = classNodeTracker.getClassNode();
        InsnList instructions = new InsnList();
        if (_meta.getPCSuperclass() != null) {
            if (getCreateSubclass()) {
                instructions.add(AsmHelper.getLoadConstantInsn(0));
                instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
            }
            else {
                // pcInheritedFieldCount = <superClass>.pcGetManagedFieldCount()
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                    classNode.superName,
                                                    PRE + "GetManagedFieldCount",
                                                    Type.getMethodDescriptor(Type.INT_TYPE)));
                instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
            }

            // pcPCSuperclass = <superClass>;
            // this intentionally calls getDescribedType() directly
            // instead of PCEnhancer.getType()
            instructions.add(AsmHelper.getLoadConstantInsn(_meta.getPCSuperclassMetaData().getDescribedType()));
            instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, SUPER, Type.getDescriptor(Class.class)));
        }

        FieldMetaData[] fmds = _meta.getDeclaredFields();

        // pcFieldNames = new String[] { "<name1>", "<name2>", ... };
        instructions.add(AsmHelper.getLoadConstantInsn(fmds.length));
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getInternalName(String.class)));
        for (int i = 0; i < fmds.length; i++) {
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(AsmHelper.getLoadConstantInsn(i));
            instructions.add(AsmHelper.getLoadConstantInsn(fmds[i].getName()));
            instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, PRE + "FieldNames", Type.getDescriptor(String[].class)));

        // pcFieldTypes = new Class[] { <type1>.class, <type2>.class, ... };
        instructions.add(AsmHelper.getLoadConstantInsn(fmds.length));
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, Type.getInternalName(Class.class)));
        for (int i = 0; i < fmds.length; i++) {
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(AsmHelper.getLoadConstantInsn(i));
            instructions.add(AsmHelper.getLoadConstantInsn(fmds[i].getDeclaredType()));
            instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, PRE + "FieldTypes", Type.getDescriptor(Class[].class)));

        // pcFieldFlags = new byte[] { <flag1>, <flag2>, ... };
        instructions.add(AsmHelper.getLoadConstantInsn(fmds.length));
        instructions.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
        for (int i = 0; i < fmds.length; i++) {
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(AsmHelper.getLoadConstantInsn(i));
            instructions.add(AsmHelper.getLoadConstantInsn(getFieldFlag(fmds[i])));
            instructions.add(new InsnNode(Opcodes.BASTORE));
        }
        instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, PRE + "FieldFlags", Type.getDescriptor(byte[].class)));

        // PCRegistry.register (cls,
        //    pcFieldNames, pcFieldTypes, pcFieldFlags,
        //  pcPCSuperclass, alias, new XXX ());
        instructions.add(AsmHelper.getLoadConstantInsn(_meta.getDescribedType()));
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, PRE + "FieldNames", Type.getDescriptor(String[].class)));
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, PRE + "FieldTypes", Type.getDescriptor(Class[].class)));
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, PRE + "FieldFlags", Type.getDescriptor(byte[].class)));
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, SUPER, Type.getDescriptor(Class.class)));

        if (_meta.isMapped() || _meta.isAbstract()) {
            instructions.add(AsmHelper.getLoadConstantInsn(_meta.getTypeAlias()));
        }
        else {
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }

        if ((pc.getClassNode().access & Opcodes.ACC_ABSTRACT) > 0) {
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        else {
            instructions.add(new TypeInsnNode(Opcodes.NEW, classNode.name));
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                classNode.name,
                                                "<init>",
                                                Type.getMethodDescriptor(Type.VOID_TYPE)));
        }

        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                            Type.getInternalName(HELPERTYPE),
                                            "register",
                                            Type.getMethodDescriptor(Type.VOID_TYPE,
                                                                     Type.getType(Class.class), Type.getType(String[].class),
                                                                     Type.getType(Class[].class), Type.getType(byte[].class),
                                                                     Type.getType(Class.class), Type.getType(String.class),
                                                                     Type.getType(PersistenceCapable.class))));

        // now add those instructions to the <clinit> method
        MethodNode clinit = getOrCreateClassInitMethod(classNode);
        final AbstractInsnNode retInsn = clinit.instructions.getLast();
        if (retInsn.getOpcode() != Opcodes.RETURN) {
            throw new IllegalStateException("Problem with parsing instructions. RETURN expected");
        }
        clinit.instructions.insertBefore(retInsn, instructions);
    }

    /**
     * Return the flag for the given field.
     */
    private static byte getFieldFlag(FieldMetaData fmd) {
        if (fmd.getManagement() == FieldMetaData.MANAGE_NONE) {
            return -1;
        }

        byte flags = 0;
        if (fmd.getDeclaredType().isPrimitive()
                || Serializable.class.isAssignableFrom(fmd.getDeclaredType())) {
            flags = PersistenceCapable.SERIALIZABLE;
        }

        if (fmd.getManagement() == FieldMetaData.MANAGE_TRANSACTIONAL) {
            flags |= PersistenceCapable.CHECK_WRITE;
        }
        else if (!fmd.isPrimaryKey() && !fmd.isInDefaultFetchGroup()) {
            flags |= PersistenceCapable.CHECK_WRITE
                    | PersistenceCapable.CHECK_READ;
        }
        else {
            flags |= PersistenceCapable.MEDIATE_WRITE
                    | PersistenceCapable.MEDIATE_READ;
        }
        return flags;
    }

    /**
     * Adds the code to properly handle PersistenceCapable serialization
     * to the bytecode. This includes creating and initializing the
     * static <code>serialVersionUID</code> constant if not already defined,
     * as well as creating a custom <code>writeObject</code> method if the
     * class is Serializable and does not define them.
     */
    private void addSerializationCode() {
        if (externalizeDetached() || !Serializable.class.isAssignableFrom(_meta.getDescribedType())) {
            return;
        }

        if (getCreateSubclass()) {
            // ##### what should happen if a type is Externalizable? It looks
            // ##### like Externalizable classes will not be serialized as PCs
            // ##### based on this logic.
            if (!Externalizable.class.isAssignableFrom(_meta.getDescribedType())) {
                addSubclassSerializationCode();
            }
            return;
        }

        // if not already present, add a serialVersionUID field; if the instance
        // is detachable and uses detached state without a declared field,
        // can't add a serial version UID because we'll be adding extra fields
        // to the enhanced version
        final Optional<FieldNode> serialVersionUIDNode = pc.getClassNode().fields.stream()
                .filter(f -> f.name.equals("serialVersionUID"))
                .findFirst();

        if (serialVersionUIDNode.isEmpty()) {
            Long uid = null;
            try {
                uid = ObjectStreamClass.lookup(_meta.getDescribedType()).getSerialVersionUID();
            }
            catch (Throwable t) {
                // last-chance catch for bug #283 (which can happen
                // in a variety of ClassLoading environments)
                if (_log.isTraceEnabled()) {
                    _log.warn(_loc.get("enhance-uid-access", _meta), t);
                }
                else {
                    _log.warn(_loc.get("enhance-uid-access", _meta));
                }
            }

            // if we couldn't access the serialVersionUID, we will have to
            // skip the override of that field and not be serialization
            // compatible with non-enhanced classes
            if (uid != null) {
                FieldNode serVersField = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                                                       "serialVersionUID",
                                                       Type.LONG_TYPE.getDescriptor(),
                                                       null, uid);
                pc.getClassNode().fields.add(serVersField);
            }
        }

        MethodNode writeObjectMeth = AsmHelper.getMethodNode(pc.getClassNode(), "writeObject", void.class, ObjectOutputStream.class)
                .orElse(null);

        boolean full = writeObjectMeth == null;

        // add write object method
        if (full) {
            // private void writeObject (ObjectOutputStream out)
            writeObjectMeth = new MethodNode(Opcodes.ACC_PRIVATE,
                                             "writeObject",
                                             Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectOutputStream.class)),
                                             null,
                                             new String[]{Type.getInternalName(IOException.class)});
            pc.getClassNode().methods.add(writeObjectMeth);
        }
        modifyWriteObjectMethod(pc.getClassNode(), writeObjectMeth, full);

        // and read object
        MethodNode readObjectMeth = AsmHelper.getMethodNode(pc.getClassNode(), "readObject",
                                                            void.class, ObjectInputStream.class)
                .orElse(null);

        full = readObjectMeth == null;
        if (full) {
            // private void readObject (ObjectInputStream in)
            readObjectMeth = new MethodNode(Opcodes.ACC_PRIVATE,
                                            "readObject",
                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectInputStream.class)),
                                            null,
                                            new String[]{Type.getInternalName(IOException.class),
                                                    Type.getInternalName(ClassNotFoundException.class)});
            pc.getClassNode().methods.add(readObjectMeth);

        }
        modifyReadObjectMethod(pc.getClassNode(), readObjectMeth, full);
    }

    private void addSubclassSerializationCode() {
        // for generated subclasses, serialization must write an instance of
        // the superclass instead of the subclass, so that the client VM can
        // deserialize successfully.

        // private Object writeReplace() throws ObjectStreamException
        MethodNode writeReplaceMeth = new MethodNode(Opcodes.ACC_PRIVATE,
                                                     "writeReplace",
                                                     Type.getMethodDescriptor(TYPE_OBJECT),
                                                     null,
                                                     new String[]{Type.getInternalName(ObjectStreamException.class)});
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(writeReplaceMeth);
        InsnList instructions = writeReplaceMeth.instructions;

        // Object o = new <managed-type>()
        instructions.add(new TypeInsnNode(Opcodes.NEW, managedType.getClassNode().name));
        instructions.add(new InsnNode(Opcodes.DUP)); // for post-<init> work
        instructions.add(new InsnNode(Opcodes.DUP)); // for <init>
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            managedType.getClassNode().name,
                                            "<init>",
                                            Type.getMethodDescriptor(Type.VOID_TYPE)));

        // copy all the fields.
        // ##### limiting to JPA @Transient limitations
        FieldMetaData[] fmds = _meta.getFields();
        for (FieldMetaData fmd : fmds) {
            if (fmd.isTransient()) {
                continue;
            }
            // o.<field> = this.<field> (or reflective analog)
            instructions.add(new InsnNode(Opcodes.DUP)); // for putfield
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this for getfield
            getfield(classNode, instructions, _meta.getDescribedType(), fmd.getName(), fmd.getDeclaredType());
            putfield(classNode, instructions, _meta.getDescribedType(), fmd.getName(), fmd.getDeclaredType());
        }
        instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    /**
     * Whether the class being enhanced should externalize to a detached
     * instance rather than serialize.
     */
    private boolean externalizeDetached() {
        return ClassMetaData.SYNTHETIC.equals(_meta.getDetachedState())
                && Serializable.class.isAssignableFrom(_meta.getDescribedType())
                && !_repos.getConfiguration().getDetachStateInstance().
                isDetachedStateTransient();
    }

    /**
     * Adds a custom writeObject method that delegates to the
     * {@link ObjectOutputStream#defaultWriteObject} method,
     * but only after calling the internal <code>pcSerializing</code> method.
     */
    private void modifyWriteObjectMethod(ClassNode classNode, MethodNode method, boolean full) {
        InsnList instructions = new InsnList();

        // bool clear = pcSerializing ();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            classNode.name,
                                            PRE + "Serializing",
                                            Type.getMethodDescriptor(Type.BOOLEAN_TYPE)));
        int clearVarPos = full ? 2 : method.maxLocals + 1;
        instructions.add(new VarInsnNode(Opcodes.ISTORE, clearVarPos));

        if (full) {
            // out.defaultWriteObject ();
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(ObjectOutputStream.class),
                                                "defaultWriteObject",
                                                Type.getMethodDescriptor(Type.VOID_TYPE)));
            instructions.add(new InsnNode(Opcodes.RETURN));

            method.instructions.insert(instructions);
            instructions.clear();
        }

        AbstractInsnNode insn = method.instructions.getFirst();
        // skip to the next RETURN instruction
        while ((insn = searchNextInstruction(insn, i -> i.getOpcode() == Opcodes.RETURN)) != null) {
            InsnList insns = new InsnList();
            insns.add(new VarInsnNode(Opcodes.ILOAD, clearVarPos));
            LabelNode lblEndIf = new LabelNode();
            insns.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIf));
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            insns.add(new InsnNode(Opcodes.ACONST_NULL));
            insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                         classNode.name,
                                         PRE + "SetDetachedState",
                                         Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class))));
            insns.add(lblEndIf);
            method.instructions.insertBefore(insn, insns);

            insn = insn.getNext();
        }

        method.instructions.insert(instructions);
    }

    /**
     * Adds a custom readObject method that delegates to the
     * {@link ObjectInputStream#readObject()} method.
     */
    private void modifyReadObjectMethod(ClassNode classNode, MethodNode method, boolean full) {
        InsnList instructions = new InsnList();

        // if this instance uses synthetic detached state, note that it has
        // been deserialized
        if (ClassMetaData.SYNTHETIC.equals(_meta.getDetachedState())) {

            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                                               Type.getInternalName(PersistenceCapable.class),
                                               "DESERIALIZED",
                                               Type.getDescriptor(Object.class)));

            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + "SetDetachedState",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT)));
        }

        if (full) {
            // in.defaultReadObject ();
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                Type.getInternalName(ObjectInputStream.class),
                                                "defaultReadObject",
                                                Type.getMethodDescriptor(Type.VOID_TYPE)));
            instructions.add(new InsnNode(Opcodes.RETURN));
        }

        method.instructions.insert(instructions);
    }

    /**
     * Creates the pcIsDetached() method to determine if an instance
     * is detached.
     */
    private void addIsDetachedMethod(ClassNode classNode) throws NoSuchMethodException {
        // public boolean pcIsDetached()
        MethodNode isDetachedMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                   PRE + "IsDetached",
                                                   Type.getMethodDescriptor(Type.getType(Boolean.class)),
                                                   null, null);
        classNode.methods.add(isDetachedMeth);

        boolean needsDefinitiveMethod = writeIsDetachedMethod(classNode, isDetachedMeth);
        if (!needsDefinitiveMethod) {
            return;
        }

        // private boolean pcIsDetachedStateDefinitive()
        //   return false;
        // auxilliary enhancers may change the return value of this method
        // if their specs consider detached state definitive

        MethodNode isDetachedStateDefinitiveMeth = new MethodNode(Opcodes.ACC_PRIVATE,
                                                                  ISDETACHEDSTATEDEFINITIVE,
                                                                  Type.getMethodDescriptor(Type.BOOLEAN_TYPE),
                                                                  null, null);
        classNode.methods.add(isDetachedStateDefinitiveMeth);
        isDetachedStateDefinitiveMeth.instructions.add(AsmHelper.getLoadConstantInsn(false));
        isDetachedStateDefinitiveMeth.instructions.add(new InsnNode(Opcodes.IRETURN));
    }

    /**
     * Creates the body of the pcIsDetached() method to determine if an
     * instance is detached.
     *
     * @return true if we need a pcIsDetachedStateDefinitive method, false
     * otherwise
     */
    private boolean writeIsDetachedMethod(ClassNode classNode, MethodNode meth) throws NoSuchMethodException {
        InsnList instructions = meth.instructions;
        // not detachable: return Boolean.FALSE
        if (!_meta.isDetachable()) {
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                               "FALSE", Type.getDescriptor(Boolean.class)));
            instructions.add(new InsnNode(Opcodes.ARETURN));
            return false;
        }

        // if (sm != null)
        //     return (sm.isDetached ()) ? Boolean.TRUE : Boolean.FALSE;
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        LabelNode lblEndIfNull = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblEndIfNull));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(SMTYPE),
                                            "isDetached",
                                            Type.getMethodDescriptor(Type.BOOLEAN_TYPE)));

        LabelNode lblEndIfFalse = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblEndIfFalse));
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                           "TRUE", Type.getDescriptor(Boolean.class)));
        instructions.add(new InsnNode(Opcodes.ARETURN));
        instructions.add(lblEndIfFalse);
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                           "FALSE", Type.getDescriptor(Boolean.class)));
        instructions.add(new InsnNode(Opcodes.ARETURN));
        // END - if (sm != null)


        // if we use detached state:
        // if (pcGetDetachedState () != null
        //     && pcGetDetachedState != DESERIALIZED)
        //     return Boolean.TRUE;
        Boolean state = _meta.usesDetachedState();
        LabelNode lblNotDeser = null;

        if (state != Boolean.FALSE) {
            instructions.add(lblEndIfNull);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + "GetDetachedState",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));

            lblEndIfNull = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblEndIfNull));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + "GetDetachedState",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                                               Type.getInternalName(PersistenceCapable.class),
                                               "DESERIALIZED",
                                               TYPE_OBJECT.getDescriptor()));

            lblNotDeser = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IF_ACMPEQ, lblNotDeser));
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                               "TRUE", Type.getDescriptor(Boolean.class)));
            instructions.add(new InsnNode(Opcodes.ARETURN));

            if (state == Boolean.TRUE) {
                // if we have to use detached state:
                // return Boolean.FALSE;
                instructions.add(lblEndIfNull);
                instructions.add(lblNotDeser);
                instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                                   "FALSE", Type.getDescriptor(Boolean.class)));
                instructions.add(new InsnNode(Opcodes.ARETURN));
                return false;
            }
        }

        instructions.add(lblEndIfNull);
        if (lblNotDeser != null) {
            instructions.add(lblNotDeser);
        }

        // allow users with version or auto-assigned pk fields to manually
        // construct a "detached" instance, so check these before taking into
        // account non-existent detached state

        // consider detached if version is non-default
        FieldMetaData version = _meta.getVersionField();
        if (state != Boolean.TRUE && version != null) {
            // if (<version> != <default>)
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            addGetManagedValueCode(classNode, instructions, version, true);
            LabelNode lblAfterDefault = ifDefaultValue(instructions, version);

            // return true
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                               "TRUE", Type.getDescriptor(Boolean.class)));
            instructions.add(new InsnNode(Opcodes.ARETURN));

            instructions.add(lblAfterDefault);

            if (!_addVersionInitFlag) {
                // else return false;
                instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                                   "FALSE", Type.getDescriptor(Boolean.class)));
                instructions.add(new InsnNode(Opcodes.ARETURN));
            }
            else {
                // if (pcVersionInit != false)
                // return true
                // else return null; //  (returning null because we don't know the correct answer)
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                getfield(classNode, instructions, null, VERSION_INIT_STR, boolean.class);
                LabelNode lblAfterEq = new LabelNode();
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lblAfterEq));
                instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                                   "TRUE", Type.getDescriptor(Boolean.class)));
                instructions.add(new InsnNode(Opcodes.ARETURN));

                instructions.add(lblAfterEq);
                instructions.add(AsmHelper.getLoadConstantInsn(null));
                instructions.add(new InsnNode(Opcodes.ARETURN));
            }

            return false;
        }

        // consider detached if auto-genned primary keys are non-default
        LabelNode ifIns = null;
        LabelNode ifIns2 = null;
        if (state != Boolean.TRUE && _meta.getIdentityType() == ClassMetaData.ID_APPLICATION) {
            // for each pk field:
            // if (<pk> != <default> [&& !"".equals (<pk>)])
            //        return Boolean.TRUE;
            FieldMetaData[] pks = _meta.getPrimaryKeyFields();
            for (FieldMetaData pk : pks) {
                if (pk.getValueStrategy() == ValueStrategies.NONE) {
                    continue;
                }

                if (ifIns != null) {
                    instructions.add(ifIns);
                }
                if (ifIns2 != null) {
                    instructions.add(ifIns2);
                }
                ifIns2 = null;
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this

                addGetManagedValueCode(classNode, instructions, pk, true);
                ifIns = ifDefaultValue(instructions, pk);
                if (pk.getDeclaredTypeCode() == JavaTypes.STRING) {
                    instructions.add(AsmHelper.getLoadConstantInsn(""));
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                    addGetManagedValueCode(classNode, instructions, pk, true);
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                        Type.getInternalName(String.class),
                                                        "equals",
                                                        Type.getMethodDescriptor(Type.BOOLEAN_TYPE, TYPE_OBJECT)));
                    ifIns2 = new LabelNode();
                    instructions.add(new JumpInsnNode(Opcodes.IFNE, ifIns2));
                }

                instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                                   "TRUE", Type.getDescriptor(Boolean.class)));
                instructions.add(new InsnNode(Opcodes.ARETURN));
            }
        }
        if (ifIns != null) {
            instructions.add(ifIns);
        }
        if (ifIns2 != null) {
            instructions.add(ifIns2);
        }

        // if detached state is not definitive, just give up now and return
        // null so that the runtime will perform a DB lookup to determine
        // whether we're detached or new
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            classNode.name,
                                            ISDETACHEDSTATEDEFINITIVE,
                                            Type.getMethodDescriptor(Type.BOOLEAN_TYPE)));
        LabelNode lblAfterNe = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNE, lblAfterNe));
        instructions.add(AsmHelper.getLoadConstantInsn(null));
        instructions.add(new InsnNode(Opcodes.ARETURN));
        instructions.add(lblAfterNe);

        // no detached state: if instance uses detached state and it's not
        // synthetic or the instance is not serializable or the state isn't
        // transient, must not be detached
        if (state == null
                && (!ClassMetaData.SYNTHETIC.equals(_meta.getDetachedState())
                || !Serializable.class.isAssignableFrom(_meta.getDescribedType())
                || !_repos.getConfiguration().getDetachStateInstance().isDetachedStateTransient())) {
            // return Boolean.FALSE
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                               "FALSE", Type.getDescriptor(Boolean.class)));
            instructions.add(new InsnNode(Opcodes.ARETURN));
            return true;
        }

        // no detached state: if instance uses detached state (and must be
        // synthetic and transient in serializable instance at this point),
        // not detached if state not set to DESERIALIZED
        if (state == null) {
            // if (pcGetDetachedState () == null) // instead of DESERIALIZED
            //     return Boolean.FALSE;
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + "GetDetachedState",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));
            LabelNode lblIfNn = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblIfNn));
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(Boolean.class),
                                               "FALSE", Type.getDescriptor(Boolean.class)));
            instructions.add(new InsnNode(Opcodes.ARETURN));
            instructions.add(lblIfNn);
        }

        // give up; we just don't know
        instructions.add(AsmHelper.getLoadConstantInsn(null));
        instructions.add(new InsnNode(Opcodes.ARETURN));
        return true;
    }

    /**
     * Compare the given field to its Java default, returning the
     * comparison instruction. The field value will already be on the stack.
     *
     * @return the LabelNode for the else block.
     */
    private static LabelNode ifDefaultValue(InsnList instructions,
                                            FieldMetaData fmd) {
        LabelNode lbl = new LabelNode();
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
            case JavaTypes.BYTE:
            case JavaTypes.CHAR:
            case JavaTypes.INT:
            case JavaTypes.SHORT:
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lbl));
                break;
            case JavaTypes.DOUBLE:
                instructions.add(AsmHelper.getLoadConstantInsn(0D));
                instructions.add(new InsnNode(Opcodes.DCMPL));
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lbl));
                break;
            case JavaTypes.FLOAT:
                instructions.add(AsmHelper.getLoadConstantInsn(0F));
                instructions.add(new InsnNode(Opcodes.FCMPL));
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lbl));
                break;
            case JavaTypes.LONG:
                instructions.add(AsmHelper.getLoadConstantInsn(0L));
                instructions.add(new InsnNode(Opcodes.LCMP));
                instructions.add(new JumpInsnNode(Opcodes.IFEQ, lbl));
                break;
            default:
                instructions.add(new JumpInsnNode(Opcodes.IFNULL, lbl));
        }
        return lbl;
    }

    /**
     * Helper method to get the code for the class initializer method,
     * creating the method if it does not already exist.
     */
    private MethodNode getOrCreateClassInitMethod(ClassNode classNode) {
        final Optional<MethodNode> clinitMethodNode = classNode.methods.stream()
                .filter(m -> m.name.equals("<clinit>"))
                .findFirst();
        if (clinitMethodNode.isPresent()) {

            return clinitMethodNode.get();
        }
        else {
            // add static initializer method if non exists
            MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC,
                                               "<clinit>",
                                               "()V",
                                               null, null);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            classNode.methods.add(clinit);
            return clinit;
        }
    }

    /**
     * Adds bytecode modifying the cloning behavior of the class being
     * enhanced to correctly replace the <code>pcStateManager</code>
     * instance fields of any clone created with their default values.
     * Also, if this class is the base PC type and does not declared
     * a clone method, one will be added. Also, if _pc is a synthetic
     * subclass, create the clone() method that clears the state manager
     * that may have been initialized in a super's clone() method.
     */
    private void addCloningCode() {
        if (_meta.getPCSuperclass() != null && !getCreateSubclass()) {
            return;
        }

        ClassNode classNode = pc.getClassNode();

        MethodNode cloneMeth = AsmHelper.getMethodNode(classNode, "clone", Object.class)
                .orElse(null);

        String superName = managedType.getClassNode().superName;

        // add the clone method if necessary
        if (cloneMeth == null) {
            // add clone support for base classes
            // which also implement cloneable
            boolean isCloneable = Cloneable.class.isAssignableFrom(managedType.getType());
            boolean extendsObject = superName.equals(Object.class.getName());
            if (!isCloneable || (!extendsObject && !getCreateSubclass())) {
                return;
            }

            if (!getCreateSubclass()) {
                if (_log.isTraceEnabled()) {
                    _log.trace(_loc.get("enhance-cloneable", managedType.getClassNode().name));
                }
            }

            // add clone method
            // protected Object clone () throws CloneNotSupportedException
            cloneMeth = new MethodNode(0,
                                       "clone",
                                       Type.getMethodDescriptor(TYPE_OBJECT),
                                       null,
                                       new String[]{Type.getInternalName(CloneNotSupportedException.class)});
            if (!setVisibilityToSuperMethod(cloneMeth)) {
                cloneMeth.access |= Opcodes.ACC_PROTECTED;
            }

            // return super.clone ();
            cloneMeth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            cloneMeth.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                          superName,
                                                          "clone",
                                                          Type.getMethodDescriptor(TYPE_OBJECT)));
            cloneMeth.instructions.add(new InsnNode(Opcodes.ARETURN));
        }
        else {
            if (cloneMeth.instructions.size() <= 1) {
                // if the clone method is basically empty
                return;
            }
        }

        // find calls to the template instruction; on match
        // clone will be on stack
        AbstractInsnNode insn = cloneMeth.instructions.getFirst();
        if ((insn = searchNextInstruction(insn, i -> i.getOpcode() == Opcodes.INVOKESPECIAL &&
                i instanceof MethodInsnNode && ((MethodInsnNode) i).name.equals("clone"))
        ) != null) {
            // ((<type>) clone).pcStateManager = null;
            InsnList instructions = new InsnList();
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, pc.getClassNode().name));
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

            cloneMeth.instructions.insert(insn, instructions);
        }
    }

    /**
     * Gets the auxiliary enhancers registered as {@link Services services}.
     */
    public AuxiliaryEnhancer[] getAuxiliaryEnhancers() {
        return _auxEnhancers;
    }

    /**
     * Allow any registered auxiliary code generators to run.
     */
    private void runAuxiliaryEnhancers() {
        for (AuxiliaryEnhancer auxEnhancer : _auxEnhancers) {
            auxEnhancer.run(pc.getClassNode(), _meta);
        }
    }

    /**
     * Affirms if the given method be skipped.
     *
     * @param method method to be skipped or not
     * @return true if any of the auxiliary enhancers skips the given method,
     * or if the method is a constructor
     */
    private boolean skipEnhance(MethodNode method) {
        if ("<init>".equals(method.name) || "<clinit>".equals(method.name)) {
            return true;
        }

        for (AuxiliaryEnhancer auxEnhancer : _auxEnhancers) {
            if (auxEnhancer.skipEnhance(method)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds synthetic field access methods that will replace all direct
     * field accesses.
     */
    private void addAccessors(ClassNodeTracker cnt) throws NoSuchMethodException {
        ClassNode classNode = cnt.getClassNode();
        FieldMetaData[] fmds = getCreateSubclass() ? _meta.getFields() : _meta.getDeclaredFields();
        for (int i = 0; i < fmds.length; i++) {
            if (getCreateSubclass()) {
                if (!getRedefine() && isPropertyAccess(fmds[i])) {
                    addSubclassSetMethod(classNode, fmds[i]);
                    addSubclassGetMethod(classNode, fmds[i]);
                }
            }
            else {
                addGetMethod(classNode, i, fmds[i]);
                addSetMethod(classNode, i, fmds[i]);
            }
        }
    }

    /**
     * Adds a non-static setter that delegates to the super methods, and
     * performs any necessary field tracking.
     */
    private void addSubclassSetMethod(ClassNode classNode, FieldMetaData fmd) throws NoSuchMethodException {
        Class propType = fmd.getDeclaredType();
        String setterName = getSetterName(fmd);

        MethodNode newMethod = new MethodNode(Opcodes.ACC_PUBLIC,
                                              setterName,
                                              Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(propType)),
                                              null, null);
        classNode.methods.add(newMethod);
        final InsnList instructions = newMethod.instructions;
        int nextFreeVarPos = 2;

        setVisibilityToSuperMethod(newMethod);


        // not necessary if we're already tracking access via redefinition
        if (!getRedefine()) {
            // get the orig value onto stack
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            addGetManagedValueCode(classNode, instructions, fmd, true);

            int valVarPos = nextFreeVarPos++;
            instructions.add(new VarInsnNode(AsmHelper.getStoreInsn(fmd.getDeclaredType()), valVarPos));
            addNotifyMutation(classNode, newMethod, newMethod.instructions.getLast(), fmd, valVarPos, 0);
        }

        // ##### test case: B extends A. Methods defined in A. What
        // ##### happens?
        // super.setXXX(...)
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(AsmHelper.getLoadInsn(propType), 1)); // 1st param
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            managedType.getClassNode().name,
                                            setterName,
                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(propType))));
        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    private boolean setVisibilityToSuperMethod(MethodNode method) {
        ClassNode classNode = managedType.getClassNode();
        final List<MethodNode> methods = classNode.methods.stream()
                .filter(m -> m.name.equals(method.name) && Objects.equals(m.parameters, method.parameters))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            throw new UserException(_loc.get("no-accessor", managedType.getClassNode().name, method.name));
        }
        MethodNode superMeth = methods.get(0);
        method.access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED);
        if ((superMeth.access & Opcodes.ACC_PRIVATE) > 0) {
            method.access |= Opcodes.ACC_PRIVATE;
            return true;
        }
        if ((superMeth.access & Opcodes.ACC_PROTECTED) > 0) {
            method.access |= Opcodes.ACC_PROTECTED;
            return true;
        }
        if ((superMeth.access & Opcodes.ACC_PUBLIC) > 0) {
            method.access |= Opcodes.ACC_PUBLIC;
            return true;
        }
        return false;
    }

    /**
     * Adds a non-static getter that delegates to the super methods, and
     * performs any necessary field tracking.
     */
    private void addSubclassGetMethod(ClassNode classNode, FieldMetaData fmd) {
        String getterName = "get" + StringUtil.capitalize(fmd.getName());

        final String finalGetterName = getterName;
        final boolean hasGetter = managedType.getClassNode().methods.stream()
                .filter(m -> m.name.equals(finalGetterName) && (m.parameters == null || m.parameters.isEmpty()))
                .findAny()
                .isPresent();
        if (!hasGetter) {
            getterName = "is" + StringUtil.capitalize(fmd.getName());
        }

        final Class propType = fmd.getDeclaredType();
        MethodNode getterMethod = new MethodNode(Opcodes.ACC_PUBLIC,
                                                 getterName,
                                                 Type.getMethodDescriptor(Type.getType(propType)),
                                                 null, null);
        classNode.methods.add(getterMethod);
        final InsnList instructions = getterMethod.instructions;

        // if we're not already tracking field access via reflection, then we
        // must make the getter hook in lazy loading before accessing the super
        // method.
        if (!getRedefine()) {
            addNotifyAccess(getterMethod, getterMethod.instructions.getLast(), fmd);
        }

        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            managedType.getClassNode().name,
                                            getterName,
                                            Type.getMethodDescriptor(Type.getType(propType))));
        instructions.add(new InsnNode(AsmHelper.getReturnInsn(propType)));
    }

    /**
     * Adds a static getter method for the given field.
     * The generated method interacts with the instance state and the
     * StateManager to get the value of the field.
     *
     * @param index the relative number of the field
     * @param fmd   metadata about the field to get
     */
    private void addGetMethod(ClassNode classNode, int index, FieldMetaData fmd) throws NoSuchMethodException {
        MethodNode method = createGetMethod(classNode, fmd);
        classNode.methods.add(method);
        final InsnList instructions = method.instructions;
        int nextFreeVarPos = 1;

        // if reads are not checked, just return the value
        byte fieldFlag = getFieldFlag(fmd);
        if ((fieldFlag & PersistenceCapable.CHECK_READ) == 0 && (fieldFlag & PersistenceCapable.MEDIATE_READ) == 0) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            addGetManagedValueCode(classNode, instructions, fmd, true);
            instructions.add(new InsnNode(AsmHelper.getReturnInsn(fmd.getDeclaredType())));
            return;
        }

        // if (inst.pcStateManager == null) return inst.<field>;
        instructions.add(loadManagedInstance());
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
        LabelNode afterIfNonNull = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, afterIfNonNull));
        instructions.add(loadManagedInstance());
        addGetManagedValueCode(classNode, instructions, fmd, true);
        instructions.add(new InsnNode(AsmHelper.getReturnInsn(fmd.getDeclaredType())));

        instructions.add(afterIfNonNull);
        // int field = pcInheritedFieldCount + <fieldindex>;
        int fieldLocalVarPos = nextFreeVarPos++;
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
        instructions.add(AsmHelper.getLoadConstantInsn(index));
        instructions.add(new InsnNode(Opcodes.IADD));
        instructions.add(new VarInsnNode(Opcodes.ISTORE, fieldLocalVarPos));

        // inst.pcStateManager.accessingField (field);
        // return inst.<field>;
        instructions.add(loadManagedInstance());
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
        instructions.add(new VarInsnNode(Opcodes.ILOAD, fieldLocalVarPos));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(SMTYPE),
                                            "accessingField",
                                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));

        instructions.add(loadManagedInstance());
        addGetManagedValueCode(classNode, instructions, fmd, true);
        instructions.add(new InsnNode(AsmHelper.getReturnInsn(fmd.getDeclaredType())));
    }

    /**
     * Adds a static setter method for the given field.
     * The generated method interacts with the instance state and the
     * StateManager to set the value of the field.
     *
     * @param index the relative number of the field
     * @param fmd   metadata about the field to set
     */
    private void addSetMethod(ClassNode classNode, int index, FieldMetaData fmd) throws NoSuchMethodException {
        MethodNode method = createSetMethod(classNode, fmd);
        classNode.methods.add(method);
        final InsnList instructions = method.instructions;

        // PCEnhancer uses static methods; PCSubclasser does not.
        // for a static method there is no 'this', so index starts with zero
        // but in that case we have the additional entity parameter on that position!
        int fieldParamPos = 1;

        // if (inst.pcStateManager == null) inst.<field> = value;
        instructions.add(loadManagedInstance());
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        LabelNode lblAfterIfNonNull = new LabelNode();
        instructions.add(new JumpInsnNode(Opcodes.IFNONNULL, lblAfterIfNonNull));
        instructions.add(loadManagedInstance());
        instructions.add(new VarInsnNode(AsmHelper.getLoadInsn(fmd.getDeclaredType()), fieldParamPos));
        addSetManagedValueCode(classNode, instructions, fmd);
        if (fmd.isVersion() && _addVersionInitFlag) {
            // if we are setting the version, flip the versionInit flag to true

            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(AsmHelper.getLoadConstantInsn(1));

            // pcVersionInit = true;
            instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, VERSION_INIT_STR, Type.BOOLEAN_TYPE.getDescriptor()));
        }
        instructions.add(new InsnNode(Opcodes.RETURN));

        instructions.add(lblAfterIfNonNull);

        // inst.pcStateManager.setting<fieldType>Field (inst,
        //     pcInheritedFieldCount + <index>, inst.<field>, value, 0);
        instructions.add(loadManagedInstance());
        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

        instructions.add(loadManagedInstance());
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
        instructions.add(AsmHelper.getLoadConstantInsn(index));
        instructions.add(new InsnNode(Opcodes.IADD));

        instructions.add(loadManagedInstance());
        addGetManagedValueCode(classNode, instructions, fmd, true);
        instructions.add(new VarInsnNode(AsmHelper.getLoadInsn(fmd.getDeclaredType()), fieldParamPos));
        instructions.add(new InsnNode(Opcodes.ICONST_0));

        final Method stateMgrMethod = getStateManagerMethod(fmd.getDeclaredType(), "setting", false, true);
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(stateMgrMethod.getDeclaringClass()),
                                            stateMgrMethod.getName(),
                                            Type.getMethodDescriptor(stateMgrMethod)));
        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Determines which attach / detach methods to use.
     */
    private void addAttachDetachCode() throws NoSuchMethodException {
        // see if any superclasses are detachable
        boolean parentDetachable = false;
        for (ClassMetaData parent = _meta.getPCSuperclassMetaData();
             parent != null; parent = parent.getPCSuperclassMetaData()) {
            if (parent.isDetachable()) {
                parentDetachable = true;
                break;
            }
        }

        ClassNode classNode = pc.getClassNode();

        // if parent not detachable, we need to add the detach state fields and
        // accessor methods
        if (_meta.getPCSuperclass() == null || getCreateSubclass() || parentDetachable != _meta.isDetachable()) {
            addIsDetachedMethod(classNode);
            addDetachedStateMethods(_meta.usesDetachedState() != Boolean.FALSE);
        }

        // if we detach on serialize, we also need to implement the
        // externalizable interface to write just the state for the fields
        // being detached
        if (externalizeDetached()) {
            try {
                addDetachExternalize(parentDetachable, _meta.usesDetachedState() != Boolean.FALSE);
            }
            catch (NoSuchMethodException nsme) {
                throw new GeneralException(nsme);
            }
        }
    }

    /**
     * Add the fields to hold detached state and their accessor methods.
     *
     * @param impl whether to fully implement detach state functionality
     */
    private void addDetachedStateMethods(boolean impl) {
        Field detachField = _meta.getDetachedStateField();
        String name = null;
        Class<?> declarer = null;
        final ClassNode classNode = pc.getClassNode();


        if (impl && detachField == null) {
            name = PRE + "DetachedState";
            FieldNode field = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                                            name,
                                            TYPE_OBJECT.getDescriptor(),
                                            null, null);
            classNode.fields.add(field);
        }
        else if (impl) {
            name = detachField.getName();
            declarer = detachField.getDeclaringClass();
        }

        // public Object pcGetDetachedState ()
        MethodNode getDetachedStateMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                         PRE + "GetDetachedState",
                                                         Type.getMethodDescriptor(TYPE_OBJECT),
                                                         null, null);
        classNode.methods.add(getDetachedStateMeth);

        if (impl) {
            // return pcDetachedState;
            getDetachedStateMeth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            getfield(classNode, getDetachedStateMeth.instructions, declarer, name, Object.class);
        }
        else {
            getDetachedStateMeth.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        getDetachedStateMeth.instructions.add(new InsnNode(Opcodes.ARETURN));


        // public void pcSetDetachedState (Object state)
        MethodNode setDetachedStateMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                         PRE + "SetDetachedState",
                                                         Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT),
                                                         null, null);
        classNode.methods.add(setDetachedStateMeth);

        if (impl) {
            // pcDetachedState = state;
            setDetachedStateMeth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            setDetachedStateMeth.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st parameter
            putfield(classNode, setDetachedStateMeth.instructions, declarer, name, Object.class);
        }
        setDetachedStateMeth.instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Adds to <code>code</code> the instructions to get field
     * <code>attrName</code> declared in type <code>declarer</code>
     * onto the top of the stack.
     * <p>
     * The instance to access must already be on the top of the
     * stack when this is invoked.
     */
    private void getfield(ClassNode classNode, InsnList instructions, Class declarer, String attrName, Class fieldType) {
        // first, see if we can convert the attribute name to a field name
        String fieldName = toBackingFieldName(attrName);
        FieldNode field = findField(classNode, declarer, fieldName);

        if (getCreateSubclass() && (field == null || !((field.access & Opcodes.ACC_PUBLIC) > 0))) {
            // we're creating the subclass, not redefining the user type.

            // Reflection.getXXX(this, Reflection.findField(...));

            // Reflection.findField(declarer, fieldName, true);
            instructions.add(AsmHelper.getLoadConstantInsn(declarer));
            instructions.add(AsmHelper.getLoadConstantInsn(fieldName));
            instructions.add(new InsnNode(Opcodes.ICONST_1)); // true
            instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                Type.getInternalName(Reflection.class),
                                                "findField",
                                                Type.getMethodDescriptor(Type.getType(Field.class),
                                                                         Type.getType(Class.class), Type.getType(String.class), Type.BOOLEAN_TYPE)));

            // Reflection.getXXX(this, Field as stackparam);
            try {
                final Method getterMethod = getReflectionGetterMethod(fieldType, Field.class);
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                    Type.getInternalName(Reflection.class),
                                                    getterMethod.getName(),
                                                    Type.getMethodDescriptor(getterMethod)));
            }
            catch (NoSuchMethodException e) {
                // should never happen
                throw new InternalException(e);
            }
            if (!fieldType.isPrimitive() && fieldType != Object.class) {
                instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(fieldType)));
            }

        }
        else {
            String owner = declarer != null ? Type.getInternalName(declarer) : classNode.name;
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, owner, fieldName, Type.getDescriptor(fieldType)));
        }

    }

    private FieldNode findField(ClassNode classNode, Class clazz, String fieldName) {
        if (classNode != null) {
            final Optional<FieldNode> field = classNode.fields.stream()
                    .filter(f -> f.name.equals(fieldName))
                    .findFirst();
            if (field.isPresent()) {
                return field.get();
            }
        }
        if (clazz == null) {
            return null;
        }
        try {
            final Field field = clazz.getDeclaredField(fieldName);
            return new FieldNode(Opcodes.ACC_PRIVATE, field.getName(), Type.getDescriptor(field.getType()), null, null);
        }
        catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != Object.class) {
                return findField(null, clazz.getSuperclass(), fieldName);
            }
        }
        return null;
    }

    /**
     * Adds to <code>code</code> the instructions to set field
     * <code>attrName</code> declared in type <code>declarer</code>
     * to the value of type <code>fieldType</code> on the top of the stack.
     * <p>
     * When this method is invoked, the value to load must
     * already be on the top of the stack,
     * and the instance to load into must be second.
     *
     * @param classNode
     * @param declarer  internal class name (org/bla/..) which contains the field
     */
    private void putfield(ClassNode classNode, InsnList instructions, Class declarer, String attrName, Class fieldType) {
        String fieldName = toBackingFieldName(attrName);

        if (getRedefine() || getCreateSubclass()) {
            // Reflection.set(this, Reflection.findField(...), value);
            // Reflection.findField(declarer, fieldName, true);
            instructions.add(AsmHelper.getLoadConstantInsn(declarer));
            instructions.add(AsmHelper.getLoadConstantInsn(fieldName));
            instructions.add(new InsnNode(Opcodes.ICONST_1)); // true
            instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                Type.getInternalName(Reflection.class),
                                                "findField",
                                                Type.getMethodDescriptor(Type.getType(Field.class),
                                                                         Type.getType(Class.class), Type.getType(String.class), Type.BOOLEAN_TYPE)));

            // Reflection.set(stackvalue, stackvalue, field);
            instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                                Type.getInternalName(Reflection.class),
                                                "set",
                                                Type.getMethodDescriptor(Type.VOID_TYPE,
                                                                         TYPE_OBJECT,
                                                                         fieldType.isPrimitive()
                                                                                 ? Type.getType(fieldType)
                                                                                 : TYPE_OBJECT,
                                                                         Type.getType(Field.class))));

        }
        else {
            String owner = declarer != null ? Type.getInternalName(declarer) : classNode.name;
            instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, owner, fieldName, Type.getDescriptor(fieldType)));
        }
    }


    /**
     * If using property access, see if there is a different backing field
     * name for the persistent attribute <code>name</code>.
     */
    private String toBackingFieldName(String name) {
        // meta is null when enhancing persistence-aware
        FieldMetaData fmd = _meta == null ? null : _meta.getField(name);
        if (_meta != null && isPropertyAccess(fmd)
                && _attrsToFields != null && _attrsToFields.containsKey(name)) {
            name = (String) _attrsToFields.get(name);
        }
        return name;
    }

    /**
     * If using property access, see if there is a different persistent
     * attribute name for the backing field <code>name</code>.
     */
    private String fromBackingFieldName(String name) {
        // meta is null when enhancing persistence-aware
        FieldMetaData fmd = _meta == null ? null : _meta.getField(name);
        if (_meta != null && isPropertyAccess(fmd)
                && _fieldsToAttrs != null && _fieldsToAttrs.containsKey(name)) {
            return (String) _fieldsToAttrs.get(name);
        }
        else {
            return name;
        }
    }

    /**
     * Implement the externalizable interface to detach on serialize.
     */
    private void addDetachExternalize(boolean parentDetachable, boolean detachedState)
            throws NoSuchMethodException {
        // ensure that the declared default constructor is public
        // for externalization
        final MethodNode ctNode = pc.getClassNode().methods.stream()
                .filter(m -> m.name.equals("<init>") && m.desc.equals("()V"))
                .findAny()
                .get();


        if ((ctNode.access & Opcodes.ACC_PUBLIC) == 0) {
            if (_log.isWarnEnabled()) {
                _log.warn(_loc.get("enhance-defcons-extern", _meta.getDescribedType()));
            }
            ctNode.access = ctNode.access & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC;
        }

        // declare externalizable interface
        if (!Externalizable.class.isAssignableFrom(_meta.getDescribedType())) {
            pc.declareInterface(Externalizable.class);
        }

        // make sure the user doesn't already have custom externalization or
        // serialization methods
        String readObjectDesc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectInputStream.class));
        boolean hasReadObject = managedType.getClassNode().methods.stream()
                .anyMatch(m -> m.name.equals("readObject") && m.desc.equals(readObjectDesc));

        String writeObjectDesc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectOutput.class));
        boolean hasWriteObject = managedType.getClassNode().methods.stream()
                .anyMatch(m -> m.name.equals("writeObject") && m.desc.equals(writeObjectDesc));

        if (hasReadObject || hasWriteObject) {
            throw new UserException(_loc.get("detach-custom-ser", _meta));
        }

        String readExternalDesc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectInput.class));
        boolean hasReadExternal = managedType.getClassNode().methods.stream()
                .anyMatch(m -> m.name.equals("readExternal") && m.desc.equals(readExternalDesc));

        String writeExternalDesc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectInput.class));
        boolean hasWriteExternal = managedType.getClassNode().methods.stream()
                .anyMatch(m -> m.name.equals("writeExternal") && m.desc.equals(writeExternalDesc));

        if (hasReadExternal || hasWriteExternal) {
            throw new UserException(_loc.get("detach-custom-extern", _meta));
        }

        // create list of all unmanaged serializable fields
        final List<FieldNode> fields = managedType.getClassNode().fields;
        List<FieldNode> unmgd = new ArrayList(fields.size());
        for (FieldNode field : fields) {
            if ((field.access & (Opcodes.ACC_TRANSIENT | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == 0
                    && !field.name.startsWith(PRE)
                    && _meta.getDeclaredField(field.name) == null) {
                unmgd.add(field);
            }
        }

        addReadExternal(parentDetachable, detachedState);
        addReadUnmanaged(unmgd, parentDetachable);
        addWriteExternal(parentDetachable, detachedState);
        addWriteUnmanaged(unmgd, parentDetachable);
    }

    /**
     * Add custom readExternal method.
     */
    private void addReadExternal(boolean parentDetachable, boolean detachedState)
            throws NoSuchMethodException {
        final String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectInput.class));
        MethodNode readExternalMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                     "readExternal",
                                                     methodDescriptor,
                                                     null,
                                                     new String[]{Type.getInternalName(IOException.class),
                                                             Type.getInternalName(ClassNotFoundException.class)});
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(readExternalMeth);
        InsnList instructions = readExternalMeth.instructions;

        // super.readExternal (in);
        // not sure if this works: this is depending on the order of the enhancement!
        // if the subclass gets enhanced first, then the superclass misses
        // the Externalizable at this point!
        Class<?> sup = _meta.getDescribedType().getSuperclass();
        if (!parentDetachable && Externalizable.class.isAssignableFrom(sup)) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(sup),
                                                "readExternal",
                                                methodDescriptor));
        }

        // readUnmanaged (in);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            Type.getInternalName(getType(_meta)),
                                            PRE + "ReadUnmanaged",
                                            methodDescriptor));

        if (detachedState) {
            // pcSetDetachedState (in.readObject ());
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(ObjectInput.class),
                                                "readObject",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + "SetDetachedState",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT)));

            // pcReplaceStateManager ((StateManager) in.readObject ());
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(ObjectInput.class),
                                                "readObject",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));

            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getInternalName(StateManager.class)));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + "ReplaceStateManager",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(StateManager.class))));
        }

        addReadExternalFields();

        // readExternalFields(in.readObject ());
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            classNode.name,
                                            "readExternalFields",
                                            methodDescriptor));
        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    private void addReadExternalFields() throws NoSuchMethodException {
        final String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectInput.class));
        MethodNode readExternalMeth = new MethodNode(Opcodes.ACC_PROTECTED,
                                                     "readExternalFields",
                                                     methodDescriptor,
                                                     null,
                                                     new String[]{Type.getInternalName(IOException.class),
                                                             Type.getInternalName(ClassNotFoundException.class)});
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(readExternalMeth);
        InsnList instructions = readExternalMeth.instructions;

        Class<?> sup = _meta.getPCSuperclass();
        if (sup != null) {
            //add a call to super.readExternalFields()
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(sup),
                                                "readExternalFields",
                                                methodDescriptor));
        }

        // read managed fields
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        for (FieldMetaData fmd : fmds) {
            if (!fmd.isTransient()) {
                readExternal(classNode, instructions, fmd.getName(), Type.getType(fmd.getDeclaredType()), fmd);
            }
        }
        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Read unmanaged fields from the stream (pcReadUnmanaged).
     */
    private void addReadUnmanaged(List<FieldNode> unmgd, boolean parentDetachable)
            throws NoSuchMethodException {
        final String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectInput.class));
        MethodNode readUnmanagedMeth = new MethodNode(Opcodes.ACC_PROTECTED,
                                                      PRE + "ReadUnmanaged",
                                                      methodDescriptor,
                                                      null,
                                                      new String[]{Type.getInternalName(IOException.class),
                                                              Type.getInternalName(ClassNotFoundException.class)});
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(readUnmanagedMeth);
        InsnList instructions = readUnmanagedMeth.instructions;

        // super.readUnmanaged (in);
        if (parentDetachable) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(getType(_meta.getPCSuperclassMetaData())),
                                                PRE + "ReadUnmanaged",
                                                methodDescriptor));
        }

        // read declared unmanaged serializable fields
        for (FieldNode field : unmgd) {
            readExternal(classNode, instructions, field.name, Type.getType(field.desc), null);
        }
        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Helper method to read a field from an externalization input stream.
     */
    private void readExternal(ClassNode classNode, InsnList instructions, String fieldName, Type fieldType, FieldMetaData fmd)
            throws NoSuchMethodException {

        if (fieldType == null) {
            fieldType = Type.getType(fmd.getDeclaredType());
        }
        String typeName = fieldType.getClassName();
        boolean isPrimitive = fieldType.getSort() != Type.OBJECT && fieldType.getSort() != Type.ARRAY;

        String methName;
        if (isPrimitive) {
            methName = typeName.substring(0, 1).toUpperCase(Locale.ENGLISH)
                    + typeName.substring(1);
            methName = "read" + methName;
        }
        else {
            methName = "readObject";
        }

        // <field> = in.read<type> ();
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        Type retType = isPrimitive ? fieldType : TYPE_OBJECT;
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(ObjectInput.class),
                                            methName,
                                            Type.getMethodDescriptor(retType)));

        if (!isPrimitive && !fieldType.getClassName().equals(Object.class.getName())) {
            instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, fieldType.getInternalName()));
        }
        if (fmd == null) {
            Class<?> type = AsmHelper.getDescribedClass(pc.getClassLoader(), fieldType.getDescriptor());
            if (type == null) {
                throw new RuntimeException("Cannot Load class " + fieldType.getDescriptor());
            }
            putfield(classNode, instructions, null, fieldName, type);
        }
        else {
            addSetManagedValueCode(classNode, instructions, fmd);
            switch (fmd.getDeclaredTypeCode()) {
                case JavaTypes.DATE:
                case JavaTypes.ARRAY:
                case JavaTypes.COLLECTION:
                case JavaTypes.MAP:
                case JavaTypes.OBJECT:
                case JavaTypes.CALENDAR:
                    // if (sm != null)
                    //   sm.proxyDetachedDeserialized (<index>);
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                    instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

                    LabelNode lblEndIf = new LabelNode();
                    instructions.add(new JumpInsnNode(Opcodes.IFNULL, lblEndIf));
                    instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
                    instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
                    instructions.add(AsmHelper.getLoadConstantInsn(fmd.getIndex()));
                    instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                        Type.getInternalName(SMTYPE),
                                                        "proxyDetachedDeserialized",
                                                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));
                    instructions.add(lblEndIf);
            }
        }
    }

    /**
     * Add custom writeExternal method.
     */
    private void addWriteExternal(boolean parentDetachable, boolean detachedState)
            throws NoSuchMethodException {

        final String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectOutput.class));
        MethodNode writeExternalMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                                      "writeExternal",
                                                      methodDescriptor,
                                                      null,
                                                      new String[]{Type.getInternalName(IOException.class)});
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(writeExternalMeth);
        InsnList instructions = writeExternalMeth.instructions;


        // super.writeExternal (out);
        Class sup = getType(_meta).getSuperclass();
        if (!parentDetachable && Externalizable.class.isAssignableFrom(sup)) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(sup),
                                                "writeExternal",
                                                methodDescriptor));
        }

        // writeUnmanaged (out);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            Type.getInternalName(getType(_meta)),
                                            PRE + "WriteUnmanaged",
                                            methodDescriptor));

        LabelNode go2 = null;

        if (detachedState) {
            // if (sm != null)
            //   if (sm.writeDetached (out))
            //      return;
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));

            LabelNode endIfNull = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFNULL, endIfNull));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, SM, Type.getDescriptor(SMTYPE)));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(SMTYPE),
                                                "writeDetached",
                                                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(ObjectOutput.class))));

            go2 = new LabelNode();
            instructions.add(new JumpInsnNode(Opcodes.IFEQ, go2));
            instructions.add(new InsnNode(Opcodes.RETURN));

            // else
            //   out.writeObject (pcGetDetachedState ());
            instructions.add(endIfNull);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + "GetDetachedState",
                                                Type.getMethodDescriptor(TYPE_OBJECT)));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(ObjectOutput.class),
                                                "writeObject",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT)));

            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(AsmHelper.getLoadConstantInsn(null));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                                Type.getInternalName(ObjectOutput.class),
                                                "writeObject",
                                                Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT)));
        }
        if (go2 != null) {
            instructions.add(go2);
        }

        addWriteExternalFields();

        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                            classNode.name,
                                            "writeExternalFields",
                                            methodDescriptor));

        // return
        instructions.add(new InsnNode(Opcodes.RETURN));
    }


    private void addWriteExternalFields() throws NoSuchMethodException {
        final String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectOutput.class));
        MethodNode writeExternalFieldsMeth = new MethodNode(Opcodes.ACC_PROTECTED,
                                                            "writeExternalFields",
                                                            methodDescriptor,
                                                            null,
                                                            new String[]{Type.getInternalName(IOException.class)});
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(writeExternalFieldsMeth);
        InsnList instructions = writeExternalFieldsMeth.instructions;

        Class<?> sup = _meta.getPCSuperclass();
        if (sup != null) {
            // add a call to super.writeExternalFields()
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(sup),
                                                "writeExternalFields",
                                                methodDescriptor));
        }

        FieldMetaData[] fmds = _meta.getDeclaredFields();
        for (FieldMetaData fmd : fmds) {
            if (!fmd.isTransient()) {
                writeExternal(classNode, instructions, fmd.getName(),
                              Type.getType(fmd.getDeclaredType()), fmd);
            }
        }

        // return
        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Write unmanaged fields to the stream (pcWriteUnmanaged).
     */
    private void addWriteUnmanaged(List<FieldNode> unmgd, boolean parentDetachable)
            throws NoSuchMethodException {
        final String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(ObjectOutput.class));
        MethodNode writeUnmanagedMeth = new MethodNode(Opcodes.ACC_PROTECTED,
                                                       PRE + "WriteUnmanaged",
                                                       methodDescriptor,
                                                       null,
                                                       new String[]{Type.getInternalName(IOException.class)});
        final ClassNode classNode = pc.getClassNode();
        classNode.methods.add(writeUnmanagedMeth);
        InsnList instructions = writeUnmanagedMeth.instructions;

        // super.writeUnmanaged (out);
        if (parentDetachable) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(getType(_meta.getPCSuperclassMetaData())),
                                                PRE + "WriteUnmanaged",
                                                methodDescriptor));
        }

        // write declared unmanaged serializable fields
        for (FieldNode field : unmgd) {
            writeExternal(classNode, instructions, field.name, Type.getType(field.desc), null);
        }
        instructions.add(new InsnNode(Opcodes.RETURN));
    }

    /**
     * Helper method to write a field to an externalization output stream.
     */
    private void writeExternal(ClassNode classNode, InsnList instructions, String fieldName, Type fieldType, FieldMetaData fmd)
            throws NoSuchMethodException {
        String typeName = fieldType.getClassName();
        boolean isPrimitive = fieldType.getSort() != Type.OBJECT && fieldType.getSort() != Type.ARRAY;

        String methName;
        if (isPrimitive) {
            methName = typeName.substring(0, 1).toUpperCase(Locale.ENGLISH)
                    + typeName.substring(1);
            methName = "write" + methName;
        }
        else {
            methName = "writeObject";
        }

        // out.write<type> (<field>);
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1)); // 1st param
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this

        if (fmd == null) {
            Class<?> type = AsmHelper.getDescribedClass(pc.getClassLoader(), fieldType.getDescriptor());
            getfield(classNode, instructions, null, fieldName, type);
        }
        else {
            addGetManagedValueCode(classNode, instructions, fmd, true);
        }

        String mdesc;
        if (fieldType.getSort() == Type.BYTE || fieldType.getSort() == Type.CHAR || fieldType.getSort() == Type.SHORT) {
            mdesc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE);
        }
        else if (!isPrimitive) {
            mdesc = Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT);
        }
        else {
            mdesc = Type.getMethodDescriptor(Type.VOID_TYPE, fieldType);
        }
        instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                                            Type.getInternalName(ObjectOutput.class),
                                            methName,
                                            mdesc));
    }


    /**
     * Load the field value specified by <code>fmd</code> onto the stack.
     * Before this method is called, the object that the data should be loaded
     * from will be on the top of the stack.
     *
     * @param fromSameClass if <code>true</code>, then <code>fmd</code> is
     *                      being loaded from an instance of the same class as the current execution
     *                      context. If <code>false</code>, then the instance on the top of the stack
     *                      might be a superclass of the current execution context's 'this' instance.
     */
    private void addGetManagedValueCode(ClassNode classNode, InsnList instructions, FieldMetaData fmd, boolean fromSameClass) {
        // if redefining, then we must always reflect (or access the field
        // directly if accessible), since the redefined methods will always
        // trigger method calls to StateManager, even from internal direct-
        // access usage. We could work around this by not redefining, and
        // just do a subclass approach instead. But this is not a good option,
        // since it would sacrifice lazy loading and efficient dirty tracking.

        if (getRedefine() || isFieldAccess(fmd)) {
            getfield(classNode, instructions, getType(_meta), fmd.getName(), fmd.getDeclaredType());
        }
        else if (getCreateSubclass()) {
            // property access, and we're not redefining. If we're operating
            // on an instance that is definitely the same type as 'this', then
            // call superclass method to bypass tracking. Otherwise, reflect
            // to both bypass tracking and avoid class verification errors.
            if (fromSameClass) {
                Method meth = (Method) fmd.getBackingMember();
                instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                    Type.getInternalName(meth.getDeclaringClass()),
                                                    meth.getName(),
                                                    Type.getMethodDescriptor(meth)));
            }
            else {
                getfield(classNode, instructions, getType(_meta), fmd.getName(), fmd.getDeclaredType());
            }
        }
        else {
            // regular enhancement + property access
            Method meth = (Method) fmd.getBackingMember();
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + meth.getName(),
                                                Type.getMethodDescriptor(meth)));
        }
    }


    /**
     * Store the given value into the field value specified
     * by <code>fmd</code>. Before this method is called, the data to load will
     * be on the top of the stack and the object that the data should be loaded
     * into will be second in the stack.
     */
    private InsnList getSetValueInsns(ClassNode classNode, FieldMetaData fmd, Object value) {
        InsnList instructions = new InsnList();
        if (value == null) {
            instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        else {
            instructions.add(AsmHelper.getLoadConstantInsn(value));
        }

        // if redefining, then we must always reflect (or access the field
        // directly if accessible), since the redefined methods will always
        // trigger method calls to StateManager, even from internal direct-
        // access usage. We could work around this by not redefining, and
        // just do a subclass approach instead. But this is not a good option,
        // since it would sacrifice lazy loading and efficient dirty tracking.
        if (getRedefine() || isFieldAccess(fmd)) {
            putfield(classNode, instructions, fmd.getDeclaringType(), fmd.getName(), fmd.getDeclaredType());
        }
        else if (getCreateSubclass()) {
            // property access, and we're not redefining. invoke the
            // superclass method to bypass tracking.
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                managedType.getClassNode().name,
                                                getSetterName(fmd),
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(fmd.getDeclaredType()))));
        }
        else {
            // regular enhancement + property access
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + getSetterName(fmd),
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(fmd.getDeclaredType()))));

        }
        return instructions;
    }

    /**
     * Store the value at the top of the stack into the field value specified
     * by <code>fmd</code>. Before this method is called, the data to load will
     * be on the top of the stack and the object that the data should be loaded
     * into will be second in the stack.
     */
    private void addSetManagedValueCode(ClassNode classNode, InsnList instructions, FieldMetaData fmd) {
        // if redefining, then we must always reflect (or access the field
        // directly if accessible), since the redefined methods will always
        // trigger method calls to StateManager, even from internal direct-
        // access usage. We could work around this by not redefining, and
        // just do a subclass approach instead. But this is not a good option,
        // since it would sacrifice lazy loading and efficient dirty tracking.
        if (getRedefine() || isFieldAccess(fmd)) {
            putfield(classNode, instructions, getType(_meta), fmd.getName(), fmd.getDeclaredType());
        }
        else if (getCreateSubclass()) {
            // property access, and we're not redefining. invoke the
            // superclass method to bypass tracking.
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                classNode.superName,
                                                getSetterName(fmd),
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(fmd.getDeclaredType()))));
        }
        else {
            // regular enhancement + property access
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                                classNode.name,
                                                PRE + getSetterName(fmd),
                                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(fmd.getDeclaredType()))));
        }
    }

    /**
     * Add the {@link Instruction}s to load the instance to modify onto the
     * stack, and return it. If <code>forStatic</code> is set, then
     * <code>code</code> is in an accessor method or another static method;
     * otherwise, it is in one of the PC-specified methods.
     *
     * @return instruction to load the persistence context to the stack.
     */
    private AbstractInsnNode loadManagedInstance() {
        // 1st method parameter is position 0 on the stack for a STATIC method
        // or use the this* for a non-STATIC method
        // In both cases we end up with ALOAD_0, while it essentially does different things.
        return new VarInsnNode(Opcodes.ALOAD, 0);
    }

    /**
     * Affirms if the given class is using field-based access.
     */
    boolean isPropertyAccess(ClassMetaData meta) {
        return meta != null && (meta.isMixedAccess() ||
                AccessCode.isProperty(meta.getAccessType()));
    }

    /**
     * Affirms if the given field is using field-based access.
     */
    boolean isPropertyAccess(FieldMetaData fmd) {
        return fmd != null && AccessCode.isProperty(fmd.getAccessType());
    }

    /**
     * Affirms if the given field is using method-based access.
     */
    boolean isFieldAccess(FieldMetaData fmd) {
        return fmd != null && AccessCode.isField(fmd.getAccessType());
    }

    /**
     * Create the generated getter {@link MethodNode} for <code>fmd</code>. The
     * calling environment will then populate this method's code block.
     */
    private MethodNode createGetMethod(ClassNode classNode, FieldMetaData fmd) {
        if (isFieldAccess(fmd)) {
            // static <fieldtype> pcGet<field> (XXX inst)
            final FieldNode field = classNode.fields.stream()
                    .filter(f -> f.name.equals(fmd.getName()))
                    .findFirst()
                    .get();

            MethodNode getter = new MethodNode((field.access & ~Opcodes.ACC_TRANSIENT & ~Opcodes.ACC_VOLATILE)
                                                       | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC,
                                               PRE + "Get" + fmd.getName(),
                                               Type.getMethodDescriptor(Type.getType(fmd.getDeclaredType()), Type.getObjectType(classNode.name)),
                                               null, null);
            return getter;
        }

        // property access:
        // change the user's getter method to a new name and create a new method with the old name
        Method meth = (Method) fmd.getBackingMember();

        MethodNode getter = AsmHelper.getMethodNode(classNode, meth).get();

        // and a new method which replaces the old one
        MethodNode newGetter = new MethodNode(getter.access,
                                              meth.getName(),
                                              Type.getMethodDescriptor(meth),
                                              null, null);

        getter.name = PRE + meth.getName();
        getter.access = (getter.access & ~Opcodes.ACC_PUBLIC & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PROTECTED;

        moveAnnotations(getter, newGetter);

        // copy over all ParemeterizedType info if any.
        newGetter.signature = getter.signature;

        return newGetter;
    }


    /**
     * move the annotations over from the original method to the other method
     */
    private void moveAnnotations(MethodNode from, MethodNode to) {
        if (from.visibleAnnotations != null) {
            if (to.visibleAnnotations == null) {
                to.visibleAnnotations = new ArrayList<>();
            }
            to.visibleAnnotations.addAll(from.visibleAnnotations);

            from.visibleAnnotations.clear();
        }
    }

    /**
     * Create the generated setter {@link MethodNode} for <code>fmd</code>. The
     * calling environment will then populate this method's code block.
     */
    private MethodNode createSetMethod(ClassNode classNode, FieldMetaData fmd) {
        if (isFieldAccess(fmd)) {
            // static void pcSet<field> (XXX inst, <fieldtype> value)
            final FieldNode field = classNode.fields.stream()
                    .filter(f -> f.name.equals(fmd.getName()))
                    .findFirst()
                    .get();
            MethodNode setter = new MethodNode((field.access & ~Opcodes.ACC_TRANSIENT & ~Opcodes.ACC_VOLATILE)
                                                       | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC,
                                               PRE + "Set" + fmd.getName(),
                                               Type.getMethodDescriptor(Type.VOID_TYPE,
                                                                        Type.getType(getType(_meta)),
                                                                        Type.getType(fmd.getDeclaredType())),
                                               null, null);

            return setter;
        }

        // property access:
        // change the user's setter method to a new name and create a new method with the old name
        final MethodNode setter = AsmHelper.getMethodNode(classNode, getSetterName(fmd), void.class, fmd.getDeclaredType()).get();

        final String setterName = setter.name;

        // and a new method which replaces the old one
        MethodNode newSetter = new MethodNode(setter.access,
                                              setterName,
                                              setter.desc,
                                              null, null);

        setter.name = PRE + setterName;
        setter.access = (setter.access & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PUBLIC) | Opcodes.ACC_PROTECTED;
        moveAnnotations(setter, newSetter);

        // copy over all ParemeterizedType info if any.
        newSetter.signature = setter.signature;

        return newSetter;
    }

    private void addGetEnhancementContractVersionMethod(ClassNodeTracker cnt) {
        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC,
                                               PRE + "GetEnhancementContractVersion",
                                               Type.getMethodDescriptor(Type.INT_TYPE),
                                               null, null);
        methodNode.instructions.add(AsmHelper.getLoadConstantInsn(ENHANCER_VERSION));
        methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
        cnt.getClassNode().methods.add(methodNode);
    }

    /**
     * Return the concrete type for the given class, i.e. impl for managed
     * interfaces
     */
    public Class getType(ClassMetaData meta) {
        if (meta.getInterfaceImpl() != null) {
            return meta.getInterfaceImpl();
        }
        return meta.getDescribedType();
    }

    /**
     * Usage: java org.apache.openjpa.enhance.PCEnhancer [option]*
     * &lt;class name | .java file | .class file | .jdo file&gt;+
     * Where the following options are recognized.
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
     * will be replaced by the appropriate    get/set method. If the type
     * explicitly declares the persistence-capable interface, it will
     * not be enhanced. Thus, it is safe to invoke the enhancer on classes
     * that are already enhanced.
     */
    public static void main(String[] args) {
        Options opts = new Options();
        args = opts.setFromCmdLine(args);
        if (!run(args, opts)) {
            // START - ALLOW PRINT STATEMENTS
            System.err.println(_loc.get("enhance-usage"));
            // STOP - ALLOW PRINT STATEMENTS
        }
    }

    /**
     * Run the tool. Returns false if invalid options given. Runs against all
     * the persistence units defined in the resource to parse.
     */
    public static boolean run(final String[] args, Options opts) {
        return Configurations.runAgainstAllAnchors(opts,
                                                   opts1 -> {
                                                       OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
                                                       try {
                                                           return run(conf, args, opts1);
                                                       }
                                                       finally {
                                                           conf.close();
                                                       }
                                                   });
    }

    /**
     * Run the tool. Returns false if invalid options given.
     */
    public static boolean run(OpenJPAConfiguration conf, String[] args,
                              Options opts)
            throws IOException {
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

        // for unit testing
        BytecodeWriter writer = (BytecodeWriter) opts.get(
                PCEnhancer.class.getName() + "#bytecodeWriter");

        Configurations.populateConfiguration(conf, opts);
        return run(conf, args, flags, null, writer, null);
    }

    /**
     * Enhance the given classes.
     */
    public static boolean run(OpenJPAConfiguration conf, String[] args,
                              Flags flags, MetaDataRepository repos, BytecodeWriter writer,
                              ClassLoader loader)
            throws IOException {
        if (loader == null) {
            loader = conf.getClassResolverInstance().
                    getClassLoader(PCEnhancer.class, null);
        }
        if (flags.tmpClassLoader) {
            loader = AccessController.doPrivileged(J2DoPrivHelper
                                                           .newTemporaryClassLoaderAction(loader));
        }

        if (repos == null) {
            repos = conf.newMetaDataRepositoryInstance();
            repos.setSourceMode(MetaDataModes.MODE_META);
        }

        Log log = conf.getLog(OpenJPAConfiguration.LOG_TOOL);
        Collection classes;
        if (args == null || args.length == 0) {
            classes = repos.getPersistentTypeNames(true, loader);
            if (classes == null) {
                log.warn(_loc.get("no-class-to-enhance"));
                return false;
            }
        }
        else {
            ClassArgParser cap = conf.getMetaDataRepositoryInstance().
                    getMetaDataFactory().newClassArgParser();
            cap.setClassLoader(loader);
            classes = new HashSet();
            for (String arg : args) {
                classes.addAll(Arrays.asList(cap.parseTypes(arg)));
            }
        }

        EnhancementProject project = new EnhancementProject();
        ClassNodeTracker cnt;
        PCEnhancer enhancer;
        Collection persAwareClasses = new HashSet();

        int status;
        for (Object o : classes) {
            if (log.isInfoEnabled()) {
                log.info(_loc.get("enhance-running", o));
            }

            if (o instanceof String) {
                cnt = project.loadClass((String) o, loader);
            }
            else {
                cnt = project.loadClass((Class) o);
            }
            enhancer = new PCEnhancer(conf, cnt, repos, loader);
            if (writer != null) {
                enhancer.setBytecodeWriter(writer);
            }
            enhancer.setDirectory(flags.directory);
            enhancer.setAddDefaultConstructor(flags.addDefaultConstructor);
            status = enhancer.run();
            if (status == ENHANCE_NONE) {
                if (log.isTraceEnabled()) {
                    log.trace(_loc.get("enhance-norun"));
                }
            }
            else if (status == ENHANCE_INTERFACE) {
                if (log.isTraceEnabled()) {
                    log.trace(_loc.get("enhance-interface"));
                }
            }
            else if (status == ENHANCE_AWARE) {
                persAwareClasses.add(o);
                enhancer.record();
            }
            else {
                enhancer.record();
            }
            project.clear();
        }
        if (log.isInfoEnabled() && !persAwareClasses.isEmpty()) {
            log.info(_loc.get("pers-aware-classes", persAwareClasses.size(), persAwareClasses));
        }
        return true;
    }

    /**
     * Run flags.
     */
    public static class Flags {

        public File directory = null;
        public boolean addDefaultConstructor = true;
        public boolean tmpClassLoader = true;
        public boolean enforcePropertyRestrictions = false;
    }

    /**
     * Plugin interface for additional enhancement.
     */
    public interface AuxiliaryEnhancer {
        void run(ClassNode classNode, ClassMetaData meta);

        boolean skipEnhance(MethodNode m);
    }

    private void addGetIDOwningClass() {
        MethodNode idOCMeth = new MethodNode(Opcodes.ACC_PUBLIC,
                                             PRE + "GetIDOwningClass",
                                             Type.getMethodDescriptor(Type.getType(Class.class)),
                                             null, null);
        pc.getClassNode().methods.add(idOCMeth);

        idOCMeth.instructions.add(AsmHelper.getLoadConstantInsn(getType(_meta)));
        idOCMeth.instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    /**
     * This static public worker method detects and logs any Entities that may have been enhanced at build time by
     * a version of the enhancer that is older than the current version.
     *
     * @param cls - A non-null Class implementing org.apache.openjpa.enhance.PersistenceCapable.
     * @param log - A non-null org.apache.openjpa.lib.log.Log.
     * @return true if the provided Class is down level from the current PCEnhancer.ENHANCER_VERSION. False
     * otherwise.
     * @throws - IllegalStateException if cls doesn't implement org.apache.openjpa.enhance.PersistenceCapable.
     */
    public static boolean checkEnhancementLevel(Class<?> cls, Log log) {
        if (cls == null || log == null) {
            return false;
        }
        PersistenceCapable pc = PCRegistry.newInstance(cls, null, false);
        if (pc == null) {
            return false;
        }
        if (pc.pcGetEnhancementContractVersion() < PCEnhancer.ENHANCER_VERSION) {
            log.info(_loc.get("down-level-enhanced-entity", new Object[]{cls.getName(),
                    pc.pcGetEnhancementContractVersion(), PCEnhancer.ENHANCER_VERSION}));
            return true;
        }
        return false;
    }

    /**
     * Read the optimizedIdCopy value from the config (if available)
     */
    private void configureOptimizeIdCopy() {
        if (_repos != null && _repos.getConfiguration() != null) {
            _optimizeIdCopy = _repos.getConfiguration().getOptimizeIdCopy();
        }
    }

    /*
     * Cycles through all primary keys verifying whether they can and should
     * be used for faster oid copy.  The field must be private and must
     * not have a public setter.  If this is the case, the list of pk fields is
     * returned.  If not, returns null.
     */
    private ArrayList<Integer> optimizeIdCopy(Class<?> oidType, FieldMetaData[] fmds) {
        // collect all object id fields and verify they
        // a) have a private field
        // b) do not have a public setter
        ArrayList<Integer> pkFields = new ArrayList<>();
        // build list of primary key fields
        for (int i = 0; i < fmds.length; i++) {
            if (!fmds[i].isPrimaryKey()) {
                continue;
            }
            // optimizing copy with PC type not (yet) supported
            if (fmds[i].getDeclaredTypeCode() == JavaTypes.PC) {
                return null;
            }
            String name = fmds[i].getName();
            Field fld = Reflection.findField(oidType, name, false);
            if (fld == null || Modifier.isPublic(fld.getModifiers())) {
                return null;
            }
            Method setter = Reflection.findSetter(oidType, name, false);
            if (setter == null || !Modifier.isPublic(setter.getModifiers())) {
                pkFields.add(i);
            }
            else {
                return null;
            }
        }
        return pkFields.size() > 0 ? pkFields : null;
    }

    /*
     * Cycles through all constructors of an IdClass and examines the instructions to find
     * a matching constructor for the provided pk fields.  If a match is found, it returns
     * the order (relative to the field metadata) of the constructor parameters.  If a match
     * is not found, returns null.
     *
     * We use byte code analysis to find the fields the ct works on.
     */
    private int[] getIdClassConstructorParmOrder(Class<?> oidType, List<Integer> pkfields, FieldMetaData[] fmds) {
        final ClassNode classNode = AsmHelper.readClassNode(oidType);
        final List<MethodNode> cts = classNode.methods.stream()
                .filter(m -> "<init>".equals(m.name))
                .collect(Collectors.toList());

        if (cts.isEmpty()) {
            return null;
        }

        int[] parmOrder = new int[pkfields.size()];
        for (MethodNode ct : cts) {
            if ((ct.access & Opcodes.ACC_PUBLIC) == 0) {
                // ignore non public constructors
                continue;
            }
            Type[] argTypes = Type.getArgumentTypes(ct.desc);

            // make sure the constructors have the same # of parms as
            // the number of pk fields
            if (listSize(pkfields) != argTypes.length) {
                continue;
            }

            int parmOrderIndex = 0;
            AbstractInsnNode insn = ct.instructions.getFirst();
            // skip to the next PUTFIELD instruction
            while ((insn = searchNextInstruction(insn, i -> i.getOpcode() == Opcodes.PUTFIELD)) != null) {
                FieldInsnNode putField = (FieldInsnNode) insn;
                for (int i = 0; i < pkfields.size(); i++) {
                    int fieldNum = pkfields.get(i);
                    // Compare the field being set with the current pk field
                    String parmName = fmds[fieldNum].getName();
                    Class<?> parmType = fmds[fieldNum].getType();
                    if (parmName.equals(putField.name)) {
                        // backup and examine the load instruction parm
                        if (AsmHelper.isLoadInsn(insn.getPrevious())) {
                            // Get the local index from the instruction.  This will be the index
                            // of the constructor parameter.  must be less than or equal to the
                            // max parm index to prevent from picking up locals that could have
                            // been produced within the constructor.  Also make sure the parm type
                            // matches the fmd type

                            VarInsnNode loadInsn = (VarInsnNode) insn.getPrevious();

                            int parm = AsmHelper.getParamIndex(ct, loadInsn.var);
                            if (parm < pkfields.size() && argTypes[parm].equals(Type.getType(parmType))) {
                                parmOrder[parmOrderIndex] = fieldNum;
                                parmOrderIndex++;
                            }
                        }
                        else {
                            // Some other instruction found. can't make a determination of which local/parm
                            // is being used on the putfield.
                            break;
                        }
                    }
                }

                insn = insn.getNext();
            }
            if (parmOrderIndex == pkfields.size()) {
                return parmOrder;
            }
        }

        return null;
    }

    private int listSize(Collection<?> coll) {
        return coll == null ? 0 : coll.size();
    }
}
