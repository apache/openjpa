/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.enhance;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.openjpa.conf.OpenJPAConfiguration;
import org.apache.openjpa.conf.OpenJPAConfigurationImpl;
import org.apache.openjpa.lib.conf.Configuration;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.log.Log;
import org.apache.openjpa.lib.meta.ClassArgParser;
import org.apache.openjpa.lib.util.BytecodeWriter;
import org.apache.openjpa.lib.util.Files;
import org.apache.openjpa.lib.util.Localizer;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.lib.util.Services;
import org.apache.openjpa.lib.util.TemporaryClassLoader;
import org.apache.openjpa.lib.util.Localizer.Message;
import org.apache.openjpa.meta.ClassMetaData;
import org.apache.openjpa.meta.FieldMetaData;
import org.apache.openjpa.meta.JavaTypes;
import org.apache.openjpa.meta.MetaDataRepository;
import org.apache.openjpa.meta.ValueStrategies;
import org.apache.openjpa.util.GeneralException;
import org.apache.openjpa.util.InternalException;
import org.apache.openjpa.util.ObjectId;
import org.apache.openjpa.util.OpenJPAException;
import org.apache.openjpa.util.UserException;
import serp.bytecode.BCClass;
import serp.bytecode.BCField;
import serp.bytecode.BCMethod;
import serp.bytecode.Code;
import serp.bytecode.Constants;
import serp.bytecode.Exceptions;
import serp.bytecode.FieldInstruction;
import serp.bytecode.GetFieldInstruction;
import serp.bytecode.IfInstruction;
import serp.bytecode.Instruction;
import serp.bytecode.JumpInstruction;
import serp.bytecode.LoadInstruction;
import serp.bytecode.MethodInstruction;
import serp.bytecode.Project;
import serp.bytecode.TableSwitchInstruction;
import serp.util.Numbers;

/**
 * Bytecode enhancer used to enhance persistent classes from metadata. The
 * enhancer must be invoked on all persistence-capable and persistence aware
 * classes.
 *
 * @author Abe White
 */
public class PCEnhancer {

    public static final int ENHANCE_NONE = 0;
    public static final int ENHANCE_AWARE = 2 << 0;
    public static final int ENHANCE_INTERFACE = 2 << 1;
    public static final int ENHANCE_PC = 2 << 2;
    public static final int ENHANCE_OID = 2 << 3;

    private static final String PRE = "pc";
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

    private static final Localizer _loc = Localizer.forPackage
        (PCEnhancer.class);

    private final BCClass _pc;
    private final MetaDataRepository _repos;
    private final ClassMetaData _meta;
    private final Log _log;
    private Collection _oids = null;

    private boolean _defCons = true;
    private boolean _fail = false;
    private AuxiliaryEnhancer[] _auxEnhancers = null;
    private File _dir = null;
    private BytecodeWriter _writer = null;
    private Map _backingFields = null;
    private Set _violations = null;

    /**
     * Constructor. Supply configuration and type to enhance.
     */
    public PCEnhancer(OpenJPAConfiguration conf, Class type) {
        this(conf, new Project().loadClass(type), (MetaDataRepository) null);
    }

    /**
     * Constructor. Supply configuration and type to enhance.
     */
    public PCEnhancer(OpenJPAConfiguration conf, ClassMetaData type) {
        this(conf, new Project().loadClass(type.getDescribedType()),
            type.getRepository());
    }

    /**
     * Constructor. Supply configuration.
     *
     * @param type the bytecode representation fo the type to
     * enhance; this can be created from any stream or file
     * @param repos a metadata repository to use for metadata access,
     * or null to create a new reporitory; the repository
     * from the given configuration isn't used by default
     * because the configuration might be an
     * implementation-specific subclass whose metadata
     * required more than just base metadata files
     */
    public PCEnhancer(OpenJPAConfiguration conf, BCClass type,
        MetaDataRepository repos) {
        _pc = type;
        _log = conf.getLog(OpenJPAConfiguration.LOG_ENHANCE);

        if (repos == null) {
            _repos = conf.newMetaDataRepositoryInstance();
            _repos.setSourceMode(MetaDataRepository.MODE_META);
        } else
            _repos = repos;
        _meta = _repos.getMetaData(type.getType(), null, false);
    }

    /**
     * Constructor. Supply configuration, type, and metadata.
     */
    public PCEnhancer(OpenJPAConfiguration conf, BCClass type,
        ClassMetaData meta) {
        _pc = type;
        _log = conf.getLog(OpenJPAConfiguration.LOG_ENHANCE);
        _repos = meta.getRepository();
        _meta = meta;
    }

    /**
     * Return the bytecode representation of the class being manipulated.
     */
    public BCClass getBytecode() {
        return _pc;
    }

