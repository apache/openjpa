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
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.meta.ClassArgParser;
import org.apache.openjpa.lib.util.BytecodeWriter;
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
import org.apache.openjpa.util.asm.ClassWriterTracker;
import org.apache.xbean.asm9.Opcodes;
import org.apache.xbean.asm9.Type;
import org.apache.xbean.asm9.tree.AbstractInsnNode;
import org.apache.xbean.asm9.tree.ClassNode;
import org.apache.xbean.asm9.tree.FieldInsnNode;
import org.apache.xbean.asm9.tree.FieldNode;
import org.apache.xbean.asm9.tree.IincInsnNode;
import org.apache.xbean.asm9.tree.InsnList;
import org.apache.xbean.asm9.tree.InsnNode;
import org.apache.xbean.asm9.tree.JumpInsnNode;
import org.apache.xbean.asm9.tree.LabelNode;
import org.apache.xbean.asm9.tree.LdcInsnNode;
import org.apache.xbean.asm9.tree.MethodInsnNode;
import org.apache.xbean.asm9.tree.MethodNode;
import org.apache.xbean.asm9.tree.TableSwitchInsnNode;
import org.apache.xbean.asm9.tree.TypeInsnNode;
import org.apache.xbean.asm9.tree.VarInsnNode;

import serp.bytecode.BCClass;
import serp.bytecode.BCField;
import serp.bytecode.BCMethod;
import serp.bytecode.Code;
import serp.bytecode.Constants;
import serp.bytecode.Exceptions;
import serp.bytecode.FieldInstruction;
import serp.bytecode.IfInstruction;
import serp.bytecode.Instruction;
import serp.bytecode.JumpInstruction;
import serp.bytecode.LoadInstruction;
import serp.bytecode.LookupSwitchInstruction;
import serp.bytecode.MethodInstruction;
import serp.bytecode.Project;
import serp.bytecode.PutFieldInstruction;
import serp.bytecode.TableSwitchInstruction;

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

    boolean _addVersionInitFlag = true;

    public static final int ENHANCE_NONE = 0;
    public static final int ENHANCE_AWARE = 2 << 0;
    public static final int ENHANCE_INTERFACE = 2 << 1;
    public static final int ENHANCE_PC = 2 << 2;

    public static final String PRE = "pc";
    public static final String ISDETACHEDSTATEDEFINITIVE = PRE
            + "isDetachedStateDefinitive";

    private static final Class PCTYPE = PersistenceCapable.class;
    private static final String SM = PRE + "StateManager";
    private static final Class SMTYPE = StateManager.class;
    private static final String INHERIT = PRE + "InheritedFieldCount";
    private static final String CONTEXTNAME = "GenericContext";
    private static final Class USEREXCEP = UserException.class;
    private static final Class INTERNEXCEP = InternalException.class;
    private static final Class HELPERTYPE = PCRegistry.class;
    private static final String SUPER = PRE + "PCSuperclass";
    private static final Class OIDFSTYPE = FieldSupplier.class;
    private static final Class OIDFCTYPE = FieldConsumer.class;

    private static final String VERSION_INIT_STR = PRE + "VersionInit";

    private static final Localizer _loc = Localizer.forPackage
            (PCEnhancer.class);
    private static final String REDEFINED_ATTRIBUTE
            = PCEnhancer.class.getName() + "#redefined-type";

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
                (new AuxiliaryEnhancer[auxEnhancers.size()]);

        int rev = 0;
        Properties revisionProps = new Properties();
        try {
            InputStream in = PCEnhancer.class.getResourceAsStream("/META-INF/org.apache.openjpa.revision.properties");
            if (in != null) {
                try {
                    revisionProps.load(in);
                }
                finally {
                    in.close();
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

    private BCClass _pc;
    private final BCClass _managedType;
    private final MetaDataRepository _repos;

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

    private final ClassMetaData _meta;
    private final Log _log;
    private Collection _oids = null;
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
    public PCEnhancer(OpenJPAConfiguration conf, Class type) {
        this(conf, AccessController.doPrivileged(J2DoPrivHelper
                                                         .loadProjectClassAction(new Project(), type)),
             (MetaDataRepository) null);
    }

    /**
     * Constructor. Supply configuration and type to enhance. This will look
     * up the metadata for <code>meta</code> by converting back to a class
     * and then loading from <code>conf</code>'s repository.
     */
    public PCEnhancer(OpenJPAConfiguration conf, ClassMetaData meta) {
        this(conf, AccessController.doPrivileged(J2DoPrivHelper
                                                         .loadProjectClassAction(new Project(), meta.getDescribedType())),
             meta.getRepository());
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
     * @deprecated use {@link #PCEnhancer(OpenJPAConfiguration, BCClass,
     * MetaDataRepository, ClassLoader)} instead.
     */
    @Deprecated
    public PCEnhancer(OpenJPAConfiguration conf, BCClass type,
                      MetaDataRepository repos) {
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
    public PCEnhancer(OpenJPAConfiguration conf, BCClass type, MetaDataRepository repos, ClassLoader loader) {
        _managedType = type;
        _pc = type;

        // we assume that the original class and the enhanced class is the same
        managedType = AsmHelper.toClassNode(type);
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
    public PCEnhancer(MetaDataRepository repos, BCClass type, ClassMetaData meta) {
        _managedType = type;
        _pc = type;

        // we assume that the original class and the enhanced class is the same
        managedType = AsmHelper.toClassNode(type);
        pc = managedType;

        _log = repos.getConfiguration()
                .getLog(OpenJPAConfiguration.LOG_ENHANCE);

        _repos = repos;
        _meta = meta;
    }

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
    public BCClass getPCBytecode() {
        return _pc;
    }

    /**
     * Return the bytecode representation of the managed class being
     * manipulated. This is usually the same as {@link #getPCBytecode},
     * except when running the enhancer to redefine and subclass
     * existing persistent types.
     */
    public BCClass getManagedTypeBytecode() {
        return _managedType;
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
        Class<?> type = _managedType.getType();
        try {
            // if enum, skip, no need of any meta
            if (type.isEnum())
                return ENHANCE_NONE;

            // if managed interface, skip
            if (type.isInterface())
                return ENHANCE_INTERFACE;

            // check if already enhanced
            // we cannot simply use instanceof or isAssignableFrom as we have a temp ClassLoader inbetween
            ClassLoader loader = AccessController.doPrivileged(J2DoPrivHelper.getClassLoaderAction(type));
            for (String iface : _managedType.getDeclaredInterfaceNames()) {
                if (iface.equals(PCTYPE.getName())) {
                    if (_log.isTraceEnabled()) {
                        _log.trace(_loc.get("pc-type", type, loader));
                    }
                    return ENHANCE_NONE;
                }
            }

            if (_log.isTraceEnabled()) {
                _log.trace(_loc.get("enhance-start", type));
            }


            configureBCs();

            // validate properties before replacing field access so that
            // we build up a record of backing fields, etc
            if (isPropertyAccess(_meta)) {
                validateProperties();
                AsmHelper.readIntoBCClass(pc, _pc);
                if (getCreateSubclass()) {
                    addAttributeTranslation();
                }
            }
            replaceAndValidateFieldAccess();
            processViolations();

            if (_meta != null) {
                pc = AsmHelper.toClassNode(_pc);
                enhanceClass(pc);
                addFields(pc);
                //X TODO finish addStaticInitializer(classNodeTracker);
                AsmHelper.readIntoBCClass(pc, _pc);
                addStaticInitializer();

                pc = AsmHelper.toClassNode(_pc);
                addPCMethods(pc);

                addAccessors();
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
                                                type.getName(), e.getMessage()), e);
        }
    }

    private void configureBCs() {
        if (!_bcsConfigured) {
            if (getRedefine()) {
                if (_managedType.getAttribute(REDEFINED_ATTRIBUTE) == null) {
                    _managedType.addAttribute(REDEFINED_ATTRIBUTE);
                }
                else {
                    _isAlreadyRedefined = true;
                }
            }

            if (getCreateSubclass()) {
                PCSubclassValidator val = new PCSubclassValidator(_meta, managedType.getClassNode(), _log, _fail);
                val.assertCanSubclass();

                _pc = _managedType.getProject().loadClass(toPCSubclassName(_managedType.getType()));
                _pc.setMajorVersion(_managedType.getMajorVersion());
                _pc.setMinorVersion(_managedType.getMinorVersion());
                if (_pc.getSuperclassBC() != _managedType) {
                    _pc.setSuperclass(_managedType);
                    _pc.setAbstract(_managedType.isAbstract());
                    _pc.declareInterface(DynamicPersistenceCapable.class);
                }
                else {
                    _isAlreadySubclassed = true;
                }
                pc = AsmHelper.toClassNode(_pc);
            }

            _bcsConfigured = true;
        }
    }

    /**
     * Write the generated bytecode.
     */
    public void record()
            throws IOException {
        if (_managedType != _pc && getRedefine()) {
            record(AsmHelper.toClassWriter(_managedType));
        }

        record(AsmHelper.toClassWriter(_pc));

        if (_oids != null)
            for (Object oid : _oids) {
                record(AsmHelper.toClassWriter((BCClass) oid));
            }
    }

    /**
     * Write the given class.
     */
    private void record(ClassWriterTracker cwt)
            throws IOException {
        if (_writer != null)
            _writer.write(AsmHelper.toBCClass(cwt));
        else if (_dir == null) {
            String name = cwt.getName().replace(".", "/");
            ClassLoader cl = cwt.getClassLoader();
            if (cl == null) {
                cl = Thread.currentThread().getContextClassLoader();
            }
            final URL resource = cl.getResource(name + ".class");
            try (OutputStream out = new FileOutputStream(URLDecoder.decode(resource.getFile()))) {
                out.write(cwt.getCw().toByteArray());
                out.flush();
            }
        }
        else {
            String name = cwt.getName().replace(".", "/") + ".class";
            File targetFile = new File(_dir, name);
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            java.nio.file.Files.write(targetFile.toPath(), cwt.getCw().toByteArray());
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
                if (setter != null)
                    registerBackingFieldInfo(fmd, setter, assigned);

                if (assigned != returned)
                    addViolation("property-setter-getter-mismatch", new Object[]
                            {fmd, assigned.getName(), (returned == null)
                                    ? null : returned.getName()}, false);
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
                if (isPropertyAccess(fmds[i]))
                    propFmds.add(i);
            }

            // if no fields have property access do not do attribute translation
            if (propFmds.size() == 0)
                return;
        }

        _pc.declareInterface(AttributeTranslator.class);
        BCMethod method = _pc.declareMethod(PRE + "AttributeIndexToFieldName",
                                            String.class, new Class[]{int.class});
        method.makePublic();
        Code code = method.getCode(true);

        // switch (val)
        code.iload().setParam(0);
        if (!_meta.isMixedAccess()) {
            // if not mixed access use a table switch on all property-based fmd.
            // a table switch is more efficient with +1 incremental operations
            TableSwitchInstruction tabins = code.tableswitch();

            tabins.setLow(0);
            tabins.setHigh(fmds.length - 1);

            // case i:
            //     return <_attrsToFields.get(fmds[i].getName())>
            for (FieldMetaData fmd : fmds) {
                tabins.addTarget(code.constant().setValue(
                        _attrsToFields.get(fmd.getName())));
                code.areturn();
            }
            // default: throw new IllegalArgumentException ()
            tabins.setDefaultTarget(throwException
                                            (code, IllegalArgumentException.class));
        }
        else {
            // In mixed access mode, property indexes are not +1 incremental
            // a lookup switch must be used to do indexed lookup.
            LookupSwitchInstruction lookupins = code.lookupswitch();

            for (Integer i : propFmds) {
                lookupins.addCase(i,
                                  code.constant().setValue(
                                          _attrsToFields.get(fmds[i].getName())));
                code.areturn();
            }
            // default: throw new IllegalArgumentException ()
            lookupins.setDefaultTarget(throwException
                                               (code, IllegalArgumentException.class));
        }

        code.calculateMaxLocals();
        code.calculateMaxStack();
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
        final MethodNode methodNode = classNode.methods.stream()
                .filter(mn -> mn.name.equals(meth.getName()) && mn.desc.equals(Type.getMethodDescriptor(meth)))
                .findAny().get();
        return methodNode;
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

    /**
     * get the Method with the given methodName and params from the given classNode or clazz
     * @param classNodeTracker
     * @param methodName
     */
    private static MethodNode getMethod(ClassNodeTracker classNodeTracker, String methodName, Class<?> returnType, Class<?>... paramTypes) {
        Type[] parms = Arrays.stream(paramTypes)
                .map(c -> Type.getType(c))
                .toArray(Type[]::new);

        String methodDescription = Type.getMethodDescriptor(Type.getType(returnType), parms);

        final Optional<MethodNode> methodNode = classNodeTracker.getClassNode().methods.stream()
                .filter(mn -> mn.name.equals(methodName) && mn.desc.equals(methodDescription))
                .findAny();
        if (methodNode.isPresent()) {
            return methodNode.get();
        }

        // otherwise look up the parent class hierarchy
        final String superName = classNodeTracker.getClassNode().superName;
        if (superName == null || superName.equals("java/lang/Object")) {
            // no need to dig further
            return null;
        }
        try {
            final Class<?> parentClass = classNodeTracker.getClassLoader().loadClass(superName.replace("/", "."));
            Method m = getMethod(parentClass, methodName, paramTypes);
            return new MethodNode(m.getModifiers(), m.getName(), Type.getMethodDescriptor(m), null, null);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException("Illegal superclass in ClassNode " + classNodeTracker.getClassNode().name, e);
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
        if (_violations == null)
            _violations = new HashSet();
        _violations.add(_loc.get(key, args));
        _fail |= fatal;
    }

    /**
     * Log / throw recorded property access violations.
     */
    private void processViolations() {
        if (_violations == null)
            return;

        String sep = J2DoPrivHelper.getLineSeparator();
        StringBuilder buf = new StringBuilder();
        for (Iterator itr = _violations.iterator(); itr.hasNext(); ) {
            buf.append(itr.next());
            if (itr.hasNext())
                buf.append(sep);
        }
        Message msg = _loc.get("property-violations", buf);

        if (_fail)
            throw new UserException(msg);
        if (_log.isWarnEnabled())
            _log.warn(msg);
    }

    /**
     * Replaced all direct access to managed fields with the appropriate
     * pcGet/pcSet method. Note that this includes access to fields
     * owned by PersistenceCapable classes other than this one.
     */
    private void replaceAndValidateFieldAccess() throws NoSuchMethodException {
        // create template putfield/getfield instructions to search for
        Code template = AccessController.doPrivileged(
                J2DoPrivHelper.newCodeAction());
        Instruction put = template.putfield();
        Instruction get = template.getfield();
        Instruction stat = template.invokestatic();

        // look through all methods; this is done before any methods are added
        // so we don't need to worry about excluding synthetic methods.
        BCMethod[] methods = _managedType.getDeclaredMethods();
        Code code;
        for (BCMethod method : methods) {
            code = method.getCode(false);

            // don't modify the methods specified by the auxiliary enhancers
            if (code != null && !skipEnhance(method)) {
                replaceAndValidateFieldAccess(code, get, true, stat);
                replaceAndValidateFieldAccess(code, put, false, stat);
            }
        }
    }

    /**
     * Replaces all instructions matching the given template in the given
     * code block with calls to the appropriate generated getter/setter.
     *
     * @param code the code block to modify; the code iterator will
     *             be placed before the first instruction on method start,
     *             and will be after the last instruction on method completion
     * @param ins  the template instruction to search for; either a
     *             getfield or putfield instruction
     * @param get  boolean indicating if this is a get instruction
     * @param stat template invokestatic instruction to replace with
     */
    private void replaceAndValidateFieldAccess(Code code, Instruction ins,
                                               boolean get, Instruction stat) throws NoSuchMethodException {
        code.beforeFirst();

        FieldInstruction fi;
        MethodInstruction mi;
        ClassMetaData owner;
        String name, typeName, methodName;
        while (code.searchForward(ins)) {
            // back up to the matched instruction
            fi = (FieldInstruction) code.previous();
            name = fi.getFieldName();
            typeName = fi.getFieldTypeName();
            owner = getPersistenceCapableOwner(name, fi.getFieldDeclarerType());
            FieldMetaData fmd = owner == null ? null : owner.getField(name);
            if (isPropertyAccess(fmd)) {
                // if we're directly accessing a field in another class
                // hierarchy that uses property access, something is wrong
                if (owner != _meta && owner.getDeclaredField(name) != null &&
                        _meta != null && !owner.getDescribedType()
                        .isAssignableFrom(_meta.getDescribedType()))
                    throw new UserException(_loc.get("property-field-access",
                                                     new Object[]{_meta, owner, name,
                                                             code.getMethod().getName()}));

                // if we're directly accessing a property-backing field outside
                // the property in our own class, notify user
                if (isBackingFieldOfAnotherProperty(name, code))
                    addViolation("property-field-access", new Object[]{_meta,
                            owner, name, code.getMethod().getName()}, false);
            }

            if (owner == null ||
                    owner.getDeclaredField(fromBackingFieldName(name)) == null) {
                // not persistent field?
                code.next();
                continue;
            }
            else if (!getRedefine() && !getCreateSubclass()
                    && isFieldAccess(fmd)) {
                // replace the instruction with a call to the generated access
                // method
                mi = (MethodInstruction) code.set(stat);

                // invoke the proper access method, whether getter or setter
                String prefix = (get) ? PRE + "Get" : PRE + "Set";
                methodName = prefix + name;
                if (get) {
                    mi.setMethod(getType(owner).getName(),
                                 methodName, typeName, new String[]
                                         {getType(owner).getName()});
                }
                else {
                    mi.setMethod(getType(owner).getName(),
                                 methodName, "void", new String[]
                                         {getType(owner).getName(), typeName});
                }
                code.next();
            }
            else if (getRedefine()) {
                name = fromBackingFieldName(name);
                if (get) {
                    addNotifyAccess(code, owner.getField(name));
                    code.next();
                }
                else {
                    // insert the set operations after the field mutation, but
                    // first load the old value for use in the
                    // StateManager.settingXXX method.
                    loadManagedInstance(code, false);
                    final FieldInstruction fFi = fi;
                    code.getfield().setField(
                            AccessController.doPrivileged(J2DoPrivHelper
                                                                  .getFieldInstructionFieldAction(fFi)));
                    int val = code.getNextLocalsIndex();
                    code.xstore().setLocal(val).setType(fi.getFieldType());

                    // move past the putfield
                    code.next();
                    addNotifyMutation(code, owner.getField(name), val, -1);
                }
            }
            else {
                code.next();
            }
            code.calculateMaxLocals();
            code.calculateMaxStack();
        }
    }

    private void addNotifyAccess(Code code, FieldMetaData fmd) {
        // PCHelper.accessingField(this, <absolute-index>);
        code.aload().setThis();
        code.constant().setValue(fmd.getIndex());
        code.invokestatic().setMethod(RedefinitionHelper.class,
                                      "accessingField", void.class,
                                      new Class[]{Object.class, int.class});
    }

    /**
     * This must be called after setting the value in the object.
     *
     * @param val   the position in the local variable table where the
     *              old value is stored
     * @param param the parameter position containing the new value, or
     *              -1 if the new value is unavailable and should therefore be looked
     *              up.
     * @throws NoSuchMethodException
     */
    private void addNotifyMutation(Code code, FieldMetaData fmd, int val,
                                   int param)
            throws NoSuchMethodException {
        // PCHelper.settingField(this, <absolute-index>, old, new);
        code.aload().setThis();
        code.constant().setValue(fmd.getIndex());
        Class type = fmd.getDeclaredType();
        // we only have special signatures for primitives and Strings
        if (!type.isPrimitive() && type != String.class)
            type = Object.class;
        code.xload().setLocal(val).setType(type);
        if (param == -1) {
            loadManagedInstance(code, false);
            addGetManagedValueCode(code, fmd);
        }
        else {
            code.xload().setParam(param).setType(type);
        }
        code.invokestatic().setMethod(RedefinitionHelper.class, "settingField",
                                      void.class, new Class[]{
                        Object.class, int.class, type, type
                });
    }

    /**
     * Return true if the given instruction accesses a field that is a backing
     * field of another property in this property-access class.
     */
    private boolean isBackingFieldOfAnotherProperty(String name, Code code) {
        String methName = code.getMethod().getName();
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
        if (f == null)
            return null;

        // managed interface
        if (_meta != null && _meta.getDescribedType().isInterface())
            return _meta;

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
    private void addPCMethods(ClassNodeTracker classNodeTracker) throws NoSuchMethodException {
        addClearFieldsMethod(classNodeTracker.getClassNode());

        addNewInstanceMethod(classNodeTracker.getClassNode(), true);
        addNewInstanceMethod(classNodeTracker.getClassNode(), false);

        addManagedFieldCountMethod(classNodeTracker.getClassNode());
        addReplaceFieldsMethods(classNodeTracker.getClassNode());
        addProvideFieldsMethods(classNodeTracker.getClassNode());

        addCopyFieldsMethod(classNodeTracker.getClassNode());

        AsmHelper.readIntoBCClass(classNodeTracker, _pc);

        if (_meta.getPCSuperclass() == null || getCreateSubclass()) {
            addStockMethods();
            addGetVersionMethod();
            addReplaceStateManagerMethod();

            if (_meta.getIdentityType() != ClassMetaData.ID_APPLICATION)
                addNoOpApplicationIdentityMethods();
        }

        // add the app id methods to each subclass rather
        // than just the superclass, since it is possible to have
        // a subclass with an app id hierarchy that matches the
        // persistent class inheritance hierarchy
        if (_meta.getIdentityType() == ClassMetaData.ID_APPLICATION
                && (_meta.getPCSuperclass() == null || getCreateSubclass() ||
                _meta.getObjectIdType() !=
                        _meta.getPCSuperclassMetaData().getObjectIdType())) {
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

        if (_pc.isAbstract()) {
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
        instructions.add(new LdcInsnNode(_meta.getDeclaredFields().length));
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
                    putfield(instructions, getType(_meta), VERSION_INIT_STR, boolean.class);
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
        if (fmds.length == 0)
            instructions.add(throwException(IllegalArgumentException.class));
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

        int fieldNumbersPos = copy? 2 :1;

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
    private void addStockMethods()
            throws NoSuchMethodException {
        try {
            // pcGetGenericContext
            translateFromStateManagerMethod(
                    AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                    SMTYPE, "get" + CONTEXTNAME, (Class[]) null)), false);

            // pcFetchObjectId
            translateFromStateManagerMethod(
                    AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                    SMTYPE, "fetchObjectId", (Class[]) null)), false);

            // pcIsDeleted
            translateFromStateManagerMethod(
                    AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                    SMTYPE, "isDeleted", (Class[]) null)), false);

            // pcIsDirty
            translateFromStateManagerMethod(
                    AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                    SMTYPE, "isDirty", (Class[]) null)), true);

            // pcIsNew
            translateFromStateManagerMethod(
                    AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                    SMTYPE, "isNew", (Class[]) null)), false);

            // pcIsPersistent
            translateFromStateManagerMethod(
                    AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                    SMTYPE, "isPersistent", (Class[]) null)), false);

            // pcIsTransactional
            translateFromStateManagerMethod(
                    AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                    SMTYPE, "isTransactional", (Class[]) null)), false);

            // pcSerializing
            translateFromStateManagerMethod(
                    AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                    SMTYPE, "serializing", (Class[]) null)), false);

            // pcDirty
            translateFromStateManagerMethod(
                    AccessController.doPrivileged(
                            J2DoPrivHelper.getDeclaredMethodAction(
                                    SMTYPE, "dirty", new Class[]{String.class})), false);

            // pcGetStateManager
            BCMethod meth = _pc.declareMethod(PRE + "GetStateManager",
                                              StateManager.class, null);
            Code code = meth.getCode(true);
            loadManagedInstance(code, false);
            code.getfield().setField(SM, StateManager.class);
            code.areturn();
            code.calculateMaxStack();
            code.calculateMaxLocals();
        }
        catch (PrivilegedActionException pae) {
            throw (NoSuchMethodException) pae.getException();
        }
    }

    /**
     * Helper method to add a stock method to the bytecode. Each
     * stock method simply delegates to a corresponding StateManager method.
     * Given the StateManager method, then, this function translates it into
     * the wrapper method that should be added to the bytecode.
     */
    private void translateFromStateManagerMethod(Method m,
                                                 boolean isDirtyCheckMethod) {
        // form the name of the method by prepending 'pc' to the sm method
        String name = PRE + StringUtil.capitalize(m.getName());
        Class[] params = m.getParameterTypes();
        Class returnType = m.getReturnType();

        // add the method to the pc
        BCMethod method = _pc.declareMethod(name, returnType, params);
        Code code = method.getCode(true);

        // if (pcStateManager == null) return <default>;
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        JumpInstruction ifins = code.ifnonnull();
        if (returnType.equals(boolean.class))
            code.constant().setValue(false);
        else if (!returnType.equals(void.class))
            code.constant().setNull();
        code.xreturn().setType(returnType);

        // if this is the dirty-check method and we're subclassing but not
        // redefining, hook into PCHelper to do the dirty check
        if (isDirtyCheckMethod && !getRedefine()) {
            // RedefinitionHelper.dirtyCheck(sm);
            ifins.setTarget(loadManagedInstance(code, false));
            code.getfield().setField(SM, SMTYPE);
            code.dup(); // for the return statement below
            code.invokestatic().setMethod(RedefinitionHelper.class,
                                          "dirtyCheck", void.class, new Class[]{SMTYPE});
        }
        else {
            ifins.setTarget(loadManagedInstance(code, false));
            code.getfield().setField(SM, SMTYPE);
        }

        // return pcStateManager.<method> (<args>);
        // managed instance loaded above in if-else block
        for (int i = 0; i < params.length; i++)
            code.xload().setParam(i);
        code.invokeinterface().setMethod(m);
        code.xreturn().setType(returnType);

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds the {@link PersistenceCapable#pcGetVersion} method to the bytecode.
     */
    private void addGetVersionMethod()
            throws NoSuchMethodException {
        BCMethod method = _pc.declareMethod(PRE + "GetVersion", Object.class,
                                            null);
        Code code = method.getCode(true);

        // if (pcStateManager == null)
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        JumpInstruction ifins = code.ifnonnull();
        FieldMetaData versionField = _meta.getVersionField();

        if (versionField == null)
            code.constant().setNull(); // return null;
        else {
            // return <versionField>;
            Class wrapper = toPrimitiveWrapper(versionField);
            if (wrapper != versionField.getDeclaredType()) {
                code.anew().setType(wrapper);
                code.dup();
            }
            loadManagedInstance(code, false);
            addGetManagedValueCode(code, versionField);
            if (wrapper != versionField.getDeclaredType())
                code.invokespecial().setMethod(wrapper, "<init>", void.class,
                                               new Class[]{versionField.getDeclaredType()});
        }
        code.areturn();

        // return pcStateManager.getVersion ();
        ifins.setTarget(loadManagedInstance(code, false));
        code.getfield().setField(SM, SMTYPE);
        code.invokeinterface().setMethod(SMTYPE, "getVersion", Object.class,
                                         null);
        code.areturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
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
        BCMethod method = _pc.declareMethod(PRE + "ReplaceStateManager",
                                            void.class, new Class[]{SMTYPE});
        method.getExceptions(true).addException(SecurityException.class);
        Code code = method.getCode(true);

        // if (pcStateManager != null)
        //    pcStateManager = pcStateManager.replaceStateManager(sm);
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        JumpInstruction ifins = code.ifnull();
        loadManagedInstance(code, false);
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        code.aload().setParam(0);
        code.invokeinterface().setMethod(SMTYPE, "replaceStateManager",
                                         SMTYPE, new Class[]{SMTYPE});
        code.putfield().setField(SM, SMTYPE);
        code.vreturn();

        // SecurityManager sec = System.getSecurityManager ();
        // if (sec != null)
        //        sec.checkPermission (Permission.SET_STATE_MANAGER);
        ifins.setTarget(code.invokestatic().setMethod(System.class,
                                                      "getSecurityManager", SecurityManager.class, null));

        // pcStateManager = sm;
        ifins.setTarget(loadManagedInstance(code, false));
        code.aload().setParam(0);
        code.putfield().setField(SM, SMTYPE);
        code.vreturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Creates the PersistenceCapable methods dealing with application
     * identity and gives them no-op implementations.
     */
    private void addNoOpApplicationIdentityMethods() {
        // public void pcCopyKeyFieldsToObjectId (ObjectIdFieldSupplier fs,
        //     Object oid)
        BCMethod method = _pc.declareMethod(PRE + "CopyKeyFieldsToObjectId",
                                            void.class, new Class[]{OIDFSTYPE, Object.class});
        Code code = method.getCode(true);
        code.vreturn();
        code.calculateMaxLocals();

        // public void pcCopyKeyFieldsToObjectId (Object oid)
        method = _pc.declareMethod(PRE + "CopyKeyFieldsToObjectId",
                                   void.class, new Class[]{Object.class});
        code = method.getCode(true);
        code.vreturn();
        code.calculateMaxLocals();

        // public void pcCopyKeyFieldsFromObjectId (ObjectIdFieldConsumer fc,
        //    Object oid)
        method = _pc.declareMethod(PRE + "CopyKeyFieldsFromObjectId",
                                   void.class, new Class[]{OIDFCTYPE, Object.class});
        code = method.getCode(true);
        code.vreturn();
        code.calculateMaxLocals();

        // public void pcCopyKeyFieldsFromObjectId (Object oid)
        method = _pc.declareMethod(PRE + "CopyKeyFieldsFromObjectId",
                                   void.class, new Class[]{Object.class});
        code = method.getCode(true);
        code.vreturn();
        code.calculateMaxLocals();

        // public Object pcNewObjectIdInstance ()
        method = _pc.declareMethod(PRE + "NewObjectIdInstance",
                                   Object.class, null);
        code = method.getCode(true);
        code.constant().setNull();
        code.areturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();

        // public Object pcNewObjectIdInstance (Object obj)
        method = _pc.declareMethod(PRE + "NewObjectIdInstance",
                                   Object.class, new Class[]{Object.class});
        code = method.getCode(true);
        code.constant().setNull();
        code.areturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds the <code>pcCopyKeyFieldsToObjectId</code> methods
     * to classes using application identity.
     */
    private void addCopyKeyFieldsToObjectIdMethod(boolean fieldManager)
            throws NoSuchMethodException {
        // public void pcCopyKeyFieldsToObjectId (ObjectIdFieldSupplier fs,
        //    Object oid)
        String[] args = (fieldManager) ?
                new String[]{OIDFSTYPE.getName(), Object.class.getName()}
                : new String[]{Object.class.getName()};
        BCMethod method = _pc.declareMethod(PRE + "CopyKeyFieldsToObjectId",
                                            void.class.getName(), args);
        Code code = method.getCode(true);

        // single field identity always throws exception
        if (_meta.isOpenJPAIdentity()) {
            throwException(code, INTERNEXCEP);

            code.calculateMaxStack();
            code.calculateMaxLocals();
            return;
        }

        // call superclass method
        if (_meta.getPCSuperclass() != null && !getCreateSubclass()) {
            loadManagedInstance(code, false);
            for (int i = 0; i < args.length; i++)
                code.aload().setParam(i);
            code.invokespecial().setMethod(getType(_meta.
                                                           getPCSuperclassMetaData()).getName(),
                                           PRE + "CopyKeyFieldsToObjectId", void.class.getName(), args);
        }

        // Object id = oid;
        if (fieldManager)
            code.aload().setParam(1);
        else
            code.aload().setParam(0);

        if (_meta.isObjectIdTypeShared()) {
            // oid = ((ObjectId) id).getId ();
            code.checkcast().setType(ObjectId.class);
            code.invokevirtual().setMethod(ObjectId.class, "getId",
                                           Object.class, null);
        }

        // <oid type> id = (<oid type>) oid;
        int id = code.getNextLocalsIndex();
        Class oidType = _meta.getObjectIdType();
        code.checkcast().setType(oidType);
        code.astore().setLocal(id);

        // int inherited = pcInheritedFieldCount;
        int inherited = 0;
        if (fieldManager) {
            code.getstatic().setField(INHERIT, int.class);
            inherited = code.getNextLocalsIndex();
            code.istore().setLocal(inherited);
        }

        // id.<field> = fs.fetch<type>Field (<index>); or...
        // id.<field> = pc.<field>;
        FieldMetaData[] fmds = getCreateSubclass() ? _meta.getFields()
                : _meta.getDeclaredFields();
        Class<?> type;
        String name;
        Field field;
        Method setter;
        boolean reflect;
        // If optimizeIdCopy is enabled and not a field manager method, try to
        // optimize the copyTo by using a public constructor instead of reflection
        if (_optimizeIdCopy) {
            ArrayList<Integer> pkfields = optimizeIdCopy(oidType, fmds);
            if (pkfields != null) {
                // search for a constructor on the IdClass that can be used
                // to construct the IdClass
                int parmOrder[] = getIdClassConstructorParmOrder(oidType, pkfields, fmds);
                if (parmOrder != null) {
                    // If using a field manager, values must be loaded into locals so they can be properly ordered
                    // as constructor parameters.
                    int[] localIndexes = new int[fmds.length];
                    if (fieldManager) {
                        for (int k = 0; k < fmds.length; k++) {
                            if (!fmds[k].isPrimaryKey())
                                continue;
                            code.aload().setParam(0);
                            code.constant().setValue(k);
                            code.iload().setLocal(inherited);
                            code.iadd();
                            code.invokeinterface().setMethod(getFieldSupplierMethod(fmds[k].getObjectIdFieldType()));
                            localIndexes[k] = code.getNextLocalsIndex();
                            storeLocalValue(code, localIndexes[k], fmds[k].getObjectIdFieldTypeCode());
                        }
                    }

                    // found a matching constructor.  parm array is constructor parm order
                    code.anew().setType(oidType);
                    code.dup();
                    // build the parm list in order
                    Class<?>[] clsArgs = new Class<?>[parmOrder.length];
                    for (int i = 0; i < clsArgs.length; i++) {
                        int parmIndex = parmOrder[i];
                        clsArgs[i] = fmds[parmIndex].getObjectIdFieldType();
                        if (!fieldManager) {
                            loadManagedInstance(code, false);
                            addGetManagedValueCode(code, fmds[parmIndex]);
                        }
                        else {
                            // Load constructor parameters in appropriate order
                            loadLocalValue(code, localIndexes[parmIndex], fmds[parmIndex].getObjectIdFieldTypeCode());
                            if (fmds[parmIndex].getObjectIdFieldTypeCode() == JavaTypes.OBJECT &&
                                    !fmds[parmIndex].getDeclaredType().isEnum()) {
                                code.checkcast().setType(ObjectId.class);
                                code.invokevirtual().setMethod(ObjectId.class, "getId",
                                                               Object.class, null);
                            }
                            // if the type of this field meta data is
                            // non-primitive and non-string, be sure to cast
                            // to the appropriate type.
                            if (!clsArgs[i].isPrimitive()
                                    && !clsArgs[i].getName().equals(String.class.getName()))
                                code.checkcast().setType(clsArgs[i]);
                        }
                    }
                    // invoke the public constructor to create a new local id
                    code.invokespecial().setMethod(oidType, "<init>", void.class, clsArgs);
                    int ret = code.getNextLocalsIndex();
                    code.astore().setLocal(ret);

                    // swap out the app id with the new one
                    code.aload().setLocal(fieldManager ? 2 : 1);
                    code.checkcast().setType(ObjectId.class);
                    code.aload().setLocal(ret);
                    code.invokestatic().setMethod(ApplicationIds.class,
                                                  "setAppId", void.class, new Class[]{ObjectId.class,
                                    Object.class});
                    code.vreturn();

                    code.calculateMaxStack();
                    code.calculateMaxLocals();
                    return;
                }
            }
        }

        for (int i = 0; i < fmds.length; i++) {
            if (!fmds[i].isPrimaryKey())
                continue;
            code.aload().setLocal(id);

            name = fmds[i].getName();
            type = fmds[i].getObjectIdFieldType();
            if (isFieldAccess(fmds[i])) {
                setter = null;
                field = Reflection.findField(oidType, name, true);
                reflect = !Modifier.isPublic(field.getModifiers());
                if (reflect) {
                    code.classconstant().setClass(oidType);
                    code.constant().setValue(name);
                    code.constant().setValue(true);
                    code.invokestatic().setMethod(Reflection.class,
                                                  "findField", Field.class, new Class[]{Class.class,
                                    String.class, boolean.class});
                }
            }
            else {
                field = null;
                setter = Reflection.findSetter(oidType, name, type, true);
                reflect = !Modifier.isPublic(setter.getModifiers());
                if (reflect) {
                    code.classconstant().setClass(oidType);
                    code.constant().setValue(name);
                    code.classconstant().setClass(type);
                    code.constant().setValue(true);
                    code.invokestatic().setMethod(Reflection.class,
                                                  "findSetter", Method.class, new Class[]{Class.class,
                                    String.class, Class.class, boolean.class});
                }
            }

            if (fieldManager) {
                code.aload().setParam(0);
                code.constant().setValue(i);
                code.iload().setLocal(inherited);
                code.iadd();
                code.invokeinterface().setMethod
                        (getFieldSupplierMethod(type));
                if (fmds[i].getObjectIdFieldTypeCode() == JavaTypes.OBJECT &&
                        !fmds[i].getDeclaredType().isEnum()) {
                    code.checkcast().setType(ObjectId.class);
                    code.invokevirtual().setMethod(ObjectId.class, "getId",
                                                   Object.class, null);
                }

                // if the type of this field meta data is
                // non-primitive and non-string, be sure to cast
                // to the appropriate type.
                if (!reflect && !type.isPrimitive()
                        && !type.getName().equals(String.class.getName()))
                    code.checkcast().setType(type);
            }
            else {
                loadManagedInstance(code, false);
                addGetManagedValueCode(code, fmds[i]);

                // get id/pk from pc instance
                if (fmds[i].getDeclaredTypeCode() == JavaTypes.PC)
                    addExtractObjectIdFieldValueCode(code, fmds[i]);
            }

            if (reflect && field != null) {
                code.invokestatic().setMethod(Reflection.class, "set",
                                              void.class, new Class[]{Object.class, Field.class,
                                (type.isPrimitive()) ? type : Object.class});
            }
            else if (reflect) {
                code.invokestatic().setMethod(Reflection.class, "set",
                                              void.class, new Class[]{Object.class, Method.class,
                                (type.isPrimitive()) ? type : Object.class});
            }
            else if (field != null)
                code.putfield().setField(field);
            else
                code.invokevirtual().setMethod(setter);
        }
        code.vreturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds the appropriate load method for the given type and local
     * index.
     */
    private void loadLocalValue(Code code, int locidx, int typeCode) {
        switch (typeCode) {
            case JavaTypes.CHAR:
            case JavaTypes.BYTE:
            case JavaTypes.SHORT:
            case JavaTypes.INT:
                code.iload().setLocal(locidx);
                break;
            case JavaTypes.DOUBLE:
                code.dload().setLocal(locidx);
                break;
            case JavaTypes.FLOAT:
                code.fload().setLocal(locidx);
                break;
            case JavaTypes.LONG:
                code.lload().setLocal(locidx);
                break;
            default:
                code.aload().setLocal(locidx);
                break;
        }
    }

    /**
     * Adds the appropriate store method for the given type and local
     * index.
     */
    private void storeLocalValue(Code code, int locidx, int typeCode) {
        switch (typeCode) {
            case JavaTypes.CHAR:
            case JavaTypes.BYTE:
            case JavaTypes.SHORT:
            case JavaTypes.INT:
                code.istore().setLocal(locidx);
                break;
            case JavaTypes.DOUBLE:
                code.dstore().setLocal(locidx);
                break;
            case JavaTypes.FLOAT:
                code.fstore().setLocal(locidx);
                break;
            case JavaTypes.LONG:
                code.lstore().setLocal(locidx);
                break;
            default:
                code.astore().setLocal(locidx);
                break;
        }
    }

    /**
     * Add code to extract the id of the given primary key relation field for
     * setting into an objectid instance.
     */
    private void addExtractObjectIdFieldValueCode(Code code, FieldMetaData pk) {
        // if (val != null)
        //  val = ((PersistenceCapable) val).pcFetchObjectId();
        int pc = code.getNextLocalsIndex();
        code.astore().setLocal(pc);
        code.aload().setLocal(pc);
        JumpInstruction ifnull1 = code.ifnull();
        code.aload().setLocal(pc);
        code.checkcast().setType(PersistenceCapable.class);
        if (!pk.getTypeMetaData().isOpenJPAIdentity())
            code.invokeinterface().setMethod(PersistenceCapable.class,
                                             PRE + "FetchObjectId", Object.class, null);
        else
            code.invokeinterface().setMethod(PersistenceCapable.class,
                                             PRE + "NewObjectIdInstance", Object.class, null);

        int oid = code.getNextLocalsIndex();
        code.astore().setLocal(oid);
        code.aload().setLocal(oid);
        JumpInstruction ifnull2 = code.ifnull();

        // for datastore / single-field identity:
        // if (val != null)
        //   val = ((OpenJPAId) val).getId();
        ClassMetaData pkmeta = pk.getDeclaredTypeMetaData();
        int pkcode = pk.getObjectIdFieldTypeCode();
        Class pktype = pk.getObjectIdFieldType();
        if (pkmeta.getIdentityType() == ClassMetaData.ID_DATASTORE
                && pkcode == JavaTypes.LONG) {
            code.aload().setLocal(oid);
            code.checkcast().setType(Id.class);
            code.invokevirtual().setMethod(Id.class, "getId",
                                           long.class, null);
        }
        else if (pkmeta.getIdentityType() == ClassMetaData.ID_DATASTORE) {
            code.aload().setLocal(oid);
        }
        else if (pkmeta.isOpenJPAIdentity()) {
            switch (pkcode) {
                case JavaTypes.BYTE_OBJ:
                    code.anew().setType(Byte.class);
                    code.dup();
                    // no break
                case JavaTypes.BYTE:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(ByteId.class);
                    code.invokevirtual().setMethod(ByteId.class, "getId",
                                                   byte.class, null);
                    if (pkcode == JavaTypes.BYTE_OBJ)
                        code.invokespecial().setMethod(Byte.class, "<init>",
                                                       void.class, new Class[]{byte.class});
                    break;
                case JavaTypes.CHAR_OBJ:
                    code.anew().setType(Character.class);
                    code.dup();
                    // no break
                case JavaTypes.CHAR:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(CharId.class);
                    code.invokevirtual().setMethod(CharId.class, "getId",
                                                   char.class, null);
                    if (pkcode == JavaTypes.CHAR_OBJ)
                        code.invokespecial().setMethod(Character.class,
                                                       "<init>", void.class, new Class[]{char.class});
                    break;
                case JavaTypes.DOUBLE_OBJ:
                    code.anew().setType(Double.class);
                    code.dup();
                    // no break
                case JavaTypes.DOUBLE:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(DoubleId.class);
                    code.invokevirtual().setMethod(DoubleId.class, "getId",
                                                   double.class, null);
                    if (pkcode == JavaTypes.DOUBLE_OBJ)
                        code.invokespecial().setMethod(Double.class, "<init>",
                                                       void.class, new Class[]{double.class});
                    break;
                case JavaTypes.FLOAT_OBJ:
                    code.anew().setType(Float.class);
                    code.dup();
                    // no break
                case JavaTypes.FLOAT:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(FloatId.class);
                    code.invokevirtual().setMethod(FloatId.class, "getId",
                                                   float.class, null);
                    if (pkcode == JavaTypes.FLOAT_OBJ)
                        code.invokespecial().setMethod(Float.class, "<init>",
                                                       void.class, new Class[]{float.class});
                    break;
                case JavaTypes.INT_OBJ:
                    code.anew().setType(Integer.class);
                    code.dup();
                    // no break
                case JavaTypes.INT:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(IntId.class);
                    code.invokevirtual().setMethod(IntId.class, "getId",
                                                   int.class, null);
                    if (pkcode == JavaTypes.INT_OBJ)
                        code.invokespecial().setMethod(Integer.class, "<init>",
                                                       void.class, new Class[]{int.class});
                    break;
                case JavaTypes.LONG_OBJ:
                    code.anew().setType(Long.class);
                    code.dup();
                    // no break
                case JavaTypes.LONG:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(LongId.class);
                    code.invokevirtual().setMethod(LongId.class, "getId",
                                                   long.class, null);
                    if (pkcode == JavaTypes.LONG_OBJ)
                        code.invokespecial().setMethod(Long.class, "<init>",
                                                       void.class, new Class[]{long.class});
                    break;
                case JavaTypes.SHORT_OBJ:
                    code.anew().setType(Short.class);
                    code.dup();
                    // no break
                case JavaTypes.SHORT:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(ShortId.class);
                    code.invokevirtual().setMethod(ShortId.class, "getId",
                                                   short.class, null);
                    if (pkcode == JavaTypes.SHORT_OBJ)
                        code.invokespecial().setMethod(Short.class, "<init>",
                                                       void.class, new Class[]{short.class});
                    break;
                case JavaTypes.DATE:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(DateId.class);
                    code.invokevirtual().setMethod(DateId.class, "getId",
                                                   Date.class, null);
                    if (pktype != Date.class) {
                        // java.sql.Date.class
                        code.checkcast().setType(pktype);
                    }
                    break;
                case JavaTypes.STRING:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(StringId.class);
                    code.invokevirtual().setMethod(StringId.class, "getId",
                                                   String.class, null);
                    break;
                case JavaTypes.BIGDECIMAL:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(BigDecimalId.class);
                    code.invokevirtual().setMethod(BigDecimalId.class, "getId",
                                                   BigDecimal.class, null);
                    break;
                case JavaTypes.BIGINTEGER:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(BigIntegerId.class);
                    code.invokevirtual().setMethod(BigIntegerId.class, "getId",
                                                   BigInteger.class, null);
                    break;
                default:
                    code.aload().setLocal(oid);
                    code.checkcast().setType(ObjectId.class);
                    code.invokevirtual().setMethod(ObjectId.class, "getId",
                                                   Object.class, null);
            }
        }
        else if (pkmeta.getObjectIdType() != null) {
            code.aload().setLocal(oid);
            if (pkcode == JavaTypes.OBJECT) {
                code.checkcast().setType(ObjectId.class);
                code.invokevirtual().setMethod(ObjectId.class, "getId",
                                               Object.class, null);
            }
            code.checkcast().setType(pktype);
        }
        else
            code.aload().setLocal(oid);
        JumpInstruction go2 = code.go2();

        // if (val == null)
        //   val = <default>;
        Instruction def;
        switch (pkcode) {
            case JavaTypes.BOOLEAN:
                def = code.constant().setValue(false);
                break;
            case JavaTypes.BYTE:
                def = code.constant().setValue((byte) 0);
                break;
            case JavaTypes.CHAR:
                def = code.constant().setValue((char) 0);
                break;
            case JavaTypes.DOUBLE:
                def = code.constant().setValue(0D);
                break;
            case JavaTypes.FLOAT:
                def = code.constant().setValue(0F);
                break;
            case JavaTypes.INT:
                def = code.constant().setValue(0);
                break;
            case JavaTypes.LONG:
                def = code.constant().setValue(0L);
                break;
            case JavaTypes.SHORT:
                def = code.constant().setValue((short) 0);
                break;
            default:
                def = code.constant().setNull();
        }
        ifnull1.setTarget(def);
        ifnull2.setTarget(def);
        go2.setTarget(code.nop());
    }

    /**
     * Adds the <code>pcCopyKeyFieldsFromObjectId</code> methods
     * to classes using application identity.
     */
    private void addCopyKeyFieldsFromObjectIdMethod(boolean fieldManager)
            throws NoSuchMethodException {
        // public void pcCopyKeyFieldsFromObjectId (ObjectIdFieldConsumer fc,
        //    Object oid)
        String[] args = (fieldManager)
                ? new String[]{OIDFCTYPE.getName(), Object.class.getName()}
                : new String[]{Object.class.getName()};
        BCMethod method = _pc.declareMethod(PRE + "CopyKeyFieldsFromObjectId",
                                            void.class.getName(), args);
        Code code = method.getCode(true);

        // call superclass method
        if (_meta.getPCSuperclass() != null && !getCreateSubclass()) {
            loadManagedInstance(code, false);
            for (int i = 0; i < args.length; i++)
                code.aload().setParam(i);
            code.invokespecial().setMethod(getType(_meta.
                                                           getPCSuperclassMetaData()).getName(),
                                           PRE + "CopyKeyFieldsFromObjectId", void.class.getName(), args);
        }

        if (fieldManager)
            code.aload().setParam(1);
        else
            code.aload().setParam(0);

        if (!_meta.isOpenJPAIdentity() && _meta.isObjectIdTypeShared()) {
            // oid = ((ObjectId) id).getId ();
            code.checkcast().setType(ObjectId.class);
            code.invokevirtual().setMethod(ObjectId.class, "getId",
                                           Object.class, null);
        }

        // <oid type> cast = (<oid type>) oid;
        int id = code.getNextLocalsIndex();
        Class oidType = _meta.getObjectIdType();
        code.checkcast().setType(oidType);
        code.astore().setLocal(id);

        // fs.store<type>Field (<index>, id.<field>); or...
        // this.<field> = id.<field>
        // or for single field identity: id.getId ()
        FieldMetaData[] fmds = getCreateSubclass() ? _meta.getFields()
                : _meta.getDeclaredFields();
        String name;
        Class type;
        Class unwrapped;
        Field field;
        Method getter;
        for (int i = 0; i < fmds.length; i++) {
            if (!fmds[i].isPrimaryKey())
                continue;

            name = fmds[i].getName();
            type = fmds[i].getObjectIdFieldType();
            if (!fieldManager
                    && fmds[i].getDeclaredTypeCode() == JavaTypes.PC) {
                // if (sm == null) return;
                loadManagedInstance(code, false);
                code.getfield().setField(SM, SMTYPE);
                JumpInstruction ifins = code.ifnonnull();
                code.vreturn();
                // sm.getPCPrimaryKey(oid, i + pcInheritedFieldCount);
                ifins.setTarget(loadManagedInstance(code, false));
                code.dup(); // leave orig on stack to set value into
                code.getfield().setField(SM, SMTYPE);
                code.aload().setLocal(id);
                code.constant().setValue(i);
                code.getstatic().setField(INHERIT, int.class);
                code.iadd();
                code.invokeinterface().setMethod(StateManager.class,
                                                 "getPCPrimaryKey", Object.class,
                                                 new Class[]{Object.class, int.class});
                code.checkcast().setType(fmds[i].getDeclaredType());
            }
            else {
                unwrapped = (fmds[i].getDeclaredTypeCode() == JavaTypes.PC)
                        ? type : unwrapSingleFieldIdentity(fmds[i]);
                if (fieldManager) {
                    code.aload().setParam(0);
                    code.constant().setValue(i);
                    code.getstatic().setField(INHERIT, int.class);
                    code.iadd();
                }
                else
                    loadManagedInstance(code, false);

                if (unwrapped != type) {
                    code.anew().setType(type);
                    code.dup();
                }
                code.aload().setLocal(id);
                if (_meta.isOpenJPAIdentity()) {
                    if (oidType == ObjectId.class) {
                        code.invokevirtual().setMethod(oidType, "getId",
                                                       Object.class, null);
                        if (!fieldManager && type != Object.class)
                            code.checkcast().setType(fmds[i].getDeclaredType());
                    }
                    else if (oidType == DateId.class) {
                        code.invokevirtual().setMethod(oidType, "getId",
                                                       Date.class, null);
                        if (!fieldManager && type != Date.class)
                            code.checkcast().setType(fmds[i].getDeclaredType());
                    }
                    else {
                        code.invokevirtual().setMethod(oidType, "getId",
                                                       unwrapped, null);
                        if (unwrapped != type)
                            code.invokespecial().setMethod(type, "<init>",
                                                           void.class, new Class[]{unwrapped});
                    }
                }
                else if (isFieldAccess(fmds[i])) {
                    field = Reflection.findField(oidType, name, true);
                    if (Modifier.isPublic(field.getModifiers()))
                        code.getfield().setField(field);
                    else {
                        boolean usedFastOid = false;
                        if (_optimizeIdCopy) {
                            // If fastOids, ignore access type and try to use a public getter
                            getter = Reflection.findGetter(oidType, name, false);
                            if (getter != null && Modifier.isPublic(getter.getModifiers())) {
                                usedFastOid = true;
                                code.invokevirtual().setMethod(getter);
                            }
                        }
                        if (!usedFastOid) {
                            // Reflection.getXXX(oid, Reflection.findField(...));
                            code.classconstant().setClass(oidType);
                            code.constant().setValue(name);
                            code.constant().setValue(true);
                            code.invokestatic().setMethod(Reflection.class,
                                                          "findField", Field.class, new Class[]{
                                            Class.class, String.class, boolean.class});
                            code.invokestatic().setMethod
                                    (getReflectionGetterMethod(type, Field.class));
                            if (!type.isPrimitive() && type != Object.class)
                                code.checkcast().setType(type);
                        }
                    }
                }
                else {
                    getter = Reflection.findGetter(oidType, name, true);
                    if (Modifier.isPublic(getter.getModifiers()))
                        code.invokevirtual().setMethod(getter);
                    else {
                        // Reflection.getXXX(oid, Reflection.findGetter(...));
                        code.classconstant().setClass(oidType);
                        code.constant().setValue(name);
                        code.constant().setValue(true);
                        code.invokestatic().setMethod(Reflection.class,
                                                      "findGetter", Method.class, new Class[]{
                                        Class.class, String.class, boolean.class});
                        code.invokestatic().setMethod
                                (getReflectionGetterMethod(type, Method.class));
                        if (!type.isPrimitive() && type != Object.class)
                            code.checkcast().setType(type);
                    }
                }
            }

            if (fieldManager)
                code.invokeinterface().setMethod(getFieldConsumerMethod(type));
            else
                addSetManagedValueCode(code, fmds[i]);
        }
        code.vreturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Return if the class uses the Class/String constructor
     * instead of just String.
     */
    private Boolean usesClassStringIdConstructor() {
        if (_meta.getIdentityType() != ClassMetaData.ID_APPLICATION)
            return Boolean.FALSE;

        if (_meta.isOpenJPAIdentity()) {
            if (_meta.getObjectIdType() == ObjectId.class)
                return null;
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
        if (!fmd.getDefiningMetaData().isOpenJPAIdentity())
            return fmd.getDeclaredType();

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
        if (type.isPrimitive())
            name += StringUtil.capitalize(type.getName());
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
    private void addNewObjectIdInstanceMethod(boolean obj)
            throws NoSuchMethodException {
        // public Object pcNewObjectIdInstance ()
        Class[] args = (obj) ? new Class[]{Object.class} : null;
        BCMethod method = _pc.declareMethod(PRE + "NewObjectIdInstance",
                                            Object.class, args);
        Code code = method.getCode(true);

        Boolean usesClsString = usesClassStringIdConstructor();
        Class oidType = _meta.getObjectIdType();
        if (obj && usesClsString == null) {
            // throw new IllegalArgumentException (...);
            String msg = _loc.get("str-cons", oidType,
                                  _meta.getDescribedType()).getMessage();
            code.anew().setType(IllegalArgumentException.class);
            code.dup();
            code.constant().setValue(msg);
            code.invokespecial().setMethod(IllegalArgumentException.class,
                                           "<init>", void.class, new Class[]{String.class});
            code.athrow();

            code.calculateMaxStack();
            code.calculateMaxLocals();
            return;
        }

        if (!_meta.isOpenJPAIdentity() && _meta.isObjectIdTypeShared()) {
            // new ObjectId (cls, oid)
            code.anew().setType(ObjectId.class);
            code.dup();
            if (_meta.isEmbeddedOnly() || _meta.hasAbstractPKField()) {
                code.aload().setThis();
                code.invokevirtual().setMethod(PRE + "GetIDOwningClass",
                                               Class.class, null);
            }
            else {
                code.classconstant().setClass(getType(_meta));
            }
        }

        // new <oid class> ();
        code.anew().setType(oidType);
        code.dup();
        if (_meta.isOpenJPAIdentity() || (obj && usesClsString == Boolean.TRUE)) {
            if ((_meta.isEmbeddedOnly()
                    && !(_meta.isEmbeddable() && _meta.getIdentityType() == ClassMetaData.ID_APPLICATION))
                    || _meta.hasAbstractPKField()) {
                code.aload().setThis();
                code.invokevirtual().setMethod(PRE + "GetIDOwningClass", Class.class, null);
            }
            else {
                code.classconstant().setClass(getType(_meta));
            }
        }
        if (obj) {
            code.aload().setParam(0);
            code.checkcast().setType(String.class);
            if (usesClsString == Boolean.TRUE)
                args = new Class[]{Class.class, String.class};
            else if (usesClsString == Boolean.FALSE)
                args = new Class[]{String.class};
        }
        else if (_meta.isOpenJPAIdentity()) {
            // new <type>Identity (XXX.class, <pk>);
            loadManagedInstance(code, false);
            FieldMetaData pk = _meta.getPrimaryKeyFields()[0];
            addGetManagedValueCode(code, pk);
            if (pk.getDeclaredTypeCode() == JavaTypes.PC)
                addExtractObjectIdFieldValueCode(code, pk);
            if (_meta.getObjectIdType() == ObjectId.class)
                args = new Class[]{Class.class, Object.class};
            else if (_meta.getObjectIdType() == Date.class)
                args = new Class[]{Class.class, Date.class};
            else
                args = new Class[]{Class.class, pk.getObjectIdFieldType()};
        }

        code.invokespecial().setMethod(oidType, "<init>", void.class, args);
        if (!_meta.isOpenJPAIdentity() && _meta.isObjectIdTypeShared())
            code.invokespecial().setMethod(ObjectId.class, "<init>",
                                           void.class, new Class[]{Class.class, Object.class});
        code.areturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
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
        if (type.isPrimitive())
            typeName = typeName.substring(0, 1).toUpperCase(Locale.ENGLISH)
                    + typeName.substring(1);
        else if (type.equals(String.class))
            typeName = "String";
        else {
            typeName = "Object";
            type = Object.class;
        }

        // the field index is always passed as an arg; the pc instance and
        // the current value may be passed; if setting the new value is
        // also passed
        List plist = new ArrayList(4);
        if (haspc)
            plist.add(PCTYPE);
        plist.add(int.class);
        if (!get || curValue)
            plist.add(type);
        if (!get && curValue) {
            plist.add(type);
            plist.add(int.class);
        }

        // use reflection to return the right method
        String name = prefix + typeName + "Field";
        Class[] params = (Class[]) plist.toArray(new Class[plist.size()]);

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
        InsnList instructions = new InsnList();
        instructions.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(type)));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                            Type.getInternalName(type),
                                            "<init>",
                                            Type.getMethodDescriptor(Type.VOID_TYPE)));
        instructions.add(new InsnNode(Opcodes.ATHROW));

        return instructions;
    }

    /**
     * Helper method to add the code necessary to throw the given
     * exception type, sans message.
     */
    private Instruction throwException(Code code, Class type) {
        Instruction ins = code.anew().setType(type);
        code.dup();
        code.invokespecial().setMethod(type, "<init>", void.class, null);
        code.athrow();
        return ins;
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
            else if (_pc.isFinal()) {
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

    @Deprecated
    private void addStaticInitializer() {
        Code code = getOrCreateClassInitCode(true);
        if (_meta.getPCSuperclass() != null) {
            if (getCreateSubclass()) {
                code.constant().setValue(0);
                code.putstatic().setField(INHERIT, int.class);
            }
            else {
                // pcInheritedFieldCount = <superClass>.pcGetManagedFieldCount()
                code.invokestatic().setMethod(getType(_meta.
                                                              getPCSuperclassMetaData()).getName(),
                                              PRE + "GetManagedFieldCount", int.class.getName(), null);
                code.putstatic().setField(INHERIT, int.class);
            }

            // pcPCSuperclass = <superClass>;
            // this intentionally calls getDescribedType() directly
            // instead of PCEnhancer.getType()
            code.classconstant().setClass(
                    _meta.getPCSuperclassMetaData().getDescribedType());
            code.putstatic().setField(SUPER, Class.class);
        }

        // pcFieldNames = new String[] { "<name1>", "<name2>", ... };
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        code.constant().setValue(fmds.length);
        code.anewarray().setType(String.class);
        for (int i = 0; i < fmds.length; i++) {
            code.dup();
            code.constant().setValue(i);
            code.constant().setValue(fmds[i].getName());
            code.aastore();
        }
        code.putstatic().setField(PRE + "FieldNames", String[].class);

        // pcFieldTypes = new Class[] { <type1>.class, <type2>.class, ... };
        code.constant().setValue(fmds.length);
        code.anewarray().setType(Class.class);
        for (int i = 0; i < fmds.length; i++) {
            code.dup();
            code.constant().setValue(i);
            code.classconstant().setClass(fmds[i].getDeclaredType());
            code.aastore();
        }
        code.putstatic().setField(PRE + "FieldTypes", Class[].class);

        // pcFieldFlags = new byte[] { <flag1>, <flag2>, ... };
        code.constant().setValue(fmds.length);
        code.newarray().setType(byte.class);
        for (int i = 0; i < fmds.length; i++) {
            code.dup();
            code.constant().setValue(i);
            code.constant().setValue(getFieldFlag(fmds[i]));
            code.bastore();
        }
        code.putstatic().setField(PRE + "FieldFlags", byte[].class);

        // PCRegistry.register (cls,
        //	pcFieldNames, pcFieldTypes, pcFieldFlags,
        //  pcPCSuperclass, alias, new XXX ());
        code.classconstant().setClass(_meta.getDescribedType());
        code.getstatic().setField(PRE + "FieldNames", String[].class);
        code.getstatic().setField(PRE + "FieldTypes", Class[].class);
        code.getstatic().setField(PRE + "FieldFlags", byte[].class);
        code.getstatic().setField(SUPER, Class.class);

        if (_meta.isMapped() || _meta.isAbstract())
            code.constant().setValue(_meta.getTypeAlias());
        else
            code.constant().setNull();

        if (_pc.isAbstract())
            code.constant().setNull();
        else {
            code.anew().setType(_pc);
            code.dup();
            code.invokespecial().setMethod("<init>", void.class, null);
        }

        code.invokestatic().setMethod(HELPERTYPE, "register", void.class,
                                      new Class[]{Class.class, String[].class, Class[].class,
                                              byte[].class, Class.class, String.class, PCTYPE});

        code.vreturn();
        code.calculateMaxStack();
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
                instructions.add(new LdcInsnNode(0));
                instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
            }
            else {
                // pcInheritedFieldCount = <superClass>.pcGetManagedFieldCount()
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, classNode.superName,
                                                    PRE + "GetManagedFieldCount",
                                                    Type.getMethodDescriptor(Type.INT_TYPE)));
                instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, INHERIT, Type.INT_TYPE.getDescriptor()));
            }

            // pcPCSuperclass = <superClass>;
            // this intentionally calls getDescribedType() directly
            // instead of PCEnhancer.getType()
            instructions.add(new LdcInsnNode(Type.getType(_meta.getPCSuperclassMetaData().getDescribedType())));
            instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, classNode.name, SUPER, Type.getDescriptor(Class.class)));
        }

        // pcFieldNames = new String[] { "<name1>", "<name2>", ... };
        FieldMetaData[] fmds = _meta.getDeclaredFields();