    /**
     * Return the bytecode representations of any oid classes that must be
     * manipulated.
     */
    public Collection getObjectIdBytecode() {
        return (_oids == null) ? Collections.EMPTY_LIST : _oids;
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
        if (_log.isTraceEnabled())
            _log.trace(_loc.get("enhance-start", _pc.getType()));

        try {
            // if managed interface, skip
            if (_pc.isInterface())
                return ENHANCE_INTERFACE;

            // check if already enhanced
            Class[] interfaces = _pc.getDeclaredInterfaceTypes();
            for (int i = 0; i < interfaces.length; i++) {
                if (interfaces[i].getName().equals(PCTYPE.getName())) {
                    if (_log.isTraceEnabled())
                        _log.trace(_loc.get("pc-type", _pc.getType()));
                    if (_meta != null && enhanceObjectId())
                        return ENHANCE_OID;
                    return ENHANCE_NONE;
                }
            }

            // validate properties before replacing field access so that
            // we build up a record of backing fields, etc
            if (_meta != null
                && _meta.getAccessType() == ClassMetaData.ACCESS_PROPERTY)
                validateProperties();
            replaceAndValidateFieldAccess();
            processViolations();

            if (_meta != null) {
                int ret = ENHANCE_PC;
                enhanceClass();
                addFields();
                addStaticInitializer();
                addPCMethods();
                addAccessors();
                addAttachDetachCode();
                addSerializationCode();
                addCloningCode();
                if (enhanceObjectId())
                    ret |= ENHANCE_OID;
                runAuxiliaryEnhancers();
                return ret;
            }

            if (_log.isWarnEnabled())
                _log.warn(_loc.get("pers-aware", _pc.getType()));
            return ENHANCE_AWARE;
        } catch (OpenJPAException ke) {
            throw ke;
        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Write the generated bytecode.
     */
    public void record()
        throws IOException {
        record(_pc);
        if (_oids != null)
            for (Iterator itr = _oids.iterator(); itr.hasNext();)
                record((BCClass) itr.next());
    }

    /**
     * Write the given class.
     */
    private void record(BCClass bc)
        throws IOException {
        if (_writer != null)
            _writer.write(bc);
        else if (_dir == null)
            bc.write();
        else {
            File dir = Files.getPackageFile(_dir, bc.getPackageName(), true);
            bc.write(new File(dir, bc.getClassName() + ".class"));
        }
    }

    /**
     * Validate that the methods in this property-access instance are
     * written correctly. This method also gathers information on each
     * property's backing field.
     */
    private void validateProperties() {
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        Method meth;
        BCMethod getter, setter = null;
        BCField returned, assigned;
        for (int i = 0; i < fmds.length; i++) {
            if (!(fmds[i].getBackingMember() instanceof Method)) {
                addViolation("property-bad-member",
                    new Object[]{ fmds[i], fmds[i].getBackingMember() },
                    true);
                continue;
            }

            meth = (Method) fmds[i].getBackingMember();
            getter = _pc.getDeclaredMethod(meth.getName(),
                meth.getParameterTypes());
            if (getter == null) {
                addViolation("property-no-getter", new Object[]{ fmds[i] },
                    true);
                continue;
            }
            returned = getReturnedField(getter);
            if (returned != null) {
                if (_backingFields == null)
                    _backingFields = new HashMap();
                _backingFields.put(getter.getName(), returned.getName());
            }

            setter = _pc.getDeclaredMethod(getSetterName(fmds[i]),
                new Class[]{ fmds[i].getDeclaredType() });
            if (setter == null) {
                if (returned == null)
                    addViolation("property-no-setter",
                        new Object[]{ fmds[i] }, true);
                else {
                    // create synthetic setter
                    setter = _pc.declareMethod(getSetterName(fmds[i]),
                        void.class, new Class[]{ fmds[i].getDeclaredType() });
                    setter.makePrivate();
                    Code code = setter.getCode(true);
                    code.aload().setThis();
                    code.xload().setParam(0);
                    code.putfield().setField(returned);
                    code.vreturn();
                    code.calculateMaxStack();
                    code.calculateMaxLocals();
                }
            }

            assigned = getAssignedField(setter);
            if (assigned != null) {
                if (_backingFields == null)
                    _backingFields = new HashMap();
                _backingFields.put(setter.getName(), assigned.getName());

                if (assigned != returned)
                    addViolation("property-setter-getter-mismatch",
                        new Object[]{ fmds[i], assigned.getName(),
                            (returned == null) ? null : returned.getName() },
                        false);
            }
        }
    }

    /**
     * Return the name of the setter method for the given field.
     */
    private static String getSetterName(FieldMetaData fmd) {
        return "set" + StringUtils.capitalize(fmd.getName());
    }

    /**
     * Return the field returned by the given method, or null if none.
     * Package-protected and static for testing.
     */
    static BCField getReturnedField(BCMethod meth) {
        return findField(meth, new Code().xreturn().setType
            (meth.getReturnType()), false);
    }

    /**
     * Return the field assigned in the given method, or null if none.
     * Package-protected and static for testing.
     */
    static BCField getAssignedField(BCMethod meth) {
        return findField(meth, new Code().putfield(), true);
    }

    /**
     * Return the field returned / assigned by <code>meth</code>. Returns
     * null if non-fields (methods, literals, parameters, variables) are
     * returned, or if non-parameters are assigned to fields.
     */
    private static BCField findField(BCMethod meth, Instruction template,
        boolean findAccessed) {
        // ignore any static methods. OpenJPA only currently supports non-static
        // setters and getters
        if (meth.isStatic())
            return null;

        Code code = meth.getCode(false);
        if (code == null)
            return null;
        code.beforeFirst();

        BCField field = null, cur;
        Instruction templateIns, prevIns, twoPrevIns;
        while (code.searchForward(template)) {
            templateIns = code.previous();
            if (!code.hasPrevious())
                return null;
            prevIns = code.previous();
            if (!code.hasPrevious())
                return null;
            twoPrevIns = code.previous();

            // if the opcode two before the template was an aload_0, check
            // against the middle instruction based on what type of find
            // we're doing
            if (!(twoPrevIns instanceof LoadInstruction)
                || !((LoadInstruction) twoPrevIns).isThis())
                return null;

            // if the middle instruction was a getfield, then it's the
            // field that's being accessed
            if (!findAccessed && prevIns instanceof GetFieldInstruction)
                cur = ((FieldInstruction) prevIns).getField();
                // if the middle instruction was an xload_1, then the
                // matched instruction is the field that's being set.
            else if (findAccessed && prevIns instanceof LoadInstruction
                && ((LoadInstruction) prevIns).getParam() == 0)
                cur = ((FieldInstruction) templateIns).getField();
            else
                return null;

            if (field != null && cur != field)
                return null;
            field = cur;

            // ready for next search iteration
            code.next();
            code.next();
            code.next();
        }
        return field;
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

        String sep = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();
        for (Iterator itr = _violations.iterator(); itr.hasNext();) {
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
    private void replaceAndValidateFieldAccess() {
        // create template putfield/getfield instructions to search for
        Code template = new Code();
        Instruction put = template.putfield();
        Instruction get = template.getfield();
        Instruction stat = template.invokestatic();

        // look through all methods; this is done before any methods are added
        // so we don't need to worry about excluding synthetic methods.
        BCMethod[] methods = _pc.getDeclaredMethods();
        Code code;
        for (int i = 0; i < methods.length; i++) {
            code = methods[i].getCode(false);

            // don't modify the methods specified by the auxiliary enhancers
            if (code != null && !skipEnhance(methods[i])) {
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
     * be placed before the first instruction on method start,
     * and will be after the last instruction on method completion
     * @param ins the template instruction to search for; either a
     * getfield or putfield instruction
     * @param get boolean indicating if this is a get instruction
     * @param stat template invokestatic instruction to replace with
     */
    private void replaceAndValidateFieldAccess(Code code, Instruction ins,
        boolean get, Instruction stat) {
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
            owner = getPersistenceCapableOwner(name,
                fi.getFieldDeclarerType());

            if (owner != null
                && owner.getAccessType() == ClassMetaData.ACCESS_PROPERTY) {
                // if we're directly accessing a field in another class
                // hierarchy that uses property access, something is wrong
                if (owner != _meta && owner.getDeclaredField(name) != null &&
                    _meta != null && !owner.getDescribedType().
                    isAssignableFrom(_meta.getDescribedType()))
                    throw new UserException(_loc.get("property-field-access",
                        new Object[]{ _meta, owner, name,
                            code.getMethod().getName() }));

                // if we're directly accessing a property-backing field outside
                // the property in our own class, notify user
                if (isBackingFieldOfAnotherProperty(name, code))
                    addViolation("property-field-access", new Object[]{ _meta,
                        owner, name, code.getMethod().getName() }, false);

                code.next();
                continue;
            }

            // not persistent field?
            if (owner == null || owner.getDeclaredField(name) == null) {
                code.next();
                continue;
            }

            // replace the instruction with a call to the generated access
            // method
            mi = (MethodInstruction) code.set(stat);

            // invoke the proper access method, whether getter or setter
            String prefix = (get) ? PRE + "Get" : PRE + "Set";
            methodName = prefix + name;
            if (get) {
                mi.setMethod(getType(owner).getName(),
                    methodName, typeName, new String[]
                    { getType(owner).getName() });
            } else {
                mi.setMethod(getType(owner).getName(),
                    methodName, "void", new String[]
                    { getType(owner).getName(), typeName });
            }
        }
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
     * @param owner the nominal owner of the field
     * @return the metadata for the PersistenceCapable type that
     * declares the field (and therefore has the static method), or null if none
     */
    private ClassMetaData getPersistenceCapableOwner(String fieldName,
        Class owner) {
        // find the actual ancestor class that declares the field, then
        // check if the class is persistent, and if the field is managed
        for (; !owner.getName().equals(Object.class.getName());
            owner = owner.getSuperclass()) {
            try {
                owner.getDeclaredField(fieldName);
                break;
            } catch (Exception e) {
            }
        }
        if (owner.getName().equals(Object.class.getName()))
            return null;

        // managed interface
        if (_meta != null && _meta.getDescribedType().isInterface())
            return _meta;

        return _repos.getMetaData(owner, null, false);
    }

    /**
     * Adds all synthetic methods to the bytecode by delegating to
     * the various addXXXMethods () functions in this class. Includes
     * all static field access methods.
     * Note that the 'stock' methods like <code>pcIsTransactional</code>,
     * <code>pcFetchObjectId</code>, etc are defined only in the
     * least-derived PersistenceCapable type.
     */
    private void addPCMethods()
        throws NoSuchMethodException {
        addClearFieldsMethod();
        addNewInstanceMethod(true);
        addNewInstanceMethod(false);
        addManagedFieldCountMethod();
        addReplaceFieldsMethods();
        addProvideFieldsMethods();
        addCopyFieldsMethod();

        if (_meta.getPCSuperclass() == null) {
            addStockMethods();
            addGetVersionMethod();
            addReplaceFlagsMethod();
            addReplaceStateManagerMethod();

            if (_meta.getIdentityType() != ClassMetaData.ID_APPLICATION)
                addNoOpApplicationIdentityMethods();
        }

        // add the app id methods to each subclass rather
        // than just the superclass, since it is possible to have
        // a subclass with an app id hierarchy that matches the
        // persistent class inheritance hierarchy
        if (_meta.getIdentityType() == ClassMetaData.ID_APPLICATION
            && (_meta.getPCSuperclass() == null || _meta.getObjectIdType()
            != _meta.getPCSuperclassMetaData().getObjectIdType())) {
            addCopyKeyFieldsToObjectIdMethod(true);
            addCopyKeyFieldsToObjectIdMethod(false);
            addCopyKeyFieldsFromObjectIdMethod(true);
            addCopyKeyFieldsFromObjectIdMethod(false);
            addNewObjectIdInstanceMethod(true);
            addNewObjectIdInstanceMethod(false);
        }
    }

    /**
     * Add a method to clear all persistent fields; we'll call this from
     * the new instance method to ensure that unloaded fields have
     * default values.
     */
    private void addClearFieldsMethod()
        throws NoSuchMethodException {
        // protected void pcClearFields ()
        BCMethod method = _pc.declareMethod(PRE + "ClearFields", void.class,
            null);
        method.makeProtected();
        Code code = method.getCode(true);

        // super.pcClearFields ()
        if (_meta.getPCSuperclass() != null) {
            code.aload().setThis();
            code.invokespecial().setMethod(getType(_meta.
                getPCSuperclassMetaData()), PRE + "ClearFields", void.class, 
                null);
        }

        FieldMetaData[] fmds = _meta.getDeclaredFields();
        for (int i = 0; i < fmds.length; i++) {
            if (fmds[i].getManagement() != FieldMetaData.MANAGE_PERSISTENT)
                continue;

            loadManagedInstance(code, false);
            switch (fmds[i].getDeclaredTypeCode()) {
                case JavaTypes.BOOLEAN:
                case JavaTypes.BYTE:
                case JavaTypes.CHAR:
                case JavaTypes.INT:
                case JavaTypes.SHORT:
                    code.constant().setValue(0);
                    break;
                case JavaTypes.DOUBLE:
                    code.constant().setValue(0D);
                    break;
                case JavaTypes.FLOAT:
                    code.constant().setValue(0F);
                    break;
                case JavaTypes.LONG:
                    code.constant().setValue(0L);
                    break;
                default:
                    code.constant().setNull();
                    break;
            }

            addSetManagedValueCode(code, fmds[i]);
        }

        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds the <code>pcNewInstance</code> method to the bytecode.
     * These methods are used by the impl helper to create new
     * managed instances efficiently without reflection.
     *
     * @param oid set to true to mimic the method version that takes
     * an oid value as well as a state manager
     */
    private void addNewInstanceMethod(boolean oid) {
        // public PersistenceCapable pcNewInstance (...)
        Class[] args =
            (oid) ? new Class[]{ SMTYPE, Object.class, boolean.class }
                : new Class[]{ SMTYPE, boolean.class };
        BCMethod method = _pc.declareMethod(PRE + "NewInstance", PCTYPE, args);
        Code code = method.getCode(true);

        // if the type is abstract, throw a UserException
        if (_pc.isAbstract()) {
            throwException(code, USEREXCEP);
            code.vreturn();

            code.calculateMaxStack();
            code.calculateMaxLocals();
            return;
        }

        // XXX pc = new XXX ();
        code.anew().setType(_pc);
        code.dup();
        code.invokespecial().setMethod("<init>", void.class, null);
        int inst = code.getNextLocalsIndex();
        code.astore().setLocal(inst);

        // if (clear)
        //   pc.pcClearFields ();
        code.iload().setParam((oid) ? 2 : 1);
        JumpInstruction noclear = code.ifeq();
        code.aload().setLocal(inst);
        code.invokevirtual().setMethod(PRE + "ClearFields", void.class, null);

        // pc.pcStateManager = sm;
        noclear.setTarget(code.aload().setLocal(inst));
        code.aload().setParam(0);
        code.putfield().setField(SM, SMTYPE);

        // pc.pcFlags = LOAD_REQUIRED
        code.aload().setLocal(inst);
        code.constant().setValue(PersistenceCapable.LOAD_REQUIRED);
        code.putfield().setField(PRE + "Flags", byte.class);

        // copy key fields from oid
        if (oid) {
            code.aload().setLocal(inst);
            code.aload().setParam(1);
            code.invokevirtual().setMethod(PRE + "CopyKeyFieldsFromObjectId",
                void.class, new Class[]{ Object.class });
        }

        // return pc;
        code.aload().setLocal(inst);
        code.areturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds the <code>protected static int pcGetManagedFieldCount ()</code>
     * method to the bytecode, returning the inherited field count added
     * to the number of managed fields in the current PersistenceCapable class.
     */
    private void addManagedFieldCountMethod() {
        // protected static int pcGetManagedFieldCount ()
        BCMethod method = _pc.declareMethod(PRE + "GetManagedFieldCount",
            int.class, null);
        method.setStatic(true);
        method.makeProtected();
        Code code = method.getCode(true);

        // return <fields> + pcInheritedFieldCount
        // awhite: the above should work, but I'm seeing a messed up situation
        // all of a sudden where when a subclass calls this method, it somehow
        // happens before <clinit> is ever invoked, and so our
        // pcInheritedFieldCount field isn't initialized!  so instead,
        // return <fields> + <superclass>.pcGetManagedFieldCount ()
        code.constant().setValue(_meta.getDeclaredFields().length);
        if (_meta.getPCSuperclass() != null) {
            code.invokestatic().setMethod(getType(_meta.
                getPCSuperclassMetaData()).getName(),
                PRE + "GetManagedFieldCount", int.class.getName(), null);
            code.iadd();
        }
        code.ireturn();
        code.calculateMaxStack();
    }

    /**
     * Adds the {@link PersistenceCapable#pcProvideField} and
     * {@link PersistenceCapable#pcProvideFields} methods to the bytecode.
     */
    private void addProvideFieldsMethods()
        throws NoSuchMethodException {
        // public void pcProvideField (int fieldNumber)
        BCMethod method = _pc.declareMethod(PRE + "ProvideField", void.class,
            new Class[]{ int.class });
        Code code = method.getCode(true);

        // adds everything through the switch ()
        int relLocal = beginSwitchMethod(PRE + "ProvideField", code);

        // if no fields in this inst, just throw exception
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        if (fmds.length == 0)
            throwException(code, IllegalArgumentException.class);
        else {
            // switch (val)
            code.iload().setLocal(relLocal);
            TableSwitchInstruction tabins = code.tableswitch();
            tabins.setLow(0);
            tabins.setHigh(fmds.length - 1);

            // <field> = pcStateManager.provided<type>Field
            //     (this, fieldNumber);
            for (int i = 0; i < fmds.length; i++) {
                tabins.addTarget(loadManagedInstance(code, false));
                code.getfield().setField(SM, SMTYPE);
                loadManagedInstance(code, false);
                code.iload().setParam(0);
                loadManagedInstance(code, false);
                addGetManagedValueCode(code, fmds[i]);
                code.invokeinterface().setMethod(getStateManagerMethod
                    (fmds[i].getDeclaredType(), "provided", false, false));
                code.vreturn();
            }

            // default: throw new IllegalArgumentException ()
            tabins.setDefaultTarget(throwException
                (code, IllegalArgumentException.class));
        }

        code.calculateMaxStack();
        code.calculateMaxLocals();

        addMultipleFieldsMethodVersion(method);
    }

    /**
     * Adds the {@link PersistenceCapable#pcReplaceField} and
     * {@link PersistenceCapable#pcReplaceFields} methods to the bytecode.
     */
    private void addReplaceFieldsMethods()
        throws NoSuchMethodException {
        // public void pcReplaceField (int fieldNumber)
        BCMethod method = _pc.declareMethod(PRE + "ReplaceField", void.class,
            new Class[]{ int.class });
        Code code = method.getCode(true);

        // adds everything through the switch ()
        int relLocal = beginSwitchMethod(PRE + "ReplaceField", code);

        // if no fields in this inst, just throw exception
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        if (fmds.length == 0)
            throwException(code, IllegalArgumentException.class);
        else {
            // switch (val)
            code.iload().setLocal(relLocal);
            TableSwitchInstruction tabins = code.tableswitch();
            tabins.setLow(0);
            tabins.setHigh(fmds.length - 1);

            // <field> = pcStateManager.replace<type>Field
            //  (this, fieldNumber);
            for (int i = 0; i < fmds.length; i++) {
                // for the addSetManagedValueCode call below.
                tabins.addTarget(loadManagedInstance(code, false));

                loadManagedInstance(code, false);
                code.getfield().setField(SM, SMTYPE);
                loadManagedInstance(code, false);
                code.iload().setParam(0);
                code.invokeinterface().setMethod(getStateManagerMethod
                    (fmds[i].getDeclaredType(), "replace", true, false));
                if (!fmds[i].getDeclaredType().isPrimitive())
                    code.checkcast().setType(fmds[i].getDeclaredType());

                addSetManagedValueCode(code, fmds[i]);
                code.vreturn();
            }

            // default: throw new IllegalArgumentException ()
            tabins.setDefaultTarget(throwException
                (code, IllegalArgumentException.class));
        }

        code.calculateMaxStack();
        code.calculateMaxLocals();

        addMultipleFieldsMethodVersion(method);
    }

    /**
     * Adds the {@link PersistenceCapable#pcCopyFields} method to the bytecode.
     */
    private void addCopyFieldsMethod()
        throws NoSuchMethodException {
        // public void pcCopyField (Object pc, int field)
        BCMethod method = _pc.declareMethod(PRE + "CopyField",
            void.class.getName(),
            new String[]{ _pc.getName(), int.class.getName() });
        method.makeProtected();
        Code code = method.getCode(true);

        // adds everything through the switch ()
        int relLocal = beginSwitchMethod(PRE + "CopyField", code);

        // if no fields in this inst, just throw exception
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        if (fmds.length == 0)
            throwException(code, IllegalArgumentException.class);
        else {
            // switch (val)
            code.iload().setLocal(relLocal);
            TableSwitchInstruction tabins = code.tableswitch();
            tabins.setLow(0);
            tabins.setHigh(fmds.length - 1);

            for (int i = 0; i < fmds.length; i++) {
                // <field> = other.<field>;
                // or set<field> (other.get<field>);
                tabins.addTarget(loadManagedInstance(code, false));
                code.aload().setParam(0);
                addGetManagedValueCode(code, fmds[i]);
                addSetManagedValueCode(code, fmds[i]);

                // break;
                code.vreturn();
            }

            // default: throw new IllegalArgumentException ()
            tabins.setDefaultTarget(throwException
                (code, IllegalArgumentException.class));
        }

        code.calculateMaxStack();
        code.calculateMaxLocals();

        addMultipleFieldsMethodVersion(method);
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
    private int beginSwitchMethod(String name, Code code) {
        boolean copy = (PRE + "CopyField").equals(name);
        int fieldNumber = (copy) ? 1 : 0;

        // int rel = fieldNumber - pcInheritedFieldCount
        int relLocal = code.getNextLocalsIndex();
        code.iload().setParam(fieldNumber);
        code.getstatic().setField(INHERIT, int.class);
        code.isub();
        code.istore().setLocal(relLocal);
        code.iload().setLocal(relLocal);

        // super: if (rel < 0) super.pcReplaceField (fieldNumber); return;
        // no super: if (rel < 0) throw new IllegalArgumentException ();
        JumpInstruction ifins = code.ifge();
        if (_meta.getPCSuperclass() != null) {
            loadManagedInstance(code, false);
            String[] args;
            if (copy) {
                args = new String[]{ getType(_meta.getPCSuperclassMetaData()).
                    getName(), int.class.getName() };
                code.aload().setParam(0);
            } else
                args = new String[]{ int.class.getName() };
            code.iload().setParam(fieldNumber);
            code.invokespecial().setMethod(getType(_meta.
                getPCSuperclassMetaData()).getName(), name, 
                void.class.getName(), args);
            code.vreturn();
        } else
            throwException(code, IllegalArgumentException.class);

        ifins.setTarget(code.nop());
        return relLocal;
    }

    /**
     * This helper method, given the pcReplaceField or pcProvideField
     * method, adds the bytecode for the corresponding 'plural' version
     * of the method -- the version that takes an int[] of fields to
     * to access rather than a single field. The multiple fields version
     * simply loops through the provided indexes and delegates to the
     * singular version for each one.
     */
    private void addMultipleFieldsMethodVersion(BCMethod single) {
        boolean copy = (PRE + "CopyField").equals(single.getName());

        // public void <method>s (int[] fields)
        Class[] args = (copy) ? new Class[]{ Object.class, int[].class }
            : new Class[]{ int[].class };
        BCMethod method = _pc.declareMethod(single.getName() + "s",
            void.class, args);
        Code code = method.getCode(true);

        int fieldNumbers = 0;
        int inst = 0;
        if (copy) {
            fieldNumbers = 1;

            // XXX other = (XXX) pc;
            code.aload().setParam(0);
            code.checkcast().setType(_pc);
            inst = code.getNextLocalsIndex();
            code.astore().setLocal(inst);

            // if (other.pcStateManager != pcStateManager)
            //	throw new IllegalArgumentException
            code.aload().setLocal(inst);
            code.getfield().setField(SM, SMTYPE);
            loadManagedInstance(code, false);
            code.getfield().setField(SM, SMTYPE);
            JumpInstruction ifins = code.ifacmpeq();
            throwException(code, IllegalArgumentException.class);
            ifins.setTarget(code.nop());

            // if (pcStateManager == null)
            //  throw new IllegalStateException
            loadManagedInstance(code, false);
            code.getfield().setField(SM, SMTYPE);
            ifins = code.ifnonnull();
            throwException(code, IllegalStateException.class);
            ifins.setTarget(code.nop());
        }

        // for (int i = 0;
        code.constant().setValue(0);
        int idx = code.getNextLocalsIndex();
        code.istore().setLocal(idx);
        JumpInstruction testins = code.go2();

        // <method> (fields[i]);
        Instruction bodyins = loadManagedInstance(code, false);
        if (copy)
            code.aload().setLocal(inst);
        code.aload().setParam(fieldNumbers);
        code.iload().setLocal(idx);
        code.iaload();
        code.invokevirtual().setMethod(single);

        // i++;
        code.iinc().setIncrement(1).setLocal(idx);

        // i < fields.length
        testins.setTarget(code.iload().setLocal(idx));
        code.aload().setParam(fieldNumbers);
        code.arraylength();
        code.ificmplt().setTarget(bodyins);
        code.vreturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds the 'stock' methods to the bytecode; these include methods
     * like {@link PersistenceCapable#pcFetchObjectId}
     * and {@link PersistenceCapable#pcIsTransactional}.
     */
    private void addStockMethods()
        throws NoSuchMethodException {
        // pcGetGenericContext
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod
            ("get" + CONTEXTNAME, (Class[]) null));

        // pcFetchObjectId
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod
            ("fetchObjectId", (Class[]) null));

        // pcIsDeleted
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod
            ("isDeleted", (Class[]) null));

        // pcIsDirty
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod
            ("isDirty", (Class[]) null));

        // pcIsNew
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod
            ("isNew", (Class[]) null));

        // pcIsPersistent
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod
            ("isPersistent", (Class[]) null));

        // pcIsTransactional
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod
            ("isTransactional", (Class[]) null));

        // pcSerializing
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod
            ("serializing", (Class[]) null));

        // pcDirty
        translateFromStateManagerMethod(SMTYPE.getDeclaredMethod("dirty",
            new Class[]{ String.class }));

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

    /**
     * Helper method to add a stock method to the bytecode. Each
     * stock method simply delegates to a corresponding StateManager method.
     * Given the StateManager method, then, this function translates it into
     * the wrapper method that should be added to the bytecode.
     */
    private void translateFromStateManagerMethod(Method m) {
        // form the name of the method by prepending 'pc' to the sm method
        String name = PRE + StringUtils.capitalize(m.getName());
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

        // return pcStateManager.<method> (<args>);
        ifins.setTarget(loadManagedInstance(code, false));
        code.getfield().setField(SM, SMTYPE);
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
            Class wrapper = unwrapVersionField(versionField);
            if (wrapper != null) {
                code.anew().setType(wrapper);
                code.dup();
            }
            loadManagedInstance(code, false);
            addGetManagedValueCode(code, versionField);
            if (wrapper != null)
                code.invokespecial().setMethod(wrapper, "<init>", void.class,
                    new Class[]{ versionField.getDeclaredType() });
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
    private Class unwrapVersionField(FieldMetaData fmd) {
        switch (fmd.getDeclaredTypeCode()) {
            case JavaTypes.BYTE:
                return Byte.class;
            case JavaTypes.CHAR:
                return Character.class;
            case JavaTypes.INT:
                return Integer.class;
            case JavaTypes.SHORT:
                return Short.class;
            case JavaTypes.LONG:
                return Long.class;
        }
        return null;
    }

    /**
     * Adds the {@link PersistenceCapable#pcReplaceFlags}
     * method to the bytecode.
     */
    private void addReplaceFlagsMethod() {
        // public final void pcReplaceFlags ()
        BCMethod method = _pc.declareMethod(PRE + "ReplaceFlags",
            void.class, null);
        Code code = method.getCode(true);

        // if (pcStateManager != null)
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        JumpInstruction ifins = code.ifnonnull();
        code.vreturn();

        // pcFlags = pcStateManager.replaceFlags ();
        ifins.setTarget(loadManagedInstance(code, false));
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        code.invokeinterface().setMethod(SMTYPE, "replaceFlags",
            byte.class, null);
        code.putfield().setField(PRE + "Flags", byte.class);
        code.vreturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds the {@link PersistenceCapable#pcReplaceStateManager}
     * method to the bytecode.
     */
    private void addReplaceStateManagerMethod() {
        // public void pcReplaceStateManager (StateManager sm)
        BCMethod method = _pc.declareMethod(PRE + "ReplaceStateManager",
            void.class, new Class[]{ SMTYPE });
        method.setSynchronized(true);
        method.getExceptions(true).addException(SecurityException.class);
        Code code = method.getCode(true);

        // if (pcStateManager != null)
        //	pcStateManager = pcStateManager.replaceStateManager(sm);
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        JumpInstruction ifins = code.ifnull();
        loadManagedInstance(code, false);
        loadManagedInstance(code, false);
        code.getfield().setField(SM, SMTYPE);
        code.aload().setParam(0);
        code.invokeinterface().setMethod(SMTYPE, "replaceStateManager",
            SMTYPE, new Class[]{ SMTYPE });
        code.putfield().setField(SM, SMTYPE);
        code.vreturn();

        // SecurityManager sec = System.getSecurityManager ();
        // if (sec != null)
        //		sec.checkPermission (Permission.SET_STATE_MANAGER);
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
        // 	Object oid)
        BCMethod method = _pc.declareMethod(PRE + "CopyKeyFieldsToObjectId",
            void.class, new Class[]{ OIDFSTYPE, Object.class });
        Code code = method.getCode(true);
        code.vreturn();
        code.calculateMaxLocals();

        // public void pcCopyKeyFieldsToObjectId (Object oid)
        method = _pc.declareMethod(PRE + "CopyKeyFieldsToObjectId",
            void.class, new Class[]{ Object.class });
        code = method.getCode(true);
        code.vreturn();
        code.calculateMaxLocals();

        // public void pcCopyKeyFieldsFromObjectId (ObjectIdFieldConsumer fc,
        //	Object oid)
        method = _pc.declareMethod(PRE + "CopyKeyFieldsFromObjectId",
            void.class, new Class[]{ OIDFCTYPE, Object.class });
        code = method.getCode(true);
        code.vreturn();
        code.calculateMaxLocals();

        // public void pcCopyKeyFieldsFromObjectId (Object oid)
        method = _pc.declareMethod(PRE + "CopyKeyFieldsFromObjectId",
            void.class, new Class[]{ Object.class });
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
            Object.class, new Class[]{ Object.class });
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
        //	Object oid)
        String[] args = (fieldManager) ?
            new String[]{ OIDFSTYPE.getName(), Object.class.getName() }
            : new String[]{ Object.class.getName() };
        BCMethod method = _pc.declareMethod(PRE + "CopyKeyFieldsToObjectId",
            void.class.getName(), args);
        Code code = method.getCode(true);

        // single field identity always throws exception
        if (_meta.isOpenJPAIdentity()) {
            throwException(code, INTERNEXCEP);
            code.vreturn();

            code.calculateMaxStack();
            code.calculateMaxLocals();
            return;
        }

        // call superclass method
        if (_meta.getPCSuperclass() != null) {
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

        if (!_meta.isOpenJPAIdentity() && _meta.isObjectIdTypeShared()) {
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
        code.getstatic().setField(INHERIT, int.class);
        int inherited = code.getNextLocalsIndex();
        code.istore().setLocal(inherited);

        // id.<field> = fs.fetch<type>Field (<index>); or...
        // id.<field> = pc.<field>;
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        Class type;
        String name;
        for (int i = 0; i < fmds.length; i++) {
            if (!fmds[i].isPrimaryKey())
                continue;

            type = fmds[i].getDeclaredType();
            name = fmds[i].getName();
            code.aload().setLocal(id);
            if (fieldManager) {
                code.aload().setParam(0);
                code.constant().setValue(i);
                code.iload().setLocal(inherited);
                code.iadd();
                code.invokeinterface().setMethod
                    (getFieldSupplierMethod(type));

                // if the type of this field meta data is
                // non-primitive and non-string, be sure to cast
                // to the appropriate type.
                if (!type.isPrimitive()
                    && !type.getName().equals(String.class.getName()))
                    code.checkcast().setType(type);
            } else {
                loadManagedInstance(code, false);
                addGetManagedValueCode(code, fmds[i]);
            }

            if (_meta.getAccessType() == ClassMetaData.ACCESS_FIELD)
                code.putfield().setField(findDeclaredField(oidType,
                    name));
            else
                code.invokevirtual().setMethod(findDeclaredMethod
                    (oidType, "set" + StringUtils.capitalize(name),
                        new Class[]{ type }));
        }
        code.vreturn();

        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds the <code>pcCopyKeyFieldsFromObjectId</code> methods
     * to classes using application identity.
     */
    private void addCopyKeyFieldsFromObjectIdMethod(boolean fieldManager)
        throws NoSuchMethodException {
        // public void pcCopyKeyFieldsFromObjectId (ObjectIdFieldConsumer fc,
        //	Object oid)
        String[] args = (fieldManager) ?
            new String[]{ OIDFCTYPE.getName(), Object.class.getName() }
            : new String[]{ Object.class.getName() };
        BCMethod method = _pc.declareMethod(PRE + "CopyKeyFieldsFromObjectId",
            void.class.getName(), args);
        Code code = method.getCode(true);

        // call superclass method
        if (_meta.getPCSuperclass() != null) {
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

        // int inherited = pcInheritedFieldCount;
        int inherited = 0;
        if (fieldManager) {
            code.getstatic().setField(INHERIT, int.class);
            inherited = code.getNextLocalsIndex();
            code.istore().setLocal(inherited);
        }

        // fs.store<type>Field (<index>, id.<field>); or...
        // this.<field> = id.<field>
        // or for single field identity: id.getId ()
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        Class type;
        Class unwrapped;
        String name;
        for (int i = 0; i < fmds.length; i++) {
            if (!fmds[i].isPrimaryKey())
                continue;

            name = fmds[i].getName();
            type = fmds[i].getDeclaredType();
            unwrapped = unwrapSingleFieldIdentity(fmds[i]);

            if (fieldManager) {
                code.aload().setParam(0);
                code.constant().setValue(i);
                code.iload().setLocal(inherited);
                code.iadd();
            } else
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
                    if (!fieldManager && fmds[i].getDeclaredType()
                        != Object.class)
                        code.checkcast().setType(fmds[i].getDeclaredType());
                } else {
                    code.invokevirtual().setMethod(oidType, "getId",
                        unwrapped, null);
                    if (unwrapped != type)
                        code.invokespecial().setMethod(type, "<init>",
                            void.class, new Class[]{ unwrapped });
                }
            } else if (_meta.getAccessType() == ClassMetaData.ACCESS_FIELD)
                code.getfield().setField(findDeclaredField(oidType, name));
            else // property
                code.invokevirtual().setMethod(findDeclaredGetterMethod
                    (oidType, StringUtils.capitalize(name)));

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
            oidType.getConstructor(new Class[]{ Class.class, String.class });
            return Boolean.TRUE;
        } catch (Throwable t) {
        }
        try {
            oidType.getConstructor(new Class[]{ String.class });
            return Boolean.FALSE;
        } catch (Throwable t) {
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
        Class[] args = (obj) ? new Class[]{ Object.class } : null;
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
                "<init>", void.class, new Class[]{ String.class });
            code.athrow();
            code.vreturn();

            code.calculateMaxStack();
            code.calculateMaxLocals();
            return;
        }

        if (!_meta.isOpenJPAIdentity() && _meta.isObjectIdTypeShared()) {
            // new ObjectId (cls, oid)
            code.anew().setType(ObjectId.class);
            code.dup();
            code.classconstant().setClass(getType(_meta));
        }

        // new <oid class> ();
        code.anew().setType(oidType);
        code.dup();
        if (_meta.isOpenJPAIdentity() || (obj && usesClsString == Boolean.TRUE))
            code.classconstant().setClass(getType(_meta));
        if (obj) {
            code.aload().setParam(0);
            code.checkcast().setType(String.class);
            if (usesClsString == Boolean.TRUE)
                args = new Class[]{ Class.class, String.class };
            else if (usesClsString == Boolean.FALSE)
                args = new Class[]{ String.class };
        } else if (_meta.isOpenJPAIdentity()) {
            // new <type>Identity (XXX.class, <pk>);
            loadManagedInstance(code, false);
            FieldMetaData pk = _meta.getPrimaryKeyFields()[0];
            addGetManagedValueCode(code, pk);
            if (_meta.getObjectIdType() == ObjectId.class)
                args = new Class[]{ Class.class, Object.class };
            else
                args = new Class[]{ Class.class, pk.getDeclaredType() };
        }

        code.invokespecial().setMethod(oidType, "<init>", void.class, args);
        if (!_meta.isOpenJPAIdentity() && _meta.isObjectIdTypeShared())
            code.invokespecial().setMethod(ObjectId.class, "<init>",
                void.class, new Class[]{ Class.class, Object.class });
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
     * @param type the type of state being passed
     * @param prefix the prefix of the method to call; all methods
     * end in '[state type]Field'; only the prefix varies
     * @param get true if receiving information from the
     * StateManager, false if passing it to the SM
     * @param curValue true if the current state value is passed to
     * the StateManager as an extra argument
     */
    private Method getStateManagerMethod(Class type, String prefix,
        boolean get, boolean curValue)
        throws NoSuchMethodException {
        return getMethod(SMTYPE, type, prefix, get, true, curValue);
    }

    /**
     * Return the method of the given owner type matching the given criteria.
     *
     * @param type the type of state being passed
     * @param prefix the prefix of the method to call; all methods
     * end in '[state type]Field'; only the prefix varies
     * @param get true if receiving information from the
     * owner, false if passing it to the owner
     * @param haspc true if the pc is passed as an extra argument
     * @param curValue true if the current state value is passed to
     * the owner as an extra argument
     */
    private Method getMethod(Class owner, Class type, String prefix,
        boolean get, boolean haspc, boolean curValue)
        throws NoSuchMethodException {
        // all methods end in [field type]Field, where the field type
        // can be any of the primitve types (but capitalized), 'String',
        // or 'Object'; figure out what type to use
        String typeName = type.getName();
        if (type.isPrimitive())
            typeName = typeName.substring(0, 1).toUpperCase()
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
        return owner.getDeclaredMethod(name, params);
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
     * Adds the PersistenceCapable interface to the class being enhanced, and
     * adds a default constructor for use by OpenJPA if it is not already present.
     */
    private void enhanceClass() {
        // make the class implement PersistenceCapable
        _pc.declareInterface(PCTYPE);

        // find the default constructor
        BCMethod method = _pc.getDeclaredMethod("<init>", (String[]) null);

        // a default constructor is required
        if (method == null) {
            Class type = _pc.getType();
            if (!_defCons)
                throw new UserException(_loc.get("enhance-defaultconst",
                    type));

            method = _pc.addDefaultConstructor();
            String access;
            if (_meta.isDetachable()) {
                // externalizable requires that the constructor
                // be public, so make the added constructor public
                method.makePublic();
                access = "public";
            } else if (_pc.isFinal()) {
                method.makePrivate();
                access = "private";
            } else {
                method.makeProtected();
                access = "protected";
            }
            if (_log.isWarnEnabled())
                _log.warn(_loc.get("enhance-adddefaultconst", type, access));
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
     * <li><code>protected transient byte pcFlags</code>
     * if no PersistenceCapable superclass present)</li>
     * </ul>
     */
    private void addFields() {
        _pc.declareField(INHERIT, int.class).setStatic(true);
        _pc.declareField(PRE + "FieldNames", String[].class).setStatic(true);
        _pc.declareField(PRE + "FieldTypes", Class[].class).setStatic(true);
        _pc.declareField(PRE + "FieldFlags", byte[].class).setStatic(true);
        _pc.declareField(SUPER, Class.class).setStatic(true);

        if (_meta.getPCSuperclass() == null) {
            BCField field = _pc.declareField(SM, SMTYPE);
            field.makeProtected();
            field.setTransient(true);

            field = _pc.declareField(PRE + "Flags", byte.class);
            field.makeProtected();
            field.setTransient(true);
        }
    }

    /**
     * Modifies the class initialization method (creating one if necessary)
     * to initialize the static fields of the PersistenceCapable instance and
     * to register it with the impl helper.
     */
    private void addStaticInitializer() {
        Code code = getOrCreateClassInitCode(true);
        if (_meta.getPCSuperclass() != null) {
            // pcInheritedFieldCount = <superClass>.pcGetManagedFieldCount()
            code.invokestatic().setMethod(getType(_meta.
                getPCSuperclassMetaData()).getName(), 
                PRE + "GetManagedFieldCount", int.class.getName(), null);
            code.putstatic().setField(INHERIT, int.class);

            // pcPCSuperclass = <superClass>;
            code.classconstant().setClass(getType(_meta.
                getPCSuperclassMetaData()));
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
        code.classconstant().setClass(_pc);
        code.getstatic().setField(PRE + "FieldNames", String[].class);
        code.getstatic().setField(PRE + "FieldTypes", Class[].class);
        code.getstatic().setField(PRE + "FieldFlags", byte[].class);
        code.getstatic().setField(SUPER, Class.class);
        code.constant().setValue(_meta.getTypeAlias());
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

        // if not already present, add a serialVersionUID field; if the instance
        // is detachable and uses detached state without a declared field,
        // can't add a serial version UID because we'll be adding extra fields
        // to the enhanced version
        BCField field = _pc.getDeclaredField("serialVersionUID");
        if (field == null) {
            Long uid = null;
            try {
                uid = Numbers.valueOf(ObjectStreamClass.lookup
                    (_meta.getDescribedType()).getSerialVersionUID());
            } catch (Throwable t) {
                // last-chance catch for bug #283 (which can happen
                // in a variety of ClassLoading environments)
                _log.warn(_loc.get("enhance-uid-access", _meta), t);
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
            new Class[]{ ObjectOutputStream.class });
        boolean full = write == null;
        if (full) {
            // private void writeObject (ObjectOutputStream out)
            write = _pc.declareMethod("writeObject", void.class,
                new Class[]{ ObjectOutputStream.class });
            write.getExceptions(true).addException(IOException.class);
            write.makePrivate();
        }
        modifyWriteObjectMethod(write, full);

        // and read object
        BCMethod read = _pc.getDeclaredMethod("readObject",
            new Class[]{ ObjectInputStream.class });
        full = read == null;
        if (full) {
            // private void readObject (ObjectInputStream in)
            read = _pc.declareMethod("readObject", void.class,
                new Class[]{ ObjectInputStream.class });
            read.getExceptions(true).addException(IOException.class);
            read.getExceptions(true).addException
                (ClassNotFoundException.class);
            read.makePrivate();
        }
        modifyReadObjectMethod(read, full);
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

        Instruction tmplate = new Code().vreturn();
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
                void.class, new Class[]{ Object.class });
            toret.setTarget(ret);
            code.next(); // jump over return
        }
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Adds a custom readObject method that delegates to the
     * {@link ObjectInputStream#readObject} method.
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
                void.class, new Class[]{ Object.class });
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
        // public boolean pcIsDetached ()
        BCMethod method = _pc.declareMethod(PRE + "IsDetached",
            Boolean.class, null);
        method.makePublic();
        Code code = method.getCode(true);
        writeIsDetachedMethod(code);
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Creates the body of the pcIsDetached() method to determine if an
     * instance is detached.
     */
    private void writeIsDetachedMethod(Code code)
        throws NoSuchMethodException {
        // not detachable: return Boolean.FALSE
        if (!_meta.isDetachable()) {
            code.getstatic().setField(Boolean.class, "FALSE", Boolean.class);
            code.areturn();
            return;
        }

        // if (sm != null)
        //		return (sm.isDetached ()) ? Boolean.TRUE : Boolean.FALSE;
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
                return;
            }
        }

        // create artificial target to simplify
        target = code.nop();
        ifins.setTarget(target);
        if (notdeser != null)
            notdeser.setTarget(target);

        // allow users with version fields to manually construct a "detached"
        // instance, so check version before taking into account non-existent
        // detached state

        // consider detached if version is non-default
        FieldMetaData version = _meta.getVersionField();
        if (state != Boolean.TRUE && version != null) {
            // if (<version> != <default>)
            //		return true;
            loadManagedInstance(code, false);
            addGetManagedValueCode(code, version);
            ifins = ifDefaultValue(code, version);
            code.getstatic().setField(Boolean.class, "TRUE", Boolean.class);
            code.areturn();
            ifins.setTarget(code.getstatic().setField(Boolean.class, "FALSE",
                Boolean.class));
            code.areturn();
            return;
        }

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
            return;
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

        // consider detached if auto-genned primary keys are non-default
        ifins = null;
        JumpInstruction ifins2 = null;
        if (state != Boolean.TRUE
            && _meta.getIdentityType() == ClassMetaData.ID_APPLICATION) {
            // for each pk field:
            // if (<pk> != <default> [&& !"".equals (<pk>)])
            //		return Boolean.TRUE;
            FieldMetaData[] pks = _meta.getPrimaryKeyFields();
            for (int i = 0; i < pks.length; i++) {
                if (pks[i].getValueStrategy() == ValueStrategies.NONE)
                    continue;

                target = loadManagedInstance(code, false);
                if (ifins != null)
                    ifins.setTarget(target);
                if (ifins2 != null)
                    ifins2.setTarget(target);
                ifins2 = null;

                addGetManagedValueCode(code, pks[i]);
                ifins = ifDefaultValue(code, pks[i]);
                if (pks[i].getDeclaredTypeCode() == JavaTypes.STRING) {
                    code.constant().setValue("");
                    loadManagedInstance(code, false);
                    addGetManagedValueCode(code, pks[i]);
                    code.invokevirtual().setMethod(String.class, "equals",
                        boolean.class, new Class[]{ Object.class });
                    ifins2 = code.ifne();
                }
                code.getstatic().setField(Boolean.class, "TRUE",
                    Boolean.class);
                code.areturn();
            }
        }

        // give up; we just don't know
        target = code.constant().setNull();
        if (ifins != null)
            ifins.setTarget(target);
        if (ifins2 != null)
            ifins2.setTarget(target);
        code.areturn();
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
    private Code getOrCreateClassInitCode(boolean replaceLast) {
        BCMethod clinit = _pc.getDeclaredMethod("<clinit>");
        Code code;
        if (clinit != null) {
            code = clinit.getCode(true);
            if (replaceLast) {
                Code template = new Code();
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
     * enhanced to correctly replace the <code>pcFlags</code> and
     * <code>pcStateManager</code> instance fields of any clone created with
     * their default values. Also, if this class is the base PC type
     * and does not declared a clone method, one will be added.
     */
    private void addCloningCode() {
        if (_meta.getPCSuperclass() != null)
            return;

        // add the clone method if necessary
        BCMethod clone = _pc.getDeclaredMethod("clone", (String[]) null);
        String superName = _pc.getSuperclassName();
        Code code = null;
        if (clone == null) {
            // add clone support for base classes
            // which also implement cloneable
            if (!_pc.isInstanceOf(Cloneable.class)
                || !superName.equals(Object.class.getName()))
                return;

            if (_log.isTraceEnabled())
                _log.trace(_loc.get("enhance-cloneable", _pc.getType()));

            // add clone method
            // protected Object clone () throws CloneNotSupportedException
            clone = _pc.declareMethod("clone", Object.class, null);
            clone.makeProtected();
            clone.getExceptions(true).addException
                (CloneNotSupportedException.class);
            code = clone.getCode(true);

            // return super.clone ();
            loadManagedInstance(code, false);
            code.invokespecial().setMethod(_pc.getSuperclassName(),
                "clone", Object.class.getName(), null);
            code.areturn();
        } else {
            // get the clone method code
            code = clone.getCode(false);
            if (code == null)
                return;
        }

        // create template super.clone () instruction to match against
        Instruction template = new Code().invokespecial().setMethod
            (superName, "clone", Object.class.getName(), null);

        // find calls to the template instruction; on match
        // clone will be on stack
        code.beforeFirst();
        if (code.searchForward(template)) {
            // ((<type>) clone).pcStateManager = null;
            code.dup();
            code.checkcast().setType(_pc);
            code.constant().setNull();
            code.putfield().setField(SM, SMTYPE);

            // ((<type>) clone).pcFlags = 0;
            code.dup();
            code.checkcast().setType(_pc);
            code.constant().setValue(PersistenceCapable.READ_WRITE_OK);
            code.putfield().setField(PRE + "Flags", byte.class);

            // if modified, increase stack
            code.calculateMaxStack();
            code.calculateMaxLocals();
        }
    }

    /**
     * Enhance the PC's object id class.
     */
    private boolean enhanceObjectId()
        throws IOException {
        Class cls = _meta.getObjectIdType();
        if (cls == null)
            return false;

        FieldMetaData[] pks = _meta.getPrimaryKeyFields();
        int access = _meta.getAccessType();
        if (_meta.isOpenJPAIdentity()) {
            if (pks[0].getDeclaredTypeCode() != JavaTypes.OID)
                return false;
            cls = pks[0].getDeclaredType();
            access = pks[0].getEmbeddedMetaData().getAccessType();
            pks = pks[0].getEmbeddedMetaData().getFields();
        }

        String cap;
        for (int i = 0; i < pks.length; i++) {
            if (access == ClassMetaData.ACCESS_FIELD)
                makeObjectIdFieldPublic(findDeclaredField(cls,
                    pks[i].getName()));
            else // property
            {
                cap = StringUtils.capitalize(pks[i].getName());
                makeObjectIdMethodPublic(findDeclaredGetterMethod(cls, cap));
                makeObjectIdMethodPublic(findDeclaredMethod(cls, "set" + cap,
                    new Class[]{ pks[i].getDeclaredType() }));
            }
        }
        return _oids != null;
    }

    /**
     * Find the given (possibly private) field.
     */
    private Field findDeclaredField(Class cls, String name) {
        if (cls == null || cls == Object.class)
            return null;

        try {
            return cls.getDeclaredField(name);
        } catch (NoSuchFieldException nsfe) {
            return findDeclaredField(cls.getSuperclass(), name);
        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Return the getter method for the given capitalized property name.
     */
    private Method findDeclaredGetterMethod(Class cls, String baseName) {
        Method meth = findDeclaredMethod(cls, "get" + baseName, null);
        if (meth != null)
            return meth;
        return findDeclaredMethod(_meta.getObjectIdType(), "is" + baseName,
            null);
    }

    /**
     * Find the given (possibly private) method.
     */
    private Method findDeclaredMethod(Class cls, String name, Class[] params) {
        if (cls == null || cls == Object.class)
            return null;

        try {
            return cls.getDeclaredMethod(name, params);
        } catch (NoSuchMethodException nsme) {
            return findDeclaredMethod(cls.getSuperclass(), name, params);
        } catch (Exception e) {
            throw new GeneralException(e);
        }
    }

    /**
     * Ensure that the given oid field is public.
     */
    private void makeObjectIdFieldPublic(Field field) {
        if (Modifier.isPublic(field.getModifiers()))
            return;

        BCClass bc = getObjectIdBytecode(field.getDeclaringClass());
        bc.getDeclaredField(field.getName()).makePublic();
    }

    /**
     * Ensure that the given oid method is public.
     */
    private void makeObjectIdMethodPublic(Method meth) {
        if (Modifier.isPublic(meth.getModifiers()))
            return;

        BCClass bc = getObjectIdBytecode(meth.getDeclaringClass());
        bc.getDeclaredMethod(meth.getName(), meth.getParameterTypes()).
            makePublic();
    }

    /**
     * Return the bytecode for the given oid class, creating and caching it
     * if necessary.
     */
    private BCClass getObjectIdBytecode(Class cls) {
        BCClass bc = _pc.getProject().loadClass(cls);
        if (_oids == null)
            _oids = new ArrayList(3);
        if (!_oids.contains(bc)) {
            if (_log.isTraceEnabled())
                _log.trace(_loc.get("enhance-oid", bc.getName()));
            _oids.add(bc);
        }
        return bc;
    }

    /**
     * Gets the auxiliary enhancers registered as {@link Services services}.
     * Multi-call safe -- the first call locates the auxiliary enhancers,
     * subsequent calls merely returns the existing set.
     * 
     * @return array of auxiliary enhancers. empty array if none is registered.
     */
    public AuxiliaryEnhancer[] getAuxiliaryEnhancers() {
		if (_auxEnhancers == null) {
		    try {
                Class[] classes = Services.getImplementorClasses(
                    AuxiliaryEnhancer.class, 
                    AuxiliaryEnhancer.class.getClassLoader());
                _auxEnhancers = new AuxiliaryEnhancer[classes.length];
                for (int i = 0; i < _auxEnhancers.length; i++)
                    _auxEnhancers[i] = (AuxiliaryEnhancer) classes[i].
                        newInstance();
		    } catch (Throwable t) {
			    throw new GeneralException(t);
		    }
		}
    	return _auxEnhancers;	
    }
    
    /**
     * Allow any registered auxiliary code generators to run.
     */
    private void runAuxiliaryEnhancers() {
    	AuxiliaryEnhancer[] auxEnhancers = getAuxiliaryEnhancers();
    	for (int i = 0; i < auxEnhancers.length; i++)
    		auxEnhancers[i].run(_pc, _meta);
    }
    
    /**
     * Affirms if the given method be skipped.
     * 
     * @param method method to be skipped or not
     * @return true if any of the auxiliary enhancers skips the given method
     */
    private boolean skipEnhance(BCMethod method) {
    	AuxiliaryEnhancer[] auxEnhancers = getAuxiliaryEnhancers();
    	for (int i = 0; i < auxEnhancers.length; i++)
    		if (auxEnhancers[i].skipEnhance(method))
    			return true;
    	return false;
    }

    /**
     * Adds synthetic field access methods that will replace all direct
     * field accesses.
     */
    private void addAccessors()
        throws NoSuchMethodException {
        FieldMetaData[] fmds = _meta.getDeclaredFields();
        for (int i = 0; i < fmds.length; i++) {
            addGetMethod(i, fmds[i]);
            addSetMethod(i, fmds[i]);
        }
    }

    /**
     * Adds a static getter method for the given field.
     * The generated method interacts with the instance state and the
     * StateManager to get the value of the field.
     *
     * @param index the relative number of the field
     * @param fmd metadata about the field to get
     */
    private void addGetMethod(int index, FieldMetaData fmd)
        throws NoSuchMethodException {
        BCMethod method = createGetMethod(fmd);
        Code code = method.getCode(true);

        // if reads are not checked, just return the value
        byte fieldFlag = getFieldFlag(fmd);
        if ((fieldFlag & PersistenceCapable.CHECK_READ) == 0
            && (fieldFlag & PersistenceCapable.MEDIATE_READ) == 0) {
            loadManagedInstance(code, true);
            addGetManagedValueCode(code, fmd);
            code.xreturn().setType(fmd.getDeclaredType());

            code.calculateMaxStack();
            code.calculateMaxLocals();
            return;
        }

        // dfg: if (inst.pcFlags <= 0) return inst.<field>;
        JumpInstruction ifins = null;
        if ((fieldFlag & PersistenceCapable.CHECK_READ) > 0) {
            loadManagedInstance(code, true);
            code.getfield().setField(PRE + "Flags", byte.class);
            ifins = code.ifgt();
            loadManagedInstance(code, true);
            addGetManagedValueCode(code, fmd);
            code.xreturn().setType(fmd.getDeclaredType());
        }

        // if (inst.pcStateManager == null) return inst.<field>;
        Instruction ins = loadManagedInstance(code, true);
        if (ifins != null)
            ifins.setTarget(ins);
        code.getfield().setField(SM, SMTYPE);
        ifins = code.ifnonnull();
        loadManagedInstance(code, true);
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
        loadManagedInstance(code, true);
        code.getfield().setField(SM, SMTYPE);
        code.iload().setLocal(fieldLocal);
        code.invokeinterface().setMethod(SMTYPE, "accessingField", void.class,
            new Class[]{ int.class });
        loadManagedInstance(code, true);
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
     * @param fmd metadata about the field to set
     */
    private void addSetMethod(int index, FieldMetaData fmd)
        throws NoSuchMethodException {
        BCMethod method = createSetMethod(fmd);
        Code code = method.getCode(true);

        // PCEnhancer uses static methods; PCSubclasser does not.
        int firstParamOffset = getAccessorParameterOffset();

        // dfg: if (inst.pcFlags == 0) inst.<field> = value;
        JumpInstruction ifins = null;
        byte fieldFlag = getFieldFlag(fmd);
        if ((fieldFlag & PersistenceCapable.CHECK_WRITE) > 0) {
            loadManagedInstance(code, true);
            code.getfield().setField(PRE + "Flags", byte.class);
            ifins = code.ifne();
            loadManagedInstance(code, true);
            code.xload().setParam(firstParamOffset);
            addSetManagedValueCode(code, fmd);
            code.vreturn();
        }

        // if (inst.pcStateManager == null) inst.<field> = value;
        Instruction ins = loadManagedInstance(code, true);
        if (ifins != null)
            ifins.setTarget(ins);
        code.getfield().setField(SM, SMTYPE);
        ifins = code.ifnonnull();
        loadManagedInstance(code, true);
        code.xload().setParam(firstParamOffset);
        addSetManagedValueCode(code, fmd);
        code.vreturn();

        // inst.pcStateManager.setting<fieldType>Field (inst,
        //	pcInheritedFieldCount + <index>, inst.<field>, value, 0);
        ifins.setTarget(loadManagedInstance(code, true));
        code.getfield().setField(SM, SMTYPE);
        loadManagedInstance(code, true);
        code.getstatic().setField(INHERIT, int.class);
        code.constant().setValue(index);
        code.iadd();
        loadManagedInstance(code, true);
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
        if (_meta.getPCSuperclass() == null
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
            } catch (NoSuchMethodException nsme) {
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
        } else if (impl) {
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
            code.getfield().setField(declarer, name, Object.class.getName());
        } else
            code.constant().setNull();
        code.areturn();
        code.calculateMaxLocals();
        code.calculateMaxStack();

        // public void pcSetDetachedState (Object state)
        method = _pc.declareMethod(PRE + "SetDetachedState",
            void.class, new Class []{ Object.class });
        method.setAccessFlags(access);
        code = method.getCode(true);
        if (impl) {
            // pcDetachedState = state;
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.putfield().setField(declarer, name,
                Object.class.getName());
        }
        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
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
        Class[] input = new Class[]{ ObjectInputStream.class };
        Class[] output = new Class[]{ ObjectOutputStream.class };
        if (_pc.getDeclaredMethod("readObject", input) != null
            || _pc.getDeclaredMethod("writeObject", output) != null)
            throw new UserException(_loc.get("detach-custom-ser", _meta));
        input[0] = ObjectInput.class;
        output[0] = ObjectOutput.class;
        if (_pc.getDeclaredMethod("readExternal", input) != null
            || _pc.getDeclaredMethod("writeExternal", output) != null)
            throw new UserException(_loc.get("detach-custom-extern", _meta));

        // create list of all unmanaged serializable fields
        BCField[] fields = _pc.getDeclaredFields();
        Collection unmgd = new ArrayList(fields.length);
        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].isTransient() && !fields[i].isStatic()
                && !fields[i].isFinal()
                && !fields[i].getName().startsWith(PRE)
                && _meta.getDeclaredField(fields[i].getName()) == null)
                unmgd.add(fields[i]);
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
        Class[] inargs = new Class[]{ ObjectInput.class };
        BCMethod meth = _pc.declareMethod("readExternal", void.class, inargs);
        Exceptions exceps = meth.getExceptions(true);
        exceps.addException(IOException.class);
        exceps.addException(ClassNotFoundException.class);
        Code code = meth.getCode(true);

        // super.readExternal (in);
        Class sup = _meta.getDescribedType().getSuperclass();
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
                void.class, new Class[]{ Object.class });

            // pcReplaceStateManager ((StateManager) in.readObject ());
            loadManagedInstance(code, false);
            code.aload().setParam(0);
            code.invokeinterface().setMethod(ObjectInput.class, "readObject",
                Object.class, null);
            code.checkcast().setType(StateManager.class);
            code.invokevirtual().setMethod(PRE + "ReplaceStateManager",
                void.class, new Class[]{ StateManager.class });
        }

        // read managed fields
        FieldMetaData[] fmds = _meta.getFields();
        for (int i = 0; i < fmds.length; i++)
            if (!fmds[i].isTransient())
                readExternal(code, fmds[i].getName(),
                    fmds[i].getDeclaredType(), fmds[i]);

        code.vreturn();
        code.calculateMaxStack();
        code.calculateMaxLocals();
    }

    /**
     * Read unmanaged fields from the stream (pcReadUnmanaged).
     */
    private void addReadUnmanaged(Collection unmgd, boolean parentDetachable)
        throws NoSuchMethodException {
        Class[] inargs = new Class[]{ ObjectInput.class };
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
        for (Iterator itr = unmgd.iterator(); itr.hasNext();) {
            field = (BCField) itr.next();
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
            methName = Character.toUpperCase(methName.charAt(0))
                + methName.substring(1);
            methName = "read" + methName;
        } else
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
            code.putfield().setField(fieldName, type);
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
                        new Class[]{ int.class });
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
        Class[] outargs = new Class[]{ ObjectOutput.class };
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
            Class[] objargs = new Class[]{ Object.class };
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

        // write managed fields
        FieldMetaData[] fmds = _meta.getFields();
        for (int i = 0; i < fmds.length; i++)
            if (!fmds[i].isTransient())
                writeExternal(code, fmds[i].getName(),
                    fmds[i].getDeclaredType(), fmds[i]);

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
        Class[] outargs = new Class[]{ ObjectOutput.class };
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
        for (Iterator itr = unmgd.iterator(); itr.hasNext();) {
            field = (BCField) itr.next();
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
            methName = Character.toUpperCase(methName.charAt(0))
                + methName.substring(1);
            methName = "write" + methName;
        } else
            methName = "writeObject";

        // out.write<type> (<field>);
        code.aload().setParam(0);
        loadManagedInstance(code, false);
        if (fmd == null)
            code.getfield().setField(fieldName, type);
        else
            addGetManagedValueCode(code, fmd);
        Class[] args = new Class[]{ type };
        if (type == byte.class || type == char.class || type == short.class)
            args[0] = int.class;
        else if (!type.isPrimitive())
            args[0] = Object.class;
        code.invokeinterface().setMethod(ObjectOutput.class, methName,
            void.class, args);
    }

    /**
     * Load the field value specified by <code>fmd</code> onto the stack.
     * Before this method is called, the object that the data should be loaded
     * from will be on the top of the stack.
     */
    private void addGetManagedValueCode(Code code, FieldMetaData fmd)
        throws NoSuchMethodException {
        if (_meta.getAccessType() == ClassMetaData.ACCESS_FIELD)
            code.getfield().setField(fmd.getName(), fmd.getDeclaredType());
        else // property
        {
            Method meth = (Method) fmd.getBackingMember();
            code.invokevirtual().setMethod(PRE + meth.getName(),
                meth.getReturnType(), meth.getParameterTypes());
        }
    }

    /**
     * Store the value at the top of the stack into the field value specified
     * by <code>fmd</code>. Before this method is called, the data to load will
     * be on the top of the stack and the object that the data should be loaded
     * into will be second in the stack.
     */
    private void addSetManagedValueCode(Code code, FieldMetaData fmd)
        throws NoSuchMethodException {
        if (_meta.getAccessType() == ClassMetaData.ACCESS_FIELD)
            code.putfield().setField(fmd.getName(), fmd.getDeclaredType());
        else // property
            code.invokevirtual().setMethod(PRE + getSetterName(fmd),
                void.class, new Class[]{ fmd.getDeclaredType() });
    }

    /**
     * Return the offset that the first meaningful accessor parameter is at.
     */
    private int getAccessorParameterOffset() {
        return (_meta.getAccessType() == ClassMetaData.ACCESS_FIELD) ? 1 : 0;
    }

    /**
     * Add the {@link Instruction}s to load the instance to modify onto the
     * stack, and return it. If <code>userObject</code> is set,
     * then <code>code</code> will be accessing data in the user-visible
     * object (which might not be 'this' in proxying contexts),
     * and the load code should behave accordingly. Otherwise,
     * <code>code</code> will be accessing PC-contract data, which must always
     * be in 'this'. If <code>forAccesor</code> is set, then <code>code</code>
     * is in an accessor method; otherwise, it is in one of the PC-specified
     * methods.
     *
     * @return the first instruction added to <code>code</code>.
     */
    private Instruction loadManagedInstance(Code code,
        boolean forAccessor) {
        if (_meta.getAccessType() == ClassMetaData.ACCESS_FIELD && forAccessor)
            return code.aload().setParam(0);
        return code.aload().setThis();
    }

    /**
     * Create the generated getter {@link BCMethod} for <code>fmd</code>. The
     * calling environment will then populate this method's code block.
     */
    private BCMethod createGetMethod(FieldMetaData fmd) {
        BCMethod getter;
        if (_meta.getAccessType() == ClassMetaData.ACCESS_FIELD) {
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
        newgetter.makePrivate();
        transferCodeAttributes(getter, newgetter);
        return getter;
    }

    /**
     * Create the generated setter {@link BCMethod} for <code>fmd</code>. The
     * calling environment will then populate this method's code block.
     */
    private BCMethod createSetMethod(FieldMetaData fmd) {
        BCMethod setter;
        if (_meta.getAccessType() == ClassMetaData.ACCESS_FIELD) {
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
        newsetter.makePrivate();
        transferCodeAttributes(setter, newsetter);
        return setter;
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
     * {@link Configuration}; optional.</li>
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
    public static void main(String[] args)
        throws IOException {
        Options opts = new Options();
        args = opts.setFromCmdLine(args);
        OpenJPAConfiguration conf = new OpenJPAConfigurationImpl();
        try {
            if (!run(conf, args, opts))
                System.err.println(_loc.get("enhance-usage"));
        } finally {
            conf.close();
        }
    }

    /**
     * Run the tool. Returns false if invalid options given.
     */
    public static boolean run(OpenJPAConfiguration conf, String[] args,
        Options opts)
        throws IOException {
        if (opts.containsKey("help") || opts.containsKey("-help"))
            return false;

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
        return run(conf, args, flags, null, null, null);
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
            loader = new TemporaryClassLoader(loader);

        if (repos == null) {
            repos = conf.newMetaDataRepositoryInstance();
            repos.setSourceMode(MetaDataRepository.MODE_META);
        }

        Log log = conf.getLog(OpenJPAConfiguration.LOG_TOOL);
        Collection classes;
        if (args.length == 0) {
            log.info(_loc.get("running-all-classes"));
            classes = repos.loadPersistentTypes(true, loader);
        } else {
            ClassArgParser cap = conf.getMetaDataRepositoryInstance().
                getMetaDataFactory().newClassArgParser();
            cap.setClassLoader(loader);
            classes = new HashSet();
            for (int i = 0; i < args.length; i++)
                classes.addAll(Arrays.asList(cap.parseTypes(args[i])));
        }

        Project project = new Project();
        BCClass bc;
        PCEnhancer enhancer;
        int status;
        Class cls;
        for (Iterator itr = classes.iterator(); itr.hasNext();) {
            cls = (Class) itr.next();
            log.info(_loc.get("enhance-running", cls));

            bc = project.loadClass(cls);
            enhancer = new PCEnhancer(conf, bc, repos);
            if (writer != null)
                enhancer.setBytecodeWriter(writer);
            enhancer.setDirectory(flags.directory);
            enhancer.setAddDefaultConstructor(flags.addDefaultConstructor);
            status = enhancer.run();
            if (status == ENHANCE_NONE)
                log.info(_loc.get("enhance-norun"));
            else if (status == ENHANCE_INTERFACE)
                log.info(_loc.get("enhance-interface"));
            else if (status == ENHANCE_AWARE) {
                log.info(_loc.get("enhance-aware"));
                enhancer.record();
            } else {
                enhancer.record();
                if ((status & ENHANCE_OID) != 0) {
                    if (log.isInfoEnabled()) {
                        Collection oids = enhancer.getObjectIdBytecode();
                        StringBuffer buf = new StringBuffer();
                        for (Iterator oiditr = oids.iterator();
                            oiditr.hasNext();) {
                            buf.append(((BCClass) oiditr.next()).getName());
                            if (oiditr.hasNext())
                                buf.append(", ");
                        }
                        log.info(_loc.get("enhance-running-oids", buf));
                    }
                }
            }
            project.clear();
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
	public static interface AuxiliaryEnhancer
	{
		public void run (BCClass bc, ClassMetaData meta);
		public boolean skipEnhance(BCMethod m);
	}
}