/*

        // pcFieldNames = new String[] { "<name1>", "<name2>", ... };
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        code.constant().setValue(fmds.length);
        code.anewarray().setType(String.class);
        for (int i = 0; i < fmds.length; i++) {
            code.dup();
            code.constant().setValue(i);
            code.constant().setValue(fmds[i].getName());
            code.aastore();
        }
        code.putstatic().setField(PRE + "FieldNames", String[].class);

        // pcFieldTypes = new Class[] { <type1>.class, <type2>.class, ... };
        code.constant().setValue(fmds.length);
        code.anewarray().setType(Class.class);
        for (int i = 0; i < fmds.length; i++) {
            code.dup();
            code.constant().setValue(i);
            code.classconstant().setClass(fmds[i].getDeclaredType());
            code.aastore();
        }
        code.putstatic().setField(PRE + "FieldTypes", Class[].class);

        // pcFieldFlags = new byte[] { <flag1>, <flag2>, ... };
        code.constant().setValue(fmds.length);
        code.newarray().setType(byte.class);
        for (int i = 0; i < fmds.length; i++) {
            code.dup();
            code.constant().setValue(i);
            code.constant().setValue(getFieldFlag(fmds[i]));
            code.bastore();
        }
        code.putstatic().setField(PRE + "FieldFlags", byte[].class);

        // PCRegistry.register (cls,
        //    pcFieldNames, pcFieldTypes, pcFieldFlags,
        //  pcPCSuperclass, alias, new XXX ());
        code.classconstant().setClass(_meta.getDescribedType());
        code.getstatic().setField(PRE + "FieldNames", String[].class);
        code.getstatic().setField(PRE + "FieldTypes", Class[].class);
        code.getstatic().setField(PRE + "FieldFlags", byte[].class);
        code.getstatic().setField(SUPER, Class.class);

        if (_meta.isMapped() || _meta.isAbstract())
            code.constant().setValue(_meta.getTypeAlias());
        else
            code.constant().setNull();

        if (_pc.isAbstract())
            code.constant().setNull();
        else {
            code.anew().setType(_pc);
            code.dup();
            code.invokespecial().setMethod("<init>", void.class, null);
        }

        code.invokestatic().setMethod(HELPERTYPE, "register", void.class,
            new Class[]{ Class.class, String[].class, Class[].class,
                byte[].class, Class.class, String.class, PCTYPE });

        code.vreturn();
        code.calculateMaxStack();
*/

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
        if (fmd.getManagement() == FieldMetaData.MANAGE_NONE)
            return -1;

        byte flags = 0;
        if (fmd.getDeclaredType().isPrimitive()
                || Serializable.class.isAssignableFrom(fmd.getDeclaredType()))
            flags = PersistenceCapable.SERIALIZABLE;

        if (fmd.getManagement() == FieldMetaData.MANAGE_TRANSACTIONAL)
            flags |= PersistenceCapable.CHECK_WRITE;
        else if (!fmd.isPrimaryKey() && !fmd.isInDefaultFetchGroup())
            flags |= PersistenceCapable.CHECK_WRITE
                    | PersistenceCapable.CHECK_READ;
        else
            flags |= PersistenceCapable.MEDIATE_WRITE
                    | PersistenceCapable.MEDIATE_READ;
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
        if (externalizeDetached()
                || !Serializable.class.isAssignableFrom(_meta.getDescribedType()))
            return;

        if (getCreateSubclass()) {
            // ##### what should happen if a type is Externalizable? It looks
            // ##### like Externalizable classes will not be serialized as PCs
            // ##### based on this logic.
            if (!Externalizable.class.isAssignableFrom(
                    _meta.getDescribedType()))
                addSubclassSerializationCode();
            return;
        }

        // if not already present, add a serialVersionUID field; if the instance
        // is detachable and uses detached state without a declared field,
        // can't add a serial version UID because we'll be adding extra fields
        // to the enhanced version
        BCField field = _pc.getDeclaredField("serialVersionUID");
        if (field == null) {
            Long uid = null;
            try {
                uid = ObjectStreamClass.lookup
                        (_meta.getDescribedType()).getSerialVersionUID();
            }
            catch (Throwable t) {
                // last-chance catch for bug #283 (which can happen
                // in a variety of ClassLoading environments)
                if (_log.isTraceEnabled())
                    _log.warn(_loc.get("enhance-uid-access", _meta), t);
                else
                    _log.warn(_loc.get("enhance-uid-access", _meta));
            }

            // if we couldn't access the serialVersionUID, we will have to
            // skip the override of that field and not be serialization
            // compatible with non-enhanced classes
            if (uid != null) {
                field = _pc.declareField("serialVersionUID", long.class);
                field.makePrivate();
                field.setStatic(true);
                field.setFinal(true);

                Code code = getOrCreateClassInitCode(false);
                code.beforeFirst();
                code.constant().setValue(uid.longValue());
                code.putstatic().setField(field);

                code.calculateMaxStack();
            }
        }

        // add write object method
        BCMethod write = _pc.getDeclaredMethod("writeObject",
                                               new Class[]{ObjectOutputStream.class});
        boolean full = write == null;
        if (full) {
            // private void writeObject (ObjectOutputStream out)
            write = _pc.declareMethod("writeObject", void.class,
                                      new Class[]{ObjectOutputStream.class});
            write.getExceptions(true).addException(IOException.class);
            write.makePrivate();
        }
        modifyWriteObjectMethod(write, full);

        // and read object
        BCMethod read = _pc.getDeclaredMethod("readObject",
                                              new Class[]{ObjectInputStream.class});
        full = read == null;
        if (full) {
            // private void readObject (ObjectInputStream in)
            read = _pc.declareMethod("readObject", void.class,
                                     new Class[]{ObjectInputStream.class});
            read.getExceptions(true).addException(IOException.class);
            read.getExceptions(true).addException
                    (ClassNotFoundException.class);
            read.makePrivate();
        }
        modifyReadObjectMethod(read, full);
    }

    private void addSubclassSerializationCode() {
        // for generated subclasses, serialization must write an instance of
        // the superclass instead of the subclass, so that the client VM can
        // deserialize successfully.

        // private Object writeReplace() throws ObjectStreamException
        BCMethod method = _pc.declareMethod("writeReplace", Object.class, null);
        method.getExceptions(true).addException(ObjectStreamException.class);
        Code code = method.getCode(true);

        // Object o = new <managed-type>()
        code.anew().setType(_managedType); // for return
        code.dup(); // for post-<init> work
        code.dup(); // for <init>
        code.invokespecial().setMethod(_managedType.getType(), "<init>",
                                       void.class, null);

        // copy all the fields.
        // ##### limiting to JPA @Transient limitations
        FieldMetaData[] fmds = _meta.getFields();
        for (FieldMetaData fmd : fmds) {
            if (fmd.isTransient())
                continue;
            // o.<field> = this.<field> (or reflective analog)
            code.dup(); // for putfield
            code.aload().setThis(); // for getfield
            getfield(code, _managedType, fmd.getName());
            putfield(code, _managedType, fmd.getName(),
                     fmd.getDeclaredType());
        }

        code.areturn().setType(Object.class);

        code.calculateMaxLocals();
        code.calculateMaxStack();
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
    private void modifyWriteObjectMethod(BCMethod method, boolean full) {
        Code code = method.getCode(true);
        code.beforeFirst();

        // bool clear = pcSerializing ();
        loadManagedInstance(code, false);
        code.invokevirtual().setMethod(PRE + "Serializing",
                                       boolean.class, null);
        int clear = code.getNextLocalsIndex();
        code.istore().setLocal(clear);

        if (full) {
            // out.defaultWriteObject ();
            code.aload().setParam(0);
            code.invokevirtual().setMethod(ObjectOutputStream.class,
                                           "defaultWriteObject", void.class, null);
            code.vreturn();
        }

        Instruction tmplate = (AccessController.doPrivileged(
                J2DoPrivHelper.newCodeAction())).vreturn();
        JumpInstruction toret;
        Instruction ret;
        code.beforeFirst();
        while (code.searchForward(tmplate)) {
            ret = code.previous();
            // if (clear) pcSetDetachedState (null);
            code.iload().setLocal(clear);
            toret = code.ifeq();
            loadManagedInstance(code, false);
            code.constant().setNull();
            code.invokevirtual().setMethod(PRE + "SetDetachedState",
                                           void.class, new Class[]{Object.class});
            toret.setTarget(ret);
            code.next(); // jump over return
        }
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds a custom readObject method that delegates to the
     * {@link ObjectInputStream#readObject()} method.
     */
    private void modifyReadObjectMethod(BCMethod method, boolean full) {
        Code code = method.getCode(true);
        code.beforeFirst();

        // if this instance uses synthetic detached state, note that it has
        // been deserialized
        if (ClassMetaData.SYNTHETIC.equals(_meta.getDetachedState())) {
            loadManagedInstance(code, false);
            code.getstatic().setField(PersistenceCapable.class,
                                      "DESERIALIZED", Object.class);
            code.invokevirtual().setMethod(PRE + "SetDetachedState",
                                           void.class, new Class[]{Object.class});
        }

        if (full) {
            // in.defaultReadObject ();
            code.aload().setParam(0);
            code.invokevirtual().setMethod(ObjectInputStream.class,
                                           "defaultReadObject", void.class, null);
            code.vreturn();
        }

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Creates the pcIsDetached() method to determine if an instance
     * is detached.
     */
    private void addIsDetachedMethod()
            throws NoSuchMethodException {
        // public boolean pcIsDetached()
        BCMethod method = _pc.declareMethod(PRE + "IsDetached",
                                            Boolean.class, null);
        method.makePublic();
        Code code = method.getCode(true);
        boolean needsDefinitiveMethod = writeIsDetachedMethod(code);
        code.calculateMaxStack();
        code.calculateMaxLocals();
        if (!needsDefinitiveMethod)
            return;

        // private boolean pcIsDetachedStateDefinitive()
        //   return false;
        // auxilliary enhancers may change the return value of this method
        // if their specs consider detached state definitive
        method = _pc.declareMethod(ISDETACHEDSTATEDEFINITIVE, boolean.class,
                                   null);
        method.makePrivate();
        code = method.getCode(true);
        code.constant().setValue(false);
        code.ireturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Creates the body of the pcIsDetached() method to determine if an
     * instance is detached.
     *
     * @return true if we need a pcIsDetachedStateDefinitive method, false
     * otherwise
     */
    private boolean writeIsDetachedMethod(Code code)
            throws NoSuchMethodException {
        // not detachable: return Boolean.FALSE
        if (!_meta.isDetachable()) {
            code.getstatic().setField(Boolean.class, "FALSE", Boolean.class);
            code.areturn();
            return false;
        }

        // if (sm != null)
        //     return (sm.isDetached ()) ? Boolean.TRUE : Boolean.FALSE;
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        JumpInstruction ifins = code.ifnull();
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        code.invokeinterface().setMethod(SMTYPE, "isDetached",
                                         boolean.class, null);
        JumpInstruction iffalse = code.ifeq();
        code.getstatic().setField(Boolean.class, "TRUE", Boolean.class);
        code.areturn();
        iffalse.setTarget(code.getstatic().setField(Boolean.class, "FALSE",
                                                    Boolean.class));
        code.areturn();

        // if we use detached state:
        // if (pcGetDetachedState () != null
        //     && pcGetDetachedState != DESERIALIZED)
        //     return Boolean.TRUE;
        Boolean state = _meta.usesDetachedState();
        JumpInstruction notdeser = null;
        Instruction target;
        if (state != Boolean.FALSE) {
            ifins.setTarget(loadManagedInstance(code, false));
            code.invokevirtual().setMethod(PRE + "GetDetachedState",
                                           Object.class, null);
            ifins = code.ifnull();
            loadManagedInstance(code, false);
            code.invokevirtual().setMethod(PRE + "GetDetachedState",
                                           Object.class, null);
            code.getstatic().setField(PersistenceCapable.class,
                                      "DESERIALIZED", Object.class);
            notdeser = code.ifacmpeq();
            code.getstatic().setField(Boolean.class, "TRUE", Boolean.class);
            code.areturn();

            if (state == Boolean.TRUE) {
                // if we have to use detached state:
                // return Boolean.FALSE;
                target = code.getstatic().setField(Boolean.class, "FALSE",
                                                   Boolean.class);
                ifins.setTarget(target);
                notdeser.setTarget(target);
                code.areturn();
                return false;
            }
        }

        // create artificial target to simplify
        target = code.nop();
        ifins.setTarget(target);
        if (notdeser != null)
            notdeser.setTarget(target);

        // allow users with version or auto-assigned pk fields to manually
        // construct a "detached" instance, so check these before taking into
        // account non-existent detached state

        // consider detached if version is non-default
        FieldMetaData version = _meta.getVersionField();
        if (state != Boolean.TRUE && version != null) {
            // if (<version> != <default>)
            //        return true;
            loadManagedInstance(code, false);
            addGetManagedValueCode(code, version);
            ifins = ifDefaultValue(code, version);
            code.getstatic().setField(Boolean.class, "TRUE", Boolean.class);
            code.areturn();
            if (!_addVersionInitFlag) {
                // else return false;
                ifins.setTarget(code.getstatic().setField(Boolean.class, "FALSE", Boolean.class));
            }
            else {
                // noop
                ifins.setTarget(code.nop());
                // if (pcVersionInit != false)
                // return true
                // else return null; //  (returning null because we don't know the correct answer)
                loadManagedInstance(code, false);
                getfield(code, null, VERSION_INIT_STR);
                ifins = code.ifeq();
                code.getstatic().setField(Boolean.class, "TRUE", Boolean.class);
                code.areturn();
                ifins.setTarget(code.nop());
                code.constant().setNull();
            }
            code.areturn();
            return false;
        }

        // consider detached if auto-genned primary keys are non-default
        ifins = null;
        JumpInstruction ifins2 = null;
        boolean hasAutoAssignedPK = false;
        if (state != Boolean.TRUE
                && _meta.getIdentityType() == ClassMetaData.ID_APPLICATION) {
            // for each pk field:
            // if (<pk> != <default> [&& !"".equals (<pk>)])
            //        return Boolean.TRUE;
            FieldMetaData[] pks = _meta.getPrimaryKeyFields();
            for (FieldMetaData pk : pks) {
                if (pk.getValueStrategy() == ValueStrategies.NONE)
                    continue;

                target = loadManagedInstance(code, false);
                if (ifins != null)
                    ifins.setTarget(target);
                if (ifins2 != null)
                    ifins2.setTarget(target);
                ifins2 = null;

                addGetManagedValueCode(code, pk);
                ifins = ifDefaultValue(code, pk);
                if (pk.getDeclaredTypeCode() == JavaTypes.STRING) {
                    code.constant().setValue("");
                    loadManagedInstance(code, false);
                    addGetManagedValueCode(code, pk);
                    code.invokevirtual().setMethod(String.class, "equals",
                                                   boolean.class, new Class[]{Object.class});
                    ifins2 = code.ifne();
                }
                code.getstatic().setField(Boolean.class, "TRUE",
                                          Boolean.class);
                code.areturn();
            }
        }

        // create artificial target to simplify
        target = code.nop();
        if (ifins != null)
            ifins.setTarget(target);
        if (ifins2 != null)
            ifins2.setTarget(target);

        // if has auto-assigned pk and we get to this point, must have default
        // value, so must be new instance
        if (hasAutoAssignedPK) {
            code.getstatic().setField(Boolean.class, "FALSE", Boolean.class);
            code.areturn();
            return false;
        }

        // if detached state is not definitive, just give up now and return
        // null so that the runtime will perform a DB lookup to determine
        // whether we're detached or new
        code.aload().setThis();
        code.invokespecial().setMethod(ISDETACHEDSTATEDEFINITIVE, boolean.class,
                                       null);
        ifins = code.ifne();
        code.constant().setNull();
        code.areturn();
        ifins.setTarget(code.nop());

        // no detached state: if instance uses detached state and it's not
        // synthetic or the instance is not serializable or the state isn't
        // transient, must not be detached
        if (state == null
                && (!ClassMetaData.SYNTHETIC.equals(_meta.getDetachedState())
                || !Serializable.class.isAssignableFrom(_meta.getDescribedType())
                || !_repos.getConfiguration().getDetachStateInstance().
                isDetachedStateTransient())) {
            // return Boolean.FALSE
            code.getstatic().setField(Boolean.class, "FALSE", Boolean.class);
            code.areturn();
            return true;
        }

        // no detached state: if instance uses detached state (and must be
        // synthetic and transient in serializable instance at this point),
        // not detached if state not set to DESERIALIZED
        if (state == null) {
            // if (pcGetDetachedState () == null) // instead of DESERIALIZED
            //     return Boolean.FALSE;
            loadManagedInstance(code, false);
            code.invokevirtual().setMethod(PRE + "GetDetachedState",
                                           Object.class, null);
            ifins = code.ifnonnull();
            code.getstatic().setField(Boolean.class, "FALSE", Boolean.class);
            code.areturn();
            ifins.setTarget(code.nop());
        }

        // give up; we just don't know
        code.constant().setNull();
        code.areturn();
        return true;
    }

    /**
     * Compare the given field to its Java default, returning the
     * comparison instruction. The field value will already be on the stack.
     */
    private static JumpInstruction ifDefaultValue(Code code,
                                                  FieldMetaData fmd) {
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.BOOLEAN:
            case JavaTypes.BYTE:
            case JavaTypes.CHAR:
            case JavaTypes.INT:
            case JavaTypes.SHORT:
                return code.ifeq();
            case JavaTypes.DOUBLE:
                code.constant().setValue(0D);
                code.dcmpl();
                return code.ifeq();
            case JavaTypes.FLOAT:
                code.constant().setValue(0F);
                code.fcmpl();
                return code.ifeq();
            case JavaTypes.LONG:
                code.constant().setValue(0L);
                code.lcmp();
                return code.ifeq();
            default:
                return code.ifnull();
        }
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
            MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                                               "<clinit>",
                                               Type.getMethodDescriptor(Type.VOID_TYPE),
                                               null, null);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            classNode.methods.add(clinit);
            return clinit;
        }
    }

    /**
     * Helper method to get the code for the class initializer method,
     * creating the method if it does not already exist.
     */
    @Deprecated
    private Code getOrCreateClassInitCode(boolean replaceLast) {
        BCMethod clinit = _pc.getDeclaredMethod("<clinit>");
        Code code;
        if (clinit != null) {
            code = clinit.getCode(true);
            if (replaceLast) {
                Code template = AccessController.doPrivileged(
                        J2DoPrivHelper.newCodeAction());
                code.searchForward(template.vreturn());
                code.previous();
                code.set(template.nop());
                code.next();
            }
            return code;
        }

        // add static initializer method if non exists
        clinit = _pc.declareMethod("<clinit>", void.class, null);
        clinit.makePackage();
        clinit.setStatic(true);
        clinit.setFinal(true);

        code = clinit.getCode(true);
        if (!replaceLast) {
            code.vreturn();
            code.previous();
        }
        return code;
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
        if (_meta.getPCSuperclass() != null && !getCreateSubclass())
            return;

        // add the clone method if necessary
        BCMethod clone = _pc.getDeclaredMethod("clone",
                                               (String[]) null);
        String superName = _managedType.getSuperclassName();
        Code code = null;
        if (clone == null) {
            // add clone support for base classes
            // which also implement cloneable
            boolean isCloneable = Cloneable.class.isAssignableFrom(
                    _managedType.getType());
            boolean extendsObject =
                    superName.equals(Object.class.getName());
            if (!isCloneable || (!extendsObject && !getCreateSubclass()))
                return;

            if (!getCreateSubclass())
                if (_log.isTraceEnabled())
                    _log.trace(
                            _loc.get("enhance-cloneable", _managedType.getName()));

            // add clone method
            // protected Object clone () throws CloneNotSupportedException
            clone = _pc.declareMethod("clone", Object.class, null);
            if (!setVisibilityToSuperMethod(clone))
                clone.makeProtected();
            clone.getExceptions(true).addException
                    (CloneNotSupportedException.class);
            code = clone.getCode(true);

            // return super.clone ();
            loadManagedInstance(code, false);
            code.invokespecial().setMethod(superName, "clone",
                                           Object.class.getName(), null);
            code.areturn();
        }
        else {
            // get the clone method code
            code = clone.getCode(false);
            if (code == null)
                return;
        }

        // create template super.clone () instruction to match against
        Instruction template = (AccessController.doPrivileged(
                J2DoPrivHelper.newCodeAction())).invokespecial()
                .setMethod(superName, "clone", Object.class.getName(), null);

        // find calls to the template instruction; on match
        // clone will be on stack
        code.beforeFirst();
        if (code.searchForward(template)) {
            // ((<type>) clone).pcStateManager = null;
            code.dup();
            code.checkcast().setType(_pc);
            code.constant().setNull();
            code.putfield().setField(SM, SMTYPE);

            // if modified, increase stack
            code.calculateMaxStack();
            code.calculateMaxLocals();
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
            auxEnhancer.run(_pc, _meta);
        }
    }

    /**
     * Affirms if the given method be skipped.
     *
     * @param method method to be skipped or not
     * @return true if any of the auxiliary enhancers skips the given method,
     * or if the method is a constructor
     */
    private boolean skipEnhance(BCMethod method) {
        if ("<init>".equals(method.getName()))
            return true;

        for (AuxiliaryEnhancer auxEnhancer : _auxEnhancers)
            if (auxEnhancer.skipEnhance(method))
                return true;

        return false;
    }

    /**
     * Adds synthetic field access methods that will replace all direct
     * field accesses.
     */
    private void addAccessors()
            throws NoSuchMethodException {
        FieldMetaData[] fmds = getCreateSubclass() ? _meta.getFields()
                : _meta.getDeclaredFields();
        for (int i = 0; i < fmds.length; i++) {
            if (getCreateSubclass()) {
                if (!getRedefine() && isPropertyAccess(fmds[i])) {
                    addSubclassSetMethod(fmds[i]);
                    addSubclassGetMethod(fmds[i]);
                }
            }
            else {
                addGetMethod(i, fmds[i]);
                addSetMethod(i, fmds[i]);
            }
        }
    }

    /**
     * Adds a non-static setter that delegates to the super methods, and
     * performs any necessary field tracking.
     */
    private void addSubclassSetMethod(FieldMetaData fmd)
            throws NoSuchMethodException {
        Class propType = fmd.getDeclaredType();
        String setterName = getSetterName(fmd);
        BCMethod setter = _pc.declareMethod(setterName, void.class,
                                            new Class[]{propType});
        setVisibilityToSuperMethod(setter);
        Code code = setter.getCode(true);

        // not necessary if we're already tracking access via redefinition
        if (!getRedefine()) {
            // get the orig value onto stack
            code.aload().setThis();
            addGetManagedValueCode(code, fmd);
            int val = code.getNextLocalsIndex();
            code.xstore().setLocal(val).setType(fmd.getDeclaredType());
            addNotifyMutation(code, fmd, val, 0);
        }

        // ##### test case: B extends A. Methods defined in A. What
        // ##### happens?
        // super.setXXX(...)
        code.aload().setThis();
        code.xload().setParam(0).setType(propType);
        code.invokespecial().setMethod(_managedType.getType(),
                                       setterName, void.class, new Class[]{propType});

        code.vreturn();
        code.calculateMaxLocals();
        code.calculateMaxStack();
    }

    private boolean setVisibilityToSuperMethod(BCMethod method) {
        BCMethod[] methods = _managedType.getMethods(method.getName(),
                                                     method.getParamTypes());
        if (methods.length == 0)
            throw new UserException(_loc.get("no-accessor",
                                             _managedType.getName(), method.getName()));
        BCMethod superMeth = methods[0];
        if (superMeth.isPrivate()) {
            method.makePrivate();
            return true;
        }
        else if (superMeth.isPackage()) {
            method.makePackage();
            return true;
        }
        else if (superMeth.isProtected()) {
            method.makeProtected();
            return true;
        }
        else if (superMeth.isPublic()) {
            method.makePublic();
            return true;
        }
        return false;
    }

    /**
     * Adds a non-static getter that delegates to the super methods, and
     * performs any necessary field tracking.
     */
    private void addSubclassGetMethod(FieldMetaData fmd) {
        String methName = "get" + StringUtil.capitalize(fmd.getName());
        if (_managedType.getMethods(methName, new Class[0]).length == 0)
            methName = "is" + StringUtil.capitalize(fmd.getName());
        BCMethod getter = _pc.declareMethod(methName, fmd.getDeclaredType(),
                                            null);
        setVisibilityToSuperMethod(getter);
        getter.makePublic();
        Code code = getter.getCode(true);

        // if we're not already tracking field access via reflection, then we
        // must make the getter hook in lazy loading before accessing the super
        // method.
        if (!getRedefine())
            addNotifyAccess(code, fmd);

        code.aload().setThis();
        code.invokespecial().setMethod(_managedType.getType(), methName,
                                       fmd.getDeclaredType(), null);
        code.xreturn().setType(fmd.getDeclaredType());
        code.calculateMaxLocals();
        code.calculateMaxStack();
    }

    /**
     * Adds a static getter method for the given field.
     * The generated method interacts with the instance state and the
     * StateManager to get the value of the field.
     *
     * @param index the relative number of the field
     * @param fmd   metadata about the field to get
     */
    private void addGetMethod(int index, FieldMetaData fmd)
            throws NoSuchMethodException {
        BCMethod method = createGetMethod(fmd);
        Code code = method.getCode(true);

        // if reads are not checked, just return the value
        byte fieldFlag = getFieldFlag(fmd);
        if ((fieldFlag & PersistenceCapable.CHECK_READ) == 0
                && (fieldFlag & PersistenceCapable.MEDIATE_READ) == 0) {
            loadManagedInstance(code, true, fmd);
            addGetManagedValueCode(code, fmd);
            code.xreturn().setType(fmd.getDeclaredType());

            code.calculateMaxStack();
            code.calculateMaxLocals();
            return;
        }

        // if (inst.pcStateManager == null) return inst.<field>;
        loadManagedInstance(code, true, fmd);
        code.getfield().setField(SM, SMTYPE);
        JumpInstruction ifins = code.ifnonnull();
        loadManagedInstance(code, true, fmd);
        addGetManagedValueCode(code, fmd);
        code.xreturn().setType(fmd.getDeclaredType());

        // int field = pcInheritedFieldCount + <fieldindex>;
        int fieldLocal = code.getNextLocalsIndex();
        ifins.setTarget(code.getstatic().setField(INHERIT, int.class));
        code.constant().setValue(index);
        code.iadd();
        code.istore().setLocal(fieldLocal);

        // inst.pcStateManager.accessingField (field);
        // return inst.<field>;
        loadManagedInstance(code, true, fmd);
        code.getfield().setField(SM, SMTYPE);
        code.iload().setLocal(fieldLocal);
        code.invokeinterface().setMethod(SMTYPE, "accessingField", void.class,
                                         new Class[]{int.class});
        loadManagedInstance(code, true, fmd);
        addGetManagedValueCode(code, fmd);
        code.xreturn().setType(fmd.getDeclaredType());

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds a static setter method for the given field.
     * The generated method interacts with the instance state and the
     * StateManager to set the value of the field.
     *
     * @param index the relative number of the field
     * @param fmd   metadata about the field to set
     */
    private void addSetMethod(int index, FieldMetaData fmd)
            throws NoSuchMethodException {
        BCMethod method = createSetMethod(fmd);
        Code code = method.getCode(true);

        // PCEnhancer uses static methods; PCSubclasser does not.
        int firstParamOffset = getAccessorParameterOffset(fmd);

        // if (inst.pcStateManager == null) inst.<field> = value;
        loadManagedInstance(code, true, fmd);
        code.getfield().setField(SM, SMTYPE);
        JumpInstruction ifins = code.ifnonnull();
        loadManagedInstance(code, true, fmd);
        code.xload().setParam(firstParamOffset);
        addSetManagedValueCode(code, fmd);
        if (fmd.isVersion() && _addVersionInitFlag) {
            // if we are setting the version, flip the versionInit flag to true
            loadManagedInstance(code, true);
            code.constant().setValue(1);
            // pcVersionInit = true;
            putfield(code, null, VERSION_INIT_STR, boolean.class);
        }
        code.vreturn();

        // inst.pcStateManager.setting<fieldType>Field (inst,
        //     pcInheritedFieldCount + <index>, inst.<field>, value, 0);
        ifins.setTarget(loadManagedInstance(code, true, fmd));
        code.getfield().setField(SM, SMTYPE);
        loadManagedInstance(code, true, fmd);
        code.getstatic().setField(INHERIT, int.class);
        code.constant().setValue(index);
        code.iadd();
        loadManagedInstance(code, true, fmd);
        addGetManagedValueCode(code, fmd);
        code.xload().setParam(firstParamOffset);
        code.constant().setValue(0);
        code.invokeinterface().setMethod(getStateManagerMethod
                                                 (fmd.getDeclaredType(), "setting", false, true));
        code.vreturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Determines which attach / detach methods to use.
     */
    private void addAttachDetachCode()
            throws NoSuchMethodException {
        // see if any superclasses are detachable
        boolean parentDetachable = false;
        for (ClassMetaData parent = _meta.getPCSuperclassMetaData();
             parent != null; parent = parent.getPCSuperclassMetaData()) {
            if (parent.isDetachable()) {
                parentDetachable = true;
                break;
            }
        }

        // if parent not detachable, we need to add the detach state fields and
        // accessor methods
        if (_meta.getPCSuperclass() == null || getCreateSubclass()
                || parentDetachable != _meta.isDetachable()) {
            addIsDetachedMethod();
            addDetachedStateMethods(_meta.usesDetachedState()
                                            != Boolean.FALSE);
        }

        // if we detach on serialize, we also need to implement the
        // externalizable interface to write just the state for the fields
        // being detached
        if (externalizeDetached()) {
            try {
                addDetachExternalize(parentDetachable,
                                     _meta.usesDetachedState() != Boolean.FALSE);
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
        String declarer = null;
        if (impl && detachField == null) {
            name = PRE + "DetachedState";
            declarer = _pc.getName();
            BCField field = _pc.declareField(name, Object.class);
            field.makePrivate();
            field.setTransient(true);
        }
        else if (impl) {
            name = detachField.getName();
            declarer = detachField.getDeclaringClass().getName();
        }

        // public Object pcGetDetachedState ()
        BCMethod method = _pc.declareMethod(PRE + "GetDetachedState",
                                            Object.class, null);
        method.setStatic(false);
        method.makePublic();
        int access = method.getAccessFlags();

        Code code = method.getCode(true);
        if (impl) {
            // return pcDetachedState;
            loadManagedInstance(code, false);
            getfield(code, _managedType.getProject().loadClass(declarer),
                     name);
        }
        else
            code.constant().setNull();
        code.areturn();
        code.calculateMaxLocals();
        code.calculateMaxStack();

        // public void pcSetDetachedState (Object state)
        method = _pc.declareMethod(PRE + "SetDetachedState",
                                   void.class, new Class[]{Object.class});
        method.setAccessFlags(access);
        code = method.getCode(true);
        if (impl) {
            // pcDetachedState = state;
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            putfield(code, _managedType.getProject().loadClass(declarer),
                     name, Object.class);
        }
        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
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

        if (getCreateSubclass()  && (field == null || !((field.access & Opcodes.ACC_PUBLIC) > 0))) {
            // we're creating the subclass, not redefining the user type.

            // Reflection.getXXX(this, Reflection.findField(...));

            // Reflection.findField(declarer, fieldName, true);
            instructions.add(new LdcInsnNode(Type.getType(declarer)));
            instructions.add(new LdcInsnNode(fieldName));
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
            instructions.add(new FieldInsnNode(Opcodes.GETFIELD, Type.getInternalName(declarer), fieldName, Type.getDescriptor(fieldType)));
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
     * Adds to <code>code</code> the instructions to get field
     * <code>attrName</code> declared in type <code>declarer</code>
     * onto the top of the stack.
     * <p>
     * The instance to access must already be on the top of the
     * stack when this is invoked.
     */
    @Deprecated
    private void getfield(Code code, BCClass declarer, String attrName) {
        if (declarer == null)
            declarer = _managedType;

        // first, see if we can convert the attribute name to a field name
        String fieldName = toBackingFieldName(attrName);

        // next, find the field in the managed type hierarchy
        BCField field = null;
        outer:
        for (BCClass bc = _pc; bc != null; bc = bc.getSuperclassBC()) {
            BCField[] fields = AccessController
                    .doPrivileged(J2DoPrivHelper.getBCClassFieldsAction(bc,
                                                                        fieldName));
            for (BCField bcField : fields) {
                field = bcField;
                // if we reach a field declared in this type, then this is the
                // most-masking field, and is the one that we want.
                if (bcField.getDeclarer() == declarer) {
                    break outer;
                }
            }
        }

        if (getCreateSubclass() && code.getMethod().getDeclarer() == _pc
                && (field == null || !field.isPublic())) {
            // we're creating the subclass, not redefining the user type.

            // Reflection.getXXX(this, Reflection.findField(...));
            code.classconstant().setClass(declarer);
            code.constant().setValue(fieldName);
            code.constant().setValue(true);
            code.invokestatic().setMethod(Reflection.class,
                                          "findField", Field.class, new Class[]{
                            Class.class, String.class, boolean.class});
            Class type = _meta.getField(attrName).getDeclaredType();
            try {
                code.invokestatic().setMethod(
                        getReflectionGetterMethod(type, Field.class));
            }
            catch (NoSuchMethodException e) {
                // should never happen
                throw new InternalException(e);
            }
            if (!type.isPrimitive() && type != Object.class)
                code.checkcast().setType(type);
        }
        else {
            code.getfield().setField(declarer.getName(), fieldName,
                                     field.getType().getName());
        }
    }

    /**
     * Adds to <code>code</code> the instructions to set field
     * <code>attrName</code> declared in type <code>declarer</code>
     * to the value of type <code>fieldType</code> on the top of the stack.
     * <p>
     * When this method is invoked, the value to load must
     * already be on the top of the stack,
     * and the instance to load into must be second.
     * @param declarer internal class name (org/bla/..) which contains the field
     */
    private void putfield(InsnList instructions, Class declarer, String attrName, Class fieldType) {
        String fieldName = toBackingFieldName(attrName);

        if (getRedefine() || getCreateSubclass()) {
            // Reflection.set(this, Reflection.findField(...), value);
            // Reflection.findField(declarer, fieldName, true);
            instructions.add(new LdcInsnNode(Type.getType(declarer)));
            instructions.add(new LdcInsnNode(fieldName));
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
            instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, Type.getInternalName(declarer), fieldName, Type.getDescriptor(fieldType)));
        }
    }

    /**
     * Adds to <code>code</code> the instructions to set field
     * <code>attrName</code> declared in type <code>declarer</code>
     * to the value of type <code>fieldType</code> on the top of the stack.
     * <p>
     * When this method is invoked, the value to load must
     * already be on the top of the stack in <code>code</code>,
     * and the instance to load into must be second.
     */
    private void putfield(Code code, BCClass declarer, String attrName,
                          Class fieldType) {
        if (declarer == null)
            declarer = _managedType;

        String fieldName = toBackingFieldName(attrName);

        if (getRedefine() || getCreateSubclass()) {
            // Reflection.set(this, Reflection.findField(...), value);
            code.classconstant().setClass(declarer);
            code.constant().setValue(fieldName);
            code.constant().setValue(true);
            code.invokestatic().setMethod(Reflection.class,
                                          "findField", Field.class, new Class[]{
                            Class.class, String.class, boolean.class});
            code.invokestatic().setMethod(Reflection.class, "set",
                                          void.class,
                                          new Class[]{
                                                  Object.class,
                                                  fieldType.isPrimitive() ? fieldType : Object.class,
                                                  Field.class});
        }
        else {
            code.putfield()
                    .setField(declarer.getName(), fieldName, fieldType.getName());
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
                && _attrsToFields != null && _attrsToFields.containsKey(name))
            name = (String) _attrsToFields.get(name);
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
                && _fieldsToAttrs != null && _fieldsToAttrs.containsKey(name))
            return (String) _fieldsToAttrs.get(name);
        else
            return name;
    }

    /**
     * Implement the externalizable interface to detach on serialize.
     */
    private void addDetachExternalize(boolean parentDetachable,
                                      boolean detachedState)
            throws NoSuchMethodException {
        // ensure that the declared default constructor is public
        // for externalization
        BCMethod meth = _pc.getDeclaredMethod("<init>", (String[]) null);
        if (!meth.isPublic()) {
            if (_log.isWarnEnabled())
                _log.warn(_loc.get("enhance-defcons-extern",
                                   _meta.getDescribedType()));
            meth.makePublic();
        }
        // declare externalizable interface
        if (!Externalizable.class.isAssignableFrom(_meta.getDescribedType()))
            _pc.declareInterface(Externalizable.class);

        // make sure the user doesn't already have custom externalization or
        // serialization methods
        Class[] input = new Class[]{ObjectInputStream.class};
        Class[] output = new Class[]{ObjectOutputStream.class};
        if (_managedType.getDeclaredMethod("readObject", input) != null
                || _managedType.getDeclaredMethod("writeObject", output) != null)
            throw new UserException(_loc.get("detach-custom-ser", _meta));
        input[0] = ObjectInput.class;
        output[0] = ObjectOutput.class;
        if (_managedType.getDeclaredMethod("readExternal", input) != null
                || _managedType.getDeclaredMethod("writeExternal", output) != null)
            throw new UserException(_loc.get("detach-custom-extern", _meta));

        // create list of all unmanaged serializable fields
        BCField[] fields = _managedType.getDeclaredFields();
        Collection unmgd = new ArrayList(fields.length);
        for (BCField field : fields) {
            if (!field.isTransient() && !field.isStatic()
                    && !field.isFinal()
                    && !field.getName().startsWith(PRE)
                    && _meta.getDeclaredField(field.getName()) == null)
                unmgd.add(field);
        }

        addReadExternal(parentDetachable, detachedState);
        addReadUnmanaged(unmgd, parentDetachable);
        addWriteExternal(parentDetachable, detachedState);
        addWriteUnmanaged(unmgd, parentDetachable);
    }

    /**
     * Add custom readExternal method.
     */
    private void addReadExternal(boolean parentDetachable,
                                 boolean detachedState)
            throws NoSuchMethodException {
        Class[] inargs = new Class[]{ObjectInput.class};
        BCMethod meth = _pc.declareMethod("readExternal", void.class, inargs);
        Exceptions exceps = meth.getExceptions(true);
        exceps.addException(IOException.class);
        exceps.addException(ClassNotFoundException.class);
        Code code = meth.getCode(true);

        // super.readExternal (in);
        // not sure if this works: this is depending on the order of the enhancement!
        // if the subclass gets enhanced first, then the superclass misses
        // the Externalizable at this point!
        Class<?> sup = _meta.getDescribedType().getSuperclass();
        if (!parentDetachable && Externalizable.class.isAssignableFrom(sup)) {
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.invokespecial().setMethod(sup, "readExternal",
                                           void.class, inargs);
        }

        // readUnmanaged (in);
        loadManagedInstance(code, false);
        code.aload().setParam(0);
        code.invokevirtual().setMethod(getType(_meta),
                                       PRE + "ReadUnmanaged", void.class, inargs);

        if (detachedState) {
            // pcSetDetachedState (in.readObject ());
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.invokeinterface().setMethod(ObjectInput.class, "readObject",
                                             Object.class, null);
            code.invokevirtual().setMethod(PRE + "SetDetachedState",
                                           void.class, new Class[]{Object.class});

            // pcReplaceStateManager ((StateManager) in.readObject ());
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.invokeinterface().setMethod(ObjectInput.class, "readObject",
                                             Object.class, null);
            code.checkcast().setType(StateManager.class);
            code.invokevirtual().setMethod(PRE + "ReplaceStateManager",
                                           void.class, new Class[]{StateManager.class});
        }

        addReadExternalFields();

        // readExternalFields(in.readObject ());
        loadManagedInstance(code, false);
        code.aload().setParam(0);
        code.invokevirtual().setMethod("readExternalFields",
                                       void.class, inargs);

        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    private void addReadExternalFields() throws NoSuchMethodException {
        Class<?>[] inargs = new Class[]{ObjectInput.class};
        BCMethod meth = _pc.declareMethod("readExternalFields", void.class, inargs);
        meth.setAccessFlags(Constants.ACCESS_PROTECTED);
        Exceptions exceps = meth.getExceptions(true);
        exceps.addException(IOException.class);
        exceps.addException(ClassNotFoundException.class);
        Code code = meth.getCode(true);

        Class<?> sup = _meta.getPCSuperclass();
        if (sup != null) {
            //add a call to super.readExternalFields()
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.invokespecial().setMethod(sup, "readExternalFields", void.class, inargs);
        }

        // read managed fields
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        for (FieldMetaData fmd : fmds) {
            if (!fmd.isTransient()) {
                readExternal(code, fmd.getName(),
                             fmd.getDeclaredType(), fmd);
            }
        }

        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Read unmanaged fields from the stream (pcReadUnmanaged).
     */
    private void addReadUnmanaged(Collection unmgd, boolean parentDetachable)
            throws NoSuchMethodException {
        Class[] inargs = new Class[]{ObjectInput.class};
        BCMethod meth = _pc.declareMethod(PRE + "ReadUnmanaged", void.class,
                                          inargs);
        meth.makeProtected();
        Exceptions exceps = meth.getExceptions(true);
        exceps.addException(IOException.class);
        exceps.addException(ClassNotFoundException.class);
        Code code = meth.getCode(true);

        // super.readUnmanaged (in);
        if (parentDetachable) {
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.invokespecial().setMethod(getType(_meta.
                                                           getPCSuperclassMetaData()), PRE + "ReadUnmanaged", void.class,
                                           inargs);
        }

        // read declared unmanaged serializable fields
        BCField field;
        for (Object o : unmgd) {
            field = (BCField) o;
            readExternal(code, field.getName(), field.getType(), null);
        }
        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Helper method to read a field from an externalization input stream.
     */
    private void readExternal(Code code, String fieldName, Class type,
                              FieldMetaData fmd)
            throws NoSuchMethodException {
        String methName;
        if (type.isPrimitive()) {
            methName = type.getName();
            methName = methName.substring(0, 1).toUpperCase(Locale.ENGLISH)
                    + methName.substring(1);
            methName = "read" + methName;
        }
        else
            methName = "readObject";

        // <field> = in.read<type> ();
        loadManagedInstance(code, false);
        code.aload().setParam(0);
        Class ret = (type.isPrimitive()) ? type : Object.class;
        code.invokeinterface().setMethod(ObjectInput.class, methName,
                                         ret, null);
        if (!type.isPrimitive() && type != Object.class)
            code.checkcast().setType(type);
        if (fmd == null)
            putfield(code, null, fieldName, type);
        else {
            addSetManagedValueCode(code, fmd);
            switch (fmd.getDeclaredTypeCode()) {
                case JavaTypes.DATE:
                case JavaTypes.ARRAY:
                case JavaTypes.COLLECTION:
                case JavaTypes.MAP:
                case JavaTypes.OBJECT:
                case JavaTypes.CALENDAR:
                    // if (sm != null)
                    //   sm.proxyDetachedDeserialized (<index>);
                    loadManagedInstance(code, false);
                    code.getfield().setField(SM, SMTYPE);
                    IfInstruction ifins = code.ifnull();
                    loadManagedInstance(code, false);
                    code.getfield().setField(SM, SMTYPE);
                    code.constant().setValue(fmd.getIndex());
                    code.invokeinterface().setMethod(SMTYPE,
                                                     "proxyDetachedDeserialized", void.class,
                                                     new Class[]{int.class});
                    ifins.setTarget(code.nop());
            }
        }
    }

    /**
     * Add custom writeExternal method.
     */
    private void addWriteExternal(boolean parentDetachable,
                                  boolean detachedState)
            throws NoSuchMethodException {
        Class[] outargs = new Class[]{ObjectOutput.class};
        BCMethod meth = _pc.declareMethod("writeExternal", void.class, outargs);
        Exceptions exceps = meth.getExceptions(true);
        exceps.addException(IOException.class);
        Code code = meth.getCode(true);

        // super.writeExternal (out);
        Class sup = getType(_meta).getSuperclass();
        if (!parentDetachable && Externalizable.class.isAssignableFrom(sup)) {
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.invokespecial().setMethod(sup, "writeExternal",
                                           void.class, outargs);
        }

        // writeUnmanaged (out);
        loadManagedInstance(code, false);
        code.aload().setParam(0);
        code.invokevirtual().setMethod(getType(_meta),
                                       PRE + "WriteUnmanaged", void.class, outargs);

        JumpInstruction go2 = null;
        if (detachedState) {
            // if (sm != null)
            //   if (sm.writeDetached (out))
            //      return;
            loadManagedInstance(code, false);
            code.getfield().setField(SM, SMTYPE);
            IfInstruction ifnull = code.ifnull();
            loadManagedInstance(code, false);
            code.getfield().setField(SM, SMTYPE);
            code.aload().setParam(0);
            code.invokeinterface().setMethod(SMTYPE, "writeDetached",
                                             boolean.class, outargs);
            go2 = code.ifeq();
            code.vreturn();

            // else
            //   out.writeObject (pcGetDetachedState ());
            Class[] objargs = new Class[]{Object.class};
            ifnull.setTarget(code.aload().setParam(0));
            loadManagedInstance(code, false);
            code.invokevirtual().setMethod(PRE + "GetDetachedState",
                                           Object.class, null);
            code.invokeinterface().setMethod(ObjectOutput.class,
                                             "writeObject", void.class, objargs);
            //    out.writeObject (null) // StateManager
            code.aload().setParam(0);
            code.constant().setValue((Object) null);
            code.invokeinterface().setMethod(ObjectOutput.class,
                                             "writeObject", void.class, objargs);
        }
        if (go2 != null)
            go2.setTarget(code.nop());

        addWriteExternalFields();

        loadManagedInstance(code, false);
        code.aload().setParam(0);
        code.invokevirtual().setMethod("writeExternalFields",
                                       void.class, outargs);

        // return
        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }


    private void addWriteExternalFields()
            throws NoSuchMethodException {
        Class<?>[] outargs = new Class[]{ObjectOutput.class};
        BCMethod meth = _pc.declareMethod("writeExternalFields", void.class, outargs);
        meth.setAccessFlags(Constants.ACCESS_PROTECTED);
        Exceptions exceps = meth.getExceptions(true);
        exceps.addException(IOException.class);
        Code code = meth.getCode(true);

        Class<?> sup = _meta.getPCSuperclass();
        if (sup != null) {
            // add a call to super.readExternalFields()
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.invokespecial().setMethod(sup, "writeExternalFields", void.class, outargs);
        }

        FieldMetaData[] fmds = _meta.getDeclaredFields();
        for (FieldMetaData fmd : fmds) {
            if (!fmd.isTransient()) {
                writeExternal(code, fmd.getName(),
                              fmd.getDeclaredType(), fmd);
            }
        }

        // return
        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Write unmanaged fields to the stream (pcWriteUnmanaged).
     */
    private void addWriteUnmanaged(Collection unmgd, boolean parentDetachable)
            throws NoSuchMethodException {
        Class[] outargs = new Class[]{ObjectOutput.class};
        BCMethod meth = _pc.declareMethod(PRE + "WriteUnmanaged", void.class,
                                          outargs);
        meth.makeProtected();
        Exceptions exceps = meth.getExceptions(true);
        exceps.addException(IOException.class);
        Code code = meth.getCode(true);

        // super.writeUnmanaged (out);
        if (parentDetachable) {
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.invokespecial().setMethod(getType(_meta.
                                                           getPCSuperclassMetaData()), PRE + "WriteUnmanaged", void.class,
                                           outargs);
        }

        // write declared unmanaged serializable fields
        BCField field;
        for (Object o : unmgd) {
            field = (BCField) o;
            writeExternal(code, field.getName(), field.getType(), null);
        }
        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Helper method to write a field to an externalization output stream.
     */
    private void writeExternal(Code code, String fieldName, Class type,
                               FieldMetaData fmd)
            throws NoSuchMethodException {
        String methName;
        if (type.isPrimitive()) {
            methName = type.getName();
            methName = methName.substring(0, 1).toUpperCase(Locale.ENGLISH)
                    + methName.substring(1);
            methName = "write" + methName;
        }
        else
            methName = "writeObject";

        // out.write<type> (<field>);
        code.aload().setParam(0);
        loadManagedInstance(code, false);
        if (fmd == null)
            getfield(code, null, fieldName);
        else
            addGetManagedValueCode(code, fmd);
        Class[] args = new Class[]{type};
        if (type == byte.class || type == char.class || type == short.class)
            args[0] = int.class;
        else if (!type.isPrimitive())
            args[0] = Object.class;
        code.invokeinterface().setMethod(ObjectOutput.class, methName,
                                         void.class, args);
    }

    private void addGetManagedValueCode(Code code, FieldMetaData fmd)
            throws NoSuchMethodException {
        addGetManagedValueCode(code, fmd, true);
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
     * Load the field value specified by <code>fmd</code> onto the stack.
     * Before this method is called, the object that the data should be loaded
     * from will be on the top of the stack.
     *
     * @param fromSameClass if <code>true</code>, then <code>fmd</code> is
     *                      being loaded from an instance of the same class as the current execution
     *                      context. If <code>false</code>, then the instance on the top of the stack
     *                      might be a superclass of the current execution context's 'this' instance.
     */
    @Deprecated
    private void addGetManagedValueCode(Code code, FieldMetaData fmd,
                                        boolean fromSameClass)
            throws NoSuchMethodException {
        // if redefining, then we must always reflect (or access the field
        // directly if accessible), since the redefined methods will always
        // trigger method calls to StateManager, even from internal direct-
        // access usage. We could work around this by not redefining, and
        // just do a subclass approach instead. But this is not a good option,
        // since it would sacrifice lazy loading and efficient dirty tracking.

        if (getRedefine() || isFieldAccess(fmd)) {
            getfield(code, null, fmd.getName());
        }
        else if (getCreateSubclass()) {
            // property access, and we're not redefining. If we're operating
            // on an instance that is definitely the same type as 'this', then
            // call superclass method to bypass tracking. Otherwise, reflect
            // to both bypass tracking and avoid class verification errors.
            if (fromSameClass) {
                Method meth = (Method) fmd.getBackingMember();
                code.invokespecial().setMethod(meth);
            }
            else {
                getfield(code, null, fmd.getName());
            }
        }
        else {
            // regular enhancement + property access
            Method meth = (Method) fmd.getBackingMember();
            code.invokevirtual().setMethod(PRE + meth.getName(),
                                           meth.getReturnType(), meth.getParameterTypes());
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
            instructions.add(new LdcInsnNode(value));
        }

        // if redefining, then we must always reflect (or access the field
        // directly if accessible), since the redefined methods will always
        // trigger method calls to StateManager, even from internal direct-
        // access usage. We could work around this by not redefining, and
        // just do a subclass approach instead. But this is not a good option,
        // since it would sacrifice lazy loading and efficient dirty tracking.
        if (getRedefine() || isFieldAccess(fmd)) {
            instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,
                                               Type.getInternalName(fmd.getDeclaringType()),
                                               fmd.getName(),
                                               Type.getDescriptor(fmd.getDeclaredType())));
        }
        else if (getCreateSubclass()) {
            // property access, and we're not redefining. invoke the
            // superclass method to bypass tracking.
            instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                                                Type.getInternalName(_managedType.getType()),
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
            putfield(instructions, getType(_meta), fmd.getName(), fmd.getDeclaredType());
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
     * Store the value at the top of the stack into the field value specified
     * by <code>fmd</code>. Before this method is called, the data to load will
     * be on the top of the stack and the object that the data should be loaded
     * into will be second in the stack.
     */
    @Deprecated
    private void addSetManagedValueCode(Code code, FieldMetaData fmd)
        throws NoSuchMethodException {
        // if redefining, then we must always reflect (or access the field
        // directly if accessible), since the redefined methods will always
        // trigger method calls to StateManager, even from internal direct-
        // access usage. We could work around this by not redefining, and
        // just do a subclass approach instead. But this is not a good option,
        // since it would sacrifice lazy loading and efficient dirty tracking.

        if (getRedefine() || isFieldAccess(fmd)) {
            putfield(code, null, fmd.getName(), fmd.getDeclaredType());
        } else if (getCreateSubclass()) {
            // property access, and we're not redefining. invoke the
            // superclass method to bypass tracking.
            code.invokespecial().setMethod(_managedType.getType(),
                getSetterName(fmd), void.class,
                new Class[] { fmd.getDeclaredType() });
        } else {
            // regular enhancement + property access
            code.invokevirtual().setMethod(PRE + getSetterName(fmd),
                void.class, new Class[] { fmd.getDeclaredType() });
        }
    }

    /**
     * Add the {@link Instruction}s to load the instance to modify onto the
     * stack, and return it. If <code>forStatic</code> is set, then
     * <code>code</code> is in an accessor method or another static method;
     * otherwise, it is in one of the PC-specified methods.
     *
     * @return the first instruction added to <code>code</code>.
     */
    private Instruction loadManagedInstance(Code code, boolean forStatic,
            FieldMetaData fmd) {
        if (forStatic && isFieldAccess(fmd))
            return code.aload().setParam(0);
        return code.aload().setThis();
    }

    /**
     * Add the {@link Instruction}s to load the instance to modify onto the
     * stack, and return it.  This method should not be used to load static
     * fields.
     *
     * @return the first instruction added to <code>code</code>.
     */
    private Instruction loadManagedInstance(Code code, boolean forStatic) {
        return loadManagedInstance(code, forStatic, null);
    }

    private int getAccessorParameterOffset(FieldMetaData fmd) {
       return isFieldAccess(fmd) ? 1 : 0;
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
     * Create the generated getter {@link BCMethod} for <code>fmd</code>. The
     * calling environment will then populate this method's code block.
     */
    private BCMethod createGetMethod(FieldMetaData fmd) {
        BCMethod getter;
        if (isFieldAccess(fmd)) {
            // static <fieldtype> pcGet<field> (XXX inst)
            BCField field = _pc.getDeclaredField(fmd.getName());
            getter = _pc.declareMethod(PRE + "Get" + fmd.getName(), fmd.
                getDeclaredType().getName(), new String[]{ _pc.getName() });
            getter.setAccessFlags(field.getAccessFlags()
                & ~Constants.ACCESS_TRANSIENT & ~Constants.ACCESS_VOLATILE);
            getter.setStatic(true);
            getter.setFinal(true);
            return getter;
        }

        // property access:
        // copy the user's getter method to a new name; we can't just reset
        // the name, because that will also reset all calls to the method
        Method meth = (Method) fmd.getBackingMember();
        getter = _pc.getDeclaredMethod(meth.getName(),
            meth.getParameterTypes());
        BCMethod newgetter = _pc.declareMethod(PRE + meth.getName(),
            meth.getReturnType(), meth.getParameterTypes());
        newgetter.setAccessFlags(getter.getAccessFlags());
        newgetter.makeProtected();
        transferCodeAttributes(getter, newgetter);
        return getter;
    }

    /**
     * Create the generated setter {@link BCMethod} for <code>fmd</code>. The
     * calling environment will then populate this method's code block.
     */
    private BCMethod createSetMethod(FieldMetaData fmd) {
        BCMethod setter;
        if (isFieldAccess(fmd)) {
            // static void pcSet<field> (XXX inst, <fieldtype> value)
            BCField field = _pc.getDeclaredField(fmd.getName());
            setter = _pc.declareMethod(PRE + "Set" + fmd.getName(), void.class,
                new Class[]{ getType(_meta), fmd.getDeclaredType() });
            setter.setAccessFlags(field.getAccessFlags()
                & ~Constants.ACCESS_TRANSIENT & ~Constants.ACCESS_VOLATILE);
            setter.setStatic(true);
            setter.setFinal(true);
            return setter;
        }

        // property access:
        // copy the user's getter method to a new name; we can't just reset
        // the name, because that will also reset all calls to the method
        setter = _pc.getDeclaredMethod(getSetterName(fmd),
            new Class[]{ fmd.getDeclaredType() });
        BCMethod newsetter = _pc.declareMethod(PRE + setter.getName(),
            setter.getReturnName(), setter.getParamNames());
        newsetter.setAccessFlags(setter.getAccessFlags());
        newsetter.makeProtected();
        transferCodeAttributes(setter, newsetter);
        return setter;
    }

    private void addGetEnhancementContractVersionMethod(ClassNodeTracker cnt) {
        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC,
                                               PRE + "GetEnhancementContractVersion",
                                               Type.getMethodDescriptor(Type.INT_TYPE),
                                               null, null);
        methodNode.instructions.add(new LdcInsnNode(ENHANCER_VERSION));
        methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
        cnt.getClassNode().methods.add(methodNode);
    }

    /**
     * Return the concrete type for the given class, i.e. impl for managed
     * interfaces
     */
    public Class getType(ClassMetaData meta) {
        if (meta.getInterfaceImpl() != null)
            return meta.getInterfaceImpl();
        return meta.getDescribedType();
    }

    /**
     * Move code-related attributes from one method to another.
     */
    private static void transferCodeAttributes(BCMethod from, BCMethod to) {
        Code code = from.getCode(false);
        if (code != null) {
            to.addAttribute(code);
            from.removeCode();
        }

        Exceptions exceps = from.getExceptions(false);
        if (exceps != null)
            to.addAttribute(exceps);
    }

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
            new Configurations.Runnable() {
            @Override
            public boolean run(Options opts) throws IOException {
                OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
                try {
                    return PCEnhancer.run(conf, args, opts);
                } finally {
                    conf.close();
                }
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
        if (loader == null)
            loader = conf.getClassResolverInstance().
                getClassLoader(PCEnhancer.class, null);
        if (flags.tmpClassLoader)
            loader = AccessController.doPrivileged(J2DoPrivHelper
                .newTemporaryClassLoaderAction(loader));

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
        } else {
            ClassArgParser cap = conf.getMetaDataRepositoryInstance().
                getMetaDataFactory().newClassArgParser();
            cap.setClassLoader(loader);
            classes = new HashSet();
            for (String arg : args) {
                classes.addAll(Arrays.asList(cap.parseTypes(arg)));
            }
        }

        Project project = new Project();
        BCClass bc;
        PCEnhancer enhancer;
        Collection persAwareClasses = new HashSet();

        int status;
        for (Object o : classes) {
            if (log.isInfoEnabled())
                log.info(_loc.get("enhance-running", o));

            if (o instanceof String)
                bc = project.loadClass((String) o, loader);
            else
                bc = project.loadClass((Class) o);
            enhancer = new PCEnhancer(conf, bc, repos, loader);
            if (writer != null) {
                enhancer.setBytecodeWriter(writer);
            }
            enhancer.setDirectory(flags.directory);
            enhancer.setAddDefaultConstructor(flags.addDefaultConstructor);
            status = enhancer.run();
            if (status == ENHANCE_NONE) {
                if (log.isTraceEnabled())
                    log.trace(_loc.get("enhance-norun"));
            }
            else if (status == ENHANCE_INTERFACE) {
                if (log.isTraceEnabled())
                    log.trace(_loc.get("enhance-interface"));
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
        if(log.isInfoEnabled() && !persAwareClasses.isEmpty()){
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
    public interface AuxiliaryEnhancer
    {
        void run (BCClass bc, ClassMetaData meta);
        boolean skipEnhance(BCMethod m);
    }

    private void addGetIDOwningClass()  {
        BCMethod method = _pc.declareMethod(PRE + "GetIDOwningClass",
            Class.class, null);
        Code code = method.getCode(true);

        code.classconstant().setClass(getType(_meta));
        code.areturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * This static public worker method detects and logs any Entities that may have been enhanced at build time by
     * a version of the enhancer that is older than the current version.
     *
     * @param cls
     *            - A non-null Class implementing org.apache.openjpa.enhance.PersistenceCapable.
     * @param log
     *            - A non-null org.apache.openjpa.lib.log.Log.
     *
     * @throws - IllegalStateException if cls doesn't implement org.apache.openjpa.enhance.PersistenceCapable.
     *
     * @return true if the provided Class is down level from the current PCEnhancer.ENHANCER_VERSION. False
     *         otherwise.
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
            log.info(_loc.get("down-level-enhanced-entity", new Object[] { cls.getName(),
                pc.pcGetEnhancementContractVersion(), PCEnhancer.ENHANCER_VERSION }));
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
            if (!fmds[i].isPrimaryKey())
                continue;
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
            } else {
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
    */
    private int[] getIdClassConstructorParmOrder(Class<?> oidType, ArrayList<Integer> pkfields,
            FieldMetaData[] fmds) {
        Project project = new Project();
        BCClass bc = project.loadClass(oidType);
        BCMethod[] methods = bc.getDeclaredMethods("<init>");
        if (methods == null || methods.length == 0) {
            return null;
        }

        int parmOrder[] = new int[pkfields.size()];
        for (BCMethod method : methods) {
            // constructor must be public
            if (!method.isPublic()) {
                continue;
            }
            Class<?>[] parmTypes = method.getParamTypes();
            // make sure the constructors have the same # of parms as
            // the number of pk fields
            if (parmTypes.length != pkfields.size()) {
                continue;
            }

            int parmOrderIndex = 0;
            Code code = method.getCode(false);
            Instruction[] ins = code.getInstructions();
            for (int i = 0; i < ins.length; i++) {
                if (ins[i] instanceof PutFieldInstruction) {
                    PutFieldInstruction pfi = (PutFieldInstruction)ins[i];
                    for (int j = 0; j < pkfields.size(); j++) {
                        int fieldNum = pkfields.get(j);
                        // Compare the field being set with the current pk field
                        String parmName = fmds[fieldNum].getName();
                        Class<?> parmType = fmds[fieldNum].getType();
                        if (parmName.equals(pfi.getFieldName())) {
                            // backup and examine the load instruction parm
                            if (i > 0 && ins[i-1] instanceof LoadInstruction) {
                                LoadInstruction li = (LoadInstruction)ins[i-1];
                                // Get the local index from the instruction.  This will be the index
                                // of the constructor parameter.  must be less than or equal to the
                                // max parm index to prevent from picking up locals that could have
                                // been produced within the constructor.  Also make sure the parm type
                                // matches the fmd type
                                int parm = li.getLocal();
                                if (parm <= pkfields.size() && parmTypes[parm-1].equals(parmType)) {
                                    parmOrder[parmOrderIndex] = fieldNum;
                                    parmOrderIndex++;
                                }
                            } else {
                                // Some other instruction found. can't make a determination of which local/parm
                                // is being used on the putfield.
                                break;
                            }
                        }
                    }
                }
            }
            if (parmOrderIndex == pkfields.size()) {
                return parmOrder;
            }
        }
        return null;
    }
}
